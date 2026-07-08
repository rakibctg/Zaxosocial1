package com.zaxo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaxo.data.local.*
import com.zaxo.ui.neumorphic.*
import com.zaxo.ui.viewmodel.CallState
import com.zaxo.ui.viewmodel.MainViewModel
import com.zaxo.ui.viewmodel.CallViewModel
import com.zaxo.ui.viewmodel.SettingsViewModel
import com.zaxo.ui.viewmodel.DeleteAccountState

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val bgColor = NeuTheme.getBgColor(darkTheme)
    val textColor = NeuTheme.getTextColor(darkTheme)

    var currentTab by remember { mutableStateOf(0) } // 0: Chats, 1: Calls, 2: Status, 3: Settings
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showDialpad by remember { mutableStateOf(false) }
    var showNewStatusDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = bgColor,
        bottomBar = {
            NeumorphicBottomBar(
                selectedTab = currentTab,
                onTabSelected = { currentTab = it },
                darkTheme = darkTheme
            )
        },
        floatingActionButton = {
            // Context-Aware FAB with Spring Animation
            AnimatedVisibility(
                visible = currentTab != 3, // Hidden on Settings tab
                enter = scaleIn(animationSpec = spring()) + fadeIn(),
                exit = scaleOut(animationSpec = spring()) + fadeOut()
            ) {
                NeumorphicIconButton(
                    onClick = {
                        when (currentTab) {
                            0 -> showNewChatDialog = true
                            1 -> showDialpad = true
                            2 -> showNewStatusDialog = true
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    cornerRadius = 28.dp,
                    darkTheme = darkTheme
                ) {
                    Icon(
                        imageVector = when (currentTab) {
                            0 -> Icons.Default.Chat
                            1 -> Icons.Default.Dialpad
                            else -> Icons.Default.AddPhotoAlternate
                        },
                        contentDescription = "FAB Action",
                        tint = NeuTheme.AccentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth > 600.dp
            when (currentTab) {
                0 -> ChatsTab(viewModel, onNavigateToChat, onSettingsClick = { currentTab = 3 }, darkTheme)
                1 -> CallsTab(viewModel, onNavigateToCall, darkTheme)
                2 -> StatusTab(viewModel, darkTheme)
                3 -> SettingsTab(viewModel, darkTheme, onClose = { currentTab = 0 })
            }

            // New Chat Selection Dialog
            if (showNewChatDialog) {
                NewChatPicker(
                    viewModel = viewModel,
                    onDismiss = { showNewChatDialog = false },
                    onContactSelected = { contact ->
                        viewModel.createChat(contact)
                        showNewChatDialog = false
                        onNavigateToChat(contact.id)
                    },
                    darkTheme = darkTheme
                )
            }

            // Soft Neumorphic Dialpad Dialog
            if (showDialpad) {
                val callViewModel: CallViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                DialpadScreen(
                    viewModel = callViewModel,
                    onDismiss = { showDialpad = false },
                    onPlaceCall = { contact, isVideo ->
                        callViewModel.startCall(contact, isVideo)
                        showDialpad = false
                        onNavigateToCall()
                    },
                    darkTheme = darkTheme
                )
            }

            // New Status Dialog
            if (showNewStatusDialog) {
                NewStatusComposer(
                    viewModel = viewModel,
                    onDismiss = { showNewStatusDialog = false },
                    darkTheme = darkTheme
                )
            }
        }
    }
}

// --- TAB 1: CHATS ---
@Composable
fun ChatsTab(
    viewModel: MainViewModel,
    onNavigateToChat: (String) -> Unit,
    onSettingsClick: () -> Unit,
    darkTheme: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val chats by viewModel.activeChats.collectAsState()
    val archivedChats by viewModel.archivedChats.collectAsState()
    val typingMap by viewModel.typingIndicators.collectAsState()
    
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)
    var searchChatText by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var showContextMenuForChat by remember { mutableStateOf<ChatEntity?>(null) }
    var showDeleteConfirmForChat by remember { mutableStateOf<ChatEntity?>(null) }
    var archivedExpanded by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // Filter active chats by name or last message (debounced filtering simulation)
    val filteredChats = chats.filter {
        it.name.contains(searchChatText, ignoreCase = true) || 
        it.lastMessage.contains(searchChatText, ignoreCase = true)
    }

    // Filter archived chats by query
    val filteredArchived = archivedChats.filter {
        it.name.contains(searchChatText, ignoreCase = true) || 
        it.lastMessage.contains(searchChatText, ignoreCase = true)
    }

    // Sort active chats using multi-key sort algorithm: isPinned DESC then lastMessageTime DESC
    val sortedActiveChats = filteredChats.sortedWith(
        compareByDescending<ChatEntity> { it.isPinned }
            .thenByDescending { it.lastMessageTime }
    )

    // Separate into Pinned and Recent lists
    val pinnedChats = sortedActiveChats.filter { it.isPinned }
    val recentChats = sortedActiveChats.filter { !it.isPinned }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        val currentUser by viewModel.currentUser.collectAsState()
        val rawName = currentUser?.name?.trim() ?: ""
        val userName = if (rawName.lowercase() == "google user" || rawName.isEmpty()) "" else rawName

        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        var currentHour by remember { androidx.compose.runtime.mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) }

        androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        val timeGreeting = when (currentHour) {
            in 0..4 -> "Good night"
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }

        // Top bar and profile section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (userName.isNotEmpty()) {
                    Text(
                        text = "$timeGreeting,",
                        color = textColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = userName,
                        color = NeuTheme.AccentColor,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = timeGreeting,
                        color = textColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Set your name",
                        color = NeuTheme.AccentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier
                            .clickable { onSettingsClick() }
                            .padding(top = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pull-to-refresh manually triggered or simulated trigger icon
                NeumorphicIconButton(
                    onClick = {
                        scope.launch {
                            isRefreshing = true
                            kotlinx.coroutines.delay(1200) // Simulated sync delay
                            isRefreshing = false
                            android.widget.Toast.makeText(context, "Up to date: Chats and presence synced", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    cornerRadius = 20.dp,
                    darkTheme = darkTheme
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = NeuTheme.AccentColor,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = NeuTheme.AccentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Custom Avatar Container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NeuTheme.AccentLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUser?.avatar != null && currentUser?.avatar!!.isNotEmpty()) {
                        AsyncImage(
                            model = currentUser?.avatar,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFF21005D),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Custom Search Input Field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .neuShadow(cornerRadius = 12.dp, isSunken = true, darkTheme = darkTheme)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = secondaryText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (searchChatText.isEmpty()) {
                    Text(
                        text = "Search",
                        color = secondaryText.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = searchChatText,
                    onValueChange = { searchChatText = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = textColor, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Unread summary text
        val totalUnread = chats.sumOf { it.unreadCount }
        Text(
            text = if (totalUnread > 0) "You have $totalUnread unread messages" else "No new messages",
            color = if (totalUnread > 0) NeuTheme.AccentColor else secondaryText,
            fontSize = 13.sp,
            fontWeight = if (totalUnread > 0) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        // Main Chat Scrollable Body
        if (sortedActiveChats.isEmpty() && archivedChats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = secondaryText,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No chats yet",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Start a chat",
                        color = secondaryText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Pinned Chats
                if (pinnedChats.isNotEmpty()) {
                    item(key = "header_pinned") {
                        Text(
                            text = "PINNED CHATS",
                            color = NeuTheme.AccentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(pinnedChats, key = { "pinned_" + it.id }) { chat ->
                        val isPeerTyping = typingMap.containsKey(chat.id)
                        ChatRowItem(
                            chat = chat,
                            isTyping = isPeerTyping,
                            onClick = {
                                viewModel.selectChat(chat.id)
                                onNavigateToChat(chat.id)
                            },
                            onLongClick = {
                                showContextMenuForChat = chat
                            },
                            darkTheme = darkTheme
                        )
                    }
                }

                // Section 2: Recent Chats
                if (recentChats.isNotEmpty()) {
                    item(key = "header_recent") {
                        Text(
                            text = "RECENT CHATS",
                            color = secondaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(recentChats, key = { "recent_" + it.id }) { chat ->
                        val isPeerTyping = typingMap.containsKey(chat.id)
                        ChatRowItem(
                            chat = chat,
                            isTyping = isPeerTyping,
                            onClick = {
                                viewModel.selectChat(chat.id)
                                onNavigateToChat(chat.id)
                            },
                            onLongClick = {
                                showContextMenuForChat = chat
                            },
                            darkTheme = darkTheme
                        )
                    }
                }

                // Section 3: Archived Chats (Collapsible)
                if (archivedChats.isNotEmpty() || filteredArchived.isNotEmpty()) {
                    item(key = "header_archived") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { archivedExpanded = !archivedExpanded }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ARCHIVED CHATS (${filteredArchived.size})",
                                color = secondaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Icon(
                                imageVector = if (archivedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Archived",
                                tint = secondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (archivedExpanded) {
                        items(filteredArchived, key = { "archived_" + it.id }) { chat ->
                            val isPeerTyping = typingMap.containsKey(chat.id)
                            ChatRowItem(
                                chat = chat,
                                isTyping = isPeerTyping,
                                onClick = {
                                    viewModel.selectChat(chat.id)
                                    onNavigateToChat(chat.id)
                                },
                                onLongClick = {
                                    showContextMenuForChat = chat
                                },
                                darkTheme = darkTheme
                            )
                        }
                    }
                }
            }
        }
    }

    // Long Press Context Menu Dialog
    if (showContextMenuForChat != null) {
        val targetChat = showContextMenuForChat!!
        AlertDialog(
            onDismissRequest = { showContextMenuForChat = null },
            containerColor = NeuTheme.getBgColor(darkTheme),
            title = { Text(targetChat.name, color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Pin/Unpin Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!targetChat.isPinned) {
                                    val pinnedCount = chats.count { it.isPinned }
                                    if (pinnedCount >= 3) {
                                        android.widget.Toast.makeText(context, "Max 3 pinned chats allowed!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.pinChat(targetChat.id, true)
                                    }
                                } else {
                                    viewModel.pinChat(targetChat.id, false)
                                }
                                showContextMenuForChat = null
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = if (targetChat.isPinned) NeuTheme.AccentColor else secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(if (targetChat.isPinned) "Unpin Conversation" else "Pin Conversation (Max 3)", color = textColor, fontSize = 14.sp)
                    }

                    // Mute/Unmute Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.muteChat(targetChat.id, !targetChat.isMuted)
                                showContextMenuForChat = null
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (targetChat.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = if (targetChat.isMuted) NeuTheme.AccentColor else secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(if (targetChat.isMuted) "Unmute Chat Notifications" else "Mute Chat Notifications", color = textColor, fontSize = 14.sp)
                    }

                    // Archive/Unarchive Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.archiveChat(targetChat.id, !targetChat.isArchived)
                                showContextMenuForChat = null
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = if (targetChat.isArchived) NeuTheme.AccentColor else secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(if (targetChat.isArchived) "Unarchive Conversation" else "Archive Conversation", color = textColor, fontSize = 14.sp)
                    }

                    // Mark as Unread Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.markChatUnread(targetChat.id)
                                showContextMenuForChat = null
                                android.widget.Toast.makeText(context, "Marked as unread", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkChatRead,
                            contentDescription = null,
                            tint = secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Mark as Unread", color = textColor, fontSize = 14.sp)
                    }

                    HorizontalDivider(color = secondaryText.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                    // Delete Chat Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDeleteConfirmForChat = targetChat
                                showContextMenuForChat = null
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Delete Chat (Cascade Messages)", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showContextMenuForChat = null }) {
                    Text("Cancel", color = textColor)
                }
            }
        )
    }

    // Cascade Delete Confirmation Dialog
    if (showDeleteConfirmForChat != null) {
        val targetChat = showDeleteConfirmForChat!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmForChat = null },
            containerColor = NeuTheme.getBgColor(darkTheme),
            title = { Text("Delete entire chat?", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will permanently delete your conversation with ${targetChat.name}, including all end-to-end encrypted messages stored locally. This action cannot be undone.",
                    color = secondaryText,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChat(targetChat.id)
                        showDeleteConfirmForChat = null
                        android.widget.Toast.makeText(context, "Chat and messages deleted successfully", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete Everywhere", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmForChat = null }) {
                    Text("Cancel", color = textColor)
                }
            }
        )
        }
    }
}

// Custom Date Formatting Helper matching Algorithm 2.4 / timezone rules
fun formatChatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val calendar = java.util.Calendar.getInstance()
    val today = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    val todayYear = calendar.get(java.util.Calendar.YEAR)
    
    val msgCalendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val msgDay = msgCalendar.get(java.util.Calendar.DAY_OF_YEAR)
    val msgYear = msgCalendar.get(java.util.Calendar.YEAR)
    
    val sdfTime = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    val sdfDate = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
    
    return when {
        msgYear == todayYear && msgDay == today -> sdfTime.format(java.util.Date(timestamp))
        msgYear == todayYear && today - msgDay == 1 -> "Yesterday"
        msgYear == todayYear && today - msgDay < 7 && today - msgDay > 0 -> {
            val dayFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
            dayFormat.format(java.util.Date(timestamp))
        }
        else -> sdfDate.format(java.util.Date(timestamp))
    }
}

// Custom Last Message formatting matching Section 2.3
fun formatLastMessage(lastMsg: String): String {
    return when {
        lastMsg.contains("PHOTO_MESSAGE") || lastMsg.contains("photo_message") -> "📷 Photo"
        lastMsg.contains("VIDEO_MESSAGE") || lastMsg.contains("video_message") -> "🎬 Video"
        lastMsg.contains("VOICE_MESSAGE") || lastMsg.contains("voice_message") || lastMsg.contains("Voice note sent") -> "🎤 Voice message"
        lastMsg.contains("DOCUMENT_MESSAGE") || lastMsg.contains("document_message") -> "📄 Document"
        lastMsg.contains("STICKER_MESSAGE") || lastMsg.contains("sticker_message") -> "😊 Sticker"
        lastMsg.contains("LOCATION_MESSAGE") || lastMsg.contains("location_message") -> "📍 Location"
        lastMsg.contains("This message was deleted") -> "This message was deleted"
        else -> if (lastMsg.length > 40) lastMsg.take(37) + "..." else lastMsg
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatRowItem(
    chat: ChatEntity,
    isTyping: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    darkTheme: Boolean
) {
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)

    NeumorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        cornerRadius = 16.dp,
        darkTheme = darkTheme
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: Avatar (56dp neumorphic circle) with status dot overlay
            Box(modifier = Modifier.size(56.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (darkTheme) Color(0xFF24222E) else Color(0xFFEADBFC)),
                    contentAlignment = Alignment.Center
                ) {
                    if (chat.avatar.isNotBlank()) {
                        AsyncImage(
                            model = chat.avatar,
                            contentDescription = chat.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = chat.name.take(2).uppercase(),
                            color = NeuTheme.AccentColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Presence / Typing Indicator Overlay
                val isAlwaysOnline = chat.id == "zaxo_ai" || chat.id == "support_alice" || chat.id == "security_bot"
                if (!chat.isGroup) {
                    if (isTyping) {
                        // Animated typing dots overlay replacing online status
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color(0xFF27AE60), CircleShape)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("•••", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(if (isAlwaysOnline) Color(0xFF27AE60) else Color(0xFFC8CDD4), CircleShape)
                                .align(Alignment.BottomEnd)
                                .neuShadow(cornerRadius = 6.dp, isSunken = false, darkTheme = darkTheme)
                        )
                    }
                }
            }

            // Center Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = chat.name,
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                        if (chat.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = NeuTheme.AccentColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        if (chat.isMuted) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "Muted",
                                tint = secondaryText.copy(alpha = 0.6f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Timestamp matching timezone logic (Algorithm 2.4)
                    Text(
                        text = formatChatTime(chat.lastMessageTime),
                        color = secondaryText,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTyping) {
                        Text(
                            text = "typing...",
                            color = Color(0xFF27AE60),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        val isDeleted = chat.lastMessage == "This message was deleted"
                        Text(
                            text = formatLastMessage(chat.lastMessage),
                            color = if (isDeleted) secondaryText.copy(alpha = 0.5f) else secondaryText,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                            fontStyle = if (isDeleted) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Unread Count Badge matching Section 2.3
                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                .background(NeuTheme.AccentColor, CircleShape)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 2: CALLS ---
@Composable
fun CallsTab(
    viewModel: MainViewModel,
    onNavigateToCall: () -> Unit,
    darkTheme: Boolean
) {
    val callLogs by viewModel.callHistory.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val callViewModel: CallViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)

    var showCallOptionsSheet by remember { mutableStateOf(false) }
    var showDialpad by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }

    fun lookupByEmail(email: String, contactsList: List<ContactEntity>): ContactEntity? {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail == "alice@zaxo.com" || cleanEmail.contains("alice")) {
            return contactsList.find { it.id == "support_alice" }
        }
        if (cleanEmail == "ai@zaxo.com" || cleanEmail.contains("ai") || cleanEmail.contains("assistant")) {
            return contactsList.find { it.id == "zaxo_ai" }
        }
        if (cleanEmail == "bob@zaxo.com" || cleanEmail.contains("bob")) {
            return contactsList.find { it.id == "bob_builder" }
        }
        if (cleanEmail == "security@zaxo.com" || cleanEmail.contains("security")) {
            return contactsList.find { it.id == "security_bot" }
        }
        return contactsList.find { 
            it.name.lowercase().contains(cleanEmail.substringBefore("@")) || 
            it.zaxoNumber.replace("-", "").contains(cleanEmail.substringBefore("@"))
        }
    }

    fun lookupByUsername(username: String, contactsList: List<ContactEntity>): ContactEntity? {
        val cleanUser = username.trim().lowercase().removePrefix("@")
        if (cleanUser == "alice") {
            return contactsList.find { it.id == "support_alice" }
        }
        if (cleanUser == "ai" || cleanUser == "assistant") {
            return contactsList.find { it.id == "zaxo_ai" }
        }
        if (cleanUser == "bob") {
            return contactsList.find { it.id == "bob_builder" }
        }
        if (cleanUser == "security") {
            return contactsList.find { it.id == "security_bot" }
        }
        return contactsList.find { 
            it.name.lowercase().contains(cleanUser) || 
            it.id.lowercase().contains(cleanUser)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Calls",
                color = textColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            NeumorphicIconButton(
                onClick = { showCallOptionsSheet = true },
                modifier = Modifier.size(40.dp),
                cornerRadius = 20.dp,
                darkTheme = darkTheme
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Call",
                    tint = NeuTheme.AccentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (callLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = secondaryText,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No calls",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Make a call",
                        color = secondaryText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(callLogs) { log ->
                    CallLogItem(log = log, darkTheme = darkTheme)
                }
            }
        }
        }
    }

    // New Call options dialog
    if (showCallOptionsSheet) {
        AlertDialog(
            onDismissRequest = { showCallOptionsSheet = false },
            containerColor = NeuTheme.getBgColor(darkTheme),
            title = { Text("New Call", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NeumorphicButton(
                        onClick = {
                            showCallOptionsSheet = false
                            showDialpad = true
                        },
                        darkTheme = darkTheme,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Dialpad, contentDescription = null, tint = NeuTheme.AccentColor)
                            Text("Call by Zaxo Number", color = textColor, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    NeumorphicButton(
                        onClick = {
                            showCallOptionsSheet = false
                            showEmailDialog = true
                        },
                        darkTheme = darkTheme,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = NeuTheme.AccentColor)
                            Text("Call by Email", color = textColor, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    NeumorphicButton(
                        onClick = {
                            showCallOptionsSheet = false
                            showUsernameDialog = true
                        },
                        darkTheme = darkTheme,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = NeuTheme.AccentColor)
                            Text("Call by Username", color = textColor, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    NeumorphicButton(
                        onClick = {
                            showCallOptionsSheet = false
                            showContactPicker = true
                        },
                        darkTheme = darkTheme,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, tint = NeuTheme.AccentColor)
                            Text("Call from Contacts", color = textColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCallOptionsSheet = false }) {
                    Text("Cancel", color = NeuTheme.AccentColor)
                }
            }
        )
    }

    if (showDialpad) {
        DialpadScreen(
            viewModel = callViewModel,
            onDismiss = { showDialpad = false },
            onPlaceCall = { contact, isVideo ->
                callViewModel.startCall(contact, isVideo)
                showDialpad = false
                onNavigateToCall()
            },
            darkTheme = darkTheme
        )
    }

    if (showEmailDialog) {
        var emailInput by remember { mutableStateOf("") }
        var searchResult by remember { mutableStateOf<ContactEntity?>(null) }
        var hasSearched by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            containerColor = NeuTheme.getBgColor(darkTheme),
            title = { Text("Call by Email", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = emailInput,
                        onValueChange = { 
                            emailInput = it 
                            hasSearched = false
                        },
                        label = { Text("Email address", color = secondaryText) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeuTheme.AccentColor,
                            unfocusedBorderColor = secondaryText
                        )
                    )

                    NeumorphicButton(
                        onClick = {
                            searchResult = lookupByEmail(emailInput, contacts)
                            hasSearched = true
                        },
                        darkTheme = darkTheme,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Search", color = textColor, fontWeight = FontWeight.Bold)
                    }

                    if (hasSearched) {
                        if (searchResult != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.size(40.dp)) {
                                    if (searchResult!!.avatar.isNotEmpty()) {
                                        AsyncImage(
                                            model = searchResult!!.avatar,
                                            contentDescription = null,
                                            modifier = Modifier.clip(CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(NeuTheme.AccentLight, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(searchResult!!.name.take(1).uppercase(), color = NeuTheme.AccentColor, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(searchResult!!.name, color = textColor, fontWeight = FontWeight.Bold)
                                    Text(searchResult!!.zaxoNumber, color = secondaryText, fontSize = 12.sp)
                                }

                                NeumorphicIconButton(
                                    onClick = {
                                        callViewModel.startCall(searchResult!!, false)
                                        showEmailDialog = false
                                        onNavigateToCall()
                                    },
                                    modifier = Modifier.size(40.dp),
                                    cornerRadius = 20.dp,
                                    darkTheme = darkTheme
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = NeuTheme.AccentColor)
                                }
                            }
                        } else {
                            Text("No Zaxo account with this email", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmailDialog = false }) {
                    Text("Close", color = NeuTheme.AccentColor)
                }
            }
        )
    }

    if (showUsernameDialog) {
        var usernameInput by remember { mutableStateOf("") }
        var searchResult by remember { mutableStateOf<ContactEntity?>(null) }
        var hasSearched by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            containerColor = NeuTheme.getBgColor(darkTheme),
            title = { Text("Call by Username", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { 
                            usernameInput = it 
                            hasSearched = false
                        },
                        label = { Text("Username (e.g. @alice)", color = secondaryText) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeuTheme.AccentColor,
                            unfocusedBorderColor = secondaryText
                        )
                    )

                    NeumorphicButton(
                        onClick = {
                            searchResult = lookupByUsername(usernameInput, contacts)
                            hasSearched = true
                        },
                        darkTheme = darkTheme,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Search", color = textColor, fontWeight = FontWeight.Bold)
                    }

                    if (hasSearched) {
                        if (searchResult != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.size(40.dp)) {
                                    if (searchResult!!.avatar.isNotEmpty()) {
                                        AsyncImage(
                                            model = searchResult!!.avatar,
                                            contentDescription = null,
                                            modifier = Modifier.clip(CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(NeuTheme.AccentLight, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(searchResult!!.name.take(1).uppercase(), color = NeuTheme.AccentColor, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(searchResult!!.name, color = textColor, fontWeight = FontWeight.Bold)
                                    Text(searchResult!!.zaxoNumber, color = secondaryText, fontSize = 12.sp)
                                }

                                NeumorphicIconButton(
                                    onClick = {
                                        callViewModel.startCall(searchResult!!, false)
                                        showUsernameDialog = false
                                        onNavigateToCall()
                                    },
                                    modifier = Modifier.size(40.dp),
                                    cornerRadius = 20.dp,
                                    darkTheme = darkTheme
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = NeuTheme.AccentColor)
                                }
                            }
                        } else {
                            Text("Username not found", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUsernameDialog = false }) {
                    Text("Close", color = NeuTheme.AccentColor)
                }
            }
        )
    }

    if (showContactPicker) {
        NewChatPicker(
            viewModel = viewModel,
            onDismiss = { showContactPicker = false },
            onContactSelected = { contact ->
                showContactPicker = false
                callViewModel.startCall(contact, false)
                onNavigateToCall()
            },
            darkTheme = darkTheme
        )
    }
}

@Composable
fun CallLogItem(
    log: CallRecordEntity,
    darkTheme: Boolean
) {
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)

    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        darkTheme = darkTheme
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.size(46.dp)) {
                AsyncImage(
                    model = if (log.callerId == "me") log.calleeAvatar else log.callerAvatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (log.callerId == "me") log.calleeName else log.callerName,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (log.callerId == "me") Icons.Default.ArrowOutward else Icons.Default.CallReceived,
                        contentDescription = null,
                        tint = if (log.status == "MISSED") Color.Red else Color.Green,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (log.status == "MISSED") "Missed Call" else "Completed (${log.durationSeconds}s)",
                        color = secondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                imageVector = if (log.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = null,
                tint = NeuTheme.AccentColor
            )
        }
    }
}

// --- TAB 3: STATUS / STORIES ---
@Composable
fun StatusTab(
    viewModel: MainViewModel,
    darkTheme: Boolean
) {
    val statuses by viewModel.activeStatuses.collectAsState()
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)
    var selectedStatusForView by remember { mutableStateOf<StatusEntity?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Text(
            text = "Status Feed",
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        // My Status Item
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            darkTheme = darkTheme
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(NeuTheme.AccentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Post Status",
                        tint = Color.White
                    )
                }

                Column {
                    Text(
                        text = "My status",
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Add status",
                        color = secondaryText,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Text(
            text = "RECENT UPDATES",
            color = secondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        if (statuses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No updates",
                    color = secondaryText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(statuses) { status ->
                    NeumorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.viewStatus(status.id)
                                selectedStatusForView = status
                            },
                        cornerRadius = 16.dp,
                        darkTheme = darkTheme
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Render Status Border Ring around avatar (F18-F22)
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(NeuTheme.AccentColor, CircleShape)
                                    .padding(3.dp)
                                    .background(
                                        if (darkTheme) NeuTheme.DarkBg else NeuTheme.LightBg,
                                        CircleShape
                                    )
                                    .padding(2.dp)
                            ) {
                                AsyncImage(
                                    model = status.userAvatar,
                                    contentDescription = status.userName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }

                            Column {
                                Text(
                                    text = status.userName,
                                    color = textColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Posted 2h ago • Views: ${status.viewsCount}",
                                    color = secondaryText,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }

    // Story viewer overlay (absolute auto-advance)
    if (selectedStatusForView != null) {
        StatusViewerOverlay(
            status = selectedStatusForView!!,
            onDismiss = { selectedStatusForView = null },
            darkTheme = darkTheme
        )
    }
}

// --- TAB 4: SETTINGS ---
@Composable
fun SettingsTab(
    viewModel: MainViewModel,
    darkTheme: Boolean,
    onClose: () -> Unit
) {
    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val deleteState by settingsViewModel.deleteState.collectAsState()

    LaunchedEffect(deleteState) {
        if (deleteState == DeleteAccountState.DELETED) {
            viewModel.logout()
        }
    }

    SettingsScreen(
        viewModel = settingsViewModel,
        darkTheme = darkTheme,
        onNavigateBack = onClose
    )
}

// --- CORE UTILITY OVERLAYS ---

@Composable
fun NeumorphicBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    darkTheme: Boolean
) {
    val accentColor = NeuTheme.AccentColor
    val activeColor = if (darkTheme) Color(0xFFC0A6FF) else accentColor
    val inactiveColor = if (darkTheme) Color(0xFF8E8A99) else Color(0xFF7A7585)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .neuShadow(cornerRadius = 24.dp, isSunken = false, darkTheme = darkTheme)
                .background(NeuTheme.getBgColor(darkTheme), RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Icons.Default.Chat to "Chats",
                Icons.Default.Call to "Calls",
                Icons.Default.StarHalf to "Status",
                Icons.Default.Settings to "Settings"
            )

            tabs.forEachIndexed { index, (icon, label) ->
                val isSelected = selectedTab == index
                
                // Scale animation for smooth tactile responses
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "tab_scale"
                )

                val tabModifier = if (isSelected) {
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .neuShadow(cornerRadius = 16.dp, isSunken = true, darkTheme = darkTheme)
                        .background(NeuTheme.getBgColor(darkTheme), RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) }
                        .padding(vertical = 8.dp)
                } else {
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) }
                        .padding(vertical = 8.dp)
                }

                Box(
                    modifier = tabModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) activeColor else inactiveColor,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(scale)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            color = if (isSelected) activeColor else inactiveColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewChatPicker(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onContactSelected: (ContactEntity) -> Unit,
    darkTheme: Boolean
) {
    val contacts by viewModel.contacts.collectAsState()
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuTheme.getBgColor(darkTheme),
        title = { Text(text = "New Chat", color = textColor, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxHeight(0.6f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(contacts) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onContactSelected(contact) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = contact.avatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = contact.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                if (contact.isFromContacts) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Text(
                                            text = "From your contacts",
                                            color = Color(0xFF2E7D32),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(text = "Zaxo Number: ${contact.zaxoNumber}", color = secondaryText, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NeuTheme.AccentColor)
            }
        }
    )
}

@Composable
fun NeumorphicDialpad(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onPlaceCall: (ContactEntity, Boolean) -> Unit,
    darkTheme: Boolean
) {
    val contacts by viewModel.contacts.collectAsState()
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)
    var dialedNumber by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuTheme.getBgColor(darkTheme),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Dial Zaxo Number", color = textColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .neuShadow(12.dp, isSunken = true, darkTheme = darkTheme),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dialedNumber.ifEmpty { "000-000-000" },
                        color = if (dialedNumber.isEmpty()) secondaryText.copy(alpha = 0.5f) else textColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dialpad 3x4 Matrix (Neumorphic 3D design)
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "◀")
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .neuShadow(cornerRadius = 27.dp, isSunken = false, darkTheme = darkTheme)
                                    .background(
                                        if (darkTheme) NeuTheme.DarkBg else NeuTheme.LightBg,
                                        CircleShape
                                    )
                                    .clickable {
                                        when (key) {
                                            "C" -> dialedNumber = ""
                                            "◀" -> if (dialedNumber.isNotEmpty()) dialedNumber = dialedNumber.dropLast(1)
                                            else -> {
                                                if (dialedNumber.length < 11) {
                                                    dialedNumber += key
                                                    // Auto format formatting
                                                    if (dialedNumber.length == 3 || dialedNumber.length == 7) {
                                                        dialedNumber += "-"
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    color = if (key == "C" || key == "◀") Color.Red else textColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = secondaryText)
                }

                Row {
                    // Audio call
                    IconButton(
                        onClick = {
                            // Find contact or use fallback Zaxo bot (F118)
                            val matchedContact = contacts.find { it.zaxoNumber == dialedNumber } 
                                ?: ContactEntity("bot_unknown", "Zaxo Peer", dialedNumber, "", isOnline = true)
                            onPlaceCall(matchedContact, false)
                        },
                        enabled = dialedNumber.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Audio", tint = NeuTheme.AccentColor)
                    }

                    // Video call
                    IconButton(
                        onClick = {
                            val matchedContact = contacts.find { it.zaxoNumber == dialedNumber } 
                                ?: ContactEntity("bot_unknown", "Zaxo Peer", dialedNumber, "", isOnline = true)
                            onPlaceCall(matchedContact, true)
                        },
                        enabled = dialedNumber.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video", tint = NeuTheme.AccentColor)
                    }
                }
            }
        }
    )
}

@Composable
fun NewStatusComposer(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    darkTheme: Boolean
) {
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)
    var statusText by remember { mutableStateOf("") }
    var selectedBgColor by remember { mutableStateOf(0) }

    val presets = listOf(
        Color(0xFFFF5252), Color(0xFF7C4DFF), Color(0xFF00E676), Color(0xFFFFAB40), Color(0xFF00B0FF), Color(0xFFE040FB)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuTheme.getBgColor(darkTheme),
        title = { Text(text = "Post Text Status", color = textColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(presets[selectedBgColor]),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = statusText,
                        onValueChange = { statusText = it },
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    if (statusText.isEmpty()) {
                        Text(
                            text = "What's on your mind?",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(text = "SELECT BACKGROUND PRESET", color = secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    presets.forEachIndexed { idx, col ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedBgColor = idx }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (statusText.isNotEmpty()) {
                        viewModel.postTextStatus(statusText, selectedBgColor, 0)
                        onDismiss()
                    }
                }
            ) {
                Text("POST", color = NeuTheme.AccentColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun StatusViewerOverlay(
    status: StatusEntity,
    onDismiss: () -> Unit,
    darkTheme: Boolean
) {
    val presets = listOf(
        Color(0xFFFF5252), Color(0xFF7C4DFF), Color(0xFF00E676), Color(0xFFFFAB40), Color(0xFF00B0FF), Color(0xFFE040FB)
    )

    // Absolute timer simulation for auto-advance (F18)
    LaunchedEffect(key1 = status.id) {
        delay(5000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        if (status.mediaType == "TEXT") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(presets[status.textBgColor.coerceIn(0..5)]),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = status.caption,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            AsyncImage(
                model = status.mediaUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            if (status.caption.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = status.caption, color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center)
                }
            }
        }

        // Header controls overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AsyncImage(
                    model = status.userAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
                Column {
                    Text(text = status.userName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Swipe to exit", color = Color.LightGray, fontSize = 11.sp)
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}
