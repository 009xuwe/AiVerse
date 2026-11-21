package com.aiverse.feature.launch.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aiverse.R
import com.aiverse.app.util.TextCipher
import com.aiverse.core.theme.AppThemePreset
import com.aiverse.core.theme.ChatBubbleStyle
import com.aiverse.feature.agent.presentation.component.chatBubbleBorderFor
import com.aiverse.feature.agent.presentation.component.chatBubbleColorFor
import com.aiverse.feature.agent.presentation.component.chatBubbleShapeFor
import com.aiverse.feature.agent.presentation.component.chatBubbleTextColorFor
import com.aiverse.feature.settings.data.repository.AppThemeMode
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Check
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronUp
import compose.icons.feathericons.Zap
import compose.icons.feathericons.Folder
import compose.icons.feathericons.Github
import compose.icons.feathericons.Maximize
import compose.icons.feathericons.Package
import compose.icons.feathericons.Power

/** 启动向导的页面种类。正式版 4 页，测试版多一页「从正式版导入」。 */
enum class LaunchWizardPage { AGREEMENT, PERMISSIONS, APPEARANCE, BETA_HANDOFF, STAR }

/**
 * 首次启动全屏向导（参考 Kazomi 的启动流程）：
 *
 * - 一页一屏，不再叠多个 AlertDialog；
 * - 顶部固定显示「当前页 / 总页数」小胶囊 + 等宽步骤点，总步数由页面列表长度决定，
 *   所以测试版从头到尾都是 5 个点、正式版从头到尾都是 4 个点，不会出现前三页 3 个点、
 *   第四页变 4 个点的跳变；
 * - 右下角是确认类操作（同意并继续 / 下一步 / 开始使用），左下角是退出或跳过。
 *
 * @param isBeta 当前包是否为测试版（`applicationId` 以 `.beta` 结尾）。
 * @param betaHandoffAvailable 是否检测到「本机已装正式版且测试版自身无数据」。
 * @param themeMode 当前深浅模式（改动即时生效并持久化）。
 * @param themePresetId 当前配色主题 id（null = 应用默认主题）。
 * @param bubbleStyle 当前聊天气泡样式（改动即时生效并持久化）。
 */
@Composable
fun LaunchWizard(
    isBeta: Boolean,
    betaHandoffAvailable: Boolean,
    themeMode: AppThemeMode,
    themePresetId: String?,
    bubbleStyle: ChatBubbleStyle,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onThemePresetChange: (String) -> Unit,
    onBubbleStyleChange: (ChatBubbleStyle) -> Unit,
    onImportFromRelease: () -> Unit,
    onAgree: () -> Unit,
    onExit: () -> Unit,
    onFinish: (startOnboarding: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // 页面列表只按包名决定长度：测试版 5 页、正式版 4 页，步数点一次算清、全程一致。
    val pages = remember(isBeta) {
        buildList {
            add(LaunchWizardPage.AGREEMENT)
            add(LaunchWizardPage.PERMISSIONS)
            add(LaunchWizardPage.APPEARANCE)
            if (isBeta) add(LaunchWizardPage.BETA_HANDOFF)
            add(LaunchWizardPage.STAR)
        }
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    // 最后一页点「开始使用」后先问一次要不要进入聚光灯引导，避免向导结束后立刻又叠一套。
    var showOnboardingPrompt by remember { mutableStateOf(false) }
    val current = pages[index.coerceIn(0, pages.lastIndex)]
    val isFirst = index == 0
    val isLast = index == pages.lastIndex
    // 横滑翻页的累计位移与阈值（阈值按当前密度换算，不写死像素）。
    var dragAccum by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

    fun goNext() {
        if (isFirst) onAgree()
        if (isLast) showOnboardingPrompt = true else index += 1
    }

    BackHandler {
        if (index > 0) index -= 1 else onExit()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // 顶部：产品名 + 页码 + 步骤点
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(
                            R.string.launch_wizard_step_counter,
                            index + 1,
                            pages.size
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                StepDots(total = pages.size, current = index)
            }

            AnimatedContent(
                targetState = current,
                transitionSpec = {
                    // 按翻页方向决定动画：前进时新页从右侧推入、旧页往左退；后退时反过来。
                    // 两页都用整屏宽度位移，方向一眼能看出来，不会再“分不清往左还是往右”。
                    val forward = targetState.ordinal > initialState.ordinal
                    val slide = tween<IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)
                    if (forward) {
                        (slideInHorizontally(slide) { it } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally(slide) { -it } + fadeOut(tween(180)))
                    } else {
                        (slideInHorizontally(slide) { -it } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally(slide) { it } + fadeOut(tween(180)))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(pages, index) {
                        // 左右滑动翻页：横向拖过阈值等价于点「下一步 / 上一页」；
                        // 只接管横向手势，页面内的纵向滚动不受影响。
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccum = 0f },
                            onDragEnd = {
                                when {
                                    dragAccum <= -swipeThresholdPx && !isLast -> index += 1
                                    dragAccum >= swipeThresholdPx && !isFirst -> index -= 1
                                }
                                dragAccum = 0f
                            },
                            onDragCancel = { dragAccum = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccum += dragAmount
                            }
                        )
                    },
                label = "LaunchWizardPage"
            ) { page ->
                when (page) {
                    LaunchWizardPage.AGREEMENT -> AgreementPage(isBeta = isBeta)
                    LaunchWizardPage.PERMISSIONS -> PermissionsPage()
                    LaunchWizardPage.APPEARANCE -> AppearancePage(
                        themeMode = themeMode,
                        themePresetId = themePresetId,
                        bubbleStyle = bubbleStyle,
                        onThemeModeChange = onThemeModeChange,
                        onThemePresetChange = onThemePresetChange,
                        onBubbleStyleChange = onBubbleStyleChange
                    )
                    LaunchWizardPage.BETA_HANDOFF -> BetaHandoffPage(
                        available = betaHandoffAvailable,
                        onImport = onImportFromRelease
                    )
                    LaunchWizardPage.STAR -> StarPage()
                }
            }

            // 底部：左下「退出 / 上一页」，右下「同意并继续 / 下一步 / 开始使用」。
            // 中间页不给「跳过」：整段向导必须逐页走完，只能回退，不能越页。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { if (isFirst) onExit() else index -= 1 },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (isFirst) R.string.launch_wizard_exit else R.string.launch_wizard_prev
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { goNext() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(
                            when {
                                isFirst -> R.string.launch_wizard_agree
                                isLast -> R.string.launch_wizard_start
                                else -> R.string.launch_wizard_next
                            }
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showOnboardingPrompt) {
        AlertDialog(
            onDismissRequest = { showOnboardingPrompt = false },
            title = { Text(stringResource(R.string.launch_onboarding_prompt_title)) },
            text = { Text(stringResource(R.string.launch_onboarding_prompt_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOnboardingPrompt = false
                        onFinish(true)
                    }
                ) { Text(stringResource(R.string.launch_onboarding_prompt_start)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOnboardingPrompt = false
                        onFinish(false)
                    }
                ) { Text(stringResource(R.string.launch_onboarding_prompt_skip)) }
            }
        )
    }
}

/** 顶部步骤点：总点数固定等于页面数，当前页用加长胶囊高亮。 */
@Composable
private fun StepDots(total: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total) { i ->
            val targetWidth = if (i == current) 26.dp else 8.dp
            val width by animateDpAsState(
                targetValue = targetWidth,
                animationSpec = tween(220),
                label = "dotWidth$i"
            )
            val color by animateColorAsState(
                targetValue = if (i <= current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                animationSpec = tween(220),
                label = "dotColor$i"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** 页头：标题 + 副标题，各页共用。 */
@Composable
private fun PageHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

// ───────────────────────────── 第 1 页：协议 ─────────────────────────────

@Composable
private fun AgreementPage(isBeta: Boolean) {
    val uriHandler = LocalUriHandler.current
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        )
    )
    val termsUrl = stringResource(R.string.agreement_terms_url)
    val privacyUrl = stringResource(R.string.agreement_privacy_url)
    val downloadUrl = stringResource(R.string.agreement_test_download_url)

    val agreementText = buildAnnotatedString {
        if (isBeta) {
            append(stringResource(R.string.agreement_test_notice))
            append(" ")
            withLink(LinkAnnotation.Clickable("download", linkStyle) {
                runCatching { uriHandler.openUri(downloadUrl) }
            }) { append(stringResource(R.string.agreement_test_download)) }
            append("\n\n")
        }
        append(stringResource(R.string.agreement_notice_prefix))
        withLink(LinkAnnotation.Clickable("terms", linkStyle) {
            runCatching { uriHandler.openUri(termsUrl) }
        }) { append(stringResource(R.string.agreement_terms_link)) }
        append(stringResource(R.string.agreement_notice_conjunction))
        withLink(LinkAnnotation.Clickable("privacy", linkStyle) {
            runCatching { uriHandler.openUri(privacyUrl) }
        }) { append(stringResource(R.string.agreement_privacy_link)) }
        append(stringResource(R.string.agreement_notice_suffix))
        append("\n\n")
        append(TextCipher.labelOfficial)
        append("：")
        withLink(LinkAnnotation.Clickable("official", linkStyle) {
            runCatching { uriHandler.openUri(TextCipher.officialUrl) }
        }) { append(TextCipher.officialUrl) }
        append("\n")
        append(TextCipher.labelRepo)
        append("：")
        withLink(LinkAnnotation.Clickable("repo", linkStyle) {
            runCatching { uriHandler.openUri(TextCipher.repoUrl) }
        }) { append(TextCipher.repoUrl) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        PageHeader(
            title = stringResource(R.string.launch_page_agreement_title),
            subtitle = stringResource(R.string.launch_page_agreement_subtitle)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = agreementText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.launch_agreement_footnote),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ───────────────────────────── 第 2 页：权限 ─────────────────────────────

@Composable
private fun PermissionsPage() {
    val context = LocalContext.current
    var notificationGranted by remember { mutableStateOf(notificationsGranted(context)) }
    var batteryExemptState by remember { mutableStateOf(batteryExempt(context)) }
    var storageGrantedState by remember { mutableStateOf(storageGranted(context)) }
    // 安装未知应用 / 悬浮窗：系统能准确查到状态；自启动没有官方 API，只能跳系统页手动开，
    // 这里用一个“已去过设置”的本地标记，避免一直显示成未授权。
    var installGrantedState by remember { mutableStateOf(installPackagesGranted(context)) }
    var overlayGrantedState by remember { mutableStateOf(overlayGranted(context)) }
    var autoStartVisited by remember { mutableStateOf(false) }
    // 首启只展示通知与后台两项；存储/安装/悬浮窗/自启动收进「更多权限」，避免一上来压满屏。
    var showMorePermissions by rememberSaveable { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
        if (!granted) {
            // 系统授权框不再弹出时，直接带去应用设置兜底。
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APP_NOTIFICATION_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        storageGrantedState = result.values.any { it } || storageGranted(context)
    }

    // 从系统设置页返回时刷新状态（不依赖轮询）。
    LifecycleResumeEffect(Unit) {
        notificationGranted = notificationsGranted(context)
        batteryExemptState = batteryExempt(context)
        storageGrantedState = storageGranted(context)
        installGrantedState = installPackagesGranted(context)
        overlayGrantedState = overlayGranted(context)
        onPauseOrDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        PageHeader(
            title = stringResource(R.string.launch_page_permission_title),
            subtitle = stringResource(R.string.launch_page_permission_subtitle)
        )
        PermissionGroup(
            title = stringResource(R.string.launch_permission_group_basic),
            subtitle = stringResource(R.string.launch_permission_group_basic_desc)
        ) {
        PermissionRow(
            icon = { Icon(FeatherIcons.Bell, null, tint = MaterialTheme.colorScheme.primary) },
            title = stringResource(R.string.launch_permission_notification_title),
            desc = stringResource(R.string.launch_permission_notification_desc),
            granted = notificationGranted,
            container = false,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    notificationGranted = true
                }
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        PermissionRow(
            icon = { Icon(FeatherIcons.Zap, null, tint = MaterialTheme.colorScheme.primary) },
            title = stringResource(R.string.settings_permission_battery),
            desc = stringResource(R.string.settings_permission_battery_desc),
            granted = batteryExemptState,
            container = false,
            onGrant = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        )
        }
        Spacer(modifier = Modifier.height(10.dp))
        PermissionGroup(
            title = stringResource(R.string.launch_permission_group_more),
            subtitle = stringResource(R.string.launch_permission_group_more_desc),
            expanded = showMorePermissions,
            onToggle = { showMorePermissions = !showMorePermissions }
        ) {
        if (showMorePermissions) {
        PermissionRow(
            icon = { Icon(FeatherIcons.Folder, null, tint = MaterialTheme.colorScheme.primary) },
            title = stringResource(R.string.settings_permission_storage),
            desc = stringResource(R.string.settings_permission_storage_desc),
            granted = storageGrantedState,
            container = false,
            onGrant = {
                storageLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        PermissionRow(
            icon = { Icon(FeatherIcons.Package, null, tint = MaterialTheme.colorScheme.primary) },
            title = stringResource(R.string.launch_permission_install_title),
            desc = stringResource(R.string.launch_permission_install_desc),
            granted = installGrantedState,
            container = false,
            onGrant = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        PermissionRow(
            icon = { Icon(FeatherIcons.Maximize, null, tint = MaterialTheme.colorScheme.primary) },
            title = stringResource(R.string.launch_permission_overlay_title),
            desc = stringResource(R.string.launch_permission_overlay_desc),
            granted = overlayGrantedState,
            container = false,
            onGrant = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        PermissionRow(
            icon = { Icon(FeatherIcons.Power, null, tint = MaterialTheme.colorScheme.primary) },
            title = stringResource(R.string.launch_permission_autostart_title),
            desc = stringResource(R.string.launch_permission_autostart_desc),
            granted = autoStartVisited,
            container = false,
            grantLabel = stringResource(R.string.launch_permission_autostart_action),
            grantedLabel = stringResource(R.string.launch_permission_autostart_visited),
            onGrant = {
                autoStartVisited = true
                // 自启动各家系统位置不同，入口统一落到「应用详情页」，用户能在里面找到自启动/后台限制。
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        )
        }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.launch_permission_footnote),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionGroup(
    title: String,
    subtitle: String? = null,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onToggle != null) Modifier.clickable { onToggle?.invoke() } else Modifier)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (expanded != null) {
                    Icon(
                        imageVector = if (expanded) FeatherIcons.ChevronUp else FeatherIcons.ChevronDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            content()
        }
    }
}

@Composable
private fun PermissionRow(
    icon: @Composable () -> Unit,
    title: String,
    desc: String,
    granted: Boolean,
    grantLabel: String? = null,
    grantedLabel: String? = null,
    container: Boolean = true,
    onGrant: () -> Unit
) {
    if (container) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            PermissionRowBody(
                icon = icon,
                title = title,
                desc = desc,
                granted = granted,
                grantLabel = grantLabel,
                grantedLabel = grantedLabel,
                onGrant = onGrant
            )
        }
    } else {
        PermissionRowBody(
            icon = icon,
            title = title,
            desc = desc,
            granted = granted,
            grantLabel = grantLabel,
            grantedLabel = grantedLabel,
            onGrant = onGrant
        )
    }
}

@Composable
private fun PermissionRowBody(
    icon: @Composable () -> Unit,
    title: String,
    desc: String,
    granted: Boolean,
    grantLabel: String?,
    grantedLabel: String?,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted) { onGrant() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { icon() }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (granted) {
                Icon(
                    imageVector = FeatherIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = if (granted) {
                    grantedLabel ?: stringResource(R.string.settings_permission_granted)
                } else {
                    grantLabel ?: stringResource(R.string.launch_permission_go)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (granted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

private fun notificationsGranted(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

private fun batteryExempt(context: android.content.Context): Boolean {
    val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
    return runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(false)
}

/** 安装未知来源应用：Android 8+ 走 packageManager 的精确判断。 */
private fun installPackagesGranted(context: android.content.Context): Boolean =
    runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)

/** 悬浮窗（在其他应用上层显示）。 */
private fun overlayGranted(context: android.content.Context): Boolean =
    runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)

private fun storageGranted(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_EXTERNAL_STORAGE
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

// ───────────────────────────── 第 3 页：外观 ─────────────────────────────

@Composable
private fun AppearancePage(
    themeMode: AppThemeMode,
    themePresetId: String?,
    bubbleStyle: ChatBubbleStyle,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onThemePresetChange: (String) -> Unit,
    onBubbleStyleChange: (ChatBubbleStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        PageHeader(
            title = stringResource(R.string.launch_page_appearance_title),
            subtitle = stringResource(R.string.launch_page_appearance_subtitle)
        )

        // 深浅模式：三张带缩略预览的选择卡
        SectionLabel(stringResource(R.string.launch_theme_section))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                AppThemeMode.AUTO to R.string.launch_theme_auto,
                AppThemeMode.LIGHT to R.string.theme_light,
                AppThemeMode.DARK to R.string.theme_dark
            ).forEach { (mode, labelRes) ->
                ThemeModeCard(
                    mode = mode,
                    label = stringResource(labelRes),
                    selected = themeMode == mode,
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeModeChange(mode) }
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 配色主题：在深浅模式下面再选一套主色，缩略图直接是用该主题的主色 + 页面底色画的
        SectionLabel(stringResource(R.string.launch_theme_color_section))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val darkNow = MaterialTheme.colorScheme.background.luminance() < 0.5f
            AppThemePreset.ALL_PRESETS.forEach { preset ->
                ThemePresetCard(
                    preset = preset,
                    dark = darkNow,
                    selected = preset.id == (themePresetId ?: AppThemePreset.APP_DEFAULT_PRESET_ID),
                    onClick = { onThemePresetChange(preset.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 聊天气泡：横向可滑动，每张卡都是真实样式预览
        SectionLabel(stringResource(R.string.launch_bubble_section))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ChatBubbleStyle.entries.forEach { style ->
                BubbleStyleCard(
                    style = style,
                    selected = bubbleStyle == style,
                    onClick = { onBubbleStyleChange(style) }
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.launch_bubble_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

/** 深浅模式选择卡：卡片本身就是一个小界面预览（浅/深/左右各半）。 */
@Composable
private fun ThemeModeCard(
    mode: AppThemeMode,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "themeBorder"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, borderColor.copy(alpha = if (selected) 0.9f else 0.4f))
        ) {
            when (mode) {
                AppThemeMode.LIGHT -> MiniUiPreview(dark = false)
                AppThemeMode.DARK -> MiniUiPreview(dark = true)
                AppThemeMode.AUTO -> Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) { MiniUiPreview(dark = false) }
                    Box(modifier = Modifier.weight(1f)) { MiniUiPreview(dark = true) }
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp)
        )
    }
}

/**
 * 配色主题卡：色块直接用该主题的「主色 + 页面底色」画出来（previewColors 与设置页选主题
 * 用的是同一个方法），不用手写近似色，换了主题色这里跟着变。
 */
@Composable
private fun ThemePresetCard(
    preset: AppThemePreset,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (primary, pageBg) = preset.previewColors(dark)
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "presetBorder"
    )
    Column(
        modifier = Modifier
            .width(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            shape = RoundedCornerShape(12.dp),
            color = pageBg,
            border = BorderStroke(1.dp, borderColor.copy(alpha = if (selected) 0.9f else 0.4f))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                // 主色条 + 一条弱化的次强调条 + 一个主色小按钮，示意这套配色的观感
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(primary)
                )
                Spacer(modifier = Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(primary.copy(alpha = 0.45f))
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(primary)
                )
            }
        }
        Text(
            text = stringResource(preset.nameRes),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp)
        )
    }
}

/** 小界面预览：底色 + 两条气泡 + 一条输入框，直观体现深浅。 */
@Composable
private fun MiniUiPreview(dark: Boolean) {
    val bg = if (dark) Color(0xFF14161A) else Color(0xFFF6F7F9)
    val bubbleAi = if (dark) Color(0xFF262A31) else Color(0xFFFFFFFF)
    val bubbleUser = if (dark) Color(0xFF3A6EA5) else Color(0xFF2F6BD8)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(bubbleAi)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (dark) Color(0xFF333941) else Color(0xFFE3E6EB))
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(bubbleUser)
                .align(Alignment.End)
        )
    }
}

/** 气泡样式卡：两枚真实样式气泡 + 名称。 */
@Composable
private fun BubbleStyleCard(
    style: ChatBubbleStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "bubbleBorder"
    )
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, borderColor.copy(alpha = if (selected) 0.9f else 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BubblePreviewChip(style = style, mine = false)
                BubblePreviewChip(style = style, mine = true)
            }
        }
        Text(
            text = stringResource(style.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        )
    }
}

/**
 * 单枚气泡预览：底色 / 文字色 / 描边 / 圆角 / 阴影全部复用聊天页 ChatMessageStyle 的同一套推导
 * （chatBubbleColorFor / chatBubbleTextColorFor / chatBubbleBorderFor / chatBubbleShapeFor），
 * 预览和真实气泡是同一个数据源，不再在这里手写近似色值。
 * NO_BUBBLE 在真实聊天里不套容器，这里同样只留文字、不画底色。
 */
@Composable
private fun BubblePreviewChip(style: ChatBubbleStyle, mine: Boolean) {
    val shape = chatBubbleShapeFor(style)
    val noBubble = style == ChatBubbleStyle.NO_BUBBLE
    val bubbleColor = chatBubbleColorFor(style)
    val textColor = chatBubbleTextColorFor(style)
    val shadowElevation = if (style == ChatBubbleStyle.SOFT_SHADOW) 4.dp else 0.dp
    // 白底样式（白描边 / 无气泡）在预览卡上几乎看不见，补一层极浅描边只为“看得见边界”，
    // 真实聊天里这类样式本来也没有描边，故这一层只在预览里加。
    val border = chatBubbleBorderFor(style)
        ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .height(20.dp)
                .width(52.dp)
                .then(
                    if (shadowElevation > 0.dp) {
                        Modifier.shadow(shadowElevation, shape, clip = false)
                    } else {
                        Modifier
                    }
                )
                .clip(shape)
                .background(if (noBubble) Color.Transparent else bubbleColor)
                .then(if (noBubble) Modifier else Modifier.border(border, shape)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.bubble_preview_text),
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}

// ───────────────────────────── 第 4 页（仅测试版）：从正式版导入 ─────────────────────────────

@Composable
private fun BetaHandoffPage(available: Boolean, onImport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        PageHeader(
            title = stringResource(R.string.launch_page_beta_title),
            subtitle = stringResource(R.string.launch_page_beta_subtitle)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (available) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (available) R.string.launch_beta_detected
                            else R.string.launch_beta_not_detected
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                InfoLine(
                    label = stringResource(R.string.launch_beta_package),
                    value = com.aiverse.feature.backup.data.BetaHandoffRepository.RELEASE_PACKAGE
                )
                InfoLine(
                    label = stringResource(R.string.launch_beta_scope),
                    value = stringResource(R.string.launch_beta_scope_items)
                )
                InfoLine(
                    label = stringResource(R.string.launch_beta_self_data),
                    value = stringResource(R.string.launch_beta_self_data_none)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (available) {
            Button(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.launch_beta_import),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        Text(
            text = stringResource(R.string.launch_beta_manual),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 76.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ───────────────────────────── 第 5 页 / 正式版第 4 页：点星 ─────────────────────────────

@Composable
private fun StarPage() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.launch_page_star_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.launch_page_star_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(18.dp))

        // 仓库信息卡：把这一页的留白填满，同时说明这是一个什么项目
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = FeatherIcons.Github,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "yhdmgf / AiVerse",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.launch_star_repo_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                StarPoint(stringResource(R.string.launch_star_point_1))
                StarPoint(stringResource(R.string.launch_star_point_2))
                StarPoint(stringResource(R.string.launch_star_point_3))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TextCipher.repoUrl))) }
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = FeatherIcons.Github,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.launch_star_action),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.launch_star_optional_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StarPoint(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}
