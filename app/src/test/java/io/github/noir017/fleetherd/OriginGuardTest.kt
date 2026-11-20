package io.github.noir017.fleetherd

import io.github.noir017.fleetherd.web.OriginGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginGuardTest {
    @Test
    fun normalizesValidServerUrls() {
        assertEquals(
            "https://fleetherd.example.com",
            OriginGuard.normalizeServerUrl("  https://fleetherd.example.com/ ")
        )
        assertEquals(
            "https://fleetherd.example.com",
            OriginGuard.normalizeServerUrl("fleetherd.example.com")
        )
        assertEquals(
            "https://fleetherd.example.com/sub",
            OriginGuard.normalizeServerUrl("https://fleetherd.example.com/sub/")
        )
        assertNull(OriginGuard.normalizeServerUrl("ftp://fleetherd.example.com"))
        assertNull(OriginGuard.normalizeServerUrl("javascript:alert(1)"))
        assertNull(OriginGuard.normalizeServerUrl(""))
    }

    @Test
    fun checksSameOriginAndHttpUpgrade() {
        val server = "https://fleetherd.example.com/chat"
        assertTrue(OriginGuard.isSameOrigin("https://fleetherd.example.com/api/events", server))
        assertTrue(OriginGuard.isSameOrigin("https://fleetherd.example.com:443/", server))
        assertFalse(OriginGuard.isSameOrigin("http://fleetherd.example.com/", server))
        assertFalse(OriginGuard.isSameOrigin("https://evil.example.com/", server))

        // http server upgraded to https on the same host
        assertTrue(OriginGuard.isSameOrigin("https://fleetherd.example.com/", "http://fleetherd.example.com"))
    }

    @Test
    fun keepsRedirectsAndAutoNavigationsInApp() {
        val server = "https://fleetherd.example.com"
        // Server redirect during SSO callback must never kick out to external browser
        assertFalse(
            OriginGuard.shouldOpenExternally(
                candidateUrl = "https://idp.example.net/callback",
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = true,
                hasGesture = true
            )
        )
        // Non-gesture automatic JS redirect stays in app
        assertFalse(
            OriginGuard.shouldOpenExternally(
                candidateUrl = "https://idp.example.net/done",
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = false
            )
        )
        // Explicit user tap on external link opens externally
        assertTrue(
            OriginGuard.shouldOpenExternally(
                candidateUrl = "https://docs.example.org/guide",
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = true
            )
        )
        // Explicit user tap on CF Access / GitHub SSO stays in app
        assertFalse(
            OriginGuard.shouldOpenExternally(
                candidateUrl = "https://team.cloudflareaccess.com/cdn-cgi/access/login",
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = true
            )
        )
    }
}
