package com.aiverse.feature.launch.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiverse.core.theme.ChatBubbleStyle
import com.aiverse.feature.agreement.data.AgreementRepository
import com.aiverse.feature.backup.data.BetaHandoffRepository
import com.aiverse.feature.backup.presentation.launchReleaseAuthGrant
import com.aiverse.feature.launch.data.LaunchWizardRepository
import com.aiverse.feature.settings.data.repository.AppThemeMode
import com.aiverse.feature.settings.data.repository.ThemeSettingsRepository
import kotlinx.coroutines.launch

/**
 * 启动闸门：取代以前「协议 AlertDialog + 测试版导入弹窗 + Star 弹窗」三个窗口叠在一起的写法。
 *
 * - 首次启动（或没走完向导）：整屏只展示 [LaunchWizard]，背后不渲染任何页面，不会出现
 *   「弹窗一层压一层、背景一黑再黑」；
 * - 向导走完（点「开始使用」）才写入完成标记，之后启动直接进 [content]；
 * - 协议同意仍在第 1 页点「同意并继续」时写入 [AgreementRepository]，保留法律意义上的同意记录。
 */
@Composable
fun LaunchGate(
    launchRepository: LaunchWizardRepository,
    agreementRepository: AgreementRepository,
    themeSettings: ThemeSettingsRepository,
    betaHandoffRepository: BetaHandoffRepository,
    onExit: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var completed by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        completed = launchRepository.isCompleted()
    }

    val themeMode by themeSettings.themeModeFlow.collectAsStateWithLifecycle(
        initialValue = AppThemeMode.AUTO
    )
    val bubbleStyle by themeSettings.userBubbleStyleFlow.collectAsStateWithLifecycle(
        initialValue = ChatBubbleStyle.CLASSIC_OUTLINE
    )
    val themePresetId by themeSettings.themePresetIdFlow.collectAsStateWithLifecycle(
        initialValue = null as String?
    )

    // 测试版判定只看包名（applicationId 以 .beta 结尾），与 BetaHandoffRepository 同一口径。
    val isBeta = remember(context) { betaHandoffRepository.isBetaPackage() }
    val betaHandoffAvailable = remember(isBeta) {
        isBeta && betaHandoffRepository.isReleaseInstalled() && betaHandoffRepository.hasNoData()
    }

    when (completed) {
        // 读取完成标记的极短窗口：只画一层底色，避免闪一下欢迎页再闪回主界面。
        null -> Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {}
        true -> content()
        false -> LaunchWizard(
            isBeta = isBeta,
            betaHandoffAvailable = betaHandoffAvailable,
            themeMode = themeMode,
            themePresetId = themePresetId,
            bubbleStyle = bubbleStyle,
            onThemeModeChange = { mode -> scope.launch { themeSettings.setThemeMode(mode) } },
            onThemePresetChange = { id -> scope.launch { themeSettings.setThemePresetId(id) } },
            onBubbleStyleChange = { style -> scope.launch { themeSettings.setBubbleStyle(style) } },
            onImportFromRelease = {
                // 复用原有 deep link：拉起正式版的授权页，由它把数据写回测试版。
                scope.launch { betaHandoffRepository.markPromptDismissed() }
                launchReleaseAuthGrant(context)
            },
            onExit = onExit,
            onAgree = { scope.launch { agreementRepository.accept() } },
            onFinish = {
                scope.launch {
                    agreementRepository.accept()
                    launchRepository.markCompleted()
                    completed = true
                }
            }
        )
    }
}
