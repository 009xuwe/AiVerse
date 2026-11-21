package com.aiverse.feature.web

import com.aiverse.feature.web.data.WebServerSettingsRepository
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网页端访问鉴权：进程级内存 token 集合。
 *
 * token 由登录接口签发、仅存于内存（重启失效，需重新登录），
 * 供鉴权插件校验 `Authorization: Bearer <token>`。
 */
@Singleton
class WebAuthManager @Inject constructor() {

    private val tokens: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** 签发一个新 token 并登记。 */
    fun issueToken(): String = UUID.randomUUID().toString().also { tokens.add(it) }

    /** 校验 token 是否为本进程签发且未失效。 */
    fun validate(token: String): Boolean = tokens.contains(token)
}

/**
 * 网页端鉴权插件：仅当 `jwtEnabled=true` 时对 `/api/` 请求校验 Bearer token，
 * 无效统一返回 401。`/api/auth/login` 与非 `/api` 路径（静态资源、SPA 回退）一律放行；
 * `jwtEnabled=false` 时全放行（向后兼容）。
 *
 * 放行 OPTIONS 预检，避免 CORS 预检请求（不带 Authorization）被拦。
 */
fun createWebAuthPlugin(
    settingsRepository: WebServerSettingsRepository,
    authManager: WebAuthManager
) = createApplicationPlugin("WebAuth") {
    onCall { call ->
        if (call.request.httpMethod == HttpMethod.Options) return@onCall
        val path = call.request.path()
        if (!path.startsWith("/api/")) return@onCall
        if (path == "/api/auth/login") return@onCall
        val jwtEnabled = settingsRepository.currentJwtEnabled()
        if (!jwtEnabled) return@onCall
        val token = call.request.header(HttpHeaders.Authorization)
            ?.removePrefix("Bearer ")
            ?.trim()
            .orEmpty()
        if (token.isBlank() || !authManager.validate(token)) {
            call.respond(HttpStatusCode.Unauthorized, ApiResponse(ok = false, error = "unauthorized"))
        }
    }
}