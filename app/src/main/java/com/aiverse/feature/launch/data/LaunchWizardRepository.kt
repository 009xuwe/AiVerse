package com.aiverse.feature.launch.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.aiverse.core.datastore.preferencesCorruptionHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.launchWizardDataStore by preferencesDataStore(
    name = "launch_wizard_prefs",
    corruptionHandler = preferencesCorruptionHandler
)

/**
 * 启动向导（欢迎页）的完成状态。
 *
 * 以前启动时会连续弹三个窗口（协议 AlertDialog、GitHub Star 弹窗、测试版导入弹窗），
 * 遮罩一层压一层。现在统一在 [com.aiverse.feature.launch.presentation.LaunchWizard] 里
 * 用一页一屏的向导走完，只在全部走完后写一个 completed 标记，之后启动直接进主界面。
 */
@Singleton
class LaunchWizardRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        val COMPLETED_KEY = booleanPreferencesKey("launch_wizard_completed")
    }

    val completedFlow: Flow<Boolean> =
        context.launchWizardDataStore.data.map { it[COMPLETED_KEY] ?: false }

    /** 一次读取，供启动判定用。 */
    suspend fun isCompleted(): Boolean = completedFlow.first()

    suspend fun markCompleted() {
        context.launchWizardDataStore.edit { it[COMPLETED_KEY] = true }
    }

    /** 重置（调试 / 设置页「重新运行欢迎向导」用）。 */
    suspend fun reset() {
        context.launchWizardDataStore.edit { it[COMPLETED_KEY] = false }
    }
}
