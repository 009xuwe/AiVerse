package com.aiverse.feature.web.routes

import com.aiverse.feature.settings.data.repository.InputBarSettingsRepository
import com.aiverse.feature.web.ApiResponse
import com.aiverse.feature.web.WebSearchSettingDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/** 设置路由：读写网页端关心的宿主设置（当前仅联网搜索开关）。 */
fun Route.settingsRoutes(inputBarSettingsRepository: InputBarSettingsRepository) {
    route("/api/settings") {
        get("/web-search") {
            call.respond(WebSearchSettingDto(enabled = inputBarSettingsRepository.webSearchEnabled))
        }
        put("/web-search") {
            val req = runCatching { call.receive<WebSearchSettingDto>() }.getOrNull()
            if (req == null) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse(ok = false, error = "invalid body"))
                return@put
            }
            inputBarSettingsRepository.webSearchEnabled = req.enabled
            call.respond(ApiResponse(ok = true))
        }
    }
}