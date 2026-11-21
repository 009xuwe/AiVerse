package com.aiverse.feature.agent.domain.tool.file

import android.util.Base64
import com.aiverse.core.util.AILogger
import com.aiverse.core.util.FileLogger
import com.aiverse.feature.agent.data.remote.gemini.GeminiApi
import com.aiverse.feature.agent.data.remote.openai.ImageGenerationResponse
import com.aiverse.feature.agent.data.remote.openai.ImageGenerationUsage
import com.aiverse.feature.agent.domain.model.AgentImage
import com.aiverse.feature.agent.domain.tool.AbstractContextualTool
import com.aiverse.feature.agent.domain.tool.ToolResult
import com.aiverse.feature.settings.data.repository.ImageGenModelSettingsRepository
import com.aiverse.feature.settings.data.repository.ProviderKeyRotator
import com.aiverse.feature.settings.domain.model.AIProviderConfig
import com.aiverse.feature.settings.domain.repository.AIProviderRepository
import com.aiverse.feature.workspace.domain.FileAccessProvider
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * 生图 / 改图工具的公共基类：抽出两类工具共用的 provider 解析、参考图与结果落盘、
 * base64 解码与大小限制、格式探测、结果组装等逻辑，供 [GenerateImageTool]（文生图）与
 * [EditImageTool]（图生图/改图）复用。
 *
 * 两者都使用「设置 → 默认模型 → 生图模型」配置的 provider + 模型；未配置或不支持改图时
 * 返回带设置指引的错误。生成/编辑出的图片始终以 [AgentImage] 返回给工作流，聊天区可直接渲染；
 * 传入 `output_path` 时同时落盘到工作区（多张时自动加序号后缀）。
 */
abstract class AbstractImageTool : AbstractContextualTool() {

    @Inject
    lateinit var fileAccess: FileAccessProvider

    @Inject
    lateinit var imageGenModelSettingsRepository: ImageGenModelSettingsRepository

    @Inject
    lateinit var aiProviderRepository: AIProviderRepository

    @Inject
    lateinit var geminiApi: GeminiApi

    @Inject
    lateinit var httpClient: OkHttpClient

    @Inject
    lateinit var keyRotator: ProviderKeyRotator

    /** 日志标签：子类各自提供，便于区分生图与改图日志。 */
    protected abstract val logTag: String

    /**
     * 解析「设置 → 默认模型 → 生图模型」配置的供应商与模型。未配置、供应商缺失/停用/
     * 未填 Key 时抛 [IllegalStateException]，由调用方转成工具错误结果。
     */
    protected suspend fun resolveImageGenProvider(): AIProviderConfig {
        val providerId = imageGenModelSettingsRepository.getImageGenProviderId().trim()
        val model = imageGenModelSettingsRepository.getImageGenModel().trim()
        if (providerId.isEmpty() || model.isEmpty()) {
            throw IllegalStateException("未配置生图模型：请到「设置 → 默认模型 → 生图模型」中选择支持图像输出的模型（如 gpt-image-1 / dall-e-3）。")
        }
        val config = aiProviderRepository.getProviderById(providerId)
            ?: throw IllegalStateException("生图模型配置的供应商不存在或已被删除，请到「设置 → 默认模型 → 生图模型」重新选择。")
        if (!config.isEnabled) throw IllegalStateException("生图模型配置的供应商「${config.name}」未启用。")
        if (!config.hasUsableApiKey) throw IllegalStateException("生图模型配置的供应商「${config.name}」未填写 API Key。")
        return config.copy(selectedModel = model)
    }

    /** base64 → 按真实格式写文件 → 组装 AgentImage 与 files 附件。返回写入字节数。 */
    protected fun persistImage(
        base64: String,
        effectiveBasePath: String,
        overwrite: Boolean,
        agentImages: MutableList<AgentImage>,
        savedDisplayPaths: MutableList<String>,
        filesList: MutableList<JsonObject>,
        currentTotalBytes: Long
    ): Long {
        val bytes = decodeImageBase64(base64)
        checkedTotalBytes(currentTotalBytes, bytes.size)
        persistImageBytes(
            bytes, base64, effectiveBasePath, overwrite,
            agentImages, savedDisplayPaths, filesList
        )
        return bytes.size.toLong()
    }

    protected fun persistImageBytes(
        bytes: ByteArray,
        base64: String,
        effectiveBasePath: String,
        overwrite: Boolean,
        agentImages: MutableList<AgentImage>,
        savedDisplayPaths: MutableList<String>,
        filesList: MutableList<JsonObject>
    ) {
        if (bytes.isEmpty()) throw IOException("图片内容为空")
        val format = detectImageFormat(bytes)
        val targetPath = buildTargetPath(effectiveBasePath, agentImages.size, format)
        val realMime = mimeForFormat(format)
        fileAccess.writeBytes(targetPath, bytes, overwrite = overwrite)
        val displayPath = fileAccess.toDisplayPath(targetPath)
        val localFile = fileAccess.copyToLocal(targetPath)
        val fileName = targetPath.substringAfterLast('/')

        agentImages.add(AgentImage(mimeType = realMime, base64Data = base64, path = displayPath))
        savedDisplayPaths.add(displayPath)
        filesList.add(
            JsonObject(
                mapOf(
                    "path" to JsonPrimitive(displayPath),
                    "local_path" to JsonPrimitive(localFile.absolutePath),
                    "name" to JsonPrimitive(fileName),
                    "mime_type" to JsonPrimitive(realMime),
                    "size_bytes" to JsonPrimitive(bytes.size.toLong()),
                    "is_image" to JsonPrimitive(true)
                )
            )
        )
    }

    /** 组装统一结果：把已落盘的图片列表汇总成工具成功结果（生图与改图共用）。 */
    protected fun buildImageResult(
        agentImages: List<AgentImage>,
        savedDisplayPaths: List<String>,
        filesList: List<JsonObject>,
        model: String,
        requestedCount: Int,
        outputPath: String?,
        failedCount: Int,
        failureReason: String? = null,
        usage: ImageGenerationUsage? = null,
        generatedVerb: String = "已生成"
    ): ToolResult {
        val effectiveFailedCount = failedCount.coerceAtLeast(0)
        val content = buildString {
            append("$generatedVerb ${agentImages.size} 张图片")
            if (effectiveFailedCount > 0) append("，另有 $effectiveFailedCount 张失败")
            if (!failureReason.isNullOrBlank()) append("（").append(failureReason).append("）")
            if (outputPath == null) append("，已保存至 $DEFAULT_OUTPUT_DIR/：")
            else append("，已保存至指定路径：")
            savedDisplayPaths.forEach { p -> append("\n- ").append(p) }
        }
        val data = mutableMapOf<String, JsonElement>(
            "status" to JsonPrimitive(if (effectiveFailedCount > 0) "partial" else "generated"),
            "content" to JsonPrimitive(content),
            "image_count" to JsonPrimitive(agentImages.size),
            "failed_count" to JsonPrimitive(effectiveFailedCount),
            "model" to JsonPrimitive(model),
            "requested_count" to JsonPrimitive(requestedCount),
            "files" to JsonArray(filesList)
        )
        data["usage"] = usage?.let { value ->
            JsonObject(
                mapOf(
                    "input_tokens" to JsonPrimitive(value.input_tokens),
                    "output_tokens" to JsonPrimitive(value.output_tokens),
                    "total_tokens" to JsonPrimitive(value.total_tokens)
                )
            )
        } ?: JsonNull
        return ToolResult.Success(JsonObject(data), images = agentImages)
    }

    /** 下载 URL 图片为字节，带大小上限。 */
    protected fun downloadImageBytes(url: String): ByteArray {
        val req = Request.Builder().url(url).build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("下载图片失败 HTTP ${resp.code}: $url")
            val body = resp.body ?: throw IOException("图片响应体为空：$url")
            val contentLength = body.contentLength()
            if (contentLength > MAX_IMAGE_BYTES) {
                throw IOException("图片超过 ${MAX_IMAGE_BYTES / 1024 / 1024}MB 限制")
            }
            val initialSize = contentLength.takeIf { it in 1..MAX_IMAGE_BYTES }?.toInt() ?: DEFAULT_BUFFER_SIZE
            val output = ByteArrayOutputStream(initialSize)
            body.byteStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_IMAGE_BYTES) {
                        throw IOException("图片超过 ${MAX_IMAGE_BYTES / 1024 / 1024}MB 限制")
                    }
                    output.write(buffer, 0, read)
                }
            }
            return output.toByteArray()
        }
    }

    protected fun decodeImageBase64(base64: String): ByteArray {
        if (estimatedDecodedBytes(base64.length) > MAX_IMAGE_BYTES) {
            throw IOException("图片超过 ${MAX_IMAGE_BYTES / 1024 / 1024}MB 限制")
        }
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        if (bytes.size > MAX_IMAGE_BYTES) {
            throw IOException("图片超过 ${MAX_IMAGE_BYTES / 1024 / 1024}MB 限制")
        }
        return bytes
    }

    protected fun checkedTotalBytes(current: Long, additional: Int): Long {
        val total = current + additional
        if (total > MAX_TOTAL_IMAGE_BYTES) {
            throw IOException("本次图片总大小超过 ${MAX_TOTAL_IMAGE_BYTES / 1024 / 1024}MB 限制")
        }
        return total
    }

    protected fun createDefaultBasePath(): String =
        "$DEFAULT_OUTPUT_DIR/gen_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"

    /** 输出路径派生：去掉调用方后缀、按真实格式落盘，多张在文件名后加 _1/_2 序号。
     * 扩展名点只在最后一个路径分隔符之后找：`~/.aiverse/...` 这类路径自身的点不能当扩展名分隔符。 */
    protected fun buildTargetPath(basePath: String, index: Int, format: String): String {
        val slash = basePath.lastIndexOf('/')
        val dot = basePath.lastIndexOf('.', basePath.length - 1).takeIf { it > slash } ?: -1
        val base = if (dot > 0) basePath.substring(0, dot) else basePath
        val suffix = if (index == 0) "" else "_${index + 1}"
        return "$base$suffix.${extForFormat(format)}"
    }

    protected fun detectImageFormat(bytes: ByteArray): String {
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()
        ) return FORMAT_PNG
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        ) return FORMAT_JPEG
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) return FORMAT_WEBP
        return FORMAT_PNG
    }

    /** 从工作区读取参考图字节并做大小校验，供改图工具上传给生图服务。 */
    protected fun readReferenceImageBytes(path: String): ByteArray {
        val bytes = fileAccess.readBytes(path, MAX_IMAGE_BYTES)
        if (bytes.isEmpty()) throw IOException("参考图为空：$path")
        return bytes
    }

    /** 组装 OpenAI Images 响应（generations / edits 共用结构）的统一结果。 */
    protected suspend fun buildOpenAIImageResponseResult(
        response: ImageGenerationResponse,
        requestedN: Int,
        outputPath: String?,
        model: String
    ): ToolResult {
        if (response.data.isEmpty()) {
            return ToolResult.Error("生图服务未返回图片数据", "EMPTY_RESULT")
        }
        val effectiveBasePath = outputPath ?: createDefaultBasePath()
        val overwrite = outputPath != null

        val agentImages = mutableListOf<AgentImage>()
        val savedDisplayPaths = mutableListOf<String>()
        val filesList = mutableListOf<JsonObject>()
        var totalBytes = 0L
        var failedCount = 0
        response.data.forEachIndexed { index, item ->
            try {
                val bytes: ByteArray
                val base64: String
                when {
                    !item.b64_json.isNullOrBlank() -> {
                        base64 = item.b64_json
                        bytes = decodeImageBase64(base64)
                    }
                    !item.url.isNullOrBlank() -> {
                        bytes = downloadImageBytes(item.url)
                        base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                    else -> throw IOException("结果中缺少 base64 与 URL")
                }
                totalBytes = checkedTotalBytes(totalBytes, bytes.size)
                persistImageBytes(
                    bytes, base64, effectiveBasePath, overwrite,
                    agentImages, savedDisplayPaths, filesList
                )
            } catch (e: Exception) {
                failedCount++
                FileLogger.w(logTag, "处理第 ${index + 1} 张生图结果失败", e)
            }
        }

        if (agentImages.isEmpty()) {
            return ToolResult.Error("未能获取生成的图片内容（图片无效、过大或下载失败）", "EMPTY_RESULT")
        }
        return buildImageResult(
            agentImages, savedDisplayPaths, filesList, model, requestedN,
            outputPath, failedCount, usage = response.usage
        )
    }

    companion object {
        const val DEFAULT_OUTPUT_DIR = "~/.aiverse/generated-images"
        const val DEFAULT_SIZE = "1024x1024"
        const val MAX_IMAGES = 4
        const val MAX_IMAGE_BYTES = 20L * 1024 * 1024
        const val MAX_TOTAL_IMAGE_BYTES = 48L * 1024 * 1024
        const val FORMAT_PNG = "png"
        const val FORMAT_JPEG = "jpeg"
        const val FORMAT_WEBP = "webp"
        val QUALITY_GPT_IMAGE = setOf("low", "medium", "high", "auto")
        val QUALITY_DALLE3 = setOf("standard", "hd")
        val QUALITY_DALLE2 = setOf("standard")
        val BACKGROUNDS = setOf("transparent", "opaque", "auto")
        val MODERATIONS = setOf("low", "auto")
        val STYLES = setOf("vivid", "natural")
        val OUTPUT_FORMATS = setOf("png", "jpeg", "webp")
        private const val DEFAULT_BUFFER_SIZE = 8192

        internal fun estimatedDecodedBytes(base64Length: Int): Long = base64Length.toLong() * 3 / 4

        internal fun mimeForFormat(format: String): String = when (format) {
            FORMAT_JPEG -> "image/jpeg"
            FORMAT_WEBP -> "image/webp"
            else -> "image/png"
        }

        internal fun extForFormat(format: String): String = if (format == FORMAT_JPEG) "jpg" else format
    }
}
