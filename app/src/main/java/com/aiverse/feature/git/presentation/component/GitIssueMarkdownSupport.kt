package com.aiverse.feature.git.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aiverse.core.ui.ImageSource
import com.aiverse.core.ui.ImageViewerRequest
import com.aiverse.feature.agent.presentation.component.LocalMarkdownImageTransformer
import com.aiverse.feature.agent.presentation.component.MarkdownContent
import com.aiverse.feature.agent.presentation.component.remoteMarkdownImage
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer

/**
 * GitHub issue 正文 / 评论的 Markdown 渲染。
 *
 * 复用聊天区的 [MarkdownContent] 排版与代码高亮，只替换图片处理：
 * 聊天流为「不渲染 HTML」会把 `<` 用零宽空格中和，导致 issue 正文里 GitHub 常见的
 * 原始 `<img src="https://github.com/user-attachments/assets/...">` 显示成字面源码；
 * 这里先把 `<img>` 还原成 `![alt](src)` Markdown 图片语法，再交给渲染器，并由
 * [GitIssueImageTransformer] 用 Coil 把远程图片显示出来（点按拉起全屏查看器）。
 *
 * @param color 正文文字色（与 MarkdownContent 的 text 色一致）。
 */
@Composable
internal fun GitIssueMarkdown(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val processed = remember(text) { preprocessGitIssueHtml(text) }
    CompositionLocalProvider(
        LocalMarkdownImageTransformer provides GitIssueImageTransformer
    ) {
        MarkdownContent(
            text = processed,
            color = color,
            modifier = modifier
        )
    }
}

/**
 * 把 issue 正文/评论里的原始 `<img ...>` HTML 标签转成 Markdown 图片语法。
 *
 * 只处理 `<img>` 标签（含大小写、单双引号、任意属性顺序）；无 src 或解析失败原样保留。
 * 其余 HTML（`<br>`、`<details>` 等）保持原样，由 [MarkdownContent] 内部的预处理按字面展示，
 * 与聊天流一致，不影响图片。
 */
internal fun preprocessGitIssueHtml(text: String): String {
    if (!text.contains('<')) return text
    return IMG_TAG_REGEX.replace(text) { m ->
        val tag = m.value
        val src = SRC_ATTR_REGEX.find(tag)?.groupValues?.get(1)
        if (src.isNullOrBlank()) {
            m.value
        } else {
            val alt = ALT_ATTR_REGEX.find(tag)?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: "Image"
            "![$alt]($src)"
        }
    }
}

private val IMG_TAG_REGEX = Regex("""<img\b[^>]*?>""", RegexOption.IGNORE_CASE)
private val SRC_ATTR_REGEX = Regex("""src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val ALT_ATTR_REGEX = Regex("""alt\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)

/**
 * Git issue 正文的 Markdown 图片 transformer。
 *
 * 只渲染 http/https 远程图片（GitHub user-attachments 及其重定向目标无需鉴权，直接 Coil 加载）；
 * 其它（本地路径、自定义 scheme）一律返回 null 不渲染 —— issue 正文一般只有远程图片。
 *
 * 远程渲染复用聊天区 [remoteMarkdownImage]：同一套限高 / 圆角 modifier、同一个统一 Coil 加载器
 * （GIF 动图逐帧播放、SVG 栅格化），点按拉起全屏查看器看原图。
 */
private object GitIssueImageTransformer : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData? {
        val url = link.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        return remoteMarkdownImage(
            url,
            ImageViewerRequest(
                sources = listOf(ImageSource.Url(url)),
                title = url.trimEnd('/').substringAfterLast('/')
            )
        )
    }
}
