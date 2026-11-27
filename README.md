# fleetherd-android

Lightweight native Android shell for the [fleetherd](https://github.com/noir017/fleetherd) WebUI.

Instead of rewriting the WebUI in native widgets, this app wraps Android's System WebView in a thin Kotlin shell so every WebUI update on the server takes effect immediately with no extra browser chrome.

[中文文档](README.zh-CN.md)

## Features

- **Zero browser chrome**: Full-screen standalone experience with adaptive `>_` launcher icon and matching `#131313` cold-start background.
- **Dynamic status & navigation bars**: Edge-to-Edge layout with smooth color transitions and automatic light/dark icon contrast. Automatically adapts to `<meta name="theme-color">`, drawers, and overlays, and also exposes `window.FleetherdNative.setSystemBars(...)`.
- **Keyboard-safe layout**: Properly adjusts viewport height for the soft keyboard without overlapping the composer bar.
- **Multi-server support**: Configure and switch between multiple `fleetherd` instances at runtime (no hardcoded URLs or tokens). Open the server manager via:
  - Long-pressing the launcher icon → **Manage Servers**
  - Triple-tapping the top status bar area
  - The **Switch Server** button on the error screen
  - Calling `window.FleetherdNative.openServerManager()` from the WebUI
- **Layered back gesture**: Dismisses open modals, menus, and drawers first (via `window.__fleetherdBack()` or `Escape` fallback), then WebView history, then double-back to exit.
- **SSO & external links**: Keeps same-origin pages and SSO login flows (e.g. Cloudflare Access, Google, Microsoft, or custom hosts) inside the app with persistent cookies. Tapping an external link from the fleetherd WebUI prompts a native chooser dialog to open it in an external browser/app or copy the URL.
- **Attachments & voice**: Supports file upload chooser, authenticated downloads via `DownloadManager` (passing active session cookies), and microphone permission requests for voice recording.

## JS Bridge Contract

Injected into same-origin pages only (strictly verified against the active server origin):

```js
// Dynamically update status bar and navigation bar colors
window.FleetherdNative?.setSystemBars({
  statusBar: '#171717',
  navBar: '#131313',
  // optional; omit to auto-detect from luminance
  lightStatusIcons: false,
});

// Open the native server switcher dialog
window.FleetherdNative?.openServerManager();

// Read app version info
window.FleetherdNative?.getAppInfo();
```

## Building

Requires JDK 17, Android SDK (API 35), and Gradle 8.11+:

```bash
gradle testDebugUnitTest assembleDebug
```

GitHub Actions builds a debug APK on every push to `main`. Pushing a `v*` tag publishes a GitHub Release (uses signed release APK when `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` secrets are configured).

## Roadmap (Phase 3)

- Native offline push notifications via UnifiedPush (self-hosted ntfy) or FCM, with notification tap deep-linking directly to `/?t=<topic>`.

## License

MIT
