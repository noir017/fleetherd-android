package io.github.noir017.fleetherd

import io.github.noir017.fleetherd.web.OriginGuard
import io.github.noir017.fleetherd.web.OriginGuard.NavigationAction
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
        val chatPage = "https://fleetherd.example.com/?t=topic-1"
        // Server redirect during SSO callback must never kick out to external browser
        assertEquals(
            NavigationAction.STAY,
            OriginGuard.decideNavigation(
                candidateUrl = "https://idp.example.net/callback",
                currentUrl = chatPage,
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = true,
                hasGesture = true
            )
        )
        // Non-gesture automatic JS redirect stays in app
        assertEquals(
            NavigationAction.STAY,
            OriginGuard.decideNavigation(
                candidateUrl = "https://idp.example.net/done",
                currentUrl = chatPage,
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = false
            )
        )
    }

    @Test
    fun promptsForOffOriginLinksTappedFromGatewayPage() {
        val server = "https://fleetherd.example.com"
        val chatPage = "https://fleetherd.example.com/?t=topic-1"

        // Tapping a github.com link in chat must prompt instead of navigating the WebView away
        assertEquals(
            NavigationAction.ASK,
            OriginGuard.decideNavigation(
                candidateUrl = "https://github.com/example/repo",
                currentUrl = chatPage,
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = true
            )
        )
        // Regular external http/https link prompts
        assertEquals(
            NavigationAction.ASK,
            OriginGuard.decideNavigation(
                candidateUrl = "https://docs.example.org/guide",
                currentUrl = chatPage,
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = true
            )
        )
        // Non-HTTP schemes like mailto: and tel: tapped from gateway page also prompt
        assertEquals(
            NavigationAction.ASK,
            OriginGuard.decideNavigation(
                candidateUrl = "mailto:user@example.com",
                currentUrl = chatPage,
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = true
            )
        )
        assertEquals(
            NavigationAction.ASK,
            OriginGuard.decideNavigation(
                candidateUrl = "tel:+15551234567",
                currentUrl = chatPage,
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = true
            )
        )
        // Same-origin links (topic switches, file downloads) stay in app
        assertEquals(
            NavigationAction.STAY,
            OriginGuard.decideNavigation(
                candidateUrl = "https://fleetherd.example.com/?t=topic-2",
                currentUrl = chatPage,
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = true
            )
        )
        assertEquals(
            NavigationAction.STAY,
            OriginGuard.decideNavigation(
                candidateUrl = "https://fleetherd.example.com/api/files/sample.txt",
                currentUrl = chatPage,
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = true
            )
        )
    }

    @Test
    fun keepsSsoFlowBehaviorWhenAlreadyOnOffOriginPage() {
        val server = "https://fleetherd.example.com"
        val ssoPage = "https://team.cloudflareaccess.com/cdn-cgi/access/login"

        // Inside an SSO login page, tapping GitHub SSO stays in WebView
        assertEquals(
            NavigationAction.STAY,
            OriginGuard.decideNavigation(
                candidateUrl = "https://github.com/login/oauth/authorize",
                currentUrl = ssoPage,
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = true
            )
        )
        // Inside an SSO login page, tapping another default or custom SSO host stays in WebView
        assertEquals(
            NavigationAction.STAY,
            OriginGuard.decideNavigation(
                candidateUrl = "https://team.cloudflareaccess.com/cdn-cgi/access/login",
                currentUrl = ssoPage,
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = true
            )
        )
        assertEquals(
            NavigationAction.STAY,
            OriginGuard.decideNavigation(
                candidateUrl = "https://id.sso.example.org/auth",
                currentUrl = ssoPage,
                serverUrl = server,
                customSsoHosts = listOf("sso.example.org"),
                isRedirect = false,
                hasGesture = true
            )
        )
        // Inside an SSO login page, tapping a non-SSO external link opens externally
        assertEquals(
            NavigationAction.EXTERNAL,
            OriginGuard.decideNavigation(
                candidateUrl = "https://docs.example.org/privacy",
                currentUrl = ssoPage,
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = true
            )
        )
        // Returning to same-origin from SSO stays in WebView
        assertEquals(
            NavigationAction.STAY,
            OriginGuard.decideNavigation(
                candidateUrl = "https://fleetherd.example.com/",
                currentUrl = ssoPage,
                serverUrl = server,
                customSsoHosts = emptyList(),
                isRedirect = false,
                hasGesture = true
            )
        )
    }
}
