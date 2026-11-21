package com.aiverse.feature.agent.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.aiverse.R
import com.aiverse.core.ui.ImageSource
import com.aiverse.core.ui.ImageViewerRequest
import com.aiverse.core.ui.LocalImageViewer
import com.aiverse.core.ui.MARKDOWN_MAX_EDGE
import com.aiverse.core.ui.decodeSampledBitmap
import com.aiverse.core.theme.LocalUIDensityTokens
import com.aiverse.core.util.FileLogger
import com.aiverse.feature.workspace.domain.FileAccessProvider
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Markdown 图片 transformer 的 CompositionLocal。
 * 默认空实现（返回 null）不渲染图片节点，与引入本功能前的行为一致。
 */
internal val LocalMarkdownImageTransformer = staticCompositionLocalOf<ImageTransformer> { NoOpImageTransformer }

internal object NoOpImageTransformer : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData? = null
}

/** Markdown 正文内嵌图片的最大显示高度（dp）：超长图按此限高等比缩放。 */
internal val MARKDOWN_IMAGE_MAX_HEIGHT_DP = 320.dp

/**
 * 构造正文图片统一的展示 modifier：满宽、限高、图片圆角（随密度档缩放）、点按拉起全屏查看器。
 *
 * 聊天区与 Git issue 正文的图片 transformer 共用，圆角走 [LocalUIDensityTokens.imageCorner]，
 * 高度走 [MARKDOWN_IMAGE_MAX_HEIGHT_DP]，不再各自写死 dp。
 */
@Composable
private fun markdownImageModifier(request: ImageViewerRequest): Modifier {
    val density = LocalUIDensityTokens.current
    val viewer = LocalImageViewer.current
    val clickLabel = stringResource(R.string.common_image_preview)
    return remember(request, density, viewer, clickLabel) {
        Modifier
            .fillMaxWidth()
            .heightIn(max = MARKDOWN_IMAGE_MAX_HEIGHT_DP)
            .clip(RoundedCornerShape(density.imageCorner))
            .clickable(onClickLabel = clickLabel) { viewer.show(request) }
    }
}

/**
 * 远程图片（http/https）的正文渲染：直接交给 Coil 按统一加载器拉取。
 *
 * 走全局统一的图片加载器后，GIF 动图会逐帧播放、SVG 会栅格化，远端图不再像旧实现那样被当
 * 文件路径 `copyToLocal` 必然失败。加载中显示占位，点按按 [request] 拉起全屏查看器看原图。
 */
@Composable
internal fun remoteMarkdownImage(url: String, request: ImageViewerRequest): ImageData {
    val painter = rememberAsyncImagePainter(model = url)
    return ImageData(
        painter = painter,
        modifier = markdownImageModifier(request),
        contentScale = ContentScale.Fit
    )
}

/**
 * Markdown 图片渲染器：按协议分流本地与远程图片。
 *
 * - http/https 远程图（GitHub user-attachments 及其重定向目标无需鉴权）：走 [remoteMarkdownImage]，
 *   由 Coil 统一加载器渲染，支持 GIF 动图与 SVG。
 * - 本地 / 容器路径（AI 容器视角的 `~/workspace/...`、容器绝对路径、相对路径、`file://`）：
 *   经 [FileAccessProvider.copyToLocal] 落到宿主文件（远程 SSH 模式自动下载到临时文件）后再采样解码，
 *   保留远程 SSH 链路与 OOM 采样兜底。
 *
 * 正文里限高等比缩放；点一下拉起全屏查看器看原图，那边会按屏幕尺寸重新解码一份更清晰的。
 */
internal class MarkdownImageTransformer(
    private val fileAccess: FileAccessProvider
) : ImageTransformer {

    @Composable
    override fun transform(link: String): ImageData? {
        val url = link.trim()
        if (url.isEmpty()) return null
        return if (isRemoteUrl(url)) {
            val request = ImageViewerRequest(
                sources = listOf(ImageSource.Url(url)),
                title = url.trimEnd('/').substringAfterLast('/')
            )
            remoteMarkdownImage(url, request)
        } else {
            transformLocal(url)
        }
    }

    @Composable
    private fun transformLocal(link: String): ImageData? {
        // GIF / 动图 WebP / SVG 交由 Coil：动图逐帧播放、SVG 栅格化。静态位图仍走采样解码这条
        // 长截图 / 超大图防线。Coil 本地文件需宿主绝对路径，容器路径先 copyToLocal。
        return if (needsCoil(link)) {
            val model = if (link.startsWith("http://") || link.startsWith("https://")) {
                link
            } else {
                runCatching { fileAccess.copyToLocal(link).absolutePath }.getOrNull() ?: link
            }
            ImageData(
                painter = rememberAsyncImagePainter(model),
                modifier = markdownImageModifier(markdownViewerRequest(link)),
                contentScale = ContentScale.Fit
            )
        } else {
            val painter by produceState<Painter?>(initialValue = null, key1 = link) {
                value = withContext(Dispatchers.IO) { decode(link) }
            }
            val p = painter ?: return null
            val request = remember(link) { markdownViewerRequest(link) }
            // markdownImageModifier 内部已 remember(request, density, viewer, clickLabel)，直接调用即可；
            // 不能再套外层 remember，否则在非 @Composable 的 calculation lambda 里调用 @Composable 函数会报错。
            val modifier = markdownImageModifier(request)
            ImageData(painter = p, modifier = modifier, contentScale = ContentScale.Fit)
        }
    }

    private fun decode(url: String): Painter? {
        return try {
            val file = fileAccess.copyToLocal(url)
            if (!file.isFile || file.length() <= 0L) return null
            decodeSampledBitmap(file.absolutePath, MARKDOWN_MAX_EDGE)?.let { BitmapPainter(it) }
        } catch (e: Exception) {
            FileLogger.w(TAG, "markdown 本地图片解码失败: $url", e)
            null
        }
    }

    private fun isRemoteUrl(url: String): Boolean =
        url.startsWith("http://") || url.startsWith("https://")

    private companion object {
        const val TAG = "MarkdownImage"
    }
}
