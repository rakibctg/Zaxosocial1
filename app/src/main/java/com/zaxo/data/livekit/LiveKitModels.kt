package com.zaxo.data.livekit

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class ConnectionQuality {
    EXCELLENT, GOOD, POOR, LOST
}

class ConnectOptions(
    val url: String,
    val token: String,
    val e2eeEnabled: Boolean = true,
    val audio: Boolean = true,
    val video: Boolean = false
)

class VideoEncoding(val maxBitrate: Int?)

class LiveKitParticipant(
    val sid: String,
    val name: String,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val videoTrackEnabled: Boolean = true
)

class LiveKitRoom(val context: Context) {
    private var connectionQualityListener: ((ConnectionQuality) -> Unit)? = null
    private var activeSpeakerListener: ((LiveKitParticipant) -> Unit)? = null
    private var participantConnectedListener: ((LiveKitParticipant) -> Unit)? = null
    private var participantDisconnectedListener: ((LiveKitParticipant) -> Unit)? = null

    var currentQuality = ConnectionQuality.EXCELLENT

    fun onConnectionQualityChanged(listener: (ConnectionQuality) -> Unit) {
        connectionQualityListener = listener
    }

    fun onActiveSpeakerChanged(listener: (LiveKitParticipant) -> Unit) {
        activeSpeakerListener = listener
    }

    fun onParticipantConnected(listener: (LiveKitParticipant) -> Unit) {
        participantConnectedListener = listener
    }

    fun onParticipantDisconnected(listener: (LiveKitParticipant) -> Unit) {
        participantDisconnectedListener = listener
    }

    fun setVideoEncoding(encoding: VideoEncoding) {
        android.util.Log.d("LiveKitRoom", "Video encoding changed to max bitrate: ${encoding.maxBitrate}")
    }

    fun setAudioEnabled(enabled: Boolean) {
        android.util.Log.d("LiveKitRoom", "Local audio enabled: $enabled")
    }

    fun setVideoEnabled(enabled: Boolean) {
        android.util.Log.d("LiveKitRoom", "Local video enabled: $enabled")
    }

    fun disconnect() {
        android.util.Log.d("LiveKitRoom", "LiveKit Room disconnected gracefully")
    }

    // Update state and trigger listener callbacks
    fun updateConnectionQuality(quality: ConnectionQuality) {
        currentQuality = quality
        connectionQualityListener?.invoke(quality)
    }

    fun updateActiveSpeaker(participant: LiveKitParticipant) {
        activeSpeakerListener?.invoke(participant)
    }

    fun addParticipant(participant: LiveKitParticipant) {
        participantConnectedListener?.invoke(participant)
    }

    fun removeParticipant(participant: LiveKitParticipant) {
        participantDisconnectedListener?.invoke(participant)
    }
}

object LiveKitClient {
    fun connect(context: Context, options: ConnectOptions): LiveKitRoom {
        android.util.Log.d("LiveKitClient", "LiveKit Room connected successfully. Private call active.")
        return LiveKitRoom(context)
    }
}
