package com.zaxo.data.call

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log

class RingbackToneManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    fun start(customUrl: String? = null) {
        stop()
        
        // Silent mode check: Don't play if on silent or vibrate
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            Log.d("RingbackToneManager", "Phone is in silent or vibrate mode, skipping ringback tone")
            return
        }

        if (customUrl != null) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(customUrl))
                    prepareAsync()
                    setOnPreparedListener {
                        setVolume(0.7f, 0.7f)
                        start()
                    }
                    isLooping = true
                }
            } catch (e: Exception) {
                Log.e("RingbackToneManager", "Error starting custom ringback, falling back to tone generator", e)
                startFallbackTone()
            }
        } else {
            startFallbackTone()
        }
    }

    private fun startFallbackTone() {
        try {
            // ToneGenerator for a dual-tone RING BACK sound (similar to American ringback: 440Hz + 480Hz)
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 70)
            handler.post(object : Runnable {
                override fun run() {
                    if (toneGenerator != null) {
                        toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE)
                        // European ring tone is 1s on, 4s off, US is 2s on, 4s off. Let's repeat
                        handler.postDelayed(this, 4000)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("RingbackToneManager", "Error playing fallback ringback tone", e)
        }
    }

    fun fadeOut(durationMs: Long = 300) {
        val mp = mediaPlayer
        if (mp != null) {
            val startVolume = 0.7f
            val steps = 10
            val stepDuration = durationMs / steps
            for (i in 0..steps) {
                handler.postDelayed({
                    val v = startVolume * (1f - i.toFloat() / steps)
                    try {
                        mediaPlayer?.setVolume(v, v)
                    } catch (e: Exception) {
                        // ignore if player is already released
                    }
                    if (i == steps) stop()
                }, i * stepDuration)
            }
        } else {
            stop()
        }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("RingbackToneManager", "Error stopping MediaPlayer", e)
        }
        mediaPlayer = null

        try {
            toneGenerator?.apply {
                stopTone()
                release()
            }
        } catch (e: Exception) {
            Log.e("RingbackToneManager", "Error stopping ToneGenerator", e)
        }
        toneGenerator = null
    }
}
