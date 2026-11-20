package io.github.noir017.fleetherd.web

import java.net.URI

/**
 * URL normalization, same-origin checks for JS bridge security, and in-app vs external routing.
 */
object OriginGuard {
    private val DEFAULT_SSO_SUFFIXES = listOf(
        "cloudflareaccess.com",
        "accounts.google.com",
        "login.microsoftonline.com",
        "login.live.com",
        "appleid.apple.com",
        "auth0.com",
        "okta.com",
    )

    /**
     * Validates and normalizes a user-supplied server URL.
     * Returns null if scheme is not http/https or host is empty.
     */
    fun normalizeServerUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.lowercase() ?: return null
        if (host.isBlank()) return null
        return trimmed.trimEnd('/')
    }

    fun originOf(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.lowercase() ?: return null
        val port = effectivePort(scheme, uri.port)
        return "$scheme://$host:$port"
    }

    fun isSameOrigin(candidateUrl: String?, serverUrl: String?): Boolean {
        val a = originOf(candidateUrl) ?: return false
        val b = originOf(serverUrl) ?: return false
        return a == b
    }

    /**
     * Returns true if the URL should stay inside the WebView (same origin or an allowed SSO IdP).
     * Everything else is handed off to Custom Tabs / external browser.
     */
    fun shouldStayInApp(
        candidateUrl: String?,
        serverUrl: String?,
        customSsoHosts: List<String> = emptyList()
    ): Boolean {
        if (candidateUrl.isNullOrBlank()) return false
        if (isSameOrigin(candidateUrl, serverUrl)) return true

        val uri = try {
            URI(candidateUrl)
        } catch (_: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false

        val allSuffixes = DEFAULT_SSO_SUFFIXES + customSsoHosts
            .map { it.trim().lowercase().removePrefix("*.") }
            .filter { it.isNotEmpty() }

        return allSuffixes.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }
    }

    private fun effectivePort(scheme: String, port: Int): Int {
        if (port > 0) return port
        return if (scheme == "https") 443 else 80
    }
}
