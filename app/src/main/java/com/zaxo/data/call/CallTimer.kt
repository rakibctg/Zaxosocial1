package com.zaxo.data.call

import android.os.SystemClock

class CallTimer {
    private var callStartTime: Long = 0L
    private var totalHoldTime: Long = 0L
    private var holdStartTime: Long = 0L
    private var isOnHold: Boolean = false

    fun start() {
        callStartTime = SystemClock.elapsedRealtime()
        totalHoldTime = 0L
        isOnHold = false
    }

    fun hold() {
        if (!isOnHold) {
            isOnHold = true
            holdStartTime = SystemClock.elapsedRealtime()
        }
    }

    fun resume() {
        if (isOnHold) {
            isOnHold = false
            totalHoldTime += SystemClock.elapsedRealtime() - holdStartTime
        }
    }

    fun getElapsedSeconds(): Int {
        if (callStartTime == 0L) return 0
        val now = SystemClock.elapsedRealtime()
        val raw = if (isOnHold) {
            now - callStartTime - totalHoldTime - (now - holdStartTime)
        } else {
            now - callStartTime - totalHoldTime
        }
        return (raw / 1000).toInt().coerceAtLeast(0)
    }

    fun format(): String {
        val seconds = getElapsedSeconds()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }
}
