package com.aiverse.feature.plugins.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * 插件「待升级」提示的持有者：App 启动时静默检查一次，把结果暴露给界面。
 *
 * 进程级单例：启动检查在任意页面都能触发一次，结果对整个 App 可见，
 * 与 APP 自身的更新检查（UpdateCheckService / SettingsViewModel）互不干扰。
 *
 * 另外订阅 [PluginManager.plugins]：安装 / 升级 / 卸载后已装版本变了会重算一次，
 * 否则「全部更新」做完回到首页，角标还挂着刚才那批可更新插件，要重启才消失。
 */
@Singleton
class PluginUpdateNotifier @Inject constructor(
    private val checker: PluginUpdateChecker,
    private val manager: PluginManager
) {

    private val _pending = MutableStateFlow<List<PendingPluginUpdate>>(emptyList())
    val pending: StateFlow<List<PendingPluginUpdate>> = _pending.asStateFlow()

    /** 当前待升级插件数，供角标/入口展示。 */
    val count: Int get() = _pending.value.size

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // 首个 emission 是启动时已有的状态，由外部启动检查负责，这里跳过。
        // 之后每次变化延迟 1s 再算：批量更新会连续发很多次，合并成一次即可。
        scope.launch {
            manager.plugins.drop(1).collect {
                delay(1000L)
                refresh()
            }
        }
    }

    /**
     * 后台静默刷新待升级列表。任何异常都吞掉，保持空值或上一次结果不变，
     * 绝不让启动路径因检查失败而中断。
     */
    suspend fun refresh() {
        try {
            _pending.value = checker.check()
        } catch (_: Exception) {
            // 静默：检查失败不打扰使用者，保持当前值。
        }
    }

    /** 本次启动不再提示，清空待升级列表。 */
    fun dismiss() {
        _pending.value = emptyList()
    }
}
