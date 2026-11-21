package com.aiverse.feature.git.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Plus
import androidx.compose.ui.text.style.TextOverflow
import com.aiverse.R
import com.aiverse.core.theme.Radius
import com.aiverse.core.theme.Spacing
import com.aiverse.core.theme.semanticColors
import com.aiverse.core.ui.floatingTabBarContentBottomPadding
import com.aiverse.feature.git.domain.model.GitHubCheckSummary
import com.aiverse.feature.git.domain.model.GitHubPullRequest
import com.aiverse.feature.git.domain.model.sortPullRequests

/** PR 状态色：与 GitHub 观感一致。 */
private val PullOpenColor = Color(0xFF2DA44E)
private val PullMergedColor = Color(0xFF8250DF)
private val PullClosedColor = Color(0xFFCF222E)
private val PullDraftColor = Color(0xFF6E7781)

/** CI 汇总色。 */
internal val CiSuccessColor = Color(0xFF2DA44E)
internal val CiFailureColor = Color(0xFFCF222E)
internal val CiPendingColor = Color(0xFFBF8700)

/**
 * Pull Request 列表 tab：顶部 open/closed/all 过滤 + 排序，下方 PR 列表。
 * 列表只为前若干条拉 CI 汇总（见 GitViewModel.PR_CHECKS_ON_LIST），未拉到的行不显示徽章。
 * 非 GitHub 仓库或无 token 时展示对应提示态。
 */
@Composable
internal fun PullRequestsTab(
    pulls: List<GitHubPullRequest>,
    checks: Map<Long, GitHubCheckSummary>,
    loading: Boolean,
    loaded: Boolean,
    filter: String,
    isGitHub: Boolean,
    hasToken: Boolean,
    error: String?,
    remoteUrl: String,
    listState: LazyListState,
    sort: String = "newest",
    onLoad: () -> Unit = {},
    onFilterChange: (String) -> Unit,
    onOpenPull: (Long) -> Unit,
    onSortChange: (String) -> Unit = {},
    onCreatePull: () -> Unit = {}
) {
    LaunchedEffect(remoteUrl) {
        if (isGitHub && hasToken && !loaded && !loading) onLoad()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isGitHub && hasToken) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                PrFilterChip(
                    selected = filter == "open",
                    label = stringResource(R.string.git_pulls_open),
                    onClick = { onFilterChange("open") }
                )
                PrFilterChip(
                    selected = filter == "closed",
                    label = stringResource(R.string.git_pulls_closed),
                    onClick = { onFilterChange("closed") }
                )
                PrFilterChip(
                    selected = filter == "all",
                    label = stringResource(R.string.git_pulls_all),
                    onClick = { onFilterChange("all") }
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onCreatePull, modifier = Modifier.size(28.dp)) {
                    Icon(
                        FeatherIcons.Plus,
                        contentDescription = stringResource(R.string.git_pr_create_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                PrSortDropdown(current = sort, onSelect = onSortChange)
            }
        }

        when {
            !isGitHub -> PrEmptyState(stringResource(R.string.git_pulls_not_github))
            !hasToken -> PrEmptyState(stringResource(R.string.git_pulls_need_token))
            loading -> PrLoading()
            error != null -> PrError(onRetry = onLoad)
            pulls.isEmpty() -> PrEmptyState(stringResource(R.string.git_no_pulls))
            else -> {
                val sorted = remember(pulls, sort) { sortPullRequests(pulls, sort) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = Spacing.lg,
                        end = Spacing.lg,
                        bottom = floatingTabBarContentBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(sorted, key = { it.number }) { pr ->
                        PullRequestRow(
                            pr = pr,
                            checks = checks[pr.number],
                            onOpen = { onOpenPull(pr.number) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrFilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.semanticColors.cardSurface,
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.semanticColors.subtleBorder,
            selectedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun PrSortDropdown(current: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = stringResource(R.string.git_issue_sort_label),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PrSortMenuItem("newest", R.string.git_issue_sort_newest, current, onSelect) { expanded = false }
            PrSortMenuItem("oldest", R.string.git_issue_sort_oldest, current, onSelect) { expanded = false }
            PrSortMenuItem("updated", R.string.git_issue_sort_updated, current, onSelect) { expanded = false }
        }
    }
}

@Composable
private fun PrSortMenuItem(
    value: String,
    labelRes: Int,
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(labelRes),
                color = if (current == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        },
        onClick = {
            onDismiss()
            onSelect(value)
        }
    )
}

@Composable
private fun PrLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(R.string.git_pulls_loading),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrError(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.git_pulls_load_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.sm))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
        }
    }
}

@Composable
private fun PrEmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PullRequestRow(
    pr: GitHubPullRequest,
    checks: GitHubCheckSummary?,
    onOpen: () -> Unit
) {
    Surface(
        color = MaterialTheme.semanticColors.cardSurface,
        shape = RoundedCornerShape(Radius.md),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.Top) {
                PullStateIcon(
                    state = pr.displayState,
                    draft = pr.draft,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                if (pr.userLogin.isNotEmpty()) {
                    IssueAvatar(
                        name = pr.userLogin,
                        size = 18.dp,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(Spacing.sm))
                }
                Text(
                    text = pr.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = "#${pr.number}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "${pr.userLogin} • ${formatIssueDate(pr.createdAt)} • ${pr.head.ref} → ${pr.base.ref}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (pr.draft || (checks != null && checks.state != "none")) {
                Spacer(Modifier.height(Spacing.xs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    if (pr.draft) {
                        PrBadge(
                            text = stringResource(R.string.git_pr_draft),
                            color = PullDraftColor
                        )
                    }
                    ChecksBadge(summary = checks)
                }
            }
            if (pr.labels.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    pr.labels.take(4).forEach { label -> LabelChip(label) }
                }
            }
        }
    }
}

/** PR 状态图标：open 圆环+圆心、merged 紫色实心圆+对勾、closed 红色实心圆+叉、draft 灰色圆环。 */
@Composable
internal fun PullStateIcon(
    state: String,
    draft: Boolean,
    modifier: Modifier = Modifier
) {
    val color = when {
        draft -> PullDraftColor
        state == "merged" -> PullMergedColor
        state == "closed" -> PullClosedColor
        else -> PullOpenColor
    }
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.12f
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (size.minDimension - stroke) / 2f
        when {
            draft -> drawCircle(color, radius, center, style = Stroke(width = stroke))
            state == "merged" -> {
                drawCircle(color, radius)
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.28f, size.height * 0.5f),
                    end = Offset(size.width * 0.44f, size.height * 0.68f),
                    strokeWidth = stroke * 1.2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.44f, size.height * 0.68f),
                    end = Offset(size.width * 0.72f, size.height * 0.34f),
                    strokeWidth = stroke * 1.2f,
                    cap = StrokeCap.Round
                )
            }
            state == "closed" -> {
                drawCircle(color, radius)
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.33f, size.height * 0.33f),
                    end = Offset(size.width * 0.67f, size.height * 0.67f),
                    strokeWidth = stroke * 1.2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.67f, size.height * 0.33f),
                    end = Offset(size.width * 0.33f, size.height * 0.67f),
                    strokeWidth = stroke * 1.2f,
                    cap = StrokeCap.Round
                )
            }
            else -> {
                drawCircle(color, radius, center, style = Stroke(width = stroke))
                drawCircle(color, radius * 0.4f, center)
            }
        }
    }
}

/** CI 汇总徽章；没有检查（null / none）时不渲染。 */
@Composable
internal fun ChecksBadge(summary: GitHubCheckSummary?, modifier: Modifier = Modifier) {
    if (summary == null || summary.state == "none") return
    val (labelRes, color) = when (summary.state) {
        "failure" -> R.string.git_pr_ci_failure to CiFailureColor
        "pending" -> R.string.git_pr_ci_pending to CiPendingColor
        else -> R.string.git_pr_ci_success to CiSuccessColor
    }
    PrBadge(text = stringResource(labelRes), color = color, modifier = modifier)
}

@Composable
private fun PrBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(Radius.pill),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs / 2f)
        )
    }
}

/** 供详情页复用：CI 检查项的短状态文案。 */
@Composable
internal fun checkOutcomeLabel(outcome: String): String = when (outcome) {
    "failure" -> stringResource(R.string.git_pr_ci_failure)
    "pending" -> stringResource(R.string.git_pr_ci_pending)
    else -> stringResource(R.string.git_pr_ci_success)
}

/** 供详情页复用：CI 检查项的状态色。 */
internal fun checkOutcomeColor(outcome: String): Color = when (outcome) {
    "failure" -> CiFailureColor
    "pending" -> CiPendingColor
    else -> CiSuccessColor
}
