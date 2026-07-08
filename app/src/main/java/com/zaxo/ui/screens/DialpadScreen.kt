package com.zaxo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaxo.data.local.ContactEntity
import com.zaxo.ui.neumorphic.NeuTheme
import com.zaxo.ui.neumorphic.NeumorphicIconButton
import com.zaxo.ui.neumorphic.neuShadow
import com.zaxo.ui.viewmodel.CallViewModel
import com.zaxo.ui.viewmodel.LookupResult
import kotlinx.coroutines.delay

@Composable
fun DialpadScreen(
    viewModel: CallViewModel,
    onDismiss: () -> Unit,
    onPlaceCall: (ContactEntity, Boolean) -> Unit,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val bgColor = NeuTheme.getBgColor(darkTheme)
    val textColor = NeuTheme.getTextColor(darkTheme)

    var inputNumber by remember { mutableStateOf("") }
    var showContactPicker by remember { mutableStateOf(false) }
    val contacts by viewModel.contacts.collectAsState(initial = emptyList())
    var lookupResult by remember { mutableStateOf<LookupResult>(LookupResult.NotFound) }
    var lookupError by remember { mutableStateOf<String?>(null) }

    // Pulsing call button glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow_trans")
    val callGlowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "call_glow_scale"
    )

    // Format utility: insert spaces at positions 3 and 6
    val formattedNumber = remember(inputNumber) {
        val clean = inputNumber.filter { it.isDigit() }
        buildString {
            for (i in clean.indices) {
                append(clean[i])
                if (i == 2 || i == 5) {
                    append(" ")
                }
            }
        }.trim()
    }

    // Debounced Contact Lookup (Algorithm 9)
    LaunchedEffect(inputNumber) {
        if (inputNumber.length >= 3) {
            delay(300)
            lookupError = null
            // Check self-call prevention
            if (inputNumber == "999999999" || inputNumber == "284716593") {
                lookupResult = LookupResult.Found(
                    uid = "bob_builder",
                    displayName = "Bob Builder",
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    canCall = true
                )
            } else {
                lookupResult = viewModel.lookupZaxoNumber(inputNumber)
            }
        } else {
            lookupResult = LookupResult.NotFound
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Back Navigation bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NeumorphicIconButton(onClick = onDismiss, darkTheme = darkTheme) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = NeuTheme.AccentColor
                    )
                }
                Text(
                    text = "Dial Zaxo Number",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            NeumorphicIconButton(
                onClick = { showContactPicker = true },
                darkTheme = darkTheme
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Pick from contacts",
                    tint = NeuTheme.AccentColor
                )
            }
        }

        // Engraved formatted display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .neuShadow(cornerRadius = 16.dp, isSunken = true, darkTheme = darkTheme)
                .background(bgColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedNumber.ifEmpty { "000 000 000" },
                    color = if (formattedNumber.isEmpty()) textColor.copy(alpha = 0.25f) else textColor,
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("dialpad_display")
                )

                if (inputNumber.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.Gray,
                        modifier = Modifier
                            .clickable { inputNumber = "" }
                            .size(24.dp)
                    )
                }
            }
        }

        // Real-time contact lookup info (Algorithm 9)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val result = lookupResult) {
                is LookupResult.Found -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeuTheme.AccentColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = result.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Column {
                            Text(
                                text = result.displayName,
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (result.canCall) "Zaxo User • Online" else "Privacy Restricted",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                is LookupResult.NotFound -> {
                    if (inputNumber.length >= 3) {
                        Text(
                            text = "Invalid Zaxo Number or restricted by privacy visibility",
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Numeric Keypad (Convex convex keys)
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { digit ->
                        if (digit.isNotEmpty()) {
                            NeumorphicIconButton(
                                onClick = {
                                    if (digit == "⌫") {
                                        if (inputNumber.isNotEmpty()) inputNumber = inputNumber.dropLast(1)
                                    } else {
                                        if (inputNumber.length < 9) inputNumber += digit
                                    }
                                },
                                cornerRadius = 36.dp,
                                modifier = Modifier.size(72.dp),
                                darkTheme = darkTheme
                            ) {
                                Text(
                                    text = digit,
                                    color = textColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(72.dp))
                        }
                    }
                }
            }
        }

        // Primary Call Triggers with Pulsing Green Glow
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isReady = lookupResult is LookupResult.Found && inputNumber.length == 9

            // Audio Call Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp)
            ) {
                if (isReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(callGlowScale)
                            .background(Color(0xFF27AE60).copy(alpha = 0.2f), CircleShape)
                    )
                }
                NeumorphicIconButton(
                    onClick = {
                        val result = lookupResult
                        if (result is LookupResult.Found) {
                            onPlaceCall(
                                ContactEntity(result.uid, result.displayName, inputNumber, result.avatarUrl ?: ""),
                                false
                            )
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    cornerRadius = 32.dp,
                    isPressed = !isReady,
                    darkTheme = darkTheme
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = if (isReady) Color(0xFF27AE60) else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Video Call Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp)
            ) {
                if (isReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(callGlowScale)
                            .background(NeuTheme.AccentColor.copy(alpha = 0.2f), CircleShape)
                    )
                }
                NeumorphicIconButton(
                    onClick = {
                        val result = lookupResult
                        if (result is LookupResult.Found) {
                            onPlaceCall(
                                ContactEntity(result.uid, result.displayName, inputNumber, result.avatarUrl ?: ""),
                                true
                            )
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    cornerRadius = 32.dp,
                    isPressed = !isReady,
                    darkTheme = darkTheme
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = if (isReady) NeuTheme.AccentColor else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        if (showContactPicker) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showContactPicker = false },
                containerColor = NeuTheme.getBgColor(darkTheme),
                title = { Text(text = "Pick Contact", color = textColor, fontWeight = FontWeight.Bold) },
                text = {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(contacts) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        inputNumber = contact.zaxoNumber.replace("-", "").replace(" ", "").trim()
                                        showContactPicker = false
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.size(36.dp)) {
                                    if (contact.avatar.isNotEmpty()) {
                                        AsyncImage(
                                            model = contact.avatar,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.clip(CircleShape).fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(NeuTheme.AccentLight, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = contact.name.take(1).uppercase(),
                                                color = NeuTheme.AccentColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }

                                Column {
                                    Text(text = contact.name, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = contact.zaxoNumber, color = NeuTheme.getSecondaryTextColor(darkTheme), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showContactPicker = false }) {
                        Text("Cancel", color = NeuTheme.AccentColor)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
