package io.github.noir017.fleetherd.web

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent

class FleetherdWebViewClient(
    private val context: Context,
    private val activeServerUrlProvider: () -> String?,
    private val ssoHostsProvider: () -> List<String>,
    private val onPageLoaded: () -> Unit,
    private val onMainFrameError: (description: String) -> Unit
) : WebViewClient() {

    private var mainFrameFailed = false

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        mainFrameFailed = false
        super.onPageStarted(view, url, favicon)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val url = uri.toString()

        if (OriginGuard.shouldStayInApp(url, activeServerUrlProvider(), ssoHostsProvider())) {
            return false
        }

        openExternally(uri)
        return true
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        CookieManager.getInstance().flush()
        if (!mainFrameFailed) {
            onPageLoaded()
            if (view != null && OriginGuard.isSameOrigin(url, activeServerUrlProvider())) {
                view.evaluateJavascript(WebBridgeScript.INJECT_JS, null)
            }
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        if (request?.isForMainFrame == true) {
            mainFrameFailed = true
            val desc = error?.description?.toString() ?: "Network error"
            onMainFrameError(desc)
        }
    }

    private fun openExternally(uri: Uri) {
        try {
            if (uri.scheme == "http" || uri.scheme == "https") {
                CustomTabsIntent.Builder().build().launchUrl(context, uri)
                return
            }
        } catch (_: Exception) {
            // Fall back to default system handler.
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            // No handler available on device.
        }
    }
}
