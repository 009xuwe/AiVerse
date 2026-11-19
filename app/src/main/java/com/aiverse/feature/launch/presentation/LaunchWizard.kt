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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aiverse.R
import com.aiverse.app.util.TextCipher
import com.aiverse.core.theme.ChatBubbleStyle
import com.aiverse.feature.settings.data.repository.AppThemeMode
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Check
import compose.icons.feathericons.Zap
import compose.icons.feathericons.Folder
import compose.icons.feathericons.Github

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
 * @param bubbleStyle 当前聊天气泡样式（改动即时生效并持久化）。
 */
@Composable
fun LaunchWizard(
    isBeta: Boolean,
    betaHandoffAvailable: Boolean,
    themeMode: AppThemeMode,
    bubbleStyle: ChatBubbleStyle,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onBubbleStyleChange: (ChatBubbleStyle) -> Unit,
    onImportFromRelease: () -> Unit,
    onAgree: () -> Unit,
    onExit: () -> Unit,
    onFinish: () -> Unit,
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
    val current = pages[index.coerceIn(0, pages.lastIndex)]
    val isFirst = index == 0
    val isLast = index == pages.lastIndex

    fun goNext() {
        if (isFirst) onAgree()
        if (isLast) onFinish() else index += 1
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
                    (fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 8 })
                        .togetherWith(fadeOut(tween(140)) + slideOutHorizontally(tween(220)) { -it / 10 })
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "LaunchWizardPage"
            ) { page ->
                when (page) {
                    LaunchWizardPage.AGREEMENT -> AgreementPage(isBeta = isBeta)
                    LaunchWizardPage.PERMISSIONS -> PermissionsPage()
                    LaunchWizardPage.APPEARANCE -> AppearancePage(
                        themeMode = themeMode,
                        bubbleStyle = bubbleStyle,
                        onThemeModeChange = onThemeModeChange,
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
                    onClick = goNext,
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
        PermissionRow(
            icon = { Icon(FeatherIcons.Bell, null, tint = MaterialTheme.colorScheme.primary) },
            title = stringResource(R.string.launch_permission_notification_title),
            desc = stringResource(R.string.launch_permission_notification_desc),
            granted = notificationGranted,
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
        Spacer(modifier = Modifier.height(10.dp))
        PermissionRow(
            icon = { Icon(FeatherIcons.Folder, null, tint = MaterialTheme.colorScheme.primary) },
            title = stringResource(R.string.settings_permission_storage),
            desc = stringResource(R.string.settings_permission_storage_desc),
            granted = storageGrantedState,
            onGrant = {
                storageLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        )
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
private fun PermissionRow(
    icon: @Composable () -> Unit,
    title: String,
    desc: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !granted) { onGrant() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
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
                    text = stringResource(
                        if (granted) R.string.settings_permission_granted else R.string.launch_permission_go
                    ),
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
    bubbleStyle: ChatBubbleStyle,
    onThemeModeChange: (AppThemeMode) -> Unit,
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

/** 单枚气泡预览：按样式切换底色 / 边框 / 圆角 / 是否镜像。 */
@Composable
private fun BubblePreviewChip(style: ChatBubbleStyle, mine: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val shape = when (style) {
        ChatBubbleStyle.FRESH_GREEN -> RoundedCornerShape(18.dp)
        ChatBubbleStyle.MINIMAL_WHITE -> RoundedCornerShape(6.dp)
        ChatBubbleStyle.NO_BUBBLE -> RoundedCornerShape(0.dp)
        else -> RoundedCornerShape(10.dp)
    }
    val background = when (style) {
        ChatBubbleStyle.NO_BUBBLE -> Color.Transparent
        ChatBubbleStyle.DARK_MODE -> if (isDark) Color(0xFF2A2E35) else Color(0xFF22252A)
        ChatBubbleStyle.FRESH_GREEN -> Color(0xFF49B36C).copy(alpha = 0.9f)
        ChatBubbleStyle.HEARTBEAT -> Color(0xFFF2C6D0)
        ChatBubbleStyle.COMIC -> Color(0xFFFFE08A)
        ChatBubbleStyle.SOFT_SHADOW -> scheme.surfaceVariant
        ChatBubbleStyle.MINIMAL_WHITE -> Color.White
        ChatBubbleStyle.ORIGINAL -> if (mine) scheme.primary.copy(alpha = 0.85f) else scheme.surfaceVariant
        ChatBubbleStyle.CLASSIC_OUTLINE -> scheme.surface
        else -> scheme.surfaceVariant
    }
    val border = when (style) {
        ChatBubbleStyle.CLASSIC_OUTLINE -> BorderStroke(1.dp, scheme.outline.copy(alpha = 0.55f))
        ChatBubbleStyle.COMIC -> BorderStroke(2.dp, Color(0xFF2B2B2B))
        ChatBubbleStyle.HEARTBEAT -> BorderStroke(1.dp, Color(0xFFE08CA8))
        else -> null
    }
    val textColor = when (style) {
        ChatBubbleStyle.DARK_MODE, ChatBubbleStyle.FRESH_GREEN, ChatBubbleStyle.ORIGINAL -> Color.White
        ChatBubbleStyle.COMIC -> Color(0xFF2B2B2B)
        else -> scheme.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .height(20.dp)
                .width(52.dp)
                .then(
                    if (style == ChatBubbleStyle.SOFT_SHADOW && !isDark) {
                        Modifier.shadow(3.dp, shape)
                    } else {
                        Modifier
                    }
                )
                .clip(shape)
                .background(background)
                .then(if (border != null) Modifier.border(border, shape) else Modifier),
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
