package com.zaxo.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.zaxo.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class SettingsRepository(private val context: Context) {
    private val syncManager = SettingsSyncManager(context)
    private val securePrefs = context.getSharedPreferences("zaxo_secure_prefs", Context.MODE_PRIVATE)
    private val db = ZaxoDatabase.getDatabase(context)
    private val deviceDao = db.deviceDao()
    private val contactDao = db.contactDao()
    
    companion object {
        // Core Keys
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_USERNAME = "username"
        const val KEY_BIO = "bio"
        const val KEY_ACCOUNT_TYPE = "account_type"
        const val KEY_ZAXO_NUMBER = "zaxo_number"
        
        // Zaxo Number & P2P
        const val KEY_ZAXO_VISIBILITY = "zaxo_visibility"
        const val KEY_P2P_CALLING = "p2p_calling"
        const val KEY_CALLER_ID = "caller_id"
        const val KEY_RING_MODE = "ring_mode"
        const val KEY_CALL_FORWARDING = "call_forwarding"
        const val KEY_CALL_FORWARDING_NUMBER = "call_forwarding_number"
        
        // Calling Switched & Quality
        const val KEY_P2P_AUDIO_ENABLED = "p2p_audio_enabled"
        const val KEY_P2P_VIDEO_ENABLED = "p2p_video_enabled"
        const val KEY_P2P_CALL_QUALITY = "p2p_call_quality"
        const val KEY_P2P_DATA_USAGE = "p2p_data_usage"
        const val KEY_P2P_AUTO_ANSWER = "p2p_auto_answer"
        const val KEY_P2P_NOISE_CANCELLATION = "p2p_noise_cancellation"
        const val KEY_P2P_ECHO_CANCELLATION = "p2p_echo_cancellation"
        
        // Security & App Lock
        const val KEY_TWO_STEP_PIN_HASH = "two_step_pin_hash"
        const val KEY_BIOMETRIC_LOCK = "security_biometric_lock"
        const val KEY_LOCK_TIMEOUT = "security_lock_timeout"
        
        // Privacy
        const val KEY_PRIVACY_LAST_SEEN = "privacy_last_seen"
        const val KEY_PRIVACY_PROFILE_PHOTO = "privacy_profile_photo"
        const val KEY_PRIVACY_STATUS = "privacy_status"
        const val KEY_PRIVACY_READ_RECEIPTS = "privacy_read_receipts"
        const val KEY_PRIVACY_TYPING_INDICATOR = "privacy_typing_indicator"
        const val KEY_PRIVACY_SCREENSHOTS = "privacy_screenshots"
        const val KEY_PRIVACY_LINK_PREVIEWS = "privacy_link_previews"
        const val KEY_PRIVACY_DATA_COLLECTION = "privacy_data_collection"
        const val KEY_PRIVACY_LOCATION_SHARING = "privacy_location_sharing"
        const val KEY_PRIVACY_CONTACT_SYNCING = "privacy_contact_syncing"
        const val KEY_PRIVACY_ENCRYPTED_BACKUP = "privacy_encrypted_backup"
        
        // Notifications
        const val KEY_NOTIFICATIONS_PUSH = "notifications_push"
        const val KEY_NOTIFICATIONS_SOUND = "notifications_sound"
        const val KEY_NOTIFICATIONS_GROUP = "notifications_group"
        const val KEY_NOTIFICATIONS_CALL = "notifications_call"
        const val KEY_NOTIFICATIONS_IN_APP = "notifications_in_app"
        const val KEY_NOTIFICATIONS_EMAIL = "notifications_email"
        const val KEY_NOTIFICATIONS_MARKETING = "notifications_marketing"
        const val KEY_NOTIFICATIONS_PREVIEW = "notifications_preview"
        const val KEY_FCM_PAIR_KEY = "fcm_pair_key"
        
        // Appearance
        const val KEY_APPEARANCE_DARK_MODE = "appearance_dark_mode"
        const val KEY_APPEARANCE_CHAT_BACKGROUND = "appearance_chat_background"
        const val KEY_APPEARANCE_FONT_SIZE = "appearance_font_size"
        const val KEY_APPEARANCE_BUBBLE_STYLE = "appearance_bubble_style"
        const val KEY_APPEARANCE_COLOR_THEME = "appearance_color_theme"
        const val KEY_APPEARANCE_ONLINE_STATUS = "appearance_online_status"
        const val KEY_APPEARANCE_COMPACT_MODE = "appearance_compact_mode"
        
        // Storage & Data
        const val KEY_STORAGE_AUTO_DOWNLOAD = "storage_auto_download"
        const val KEY_STORAGE_KEEP_MESSAGES = "storage_keep_messages"
        const val KEY_STORAGE_AUTO_PLAY = "storage_auto_play"
        
        // Chats
        const val KEY_CHATS_DEFAULT_TIMER = "chats_default_timer"
        const val KEY_CHATS_BACKUP_FREQUENCY = "chats_backup_frequency"
        const val KEY_CHATS_ENTER_KEY_SENDS = "chats_enter_key_sends"
        const val KEY_CHATS_STICKER_SUGGESTIONS = "chats_sticker_suggestions"
        
        // AI & Assistant
        const val KEY_AI_SMART_REPLIES = "ai_smart_replies"
        const val KEY_AI_VOICE_ASSISTANT = "ai_voice_assistant"
        const val KEY_AI_MESSAGE_SUMMARIES = "ai_message_summaries"
        const val KEY_AI_SMART_SEARCH = "ai_smart_search"
        
        // Accessibility
        const val KEY_ACCESSIBILITY_TALKBACK = "accessibility_talkback"
        const val KEY_ACCESSIBILITY_REDUCE_MOTION = "accessibility_reduce_motion"
        const val KEY_ACCESSIBILITY_HIGH_CONTRAST = "accessibility_high_contrast"
        const val KEY_ACCESSIBILITY_FONT_SCALE = "accessibility_font_scale"
        
        // Language
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_KEYBOARD_LANGUAGE = "preferred_keyboard_language"
    }

    // --- SECURE STORAGE ENCRYPTION (Hardware-Backed Secure Element Shield) ---
    private fun encrypt(data: String): String {
        return com.zaxo.data.security.ZaxoKeyStoreHelper.encrypt(data)
    }

    private fun decrypt(encryptedData: String): String {
        return com.zaxo.data.security.ZaxoKeyStoreHelper.decrypt(encryptedData)
    }

    // --- SECURE WRITES ---
    fun saveSecureString(key: String, value: String) {
        val encrypted = encrypt(value)
        securePrefs.edit().putString(key, encrypted).apply()
    }

    fun getSecureString(key: String, defaultValue: String): String {
        val encrypted = securePrefs.getString(key, null) ?: return defaultValue
        return decrypt(encrypted)
    }

    // --- STANDARD WRITES ---
    fun saveBoolean(key: String, value: Boolean) {
        syncManager.onSettingChanged(key, value)
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return syncManager.getLocalBoolean(key, defaultValue)
    }

    fun saveString(key: String, value: String) {
        syncManager.onSettingChanged(key, value)
    }

    fun getString(key: String, defaultValue: String): String {
        return syncManager.getLocalString(key, defaultValue)
    }

    fun saveInt(key: String, value: Int) {
        syncManager.onSettingChanged(key, value)
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return syncManager.getLocalInt(key, defaultValue)
    }

    fun saveFloat(key: String, value: Float) {
        syncManager.onSettingChanged(key, value)
    }

    fun getFloat(key: String, defaultValue: Float): Float {
        return syncManager.getLocalFloat(key, defaultValue)
    }

    // --- ROOM DATABASE DIRECT DIRECTORY FEEDS ---
    fun getActiveDevices(): Flow<List<DeviceEntity>> {
        return deviceDao.getActiveDevices()
    }

    suspend fun removeDevice(id: String) = withContext(Dispatchers.IO) {
        deviceDao.removeDevice(id)
    }

    suspend fun clearOtherDevices() = withContext(Dispatchers.IO) {
        deviceDao.clearOtherDevices()
    }
}
