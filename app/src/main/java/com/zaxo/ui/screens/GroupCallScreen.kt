package com.zaxo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaxo.data.livekit.LiveKitParticipant
import com.zaxo.ui.neumorphic.NeuTheme
import com.zaxo.ui.viewmodel.CallViewModel

@Composable
fun GroupCallScreen(
    viewModel: CallViewModel,
    groupId: String,
    isVideo: Boolean,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val participants by viewModel.groupParticipants.collectAsState()
    val activeSpeakerId by viewModel.activeSpeakerId.collectAsState()
    val timerString by viewModel.callTimerString.collectAsState()

    // Default simulation list if empty
    val visibleParticipants = if (participants.isEmpty()) {
        listOf(
            LiveKitParticipant("1", "John Doe", isSpeaking = true),
            LiveKitParticipant("2", "Sarah Jenkins"),
            LiveKitParticipant("3", "Mike Peterson", isMuted = true),
            LiveKitParticipant("4", "Anna Kovalenko"),
            LiveKitParticipant("5", "Tom Hanks")
        )
    } else participants

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141218))
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Group",
                        tint = NeuTheme.AccentColor
                    )
                    Text(
                        text = "Team Chat",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = timerString,
                    color = Color.Green,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Grid Layout (Max 6 visible, adaptive, scrolling)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1.0f)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visibleParticipants) { participant ->
                    val isActiveSpeaker = participant.isSpeaking || participant.sid == activeSpeakerId
                    val borderStroke = if (isActiveSpeaker) {
                        BorderStroke(3.dp, Color(0xFF27AE60))
                    } else {
                        BorderStroke(1.dp, Color(0xFF32363D))
                    }

                    Card(
                        modifier = Modifier
                            .aspectRatio(0.95f)
                            .border(borderStroke, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF232830))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isVideo && participant.videoTrackEnabled) {
                                // Simulate Video Feed
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // Audio Avatar
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(Color(0xFF141218), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = participant.name.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                    Text(
                                        text = participant.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Individual mute / speaking overlays
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (participant.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (participant.isMuted) Color.Red else Color.Green,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Controls overlay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MuteButton(isMuted = false, onClick = { viewModel.toggleMute() }, darkTheme = true)
                SpeakerButton(isSpeakerOn = true, onClick = { viewModel.toggleSpeaker() }, darkTheme = true)
                AddParticipantButton(onClick = { viewModel.flipCamera() }, darkTheme = true)
                EndCallButton(onClick = { viewModel.endCall() }, darkTheme = true)
            }
        }
    }
}
