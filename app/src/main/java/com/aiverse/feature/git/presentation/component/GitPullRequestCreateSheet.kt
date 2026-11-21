package com.aiverse.feature.git.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aiverse.R
import com.aiverse.core.theme.Spacing
import com.aiverse.core.ui.AdaptiveModalBottomSheet
import com.aiverse.core.ui.AppSwitch

/**
 * 新建 PR 弹层：两种模式——基于已有分支创建，或把当前工作区改动提交到新分支后创建。
 * 所有写操作由 ViewModel 执行；这里只收集表单并展示失败原因。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GitPullRequestCreateSheet(
    busy: Boolean,
    error: String?,
    defaultHead: String,
    defaultBase: String,
    canSubmitWorktree: Boolean,
    onDismiss: () -> Unit,
    onCreateFromBranch: (title: String, body: String, head: String, base: String, draft: Boolean) -> Unit,
    onCreateFromWorktree: (
        title: String,
        body: String,
        newBranch: String,
        base: String,
        commitMessage: String,
        draft: Boolean
    ) -> Unit
) {
    var mode by remember { mutableStateOf("branch") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var head by remember { mutableStateOf(defaultHead) }
    var base by remember { mutableStateOf(defaultBase) }
    var newBranch by remember { mutableStateOf("") }
    var commitMessage by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(false) }

    val branchMode = mode == "branch"
    val sourceValid = if (branchMode) head.isNotBlank() && head != base else canSubmitWorktree
    val canSubmit = !busy && title.isNotBlank() && base.isNotBlank() && sourceValid

    AdaptiveModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.git_pr_create_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(
                    selected = branchMode,
                    onClick = { mode = "branch" },
                    label = { Text(stringResource(R.string.git_pr_create_mode_branch)) }
                )
                FilterChip(
                    selected = !branchMode,
                    onClick = { mode = "worktree" },
                    label = { Text(stringResource(R.string.git_pr_create_mode_worktree)) }
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.git_pr_create_name)) },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text(stringResource(R.string.git_pr_create_desc)) },
                enabled = !busy,
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = base,
                onValueChange = { base = it },
                label = { Text(stringResource(R.string.git_pr_create_base)) },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
            if (branchMode) {
                OutlinedTextField(
                    value = head,
                    onValueChange = { head = it },
                    label = { Text(stringResource(R.string.git_pr_create_head)) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = newBranch,
                    onValueChange = { newBranch = it },
                    label = { Text(stringResource(R.string.git_pr_create_new_branch)) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = { Text(stringResource(R.string.git_pr_create_commit_message)) },
                    enabled = !busy,
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!canSubmitWorktree) {
                    Text(
                        text = stringResource(R.string.git_pr_create_worktree_unavailable),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.git_pr_create_draft),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AppSwitch(
                    checked = draft,
                    onCheckedChange = { draft = it },
                    enabled = !busy,
                    modifier = Modifier.padding(start = Spacing.sm)
                )
            }
            if (!error.isNullOrBlank()) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss, enabled = !busy) {
                    Text(stringResource(R.string.common_cancel))
                }
                TextButton(
                    onClick = {
                        if (branchMode) {
                            onCreateFromBranch(title.trim(), body.trim(), head.trim(), base.trim(), draft)
                        } else {
                            onCreateFromWorktree(
                                title.trim(),
                                body.trim(),
                                newBranch.trim(),
                                base.trim(),
                                commitMessage.trim(),
                                draft
                            )
                        }
                    },
                    enabled = canSubmit
                ) {
                    Text(stringResource(R.string.git_pr_create_submit))
                }
            }
        }
    }
}
