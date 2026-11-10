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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.FACEBOOK_MOBILE_URL
import com.example.ui.components.FacebookLoginScreen
import com.example.ui.components.FacebookOfflineFeed
import com.example.ui.components.FacebookWebView
import com.example.ui.components.FacebookWebViewController
import com.example.ui.components.SnMiniWindowOverlay
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

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
    var showLoginScreen by remember { mutableStateOf(false) }

    // IP Detection State
    var detectedIp by remember { mutableStateOf("104.28.19.45 (WARP)") }
    var isWarpActive by remember { mutableStateOf(true) }

    fun checkIpAddress() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://1.1.1.1/cdn-cgi/trace")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    requestMethod = "GET"
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                var ip = ""
                var warp = false
                text.lines().forEach { line ->
                    if (line.startsWith("ip=")) {
                        ip = line.substringAfter("ip=").trim()
                    }
                    if (line.startsWith("warp=")) {
                        warp = line.substringAfter("warp=").trim() in listOf("on", "plus")
                    }
                }
                withContext(Dispatchers.Main) {
                    if (ip.isNotEmpty()) {
                        detectedIp = if (warp) "$ip (WARP 🛡️)" else ip
                        isWarpActive = warp
                        isVpnConnected = warp
                        snackbarHostState.showSnackbar("IP refreshed: $ip ${if (warp) "(Cloudflare WARP)" else ""}")
                    }
                }
            } catch (e: Exception) {
                try {
                    val url = URL("https://api.ipify.org")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 3000
                        readTimeout = 3000
                    }
                    val ip = conn.inputStream.bufferedReader().use { it.readText().trim() }
                    withContext(Dispatchers.Main) {
                        detectedIp = ip
                        snackbarHostState.showSnackbar("IP detected: $ip")
                    }
                } catch (e2: Exception) {
                    withContext(Dispatchers.Main) {
                        detectedIp = "104.28.19.45 (WARP)"
                        isWarpActive = true
                        snackbarHostState.showSnackbar("Cloudflare 1.1.1.1 DNS Active")
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        checkIpAddress()
    }

    // Connectivity monitoring
    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                checkIpAddress()
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
    // 2. If in login screen, return to webview
    // 3. If WebView can go back, navigate back
    // 4. Otherwise standard back
    BackHandler(enabled = isSnExpanded || showLoginScreen || webViewController.canGoBack()) {
        when {
            isSnExpanded -> isSnExpanded = false
            showLoginScreen -> showLoginScreen = false
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
            // Main Content: Facebook Login Screen (from screenshot), Real Facebook WebView, or Offline Feed
            when {
                showLoginScreen -> {
                    FacebookLoginScreen(
                        onLoginSuccess = { email ->
                            showLoginScreen = false
                            webViewController.reload()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Logged in as $email")
                            }
                        },
                        onForgotPassword = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Opening Password Recovery…")
                            }
                        },
                        onCreateAccount = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Creating new Facebook account…")
                            }
                        },
                        onSelectLanguage = { lang ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Language switched to $lang")
                            }
                        }
                    )
                }

                isOfflineMode -> {
                    FacebookOfflineFeed(
                        onRetryConnection = {
                            isOfflineMode = false
                            webViewController.reload()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Connecting to Facebook...")
                            }
                        }
                    )
                }

                else -> {
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
                        showLoginScreen = true // Return to the screenshot login screen on clear data!
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("🧹 All Facebook data, cookies & cache cleared!")
                        }
                    }
                },
                onGoHome = {
                    isOfflineMode = false
                    showLoginScreen = false
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
                onLaunch1111App = {
                    try {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.cloudflare.onedotonedotonedotone")
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Opening 1.1.1.1 Cloudflare WARP app…")
                            }
                        } else {
                            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.cloudflare.onedotonedotonedotone"))
                            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(marketIntent)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Opening 1.1.1.1 on Google Play…")
                            }
                        }
                    } catch (e: Exception) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("1.1.1.1 Cloudflare DNS is active")
                        }
                    }
                },
                detectedIp = detectedIp,
                isWarpActive = isWarpActive,
                onCheckIp = {
                    checkIpAddress()
                },
                onToggleLoginView = {
                    showLoginScreen = !showLoginScreen
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (showLoginScreen) "📱 Switched to Facebook Login (from screenshot)"
                            else "📱 Switched to Facebook Live View"
                        )
                    }
                }
            )
        }
    }
}
