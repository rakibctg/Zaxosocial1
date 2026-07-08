package com.zaxo.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Query("UPDATE contacts SET isBlocked = :isBlocked WHERE id = :id")
    suspend fun setBlockedStatus(id: String, isBlocked: Boolean)

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContact(id: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getActiveChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 1 ORDER BY lastMessageTime DESC")
    fun getArchivedChats(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    suspend fun setPinned(chatId: String, isPinned: Boolean)

    @Query("UPDATE chats SET isArchived = :isArchived WHERE id = :chatId")
    suspend fun setArchived(chatId: String, isArchived: Boolean)

    @Query("UPDATE chats SET isMuted = :isMuted WHERE id = :chatId")
    suspend fun setMuted(chatId: String, isMuted: Boolean)

    @Query("UPDATE chats SET lastMessage = :lastMsg, lastMessageTime = :time WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMsg: String, time: Long)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun clearUnread(chatId: String)

    @Query("UPDATE chats SET unreadCount = unreadCount + 1 WHERE id = :chatId")
    suspend fun incrementUnread(chatId: String)

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageFts(fts: MessageFtsEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateStatus(messageId: String, status: String)

    @Query("UPDATE messages SET decryptedText = :newText, isEdited = 1 WHERE id = :messageId")
    suspend fun editMessageText(messageId: String, newText: String)

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :messageId")
    suspend fun setStarred(messageId: String, isStarred: Boolean)

    @Query("SELECT * FROM messages WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearChatMessages(chatId: String)

    // FTS4 full text search join
    @Query("""
        SELECT m.* FROM messages m 
        JOIN messages_fts f ON m.id = f.messageId 
        WHERE messages_fts MATCH :query
    """)
    suspend fun searchMessages(query: String): List<MessageEntity>
}

@Dao
interface StatusDao {
    @Query("SELECT * FROM status_updates WHERE timestamp > :expiryTime ORDER BY timestamp DESC")
    fun getActiveStatuses(expiryTime: Long = System.currentTimeMillis() - 86400000): Flow<List<StatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusEntity)

    @Query("UPDATE status_updates SET isMuted = :isMuted WHERE userId = :userId")
    suspend fun setMutedStatus(userId: String, isMuted: Boolean)

    @Query("DELETE FROM status_updates WHERE timestamp <= :expiryTime")
    suspend fun deleteExpiredStatuses(expiryTime: Long = System.currentTimeMillis() - 86400000)

    @Query("DELETE FROM status_updates WHERE id = :statusId")
    suspend fun deleteStatus(statusId: String)

    @Query("UPDATE status_updates SET viewsCount = viewsCount + 1 WHERE id = :id")
    suspend fun incrementViews(id: String)
}

@Dao
interface CallRecordDao {
    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    fun getAllCallRecords(): Flow<List<CallRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecord(call: CallRecordEntity)

    @Query("DELETE FROM call_records")
    suspend fun clearCallHistory()
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM active_devices ORDER BY lastActive DESC")
    fun getActiveDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM active_devices ORDER BY lastActive DESC")
    suspend fun getActiveDevicesList(): List<DeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Query("DELETE FROM active_devices WHERE id = :id")
    suspend fun removeDevice(id: String)

    @Query("DELETE FROM active_devices WHERE isCurrent = 0")
    suspend fun clearOtherDevices()
}

@Dao
interface SettingDao {
    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)
}
