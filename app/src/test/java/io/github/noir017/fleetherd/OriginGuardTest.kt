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
            "https://fleetherd.example.com/sub",
            OriginGuard.normalizeServerUrl("https://fleetherd.example.com/sub/")
        )
        assertNull(OriginGuard.normalizeServerUrl("ftp://fleetherd.example.com"))
        assertNull(OriginGuard.normalizeServerUrl("javascript:alert(1)"))
        assertNull(OriginGuard.normalizeServerUrl(""))
    }

    @Test
    fun checksSameOriginStrictly() {
        val server = "https://fleetherd.example.com/chat"
        assertTrue(OriginGuard.isSameOrigin("https://fleetherd.example.com/api/events", server))
        assertTrue(OriginGuard.isSameOrigin("https://fleetherd.example.com:443/", server))
        assertFalse(OriginGuard.isSameOrigin("http://fleetherd.example.com/", server))
        assertFalse(OriginGuard.isSameOrigin("https://evil.example.com/", server))
    }

    @Test
    fun routesSameOriginAndSsoInAppAndExternalOut() {
        val server = "https://fleetherd.example.com"
        assertTrue(OriginGuard.shouldStayInApp("https://fleetherd.example.com/?t=abc", server))
        assertTrue(OriginGuard.shouldStayInApp("https://team.cloudflareaccess.com/cdn-cgi/access/login", server))
        assertTrue(
            OriginGuard.shouldStayInApp(
                "https://sso.corp.example.org/auth",
                server,
                customSsoHosts = listOf("*.corp.example.org")
            )
        )
        assertFalse(OriginGuard.shouldStayInApp("https://github.com/noir017/fleetherd", server))
    }
}
