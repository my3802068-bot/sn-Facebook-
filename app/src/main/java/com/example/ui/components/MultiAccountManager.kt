package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
    private val webViews = mutableMapOf<Int, WebView>()

    var accountCount by mutableIntStateOf(1)
        private set

    var isMultiSyncEnabled by mutableStateOf(false)
        private set

    var lastActivePane by mutableIntStateOf(0)

    val paneLoadingProgress = mutableStateMapOf<Int, Int>()
    val paneErrors = mutableStateMapOf<Int, String?>()

    fun updateAccountCount(newCount: Int) {
        val clamped = newCount.coerceIn(1, 4)
        val previous = accountCount
        accountCount = clamped
        if (clamped > 1 && previous == 1) {
            // Auto-enable multi-sync when switching from 1 to multi (2-4)
            isMultiSyncEnabled = true
        } else if (clamped == 1) {
            isMultiSyncEnabled = false
        }
        // Clean up WebViews above newCount
        val keysToRemove = webViews.keys.filter { it >= clamped }
        keysToRemove.forEach { idx ->
            webViews.remove(idx)?.destroy()
            paneLoadingProgress.remove(idx)
            paneErrors.remove(idx)
        }
    }

    fun toggleMultiSync() {
        if (accountCount > 1) {
            isMultiSyncEnabled = !isMultiSyncEnabled
        }
    }

    fun setMultiSync(enabled: Boolean) {
        isMultiSyncEnabled = if (accountCount > 1) enabled else false
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebView(webView: WebView, paneIndex: Int, isCompactWidth: Boolean) {
        webViews[paneIndex] = webView

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

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            useWideViewPort = isCompactWidth
            loadWithOverviewMode = isCompactWidth
            textZoom = if (isCompactWidth) 85 else 100
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        if (isCompactWidth) {
            webView.setInitialScale(72)
        } else {
            webView.setInitialScale(0)
        }

        webView.addJavascriptInterface(
            NushaSyncBridge(paneIndex) { sourcePane, payloadJson ->
                handleCapturedEvent(sourcePane, payloadJson)
            },
            "NushaBridge"
        )
    }

    fun updatePaneDensity(paneIndex: Int, isCompactWidth: Boolean) {
        val view = webViews[paneIndex] ?: return
        view.settings.apply {
            useWideViewPort = isCompactWidth
            loadWithOverviewMode = isCompactWidth
            textZoom = if (isCompactWidth) 85 else 100
        }
        view.setInitialScale(if (isCompactWidth) 72 else 0)
    }

    fun unregisterWebView(paneIndex: Int) {
        webViews.remove(paneIndex)?.destroy()
        paneLoadingProgress.remove(paneIndex)
        paneErrors.remove(paneIndex)
    }

    fun injectSyncScript(webView: WebView) {
        webView.evaluateJavascript(SYNC_LISTENER_JS, null)
    }

    private fun handleCapturedEvent(sourcePane: Int, payloadJson: String) {
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
        for (idx in 0 until accountCount) {
            paneErrors[idx] = null
            val view = webViews[idx]
            if (view != null) {
                if (view.url.isNullOrBlank() || view.url == "about:blank") {
                    view.loadUrl(FACEBOOK_LOGIN_URL)
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
            view.loadUrl(FACEBOOK_LOGIN_URL)
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

        // Clear Multi-Profiles (panes 1..3 -> nusha_acc_2..4) if supported
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

        // Also clear default WebStorage & CookieManager
        WebStorage.getInstance().deleteAllData()
        val defaultCookies = CookieManager.getInstance()
        defaultCookies.removeAllCookies {
            defaultCookies.flush()
            mainHandler.post {
                for (idx in 0 until accountCount) {
                    webViews[idx]?.loadUrl(FACEBOOK_LOGIN_URL)
                }
                onComplete()
            }
        }
    }

    companion object {
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
                        if (!href) return '';
                        var u = new URL(href, location.origin);
                        return u.pathname + u.search;
                    } catch (e) {
                        return el.getAttribute('href') || '';
                    }
                }

                function buildDescriptor(el) {
                    if (!el || el.nodeType !== 1) return {};
                    var rect = el.getBoundingClientRect();
                    var tag = el.tagName.toLowerCase();
                    var role = el.getAttribute('role') || '';
                    var ariaLabel = el.getAttribute('aria-label') || '';
                    var testId = el.getAttribute('data-testid') || '';
                    var name = el.getAttribute('name') || '';
                    var placeholder = el.getAttribute('placeholder') || '';
                    var type = el.getAttribute('type') || '';
                    var hrefPath = getHrefPath(el);
                    var text = (el.innerText || el.textContent || '').trim().slice(0, 60);
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
                        hrefPath: hrefPath,
                        text: text,
                        selector: getCssPath(el),
                        fieldIndex: fieldIndex,
                        xRatio: window.innerWidth > 0 ? (rect.left + rect.width / 2) / window.innerWidth : 0.5,
                        yRatio: window.innerHeight > 0 ? (rect.top + rect.height / 2) / window.innerHeight : 0.5
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
                    if (d.hrefPath && d.tag === 'a') {
                        var links = document.querySelectorAll('a[href]');
                        for (var i = 0; i < links.length; i++) {
                            if (getHrefPath(links[i]) === d.hrefPath) return links[i];
                        }
                    }
                    if (d.text && (d.tag === 'button' || d.tag === 'a' || d.role === 'button' || d.role === 'link')) {
                        var candidates = document.querySelectorAll('button, a, [role="button"], [role="link"], input[type="submit"], input[type="button"]');
                        for (var j = 0; j < candidates.length; j++) {
                            var cText = (candidates[j].innerText || candidates[j].value || '').trim().slice(0, 60);
                            if (cText === d.text) return candidates[j];
                        }
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

                // 1. Capture Clicks / Taps
                document.addEventListener('click', function(e) {
                    if (window.__nushaReplaying) return;
                    if (!e.isTrusted) return;
                    var target = e.target && e.target.closest
                        ? (e.target.closest('button, a, input, select, textarea, label, [role="button"], [role="link"], [role="menuitem"], [role="tab"], [tabindex]') || e.target)
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

                // 3. Capture Enter / Submit
                document.addEventListener('keydown', function(e) {
                    if (window.__nushaReplaying) return;
                    if (e.key === 'Enter' && e.target) {
                        var desc = buildDescriptor(e.target);
                        desc.type = 'enter';
                        NushaBridge.onAction(JSON.stringify(desc));
                    }
                }, true);

                // 4. Capture Scroll (throttled)
                var scrollTimer = null;
                window.addEventListener('scroll', function() {
                    if (window.__nushaReplaying) return;
                    if (scrollTimer) clearTimeout(scrollTimer);
                    scrollTimer = setTimeout(function() {
                        var maxScroll = Math.max(1, document.documentElement.scrollHeight - window.innerHeight);
                        var payload = {
                            type: 'scroll',
                            yRatio: window.scrollY / maxScroll,
                            scrollY: window.scrollY
                        };
                        NushaBridge.onAction(JSON.stringify(payload));
                    }, 55);
                }, true);

                // Replay Handler
                window.__nushaReplay = function(jsonStr) {
                    try {
                        var data = JSON.parse(jsonStr);
                        window.__nushaReplaying = true;
                        if (data.type === 'click') {
                            var el = findElement(data);
                            if (el) {
                                if (el.focus) el.focus();
                                el.click();
                            }
                        } else if (data.type === 'input') {
                            var inp = findElement(data);
                            if (inp) {
                                if (inp.focus) inp.focus();
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
                        } else if (data.type === 'scroll') {
                            var maxTargetScroll = Math.max(0, document.documentElement.scrollHeight - window.innerHeight);
                            var targetY = maxTargetScroll > 0 ? Math.round((data.yRatio || 0) * maxTargetScroll) : (data.scrollY || 0);
                            window.scrollTo({ top: targetY, behavior: 'auto' });
                        }
                    } catch (err) {
                    } finally {
                        setTimeout(function() {
                            window.__nushaReplaying = false;
                        }, 80);
                    }
                };
            })();
        """.trimIndent()
    }
}
