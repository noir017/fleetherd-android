# Keep @JavascriptInterface methods exposed to the trusted WebUI origin.
-keepclassmembers class io.github.noir017.fleetherd.web.NativeBridge {
    @android.webkit.JavascriptInterface <methods>;
}
