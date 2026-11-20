package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FbBlue

@Composable
fun NushaTopBar(
    accountCount: Int,
    isMultiSyncEnabled: Boolean,
    onSelectAccountCount: (Int) -> Unit,
    onSetMultiSync: (Boolean) -> Unit,
    onReloadAll: () -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF0A0B0D),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand Identity: Black + Facebook 'f' + Nusha Lite 2
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.dp, Color(0xFF2B2D33), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(FbBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "f",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Nusha Lite 2",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = if (accountCount == 1) {
                                "Facebook"
                            } else {
                                "$accountCount Accounts • " + if (isMultiSyncEnabled) "Multi ON" else "Manual OFF"
                            },
                            color = if (accountCount > 1 && isMultiSyncEnabled) {
                                Color(0xFF10B981)
                            } else if (accountCount > 1) {
                                Color(0xFFF59E0B)
                            } else {
                                Color(0xFF9CA3AF)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 13.sp
                        )
                    }
                }

                // Right Actions: Quick Multiple ON/OFF Pill (when 2-4 active) + 3-Dot Menu
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (accountCount > 1) {
                        Surface(
                            onClick = { onSetMultiSync(!isMultiSyncEnabled) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isMultiSyncEnabled) {
                                Color(0xFF10B981).copy(alpha = 0.18f)
                            } else {
                                Color(0xFFF59E0B).copy(alpha = 0.16f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isMultiSyncEnabled) Color(0xFF10B981) else Color(0xFFF59E0B)
                            ),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("multi_sync_quick_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMultiSyncEnabled) Icons.Default.Sync else Icons.Default.SyncDisabled,
                                    contentDescription = "Multiple Sync Status",
                                    tint = if (isMultiSyncEnabled) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isMultiSyncEnabled) "Multi ON" else "Manual",
                                    color = if (isMultiSyncEnabled) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("three_dot_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = Color(0xFF18191C),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF2D2F36)),
                            modifier = Modifier.widthIn(min = 248.dp, max = 280.dp)
                        ) {
                            // 1. Multiple Account (2-4) + 1 Single
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Multiple Account (2–4)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Select how many Facebook pages to open",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(1, 2, 3, 4).forEach { count ->
                                        val selected = accountCount == count
                                        Surface(
                                            onClick = {
                                                onSelectAccountCount(count)
                                                menuExpanded = false
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selected) FbBlue else Color(0xFF24262B),
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) FbBlue else Color(0xFF3A3D45)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .testTag("multi_count_$count")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = if (count == 1) "1" else "${count}x",
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF2A2C32))

                            // 2. Multiple Sync ON / OFF Button
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Multiple Mode",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = when {
                                                accountCount == 1 -> "Open 2–4 accounts to sync"
                                                isMultiSyncEnabled -> "ON: 1 action repeats on all"
                                                else -> "OFF: Manual touch one by one"
                                            },
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // OFF (Manual)
                                    val isOff = !isMultiSyncEnabled
                                    Surface(
                                        onClick = {
                                            onSetMultiSync(false)
                                            menuExpanded = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isOff) Color(0xFFF59E0B).copy(alpha = 0.22f) else Color(0xFF24262B),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isOff) Color(0xFFF59E0B) else Color(0xFF3A3D45)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("menu_multi_off")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "OFF (Manual)",
                                                color = if (isOff) Color(0xFFFBBF24) else Color(0xFFD1D5DB),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // ON (Auto Multi)
                                    val isOn = isMultiSyncEnabled && accountCount > 1
                                    Surface(
                                        onClick = {
                                            if (accountCount == 1) {
                                                onSelectAccountCount(2)
                                            }
                                            onSetMultiSync(true)
                                            menuExpanded = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isOn) Color(0xFF10B981).copy(alpha = 0.22f) else Color(0xFF24262B),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isOn) Color(0xFF10B981) else Color(0xFF3A3D45)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("menu_multi_on")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "ON (Sync)",
                                                color = if (isOn) Color(0xFF34D399) else Color(0xFFD1D5DB),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF2A2C32))

                            // 3. Reload Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        menuExpanded = false
                                        onReloadAll()
                                    }
                                    .padding(horizontal = 14.dp)
                                    .testTag("menu_reload_button"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Reload",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // 4. Clear Data Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        menuExpanded = false
                                        onClearAllData()
                                    }
                                    .padding(horizontal = 14.dp)
                                    .testTag("menu_clear_data_button"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Data",
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Clear Data",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF22242A), thickness = 1.dp)
        }
    }
}
