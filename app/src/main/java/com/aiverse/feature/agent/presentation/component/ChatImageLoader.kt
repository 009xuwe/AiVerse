package com.aiverse.feature.agent.presentation.component

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aiverse.core.ui.ImageSource
import com.aiverse.core.ui.ImageViewerRequest
import com.aiverse.core.ui.MediaKind
import com.aiverse.core.ui.MediaSource
import com.aiverse.core.ui.MediaViewerRequest
import com.aiverse.core.ui.decodeSampledBitmap
import com.aiverse.core.ui.mediaKindOf
import com.aiverse.core.util.FileLogger
import com.aiverse.feature.agent.presentation.AgentAttachment
import com.aiverse.feature.workspace.domain.FileAccessProvider
import java.io.File
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ChatImageLoader"

/**
 * 聊天区图片加载器：把 [ImageSource] 落成位图交给全屏查看器。全项目只有这一份解码策略。
 *
 * [ImageSource.ContainerPath] 经 [FileAccessProvider.copyToLocal] 落到宿主文件 —— 本地 PRoot 模式
 * 只是路径映射、不产生拷贝；远程 SSH 模式会把文件 base64 拉回来写进临时文件。
 *
 * 解码按来源分流：远程 URL、SVG、GIF / 动图 WebP 交给统一 [ImageLoader]（其注册的解码器覆盖
 * BitmapFactory 解不了的格式）；其余本地文件与 base64 仍走 [decodeSampledBitmap]，保留 inSampleSize
 * 采样、总像素兜底与 OOM 降档重试这道长截图 / 超大图的真实防线。
 */
internal fun chatImageLoader(
    context: android.content.Context,
    imageLoader: ImageLoader,
    fileAccess: FileAccessProvider,
    maxEdge: Int,
    maxPixels: Long
): suspend (ImageSource) -> ImageBitmap? = { source ->
    withContext(Dispatchers.IO) {
        try {
            val sampled = when (source) {
                is ImageSource.LocalFile ->
                    decodeExistingFile(File(source.path), maxEdge, maxPixels)

                is ImageSource.ContainerPath ->
                    decodeExistingFile(fileAccess.copyToLocal(source.path), maxEdge, maxPixels)

                is ImageSource.Base64 ->
                    decodeSampledBitmap(Base64.getDecoder().decode(source.data), maxEdge, maxPixels)

                is ImageSource.Url -> null
            }
            sampled ?: decodeViaCoil(context, imageLoader, fileAccess, source, maxEdge)
        } catch (e: Exception) {
            FileLogger.w(TAG, "图片加载失败: $source", e)
            null
        }
    }
}

/**
 * 经统一 [ImageLoader] 解码：远程 URL 必走这条路，本地 SVG / GIF / 动图 WebP 因 BitmapFactory
 * 解不出（SVG 直接失败、动图只取首帧）也交由 Coil 的对应解码器处理。
 *
 * 注意 [ImageSource.ContainerPath] 是容器内路径（如 `~/workspace/...`），Coil 在 Android 宿主解析不了，
 * 必须先 [FileAccessProvider.copyToLocal] 落到宿主文件再交给 Coil，否则直接加载失败
 * （表现为查看器报「文件可能已被移动或删除」）。
 */
private suspend fun decodeViaCoil(
    context: android.content.Context,
    imageLoader: ImageLoader,
    fileAccess: FileAccessProvider,
    source: ImageSource,
    maxEdge: Int
): ImageBitmap? {
    if (!needsCoil(source)) return null
    val data: Any = when (source) {
        is ImageSource.LocalFile -> File(source.path)
        is ImageSource.ContainerPath -> fileAccess.copyToLocal(source.path)
        is ImageSource.Base64 -> source.data
        is ImageSource.Url -> source.url
    }
    val result = imageLoader.execute(
        ImageRequest.Builder(context)
            .data(data)
            .size(maxEdge)
            .build()
    )
    val bitmap = ((result as? SuccessResult)?.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        ?: return null
    return bitmap.asImageBitmap()
}

private fun needsCoil(source: ImageSource): Boolean = when (source) {
    is ImageSource.Base64 -> false
    is ImageSource.Url -> needsCoil(source.url)
    is ImageSource.LocalFile -> needsCoil(source.path)
    is ImageSource.ContainerPath -> needsCoil(source.path)
}

/**
 * 按路径 / URL 判断是否必须交由 Coil 解码：远程 URL、SVG，以及 BitmapFactory 解不出或只能取
 * 首帧的动图（GIF / 动图 WebP）。internal 供聊天正文图片 transformer 与查看器共用。
 */
internal fun needsCoil(pathOrUrl: String): Boolean {
    val ext = pathOrUrl.substringBefore('?').substringAfterLast('.', "").lowercase()
    return pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://") ||
        ext == "svg" || ext == "gif" || ext == "webp" || ext == "heif" || ext == "heic"
}

private fun decodeExistingFile(file: File, maxEdge: Int, maxPixels: Long): ImageBitmap? {
    if (!file.isFile || file.length() <= 0L) return null
    return decodeSampledBitmap(file.absolutePath, maxEdge, maxPixels)
}

/**
 * 待发送图片附件的查看请求。
 *
 * `localPath` 优先、base64 兜底：`decodeByteArray` 要完整 byte[] 常驻，加上本来就压在内存里的
 * base64 字符串（UTF-16 实占两倍），一张 5MB 的图瞬时要多吃十几 MB；从磁盘解码没这份开销。
 */
internal fun PendingUploadAttachment.toViewerRequest(): ImageViewerRequest = ImageViewerRequest(
    sources = listOfNotNull(
        localPath.takeIf { it.isNotBlank() }?.let { ImageSource.LocalFile(it) },
        image?.base64Data?.takeIf { it.isNotBlank() }?.let { ImageSource.Base64(it) }
    ),
    title = fileName
)

/**
 * 已发送附件的查看请求。
 *
 * 带上 `containerPath` 兜底：远程模式下 `sendFile` 记的 `localPath` 来自 `createTempFile` +
 * `deleteOnExit`，进程重启或缓存被清掉后文件就没了，这时得能退回容器路径重新拉一次。
 */
internal fun AgentAttachment.toViewerRequest(): ImageViewerRequest = ImageViewerRequest(
    sources = listOfNotNull(
        localPath.takeIf { it.isNotBlank() }?.let { ImageSource.LocalFile(it) },
        containerPath.takeIf { it.isNotBlank() }?.let { ImageSource.ContainerPath(it) }
    ),
    title = fileName
)

/**
 * Markdown 内嵌图片的查看请求：link 是 AI 视角的容器路径或远程 URL。
 *
 * http(s) 远程图包成 [ImageSource.Url]（查看器经统一加载器直接拉取，支持动图 / SVG），
 * 其余按容器路径处理（本地 PRoot 仅映射，远程 SSH 先下载到临时文件）。
 *
 * 库把 alt 直接交给 Image 当 contentDescription，不经过 transformer，所以这里拿不到它，
 * 退而取路径末段的文件名当标题。
 */
internal fun markdownViewerRequest(link: String): ImageViewerRequest {
    val url = link.trim()
    val source = if (url.startsWith("http://") || url.startsWith("https://")) {
        ImageSource.Url(url)
    } else {
        ImageSource.ContainerPath(link)
    }
    return ImageViewerRequest(
        sources = listOf(source),
        title = link.trimEnd('/').substringAfterLast('/')
    )
}

/** 已发送附件是否为应用内可播放的音视频（按文件名 + MIME 判定）。 */
internal val AgentAttachment.isMedia: Boolean
    get() = mediaKindOf(fileName, mimeType) != null

/** 已发送附件的播放请求：localPath 优先、containerPath 兜底（远程临时副本可能被清）。 */
internal fun AgentAttachment.toMediaRequest(): MediaViewerRequest = MediaViewerRequest(
    sources = listOfNotNull(
        localPath.takeIf { it.isNotBlank() }?.let { MediaSource.LocalFile(it) },
        containerPath.takeIf { it.isNotBlank() }?.let { MediaSource.ContainerPath(it) }
    ),
    title = fileName,
    kind = mediaKindOf(fileName, mimeType) ?: MediaKind.AUDIO
)

/** 待发送附件是否为应用内可播放的音视频。 */
internal val PendingUploadAttachment.isMedia: Boolean
    get() = mediaKindOf(fileName, mimeType) != null

/** 待发送附件的播放请求：localPath（已落宿主）优先、containerPath 兜底。 */
internal fun PendingUploadAttachment.toMediaRequest(): MediaViewerRequest = MediaViewerRequest(
    sources = listOfNotNull(
        localPath.takeIf { it.isNotBlank() }?.let { MediaSource.LocalFile(it) },
        containerPath.takeIf { it.isNotBlank() }?.let { MediaSource.ContainerPath(it) }
    ),
    title = fileName,
    kind = mediaKindOf(fileName, mimeType) ?: MediaKind.AUDIO
)

/**
 * 应用内媒体播放器的取源解析器：把 [MediaSource] 落成宿主 [File]。
 *
 * [MediaSource.ContainerPath] 经 [FileAccessProvider.copyToLocal] —— 本地 PRoot 模式只是路径映射、
 * 不产生拷贝；远程 SSH 模式整份下载到临时文件。本地路径失效或空文件一律返回 null（播放器显示失败占位）。
 */
internal fun mediaFileResolver(fileAccess: FileAccessProvider): suspend (MediaSource) -> File? = { source ->
    withContext(Dispatchers.IO) {
        runCatching {
            when (source) {
                is MediaSource.LocalFile -> File(source.path)
                is MediaSource.ContainerPath -> fileAccess.copyToLocal(source.path)
            }
        }.getOrNull()?.takeIf { it.isFile && it.length() > 0L }
    }
}
