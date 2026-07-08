package com.zaxo.data.call

import android.app.NotificationManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class IncomingRingtoneManager(private val context: Context) {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    @Suppress("DEPRECATION")
    private fun getVibratorService(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun start(ringMode: String, customUri: String? = null, overrideDnd: Boolean = false) {
        stop()

        // Check Do Not Disturb (DND) mode
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.currentInterruptionFilter >= NotificationManager.INTERRUPTION_FILTER_PRIORITY && !overrideDnd) {
            Log.d("IncomingRingtoneManager", "DND is enabled and override is false, running in SILENT")
            return
        }

        vibrator = getVibratorService()

        when (ringMode) {
            "Ring", "FULL" -> {
                // Play Ringtone
                val uri = customUri?.let { Uri.parse(it) }
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                try {
                    ringtone = RingtoneManager.getRingtone(context, uri)
                    ringtone?.play()
                } catch (e: Exception) {
                    Log.e("IncomingRingtoneManager", "Failed to play ringtone", e)
                }

                // Vibrate
                val pattern = longArrayOf(0, 400, 200, 400) // 0ms wait, 400ms vibrate, 200ms rest, 400ms vibrate
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    vibrator?.vibrate(pattern, 0)
                }
            }
            "Vibrate", "VIBRATE_ONLY" -> {
                // Vibrate only
                val pattern = longArrayOf(0, 400, 200, 400)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    vibrator?.vibrate(pattern, 0)
                }
            }
            "Silent", "SILENT" -> {
                // Silent, no vibration, no sound
                Log.d("IncomingRingtoneManager", "Silent mode, no ringtone or vibration")
            }
        }
    }

    fun stop() {
        try {
            ringtone?.apply {
                if (isPlaying) stop()
            }
        } catch (e: Exception) {
            Log.e("IncomingRingtoneManager", "Error stopping ringtone", e)
        }
        ringtone = null

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("IncomingRingtoneManager", "Error cancelling vibration", e)
        }
        vibrator = null
    }
}
