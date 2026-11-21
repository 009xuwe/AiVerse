package com.aiverse.feature.agent.domain.tool.file

import com.aiverse.core.util.AILogger
import com.aiverse.core.util.FileLogger
import com.aiverse.feature.agent.data.remote.openai.ImageGenerationRequest
import com.aiverse.feature.agent.data.remote.openai.OpenAIApi
import com.aiverse.feature.agent.domain.model.AgentContext
import com.aiverse.feature.agent.domain.model.AgentImage
import com.aiverse.feature.agent.domain.provider.enrichWithHttpErrorBody
import com.aiverse.feature.agent.domain.provider.isKeySwitchFailure
import com.aiverse.feature.agent.domain.provider.joinUrl
import com.aiverse.feature.agent.domain.provider.parseInteractionSteps
import com.aiverse.feature.agent.domain.provider.resolveCustomHeaders
import com.aiverse.feature.agent.domain.tool.ParameterType
import com.aiverse.feature.agent.domain.tool.PendingToolPermission
import com.aiverse.feature.agent.domain.tool.ToolCapability
import com.aiverse.feature.agent.domain.tool.ToolParameter
import com.aiverse.feature.agent.domain.tool.ToolPermissionPolicy
import com.aiverse.feature.agent.domain.tool.ToolResult
import com.aiverse.feature.settings.domain.model.AIProviderConfig
import com.aiverse.feature.settings.domain.model.ProviderType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * 生图工具：调用 OpenAI Images API（POST /v1/images/generations）按提示词生成位图。
 *
 * 使用「设置 → 默认模型 → 生图模型」配置的 provider + 模型（如 gpt-image-1 / dall-e-3）；
 * 未配置时返回带设置指引的错误。生成的图片：
 * - 始终以 [AgentImage] 返回给工作流，聊天区可直接渲染；
 * - 传入 `output_path` 时同时落盘到工作区（多张时自动加序号后缀）。
 *
 * 请求体兼容 dall-e 系（显式 `response_format=b64_json`）与 GPT image 系
 * （不支持该参数、总是返回 base64）；调用失败时把错误信息原样作为工具结果返回。
 *
 * 与改图工具 [EditImageTool] 共用 provider 解析、落盘、结果组装等逻辑（见 [AbstractImageTool]）。
 */
class GenerateImageTool @Inject constructor(
    private val openAIApi: OpenAIApi
) : AbstractImageTool() {

    override val name = "generateImage"
    override val description = "根据文本描述生成图片并在聊天区展示给用户。支持指定尺寸与张数，可选通过 output_path 同时保存为工作区文件。"
    override val permissionPolicy = ToolPermissionPolicy.ASK
    override val capabilities = setOf(ToolCapability.NETWORK_WRITE, ToolCapability.WRITE_WORKSPACE)
    override val logTag = "GenerateImageTool"
    override val parameters = mapOf(
        "prompt" to ToolParameter(
            name = "prompt",
            type = ParameterType.STRING,
            description = "图片内容的详细描述（主体、风格、构图、色调、氛围等）。",
            required = true
        ),
        "size" to ToolParameter(
            name = "size",
            type = ParameterType.STRING,
            description = "图片尺寸，如 1024x1024（默认）、1024x1536、1536x1024。",
            required = false
        ),
        "n" to ToolParameter(
            name = "n",
            type = ParameterType.INTEGER,
            description = "生成张数，默认 1，最多 4。dall-e-3 只支持 1 张。",
            required = false
        ),
        "quality" to ToolParameter(
            name = "quality",
            type = ParameterType.STRING,
            description = "画质：GPT Image 系列支持 low / medium / high / auto（默认 auto）；dall-e-3 支持 standard / hd（默认 standard）。",
            required = false,
            enum = listOf("low", "medium", "high", "auto", "standard", "hd")
        ),
        "background" to ToolParameter(
            name = "background",
            type = ParameterType.STRING,
            description = "背景：transparent / opaque / auto（默认 auto），仅 GPT Image 系列模型支持；transparent 需配合 png 或 webp 输出格式。",
            required = false,
            enum = listOf("transparent", "opaque", "auto")
        ),
        "moderation" to ToolParameter(
            name = "moderation",
            type = ParameterType.STRING,
            description = "内容审核级别：low / auto（默认 auto），仅 GPT Image 系列模型支持。",
            required = false,
            enum = listOf("low", "auto")
        ),
        "style" to ToolParameter(
            name = "style",
            type = ParameterType.STRING,
            description = "风格：vivid / natural，仅 dall-e-3 支持。",
            required = false,
            enum = listOf("vivid", "natural")
        ),
        "output_format" to ToolParameter(
            name = "output_format",
            type = ParameterType.STRING,
            description = "输出格式：png / jpeg / webp（默认 png），仅 GPT Image 系列模型支持。",
            required = false,
            enum = listOf("png", "jpeg", "webp")
        ),
        "output_path" to ToolParameter(
            name = "output_path",
            type = ParameterType.STRING,
            description = "保存路径（可选）。不传默认保存到 ~/.aiverse/generated-images/；传了则保存到指定路径，如 ~/workspace/assets/image.png。",
            required = false
        )
    )

    override fun buildPermissionRequest(
        callId: String,
        args: Map<String, JsonElement>,
        argsPreview: String
    ): PendingToolPermission {
        val prompt = args["prompt"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val outputPath = args["output_path"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val count = args["n"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
        return PendingToolPermission(
            id = callId,
            toolName = name,
            title = "确认生成图片",
            summary = "AI 请求调用生图模型生成图片",
            details = if (outputPath.isBlank()) {
                "提示词：$prompt\n数量：$count\n保存到：$DEFAULT_OUTPUT_DIR/"
            } else {
                "提示词：$prompt\n数量：$count\n保存到：$outputPath"
            },
            argsPreview = argsPreview
        )
    }

    override suspend fun executeWithContext(
        args: Map<String, JsonElement>,
        context: AgentContext
    ): ToolResult = withContext(Dispatchers.IO) {
        val prompt = args["prompt"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (prompt.isEmpty()) {
            return@withContext ToolResult.Error("缺少必需参数：prompt", "MISSING_PROMPT")
        }
        val size = args["size"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null }
            ?: DEFAULT_SIZE
        val n = args["n"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
        if (n !in 1..MAX_IMAGES) {
            return@withContext ToolResult.Error("n 取值必须在 1 到 $MAX_IMAGES 之间，当前为 $n", "INVALID_PARAMS")
        }
        val quality = args["quality"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()?.ifBlank { null }
        val background = args["background"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()?.ifBlank { null }
        val moderation = args["moderation"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()?.ifBlank { null }
        val style = args["style"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()?.ifBlank { null }
        val outputFormat = args["output_format"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()?.ifBlank { null }
        val outputPath = args["output_path"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null }

        var seq = 0
        var lastProviderId = ""
        var activeApiKey = ""
        return@withContext try {
            val provider = resolveImageGenProvider()
            lastProviderId = provider.id
            activeApiKey = keyRotator.activeKey(provider, context.sessionId) ?: provider.firstUsableApiKey
            val model = provider.effectiveModel
            // Gemini 官方图像模型走 Interactions 协议（v1beta/interactions），OpenAI 的
            // /v1/images/generations 端点打不通；设了 Gemini 生图模型时切独立通道，
            // OpenAI 特有的参数（size/quality/background 等）在 Gemini 分支里忽略。
            if (provider.type == ProviderType.GEMINI) {
                return@withContext generateViaGemini(provider, activeApiKey, prompt, n, outputPath, context.sessionId)
            }
            val isGptImage = model.startsWith("gpt-image", ignoreCase = true)
            val isDalle2 = model.equals("dall-e-2", ignoreCase = true)
            val isDalle3 = model.equals("dall-e-3", ignoreCase = true)
            validateImageParams(
                model, isGptImage, isDalle2, isDalle3, n, quality,
                background, moderation, style, outputFormat
            )?.let {
                return@withContext ToolResult.Error(it, "INVALID_PARAMS")
            }
            FileLogger.i(logTag, "generateImage provider=${provider.id} model=$model prompt=$prompt n=$n size=$size")

            val url = if (provider.useFullUrl) provider.baseUrl else joinUrl(provider.baseUrl, "v1/images/generations")
            // 兼容性规则：
            // 1. 只有真正的官方 GPT Image 系列模型（如 gpt-image-1 / gpt-image-1.5）才支持 output_format，且其默认就返回 base64、不支持 response_format；
            // 2. 其它模型（DALL-E、各类聚合网关如 agnes/OneAPI/NewAPI/Flux/SD 等）必须传 response_format=b64_json 才会返回 base64，
            //    且绝对不能传 output_format（带上会直接被网关报 HTTP 400：output_format is not supported）。
            val request = ImageGenerationRequest(
                model = model,
                prompt = prompt,
                n = n,
                size = size,
                quality = quality,
                response_format = if (!isGptImage) "b64_json" else null,
                output_format = if (isGptImage) outputFormat else null,
                background = if (isGptImage) background else null,
                moderation = if (isGptImage) moderation else null,
                style = if (isDalle3) style else null
            )

            seq = AILogger.logRequest(context.sessionId, provider.id, model, "POST", url, request)

            val response = openAIApi.createImage(
                url = url,
                authorization = "Bearer $activeApiKey",
                extraHeaders = resolveCustomHeaders(provider.customHeaders, context.sessionId, activeApiKey),
                request = request
            )
            AILogger.logResponse(context.sessionId, provider.id, response, seq)
            buildOpenAIImageResponseResult(response, n, outputPath, model)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val enriched = e.enrichWithHttpErrorBody()
            if (activeApiKey.isNotEmpty() && enriched.isKeySwitchFailure()) {
                keyRotator.reportFailure(lastProviderId, context.sessionId, activeApiKey)
            }
            FileLogger.e(logTag, "generateImage 失败", enriched)
            AILogger.logError(context.sessionId, lastProviderId, enriched, seq)
            ToolResult.Error(enriched.message ?: "生图调用失败", "IMAGE_GEN_FAILED")
        }
    }

    /**
     * Gemini 生图通道：跟随 provider 的 useResponseApi 设置。开启走 Interactions 非流式端点
     * （从 `steps` 时间线解析图片）；关闭（默认）走 generateContent（图像模型原生支持，从
     * candidates[].parts 的 inlineData 解析）。多张走多次调用；OpenAI 特有参数忽略。
     */
    private suspend fun generateViaGemini(
        provider: AIProviderConfig,
        apiKey: String,
        prompt: String,
        n: Int,
        outputPath: String?,
        sessionId: String?
    ): ToolResult {
        val model = provider.effectiveModel
        FileLogger.i(logTag, "generateImage(Gemini) provider=${provider.id} model=$model prompt=$prompt n=$n")
        return if (provider.useResponseApi) {
            generateViaGeminiInteractions(provider, apiKey, model, prompt, n, outputPath, sessionId)
        } else {
            generateViaGeminiGenerateContent(provider, apiKey, model, prompt, n, outputPath, sessionId)
        }
    }

    /** Interactions 通道：非流式 createInteraction，从响应 steps 提取图片。 */
    private suspend fun generateViaGeminiInteractions(
        provider: AIProviderConfig,
        apiKey: String,
        model: String,
        prompt: String,
        n: Int,
        outputPath: String?,
        sessionId: String?
    ): ToolResult {
        val url = if (provider.useFullUrl) provider.baseUrl else joinUrl(provider.baseUrl, "v1beta/interactions")
        val effectiveBasePath = outputPath ?: createDefaultBasePath()
        val overwrite = outputPath != null

        val agentImages = mutableListOf<AgentImage>()
        val savedDisplayPaths = mutableListOf<String>()
        val filesList = mutableListOf<JsonObject>()
        var totalBytes = 0L
        var failedCount = 0
        var lastSeq = 0
        try {
            for (index in 0 until n) {
                val request = mapOf("model" to model, "input" to prompt, "store" to false)
                lastSeq = AILogger.logRequest(sessionId, provider.id, model, "POST", url, request)
                val response = geminiApi.createInteraction(
                    url = url,
                    apiKey = apiKey,
                    extraHeaders = resolveCustomHeaders(provider.customHeaders, sessionId, apiKey),
                    request = request
                )
                AILogger.logResponse(sessionId, provider.id, response, lastSeq)
                val status = response.get("status")?.takeIf { it.isJsonPrimitive }?.asString
                if (status == "failed" || status == "cancelled" || status == "budget_exceeded") {
                    val reason = response.get("errors")?.takeIf { it.isJsonArray }?.asJsonArray
                        ?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
                        ?.get("message")?.takeIf { it.isJsonPrimitive }?.asString
                        ?: "Gemini 生图失败（status=$status）"
                    failedCount += n - index
                    return if (agentImages.isEmpty()) {
                        ToolResult.Error(reason, "IMAGE_GEN_FAILED")
                    } else {
                        buildGeminiResult(agentImages, savedDisplayPaths, filesList, model, n, outputPath, failedCount, reason)
                    }
                }
                val beforeCount = agentImages.size
                val parsed = parseInteractionSteps(response.get("steps")?.takeIf { it.isJsonArray }?.asJsonArray)
                parsed.images.forEach { image ->
                    runCatching {
                        totalBytes += persistImage(
                            image.base64Data, effectiveBasePath, overwrite, agentImages,
                            savedDisplayPaths, filesList, totalBytes
                        )
                    }.onFailure { FileLogger.w(logTag, "处理 Gemini 生图结果失败", it) }
                }
                if (agentImages.size == beforeCount) failedCount++
                if (index == 0 && agentImages.isEmpty() && n > 1) break
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val enriched = e.enrichWithHttpErrorBody()
            if (enriched.isKeySwitchFailure()) keyRotator.reportFailure(provider.id, sessionId, apiKey)
            FileLogger.e(logTag, "generateImage(Gemini) 失败", enriched)
            AILogger.logError(sessionId, provider.id, enriched, lastSeq)
            if (agentImages.isNotEmpty()) {
                return buildGeminiResult(
                    agentImages, savedDisplayPaths, filesList, model, n, outputPath,
                    failedCount.coerceAtLeast(n - agentImages.size), enriched.message
                )
            }
            return ToolResult.Error(enriched.message ?: "Gemini 生图调用失败", "IMAGE_GEN_FAILED")
        }

        return buildGeminiResult(agentImages, savedDisplayPaths, filesList, model, n, outputPath, failedCount)
    }

    /**
     * generateContent 通道：图像模型（Nano Banana）原生支持该端点，图片在
     * candidates[].content.parts 的 inlineData（camelCase）中整块返回。
     */
    private suspend fun generateViaGeminiGenerateContent(
        provider: AIProviderConfig,
        apiKey: String,
        model: String,
        prompt: String,
        n: Int,
        outputPath: String?,
        sessionId: String?
    ): ToolResult {
        val url = if (provider.useFullUrl) provider.baseUrl else joinUrl(provider.baseUrl, "v1beta/models/$model:generateContent")
        val effectiveBasePath = outputPath ?: createDefaultBasePath()
        val overwrite = outputPath != null

        val agentImages = mutableListOf<AgentImage>()
        val savedDisplayPaths = mutableListOf<String>()
        val filesList = mutableListOf<JsonObject>()
        var totalBytes = 0L
        var failedCount = 0
        var lastSeq = 0
        try {
            for (index in 0 until n) {
                val request = mapOf(
                    "contents" to listOf(mapOf("parts" to listOf(mapOf("text" to prompt))))
                )
                lastSeq = AILogger.logRequest(sessionId, provider.id, model, "POST", url, request)
                val response = geminiApi.generateContent(
                    url = url,
                    apiKey = apiKey,
                    extraHeaders = resolveCustomHeaders(provider.customHeaders, sessionId, apiKey),
                    request = request
                )
                AILogger.logResponse(sessionId, provider.id, response, lastSeq)
                val beforeCount = agentImages.size
                extractGenerateContentImageData(response).forEach { base64 ->
                    runCatching {
                        totalBytes += persistImage(
                            base64, effectiveBasePath, overwrite, agentImages,
                            savedDisplayPaths, filesList, totalBytes
                        )
                    }.onFailure { FileLogger.w(logTag, "处理 Gemini 生图结果失败", it) }
                }
                if (agentImages.size == beforeCount) failedCount++
                if (index == 0 && agentImages.isEmpty() && n > 1) break
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val enriched = e.enrichWithHttpErrorBody()
            if (enriched.isKeySwitchFailure()) keyRotator.reportFailure(provider.id, sessionId, apiKey)
            FileLogger.e(logTag, "generateImage(Gemini) 失败", enriched)
            AILogger.logError(sessionId, provider.id, enriched, lastSeq)
            if (agentImages.isNotEmpty()) {
                return buildGeminiResult(
                    agentImages, savedDisplayPaths, filesList, model, n, outputPath,
                    failedCount.coerceAtLeast(n - agentImages.size), enriched.message
                )
            }
            return ToolResult.Error(enriched.message ?: "Gemini 生图调用失败", "IMAGE_GEN_FAILED")
        }

        return buildGeminiResult(agentImages, savedDisplayPaths, filesList, model, n, outputPath, failedCount)
    }

    /** 从 generateContent 响应提取图片 base64 列表（inlineData / inline_data 都认）。 */
    private fun extractGenerateContentImageData(response: com.google.gson.JsonObject): List<String> {
        val parts = response.get("candidates")?.takeIf { it.isJsonArray }?.asJsonArray
            ?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
            ?.get("content")?.takeIf { it.isJsonObject }?.asJsonObject
            ?.get("parts")?.takeIf { it.isJsonArray }?.asJsonArray
            ?: return emptyList()
        return parts.mapNotNull { partEl ->
            val part = partEl.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val inline = part.get("inlineData") ?: part.get("inline_data")
            (inline?.takeIf { it.isJsonObject }?.asJsonObject)?.get("data")
                ?.takeIf { it.isJsonPrimitive }?.asString
                ?.takeIf { it.isNotBlank() }
        }
    }

    /** 组装 Gemini 生图的统一结果（两通道共用）。 */
    private fun buildGeminiResult(
        agentImages: List<AgentImage>,
        savedDisplayPaths: List<String>,
        filesList: List<JsonObject>,
        model: String,
        n: Int,
        outputPath: String?,
        failedCount: Int,
        failureReason: String? = null
    ): ToolResult {
        if (agentImages.isEmpty()) {
            return ToolResult.Error("Gemini 生图未返回图片数据（模型可能只回复了文本，或图片无效或过大）", "EMPTY_RESULT")
        }
        return buildImageResult(
            agentImages, savedDisplayPaths, filesList, model, n,
            outputPath, failedCount, failureReason
        )
    }

    internal companion object {
        const val TAG = "GenerateImageTool"

        internal fun validateImageParams(
            model: String,
            isGptImage: Boolean,
            isDalle2: Boolean,
            isDalle3: Boolean,
            n: Int,
            quality: String?,
            background: String?,
            moderation: String?,
            style: String?,
            outputFormat: String?
        ): String? {
            if (background != null && background !in BACKGROUNDS) return "background 取值 $background 不支持，可选：${BACKGROUNDS.joinToString(" / ")}。"
            if (moderation != null && moderation !in MODERATIONS) return "moderation 取值 $moderation 不支持，可选：${MODERATIONS.joinToString(" / ")}。"
            if (style != null && style !in STYLES) return "style 取值 $style 不支持，可选：${STYLES.joinToString(" / ")}。"
            if (outputFormat != null && outputFormat !in OUTPUT_FORMATS) return "output_format 取值 $outputFormat 不支持，可选：${OUTPUT_FORMATS.joinToString(" / ")}。"
            if (!isGptImage && background != null) return "background 参数仅 GPT Image 系列模型支持，当前模型 $model 不支持。"
            if (!isGptImage && moderation != null) return "moderation 参数仅 GPT Image 系列模型支持，当前模型 $model 不支持。"
            if (!isGptImage && outputFormat != null) return "output_format 参数仅 GPT Image 系列模型支持，当前模型 $model 不支持。"
            if (!isDalle3 && style != null) return "style 参数仅 dall-e-3 支持，当前模型 $model 不支持。"
            if (isDalle3 && n > 1) return "dall-e-3 一次只能生成 1 张（n=1），需要多张请改用 GPT Image 系列模型。"
            if (quality != null) {
                val allowed = when {
                    isGptImage -> QUALITY_GPT_IMAGE
                    isDalle3 -> QUALITY_DALLE3
                    isDalle2 -> QUALITY_DALLE2
                    else -> return "quality 参数仅 OpenAI 官方生图模型（gpt-image 系列 / dall-e-2 / dall-e-3）支持，当前模型 $model 不支持。"
                }
                if (quality !in allowed) return "quality 取值 $quality 当前模型 $model 不支持，可选：${allowed.joinToString(" / ")}。"
            }
            return null
        }
    }
}
