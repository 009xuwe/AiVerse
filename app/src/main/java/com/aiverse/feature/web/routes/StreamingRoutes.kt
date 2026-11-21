package com.aiverse.feature.web.routes

import com.aiverse.feature.agent.presentation.run.AgentRunManager
import com.aiverse.feature.web.StreamingEventDto
import io.ktor.server.routing.Route
import io.ktor.server.sse.sse
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val streamingJson = Json { encodeDefaults = true }

/**
 * SSE 流式路由：订阅 [AgentRunManager.streamingAssistants]，按 sessionId 过滤，
 * 把 content/reasoning 变化以 `event: streaming` 推送；本轮结束时推 `event: completed`。
 *
 * 前端按 `data:` 行解析 [StreamingEventDto] JSON。连接保持打开直到客户端断开（协程被取消）。
 */
fun Route.streamingRoutes(agentRunManager: AgentRunManager) {
    sse("/api/sessions/{id}/stream") {
        val sessionId = call.parameters["id"]
        if (sessionId == null) {
            send(event = "error", data = streamingJson.encodeToString(StreamingEventDto()))
            return@sse
        }
        var completedSent = false

        // 先推当前快照，让「迟连接」的客户端立即拿到状态；已结束则补 completed。
        agentRunManager.streamingAssistants.value[sessionId]?.let { snapshot ->
            send(
                event = "streaming",
                data = streamingJson.encodeToString(
                    StreamingEventDto(snapshot.content, snapshot.reasoning, snapshot.settled)
                )
            )
            if (snapshot.settled) {
                completedSent = true
                send(
                    event = "completed",
                    data = streamingJson.encodeToString(
                        StreamingEventDto(snapshot.content, snapshot.reasoning, settled = true)
                    )
                )
            }
        }

        // 持续推送该会话的流式更新；消息从 map 移除视为本轮收尾，兜底补一次 completed。
        agentRunManager.streamingAssistants
            .map { it[sessionId] }
            .distinctUntilChanged()
            .collect { msg ->
                if (msg == null) {
                    if (!completedSent) {
                        completedSent = true
                        send(
                            event = "completed",
                            data = streamingJson.encodeToString(StreamingEventDto(settled = true))
                        )
                    }
                    return@collect
                }
                send(
                    event = "streaming",
                    data = streamingJson.encodeToString(
                        StreamingEventDto(msg.content, msg.reasoning, msg.settled)
                    )
                )
                if (msg.settled && !completedSent) {
                    completedSent = true
                    send(
                        event = "completed",
                        data = streamingJson.encodeToString(
                            StreamingEventDto(msg.content, msg.reasoning, settled = true)
                        )
                    )
                }
            }
    }
}