package com.zaxo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaxo.data.local.CallRecordEntity
import com.zaxo.data.local.ContactEntity
import com.zaxo.ui.neumorphic.NeuTheme
import com.zaxo.ui.neumorphic.NeumorphicIconButton
import com.zaxo.ui.viewmodel.CallViewModel

@Composable
fun PostCallScreen(
    viewModel: CallViewModel,
    record: CallRecordEntity,
    onNavigateToChat: (String) -> Unit,
    onDismiss: () -> Unit,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val bgColor = NeuTheme.getBgColor(darkTheme)
    val textColor = NeuTheme.getTextColor(darkTheme)

    var showContactPrompt by remember { mutableStateOf(true) }

    val formattedDuration = remember(record.durationSeconds) {
        val minutes = record.durationSeconds / 60
        val seconds = record.durationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
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
        Spacer(modifier = Modifier.height(24.dp))

        // Call status banner
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = if (record.callerId == "me") record.calleeAvatar else record.callerAvatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
            )

            Text(
                text = "Call Finished",
                color = textColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Secure Call with ${if (record.callerId == "me") record.calleeName else record.callerName}",
                color = NeuTheme.getSecondaryTextColor(darkTheme),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .background(NeuTheme.AccentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Duration: $formattedDuration",
                    color = NeuTheme.AccentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Add Contact Suggestion Dialog/Section for unrecognized numbers
        if (showContactPrompt && record.durationSeconds > 10L) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeuTheme.AccentColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Add to Contacts?",
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Would you like to save this number to your contacts for easy access?",
                        color = NeuTheme.getSecondaryTextColor(darkTheme),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { showContactPrompt = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Gray)
                        ) {
                            Text("Not Now")
                        }

                        Button(
                            onClick = {
                                showContactPrompt = false
                                viewModel.flipCamera() // Simulate Contact added successfully
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeuTheme.AccentColor)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Add Contact")
                            }
                        }
                    }
                }
            }
        }

        // Primary actions: message or recall
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Message Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NeumorphicIconButton(
                    onClick = {
                        val peerId = if (record.callerId == "me") record.calleeId else record.callerId
                        onNavigateToChat(peerId)
                    },
                    darkTheme = darkTheme,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = "Message", tint = NeuTheme.AccentColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Message", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }

            // Recall Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NeumorphicIconButton(
                    onClick = {
                        val peer = ContactEntity(
                            id = if (record.callerId == "me") record.calleeId else record.callerId,
                            name = if (record.callerId == "me") record.calleeName else record.callerName,
                            zaxoNumber = "",
                            avatar = if (record.callerId == "me") record.calleeAvatar else record.callerAvatar
                        )
                        viewModel.startCall(peer, record.isVideo)
                    },
                    darkTheme = darkTheme,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = "Recall", tint = NeuTheme.AccentColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Recall", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Return to main button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeuTheme.AccentColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Back to Chats", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
