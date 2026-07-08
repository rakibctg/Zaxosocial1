package com.zaxo.data.local

import androidx.annotation.Keep

@Keep
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Keep
enum class RingMode { RING, VIBRATE, SILENT }

@Keep
enum class FontSize { SMALL, MEDIUM, LARGE }

@Keep
enum class BubbleStyle { ROUNDED, SHARP, MODERN }

@Keep
enum class ColorTheme { BLUE, GREEN, PURPLE, ORANGE, PINK }

@Keep
enum class DisappearingMessages { OFF, HOURS_24, DAYS_7, DAYS_30 }

@Keep
enum class BackupFrequency { DAILY, WEEKLY, MONTHLY, OFF }

@Keep
enum class VisibilityMode { EVERYONE, CONTACTS, NOBODY }

@Keep
data class DeviceSession(
    val id: String,
    val name: String,
    val location: String,
    val lastActive: Long,
    val isCurrent: Boolean,
    val model: String = "Pixel 8 Pro",
    val osVersion: String = "14",
    val appVersion: String = "6.0.0",
    val firstSeen: Long = System.currentTimeMillis() - 86400000 * 7,
    val isVerified: Boolean = true
)
