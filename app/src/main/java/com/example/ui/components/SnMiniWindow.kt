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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.FbBlue
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
    modifier: Modifier = Modifier
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var isReloadingAnim by remember { mutableStateOf(false) }

    val reloadRotation by animateFloatAsState(
        targetValue = if (isReloadingAnim) 360f else 0f,
        animationSpec = tween(durationMillis = 600),
        finishedListener = { isReloadingAnim = false },
        label = "ReloadRotation"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxAvailableWidthPx = with(density) { maxWidth.toPx() }
        val maxAvailableHeightPx = with(density) { maxHeight.toPx() }

        // Initial position at top-right
        var offsetX by remember { mutableFloatStateOf(maxAvailableWidthPx - with(density) { 140.dp.toPx() }) }
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
                        offsetX = maxAvailableWidthPx - with(density) { 140.dp.toPx() }
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
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(WinHeaderBlue, FbBlue, Color(0xFF00C6FF))
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

            // Windows Logo Symbol (4 small squares in blue/cyan)
            WindowsTileLogo(size = 15.dp)

            // "SN" Name Tag
            Text(
                text = "SN",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            )

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
            .widthIn(min = 280.dp, max = 310.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(12.dp))
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
                                .background(Color(0xFF22C55E))
                        )
                        Text(
                            text = "m.facebook.com",
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
