package io.github.noir017.fleetherd

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.noir017.fleetherd.data.ServerConfigStore
import io.github.noir017.fleetherd.data.ServerProfile
import io.github.noir017.fleetherd.databinding.ActivityMainBinding
import io.github.noir017.fleetherd.download.AuthenticatedDownloader
import io.github.noir017.fleetherd.ui.ServerSetupDialog
import io.github.noir017.fleetherd.ui.SystemBarController
import io.github.noir017.fleetherd.web.FleetherdWebChromeClient
import io.github.noir017.fleetherd.web.FleetherdWebViewClient
import io.github.noir017.fleetherd.web.NativeBridge

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var store: ServerConfigStore
    private lateinit var systemBars: SystemBarController

    private var activeServer: ServerProfile? = null
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingAudioPermission: PermissionRequest? = null
    private var lastBackPressMs = 0L
    private var statusTapCount = 0
    private var lastStatusTapMs = 0L

    private val singleFilePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingFileCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        pendingFileCallback = null
    }

    private val multiFilePicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        pendingFileCallback?.onReceiveValue(if (uris.isNotEmpty()) uris.toTypedArray() else null)
        pendingFileCallback = null
    }

    private val recordAudioLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val req = pendingAudioPermission
        pendingAudioPermission = null
        if (req != null) {
            if (granted) {
                req.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
            } else {
                req.deny()
                Toast.makeText(this, R.string.mic_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = ServerConfigStore(this)
        val defaultBg = ContextCompat.getColor(this, R.color.fh_bg)
        systemBars = SystemBarController(
            window = window,
            rootContainer = binding.rootContainer,
            statusBarScrim = binding.statusBarScrim,
            navBarScrim = binding.navBarScrim,
            initialColor = defaultBg
        ).also { it.attach() }

        setupStatusBarTripleTap()
        setupErrorActions()
        setupWebView()
        setupBackNavigation()

        val initial = store.active()
        if (initial == null || intent?.action == ACTION_MANAGE_SERVERS) {
            activeServer = initial
            if (initial != null) loadServer(initial, extractTopicFromIntent(intent))
            openServerManager(cancelable = initial != null)
        } else {
            loadServer(initial, extractTopicFromIntent(intent))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_MANAGE_SERVERS) {
            openServerManager(cancelable = activeServer != null)
            return
        }
        val topic = extractTopicFromIntent(intent)
        val server = activeServer ?: store.active()
        if (server != null && !topic.isNullOrBlank()) {
            loadServer(server, topic)
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = null
        binding.webView.destroy()
        super.onDestroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            userAgentString = "$userAgentString FleetherdAndroid/${BuildConfig.VERSION_NAME}"
        }

        val bridge = NativeBridge(
            currentUrlProvider = { webView.url },
            activeServerUrlProvider = { activeServer?.url },
            onSystemBarsRequested = { statusColor, navColor, lightIcons ->
                systemBars.applyColors(statusColor, navColor, lightIcons, animate = true)
            },
            onOpenServerManager = { openServerManager(cancelable = true) }
        )
        webView.addJavascriptInterface(bridge, NativeBridge.JS_OBJECT_NAME)

        webView.webViewClient = FleetherdWebViewClient(
            context = this,
            activeServerUrlProvider = { activeServer?.url },
            ssoHostsProvider = { activeServer?.ssoHosts ?: emptyList() },
            onPageLoaded = { binding.errorOverlay.visibility = View.GONE },
            onMainFrameError = { detail ->
                binding.errorDetail.text = detail
                binding.errorOverlay.visibility = View.VISIBLE
            }
        )

        webView.webChromeClient = FleetherdWebChromeClient(
            onProgress = { progress ->
                binding.loadingBar.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
                binding.loadingBar.progress = progress
            },
            onOpenFileChooser = { callback, acceptTypes, allowMultiple ->
                pendingFileCallback?.onReceiveValue(null)
                pendingFileCallback = callback
                val mimes = acceptTypes.ifEmpty { arrayOf("*/*") }
                if (allowMultiple) {
                    multiFilePicker.launch(mimes)
                } else {
                    singleFilePicker.launch(mimes)
                }
            },
            onAudioPermissionRequested = { request ->
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                } else {
                    pendingAudioPermission?.deny()
                    pendingAudioPermission = request
                    recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        )

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            AuthenticatedDownloader.download(this, url, userAgent, contentDisposition, mimeType)
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                binding.webView.evaluateJavascript(
                    "(function(){return !!(window.__fleetherdConsumeBack && window.__fleetherdConsumeBack());})()"
                ) { result ->
                    if (result == "true") return@evaluateJavascript
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                    } else {
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastBackPressMs < 2000L) {
                            finish()
                        } else {
                            lastBackPressMs = now
                            Toast.makeText(
                                this@MainActivity,
                                R.string.back_press_again_to_exit,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        })
    }

    private fun setupStatusBarTripleTap() {
        binding.statusBarScrim.setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            if (now - lastStatusTapMs > 600L) {
                statusTapCount = 1
            } else {
                statusTapCount++
            }
            lastStatusTapMs = now
            if (statusTapCount >= 3) {
                statusTapCount = 0
                openServerManager(cancelable = activeServer != null)
            }
        }
    }

    private fun setupErrorActions() {
        binding.retryButton.setOnClickListener {
            binding.errorOverlay.visibility = View.GONE
            binding.webView.reload()
        }
        binding.switchServerButton.setOnClickListener {
            openServerManager(cancelable = true)
        }
    }

    private fun openServerManager(cancelable: Boolean) {
        ServerSetupDialog.show(this, store, cancelable) { selected ->
            loadServer(selected, topic = null)
        }
    }

    private fun loadServer(profile: ServerProfile, topic: String?) {
        activeServer = profile
        binding.errorOverlay.visibility = View.GONE
        val targetUrl = if (!topic.isNullOrBlank()) {
            "${profile.url}/?t=${Uri.encode(topic)}"
        } else {
            profile.url
        }
        binding.webView.loadUrl(targetUrl)
    }

    private fun extractTopicFromIntent(intent: Intent?): String? {
        return intent?.getStringExtra(EXTRA_TOPIC)
            ?: intent?.data?.getQueryParameter("t")
    }

    companion object {
        const val ACTION_MANAGE_SERVERS = "io.github.noir017.fleetherd.ACTION_MANAGE_SERVERS"
        const val EXTRA_TOPIC = "topic"
    }
}
