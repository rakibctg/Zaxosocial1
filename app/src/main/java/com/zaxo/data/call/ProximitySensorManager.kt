package com.zaxo.data.call

import android.content.Context
import android.os.PowerManager
import android.util.Log

class ProximitySensorManager(private val context: Context) {
    private var wakeLock: PowerManager.WakeLock? = null

    fun shouldActivate(
        callType: String,
        isSpeakerOn: Boolean,
        isBluetoothConnected: Boolean,
        callActive: Boolean
    ): Boolean {
        // Active audio calls in earpiece mode (not speaker, not bluetooth)
        return callType == "audio" && !isSpeakerOn && !isBluetoothConnected && callActive
    }

    @Suppress("DEPRECATION")
    fun acquire() {
        if (wakeLock != null && wakeLock?.isHeld == true) return

        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                wakeLock = pm.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "Zaxo:ProximitySensor"
                ).apply {
                    acquire(10 * 60 * 1000L) // 10-minute safe timeout
                }
                Log.d("ProximitySensorManager", "Proximity wake lock acquired successfully")
            } else {
                Log.w("ProximitySensorManager", "Proximity wake lock is not supported on this device")
            }
        } catch (e: Exception) {
            Log.e("ProximitySensorManager", "Failed to acquire proximity wake lock", e)
        }
    }

    fun release() {
        try {
            wakeLock?.apply {
                if (isHeld) {
                    release()
                    Log.d("ProximitySensorManager", "Proximity wake lock released")
                }
            }
        } catch (e: Exception) {
            Log.e("ProximitySensorManager", "Failed to release proximity wake lock", e)
        }
        wakeLock = null
    }
}
