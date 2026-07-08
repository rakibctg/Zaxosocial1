package com.zaxo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaxo.data.local.CallRecordEntity
import com.zaxo.ui.neumorphic.NeuTheme
import com.zaxo.ui.neumorphic.NeumorphicIconButton
import com.zaxo.ui.viewmodel.CallState
import com.zaxo.ui.viewmodel.CallViewModel
import kotlin.math.roundToInt

@Composable
fun ActiveCallScreen(
    viewModel: CallViewModel,
    record: CallRecordEntity,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val callState by viewModel.callState.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isCameraOff by viewModel.isCameraOff.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val timerString by viewModel.callTimerString.collectAsState()
    val networkQualityMessage by viewModel.networkQualityMessage.collectAsState()

    // Drag offset tracking for local video PiP
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Avatar pulse in audio call
    val infiniteTransition = rememberInfiniteTransition(label = "audio_glow")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141218)) // Dark theme background
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        if (record.isVideo && !isCameraOff) {
            // --- VIDEO LAYOUT ---
            // Full screen remote video simulator
            AsyncImage(
                model = record.calleeAvatar,
                contentDescription = "Remote video stream",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Connection Quality alert banner overlay
            networkQualityMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(Color(0xFFF39C12).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = msg, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Draggable snap-to-corner Local Picture-in-Picture window
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(110.dp, 150.dp)
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .background(Color.Black, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    contentDescription = "Local PiP Feed",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = "You",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        } else {
            // --- AUDIO LAYOUT ---
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // Encryption and title details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = "Secured",
                            tint = NeuTheme.AccentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "PRIVATE",
                            color = NeuTheme.AccentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = if (callState == CallState.HELD) "Call On Hold" else "Voice Call",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = timerString,
                        color = Color.Green,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Pulsing Avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(180.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(scalePulse)
                            .background(NeuTheme.AccentColor.copy(alpha = 0.12f), CircleShape)
                    )
                    AsyncImage(
                        model = if (record.callerId == "me") record.calleeAvatar else record.callerAvatar,
                        contentDescription = "Active Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                    )
                }

                Text(
                    text = if (record.callerId == "me") record.calleeName else record.callerName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1.0f))
            }
        }

        // Overlay control buttons bottom pane
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Main toggle rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MuteButton(isMuted = isMuted, onClick = { viewModel.toggleMute() }, darkTheme = true)
                    SpeakerButton(isSpeakerOn = isSpeakerOn, onClick = { viewModel.toggleSpeaker() }, darkTheme = true)
                    VideoToggleButton(isVideoOff = isCameraOff, onClick = { viewModel.toggleCamera() }, darkTheme = true)
                    FlipCameraButton(onClick = { viewModel.flipCamera() }, darkTheme = true)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hold resume toggle
                    NeumorphicIconButton(
                        onClick = { viewModel.toggleHold() },
                        darkTheme = true,
                        cornerRadius = 12.dp,
                        isPressed = callState == CallState.HELD
                    ) {
                        Text(
                            text = if (callState == CallState.HELD) "RESUME" else "HOLD",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    EndCallButton(onClick = { viewModel.endCall() }, darkTheme = true)
                }
            }
        }

        // --- ALGORITHM 10: CALL WAITING OVERLAY ---
        if (callState == CallState.CALL_WAITING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF232830), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Call Waiting...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Another secure call is incoming.",
                        color = Color(0xFFB0B5BD),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.declineWaitingCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reject", color = Color.White)
                        }
                        Button(
                            onClick = { viewModel.acceptWaitingCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hold & Accept", color = Color.White)
                        }
                    }

                    Button(
                        onClick = { viewModel.endCurrentAndAcceptWaiting() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("End Current & Accept", color = Color.Black)
                    }
                }
            }
        }
    }
}
