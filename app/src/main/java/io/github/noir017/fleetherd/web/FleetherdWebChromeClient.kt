package io.github.noir017.fleetherd.web

import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class FleetherdWebChromeClient(
    private val onProgress: (Int) -> Unit,
    private val onOpenFileChooser: (
        filePathCallback: ValueCallback<Array<Uri>>,
        acceptTypes: Array<String>,
        allowMultiple: Boolean
    ) -> Unit,
    private val onAudioPermissionRequested: (PermissionRequest) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onProgress(newProgress)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        if (filePathCallback == null) return false
        val accept = fileChooserParams?.acceptTypes?.filter { it.isNotBlank() }?.toTypedArray()
            ?: arrayOf("*/*")
        val multiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
        onOpenFileChooser(filePathCallback, accept, multiple)
        return true
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        if (request == null) return
        if (request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
            onAudioPermissionRequested(request)
        } else {
            request.deny()
        }
    }
}
