package com.zaxo.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaxo.ui.neumorphic.NeuTheme
import com.zaxo.ui.neumorphic.NeumorphicIconButton

@Composable
fun CallControlItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    activeColor: Color = NeuTheme.AccentColor,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1.0f,
        animationSpec = spring(),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        NeumorphicIconButton(
            onClick = onClick,
            isPressed = isSelected,
            darkTheme = darkTheme,
            modifier = Modifier
                .size(56.dp)
                .testTag(testTag),
            cornerRadius = 28.dp
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) activeColor else NeuTheme.getSecondaryTextColor(darkTheme),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            color = if (isSelected) activeColor else NeuTheme.getSecondaryTextColor(darkTheme),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MuteButton(
    isMuted: Boolean,
    onClick: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    CallControlItem(
        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
        label = if (isMuted) "Muted" else "Mute",
        onClick = onClick,
        isSelected = isMuted,
        activeColor = Color.Red,
        darkTheme = darkTheme,
        modifier = modifier,
        testTag = "mute_button"
    )
}

@Composable
fun SpeakerButton(
    isSpeakerOn: Boolean,
    onClick: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    CallControlItem(
        icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
        label = if (isSpeakerOn) "Speaker" else "Earpiece",
        onClick = onClick,
        isSelected = isSpeakerOn,
        activeColor = Color(0xFF27AE60),
        darkTheme = darkTheme,
        modifier = modifier,
        testTag = "speaker_button"
    )
}

@Composable
fun VideoToggleButton(
    isVideoOff: Boolean,
    onClick: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    CallControlItem(
        icon = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
        label = if (isVideoOff) "Video Off" else "Video",
        onClick = onClick,
        isSelected = isVideoOff,
        activeColor = Color.Red,
        darkTheme = darkTheme,
        modifier = modifier,
        testTag = "video_toggle_button"
    )
}

@Composable
fun FlipCameraButton(
    onClick: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    CallControlItem(
        icon = Icons.Default.FlipCameraAndroid,
        label = "Flip",
        onClick = onClick,
        darkTheme = darkTheme,
        modifier = modifier,
        testTag = "flip_camera_button"
    )
}

@Composable
fun SwapCallButton(
    onClick: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    CallControlItem(
        icon = Icons.Default.SwapCalls,
        label = "Swap",
        onClick = onClick,
        darkTheme = darkTheme,
        modifier = modifier,
        testTag = "swap_calls_button"
    )
}

@Composable
fun AddParticipantButton(
    onClick: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    CallControlItem(
        icon = Icons.Default.PersonAdd,
        label = "Add Participant",
        onClick = onClick,
        darkTheme = darkTheme,
        modifier = modifier,
        testTag = "add_participant_button"
    )
}

@Composable
fun EndCallButton(
    onClick: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    NeumorphicIconButton(
        onClick = onClick,
        darkTheme = darkTheme,
        modifier = modifier
            .size(64.dp)
            .testTag("end_call_button"),
        cornerRadius = 32.dp
    ) {
        Icon(
            imageVector = Icons.Default.CallEnd,
            contentDescription = "End Call",
            tint = Color.Red,
            modifier = Modifier.size(32.dp)
        )
    }
}
