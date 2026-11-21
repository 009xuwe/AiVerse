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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aiverse.R
import com.aiverse.core.theme.Spacing
import com.aiverse.core.ui.AdaptiveModalBottomSheet

/** 新建 GitHub issue 弹层：标题、正文、标签与受理人（后两者逗号分隔）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GitIssueCreateSheet(
    busy: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onCreate: (title: String, body: String, labels: List<String>, assignees: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var labels by remember { mutableStateOf("") }
    var assignees by remember { mutableStateOf("") }

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
                text = stringResource(R.string.git_issue_create_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.git_issue_create_name)) },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text(stringResource(R.string.git_issue_create_body)) },
                enabled = !busy,
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = labels,
                onValueChange = { labels = it },
                label = { Text(stringResource(R.string.git_issue_create_labels)) },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = assignees,
                onValueChange = { assignees = it },
                label = { Text(stringResource(R.string.git_issue_create_assignees)) },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
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
                        onCreate(
                            title.trim(),
                            body.trim(),
                            labels.split(',', '，').map { it.trim() }.filter { it.isNotBlank() },
                            assignees.split(',', '，').map { it.trim() }.filter { it.isNotBlank() }
                        )
                    },
                    enabled = !busy && title.isNotBlank()
                ) {
                    Text(stringResource(R.string.git_issue_create_submit))
                }
            }
        }
    }
}
