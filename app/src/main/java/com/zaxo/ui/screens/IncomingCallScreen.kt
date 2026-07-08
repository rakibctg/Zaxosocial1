package com.zaxo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaxo.data.local.CallRecordEntity
import com.zaxo.ui.neumorphic.NeuTheme
import com.zaxo.ui.neumorphic.NeumorphicIconButton
import com.zaxo.ui.viewmodel.CallViewModel

@Composable
fun IncomingCallScreen(
    viewModel: CallViewModel,
    record: CallRecordEntity,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    var showQuickReplies by remember { mutableStateOf(false) }

    val quickReplies = listOf(
        "Can't talk right now.",
        "In a meeting.",
        "Call you back soon.",
        "Text me instead.",
        "Driving — call you later."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1D23))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Peer Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                AsyncImage(
                    model = record.callerAvatar,
                    contentDescription = "Caller Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            }

            // Identity Metadata
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = record.callerName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                // Caller ID Zaxo Number format XXX XXX XXX
                Text(
                    text = "Incoming VoIP Secure Call",
                    color = Color(0xFFB0B5BD),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1.0f))

            if (showQuickReplies) {
                // Quick Replies layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF232830), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Quick Response",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    quickReplies.forEach { reply ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.declineCall(reply) }
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                                .background(Color(0xFF1A1D23), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = reply,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Text(
                        text = "Decline without message",
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.declineCall() }
                            .padding(vertical = 8.dp)
                    )
                }
            } else {
                // Accept/Decline primary triggers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline Trigger
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeumorphicIconButton(
                            onClick = { showQuickReplies = true },
                            darkTheme = true,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("decline_call_button"),
                            cornerRadius = 32.dp
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline Call",
                                tint = Color.Red,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Decline",
                            color = Color(0xFFB0B5BD),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Accept Trigger
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeumorphicIconButton(
                            onClick = { viewModel.answerCall() },
                            darkTheme = true,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("accept_call_button"),
                            cornerRadius = 32.dp
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Accept Call",
                                tint = Color(0xFF27AE60),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Accept",
                            color = Color(0xFFB0B5BD),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
