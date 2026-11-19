package com.aiverse.feature.onboarding.presentation

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.aiverse.feature.onboarding.domain.OnboardingStep

/**
 * 引导目标组件注册表：记录各步骤高亮组件在 Root 坐标系中的边界矩形。
 *
 * 同一步骤允许注册多个候选（例如第 1 步既可能是顶栏「···」按钮，也可能是展开后的「终端」菜单项），
 * 用 [priority] 区分：始终取当前已注册且优先级最高的那个，低优先级作为兜底。
 * 这样「菜单收起 / 展开」两种形态都能正确挖孔，不会出现整屏只剩半透明遮罩、用户只能手动点下一步的情况。
 */
class OnboardingTargetRegistry {
    private data class Key(val step: OnboardingStep, val priority: Int)

    private val _targets = mutableStateMapOf<Key, Rect>()

    fun register(step: OnboardingStep, priority: Int, rect: Rect) {
        _targets[Key(step, priority)] = rect
    }

    fun unregister(step: OnboardingStep, priority: Int) {
        _targets.remove(Key(step, priority))
    }

    /** 当前步骤可见的聚焦区域：优先级最高的已注册候选。 */
    fun targetFor(step: OnboardingStep): Rect? =
        _targets.entries
            .filter { it.key.step == step }
            .maxByOrNull { it.key.priority }
            ?.value
}

/**
 * 全局 CompositionLocal，便于深层组件无感知上报高亮位置。
 */
val LocalOnboardingTargetRegistry = compositionLocalOf<OnboardingTargetRegistry?> { null }

/**
 * 快捷 Modifier：为目标组件打标并在布局完成后自动上报坐标边界。
 *
 * @param priority 候选优先级，数值越大越优先（默认 0）。
 */
fun Modifier.onboardingTarget(step: OnboardingStep, priority: Int = 0): Modifier = composed {
    val registry = LocalOnboardingTargetRegistry.current
    if (registry == null) {
        this
    } else {
        DisposableEffect(step, priority) {
            onDispose {
                registry.unregister(step, priority)
            }
        }
        this.onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                registry.register(step, priority, coordinates.boundsInRoot())
            }
        }
    }
}
