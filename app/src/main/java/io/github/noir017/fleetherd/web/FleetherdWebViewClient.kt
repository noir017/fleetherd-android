package io.github.noir017.fleetherd.web

import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.github.noir017.fleetherd.ui.ExternalLinkDialog

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

        return when (
            OriginGuard.decideNavigation(
                candidateUrl = url,
                currentUrl = view?.url,
                serverUrl = activeServerUrlProvider(),
                customSsoHosts = ssoHostsProvider(),
                isRedirect = request.isRedirect,
                hasGesture = request.hasGesture()
            )
        ) {
            OriginGuard.NavigationAction.STAY -> false
            OriginGuard.NavigationAction.ASK -> {
                ExternalLinkDialog.show(context, uri)
                true
            }
            OriginGuard.NavigationAction.EXTERNAL -> {
                ExternalLinkDialog.openInExternalApp(context, uri)
                true
            }
        }
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
}
