package com.aiverse.feature.plugins.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.aiverse.R
import com.aiverse.core.theme.LocalUIDensityTokens
import com.aiverse.core.theme.Spacing
import com.aiverse.core.ui.PressableSurface
import com.aiverse.feature.plugins.domain.InstalledPlugin
import com.aiverse.feature.plugins.domain.newmodel.PluginPage
import com.aiverse.feature.plugins.domain.newmodel.PluginUiEntry

/** 空态建议区最多展示的插件入口数：避免多个插件把不滚动的空态撑出屏幕。 */
private const val MAX_WELCOME_ENTRIES = 4

/**
 * 聊天页可用的插件槽位入口。只取启用插件在当前槽位声明的入口，按插件与本地 id 稳定排序。
 * 数据型槽位（backup.data）不会出现在这两个槽位。
 */
internal fun chatSlotEntries(plugins: List<InstalledPlugin>, slot: String): List<PluginUiEntry> =
    plugins.asSequence()
        .filter { it.isActive }
        .flatMap { plugin -> plugin.content?.uiEntries.orEmpty().asSequence() }
        .filter { it.slot == slot }
        .distinctBy { it.key }
        .sortedWith(compareBy({ it.pluginId }, { it.localId }))
        .toList()

/** 顶栏「···」菜单里的插件入口；由调用方放在 DropdownMenu 内。 */
@Composable
internal fun PluginChatHeaderMenuItems(
    entries: List<PluginUiEntry>,
    languageTag: String,
    onOpenPage: (PluginPage) -> Unit,
    onRunAction: (PluginUiEntry) -> Unit,
    onDismiss: () -> Unit = {}
) {
    entries.forEach { entry ->
        DropdownMenuItem(
            text = {
                Text(
                    text = entry.title.resolve(languageTag),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = pluginPageIcon(entry.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = {
                onDismiss()
                val page = entry.page
                if (page != null) onOpenPage(page) else onRunAction(entry)
            }
        )
    }
}

/** 空态建议区里的插件入口卡片；样式与「新对话快捷入口」保持同一套圆角与底色。 */
@Composable
internal fun PluginChatWelcomeEntries(
    entries: List<PluginUiEntry>,
    languageTag: String,
    onOpenPage: (PluginPage) -> Unit,
    onRunAction: (PluginUiEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return
    val visibleEntries = entries.take(MAX_WELCOME_ENTRIES)
    val t = LocalUIDensityTokens.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = stringResource(R.string.plugins_slot_welcome_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        visibleEntries.forEach { entry ->
            val page = entry.page
            PressableSurface(
                onClick = { if (page != null) onOpenPage(page) else onRunAction(entry) },
                shape = RoundedCornerShape(t.cardCorner),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentPadding = PaddingValues(Spacing.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = pluginPageIcon(entry.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(t.scale(20))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title.resolve(languageTag),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.plugins_ext_entry_subtitle, entry.pluginId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** 聊天页打开插件 HTML 页面：复用设置页同一套 WebView 与桥，并带上当前会话 id。 */
@Composable
internal fun PluginChatPageHost(
    page: PluginPage,
    pluginsViewModel: PluginsViewModel,
    sessionId: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    PluginPageHost(
        page = page,
        languageTag = currentLanguageTag(),
        pluginsViewModel = pluginsViewModel,
        onClose = onClose,
        modifier = modifier,
        context = { PluginEntryContext(sessionId = sessionId) }
    )
}

/**
 * 编辑器选中文本后的浮动插件菜单（editor.selection.menu）：只接受动作入口。
 * 悬浮在编辑区上方，卡片 / 图标样式与空态建议区保持同一套圆角与底色。
 */
@Composable
internal fun PluginEditorSelectionBar(
    entries: List<PluginUiEntry>,
    languageTag: String,
    onRunAction: (PluginUiEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return
    val t = LocalUIDensityTokens.current
    Surface(
        shape = RoundedCornerShape(t.cardCorner),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = stringResource(R.string.plugins_slot_welcome_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.xs)
            )
            entries.forEach { entry ->
                PressableSurface(
                    onClick = { onRunAction(entry) },
                    shape = RoundedCornerShape(t.cardCorner),
                    color = MaterialTheme.colorScheme.surface,
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            imageVector = pluginPageIcon(entry.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(t.scale(20))
                        )
                        Text(
                            text = entry.title.resolve(languageTag),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** 动作入口声明 confirm=true 时的确认框。 */
@Composable
internal fun PluginActionConfirmDialog(
    entry: PluginUiEntry,
    languageTag: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plugins_slot_action_confirm_title)) },
        text = {
            Text(
                stringResource(
                    R.string.plugins_slot_action_confirm_message,
                    entry.title.resolve(languageTag)
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
