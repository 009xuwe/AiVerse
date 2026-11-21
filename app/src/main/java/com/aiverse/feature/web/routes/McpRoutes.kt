package com.aiverse.feature.web.routes

import com.aiverse.feature.agent.domain.mcp.McpManager
import com.aiverse.feature.web.McpServerDto
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/** MCP 路由：返回各 MCP server 的连接状态（供网页端展示）。 */
fun Route.mcpRoutes(mcpManager: McpManager) {
    route("/api") {
        get("/mcp") {
            val statuses = mcpManager.statuses.value
            call.respond(statuses.map { s ->
                McpServerDto(
                    name = s.name,
                    state = s.state.name,
                    toolCount = s.toolCount,
                    error = s.error
                )
            })
        }
    }
}