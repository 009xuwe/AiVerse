package com.aiverse.feature.web.routes

import com.aiverse.feature.web.ApiResponse
import com.aiverse.feature.web.LoginRequest
import com.aiverse.feature.web.LoginResponse
import com.aiverse.feature.web.WebAuthManager
import com.aiverse.feature.web.data.WebServerSettingsRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** 鉴权路由：访问密码换 Bearer token（仅当服务端开启身份验证时有意义）。 */
fun Route.authRoutes(
    authManager: WebAuthManager,
    settingsRepository: WebServerSettingsRepository
) {
    route("/api/auth") {
        post("/login") {
            val req = runCatching { call.receive<LoginRequest>() }.getOrNull()
            if (req == null) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse(ok = false, error = "invalid body"))
                return@post
            }
            // 与设置的访问密码明文比对；两者都为空也视为匹配（未设密码即开放）。
            val expected = settingsRepository.currentAccessPassword()
            if (req.password != expected) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse(ok = false, error = "invalid password"))
                return@post
            }
            call.respond(LoginResponse(token = authManager.issueToken()))
        }
    }
}