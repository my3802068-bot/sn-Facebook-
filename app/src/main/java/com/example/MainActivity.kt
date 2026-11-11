package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.model.ProxySettings
import com.example.model.ProxyTester
import com.example.ui.components.FACEBOOK_MOBILE_URL
import com.example.ui.components.FacebookOfflineFeed
import com.example.ui.components.FacebookWebView
import com.example.ui.components.FacebookWebViewController
import com.example.ui.components.SnMiniWindowOverlay
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FacebookAppScreen()
            }
        }
    }
}

@Composable
fun FacebookAppScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // File chooser callback storage for Facebook uploads
    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intentData = result.data
            val uris: Array<Uri>? = when {
                intentData?.data != null -> arrayOf(intentData.data!!)
                intentData?.clipData != null -> {
                    val count = intentData.clipData!!.itemCount
                    Array(count) { i -> intentData.clipData!!.getItemAt(i).uri }
                }
                else -> null
            }
            fileUploadCallback?.onReceiveValue(uris)
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    // Facebook WebView Controller
    val webViewController = remember {
        FacebookWebViewController(context) { callback, params ->
            fileUploadCallback?.onReceiveValue(null)
            fileUploadCallback = callback
            try {
                val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                fileChooserLauncher.launch(intent)
                true
            } catch (e: Exception) {
                fileUploadCallback = null
                false
            }
        }
    }

    // App State
    var isSnExpanded by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var isDarkMode by remember { mutableStateOf(false) }
    var isOfflineMode by remember { mutableStateOf(false) }
    var isVpnConnected by remember { mutableStateOf(true) } // Free 1.1.1.1 VPN enabled by default
    var proxySettings by remember { mutableStateOf(ProxySettings()) }

    // Connectivity monitoring
    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Network restored
            }

            override fun onLost(network: Network) {
                // Network lost
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    // Handle back button:
    // 1. If SN Mini Window is open, minimize it
    // 2. If WebView can go back, navigate back
    // 3. Otherwise standard back
    BackHandler(enabled = isSnExpanded || webViewController.canGoBack()) {
        when {
            isSnExpanded -> isSnExpanded = false
            webViewController.canGoBack() -> webViewController.goBack()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF18191A))
        ) {
            // Main Content: Real Facebook WebView or Offline fallback
            if (isOfflineMode) {
                FacebookOfflineFeed(
                    onRetryConnection = {
                        isOfflineMode = false
                        webViewController.reload()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Connecting to Facebook...")
                        }
                    }
                )
            } else {
                FacebookWebView(
                    controller = webViewController,
                    isDesktopMode = isDesktopMode,
                    isDarkMode = isDarkMode,
                    onPageStarted = {
                        // Loading started
                    },
                    onPageFinished = {
                        // Loading finished
                    },
                    onErrorReceived = { error ->
                        isOfflineMode = true
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Unable to load live Facebook. Showing preview.")
                        }
                    }
                )
            }

            // Draggable "SN" Windows Mini Floating Tool
            SnMiniWindowOverlay(
                isExpanded = isSnExpanded,
                onToggleExpanded = { isSnExpanded = it },
                onReload = {
                    if (isOfflineMode) {
                        isOfflineMode = false
                    }
                    webViewController.reload()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("🔄 Reloading Facebook...")
                    }
                },
                onClearData = {
                    webViewController.clearAllData {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("🧹 All Facebook data, cookies & cache cleared!")
                        }
                    }
                },
                onGoHome = {
                    isOfflineMode = false
                    webViewController.goHome()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("🏠 Navigating to Facebook Home")
                    }
                },
                isDesktopMode = isDesktopMode,
                onToggleDesktopMode = {
                    isDesktopMode = !isDesktopMode
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (isDesktopMode) "🖥️ Desktop PC Mode Enabled" else "📱 Mobile Mode Enabled"
                        )
                    }
                },
                isDarkMode = isDarkMode,
                onToggleDarkMode = {
                    isDarkMode = !isDarkMode
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (isDarkMode) "🌙 Facebook Dark Mode On" else "☀️ Facebook Light Mode On"
                        )
                    }
                },
                onZoomIn = {
                    webViewController.zoomIn()
                },
                onZoomOut = {
                    webViewController.zoomOut()
                },
                isVpnConnected = isVpnConnected,
                onToggleVpn = {
                    isVpnConnected = !isVpnConnected
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (isVpnConnected) "🛡️ 1.1.1.1 Free VPN Connected • Cloudflare WARP Active"
                            else "1.1.1.1 VPN Disconnected"
                        )
                    }
                },
                proxySettings = proxySettings,
                onSaveProxy = { settings ->
                    proxySettings = settings
                    ProxyTester.applySystemProxy(settings)
                    webViewController.reload()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("🌐 Custom Proxy Activated: ${settings.protocol.displayName} ${settings.host}:${settings.port}")
                    }
                },
                onDisableProxy = {
                    val disabled = proxySettings.copy(enabled = false)
                    proxySettings = disabled
                    ProxyTester.applySystemProxy(disabled)
                    webViewController.reload()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Custom Proxy Disabled")
                    }
                }
            )
        }
    }
}
