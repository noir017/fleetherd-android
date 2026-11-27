package io.github.noir017.fleetherd.web

import java.net.URI

/**
 * URL normalization, same-origin checks for JS bridge security, and in-app vs external routing.
 */
object OriginGuard {
    enum class NavigationAction {
        STAY,
        ASK,
        EXTERNAL,
    }

    private val DEFAULT_SSO_SUFFIXES = listOf(
        "cloudflareaccess.com",
        "cloudflare.com",
        "accounts.google.com",
        "login.microsoftonline.com",
        "login.live.com",
        "appleid.apple.com",
        "github.com",
        "auth0.com",
        "okta.com",
    )

    /**
     * Validates and normalizes a user-supplied server URL.
     * Defaults to https:// when scheme is omitted. Returns null if invalid.
     */
    fun normalizeServerUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        val uri = try {
            URI(withScheme)
        } catch (_: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.lowercase() ?: return null
        if (host.isBlank()) return null
        return withScheme.trimEnd('/')
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

    /**
     * Strict same-origin check, plus allowing an automatic http -> https upgrade on the exact same host.
     */
    fun isSameOrigin(candidateUrl: String?, serverUrl: String?): Boolean {
        val a = originOf(candidateUrl) ?: return false
        val b = originOf(serverUrl) ?: return false
        if (a == b) return true

        val aUri = try { URI(candidateUrl) } catch (_: Exception) { return false }
        val bUri = try { URI(serverUrl) } catch (_: Exception) { return false }
        return bUri.scheme?.lowercase() == "http" &&
            aUri.scheme?.lowercase() == "https" &&
            aUri.host?.lowercase() == bUri.host?.lowercase()
    }

    /**
     * Returns true if the URL should stay inside the WebView (same origin or an allowed SSO IdP).
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

    /**
     * Decides how to handle a WebView navigation request:
     * - Redirects and non-gesture navigations always stay in WebView.
     * - Same-origin navigations (topics, downloads, etc.) always stay in WebView.
     * - User-initiated non-redirect clicks from a same-origin page to any off-origin URL
     *   (including SSO domains like github.com and non-HTTP schemes like mailto:/tel:)
     *   prompt the user with a chooser dialog so chat links never navigate away from the app.
     * - When already on an off-origin page (e.g. an SSO login flow), user clicks follow the
     *   SSO allowlist: allowed SSO hosts stay in WebView, everything else opens externally.
     */
    fun decideNavigation(
        candidateUrl: String?,
        currentUrl: String?,
        serverUrl: String?,
        customSsoHosts: List<String> = emptyList(),
        isRedirect: Boolean,
        hasGesture: Boolean
    ): NavigationAction {
        if (isRedirect || !hasGesture) return NavigationAction.STAY
        if (candidateUrl.isNullOrBlank()) return NavigationAction.STAY
        if (isSameOrigin(candidateUrl, serverUrl)) return NavigationAction.STAY

        if (isSameOrigin(currentUrl, serverUrl)) {
            return NavigationAction.ASK
        }

        return if (shouldStayInApp(candidateUrl, serverUrl, customSsoHosts)) {
            NavigationAction.STAY
        } else {
            NavigationAction.EXTERNAL
        }
    }

    private fun effectivePort(scheme: String, port: Int): Int {
        if (port > 0) return port
        return if (scheme == "https") 443 else 80
    }
}
