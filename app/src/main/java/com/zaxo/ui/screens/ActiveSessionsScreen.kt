package com.zaxo.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaxo.data.local.DeviceEntity
import com.zaxo.ui.neumorphic.*
import com.zaxo.ui.viewmodel.ActiveSessionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActiveSessionsOverlay(
    viewModel: ActiveSessionsViewModel,
    darkTheme: Boolean,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)
    val bgColor = NeuTheme.getBgColor(darkTheme)
    val context = LocalContext.current

    var showDetailsDialog by remember { mutableStateOf<DeviceEntity?>(null) }
    var showRevokeAllConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(enabled = false) {} // block clickthrough
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // BACK & TITLE HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeumorphicIconButton(
                    onClick = onBack,
                    darkTheme = darkTheme,
                    cornerRadius = 12.dp
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }

                Text(
                    text = "Active Devices",
                    color = textColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = "Manage all phone, computer, and tablet sessions currently authorized to transmit on your Zaxo Number channels.",
                color = secondaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // LOADING AND SESSIONS LIST
            if (state.isLoading) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeuTheme.AccentColor)
                }
            } else if (state.sessions.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No sessions active", color = secondaryText, fontSize = 14.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Current device card (Section 17.1)
                    val currentDevice = state.sessions.firstOrNull { it.isCurrent }
                    if (currentDevice != null) {
                        Text(
                            text = "THIS DEVICE",
                            color = NeuTheme.AccentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )

                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth().clickable { showDetailsDialog = currentDevice },
                            cornerRadius = 20.dp,
                            darkTheme = darkTheme
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(NeuTheme.AccentLight, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = NeuTheme.AccentColor)
                                    }
                                    Column {
                                        Text(currentDevice.name, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text("${currentDevice.location} • Active Now", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Icon(imageVector = Icons.Default.Info, contentDescription = "Details", tint = secondaryText)
                            }
                        }
                    }

                    // Other active sessions card (Section 17.2)
                    val otherDevices = state.sessions.filter { !it.isCurrent }
                    if (otherDevices.isNotEmpty()) {
                        Text(
                            text = "OTHER SESSIONS",
                            color = NeuTheme.AccentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )

                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            darkTheme = darkTheme
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                otherDevices.forEach { device ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showDetailsDialog = device }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        if (darkTheme) Color(0xFF2E2A36) else Color(0xFFF3EDF7),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (device.name.contains("MacBook") || device.name.contains("PC")) {
                                                        Icons.Default.Laptop
                                                    } else {
                                                        Icons.Default.TabletAndroid
                                                    },
                                                    contentDescription = null,
                                                    tint = textColor
                                                )
                                            }
                                            Column {
                                                Text(device.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                                Text("${device.location} • Seen ${sdf.format(Date(device.lastActive))}", color = secondaryText, fontSize = 11.sp)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            IconButton(
                                                onClick = { viewModel.revokeSession(device.id) }
                                            ) {
                                                Icon(imageVector = Icons.Default.Cancel, contentDescription = "Revoke", tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Logout all other sessions button (Section 17.5)
                    if (otherDevices.isNotEmpty()) {
                        NeumorphicButton(
                            onClick = { showRevokeAllConfirm = true },
                            darkTheme = darkTheme,
                            cornerRadius = 12.dp
                        ) {
                            Icon(imageVector = Icons.Default.Dangerous, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out All Other Devices", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Session Detail Dialog
        if (showDetailsDialog != null) {
            val device = showDetailsDialog!!
            AlertDialog(
                onDismissRequest = { showDetailsDialog = null },
                title = { Text(device.name, color = textColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SessionDetailRow(label = "Hardware Fingerprint", value = device.id.take(16) + "...", darkTheme = darkTheme)
                        SessionDetailRow(label = "Location Resolved", value = device.location, darkTheme = darkTheme)
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        SessionDetailRow(label = "Last Active Timestamp", value = sdf.format(Date(device.lastActive)), darkTheme = darkTheme)
                        SessionDetailRow(label = "Platform OS", value = "Android 14 (API 34)", darkTheme = darkTheme)
                        SessionDetailRow(label = "Protocol Client Version", value = "6.0.0-Stable", darkTheme = darkTheme)
                    }
                },
                confirmButton = {
                    if (!device.isCurrent) {
                        Button(
                            onClick = {
                                viewModel.revokeSession(device.id)
                                showDetailsDialog = null
                                Toast.makeText(context, "Session revoked!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Log Out Device", color = Color.White)
                        }
                    } else {
                        TextButton(onClick = { showDetailsDialog = null }) {
                            Text("Dismiss", color = NeuTheme.AccentColor)
                        }
                    }
                },
                dismissButton = {
                    if (!device.isCurrent) {
                        TextButton(onClick = { showDetailsDialog = null }) {
                            Text("Close", color = textColor)
                        }
                    }
                }
            )
        }

        // Revoke All Confirm
        if (showRevokeAllConfirm) {
            AlertDialog(
                onDismissRequest = { showRevokeAllConfirm = false },
                title = { Text("Log Out Other Sessions?", color = Color.Red, fontWeight = FontWeight.Bold) },
                text = { Text("Confirm signing out of all logged sessions except this device.", color = textColor, fontSize = 13.sp) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.revokeAllOtherSessions()
                        showRevokeAllConfirm = false
                        Toast.makeText(context, "Other sessions revoked!", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Sign Out", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRevokeAllConfirm = false }) {
                        Text("Cancel", color = textColor)
                    }
                }
            )
        }
    }
}

@Composable
fun SessionDetailRow(label: String, value: String, darkTheme: Boolean) {
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = textColor, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}
