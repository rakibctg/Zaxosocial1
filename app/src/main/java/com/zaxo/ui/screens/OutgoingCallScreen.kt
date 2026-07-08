package com.zaxo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaxo.data.local.CallRecordEntity
import com.zaxo.ui.neumorphic.NeuTheme
import com.zaxo.ui.viewmodel.CallState
import com.zaxo.ui.viewmodel.CallViewModel
import kotlinx.coroutines.delay

@Composable
fun OutgoingCallScreen(
    viewModel: CallViewModel,
    record: CallRecordEntity,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val callState by viewModel.callState.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isCameraOff by viewModel.isCameraOff.collectAsState()

    // Pulsing radial glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_radial_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Animated dots cycle for Calling...
    var dots by remember { mutableStateOf(".") }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dots = when (dots) {
                "." -> ".."
                ".." -> "..."
                else -> "."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1D23)) // Rich Slate Dark Background
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Pulsing Avatar Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseScale)
                        .background(NeuTheme.AccentColor.copy(alpha = 0.15f), CircleShape)
                )
                // Inner glow
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale * 0.9f)
                        .background(NeuTheme.AccentColor.copy(alpha = 0.25f), CircleShape)
                )
                // Contact Avatar
                AsyncImage(
                    model = record.calleeAvatar,
                    contentDescription = "Callee Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            }

            // Peer Identity Details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = record.calleeName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (callState) {
                        CallState.VALIDATING -> "Validating Connection..."
                        CallState.CREATING_ROOM -> "Connecting..."
                        CallState.SENDING_PUSH -> "Connecting peers..."
                        CallState.DIALING -> "Calling$dots"
                        CallState.RINGING -> "Ringing..."
                        CallState.CONNECTING -> "Connecting..."
                        else -> "Dialing..."
                    },
                    color = Color(0xFFB0B5BD),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MuteButton(
                    isMuted = isMuted,
                    onClick = { viewModel.toggleMute() },
                    darkTheme = true
                )

                SpeakerButton(
                    isSpeakerOn = isSpeakerOn,
                    onClick = { viewModel.toggleSpeaker() },
                    darkTheme = true
                )

                VideoToggleButton(
                    isVideoOff = isCameraOff,
                    onClick = { viewModel.toggleCamera() },
                    darkTheme = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            EndCallButton(
                onClick = { viewModel.endCall() },
                darkTheme = true
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
