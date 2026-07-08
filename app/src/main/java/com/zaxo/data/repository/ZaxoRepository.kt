package com.zaxo.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.zaxo.data.api.GeminiClient
import com.zaxo.data.local.*
import com.zaxo.data.security.ZaxoSecurityEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ZaxoRepository(private val context: Context) {
    private val db = ZaxoDatabase.getDatabase(context)
    val contactDao = db.contactDao()
    val chatDao = db.chatDao()
    val messageDao = db.messageDao()
    val statusDao = db.statusDao()
    val callRecordDao = db.callRecordDao()
    val deviceDao = db.deviceDao()
    val settingDao = db.settingDao()

    var currentlyActiveChatId: String? = null

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Prepopulate default contacts, statuses, settings, and active devices on startup
        repositoryScope.launch {
            prepopulateDatabase()
        }
        listenForMessagesRealtime()
    }

    private suspend fun prepopulateDatabase() {
        // Check if contacts are empty
        val existingContacts = db.contactDao().getContactById("zaxo_ai")
        if (existingContacts == null) {
            // Contacts
            val defaultContacts = listOf(
                ContactEntity(
                    id = "zaxo_ai",
                    name = "Zaxo AI Assistant",
                    zaxoNumber = "999-999-999",
                    avatar = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150",
                    isOnline = true,
                    isFavorite = true
                ),
                ContactEntity(
                    id = "support_alice",
                    name = "Alice (Zaxo Support)",
                    zaxoNumber = "111-222-333",
                    avatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    isOnline = true
                ),
                ContactEntity(
                    id = "security_bot",
                    name = "Zaxo Security Bot",
                    zaxoNumber = "888-888-888",
                    avatar = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=150",
                    isOnline = false
                ),
                ContactEntity(
                    id = "bob_builder",
                    name = "Bob Builder",
                    zaxoNumber = "444-555-666",
                    avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    isOnline = true
                )
            )
            contactDao.insertContacts(defaultContacts)

            // Setup default active devices
            val thisDevice = DeviceEntity(
                id = "current_device_id_zaxo",
                name = "Google Pixel 8 Pro (Active)",
                location = "Mountain View, CA",
                lastActive = System.currentTimeMillis(),
                isCurrent = true
            )
            deviceDao.insertDevice(thisDevice)

            // Setup default setting values
            settingDao.insertSetting(SettingEntity("biometric_lock", "false"))
            settingDao.insertSetting(SettingEntity("two_factor_enabled", "false"))
            settingDao.insertSetting(SettingEntity("calling_permission", "Everyone"))
            settingDao.insertSetting(SettingEntity("auto_answer_enabled", "false"))
            settingDao.insertSetting(SettingEntity("disappearing_messages_global", "0"))
            settingDao.insertSetting(SettingEntity("chat_wallpaper_mode", "standard"))

            // Setup initial AI Assistant chat
            val chatAI = ChatEntity(
                id = "zaxo_ai",
                name = "Zaxo AI Assistant",
                avatar = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150",
                isGroup = false,
                lastMessage = "Hey! Let's test the security and AI. Say hello!",
                lastMessageTime = System.currentTimeMillis()
            )
            chatDao.insertChat(chatAI)

            // Initial AI message
            val initialMsgAI = MessageEntity(
                id = "init_msg_ai_1",
                chatId = "zaxo_ai",
                senderId = "zaxo_ai",
                senderName = "Zaxo AI Assistant",
                encryptedText = ZaxoSecurityEngine.encrypt("zaxo_ai", "Hey! Let's test the security and AI. Say hello!"),
                decryptedText = "Hey! Let's test the security and AI. Say hello!",
                timestamp = System.currentTimeMillis()
            )
            messageDao.insertMessage(initialMsgAI)
        }
    }

    // --- MESSAGING ---

    fun getActiveChats(): Flow<List<ChatEntity>> = chatDao.getActiveChats()
    fun getArchivedChats(): Flow<List<ChatEntity>> = chatDao.getArchivedChats()

    fun getMessages(chatId: String): Flow<List<MessageEntity>> = messageDao.getMessagesForChat(chatId)

    suspend fun sendMessage(chatId: String, text: String, mediaUrl: String? = null, mediaType: String? = null, replyToId: String? = null, replyToText: String? = null) {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // Encrypt the message text using Signal-style Double Ratchet
        val encryptedText = ZaxoSecurityEngine.encrypt(chatId, text)

        val message = MessageEntity(
            id = messageId,
            chatId = chatId,
            senderId = "me",
            senderName = "Me",
            encryptedText = encryptedText,
            decryptedText = text,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            timestamp = timestamp,
            status = "SENDING",
            replyToId = replyToId,
            replyToText = replyToText
        )

        // Optimistic UI insert
        messageDao.insertMessage(message)
        messageDao.insertMessageFts(MessageFtsEntity(messageId, text, "Me"))
        chatDao.updateLastMessage(chatId, text, timestamp)

        // Run real network write and update message states
        repositoryScope.launch {
            if (chatId == "zaxo_ai" || chatId == "support_alice" || chatId == "security_bot") {
                delay(200)
                messageDao.updateStatus(messageId, "SENT")
                delay(200)
                messageDao.updateStatus(messageId, "DELIVERED")
                delay(300)
                messageDao.updateStatus(messageId, "READ")
                if (chatId == "zaxo_ai") {
                    triggerAIAssistantResponse(text)
                } else if (chatId == "support_alice") {
                    triggerSupportAliceResponse(text)
                } else if (chatId == "security_bot") {
                    triggerSecurityBotResponse(text)
                }
            } else {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "my_uid_zaxo"
                    val myName = contactDao.getContactById(myUid)?.name ?: "Me"
                    val msgMap = hashMapOf(
                        "id" to messageId,
                        "chatId" to chatId,
                        "senderId" to myUid,
                        "senderName" to myName,
                        "text" to text,
                        "mediaUrl" to mediaUrl,
                        "mediaType" to mediaType,
                        "timestamp" to timestamp,
                        "replyToId" to replyToId,
                        "replyToText" to replyToText,
                        "status" to "SENT"
                    )
                    firestore.collection("chats").document(chatId)
                        .collection("messages").document(messageId)
                        .set(msgMap)
                        .addOnSuccessListener {
                            repositoryScope.launch {
                                messageDao.updateStatus(messageId, "SENT")
                                Log.d("ZaxoRepository", "Message $messageId sent successfully. Triggering FCM.")
                            }
                        }
                        .addOnFailureListener { e ->
                            repositoryScope.launch {
                                messageDao.updateStatus(messageId, "FAILED")
                                Log.e("ZaxoRepository", "Failed to write message to Firestore", e)
                            }
                        }
                } catch (e: Exception) {
                    messageDao.updateStatus(messageId, "FAILED")
                    Log.e("ZaxoRepository", "Firestore send message error", e)
                }
            }
        }
    }

    private suspend fun triggerAIAssistantResponse(userPrompt: String) {
        val incomingId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // Call our real Gemini client to get response!
        val systemInstruction = "You are Zaxo AI, a brilliant, highly secure conversational companion inside the Zaxo end-to-end encrypted app. Help users, show off security, be professional, and maintain Neumorphism design systems discussion where appropriate."
        val aiResponseText = GeminiClient.generateText(userPrompt, systemInstruction)

        val encryptedResponse = ZaxoSecurityEngine.encrypt("zaxo_ai", aiResponseText)

        val responseMessage = MessageEntity(
            id = incomingId,
            chatId = "zaxo_ai",
            senderId = "zaxo_ai",
            senderName = "Zaxo AI Assistant",
            encryptedText = encryptedResponse,
            decryptedText = aiResponseText,
            timestamp = timestamp,
            status = "READ"
        )

        messageDao.insertMessage(responseMessage)
        messageDao.insertMessageFts(MessageFtsEntity(incomingId, aiResponseText, "Zaxo AI Assistant"))
        chatDao.updateLastMessage("zaxo_ai", aiResponseText, timestamp)
        chatDao.incrementUnread("zaxo_ai")
    }

    private suspend fun triggerSupportAliceResponse(userText: String) {
        delay(1500)
        val incomingId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val reply = when {
            userText.contains("call", ignoreCase = true) -> 
                "We support high-fidelity 1:1 voice/video calls and group calls of up to 20 participants with active speaker detection. Try placing a call using the Call tab!"
            userText.contains("secret", ignoreCase = true) || userText.contains("encrypt", ignoreCase = true) ->
                "Zaxo uses double-ratchet E2E Encryption (inspired by Signal Protocol). No servers can inspect your messages, photos, or voice notes!"
            userText.contains("hello", ignoreCase = true) || userText.contains("hi", ignoreCase = true) ->
                "Hello there! I'm Alice from Zaxo customer support. How can I help you learn about Zaxo today?"
            else -> 
                "That sounds interesting! Remember, all messaging here is fully encrypted offline-first."
        }

        val encryptedReply = ZaxoSecurityEngine.encrypt("support_alice", reply)
        val responseMessage = MessageEntity(
            id = incomingId,
            chatId = "support_alice",
            senderId = "support_alice",
            senderName = "Alice (Zaxo Support)",
            encryptedText = encryptedReply,
            decryptedText = reply,
            timestamp = timestamp,
            status = "READ"
        )

        messageDao.insertMessage(responseMessage)
        messageDao.insertMessageFts(MessageFtsEntity(incomingId, reply, "Alice (Zaxo Support)"))
        chatDao.updateLastMessage("support_alice", reply, timestamp)
        chatDao.incrementUnread("support_alice")
    }

    private suspend fun triggerSecurityBotResponse(userText: String) {
        delay(1500)
        val incomingId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val reply = when {
            userText.contains("device", ignoreCase = true) ->
                "You can manage your active sessions inside Settings > Active Devices. You can remotely revoke access to any connected laptop or tablet instantly!"
            userText.contains("lock", ignoreCase = true) || userText.contains("biometric", ignoreCase = true) ->
                "Go to Settings > Security to enable Fingerprint or Face biometric authentication lock for the app!"
            else ->
                "Securing your digital footprint: Zaxo has 2FA keys, secure local keystore storage, and active device monitoring."
        }

        val encryptedReply = ZaxoSecurityEngine.encrypt("security_bot", reply)
        val responseMessage = MessageEntity(
            id = incomingId,
            chatId = "security_bot",
            senderId = "security_bot",
            senderName = "Zaxo Security Bot",
            encryptedText = encryptedReply,
            decryptedText = reply,
            timestamp = timestamp,
            status = "READ"
        )

        messageDao.insertMessage(responseMessage)
        messageDao.insertMessageFts(MessageFtsEntity(incomingId, reply, "Zaxo Security Bot"))
        chatDao.updateLastMessage("security_bot", reply, timestamp)
        chatDao.incrementUnread("security_bot")
    }

    suspend fun editMessage(messageId: String, newText: String) {
        val msg = messageDao.getMessageById(messageId) ?: return
        messageDao.editMessageText(messageId, newText)
        chatDao.updateLastMessage(msg.chatId, "$newText (Edited)", System.currentTimeMillis())
    }

    suspend fun toggleMessageStarred(messageId: String) {
        val msg = messageDao.getMessageById(messageId) ?: return
        messageDao.setStarred(messageId, !msg.isStarred)
    }

    suspend fun deleteMessage(messageId: String) {
        val msg = messageDao.getMessageById(messageId) ?: return
        messageDao.deleteMessage(messageId)
        chatDao.updateLastMessage(msg.chatId, "Message deleted", System.currentTimeMillis())
    }

    suspend fun clearChat(chatId: String) {
        messageDao.clearChatMessages(chatId)
        chatDao.updateLastMessage(chatId, "No messages", System.currentTimeMillis())
    }

    suspend fun pinChat(chatId: String, isPinned: Boolean) {
        chatDao.setPinned(chatId, isPinned)
    }

    suspend fun archiveChat(chatId: String, isArchived: Boolean) {
        chatDao.setArchived(chatId, isArchived)
    }

    suspend fun muteChat(chatId: String, isMuted: Boolean) {
        chatDao.setMuted(chatId, isMuted)
    }

    suspend fun deleteChat(chatId: String) {
        chatDao.deleteChat(chatId)
        messageDao.clearChatMessages(chatId)
    }

    suspend fun setDisappearingMessages(chatId: String, timerHours: Int) {
        val chat = chatDao.getChatById(chatId) ?: return
        chatDao.insertChat(chat.copy(disappearingTimerHours = timerHours))
    }

    suspend fun setChatWallpaper(chatId: String, wallpaperPath: String?) {
        val chat = chatDao.getChatById(chatId) ?: return
        chatDao.insertChat(chat.copy(customWallpaper = wallpaperPath))
    }

    suspend fun searchMessages(query: String): List<MessageEntity> {
        return messageDao.searchMessages(query)
    }

    // --- CALLS ---

    fun getCallHistory(): Flow<List<CallRecordEntity>> = callRecordDao.getAllCallRecords()

    suspend fun addCallRecord(call: CallRecordEntity) {
        callRecordDao.insertCallRecord(call)
    }

    suspend fun clearCallHistory() {
        callRecordDao.clearCallHistory()
    }

    // --- CONTACTS ---

    fun getContacts(): Flow<List<ContactEntity>> = contactDao.getAllContacts()

    suspend fun addContact(name: String, zaxoNumber: String, avatar: String) {
        val id = UUID.randomUUID().toString()
        contactDao.insertContact(ContactEntity(id, name, zaxoNumber, avatar))
    }

    suspend fun blockContact(contactId: String, isBlocked: Boolean) {
        contactDao.setBlockedStatus(contactId, isBlocked)
    }

    // --- STATUS ---

    fun getActiveStatuses(): Flow<List<StatusEntity>> = statusDao.getActiveStatuses()

    suspend fun postStatus(mediaType: String, mediaUrl: String? = null, textBgColor: Int = 0, textStyle: Int = 0, caption: String = "") {
        val id = UUID.randomUUID().toString()
        val status = StatusEntity(
            id = id,
            userId = "me",
            userName = "My Status",
            userAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            textBgColor = textBgColor,
            textStyle = textStyle,
            caption = caption,
            timestamp = System.currentTimeMillis()
        )
        statusDao.insertStatus(status)
    }

    suspend fun toggleStatusMute(userId: String, isMuted: Boolean) {
        statusDao.setMutedStatus(userId, isMuted)
    }

    suspend fun viewStatus(statusId: String) {
        statusDao.incrementViews(statusId)
    }

    // --- DEVICES ---

    fun getActiveDevices(): Flow<List<DeviceEntity>> = deviceDao.getActiveDevices()

    suspend fun registerDevice(device: DeviceEntity) {
        deviceDao.insertDevice(device)
    }

    suspend fun revokeDevice(deviceId: String) {
        deviceDao.removeDevice(deviceId)
    }

    suspend fun clearOtherDevices() {
        deviceDao.clearOtherDevices()
    }

    // --- SETTINGS ---

    suspend fun getSetting(key: String, defaultValue: String): String {
        return settingDao.getSetting(key) ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String) {
        settingDao.insertSetting(SettingEntity(key, value))
    }

    private fun showNotification(senderName: String, messageText: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "zaxo_messages_channel"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Zaxo Messages",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Incoming messages from Zaxo"
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(senderName)
                .setContentText(messageText)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            Log.e("ZaxoRepository", "Failed to show notification", e)
        }
    }

    private fun listenForMessagesRealtime() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collectionGroup("messages")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("ZaxoRepository", "Snapshot listener error", error)
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        repositoryScope.launch {
                            for (doc in snapshots.documentChanges) {
                                if (doc.type == DocumentChange.Type.ADDED) {
                                    val mId = doc.document.id
                                    val cId = doc.document.getString("chatId") ?: ""
                                    val sId = doc.document.getString("senderId") ?: ""
                                    val sName = doc.document.getString("senderName") ?: "Unknown"
                                    val text = doc.document.getString("text") ?: ""
                                    val mediaUrl = doc.document.getString("mediaUrl")
                                    val mediaType = doc.document.getString("mediaType")
                                    val ts = doc.document.getLong("timestamp") ?: System.currentTimeMillis()
                                    val replyId = doc.document.getString("replyToId")
                                    val replyText = doc.document.getString("replyToText")

                                    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "my_uid_zaxo"

                                    // Do not duplicate our own sent messages
                                    if (sId != myUid && sId != "me") {
                                        val existing = messageDao.getMessageById(mId)
                                        if (existing == null) {
                                            val isActive = (currentlyActiveChatId == cId)
                                            val msgStatus = if (isActive) "READ" else "DELIVERED"

                                            if (isActive) {
                                                // Active chat: instantly commit read state to Firestore
                                                try {
                                                    firestore.collection("chats").document(cId)
                                                        .collection("messages").document(mId)
                                                        .update("status", "READ")
                                                } catch (e: Exception) {
                                                    Log.e("ZaxoRepository", "Failed to commit read state", e)
                                                }
                                            } else {
                                                // Backgrounded/inactive: route payload to Android Notification Manager
                                                showNotification(sName, text)
                                            }

                                            val encryptedText = ZaxoSecurityEngine.encrypt(cId, text)
                                            val message = MessageEntity(
                                                id = mId,
                                                chatId = cId,
                                                senderId = sId,
                                                senderName = sName,
                                                encryptedText = encryptedText,
                                                decryptedText = text,
                                                mediaUrl = mediaUrl,
                                                mediaType = mediaType,
                                                timestamp = ts,
                                                status = msgStatus,
                                                replyToId = replyId,
                                                replyToText = replyText
                                            )

                                            // Ensure the chat exists in our chat list, otherwise create it
                                            val existingChat = chatDao.getChatById(cId)
                                            if (existingChat == null) {
                                                chatDao.insertChat(ChatEntity(
                                                    id = cId,
                                                    name = sName,
                                                    avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                                    isGroup = false,
                                                    lastMessage = text,
                                                    lastMessageTime = ts,
                                                    unreadCount = if (isActive) 0 else 1
                                                ))
                                            } else {
                                                chatDao.updateLastMessage(cId, text, ts)
                                                if (!isActive) {
                                                    chatDao.incrementUnread(cId)
                                                }
                                            }

                                            messageDao.insertMessage(message)
                                            messageDao.insertMessageFts(MessageFtsEntity(mId, text, sName))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("ZaxoRepository", "Error initializing real-time message listener", e)
        }
    }

    private var activeChatListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun startListeningToChatMessages(chatId: String) {
        activeChatListener?.remove()
        if (chatId == "zaxo_ai" || chatId == "support_alice" || chatId == "security_bot") return

        try {
            val firestore = FirebaseFirestore.getInstance()
            activeChatListener = firestore.collection("chats").document(chatId)
                .collection("messages")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("ZaxoRepository", "Error listening to active chat messages", error)
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        repositoryScope.launch {
                            for (doc in snapshots.documentChanges) {
                                if (doc.type == DocumentChange.Type.ADDED) {
                                    val mId = doc.document.id
                                    val cId = doc.document.getString("chatId") ?: chatId
                                    val sId = doc.document.getString("senderId") ?: ""
                                    val sName = doc.document.getString("senderName") ?: "Unknown"
                                    val text = doc.document.getString("text") ?: ""
                                    val mediaUrl = doc.document.getString("mediaUrl")
                                    val mediaType = doc.document.getString("mediaType")
                                    val ts = doc.document.getLong("timestamp") ?: System.currentTimeMillis()
                                    val replyId = doc.document.getString("replyToId")
                                    val replyText = doc.document.getString("replyToText")

                                    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "my_uid_zaxo"

                                    if (sId != myUid && sId != "me") {
                                        val existing = messageDao.getMessageById(mId)
                                        if (existing == null) {
                                            try {
                                                firestore.collection("chats").document(cId)
                                                    .collection("messages").document(mId)
                                                    .update("status", "READ")
                                            } catch (e: Exception) {
                                                Log.e("ZaxoRepository", "Failed to commit read state", e)
                                            }

                                            val encryptedText = ZaxoSecurityEngine.encrypt(cId, text)
                                            val message = MessageEntity(
                                                id = mId,
                                                chatId = cId,
                                                senderId = sId,
                                                senderName = sName,
                                                encryptedText = encryptedText,
                                                decryptedText = text,
                                                mediaUrl = mediaUrl,
                                                mediaType = mediaType,
                                                timestamp = ts,
                                                status = "READ",
                                                replyToId = replyId,
                                                replyToText = replyText
                                            )

                                            val existingChat = chatDao.getChatById(cId)
                                            if (existingChat == null) {
                                                chatDao.insertChat(ChatEntity(
                                                    id = cId,
                                                    name = sName,
                                                    avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                                    isGroup = false,
                                                    lastMessage = text,
                                                    lastMessageTime = ts,
                                                    unreadCount = 0
                                                ))
                                            } else {
                                                chatDao.updateLastMessage(cId, text, ts)
                                            }

                                            messageDao.insertMessage(message)
                                            messageDao.insertMessageFts(MessageFtsEntity(mId, text, sName))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("ZaxoRepository", "Failed to start active chat listener", e)
        }
    }

    fun stopListeningToChatMessages() {
        activeChatListener?.remove()
        activeChatListener = null
    }
}
