package io.github.noir017.fleetherd.web

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import io.github.noir017.fleetherd.BuildConfig
import org.json.JSONObject

/**
 * Minimal JS-to-Native bridge exposed only to the configured fleetherd origin.
 */
class NativeBridge(
    private val currentUrlProvider: () -> String?,
    private val activeServerUrlProvider: () -> String?,
    private val onSystemBarsRequested: (statusColor: Int, navColor: Int, lightStatusIcons: Boolean?) -> Unit,
    private val onOpenServerManager: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun setSystemBars(statusHex: String?, navHex: String?, lightStatusIcons: String?) {
        mainHandler.post {
            if (!isTrustedOrigin()) return@post
            val statusColor = ColorUtils.parseCssColor(statusHex) ?: return@post
            val navColor = ColorUtils.parseCssColor(navHex) ?: statusColor
            val lightOverride = when (lightStatusIcons?.lowercase()) {
                "true" -> true
                "false" -> false
                else -> null
            }
            onSystemBarsRequested(statusColor, navColor, lightOverride)
        }
    }

    @JavascriptInterface
    fun openServerManager() {
        mainHandler.post {
            if (!isTrustedOrigin()) return@post
            onOpenServerManager()
        }
    }

    @JavascriptInterface
    fun getAppInfo(): String {
        return JSONObject()
            .put("platform", "android")
            .put("versionName", BuildConfig.VERSION_NAME)
            .put("versionCode", BuildConfig.VERSION_CODE)
            .toString()
    }

    private fun isTrustedOrigin(): Boolean {
        return OriginGuard.isSameOrigin(currentUrlProvider(), activeServerUrlProvider())
    }

    companion object {
        const val JS_OBJECT_NAME = "FleetherdAndroid"
    }
}
