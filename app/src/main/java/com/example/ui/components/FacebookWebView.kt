package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.FbBlue

const val FACEBOOK_MOBILE_URL = "https://m.facebook.com"
const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

class FacebookWebViewController(
    private val context: Context,
    val onFileChooserRequested: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean
) {
    var webView: WebView? = null
    var defaultUserAgent: String = ""

    fun reload() {
        webView?.reload()
    }

    fun loadUrl(url: String) {
        webView?.loadUrl(url)
    }

    fun goHome() {
        webView?.loadUrl(FACEBOOK_MOBILE_URL)
    }

    fun goBack(): Boolean {
        return if (webView?.canGoBack() == true) {
            webView?.goBack()
            true
        } else {
            false
        }
    }

    fun canGoBack(): Boolean {
        return webView?.canGoBack() == true
    }

    fun setDesktopMode(enabled: Boolean) {
        webView?.settings?.let { settings ->
            if (enabled) {
                settings.userAgentString = DESKTOP_USER_AGENT
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
            } else {
                settings.userAgentString = defaultUserAgent
                settings.useWideViewPort = false
                settings.loadWithOverviewMode = false
            }
            webView?.reload()
        }
    }

    fun setDarkMode(enabled: Boolean) {
        val js = if (enabled) {
            """
            (function() {
                var style = document.getElementById('sn-dark-mode');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'sn-dark-mode';
                    style.innerHTML = 'html { filter: invert(90%) hue-rotate(180deg) !important; background: #121212 !important; } img, video, [style*="background-image"] { filter: invert(100%) hue-rotate(180deg) !important; }';
                    document.head.appendChild(style);
                }
            })();
            """.trimIndent()
        } else {
            """
            (function() {
                var style = document.getElementById('sn-dark-mode');
                if (style) { style.remove(); }
            })();
            """.trimIndent()
        }
        webView?.evaluateJavascript(js, null)
    }

    fun zoomIn() {
        webView?.zoomIn()
    }

    fun zoomOut() {
        webView?.zoomOut()
    }

    fun clearAllData(onComplete: () -> Unit) {
        webView?.let { view ->
            // Clear cache and form data
            view.clearCache(true)
            view.clearHistory()
            view.clearFormData()
            view.clearSslPreferences()

            // Clear WebStorage (local storage, session storage)
            WebStorage.getInstance().deleteAllData()

            // Clear Cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies {
                cookieManager.flush()
                // Reload clean home page
                view.loadUrl(FACEBOOK_MOBILE_URL)
                onComplete()
            }
        } ?: onComplete()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FacebookWebView(
    controller: FacebookWebViewController,
    isDesktopMode: Boolean,
    isDarkMode: Boolean,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit,
    onErrorReceived: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var loadProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    // Save default user agent
                    controller.defaultUserAgent = settings.userAgentString

                    // Setup Cookie Manager
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            loadProgress = newProgress
                            isLoading = newProgress < 100
                        }

                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            return controller.onFileChooserRequested(filePathCallback, fileChooserParams)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            onPageStarted()
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            if (isDarkMode) {
                                controller.setDarkMode(true)
                            }
                            onPageFinished()
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                val errorDesc = error?.description?.toString() ?: "Connection error"
                                onErrorReceived(errorDesc)
                            }
                        }
                    }

                    controller.webView = this

                    if (isDesktopMode) {
                        settings.userAgentString = DESKTOP_USER_AGENT
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                    }

                    loadUrl(FACEBOOK_MOBILE_URL)
                }
            },
            update = { view ->
                controller.webView = view
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("facebook_webview")
        )

        // Web Loading progress bar
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = FbBlue,
                trackColor = FbBlue.copy(alpha = 0.2f)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.webView?.destroy()
            controller.webView = null
        }
    }

    LaunchedEffect(isDesktopMode) {
        controller.setDesktopMode(isDesktopMode)
    }

    LaunchedEffect(isDarkMode) {
        controller.setDarkMode(isDarkMode)
    }
}
