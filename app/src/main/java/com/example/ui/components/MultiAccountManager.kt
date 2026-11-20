package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

const val FACEBOOK_LOGIN_URL = "https://m.facebook.com/"
const val FACEBOOK_DESKTOP_URL = "https://www.facebook.com/"
const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

class SyncWebView(context: Context) : WebView(context) {
    var onUserTouchEvent: ((MotionEvent) -> Unit)? = null
    var isReplayingTouch: Boolean = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!isReplayingTouch) {
            onUserTouchEvent?.invoke(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        if (isReplayingTouch) {
            return false
        }
        return super.requestFocus(direction, previouslyFocusedRect)
    }
}

class NushaSyncBridge(
    private val paneIndex: Int,
    private val onEventCaptured: (sourcePane: Int, payloadJson: String) -> Unit
) {
    @JavascriptInterface
    fun onAction(payloadJson: String) {
        onEventCaptured(paneIndex, payloadJson)
    }
}

class MultiAccountManager(
    private val context: Context,
    val onFileChooserRequested: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val webViews = mutableMapOf<Int, SyncWebView>()
    private val defaultUserAgents = mutableMapOf<Int, String>()
    private val compactFlags = mutableMapOf<Int, Boolean>()

    var accountCount by mutableIntStateOf(1)
        private set

    var isMultiSyncEnabled by mutableStateOf(false)
        private set

    var isDesktopMode by mutableStateOf(false)
        private set

    var lastActivePane by mutableIntStateOf(0)

    val paneLoadingProgress = mutableStateMapOf<Int, Int>()
    val paneErrors = mutableStateMapOf<Int, String?>()

    fun getInitialUrl(): String = if (isDesktopMode) FACEBOOK_DESKTOP_URL else FACEBOOK_LOGIN_URL

    fun updateAccountCount(newCount: Int) {
        val clamped = newCount.coerceIn(1, 4)
        val previous = accountCount
        accountCount = clamped
        if (clamped > 1 && previous == 1) {
            isMultiSyncEnabled = true
        } else if (clamped == 1) {
            isMultiSyncEnabled = false
        }
        val keysToRemove = webViews.keys.filter { it >= clamped }
        keysToRemove.forEach { idx ->
            webViews.remove(idx)?.destroy()
            defaultUserAgents.remove(idx)
            compactFlags.remove(idx)
            paneLoadingProgress.remove(idx)
            paneErrors.remove(idx)
        }
    }

    fun setMultiSync(enabled: Boolean) {
        isMultiSyncEnabled = if (accountCount > 1) enabled else false
    }

    fun updateDesktopMode(enabled: Boolean) {
        if (isDesktopMode == enabled) return
        isDesktopMode = enabled
        for (idx in 0 until accountCount) {
            val view = webViews[idx] ?: continue
            val isCompact = compactFlags[idx] ?: false
            applyViewSettings(view, idx, isCompact, enabled)
            val currentUrl = view.url ?: ""
            if (enabled) {
                if (currentUrl.contains("://m.facebook.com") || currentUrl.contains("://mbasic.facebook.com")) {
                    view.loadUrl(
                        currentUrl
                            .replace("://m.facebook.com", "://www.facebook.com")
                            .replace("://mbasic.facebook.com", "://www.facebook.com")
                    )
                } else if (currentUrl.isBlank() || currentUrl == "about:blank") {
                    view.loadUrl(FACEBOOK_DESKTOP_URL)
                } else {
                    view.reload()
                }
            } else {
                if (currentUrl.contains("://www.facebook.com") || currentUrl.contains("://web.facebook.com")) {
                    view.loadUrl(
                        currentUrl
                            .replace("://www.facebook.com", "://m.facebook.com")
                            .replace("://web.facebook.com", "://m.facebook.com")
                    )
                } else if (currentUrl.isBlank() || currentUrl == "about:blank") {
                    view.loadUrl(FACEBOOK_LOGIN_URL)
                } else {
                    view.reload()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebView(webView: SyncWebView, paneIndex: Int, isCompactWidth: Boolean) {
        webViews[paneIndex] = webView
        compactFlags[paneIndex] = isCompactWidth

        // Pane 0 uses Default profile cleanly; Panes 1..3 use isolated named profiles when supported
        if (paneIndex > 0 && WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            val profileName = "nusha_acc_${paneIndex + 1}"
            try {
                val profileCache = java.io.File(context.cacheDir, "WebView/$profileName/HTTP Cache/Code Cache")
                java.io.File(profileCache, "js").mkdirs()
                java.io.File(profileCache, "wasm").mkdirs()
                WebViewCompat.setProfile(webView, profileName)
                val profile = ProfileStore.getInstance().getProfile(profileName)
                profile?.cookieManager?.let { cm ->
                    cm.setAcceptCookie(true)
                    cm.setAcceptThirdPartyCookies(webView, true)
                }
            } catch (_: Throwable) {
                val cm = CookieManager.getInstance()
                cm.setAcceptCookie(true)
                cm.setAcceptThirdPartyCookies(webView, true)
            }
        } else {
            val cm = CookieManager.getInstance()
            cm.setAcceptCookie(true)
            cm.setAcceptThirdPartyCookies(webView, true)
        }

        defaultUserAgents[paneIndex] = webView.settings.userAgentString
        applyViewSettings(webView, paneIndex, isCompactWidth, isDesktopMode)

        webView.onUserTouchEvent = { ev ->
            if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                lastActivePane = paneIndex
            }
            mirrorTouchEvent(paneIndex, ev)
        }

        webView.addJavascriptInterface(
            NushaSyncBridge(paneIndex) { sourcePane, payloadJson ->
                handleCapturedJsEvent(sourcePane, payloadJson)
            },
            "NushaBridge"
        )
    }

    private fun applyViewSettings(
        webView: SyncWebView,
        paneIndex: Int,
        isCompactWidth: Boolean,
        desktopMode: Boolean
    ) {
        val mobileUa = defaultUserAgents[paneIndex] ?: webView.settings.userAgentString
        webView.settings.apply {
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

            if (desktopMode) {
                userAgentString = DESKTOP_USER_AGENT
                useWideViewPort = true
                loadWithOverviewMode = true
                textZoom = 100
            } else {
                userAgentString = mobileUa
                useWideViewPort = isCompactWidth
                loadWithOverviewMode = isCompactWidth
                textZoom = if (isCompactWidth) 85 else 100
            }
        }

        when {
            desktopMode -> webView.setInitialScale(0)
            isCompactWidth -> webView.setInitialScale(72)
            else -> webView.setInitialScale(0)
        }
    }

    fun updatePaneDensity(paneIndex: Int, isCompactWidth: Boolean) {
        compactFlags[paneIndex] = isCompactWidth
        val view = webViews[paneIndex] ?: return
        applyViewSettings(view, paneIndex, isCompactWidth, isDesktopMode)
    }

    fun unregisterWebView(paneIndex: Int) {
        webViews.remove(paneIndex)?.destroy()
        defaultUserAgents.remove(paneIndex)
        compactFlags.remove(paneIndex)
        paneLoadingProgress.remove(paneIndex)
        paneErrors.remove(paneIndex)
    }

    fun injectSyncScript(webView: WebView) {
        webView.evaluateJavascript(SYNC_LISTENER_JS, null)
    }

    private fun mirrorTouchEvent(sourcePane: Int, ev: MotionEvent) {
        if (!isMultiSyncEnabled || accountCount <= 1) return
        val sourceView = webViews[sourcePane] ?: return
        val srcW = sourceView.width
        val srcH = sourceView.height
        if (srcW <= 0 || srcH <= 0) return

        for (idx in 0 until accountCount) {
            if (idx == sourcePane) continue
            val target = webViews[idx] ?: continue
            val dstW = target.width
            val dstH = target.height
            if (dstW <= 0 || dstH <= 0) continue

            val cloned = MotionEvent.obtain(ev)
            if (srcW != dstW || srcH != dstH) {
                val matrix = Matrix()
                matrix.setScale(dstW.toFloat() / srcW.toFloat(), dstH.toFloat() / srcH.toFloat())
                cloned.transform(matrix)
            }
            target.isReplayingTouch = true
            try {
                target.dispatchTouchEvent(cloned)
            } finally {
                target.isReplayingTouch = false
                cloned.recycle()
            }
        }
    }

    private fun handleCapturedJsEvent(sourcePane: Int, payloadJson: String) {
        mainHandler.post {
            lastActivePane = sourcePane
            if (!isMultiSyncEnabled || accountCount <= 1) return@post

            val quoted = JSONObject.quote(payloadJson)
            val script = "window.__nushaReplay && window.__nushaReplay($quoted);"
            for (idx in 0 until accountCount) {
                if (idx != sourcePane) {
                    webViews[idx]?.evaluateJavascript(script, null)
                }
            }
        }
    }

    fun reloadAll() {
        val fallback = getInitialUrl()
        for (idx in 0 until accountCount) {
            paneErrors[idx] = null
            val view = webViews[idx]
            if (view != null) {
                if (view.url.isNullOrBlank() || view.url == "about:blank") {
                    view.loadUrl(fallback)
                } else {
                    view.reload()
                }
            }
        }
    }

    fun reloadPane(paneIndex: Int) {
        paneErrors[paneIndex] = null
        val view = webViews[paneIndex] ?: return
        if (view.url.isNullOrBlank() || view.url == "about:blank") {
            view.loadUrl(getInitialUrl())
        } else {
            view.reload()
        }
    }

    fun goBackPane(paneIndex: Int): Boolean {
        val view = webViews[paneIndex] ?: return false
        return if (view.canGoBack()) {
            view.goBack()
            true
        } else {
            false
        }
    }

    fun canAnyGoBack(): Boolean {
        return (0 until accountCount).any { webViews[it]?.canGoBack() == true }
    }

    fun handleSystemBack(): Boolean {
        if (isMultiSyncEnabled && accountCount > 1) {
            var wentBack = false
            for (idx in 0 until accountCount) {
                if (webViews[idx]?.canGoBack() == true) {
                    webViews[idx]?.goBack()
                    wentBack = true
                }
            }
            return wentBack
        } else {
            val active = webViews[lastActivePane]
            if (active?.canGoBack() == true) {
                active.goBack()
                return true
            }
            for (idx in 0 until accountCount) {
                if (webViews[idx]?.canGoBack() == true) {
                    webViews[idx]?.goBack()
                    return true
                }
            }
            return false
        }
    }

    fun clearAllData(onComplete: () -> Unit) {
        for (idx in 0 until accountCount) {
            paneErrors[idx] = null
            webViews[idx]?.apply {
                clearCache(true)
                clearHistory()
                clearFormData()
                clearSslPreferences()
            }
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            try {
                val store = ProfileStore.getInstance()
                for (i in 2..4) {
                    store.getProfile("nusha_acc_$i")?.let { profile ->
                        profile.webStorage.deleteAllData()
                        profile.cookieManager.removeAllCookies {
                            profile.cookieManager.flush()
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }

        WebStorage.getInstance().deleteAllData()
        val defaultCookies = CookieManager.getInstance()
        defaultCookies.removeAllCookies {
            defaultCookies.flush()
            mainHandler.post {
                val homeUrl = getInitialUrl()
                for (idx in 0 until accountCount) {
                    webViews[idx]?.loadUrl(homeUrl)
                }
                onComplete()
            }
        }
    }

    companion object {
        // Native MotionEvent handles taps, drags, scrolls, and button clicks directly across panes.
        // JS listener handles text typing, select/checkbox changes, and Enter key submissions.
        private val SYNC_LISTENER_JS = """
            (function() {
                if (window.__nushaHooked) return;
                window.__nushaHooked = true;
                window.__nushaReplaying = false;

                function getCssPath(el) {
                    if (!el || el.nodeType !== 1) return '';
                    var path = [];
                    var curr = el;
                    while (curr && curr.nodeType === 1 && curr !== document.body && path.length < 6) {
                        var selector = curr.tagName.toLowerCase();
                        if (curr.id) {
                            selector += '#' + CSS.escape(curr.id);
                            path.unshift(selector);
                            break;
                        } else {
                            var sib = curr;
                            var nth = 1;
                            while (sib = sib.previousElementSibling) {
                                if (sib.tagName === curr.tagName) nth++;
                            }
                            if (nth > 1) selector += ':nth-of-type(' + nth + ')';
                        }
                        path.unshift(selector);
                        curr = curr.parentElement;
                    }
                    return path.join(' > ');
                }

                function buildDescriptor(el) {
                    if (!el || el.nodeType !== 1) return {};
                    var tag = el.tagName.toLowerCase();
                    var role = el.getAttribute('role') || '';
                    var ariaLabel = el.getAttribute('aria-label') || '';
                    var testId = el.getAttribute('data-testid') || '';
                    var name = el.getAttribute('name') || '';
                    var placeholder = el.getAttribute('placeholder') || '';
                    var type = el.getAttribute('type') || '';
                    var inputs = Array.from(document.querySelectorAll(tag));
                    var fieldIndex = inputs.indexOf(el);
                    return {
                        id: el.id || '',
                        tag: tag,
                        role: role,
                        ariaLabel: ariaLabel,
                        testId: testId,
                        name: name,
                        placeholder: placeholder,
                        inputType: type,
                        selector: getCssPath(el),
                        fieldIndex: fieldIndex
                    };
                }

                function findElement(d) {
                    if (!d) return null;
                    if (d.id) {
                        var byId = document.getElementById(d.id);
                        if (byId) return byId;
                    }
                    if (d.name) {
                        var byName = document.querySelector(d.tag + '[name="' + CSS.escape(d.name) + '"]');
                        if (byName) return byName;
                    }
                    if (d.testId) {
                        var byTestId = document.querySelector('[data-testid="' + CSS.escape(d.testId) + '"]');
                        if (byTestId) return byTestId;
                    }
                    if (d.ariaLabel) {
                        var byAria = document.querySelector(d.tag + '[aria-label="' + CSS.escape(d.ariaLabel) + '"]')
                            || document.querySelector('[aria-label="' + CSS.escape(d.ariaLabel) + '"]');
                        if (byAria) return byAria;
                    }
                    if (d.placeholder) {
                        var byPh = document.querySelector(d.tag + '[placeholder="' + CSS.escape(d.placeholder) + '"]');
                        if (byPh) return byPh;
                    }
                    if (d.selector) {
                        try {
                            var bySel = document.querySelector(d.selector);
                            if (bySel) return bySel;
                        } catch (e) {}
                    }
                    if (typeof d.fieldIndex === 'number' && d.fieldIndex >= 0 && (d.tag === 'input' || d.tag === 'textarea' || d.tag === 'select')) {
                        var list = document.querySelectorAll(d.tag);
                        if (list[d.fieldIndex]) return list[d.fieldIndex];
                    }
                    return null;
                }

                function setNativeValue(el, value) {
                    var proto = el.tagName === 'TEXTAREA'
                        ? HTMLTextAreaElement.prototype
                        : (el.tagName === 'SELECT' ? HTMLSelectElement.prototype : HTMLInputElement.prototype);
                    var descriptor = Object.getOwnPropertyDescriptor(proto, 'value');
                    if (descriptor && descriptor.set) {
                        descriptor.set.call(el, value);
                    } else {
                        el.value = value;
                    }
                }

                function emitInput(e) {
                    if (window.__nushaReplaying) return;
                    var target = e.target;
                    if (!target) return;
                    var isEditable = target.isContentEditable;
                    var tag = target.tagName ? target.tagName.toLowerCase() : '';
                    if (!isEditable && tag !== 'input' && tag !== 'textarea' && tag !== 'select') return;
                    var desc = buildDescriptor(target);
                    desc.type = 'input';
                    desc.isContentEditable = !!isEditable;
                    desc.value = isEditable ? (target.innerText || '') : (target.value || '');
                    desc.checked = !!target.checked;
                    NushaBridge.onAction(JSON.stringify(desc));
                }
                document.addEventListener('input', emitInput, true);
                document.addEventListener('change', emitInput, true);

                document.addEventListener('keydown', function(e) {
                    if (window.__nushaReplaying) return;
                    if (e.key === 'Enter' && e.target) {
                        var desc = buildDescriptor(e.target);
                        desc.type = 'enter';
                        NushaBridge.onAction(JSON.stringify(desc));
                    }
                }, true);

                window.__nushaReplay = function(jsonStr) {
                    try {
                        var data = JSON.parse(jsonStr);
                        window.__nushaReplaying = true;
                        if (data.type === 'input') {
                            var inp = findElement(data);
                            if (inp) {
                                if (data.isContentEditable) {
                                    inp.innerText = data.value || '';
                                } else if (inp.type === 'checkbox' || inp.type === 'radio') {
                                    inp.checked = !!data.checked;
                                } else {
                                    setNativeValue(inp, data.value || '');
                                }
                                inp.dispatchEvent(new Event('input', { bubbles: true }));
                                inp.dispatchEvent(new Event('change', { bubbles: true }));
                            }
                        } else if (data.type === 'enter') {
                            var targetEl = findElement(data) || document.activeElement;
                            if (targetEl) {
                                if (targetEl.form && typeof targetEl.form.requestSubmit === 'function') {
                                    targetEl.form.requestSubmit();
                                } else if (targetEl.form) {
                                    targetEl.form.submit();
                                } else {
                                    targetEl.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', keyCode: 13, bubbles: true }));
                                    targetEl.dispatchEvent(new KeyboardEvent('keyup', { key: 'Enter', code: 'Enter', keyCode: 13, bubbles: true }));
                                }
                            }
                        }
                    } catch (err) {
                    } finally {
                        setTimeout(function() {
                            window.__nushaReplaying = false;
                        }, 60);
                    }
                };
            })();
        """.trimIndent()
    }
}
