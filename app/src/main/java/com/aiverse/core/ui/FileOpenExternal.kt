package com.aiverse.core.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.aiverse.R
import com.aiverse.core.util.shareableMimeType
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 用系统应用打开一个宿主本地文件（issue #176：Office 文档的「用其他应用打开」出口）。
 *
 * 「用系统对应 app 打开文件」能力的 core 层落点，聊天附件（`openSentAttachment`）与文件树长按菜单
 * 共用同一份：APK 安装、失效路径重取、文件缺失提示这些调用方语义留在外面，这里只负责
 * 「拿到可授权的 URI → ACTION_VIEW 交给系统」这一段。
 *
 * 文件所在位置不在 FileProvider 声明的目录树内时（外部本地目录工作区、自定义挂载源、SD 卡等）
 * 无法直接授权，会先复制进 cacheDir（已由 cache-path 覆盖）再分享。
 *
 * @param mimeType 期望的 MIME；空串或 `application/octet-stream` 视为「不知道」，按 [file] 的
 *                 扩展名再查一次，避免系统应用因类型不明而拒绝打开。
 */
suspend fun openFileExternal(context: Context, file: File, mimeType: String) {
    val shareable = withContext(Dispatchers.IO) {
        if (isShareableFile(context, file)) file else copyToShareCache(context, file)
    }
    val uri = shareable?.let { shareProviderUri(context, it) }
    if (uri == null) {
        Toast.makeText(context, context.getString(R.string.chat_open_file_unshareable), Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, shareableMimeType(mimeType, file.name))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.chat_open_file_no_app), Toast.LENGTH_SHORT).show()
    }
}

/** 取 FileProvider 授权 URI；路径不在声明的目录树内时抛异常，故按能否取到判定。 */
private fun shareProviderUri(context: Context, file: File) =
    runCatching { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) }.getOrNull()

/** 路径落在 FileProvider 声明的目录树内时可直接授权，无需复制。 */
internal fun isShareableFile(context: Context, file: File): Boolean =
    runCatching { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) }.isSuccess

/**
 * 把无法直接授权的文件（外部本地目录工作区、自定义挂载源、SD 卡等）复制进 cacheDir 再分享。
 * 目标目录按源路径散列分隔：既避开同名文件互相覆盖，也不把散列值挂进分享出去的文件名。
 */
internal fun copyToShareCache(context: Context, source: File): File? = runCatching {
    val dir = File(File(context.cacheDir, SHARE_CACHE_DIR), source.absolutePath.hashCode().toString())
    dir.mkdirs()
    val target = File(dir, source.name.ifBlank { "file" })
    source.copyTo(target, overwrite = true)
    target
}.getOrNull()

private const val SHARE_CACHE_DIR = "sent_files"
