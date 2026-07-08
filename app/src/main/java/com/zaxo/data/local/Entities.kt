package com.zaxo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.Fts4

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val zaxoNumber: String,
    val avatar: String,
    val isBlocked: Boolean = false,
    val isOnline: Boolean = false,
    val lastActive: Long = 0L,
    val isFavorite: Boolean = false,
    val isFromContacts: Boolean = false
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatar: String,
    val isGroup: Boolean = false,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val disappearingTimerHours: Int = 0, // 0 = off, 24, 168 (7 days), 720 (30 days)
    val customWallpaper: String? = null
)

@Entity(
    tableName = "messages",
    indices = [Index(value = ["chatId"]), Index(value = ["timestamp"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val encryptedText: String,
    val decryptedText: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null, // "PHOTO", "VIDEO", "VOICE"
    val voiceWaveform: String? = null, // Comma separated integers
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENDING", // "SENDING", "SENT", "DELIVERED", "READ", "FAILED"
    val isStarred: Boolean = false,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val isEdited: Boolean = false
)

@Fts4
@Entity(tableName = "messages_fts")
data class MessageFtsEntity(
    val messageId: String,
    val decryptedText: String,
    val senderName: String
)

@Entity(tableName = "status_updates")
data class StatusEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val mediaUrl: String? = null,
    val mediaType: String = "TEXT", // "TEXT", "PHOTO", "VIDEO"
    val textBgColor: Int = 0, // Index of preset colors
    val textStyle: Int = 0, // Index of fonts
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isMuted: Boolean = false,
    val viewsCount: Int = 0
)

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey val id: String,
    val callerId: String,
    val callerName: String,
    val calleeId: String,
    val calleeName: String,
    val callerAvatar: String,
    val calleeAvatar: String,
    val isVideo: Boolean = false,
    val isGroup: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val status: String = "MISSED" // "CONNECTED", "MISSED", "REJECTED", "DECLINED", "NO_ANSWER"
)

@Entity(tableName = "active_devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val location: String,
    val lastActive: Long = System.currentTimeMillis(),
    val isCurrent: Boolean = false
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
