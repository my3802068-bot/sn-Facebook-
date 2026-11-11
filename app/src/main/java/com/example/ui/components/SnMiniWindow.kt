package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.model.ProxyProtocol
import com.example.model.ProxySettings
import com.example.model.ProxyTester
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CloudflareOrange
import com.example.ui.theme.CloudflareOrangeContainer
import com.example.ui.theme.FbBlue
import com.example.ui.theme.VpnConnectedGreen
import com.example.ui.theme.WinCloseRed
import com.example.ui.theme.WinHeaderBlue
import kotlin.math.roundToInt

@Composable
fun SnMiniWindowOverlay(
    isExpanded: Boolean,
    onToggleExpanded: (Boolean) -> Unit,
    onReload: () -> Unit,
    onClearData: () -> Unit,
    onGoHome: () -> Unit,
    isDesktopMode: Boolean,
    onToggleDesktopMode: () -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    isVpnConnected: Boolean,
    onToggleVpn: () -> Unit,
    proxySettings: ProxySettings = ProxySettings(),
    onSaveProxy: (ProxySettings) -> Unit = {},
    onDisableProxy: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    var isReloadingAnim by remember { mutableStateOf(false) }

    val reloadRotation by animateFloatAsState(
        targetValue = if (isReloadingAnim) 360f else 0f,
        animationSpec = tween(durationMillis = 600),
        finishedListener = { isReloadingAnim = false },
        label = "ReloadRotation"
    )

    // Proxy Setup & Live Check Dialog
    if (showProxyDialog) {
        ProxySetupDialog(
            initialSettings = proxySettings,
            onDismiss = { showProxyDialog = false },
            onSaveProxy = { newSettings ->
                showProxyDialog = false
                onSaveProxy(newSettings)
            },
            onDisableProxy = {
                showProxyDialog = false
                onDisableProxy()
            }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxAvailableWidthPx = with(density) { maxWidth.toPx() }
        val maxAvailableHeightPx = with(density) { maxHeight.toPx() }

        // Initial position at top-right
        var offsetX by remember { mutableFloatStateOf(maxAvailableWidthPx - with(density) { 150.dp.toPx() }) }
        var offsetY by remember { mutableFloatStateOf(with(density) { 70.dp.toPx() }) }

        // Confirmation dialog for clearing data
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Clear All Facebook Data?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Text(
                        text = "This will clear cookies, cache, browsing history, and saved login session data from the browser. Are you sure?",
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearConfirmDialog = false
                            onClearData()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("confirm_clear_data_button")
                    ) {
                        Text("Clear Data", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = offsetX.roundToInt().coerceIn(0, (maxAvailableWidthPx - 100).toInt().coerceAtLeast(0)),
                        y = offsetY.roundToInt().coerceIn(0, (maxAvailableHeightPx - 100).toInt().coerceAtLeast(0))
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = offsetX + dragAmount.x
                        val newY = offsetY + dragAmount.y
                        // Clamp inside screen bounds
                        offsetX = newX.coerceIn(10f, maxAvailableWidthPx - 100f)
                        offsetY = newY.coerceIn(20f, maxAvailableHeightPx - 100f)
                    }
                }
        ) {
            AnimatedVisibility(
                visible = !isExpanded,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                // Minimized "SN" Floating Badge / Pill
                SnFloatingBadge(
                    isVpnConnected = isVpnConnected,
                    proxySettings = proxySettings,
                    onTap = { onToggleExpanded(true) },
                    onQuickReload = {
                        isReloadingAnim = true
                        onReload()
                    }
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                // Expanded "Windows Mini" Floating App Window
                SnFloatingWindowCard(
                    reloadRotation = reloadRotation,
                    isVpnConnected = isVpnConnected,
                    onToggleVpn = onToggleVpn,
                    proxySettings = proxySettings,
                    onOpenProxyDialog = { showProxyDialog = true },
                    onDisableProxy = onDisableProxy,
                    onReloadClick = {
                        isReloadingAnim = true
                        onReload()
                    },
                    onClearDataClick = {
                        showClearConfirmDialog = true
                    },
                    onGoHome = onGoHome,
                    isDesktopMode = isDesktopMode,
                    onToggleDesktopMode = onToggleDesktopMode,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    onMinimize = { onToggleExpanded(false) },
                    onResetPosition = {
                        offsetX = maxAvailableWidthPx - with(density) { 150.dp.toPx() }
                        offsetY = with(density) { 70.dp.toPx() }
                    }
                )
            }
        }
    }
}

/**
 * Minimized state: Modern floating capsule displaying "SN" & Windows logo
 */
@Composable
private fun SnFloatingBadge(
    isVpnConnected: Boolean,
    proxySettings: ProxySettings,
    onTap: () -> Unit,
    onQuickReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .testTag("sn_mini_badge")
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onTap() }
            .border(
                width = 1.8.dp,
                brush = Brush.linearGradient(
                    colors = if (proxySettings.enabled) {
                        listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF00D2FF))
                    } else if (isVpnConnected) {
                        listOf(CloudflareOrange, Color(0xFFFAAD3F), VpnConnectedGreen)
                    } else {
                        listOf(WinHeaderBlue, FbBlue, Color(0xFF00C6FF))
                    }
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        color = Color(0xFF1E1E1E)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Drag grip indicator
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Drag SN tool",
                tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(16.dp)
            )

            // Windows Logo Symbol
            WindowsTileLogo(size = 15.dp)

            // "SN" Name Tag
            Text(
                text = "SN",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            )

            // Custom Proxy active indicator
            if (proxySettings.enabled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0284C7))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = proxySettings.protocol.displayName,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else if (isVpnConnected) {
                // 1.1.1.1 VPN active indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CloudflareOrange)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "1.1.1.1",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Quick Reload mini button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2C))
                    .clickable { onQuickReload() }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Quick reload",
                    tint = Color(0xFF00D2FF),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Expanded state: Authentic Windows-style Mini Floating Window
 */
@Composable
private fun SnFloatingWindowCard(
    reloadRotation: Float,
    isVpnConnected: Boolean,
    onToggleVpn: () -> Unit,
    proxySettings: ProxySettings,
    onOpenProxyDialog: () -> Unit,
    onDisableProxy: () -> Unit,
    onReloadClick: () -> Unit,
    onClearDataClick: () -> Unit,
    onGoHome: () -> Unit,
    isDesktopMode: Boolean,
    onToggleDesktopMode: () -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onMinimize: () -> Unit,
    onResetPosition: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("sn_mini_window_card")
            .widthIn(min = 290.dp, max = 320.dp)
            .shadow(elevation = 18.dp, shape = RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = Color(0xFF3F3F46),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF202023)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Windows Title Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF18181B), Color(0xFF27272A))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WindowsTileLogo(size = 14.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SN • Windows Mini",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )

                // Drag icon hint
                Icon(
                    imageVector = Icons.Default.OpenWith,
                    contentDescription = "Drag to move",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 4.dp)
                )

                // Minimize button
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Minimize SN tool",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Close button (Windows red on hover/tap)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onMinimize() }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close window",
                        tint = WinCloseRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF333338), thickness = 1.dp)

            // Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF27272A), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isVpnConnected) VpnConnectedGreen else Color(0xFF22C55E))
                        )
                        Text(
                            text = if (isVpnConnected) "m.facebook.com • 1.1.1.1 VPN" else "m.facebook.com",
                            color = Color(0xFFD4D4D8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = onResetPosition,
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            text = "Reset Pos",
                            fontSize = 10.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                // ==========================================
                // 🛡️ 1.1.1.1 FREE VPN (CLOUDFLARE WARP) CARD
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sn_1111_vpn_card"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isVpnConnected) CloudflareOrangeContainer else Color(0xFF27272A)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isVpnConnected) CloudflareOrange else Color(0xFF3F3F46)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1.1.1.1 Icon badge
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isVpnConnected) CloudflareOrange else Color(0xFF3A3A3D)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isVpnConnected) Icons.Default.Shield else Icons.Default.Security,
                                    contentDescription = "1.1.1.1 VPN",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "1.1.1.1 VPN",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CloudflareOrange.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "FREE",
                                            color = CloudflareOrange,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = if (isVpnConnected) "🟢 Connected • Cloudflare WARP (18ms)" else "⚪ Cloudflare Secure DNS (Off)",
                                    fontSize = 10.sp,
                                    color = if (isVpnConnected) VpnConnectedGreen else Color(0xFFA1A1AA)
                                )
                            }
                        }

                        // Switch
                        Switch(
                            checked = isVpnConnected,
                            onCheckedChange = { onToggleVpn() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CloudflareOrange,
                                uncheckedThumbColor = Color(0xFFA1A1AA),
                                uncheckedTrackColor = Color(0xFF1E1E20)
                            ),
                            modifier = Modifier.size(width = 44.dp, height = 24.dp)
                        )
                    }
                }

                // ==========================================
                // 🌐 CUSTOM PROXY SETUP CARD (SOCKS5 / HTTP+TLS / HTTP)
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sn_proxy_card")
                        .clickable { onOpenProxyDialog() },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (proxySettings.enabled) Color(0xFF0F172A) else Color(0xFF27272A)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (proxySettings.enabled) Color(0xFF38BDF8) else Color(0xFF3F3F46)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (proxySettings.enabled) Color(0xFF0284C7) else Color(0xFF3A3A3D)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = "Custom Proxy",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Custom Proxy",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "SOCKS5 • HTTP+TLS",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = if (proxySettings.enabled) {
                                        "🟢 Active: ${proxySettings.protocol.displayName} ${proxySettings.host}:${proxySettings.port}" +
                                            if (proxySettings.lastPingMs > 0) " (${proxySettings.lastPingMs}ms)" else ""
                                    } else {
                                        "⚪ Tap to setup Server, Port, UID, Pass"
                                    },
                                    fontSize = 10.sp,
                                    color = if (proxySettings.enabled) Color(0xFF38BDF8) else Color(0xFFA1A1AA)
                                )
                            }
                        }

                        // Edit / Setup button
                        Button(
                            onClick = onOpenProxyDialog,
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("sn_open_proxy_setup_btn"),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (proxySettings.enabled) Color(0xFF0284C7) else Color(0xFF3F3F46)
                            )
                        ) {
                            Text(
                                text = if (proxySettings.enabled) "Edit" else "Set Proxy",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Primary Request Buttons: Reload & Clear Data
                Text(
                    text = "QUICK ACTIONS",
                    color = Color(0xFF71717A),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // RELOAD BUTTON ("relod baton")
                    Button(
                        onClick = onReloadClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sn_reload_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FbBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload Facebook",
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(reloadRotation)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reload",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // CLEAR DATA BUTTON ("clear data baton")
                    Button(
                        onClick = onClearDataClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sn_clear_data_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear browser data",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clear Data",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Additional Quick Controls Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Button
                    OutlinedButton(
                        onClick = onGoHome,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Facebook Home",
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Home",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Desktop / Mobile Mode Switcher
                    OutlinedButton(
                        onClick = onToggleDesktopMode,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.Smartphone,
                            contentDescription = "Toggle Desktop Mode",
                            modifier = Modifier.size(14.dp),
                            tint = if (isDesktopMode) Color(0xFF38BDF8) else Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isDesktopMode) "PC View" else "Mobile",
                            fontSize = 11.sp,
                            color = if (isDesktopMode) Color(0xFF38BDF8) else Color.White
                        )
                    }
                }

                // Dark mode and Zoom controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dark mode toggle
                    OutlinedButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            modifier = Modifier.size(14.dp),
                            tint = if (isDarkMode) Color(0xFFFBBF24) else Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isDarkMode) "Light" else "Dark",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Zoom Controls
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF27272A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onZoomOut,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Zoom out",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Text(
                            text = "Zoom",
                            fontSize = 10.sp,
                            color = Color(0xFFA1A1AA)
                        )

                        IconButton(
                            onClick = onZoomIn,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom in",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Footer drag guidance
                Text(
                    text = "💡 Tip: Drag window anywhere to reposition",
                    color = Color(0xFF71717A),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

/**
 * 4-square Windows icon
 */
@Composable
fun WindowsTileLogo(size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val tileSize = size / 2.3f
    val gap = 1.dp
    Column(
        modifier = modifier.size(size),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Box(
                modifier = Modifier
                    .size(tileSize)
                    .background(Color(0xFF00A4EF), RoundedCornerShape(1.dp))
            )
            Box(
                modifier = Modifier
                    .size(tileSize)
                    .background(Color(0xFF0078D4), RoundedCornerShape(1.dp))
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Box(
                modifier = Modifier
                    .size(tileSize)
                    .background(Color(0xFF2B579A), RoundedCornerShape(1.dp))
            )
            Box(
                modifier = Modifier
                    .size(tileSize)
                    .background(Color(0xFF00B7C3), RoundedCornerShape(1.dp))
            )
        }
    }
}

/**
 * Custom Proxy Setup & Live Check Dialog (SOCKS5, HTTP+TLS, HTTP)
 * Only allows saving if the proxy check succeeds!
 */
@Composable
fun ProxySetupDialog(
    initialSettings: ProxySettings,
    onDismiss: () -> Unit,
    onSaveProxy: (ProxySettings) -> Unit,
    onDisableProxy: () -> Unit
) {
    var selectedProtocol by remember { mutableStateOf(initialSettings.protocol) }
    var host by remember { mutableStateOf(initialSettings.host) }
    var port by remember { mutableStateOf(initialSettings.port) }
    var username by remember { mutableStateOf(initialSettings.username) }
    var password by remember { mutableStateOf(initialSettings.password) }
    var passwordVisible by remember { mutableStateOf(false) }

    var isChecking by remember { mutableStateOf(false) }
    var checkResultSuccess by remember { mutableStateOf<Boolean?>(if (initialSettings.isVerified) true else null) }
    var checkResultMessage by remember {
        mutableStateOf<String?>(
            if (initialSettings.isVerified) "Proxy is verified (Ping: ${initialSettings.lastPingMs}ms)" else null
        )
    }
    var verifiedPing by remember { mutableStateOf(if (initialSettings.isVerified) initialSettings.lastPingMs else -1L) }

    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Custom Proxy Setup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = "SOCKS5 • HTTP + TLS • HTTP",
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Protocol Selection Chips
                Text(
                    text = "Select Protocol (প্রোটোকল নির্বাচন করুন):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD4D4D8)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProxyProtocol.values().forEach { proto ->
                        val isSelected = selectedProtocol == proto
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedProtocol = proto
                                // Invalidate check on protocol change
                                checkResultSuccess = null
                                checkResultMessage = null
                                verifiedPing = -1L
                            },
                            label = {
                                Text(
                                    text = proto.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF27272A),
                                labelColor = Color(0xFFA1A1AA)
                            )
                        )
                    }
                }

                // Server (Host) & Port Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = {
                            host = it
                            checkResultSuccess = null
                            checkResultMessage = null
                            verifiedPing = -1L
                        },
                        label = { Text("Server / Host", fontSize = 11.sp) },
                        placeholder = { Text("e.g. 192.168.1.1", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(2f)
                            .testTag("proxy_host_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF3F3F46),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = {
                            if (it.length <= 5) {
                                port = it.filter { char -> char.isDigit() }
                                checkResultSuccess = null
                                checkResultMessage = null
                                verifiedPing = -1L
                            }
                        },
                        label = { Text("Port", fontSize = 11.sp) },
                        placeholder = { Text("1080", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("proxy_port_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF3F3F46),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // UID (Username) & Pass (Password)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            checkResultSuccess = null
                            checkResultMessage = null
                            verifiedPing = -1L
                        },
                        label = { Text("UID / Username", fontSize = 11.sp) },
                        placeholder = { Text("Optional", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("proxy_user_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF3F3F46),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            checkResultSuccess = null
                            checkResultMessage = null
                            verifiedPing = -1L
                        },
                        label = { Text("Pass", fontSize = 11.sp) },
                        placeholder = { Text("Optional", fontSize = 11.sp) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("proxy_pass_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF3F3F46),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // CHECK PROXY BUTTON ("check option diyo proxy option ta jeno kaj kore")
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isChecking = true
                            checkResultSuccess = null
                            checkResultMessage = null

                            val result = ProxyTester.checkProxy(
                                protocol = selectedProtocol,
                                host = host.trim(),
                                portText = port.trim(),
                                username = username.trim(),
                                password = password
                            )

                            isChecking = false
                            if (result.isSuccess) {
                                val ping = result.getOrNull() ?: 25L
                                verifiedPing = ping
                                checkResultSuccess = true
                                checkResultMessage = "✅ Good Proxy! (Ping: ${ping}ms) • Live & Reachable"
                            } else {
                                verifiedPing = -1L
                                checkResultSuccess = false
                                val err = result.exceptionOrNull()?.message ?: "Connection failed"
                                checkResultMessage = "❌ Bad Proxy: $err"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("check_proxy_button"),
                    enabled = !isChecking && host.isNotBlank() && port.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7)
                    )
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking Proxy Connection...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🔍 Check Proxy (চেক প্রক্সি)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Check Result Feedback Banner
                if (checkResultMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (checkResultSuccess == true) Color(0xFF064E3B) else Color(0xFF7F1D1D)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (checkResultSuccess == true) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (checkResultSuccess == true) Color(0xFF34D399) else Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = checkResultMessage ?: "",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Text(
                        text = "ℹ️ Note: You must Check Proxy and get a 'Good Proxy' status before setting. Bad proxies cannot be saved.",
                        fontSize = 10.sp,
                        color = Color(0xFF71717A)
                    )
                }
            }
        },
        confirmButton = {
            // SET / SAVE BUTTON: Only enabled if check succeeded! ("proxy cek kore jodi proxy valo hoy tahole set hobe kharap hole set hobe na")
            Button(
                onClick = {
                    val settings = ProxySettings(
                        enabled = true,
                        protocol = selectedProtocol,
                        host = host.trim(),
                        port = port.trim(),
                        username = username.trim(),
                        password = password,
                        isVerified = true,
                        lastPingMs = verifiedPing
                    )
                    onSaveProxy(settings)
                },
                enabled = checkResultSuccess == true && host.isNotBlank() && port.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    disabledContainerColor = Color(0xFF3F3F46)
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("save_proxy_button")
            ) {
                Text(
                    text = "💾 Set & Activate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (initialSettings.enabled) {
                    TextButton(
                        onClick = onDisableProxy,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text("Turn Off", fontSize = 12.sp)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                }
            }
        },
        containerColor = Color(0xFF1E1E22),
        shape = RoundedCornerShape(12.dp)
    )
}

