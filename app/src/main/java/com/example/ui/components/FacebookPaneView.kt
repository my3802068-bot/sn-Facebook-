package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.FbBlue

@SuppressLint("ClickableViewAccessibility")
@Composable
fun FacebookPaneView(
    paneIndex: Int,
    totalPanes: Int,
    isCompactWidth: Boolean,
    isMultiSyncEnabled: Boolean,
    isLastActive: Boolean,
    manager: MultiAccountManager,
    onPaneTouched: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = manager.paneLoadingProgress[paneIndex] ?: 100
    val errorText = manager.paneErrors[paneIndex]
    val isLoading = progress in 1..99

    val borderColor = when {
        totalPanes == 1 -> Color.Transparent
        isMultiSyncEnabled && isLastActive -> Color(0xFF10B981)
        isMultiSyncEnabled -> Color(0xFF1877F2).copy(alpha = 0.55f)
        isLastActive -> FbBlue
        else -> Color(0xFF2D2F34)
    }

    LaunchedEffect(isCompactWidth) {
        manager.updatePaneDensity(paneIndex, isCompactWidth)
    }

    DisposableEffect(paneIndex) {
        onDispose {
            manager.unregisterWebView(paneIndex)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (totalPanes > 1) {
                    Modifier.border(
                        BorderStroke(1.5.dp, borderColor),
                        shape = RoundedCornerShape(6.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = if (totalPanes > 1) RoundedCornerShape(6.dp) else RoundedCornerShape(0.dp),
        color = Color(0xFF121316)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Compact header bar when 2-4 panes are visible
            if (totalPanes > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .background(Color(0xFF16181D))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isMultiSyncEnabled) Color(0xFF10B981) else Color(0xFFF59E0B)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FB #${paneIndex + 1}",
                            color = if (isLastActive) Color.White else Color(0xFFB0B3B8),
                            fontSize = 11.sp,
                            fontWeight = if (isLastActive) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isMultiSyncEnabled && isLastActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SYNC",
                                color = Color(0xFF10B981),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { manager.goBackPane(paneIndex) }
                                .testTag("pane_${paneIndex}_back"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back pane ${paneIndex + 1}",
                                tint = Color(0xFFD1D5DB),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { manager.reloadPane(paneIndex) }
                                .testTag("pane_${paneIndex}_reload"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload pane ${paneIndex + 1}",
                                tint = Color(0xFFD1D5DB),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

                            manager.configureWebView(this, paneIndex, isCompactWidth)

                            setOnTouchListener { _, event ->
                                if (event.action == MotionEvent.ACTION_DOWN) {
                                    onPaneTouched(paneIndex)
                                }
                                false
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    manager.paneLoadingProgress[paneIndex] = newProgress
                                }

                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<android.net.Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    return manager.onFileChooserRequested(filePathCallback, fileChooserParams)
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    manager.paneErrors[paneIndex] = null
                                    manager.paneLoadingProgress[paneIndex] = 10
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    manager.paneLoadingProgress[paneIndex] = 100
                                    if (view != null) {
                                        manager.injectSyncScript(view)
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        manager.paneErrors[paneIndex] =
                                            error?.description?.toString() ?: "Unable to connect to Facebook"
                                    }
                                }
                            }

                            loadUrl(FACEBOOK_LOGIN_URL)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("facebook_webview_$paneIndex")
                )

                // Loading progress indicator
                androidx.compose.animation.AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = FbBlue,
                        trackColor = FbBlue.copy(alpha = 0.2f)
                    )
                }

                // Real network error overlay with Retry
                if (errorText != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF121316))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Connection Error",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Connection failed",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorText,
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { manager.reloadPane(paneIndex) },
                            colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("pane_${paneIndex}_retry")
                        ) {
                            Text("Retry", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
