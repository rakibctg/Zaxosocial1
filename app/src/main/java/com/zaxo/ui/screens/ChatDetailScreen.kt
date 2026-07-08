package com.zaxo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaxo.data.local.ChatEntity
import com.zaxo.data.local.MessageEntity
import com.zaxo.ui.neumorphic.*
import com.zaxo.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

sealed class ChatScreenItem {
    data class DateHeader(val dateString: String) : ChatScreenItem()
    data class MessageItem(val message: MessageEntity) : ChatScreenItem()
}

@Composable
fun ChatDetailScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val bgColor = NeuTheme.getBgColor(darkTheme)
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)

    val activeChatId by viewModel.activeChatId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val chats by viewModel.activeChats.collectAsState()

    val currentChat = chats.find { it.id == activeChatId } ?: ChatEntity(
        id = activeChatId ?: "",
        name = "Secure Chat",
        avatar = ""
    )

    var textInput by remember { mutableStateOf("") }
    var replyingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var selectedMediaViewerMsg by remember { mutableStateOf<MessageEntity?>(null) }

    val chatWallpaperMode by viewModel.chatWallpaperMode.collectAsState()

    val wallpaperModifier = when (chatWallpaperMode) {
        "custom" -> Modifier.background(if (darkTheme) Color(0xFF13111C) else Color(0xFFF3E9F8))
        else -> Modifier.background(bgColor)
    }

    val screenItems = remember(messages) {
        val list = mutableListOf<ChatScreenItem>()
        var lastDateStr = ""
        for (msg in messages) {
            val dateStr = formatChatDateHeader(msg.timestamp)
            if (dateStr != lastDateStr) {
                list.add(ChatScreenItem.DateHeader(dateStr))
                lastDateStr = dateStr
            }
            list.add(ChatScreenItem.MessageItem(msg))
        }
        list
    }

    val listState = rememberLazyListState()

    LaunchedEffect(screenItems.size) {
        if (screenItems.isNotEmpty()) {
            listState.animateScrollToItem(screenItems.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ChatHeader(
                chat = currentChat,
                onBack = {
                    viewModel.selectChat(null)
                    onNavigateBack()
                },
                onWallpaperClick = { showWallpaperDialog = true },
                darkTheme = darkTheme
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(wallpaperModifier)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        PrivateChatBanner(darkTheme)
                    }

                    items(screenItems) { item ->
                        when (item) {
                            is ChatScreenItem.DateHeader -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.dateString,
                                        color = secondaryText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(
                                                if (darkTheme) Color(0xFF221E2E) else Color(0xFFEADBFC),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            is ChatScreenItem.MessageItem -> {
                                MessageBubble(
                                    message = item.message,
                                    onReplyClick = { replyingMessage = item.message },
                                    onStarClick = {
                                        scope.launch {
                                            viewModel.repository.toggleMessageStarred(item.message.id)
                                        }
                                    },
                                    onEditClick = {
                                        editingMessage = item.message
                                        textInput = item.message.decryptedText
                                    },
                                    onDeleteClick = {
                                        scope.launch {
                                            viewModel.repository.deleteMessage(item.message.id)
                                        }
                                    },
                                    onMediaClick = { clickedMsg ->
                                        selectedMediaViewerMsg = clickedMsg
                                    },
                                    darkTheme = darkTheme
                                )
                            }
                        }
                    }
                }

                val typingMap by viewModel.typingIndicators.collectAsState()
                val peerTypingText = typingMap[currentChat.id]
                if (peerTypingText != null) {
                    Text(
                        text = "Peer is typing...",
                        color = Color(0xFF27AE60),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)
                    )
                }

                if (replyingMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (darkTheme) Color(0xFF1B1D26) else Color(0xFFEADBFC))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to ${replyingMessage!!.senderName}",
                                    color = NeuTheme.AccentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatLastMessage(replyingMessage!!.decryptedText),
                                    color = textColor,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { replyingMessage = null }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = secondaryText)
                            }
                        }
                    }
                }

                InputActionBar(
                    value = textInput,
                    onValueChange = { textInput = it },
                    isRecording = isRecordingAudio,
                    onRecordToggle = { isRecordingAudio = !isRecordingAudio },
                    onSend = {
                        if (editingMessage != null) {
                            scope.launch {
                                viewModel.repository.editMessage(editingMessage!!.id, textInput)
                            }
                            editingMessage = null
                        } else {
                            viewModel.sendMessage(
                                text = textInput,
                                replyToId = replyingMessage?.id,
                                replyToText = replyingMessage?.decryptedText
                            )
                            replyingMessage = null
                        }
                        textInput = ""
                    },
                    onAttachmentSelected = { mediaType, mediaUrl, textDesc ->
                        viewModel.sendMessage(
                            text = textDesc,
                            mediaUrl = mediaUrl,
                            mediaType = mediaType,
                            replyToId = replyingMessage?.id,
                            replyToText = replyingMessage?.decryptedText
                        )
                        replyingMessage = null
                    },
                    darkTheme = darkTheme
                )
            }

            val showScrollToBottom by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 5
                }
            }

            AnimatedVisibility(
                visible = showScrollToBottom,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
            ) {
                NeumorphicIconButton(
                    onClick = {
                        scope.launch {
                            if (screenItems.isNotEmpty()) {
                                listState.animateScrollToItem(screenItems.size - 1)
                            }
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    cornerRadius = 22.dp,
                    darkTheme = darkTheme
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Scroll to bottom",
                        tint = NeuTheme.AccentColor
                    )
                }
            }

            if (showWallpaperDialog) {
                AlertDialog(
                    onDismissRequest = { showWallpaperDialog = false },
                    containerColor = bgColor,
                    title = { Text("Chat Wallpaper Background", color = textColor, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Adjust thread appearance:", color = secondaryText, fontSize = 13.sp)

                            NeumorphicButton(
                                onClick = {
                                    viewModel.saveSetting("chat_wallpaper_mode", "standard")
                                    showWallpaperDialog = false
                                },
                                darkTheme = darkTheme
                            ) {
                                Text("Standard Neumorphism", color = textColor, fontWeight = FontWeight.SemiBold)
                            }

                            NeumorphicButton(
                                onClick = {
                                    viewModel.saveSetting("chat_wallpaper_mode", "custom")
                                    showWallpaperDialog = false
                                },
                                darkTheme = darkTheme
                            ) {
                                Text("High Contrast Premium", color = textColor, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showWallpaperDialog = false }) {
                            Text("Close", color = NeuTheme.AccentColor)
                        }
                    }
                )
            }

            if (selectedMediaViewerMsg != null) {
                MediaViewerDialog(
                    message = selectedMediaViewerMsg!!,
                    onDismiss = { selectedMediaViewerMsg = null },
                    darkTheme = darkTheme
                )
            }
        }
    }
}

@Composable
fun ChatHeader(
    chat: ChatEntity,
    onBack: () -> Unit,
    onWallpaperClick: () -> Unit,
    darkTheme: Boolean
) {
    val bgColor = NeuTheme.getBgColor(darkTheme)
    val textColor = NeuTheme.getTextColor(darkTheme)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .neuShadow(cornerRadius = 20.dp, isSunken = false, darkTheme = darkTheme)
                .background(bgColor, RoundedCornerShape(20.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
            }

            Box(modifier = Modifier.size(40.dp)) {
                if (chat.avatar.isNotBlank()) {
                    AsyncImage(
                        model = chat.avatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NeuTheme.AccentLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.name.take(1).uppercase(),
                            color = NeuTheme.AccentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.name,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Private",
                    color = NeuTheme.AccentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onWallpaperClick) {
                Icon(imageVector = Icons.Default.Palette, contentDescription = "Wallpaper", tint = NeuTheme.AccentColor)
            }
        }
    }
}

@Composable
fun PrivateChatBanner(darkTheme: Boolean) {
    NeumorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        cornerRadius = 14.dp,
        darkTheme = darkTheme
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = NeuTheme.AccentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Messages are private. No server, including Zaxo, can access them.",
                color = NeuTheme.getSecondaryTextColor(darkTheme),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    onReplyClick: () -> Unit,
    onStarClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMediaClick: (MessageEntity) -> Unit,
    darkTheme: Boolean
) {
    val isMe = message.senderId == "me"
    val alignment = if (isMe) Alignment.End else Alignment.Start

    var showActionsMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (message.isStarred) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Starred",
                tint = Color(0xFFFFD700),
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 6.dp)
            )
        }

        // Custom asymmetrical rounded corners
        val bubbleShape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isMe) 16.dp else 4.dp,
            bottomEnd = if (isMe) 4.dp else 16.dp
        )

        // Custom premium bubble colors
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .neuShadow(cornerRadius = 16.dp, isSunken = false, darkTheme = darkTheme)
                .background(
                    color = if (isMe) NeuTheme.AccentColor else (if (darkTheme) NeuTheme.DarkBg else NeuTheme.LightBg),
                    shape = bubbleShape
                )
                .clickable { showActionsMenu = !showActionsMenu }
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (message.replyToText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isMe) Color.White.copy(alpha = 0.2f) else (if (darkTheme) Color(0xFF1A1721) else Color(0xFFF3EDF7)),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(6.dp)
                    ) {
                        Text(
                            text = message.replyToText,
                            color = if (isMe) Color.White.copy(alpha = 0.8f) else NeuTheme.getSecondaryTextColor(darkTheme),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                MessageBubbleContent(
                    message = message,
                    isMe = isMe,
                    onMediaClick = onMediaClick,
                    darkTheme = darkTheme
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isEdited) {
                        Text(
                            text = "Edited • ",
                            color = if (isMe) Color.White.copy(alpha = 0.6f) else NeuTheme.getSecondaryTextColor(darkTheme),
                            fontSize = 9.sp
                        )
                    }

                    val sdf = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
                    Text(
                        text = sdf.format(java.util.Date(message.timestamp)),
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else NeuTheme.getSecondaryTextColor(darkTheme),
                        fontSize = 9.sp
                    )

                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = when (message.status) {
                                "SENDING" -> Icons.Default.AccessTime
                                "SENT" -> Icons.Default.Check
                                "DELIVERED" -> Icons.Default.CheckCircle
                                else -> Icons.Default.DoneAll
                            },
                            contentDescription = null,
                            tint = if (message.status == "READ") Color.White else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }

        if (showActionsMenu) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .neuShadow(cornerRadius = 8.dp, isSunken = false, darkTheme = darkTheme)
                    .background(
                        if (darkTheme) NeuTheme.DarkBg else NeuTheme.LightBg,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = {
                    onReplyClick()
                    showActionsMenu = false
                }) {
                    Icon(imageVector = Icons.Default.Reply, contentDescription = "Reply", tint = NeuTheme.AccentColor, modifier = Modifier.size(18.dp))
                }
                if (isMe) {
                    IconButton(onClick = {
                        onEditClick()
                        showActionsMenu = false
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = NeuTheme.AccentColor, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = {
                    onStarClick()
                    showActionsMenu = false
                }) {
                    Icon(imageVector = Icons.Default.StarBorder, contentDescription = "Star", tint = NeuTheme.AccentColor, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = {
                    onDeleteClick()
                    showActionsMenu = false
                }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun MessageBubbleContent(
    message: MessageEntity,
    isMe: Boolean,
    onMediaClick: (MessageEntity) -> Unit,
    darkTheme: Boolean
) {
    val textColor = if (isMe) Color.White else NeuTheme.getTextColor(darkTheme)
    val secondaryColor = if (isMe) Color.White.copy(alpha = 0.7f) else NeuTheme.getSecondaryTextColor(darkTheme)

    when {
        message.decryptedText == "This message was deleted" || message.mediaType == "DELETED" -> {
            Text(
                text = "Message deleted",
                color = secondaryColor.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }

        message.mediaType == "PHOTO" -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onMediaClick(message) }
                ) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Photo Message",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (message.decryptedText.isNotEmpty() && !message.decryptedText.contains("PHOTO_MESSAGE") && !message.decryptedText.contains("photo_message")) {
                    Text(text = message.decryptedText, color = textColor, fontSize = 14.sp)
                }
            }
        }

        message.mediaType == "VIDEO" -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onMediaClick(message) },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Video Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                if (message.decryptedText.isNotEmpty() && !message.decryptedText.contains("VIDEO_MESSAGE") && !message.decryptedText.contains("video_message")) {
                    Text(text = message.decryptedText, color = textColor, fontSize = 14.sp)
                }
            }
        }

        message.mediaType == "VOICE" -> {
            VoiceNotePlayer(message = message, isMe = isMe, darkTheme = darkTheme)
        }

        message.mediaType == "DOCUMENT" -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isMe) Color.White.copy(alpha = 0.15f) else (if (darkTheme) Color(0xFF1E1C28) else Color(0xFFF3EDF7)),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp)
                    .clickable { onMediaClick(message) }
            ) {
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = if (isMe) Color.White else NeuTheme.AccentColor,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.decryptedText.ifEmpty { "Attachment_Document.pdf" },
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "2.4 MB • PDF Document",
                        color = secondaryColor,
                        fontSize = 10.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download",
                    tint = if (isMe) Color.White else NeuTheme.AccentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        message.mediaType == "STICKER" -> {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = message.mediaUrl,
                    contentDescription = "Sticker",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        message.mediaType == "LOCATION" -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isMe) Color.White.copy(alpha = 0.15f) else (if (darkTheme) Color(0xFF1E1C28) else Color(0xFFF3EDF7)))
                    .clickable { onMediaClick(message) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1524661135-423995f22d0b?w=400",
                        contentDescription = "Map location",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location",
                        tint = Color.Red,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text(
                    text = message.decryptedText.ifEmpty { "Mountain View, CA" },
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        else -> {
            Text(
                text = message.decryptedText,
                color = textColor,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun VoiceNotePlayer(message: MessageEntity, isMe: Boolean, darkTheme: Boolean) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (progress < 1.0f) {
                kotlinx.coroutines.delay(100)
                progress += 0.02f
            }
            isPlaying = false
            progress = 0f
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isMe) Color.White else NeuTheme.AccentColor
            )
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
        ) {
            val hValues = listOf(6, 12, 18, 10, 22, 14, 8, 16, 24, 12, 10, 18, 20, 14, 8, 18, 12, 6, 14, 10)
            val spacing = size.width / hValues.size
            hValues.forEachIndexed { index, h ->
                val x = index * spacing + spacing / 2
                val barProgress = index.toFloat() / hValues.size
                val isHighlighted = barProgress <= progress
                drawLine(
                    color = if (isHighlighted) (if (isMe) Color.White else NeuTheme.AccentColor) else (if (isMe) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.5f)),
                    start = Offset(x, size.height / 2 - h),
                    end = Offset(x, size.height / 2 + h),
                    strokeWidth = 4f
                )
            }
        }

        Text(
            text = if (isPlaying) String.format("0:%02d", (progress * 15).toInt()) else "0:15",
            color = if (isMe) Color.White.copy(alpha = 0.8f) else NeuTheme.getSecondaryTextColor(darkTheme),
            fontSize = 11.sp
        )
    }
}

@Composable
fun InputActionBar(
    value: String,
    onValueChange: (String) -> Unit,
    isRecording: Boolean,
    onRecordToggle: () -> Unit,
    onSend: () -> Unit,
    onAttachmentSelected: (mediaType: String, mediaUrl: String, text: String) -> Unit,
    darkTheme: Boolean
) {
    val bgColor = NeuTheme.getBgColor(darkTheme)
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryColor = NeuTheme.getSecondaryTextColor(darkTheme)

    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(8.dp)
    ) {
        if (showEmojiPicker) {
            EmojiPickerPanel(
                onEmojiClicked = { emoji ->
                    onValueChange(value + emoji)
                    showEmojiPicker = false
                },
                darkTheme = darkTheme
            )
        }

        if (showAttachmentMenu) {
            AttachmentGridView(
                onSelected = { mediaType, mediaUrl, desc ->
                    onAttachmentSelected(mediaType, mediaUrl, desc)
                    showAttachmentMenu = false
                },
                darkTheme = darkTheme
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeumorphicIconButton(
                onClick = {
                    showEmojiPicker = !showEmojiPicker
                    showAttachmentMenu = false
                },
                modifier = Modifier.size(40.dp).padding(bottom = 2.dp),
                cornerRadius = 20.dp,
                darkTheme = darkTheme
            ) {
                Icon(
                    imageVector = if (showEmojiPicker) Icons.Default.Keyboard else Icons.Default.SentimentSatisfiedAlt,
                    contentDescription = "Emoji",
                    tint = NeuTheme.AccentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            NeumorphicIconButton(
                onClick = {
                    showAttachmentMenu = !showAttachmentMenu
                    showEmojiPicker = false
                },
                modifier = Modifier.size(40.dp).padding(bottom = 2.dp),
                cornerRadius = 20.dp,
                darkTheme = darkTheme
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attachments",
                    tint = NeuTheme.AccentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp, max = 120.dp)
                    .neuShadow(cornerRadius = 20.dp, isSunken = true, darkTheme = darkTheme)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isRecording) {
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                        ) {
                            val count = 20
                            val spacing = size.width / count
                            repeat(count) { i ->
                                val h = (4..18).random().toFloat()
                                drawLine(
                                    color = Color.Red,
                                    start = Offset(i * spacing, size.height / 2 - h),
                                    end = Offset(i * spacing, size.height / 2 + h),
                                    strokeWidth = 3f
                                )
                            }
                        }
                        Text(
                            text = "Recording... (Auto-stop: 10m)",
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    } else {
                        if (value.isEmpty()) {
                            Text(
                                text = "Message...",
                                color = secondaryColor.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }

                        androidx.compose.foundation.text.BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            maxLines = 5,
                            textStyle = LocalTextStyle.current.copy(color = textColor, fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            NeumorphicIconButton(
                onClick = {
                    if (isRecording) {
                        onRecordToggle()
                        onAttachmentSelected("VOICE", "https://zaxo.app/assets/voice_recording.wav", "Voice message")
                    } else {
                        if (value.isNotEmpty()) {
                            onSend()
                        } else {
                            onRecordToggle()
                        }
                    }
                },
                modifier = Modifier.size(40.dp).padding(bottom = 2.dp),
                cornerRadius = 20.dp,
                darkTheme = darkTheme
            ) {
                Icon(
                    imageVector = if (value.isNotEmpty()) Icons.Default.Send else (if (isRecording) Icons.Default.Stop else Icons.Default.Mic),
                    contentDescription = "Send",
                    tint = if (isRecording) Color.Red else NeuTheme.AccentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmojiPickerPanel(onEmojiClicked: (String) -> Unit, darkTheme: Boolean) {
    val emojis = listOf(
        "😊", "😂", "🥰", "👍", "🔥", "❤️", "🙌", "🎉",
        "💡", "🚀", "🤫", "🔐", "💬", "👀", "🎙️", "📷",
        "🤯", "👏", "😭", "🙏", "🤩", "✨", "💯", "👋"
    )
    NeumorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        cornerRadius = 14.dp,
        darkTheme = darkTheme
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "QUICK EMOJIS",
                color = NeuTheme.getSecondaryTextColor(darkTheme),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(8),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(100.dp)
            ) {
                items(emojis.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onEmojiClicked(emojis[index]) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emojis[index], fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentGridView(
    onSelected: (mediaType: String, mediaUrl: String, text: String) -> Unit,
    darkTheme: Boolean
) {
    val options = listOf(
        AttachmentOption("Camera", Icons.Default.PhotoCamera, "PHOTO", "https://images.unsplash.com/photo-1542038784456-1ea8e935640e?w=500", "Captured Photo"),
        AttachmentOption("Gallery", Icons.Default.Photo, "PHOTO", "https://images.unsplash.com/photo-1472214222541-d510753a4707?w=500", "Image from Gallery"),
        AttachmentOption("Document", Icons.Default.InsertDriveFile, "DOCUMENT", "", "Project_Spec_V2.pdf"),
        AttachmentOption("Location", Icons.Default.Place, "LOCATION", "", "1600 Amphitheatre Pkwy, Mountain View, CA"),
        AttachmentOption("Sticker", Icons.Default.SentimentSatisfiedAlt, "STICKER", "https://images.unsplash.com/photo-1531804055935-76f44d7c3621?w=200", "Zaxo Sticker")
    )

    NeumorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        cornerRadius = 14.dp,
        darkTheme = darkTheme
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "PRIVATE ATTACHMENTS",
                color = NeuTheme.getSecondaryTextColor(darkTheme),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                options.forEach { option ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onSelected(option.mediaType, option.mediaUrl, option.desc) }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(if (darkTheme) Color(0xFF2C2A3A) else Color(0xFFEADBFC), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.name,
                                tint = NeuTheme.AccentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = option.name,
                            color = NeuTheme.getTextColor(darkTheme),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

data class AttachmentOption(
    val name: String,
    val icon: ImageVector,
    val mediaType: String,
    val mediaUrl: String,
    val desc: String
)

@Composable
fun MediaViewerDialog(
    message: MessageEntity,
    onDismiss: () -> Unit,
    darkTheme: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
        title = {},
        text = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Text(
                        text = if (message.mediaType == "PHOTO") "Photo Viewer" else if (message.mediaType == "VIDEO") "Video Player" else "Location Pin",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                    }
                }

                when (message.mediaType) {
                    "PHOTO" -> {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = "Fullscreen Photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        )
                    }
                    "VIDEO" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16/9f)
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleFilled,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                            Text("Secure Video Stream", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                    "LOCATION" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "GPS",
                                tint = Color.Red,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(message.decryptedText, color = Color.White, textAlign = TextAlign.Center)
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = NeuTheme.AccentColor)
                            ) {
                                Text("Navigate using Google Maps")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

fun formatChatDateHeader(timestamp: Long): String {
    val calendar = java.util.Calendar.getInstance()
    val today = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    val todayYear = calendar.get(java.util.Calendar.YEAR)

    val msgCalendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val msgDay = msgCalendar.get(java.util.Calendar.DAY_OF_YEAR)
    val msgYear = msgCalendar.get(java.util.Calendar.YEAR)

    return when {
        msgYear == todayYear && msgDay == today -> "Today"
        msgYear == todayYear && today - msgDay == 1 -> "Yesterday"
        msgYear == todayYear && today - msgDay < 7 && today - msgDay > 0 -> {
            val dayFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
            dayFormat.format(java.util.Date(timestamp))
        }
        else -> {
            val dateFormat = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault())
            dateFormat.format(java.util.Date(timestamp))
        }
    }
}
