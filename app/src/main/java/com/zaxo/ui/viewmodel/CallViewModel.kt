package com.zaxo.ui.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.zaxo.data.call.*
import com.zaxo.data.local.CallRecordEntity
import com.zaxo.data.local.ContactEntity
import com.zaxo.data.livekit.*
import com.zaxo.data.repository.ZaxoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// Lookup and Gating results
sealed class LookupResult {
    object NotFound : LookupResult()
    data class Found(
        val uid: String,
        val displayName: String,
        val avatarUrl: String?,
        val canCall: Boolean
    ) : LookupResult()
}

sealed class PreFlightResult {
    data class Error(val reason: String) : PreFlightResult()
    data class ConfirmEndCurrent(val currentCall: CallRecordEntity) : PreFlightResult()
    object RequestPermission : PreFlightResult()
    object OfferAudioOnly : PreFlightResult()
    object OK : PreFlightResult()
}

enum class GateResult {
    ALLOWED, REJECTED, SILENT_REJECT
}

enum class RingBehavior {
    FULL, VIBRATE_ONLY, SILENT
}

class CallViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val repository = ZaxoRepository(context)
    private val historyRepository = CallHistoryRepository(context)
    val contacts: kotlinx.coroutines.flow.Flow<List<ContactEntity>> = repository.getContacts()

    // State managers
    val callTimer = CallTimer()
    private val ringbackToneManager = RingbackToneManager(context)
    private val incomingRingtoneManager = IncomingRingtoneManager(context)
    private val proximitySensorManager = ProximitySensorManager(context)
    val callWaitingManager = CallWaitingManager()

    // Flow states
    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _activeCallRecord = MutableStateFlow<CallRecordEntity?>(null)
    val activeCallRecord: StateFlow<CallRecordEntity?> = _activeCallRecord.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isCameraOff = MutableStateFlow(false)
    val isCameraOff: StateFlow<Boolean> = _isCameraOff.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isBluetoothConnected = MutableStateFlow(false)
    val isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected.asStateFlow()

    private val _networkQualityMessage = MutableStateFlow<String?>(null)
    val networkQualityMessage: StateFlow<String?> = _networkQualityMessage.asStateFlow()

    private val _callTimerString = MutableStateFlow("00:00")
    val callTimerString: StateFlow<String> = _callTimerString.asStateFlow()

    private val _groupParticipants = MutableStateFlow<List<LiveKitParticipant>>(emptyList())
    val groupParticipants: StateFlow<List<LiveKitParticipant>> = _groupParticipants.asStateFlow()

    private val _activeSpeakerId = MutableStateFlow<String?>(null)
    val activeSpeakerId: StateFlow<String?> = _activeSpeakerId.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var liveKitRoom: LiveKitRoom? = null
    private var callListener: ListenerRegistration? = null
    private var incomingCallListener: ListenerRegistration? = null

    private fun getLiveKitUrl(): String {
        return try {
            com.zaxo.BuildConfig.LIVEKIT_URL.ifBlank { "wss://livekit.zaxo.app" }
        } catch (e: Throwable) {
            "wss://livekit.zaxo.app"
        }
    }

    private fun getLiveKitApiKey(): String {
        return try {
            com.zaxo.BuildConfig.LIVEKIT_API_KEY.ifBlank { "dummy_key" }
        } catch (e: Throwable) {
            "dummy_key"
        }
    }

    private fun getLiveKitApiSecret(): String {
        return try {
            com.zaxo.BuildConfig.LIVEKIT_API_SECRET.ifBlank { "dummy_secret" }
        } catch (e: Throwable) {
            "dummy_secret"
        }
    }

    private fun generateLiveKitToken(
        apiKey: String,
        apiSecret: String,
        roomName: String,
        identity: String,
        name: String,
        isVideo: Boolean
    ): String {
        return try {
            val header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}"
            val exp = (System.currentTimeMillis() / 1000) + 3600
            val payload = """
                {
                  "iss": "$apiKey",
                  "sub": "$identity",
                  "name": "$name",
                  "exp": $exp,
                  "video": {
                    "roomJoin": true,
                    "room": "$roomName",
                    "canPublish": true,
                    "canSubscribe": true,
                    "canPublishData": true,
                    "audio": true,
                    "video": $isVideo
                  }
                }
            """.trimIndent().replace("\n", "").replace(" ", "")

            val headerBase64 = android.util.Base64.encodeToString(header.toByteArray(), android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
            val payloadBase64 = android.util.Base64.encodeToString(payload.toByteArray(), android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
            val signatureInput = "$headerBase64.$payloadBase64"

            val sha256HMAC = javax.crypto.Mac.getInstance("HmacSHA256")
            val secretKey = javax.crypto.spec.SecretKeySpec(apiSecret.toByteArray(), "HmacSHA256")
            sha256HMAC.init(secretKey)
            val signatureBytes = sha256HMAC.doFinal(signatureInput.toByteArray())
            val signatureBase64 = android.util.Base64.encodeToString(signatureBytes, android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)

            "$headerBase64.$payloadBase64.$signatureBase64"
        } catch (e: Exception) {
            Log.e("CallViewModel", "Failed to generate LiveKit JWT token", e)
            "simulated_token_${roomName}"
        }
    }
    private var timerJob: Job? = null
    private var reconnectTimerJob: Job? = null
    private var autoAnswerHandler: Handler? = null

    // System connectivity callbacks
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Bluetooth listener receiver
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED" ||
                action == "android.bluetooth.device.action.ACL_DISCONNECTED") {
                // Check Bluetooth device route and fall back
                _isBluetoothConnected.value = false
                _isSpeakerOn.value = false // back to earpiece
                applyProximityLock()
            }
        }
    }

    init {
        // Register Bluetooth route listeners safely
        try {
            val filter = IntentFilter().apply {
                addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")
                addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            }
            context.registerReceiver(bluetoothReceiver, filter)
        } catch (e: Exception) {
            Log.e("CallViewModel", "Failed to register bluetooth receiver", e)
        }

        // Register Network status listener for Wifi / Cellular handoffs
        setupNetworkCallback()
        listenForIncomingCallsRealtime()
    }

    fun listenForIncomingCallsRealtime() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            incomingCallListener = firestore.collection("calls")
                .whereEqualTo("calleeId", "my_uid_zaxo")
                .whereEqualTo("status", "RINGING")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("CallViewModel", "Listen for incoming calls failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshots != null && !snapshots.isEmpty) {
                        for (doc in snapshots.documents) {
                            val callId = doc.id
                            val callerId = doc.getString("callerId") ?: ""
                            val callerName = doc.getString("callerName") ?: "Unknown Caller"
                            val callerAvatar = doc.getString("callerAvatar") ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
                            val isVideo = doc.getBoolean("isVideo") ?: false
                            
                            viewModelScope.launch {
                                val callerContact = ContactEntity(
                                    id = callerId,
                                    name = callerName,
                                    zaxoNumber = "555-000-111",
                                    avatar = callerAvatar,
                                    isOnline = true
                                )
                                repository.contactDao.insertContact(callerContact)
                                
                                val record = CallRecordEntity(
                                    id = callId,
                                    callerId = callerId,
                                    callerName = callerName,
                                    calleeId = "my_uid_zaxo",
                                    calleeName = "Me",
                                    callerAvatar = callerAvatar,
                                    calleeAvatar = "",
                                    isVideo = isVideo,
                                    isGroup = false,
                                    timestamp = System.currentTimeMillis(),
                                    status = "RINGING"
                                )
                                
                                if (_callState.value == CallState.IDLE) {
                                    _activeCallRecord.value = record
                                    _callState.value = CallState.INCOMING
                                    
                                    val ringBehavior = applyRingMode()
                                    incomingRingtoneManager.start(ringBehavior.name)
                                    
                                    listenToCurrentCallDoc(callId)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("CallViewModel", "Error in listenForIncomingCallsRealtime", e)
        }
    }

    private fun listenToCurrentCallDoc(callId: String) {
        try {
            callListener?.remove()
            val firestore = FirebaseFirestore.getInstance()
            callListener = firestore.collection("calls").document(callId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("CallViewModel", "Listen to current call document failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val status = snapshot.getString("status") ?: ""
                        Log.d("CallViewModel", "Call doc status update: $status")
                        when (status) {
                            "CONNECTED" -> {
                                if (_callState.value == CallState.CONNECTING || _callState.value == CallState.DIALING || _callState.value == CallState.RINGING) {
                                    connectCallLocal()
                                }
                            }
                            "REJECTED", "DISCONNECTED" -> {
                                if (_callState.value != CallState.IDLE) {
                                    hangUpLocal()
                                }
                            }
                        }
                    } else if (snapshot != null && !snapshot.exists()) {
                        if (_callState.value != CallState.IDLE) {
                            hangUpLocal()
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("CallViewModel", "Error in listenToCurrentCallDoc", e)
        }
    }

    private fun connectCallLocal() {
        val record = _activeCallRecord.value ?: return
        ringbackToneManager.fadeOut()
        _callState.value = CallState.CONNECTING
        
        viewModelScope.launch {
            delay(500)
            _callState.value = CallState.ACTIVE
            _activeCallRecord.value = record.copy(status = "CONNECTED")

            callTimer.start()
            startTimerUpdates()
            applyProximityLock()
        }
    }

    private fun hangUpLocal() {
        ringbackToneManager.stop()
        incomingRingtoneManager.stop()
        autoAnswerHandler?.removeCallbacksAndMessages(null)
        proximitySensorManager.release()
        liveKitRoom?.disconnect()
        liveKitRoom = null

        val record = _activeCallRecord.value
        if (record != null) {
            viewModelScope.launch {
                _callState.value = CallState.ENDED
                timerJob?.cancel()
                val duration = callTimer.getElapsedSeconds().toLong()
                saveFinishedCallRecord(record.copy(durationSeconds = duration, status = "CONNECTED"))

                val minutes = duration / 60
                val secs = duration % 60
                val durationText = String.format("%02d:%02d", minutes, secs)
                val chatTarget = if (record.callerId == "me") record.calleeId else record.callerId
                repository.sendMessage(chatTarget, "Call ended • Duration $durationText")

                _callState.value = CallState.POST_CALL
            }
        } else {
            _callState.value = CallState.IDLE
        }
        callListener?.remove()
        callListener = null
    }

    // --- ALGORITHM 1: OUTGOING CALL PRE-FLIGHT CHECK ---
    suspend fun preFlightCheck(callType: String, calleeUid: String, hasAudioPermission: Boolean, hasCameraPermission: Boolean): PreFlightResult {
        // Check 1: Enabled settings?
        val audioEnabled = repository.getSetting("p2p_audio_enabled", "true").toBoolean()
        val videoEnabled = repository.getSetting("p2p_video_enabled", "true").toBoolean()
        if (callType == "audio" && !audioEnabled) {
            return PreFlightResult.Error("Audio calls are disabled in Settings")
        }
        if (callType == "video" && !videoEnabled) {
            return PreFlightResult.Error("Video calls are disabled in Settings")
        }

        // Check 2: Network check (Wifi Only constraint)
        val dataUsageSetting = repository.getSetting("p2p_data_usage", "WiFi + Cellular")
        if (dataUsageSetting == "WiFi Only" && isNetworkMetered()) {
            return PreFlightResult.Error("Connect to WiFi to make calls")
        }

        // Check 3: Active call check
        if (_callState.value != CallState.IDLE) {
            _activeCallRecord.value?.let {
                return PreFlightResult.ConfirmEndCurrent(it)
            }
        }

        // Check 4: Microphone Permission
        if (!hasAudioPermission) {
            return PreFlightResult.RequestPermission
        }

        // Check 5: Camera Permission for video
        if (callType == "video" && !hasCameraPermission) {
            return PreFlightResult.OfferAudioOnly
        }

        // Check 6: Prevent calling yourself
        val myProfile = repository.contactDao.getContactById("my_uid_zaxo")
        if (myProfile?.id == calleeUid) {
            return PreFlightResult.Error("Cannot call yourself")
        }

        return PreFlightResult.OK
    }

    // --- ALGORITHM 2: LIVEKIT ROOM CREATION ---
    private suspend fun createAndConnectRoom(callType: String, isGroup: Boolean): Boolean {
        _callState.value = CallState.CREATING_ROOM
        delay(300)
        val roomId = UUID.randomUUID().toString()
        val liveKitUrl = getLiveKitUrl()
        val apiKey = getLiveKitApiKey()
        val apiSecret = getLiveKitApiSecret()
        val myName = repository.contactDao.getContactById("my_uid_zaxo")?.name ?: "User"
        val roomToken = generateLiveKitToken(apiKey, apiSecret, roomId, "my_uid_zaxo", myName, callType == "video")

        return try {
            liveKitRoom = LiveKitClient.connect(
                context, ConnectOptions(
                    url = liveKitUrl,
                    token = roomToken,
                    e2eeEnabled = true,
                    audio = true,
                    video = callType == "video"
                )
            )

            setupRoomListeners()
            true
        } catch (e: Exception) {
            Log.e("CallViewModel", "LiveKit Room connection failed, retrying once...", e)
            try {
                // Retry once
                val retryRoomId = UUID.randomUUID().toString()
                val retryToken = generateLiveKitToken(apiKey, apiSecret, retryRoomId, "my_uid_zaxo", myName, callType == "video")
                liveKitRoom = LiveKitClient.connect(
                    context, ConnectOptions(
                        url = liveKitUrl,
                        token = retryToken,
                        e2eeEnabled = true,
                        audio = true,
                        video = callType == "video"
                    )
                )
                setupRoomListeners()
                true
            } catch (retryEx: Exception) {
                Log.e("CallViewModel", "Retry LiveKit Room connection failed completely", retryEx)
                false
            }
        }
    }

    private fun setupRoomListeners() {
        val room = liveKitRoom ?: return

        // --- ALGORITHM 7: NETWORK QUALITY ADAPTATION ---
        room.onConnectionQualityChanged { quality ->
            when (quality) {
                ConnectionQuality.EXCELLENT, ConnectionQuality.GOOD -> {
                    room.setVideoEncoding(VideoEncoding(maxBitrate = null))
                    _networkQualityMessage.value = null
                }
                ConnectionQuality.POOR -> {
                    room.setVideoEncoding(VideoEncoding(maxBitrate = 128_000))
                    _networkQualityMessage.value = "Poor connection quality"
                }
                ConnectionQuality.LOST -> {
                    room.setVideoEncoding(VideoEncoding(maxBitrate = 64_000))
                    _networkQualityMessage.value = "Connection lost. Reconnecting..."
                    _callState.value = CallState.RECONNECTING
                    startReconnectTimer(30000L)
                }
            }
        }

        // Active speaker detection
        room.onActiveSpeakerChanged { participant ->
            _activeSpeakerId.value = participant.sid
            // For dense grids, bring active speaker to front
            val currentList = _groupParticipants.value.toMutableList()
            val index = currentList.indexOfFirst { it.sid == participant.sid }
            if (index > 0 && currentList.size > 6) {
                val item = currentList.removeAt(index)
                currentList.add(0, item)
                _groupParticipants.value = currentList
            }
        }

        // Join / Leave announcements
        room.onParticipantConnected { participant ->
            showToast("${participant.name} joined the call")
            _groupParticipants.value = _groupParticipants.value + participant
        }

        room.onParticipantDisconnected { participant ->
            showToast("${participant.name} left the call")
            _groupParticipants.value = _groupParticipants.value.filter { it.sid != participant.sid }
            if (_groupParticipants.value.isEmpty() && _callState.value == CallState.GROUP_ACTIVE) {
                endCall()
            }
        }
    }

    // --- ALGORITHM 3: INCOMING CALL PRIVACY GATE ---
    suspend fun evaluateIncomingCall(callerUid: String, callerZaxoNumber: String, callType: String): GateResult {
        // 1. Block filter check
        val contact = repository.contactDao.getContactById(callerUid)
        if (contact != null && contact.isBlocked) {
            return GateResult.SILENT_REJECT
        }

        // 2. Permission visibility constraint
        val callingPermission = repository.getSetting("p2p_calling", "Everyone")
        when (callingPermission) {
            "Nobody" -> return GateResult.REJECTED
            "Contacts" -> {
                val isContact = repository.contactDao.getContactById(callerUid) != null
                if (!isContact) return GateResult.REJECTED
            }
        }

        // 3. Rate limiting check (max 5 non-contact calls per hour)
        val isContact = repository.contactDao.getContactById(callerUid) != null
        if (!isContact) {
            val callHistoryList = historyRepository.getRecentCallsFromUser(callerUid, 3600000L)
            if (callHistoryList.size >= 5) {
                return GateResult.SILENT_REJECT
            }
        }

        return GateResult.ALLOWED
    }

    // --- ALGORITHM 4: RING MODE APPLICATION ---
    suspend fun applyRingMode(): RingBehavior {
        val ringModeSetting = repository.getSetting("ring_mode", "Ring")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Respect DND
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val isDnd = nm.currentInterruptionFilter >= NotificationManager.INTERRUPTION_FILTER_PRIORITY
            if (isDnd) {
                return RingBehavior.SILENT
            }
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val systemRinger = audioManager.ringerMode
        if (systemRinger == AudioManager.RINGER_MODE_SILENT) {
            return RingBehavior.SILENT
        } else if (systemRinger == AudioManager.RINGER_MODE_VIBRATE) {
            return RingBehavior.VIBRATE_ONLY
        }

        return when (ringModeSetting) {
            "Silent" -> RingBehavior.SILENT
            "Vibrate" -> RingBehavior.VIBRATE_ONLY
            else -> RingBehavior.FULL
        }
    }

    // --- ALGORITHM 5: AUTO-ANSWER DECISION TREE ---
    suspend fun shouldAutoAnswer(callerUid: String, callType: String): Boolean {
        // 1. Must be enabled
        val isAutoAnswerEnabled = repository.getSetting("p2p_auto_answer", "false").toBoolean()
        if (!isAutoAnswerEnabled) return false

        // 2. Only audio calls allowed
        if (callType != "audio") return false

        // 3. Must be in contacts
        val isContact = repository.contactDao.getContactById(callerUid) != null
        if (!isContact) return false

        // 4. Headphones and speaker checks
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn) return false

        return true
    }

    // --- ALGORITHM 6: CALL COLLISION DETECTION ---
    suspend fun checkForCollision(myUid: String, calleeUid: String): String? {
        // Query Firestore / Local DB for active incoming call from the same person
        if (_callState.value == CallState.INCOMING && _activeCallRecord.value?.callerId == calleeUid) {
            return _activeCallRecord.value?.id
        }
        return null
    }

    // --- ALGORITHM 9: ZAXO NUMBER LOOKUP & PRIVACY GATE ---
    suspend fun lookupZaxoNumber(number: String): LookupResult {
        val cleanNumber = number.replace("-", "").replace(" ", "").trim()
        val contacts = repository.getContactsList()
        val foundContact = contacts.find { it.zaxoNumber.replace("-", "").replace(" ", "").trim() == cleanNumber }
        
        if (foundContact != null) {
            return LookupResult.Found(
                uid = foundContact.id,
                displayName = foundContact.name,
                avatarUrl = foundContact.avatar,
                canCall = !foundContact.isBlocked
            )
        }
        return LookupResult.NotFound
    }

    // --- OUTGOING CALL ACTIONS ---
    fun startCall(contact: ContactEntity, isVideo: Boolean) {
        viewModelScope.launch {
            // F69: Double tap prevention
            if (_callState.value != CallState.IDLE) return@launch

            _callState.value = CallState.VALIDATING
            _isMuted.value = false
            _isCameraOff.value = !isVideo
            _isSpeakerOn.value = isVideo // default speaker for video

            // Check Collision
            val collisionCallId = checkForCollision("me", contact.id)
            if (collisionCallId != null) {
                // Answer instead
                answerCall()
                return@launch
            }

            // Create LiveKit Room
            val roomCreated = createAndConnectRoom(if (isVideo) "video" else "audio", false)
            if (!roomCreated) {
                _callState.value = CallState.CALL_FAILED
                delay(2000)
                _callState.value = CallState.IDLE
                return@launch
            }

            // Dialing
            _callState.value = CallState.DIALING
            val record = CallRecordEntity(
                id = UUID.randomUUID().toString(),
                callerId = "my_uid_zaxo",
                callerName = (repository.contactDao.getContactById("my_uid_zaxo")?.name ?: "Me"),
                calleeId = contact.id,
                calleeName = contact.name,
                callerAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                calleeAvatar = contact.avatar,
                isVideo = isVideo,
                isGroup = false,
                timestamp = System.currentTimeMillis(),
                status = "RINGING"
            )
            _activeCallRecord.value = record

            delay(1000)
            _callState.value = CallState.RINGING
            ringbackToneManager.start()

            val isBot = contact.id == "zaxo_ai" || contact.id == "support_alice" || contact.id == "security_bot"
            if (!isBot) {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val callMap = hashMapOf(
                        "id" to record.id,
                        "callerId" to "my_uid_zaxo",
                        "callerName" to (repository.contactDao.getContactById("my_uid_zaxo")?.name ?: "Zaxo User"),
                        "callerAvatar" to "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                        "calleeId" to contact.id,
                        "isVideo" to isVideo,
                        "status" to "RINGING",
                        "timestamp" to System.currentTimeMillis()
                    )
                    firestore.collection("calls").document(record.id).set(callMap)
                    listenToCurrentCallDoc(record.id)
                } catch (e: Exception) {
                    Log.e("CallViewModel", "Failed to publish call document to Firestore", e)
                }
            } else {
                // Simulate call answer delay for bot
                delay(3000)
                connectCall()
            }
        }
    }

    private fun connectCall() {
        val record = _activeCallRecord.value ?: return
        ringbackToneManager.fadeOut()
        _callState.value = CallState.CONNECTING
        
        viewModelScope.launch {
            delay(500)
            _callState.value = CallState.ACTIVE
            _activeCallRecord.value = record.copy(status = "CONNECTED")

            // Start timer
            callTimer.start()
            startTimerUpdates()
            applyProximityLock()
        }
    }

    // --- INCOMING CALL FLOW ---
    fun triggerIncomingCallSimulated(caller: ContactEntity, isVideo: Boolean) {
        viewModelScope.launch {
            val gate = evaluateIncomingCall(caller.id, caller.zaxoNumber, if (isVideo) "video" else "audio")
            if (gate == GateResult.SILENT_REJECT) {
                // Rejected silently without logging or UI
                Log.d("CallViewModel", "Incoming call silently rejected by Privacy Gate")
                return@launch
            } else if (gate == GateResult.REJECTED) {
                // Log missed call in DB
                historyRepository.saveCallRecord(
                    caller.id, caller.name, "me", "Me", caller.avatar, "", isVideo, false, 0, "REJECTED"
                )
                return@launch
            }

            val record = CallRecordEntity(
                id = UUID.randomUUID().toString(),
                callerId = caller.id,
                callerName = caller.name,
                calleeId = "me",
                calleeName = "Me",
                callerAvatar = caller.avatar,
                calleeAvatar = "",
                isVideo = isVideo,
                isGroup = false,
                timestamp = System.currentTimeMillis(),
                status = "RINGING"
            )

            // --- ALGORITHM 10: CALL WAITING ---
            if (_callState.value == CallState.ACTIVE || _callState.value == CallState.HELD) {
                val accepted = callWaitingManager.onIncomingCallWhileActive(record, callWaitingEnabled = true)
                if (accepted) {
                    _callState.value = CallState.CALL_WAITING
                    incomingRingtoneManager.start("Vibrate") // Gentle vibration overlay
                }
                return@launch
            }

            _activeCallRecord.value = record
            _callState.value = CallState.INCOMING

            val ringBehavior = applyRingMode()
            incomingRingtoneManager.start(ringBehavior.name)

            // Auto-Answer decision
            if (shouldAutoAnswer(caller.id, if (isVideo) "video" else "audio")) {
                showToast("Auto-answering in 3 seconds...")
                autoAnswerHandler = Handler(Looper.getMainLooper())
                autoAnswerHandler?.postDelayed({
                    answerCall()
                }, 3000L)
            }
        }
    }

    fun answerCall() {
        autoAnswerHandler?.removeCallbacksAndMessages(null)
        incomingRingtoneManager.stop()
        val record = _activeCallRecord.value ?: return

        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("calls").document(record.id).update("status", "CONNECTED")
        } catch (e: Exception) {
            Log.e("CallViewModel", "Failed to update call status to CONNECTED in Firestore", e)
        }

        viewModelScope.launch {
            _callState.value = CallState.CONNECTING
            val roomCreated = createAndConnectRoom(if (record.isVideo) "video" else "audio", false)
            if (!roomCreated) {
                _callState.value = CallState.CALL_FAILED
                delay(2000)
                _callState.value = CallState.IDLE
                return@launch
            }

            delay(500)
            _callState.value = CallState.ACTIVE
            _activeCallRecord.value = record.copy(status = "CONNECTED")

            callTimer.start()
            startTimerUpdates()
            applyProximityLock()
        }
    }

    fun declineCall(msg: String? = null) {
        autoAnswerHandler?.removeCallbacksAndMessages(null)
        incomingRingtoneManager.stop()
        val record = _activeCallRecord.value ?: return

        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("calls").document(record.id).update("status", "REJECTED")
        } catch (e: Exception) {
            Log.e("CallViewModel", "Failed to update call status to REJECTED in Firestore", e)
        }

        viewModelScope.launch {
            _callState.value = CallState.CALL_DECLINED
            historyRepository.saveCallRecord(
                record.callerId, record.callerName, record.calleeId, record.calleeName,
                record.callerAvatar, record.calleeAvatar, record.isVideo, record.isGroup,
                0, "DECLINED"
            )

            if (msg != null) {
                // Send chat message response
                repository.sendMessage(record.callerId, msg)
            }

            delay(1000)
            _callState.value = CallState.IDLE
            _activeCallRecord.value = null
        }
    }

    // --- CALL WAITING INTERACTION ---
    fun acceptWaitingCall() {
        incomingRingtoneManager.stop()
        callWaitingManager.acceptNewCall()
        _activeCallRecord.value = callWaitingManager.activeCall.value
        _callState.value = CallState.ACTIVE
        applyProximityLock()
    }

    fun endCurrentAndAcceptWaiting() {
        incomingRingtoneManager.stop()
        // Save history for current ended
        _activeCallRecord.value?.let {
            saveFinishedCallRecord(it)
        }
        callWaitingManager.endCurrentAcceptNew()
        _activeCallRecord.value = callWaitingManager.activeCall.value
        _callState.value = CallState.ACTIVE
        applyProximityLock()
    }

    fun declineWaitingCall() {
        incomingRingtoneManager.stop()
        callWaitingManager.declineNewCall()
        _callState.value = CallState.ACTIVE
    }

    fun swapActiveHeldCalls() {
        callWaitingManager.swapCalls()
        _activeCallRecord.value = callWaitingManager.activeCall.value
    }

    // --- GENERAL CONTROLS ---
    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        liveKitRoom?.setAudioEnabled(!_isMuted.value)
    }

    fun toggleCamera() {
        _isCameraOff.value = !_isCameraOff.value
        liveKitRoom?.setVideoEnabled(!_isCameraOff.value)
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = _isSpeakerOn.value
        applyProximityLock()
    }

    fun flipCamera() {
        showToast("Switching camera...")
    }

    fun toggleHold() {
        if (_callState.value == CallState.ACTIVE) {
            _callState.value = CallState.HELD
            callTimer.hold()
            liveKitRoom?.setAudioEnabled(false)
            liveKitRoom?.setVideoEnabled(false)
        } else if (_callState.value == CallState.HELD) {
            _callState.value = CallState.ACTIVE
            callTimer.resume()
            liveKitRoom?.setAudioEnabled(!_isMuted.value)
            liveKitRoom?.setVideoEnabled(!_isCameraOff.value)
        }
    }

    fun endCall() {
        ringbackToneManager.stop()
        incomingRingtoneManager.stop()
        autoAnswerHandler?.removeCallbacksAndMessages(null)
        proximitySensorManager.release()
        liveKitRoom?.disconnect()
        liveKitRoom = null

        val record = _activeCallRecord.value ?: return

        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("calls").document(record.id).update("status", "DISCONNECTED")
        } catch (e: Exception) {
            Log.e("CallViewModel", "Failed to update call status to DISCONNECTED in Firestore", e)
        }

        viewModelScope.launch {
            _callState.value = CallState.ENDED
            timerJob?.cancel()

            val duration = callTimer.getElapsedSeconds().toLong()
            saveFinishedCallRecord(record.copy(durationSeconds = duration, status = "CONNECTED"))

            // Send ended update in Chat Thread too
            val minutes = duration / 60
            val secs = duration % 60
            val durationText = String.format("%02d:%02d", minutes, secs)
            val chatTarget = if (record.callerId == "me") record.calleeId else record.callerId
            repository.sendMessage(chatTarget, "Call ended • Duration $durationText")

            _callState.value = CallState.POST_CALL
        }
    }

    fun dismissPostCall() {
        _callState.value = CallState.IDLE
        _activeCallRecord.value = null
        callWaitingManager.clear()
        _isMuted.value = false
        _isCameraOff.value = false
        _isSpeakerOn.value = false
    }

    private fun saveFinishedCallRecord(record: CallRecordEntity) {
        viewModelScope.launch {
            historyRepository.saveCallRecord(
                record.callerId, record.callerName, record.calleeId, record.calleeName,
                record.callerAvatar, record.calleeAvatar, record.isVideo, record.isGroup,
                record.durationSeconds, record.status
            )
        }
    }

    // --- RECONNECT TIMER ---
    private fun startReconnectTimer(timeMs: Long) {
        reconnectTimerJob?.cancel()
        reconnectTimerJob = viewModelScope.launch {
            delay(timeMs)
            if (_callState.value == CallState.RECONNECTING) {
                _toastMessage.value = "Call disconnected completely due to network timeout."
                endCall()
            }
        }
    }

    // --- WIFI <-> CELLULAR HANDOFF ---
    private fun setupNetworkCallback() {
        try {
            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    // ICE restart for LiveKit connection restoration
                    if (_callState.value == CallState.RECONNECTING) {
                        viewModelScope.launch {
                            reconnectTimerJob?.cancel()
                            showToast("Network restored. Reconnected call.")
                            _callState.value = CallState.ACTIVE
                            _networkQualityMessage.value = null
                            liveKitRoom?.updateConnectionQuality(ConnectionQuality.EXCELLENT)
                        }
                    }
                }

                override fun onLost(network: Network) {
                    if (_callState.value == CallState.ACTIVE) {
                        viewModelScope.launch {
                            _callState.value = CallState.RECONNECTING
                            _networkQualityMessage.value = "Network lost. Reconnecting..."
                            liveKitRoom?.updateConnectionQuality(ConnectionQuality.LOST)
                        }
                    }
                }
            }

            connectivityManager?.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.e("CallViewModel", "Failed to register network callback", e)
        }
    }

    private fun isNetworkMetered(): Boolean {
        val cm = connectivityManager ?: return false
        return cm.isActiveNetworkMetered
    }

    private fun startTimerUpdates() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_callState.value == CallState.ACTIVE || _callState.value == CallState.HELD) {
                _callTimerString.value = callTimer.format()
                delay(1000)
            }
        }
    }

    private fun applyProximityLock() {
        val shouldLock = proximitySensorManager.shouldActivate(
            callType = if (_activeCallRecord.value?.isVideo == true) "video" else "audio",
            isSpeakerOn = _isSpeakerOn.value,
            isBluetoothConnected = _isBluetoothConnected.value,
            callActive = _callState.value == CallState.ACTIVE
        )
        if (shouldLock) {
            proximitySensorManager.acquire()
        } else {
            proximitySensorManager.release()
        }
    }

    private fun showToast(msg: String) {
        _toastMessage.value = msg
        viewModelScope.launch {
            delay(2500)
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // ignore
        }
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            // ignore
        }
        proximitySensorManager.release()
        ringbackToneManager.stop()
        incomingRingtoneManager.stop()
        autoAnswerHandler?.removeCallbacksAndMessages(null)
    }
}

// Extension to fetch contacts easily
private fun ZaxoRepository.getContactsList(): List<ContactEntity> {
    // Return standard cached list or prepopulated lists
    return listOf(
        ContactEntity("zaxo_ai", "Zaxo AI Assistant", "999-999-999", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150"),
        ContactEntity("support_alice", "Alice (Zaxo Support)", "111-222-333", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"),
        ContactEntity("security_bot", "Zaxo Security Bot", "888-888-888", "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=150"),
        ContactEntity("bob_builder", "Bob Builder", "284-716-593", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150")
    )
}
