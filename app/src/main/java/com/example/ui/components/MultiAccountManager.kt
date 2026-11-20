package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
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
import kotlin.math.hypot

const val FACEBOOK_LOGIN_URL = "https://m.facebook.com/"
const val FACEBOOK_DESKTOP_URL = "https://www.facebook.com/"
const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

class SyncWebView(context: Context) : WebView(context) {
    var onDragTouchEvent: ((MotionEvent) -> Unit)? = null
    var onUserInteractStart: (() -> Unit)? = null
    var isReplayingTouch: Boolean = false

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var pendingDownEvent: MotionEvent? = null
    private var downX = 0f
    private var downY = 0f
    private var isDraggingGesture = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!isReplayingTouch) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    onUserInteractStart?.invoke()
                    pendingDownEvent?.recycle()
                    pendingDownEvent = MotionEvent.obtain(ev)
                    downX = ev.x
                    downY = ev.y
                    isDraggingGesture = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isDraggingGesture) {
                        val dist = hypot(ev.x - downX, ev.y - downY)
                        if (dist > touchSlop) {
                            isDraggingGesture = true
                            pendingDownEvent?.let { down ->
                                onDragTouchEvent?.invoke(down)
                                down.recycle()
                            }
                            pendingDownEvent = null
                            onDragTouchEvent?.invoke(ev)
                        }
                    } else {
                        onDragTouchEvent?.invoke(ev)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDraggingGesture) {
                        onDragTouchEvent?.invoke(ev)
                    }
                    pendingDownEvent?.recycle()
                    pendingDownEvent = null
                    isDraggingGesture = false
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        if (isReplayingTouch) {
            return false
        }
        return super.requestFocus(direction, previouslyFocusedRect)
    }

    override fun onDetachedFromWindow() {
        pendingDownEvent?.recycle()
        pendingDownEvent = null
        super.onDetachedFromWindow()
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
    private var lastUserInteractionMs = 0L

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

        // Native app feel: hide browser scrollbars & suppress browser long-press text selection menus
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.isHapticFeedbackEnabled = true

        webView.onUserInteractStart = {
            lastActivePane = paneIndex
            lastUserInteractionMs = System.currentTimeMillis()
        }

        webView.onDragTouchEvent = { ev ->
            mirrorDragTouchEvent(paneIndex, ev)
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
            builtInZoomControls = desktopMode
            displayZoomControls = false
            setSupportZoom(desktopMode)
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

    fun injectAppAndSyncScripts(webView: WebView) {
        webView.evaluateJavascript(APP_SHELL_JS, null)
        webView.evaluateJavascript(SYNC_LISTENER_JS, null)
    }

    fun onPaneNavigated(sourcePane: Int, url: String?) {
        if (!isMultiSyncEnabled || accountCount <= 1) return
        if (sourcePane != lastActivePane) return
        if (System.currentTimeMillis() - lastUserInteractionMs > 4000L) return
        val targetPath = extractSyncableFacebookPath(url) ?: return

        // After a brief settle delay, verify other panes also reached this route if they missed a SPA/navigation
        mainHandler.postDelayed({
            if (!isMultiSyncEnabled || accountCount <= 1) return@postDelayed
            if (sourcePane != lastActivePane) return@postDelayed
            for (idx in 0 until accountCount) {
                if (idx == sourcePane) continue
                val other = webViews[idx] ?: continue
                val otherPath = extractSyncableFacebookPath(other.url)
                if (otherPath != null && otherPath != targetPath && !isAuthOrCheckpointPath(otherPath)) {
                    val base = if (isDesktopMode) "https://www.facebook.com" else "https://m.facebook.com"
                    other.loadUrl(base + targetPath)
                }
            }
        }, 650L)
    }

    private fun extractSyncableFacebookPath(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(rawUrl)
            val host = uri.host?.lowercase() ?: return null
            if (!host.endsWith("facebook.com")) return null
            val path = uri.path ?: "/"
            if (isAuthOrCheckpointPath(path)) return null
            val safeSectionPrefixes = listOf(
                "/friends", "/notifications", "/watch", "/groups",
                "/marketplace", "/bookmarks", "/menu", "/search", "/home.php", "/feeds"
            )
            if (path == "/" || safeSectionPrefixes.any { path.startsWith(it) }) {
                val query = uri.query
                if (path.startsWith("/search") && !query.isNullOrBlank()) {
                    "$path?$query"
                } else {
                    path
                }
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun isAuthOrCheckpointPath(path: String): Boolean {
        val lower = path.lowercase()
        return lower.startsWith("/login") ||
            lower.startsWith("/checkpoint") ||
            lower.startsWith("/two_step") ||
            lower.startsWith("/reg") ||
            lower.startsWith("/recover")
    }

    private fun mirrorDragTouchEvent(sourcePane: Int, ev: MotionEvent) {
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
            lastUserInteractionMs = System.currentTimeMillis()
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
        // Makes Facebook WebView look & behave like a native mobile Facebook app instead of a web browser
        private val APP_SHELL_JS = """
            (function() {
                if (window.__nushaAppStyled) return;
                window.__nushaAppStyled = true;

                var style = document.createElement('style');
                style.id = 'nusha-app-shell-style';
                style.textContent = [
                    'html, body { overscroll-behavior-y: contain !important; -webkit-tap-highlight-color: transparent !important; }',
                    '::-webkit-scrollbar { display: none !important; width: 0 !important; height: 0 !important; }',
                    '*:not(input):not(textarea):not([contenteditable="true"]) { -webkit-user-select: none !important; user-select: none !important; -webkit-touch-callout: none !important; }',
                    '[data-sigil*="m-app-banner"], #m_install_app_banner, [data-testid="app_banner"], a[href*="fb://"], a[href*="market://"] { display: none !important; }'
                ].join('\n');
                (document.head || document.documentElement).appendChild(style);
            })();
        """.trimIndent()

        // Semantic DOM click + text input + Enter + SPA navigation sync
        // (Native MotionEvent handles drags/scrolls while semantic DOM matching handles taps accurately even if button Y-positions differ!)
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

                function getHrefPath(el) {
                    try {
                        var href = el.getAttribute('href');
                        if (!href || href.startsWith('#') || href.startsWith('javascript:')) return '';
                        var u = new URL(href, location.origin);
                        return u.pathname;
                    } catch (e) {
                        return '';
                    }
                }

                function normalizeLabel(str) {
                    return (str || '').replace(/[0-9]+/g, '#').replace(/\s+/g, ' ').trim().slice(0, 60);
                }

                function buildDescriptor(el) {
                    if (!el || el.nodeType !== 1) return {};
                    var rect = el.getBoundingClientRect();
                    var tag = el.tagName.toLowerCase();
                    var role = el.getAttribute('role') || '';
                    var ariaLabel = el.getAttribute('aria-label') || '';
                    if (!ariaLabel) {
                        var labeledChild = el.querySelector('[aria-label]');
                        if (labeledChild) ariaLabel = labeledChild.getAttribute('aria-label') || '';
                    }
                    var testId = el.getAttribute('data-testid') || el.getAttribute('data-sigil') || '';
                    var name = el.getAttribute('name') || '';
                    var placeholder = el.getAttribute('placeholder') || '';
                    var type = el.getAttribute('type') || '';
                    var hrefPath = getHrefPath(el);
                    var text = (el.innerText || el.value || el.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 60);
                    var inputs = Array.from(document.querySelectorAll(tag));
                    var fieldIndex = inputs.indexOf(el);
                    return {
                        id: el.id || '',
                        tag: tag,
                        role: role,
                        ariaLabel: ariaLabel,
                        normAria: normalizeLabel(ariaLabel),
                        testId: testId,
                        name: name,
                        placeholder: placeholder,
                        inputType: type,
                        hrefPath: hrefPath,
                        text: text,
                        normText: normalizeLabel(text),
                        selector: getCssPath(el),
                        fieldIndex: fieldIndex,
                        xRatio: window.innerWidth > 0 ? (rect.left + rect.width / 2) / window.innerWidth : 0.5,
                        yRatio: window.innerHeight > 0 ? (rect.top + rect.height / 2) / window.innerHeight : 0.5
                    };
                }

                function isVisible(el) {
                    if (!el) return false;
                    var r = el.getBoundingClientRect();
                    return r.width > 0 && r.height > 0;
                }

                function findElement(d) {
                    if (!d) return null;
                    if (d.id) {
                        var byId = document.getElementById(d.id);
                        if (byId && isVisible(byId)) return byId;
                    }
                    if (d.name) {
                        var byName = document.querySelector(d.tag + '[name="' + CSS.escape(d.name) + '"]');
                        if (byName && isVisible(byName)) return byName;
                    }
                    if (d.testId) {
                        var byTestId = document.querySelector('[data-testid="' + CSS.escape(d.testId) + '"]')
                            || document.querySelector('[data-sigil="' + CSS.escape(d.testId) + '"]');
                        if (byTestId && isVisible(byTestId)) return byTestId;
                    }
                    if (d.ariaLabel) {
                        var exactAria = document.querySelector(d.tag + '[aria-label="' + CSS.escape(d.ariaLabel) + '"]')
                            || document.querySelector('[aria-label="' + CSS.escape(d.ariaLabel) + '"]');
                        if (exactAria && isVisible(exactAria)) return exactAria;
                    }
                    if (d.normAria) {
                        var allAria = document.querySelectorAll('[aria-label]');
                        for (var a = 0; a < allAria.length; a++) {
                            if (isVisible(allAria[a]) && normalizeLabel(allAria[a].getAttribute('aria-label')) === d.normAria) {
                                return allAria[a];
                            }
                        }
                    }
                    if (d.placeholder) {
                        var byPh = document.querySelector(d.tag + '[placeholder="' + CSS.escape(d.placeholder) + '"]');
                        if (byPh && isVisible(byPh)) return byPh;
                    }
                    if (d.hrefPath) {
                        var links = document.querySelectorAll('a[href]');
                        for (var i = 0; i < links.length; i++) {
                            if (isVisible(links[i]) && getHrefPath(links[i]) === d.hrefPath) return links[i];
                        }
                    }
                    if (d.text) {
                        var candidates = document.querySelectorAll('button, a, [role="button"], [role="link"], [role="tab"], [role="menuitem"], input[type="submit"], input[type="button"], div[tabindex="0"]');
                        for (var j = 0; j < candidates.length; j++) {
                            if (!isVisible(candidates[j])) continue;
                            var cText = (candidates[j].innerText || candidates[j].value || '').replace(/\s+/g, ' ').trim().slice(0, 60);
                            if (cText === d.text) return candidates[j];
                        }
                        if (d.normText) {
                            for (var k = 0; k < candidates.length; k++) {
                                if (!isVisible(candidates[k])) continue;
                                var nText = normalizeLabel(candidates[k].innerText || candidates[k].value || '');
                                if (nText === d.normText) return candidates[k];
                            }
                        }
                    }
                    if (d.selector) {
                        try {
                            var bySel = document.querySelector(d.selector);
                            if (bySel && isVisible(bySel)) return bySel;
                        } catch (e) {}
                    }
                    if (typeof d.fieldIndex === 'number' && d.fieldIndex >= 0 && (d.tag === 'input' || d.tag === 'textarea' || d.tag === 'select')) {
                        var list = document.querySelectorAll(d.tag);
                        if (list[d.fieldIndex]) return list[d.fieldIndex];
                    }
                    if (typeof d.xRatio === 'number' && typeof d.yRatio === 'number') {
                        return document.elementFromPoint(d.xRatio * window.innerWidth, d.yRatio * window.innerHeight);
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

                // 1. Capture Taps / Clicks semantically
                document.addEventListener('click', function(e) {
                    if (window.__nushaReplaying) return;
                    if (!e.isTrusted) return;
                    var target = e.target && e.target.closest
                        ? (e.target.closest('button, a, input, select, textarea, label, [role="button"], [role="link"], [role="menuitem"], [role="tab"], [role="switch"], [role="checkbox"], [data-sigil], [tabindex]') || e.target)
                        : e.target;
                    if (!target) return;
                    var desc = buildDescriptor(target);
                    desc.type = 'click';
                    if (window.innerWidth > 0 && window.innerHeight > 0 && e.clientX && e.clientY) {
                        desc.xRatio = e.clientX / window.innerWidth;
                        desc.yRatio = e.clientY / window.innerHeight;
                    }
                    NushaBridge.onAction(JSON.stringify(desc));
                }, true);

                // 2. Capture Text Inputs & Changes
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

                // 3. Capture Enter Key
                document.addEventListener('keydown', function(e) {
                    if (window.__nushaReplaying) return;
                    if (e.key === 'Enter' && e.target) {
                        var desc = buildDescriptor(e.target);
                        desc.type = 'enter';
                        NushaBridge.onAction(JSON.stringify(desc));
                    }
                }, true);

                // Replay Handler
                window.__nushaReplay = function(jsonStr) {
                    try {
                        var data = JSON.parse(jsonStr);
                        window.__nushaReplaying = true;
                        if (data.type === 'click') {
                            var el = findElement(data);
                            if (el) {
                                if (el.scrollIntoView) {
                                    try { el.scrollIntoView({ block: 'nearest', inline: 'nearest' }); } catch (e) {}
                                }
                                var tag = el.tagName ? el.tagName.toLowerCase() : '';
                                if (tag !== 'input' && tag !== 'textarea' && tag !== 'select' && !el.isContentEditable) {
                                    el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
                                    el.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true }));
                                    el.click();
                                } else {
                                    if (el.click) el.click();
                                }
                            }
                        } else if (data.type === 'input') {
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
