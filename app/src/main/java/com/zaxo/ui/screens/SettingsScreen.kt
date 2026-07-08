package com.zaxo.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaxo.data.local.DeviceEntity
import com.zaxo.data.repository.SettingsRepository
import com.zaxo.ui.neumorphic.*
import com.zaxo.ui.viewmodel.*
import androidx.compose.ui.res.painterResource
import com.zaxo.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    darkTheme: Boolean,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val usernameCheck by viewModel.usernameCheck.collectAsState()
    val twoStepState by viewModel.twoStepState.collectAsState()
    val twoStepPin by viewModel.twoStepPin.collectAsState()
    val twoStepConfirmPin by viewModel.twoStepConfirmPin.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val activeDevices by viewModel.activeDevices.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Dialog state controllers
    var editProfileDialog by remember { mutableStateOf(false) }
    var editProfileName by remember { mutableStateOf(state.displayName) }
    var editProfileBio by remember { mutableStateOf(state.bio) }

    var showQRDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showTwoStepDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showDeviceDetailDialog by remember { mutableStateOf<DeviceEntity?>(null) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showFcmKeyDialog by remember { mutableStateOf(false) }

    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)
    val bgColor = NeuTheme.getBgColor(darkTheme)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // TOP NAVIGATION HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Zaxo Logo",
                        tint = NeuTheme.AccentColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Zaxo Settings",
                        color = textColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                NeumorphicIconButton(
                    onClick = { onNavigateBack() },
                    darkTheme = darkTheme,
                    cornerRadius = 12.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ==========================================
            // SECTION 1: PROFILE HEADER CARD (7 elements)
            // ==========================================
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth().testTag("profile_card"),
                cornerRadius = 24.dp,
                darkTheme = darkTheme
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // [1.1] AVATAR with modern dual-ring status border
                    Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .border(2.dp, NeuTheme.AccentColor.copy(alpha = 0.6f), CircleShape)
                                .padding(4.dp)
                                .background(if (darkTheme) Color(0xFF211E26) else Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = state.avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                            )
                        }
                        if (state.showOnlineStatus) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(if (darkTheme) Color(0xFF1E1B24) else Color.White, CircleShape)
                                    .padding(2.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                                    .align(Alignment.BottomEnd)
                            )
                        }
                    }

                    // [1.2] DISPLAY NAME & [1.3] @USERNAME (with Edit pencil indicator)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable {
                                editProfileName = state.displayName
                                editProfileBio = state.bio
                                editProfileDialog = true
                            }
                        ) {
                            Text(
                                text = state.displayName,
                                color = textColor,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = secondaryText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable {
                                showUsernameDialog = true
                            }
                        ) {
                            Text(
                                text = "@${state.username}",
                                color = NeuTheme.AccentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.AlternateEmail,
                                contentDescription = "Change Username",
                                tint = NeuTheme.AccentColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // [1.4] BIO with clean quote style container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(
                                color = if (darkTheme) Color(0xFF26202E) else Color(0xFFF9F5FC),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                editProfileName = state.displayName
                                editProfileBio = state.bio
                                editProfileDialog = true
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = state.bio.ifBlank { "Tap to add a bio..." },
                            color = secondaryText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // [1.5] ACCOUNT TYPE BADGE & [1.6] QR BUTTON & [1.7] ZAXO NUMBER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (state.accountType == "Personal") Color(0xFFE3F2FD) else Color(0xFFEDE7F6)
                        ) {
                            Text(
                                text = state.accountType,
                                color = if (state.accountType == "Personal") Color(0xFF1E88E5) else Color(0xFF5E35B1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = { showQRDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (darkTheme) Color(0xFF2C2535) else Color(0xFFF3EDF7), CircleShape)
                                .border(1.dp, NeuTheme.AccentColor.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCode,
                                contentDescription = "QR Code",
                                tint = NeuTheme.AccentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    if (darkTheme) Color(0xFF241F2B) else Color(0xFFEFE8F4),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(state.zaxoNumber))
                                    Toast.makeText(context, "Zaxo ID copied!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = state.zaxoNumber,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = secondaryText,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 2: ZAXO NUMBER SETTINGS (8 items)
            // ==========================================
            SettingsSection(title = "ZAXO NUMBER", darkTheme = darkTheme) {
                SettingsNavigationRow(
                    icon = Icons.Default.ContactPhone,
                    title = "My Registered Zaxo ID",
                    subtitle = state.zaxoNumber,
                    darkTheme = darkTheme,
                    onClick = {
                        clipboardManager.setText(AnnotatedString(state.zaxoNumber))
                        Toast.makeText(context, "Zaxo ID copied!", Toast.LENGTH_SHORT).show()
                    }
                )

                SettingsPickerRow(
                    icon = Icons.Default.Visibility,
                    title = "Who can discover me",
                    selected = state.zaxoVisibility,
                    options = listOf("Everyone", "My Contacts", "Nobody"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_ZAXO_VISIBILITY, it) }
                )

                 SettingsPickerRow(
                    icon = Icons.Default.PhoneCallback,
                    title = "Who can call me",
                    selected = state.p2pCallingPermission,
                    options = listOf("Everyone", "Contacts", "Nobody"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_P2P_CALLING, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.Badge,
                    title = "Transmit Caller ID",
                    subtitle = "Reveal your Zaxo Number in outgoing voice/video streams",
                    checked = state.callerIdEnabled,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_CALLER_ID, it) }
                )

                SettingsPickerRow(
                    icon = Icons.Default.VolumeUp,
                    title = "Ring Mode",
                    selected = state.ringMode,
                    options = listOf("Ring", "Vibrate", "Silent"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_RING_MODE, it) }
                )

                // Dependent Forwarding Block
                SettingsToggleRow(
                    icon = Icons.Default.ForwardToInbox,
                    title = "Zaxo Call Forwarding",
                    subtitle = "Forward unanswered voice calls to secondary line",
                    checked = state.callForwardingEnabled,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_CALL_FORWARDING, it) }
                )

                AnimatedVisibility(visible = state.callForwardingEnabled) {
                    OutlinedTextField(
                        value = state.callForwardingNumber,
                        onValueChange = { viewModel.saveSettingString(SettingsRepository.KEY_CALL_FORWARDING_NUMBER, it) },
                        label = { Text("Forwarding Destination Line") },
                        placeholder = { Text("e.g., +1 555-019-283") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeuTheme.AccentColor,
                            focusedLabelColor = NeuTheme.AccentColor
                        )
                    )
                }

                SettingsNavigationRow(
                    icon = Icons.Default.Block,
                    title = "Blocked Zaxo IDs",
                    subtitle = "Manage secure block filters and rules",
                    darkTheme = darkTheme,
                    onClick = { Toast.makeText(context, "Opening blocked directory...", Toast.LENGTH_SHORT).show() }
                )

                SettingsNavigationRow(
                    icon = Icons.Default.Call,
                    title = "Secure Call Registry",
                    subtitle = "View call logs",
                    darkTheme = darkTheme,
                    onClick = { Toast.makeText(context, "Opening call log registry...", Toast.LENGTH_SHORT).show() }
                )
            }

            // ==========================================
            // SECTION 3: ZAXO CALLING SETTINGS (8 items)
            // ==========================================
            SettingsSection(title = "ZAXO CALLING", darkTheme = darkTheme) {
                SettingsToggleRow(
                    icon = Icons.Default.Audiotrack,
                    title = "Voice Calling",
                    subtitle = "Enable high-fidelity voice transmission over secure Zaxo channels",
                    checked = state.p2pAudioEnabled,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_P2P_AUDIO_ENABLED, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.Videocam,
                    title = "Video Calling",
                    subtitle = "Allow incoming high-definition video feeds on secure Zaxo lines",
                    checked = state.p2pVideoEnabled,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_P2P_VIDEO_ENABLED, it) }
                )

                DependentSetting(enabled = state.p2pAudioEnabled) {
                    SettingsPickerRow(
                        icon = Icons.Default.SettingsVoice,
                        title = "Bitrate Quality Preset",
                        selected = state.callQuality,
                        options = listOf("Auto", "Low 64kbps", "Medium 128kbps", "High 256kbps"),
                        darkTheme = darkTheme,
                        onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_P2P_CALL_QUALITY, it) }
                    )
                }

                SettingsNavigationRow(
                    icon = Icons.Default.Lock,
                    title = "Private Calling",
                    subtitle = "All voice and video is fully private and secured locally.",
                    darkTheme = darkTheme,
                    onClick = {}
                )

                SettingsPickerRow(
                    icon = Icons.Default.NetworkCell,
                    title = "Network Usage",
                    selected = state.dataUsage,
                    options = listOf("WiFi Only", "WiFi + Cellular"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_P2P_DATA_USAGE, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.PhoneCallback,
                    title = "Auto-Answer Voice Streams",
                    subtitle = "Answer contacts instantly after 3s delay",
                    checked = state.autoAnswerEnabled,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_P2P_AUTO_ANSWER, it) }
                )

                DependentSetting(enabled = state.p2pAudioEnabled) {
                    SettingsToggleRow(
                        icon = Icons.Default.Hearing,
                        title = "Active Noise Cancellation",
                        subtitle = "Suppress ambient audio artifacts from feed",
                        checked = state.noiseCancellationEnabled,
                        darkTheme = darkTheme,
                        onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_P2P_NOISE_CANCELLATION, it) }
                    )
                }

                DependentSetting(enabled = state.p2pAudioEnabled) {
                    SettingsToggleRow(
                        icon = Icons.Default.RingVolume,
                        title = "Acoustic Echo Cancellation",
                        subtitle = "Prevent receiver voice loop back",
                        checked = state.echoCancellationEnabled,
                        darkTheme = darkTheme,
                        onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_P2P_ECHO_CANCELLATION, it) }
                    )
                }
            }

            // ==========================================
            // SECTION 4: ACCOUNT SETTINGS (12 items)
            // ==========================================
            SettingsSection(title = "ACCOUNT CONFIGURATION", darkTheme = darkTheme) {
                SettingsNavigationRow(
                    icon = Icons.Default.AlternateEmail,
                    title = "User ID Nickname",
                    subtitle = "@${state.username}",
                    darkTheme = darkTheme,
                    onClick = { showUsernameDialog = true }
                )

                SettingsNavigationRow(
                    icon = Icons.Default.Security,
                    title = "Two-Step Authentication PIN",
                    subtitle = if (state.twoStepEnabled) "Protected (Active)" else "Vulnerable (Inactive)",
                    darkTheme = darkTheme,
                    onClick = { showTwoStepDialog = true }
                )

                SettingsNavigationRow(
                    icon = Icons.Default.Phone,
                    title = "Port Registered Phone Number",
                    subtitle = "Verify authentication session logs",
                    darkTheme = darkTheme,
                    onClick = { Toast.makeText(context, "Verification port initiated...", Toast.LENGTH_SHORT).show() }
                )

                SettingsNavigationRow(
                    icon = Icons.Default.Mail,
                    title = "Recovery Email ID",
                    subtitle = "Register backup verification endpoints",
                    darkTheme = darkTheme,
                    onClick = { Toast.makeText(context, "Recovery sync triggered...", Toast.LENGTH_SHORT).show() }
                )

                SettingsNavigationRow(
                    icon = Icons.Default.VpnKey,
                    title = "Reset Master Key Password",
                    subtitle = "Update local encryption passphrases",
                    darkTheme = darkTheme,
                    onClick = { Toast.makeText(context, "Key reset page opening...", Toast.LENGTH_SHORT).show() }
                )

                SettingsToggleRow(
                    icon = Icons.Default.Link,
                    title = "Link Google Identity Account",
                    subtitle = "Single-Sign-On login syncing support",
                    checked = true,
                    darkTheme = darkTheme,
                    onCheckedChange = { Toast.makeText(context, "Link status locked.", Toast.LENGTH_SHORT).show() }
                )

                SettingsToggleRow(
                    icon = Icons.Default.Laptop,
                    title = "Link Apple iCloud Keychain",
                    subtitle = "iCloud SSO identity credentials binding",
                    checked = false,
                    darkTheme = darkTheme,
                    onCheckedChange = { Toast.makeText(context, "Apple integration is only available in iOS wrappers.", Toast.LENGTH_SHORT).show() }
                )

                SettingsPickerRow(
                    icon = Icons.Default.CardMembership,
                    title = "Profile Classification",
                    selected = state.accountType,
                    options = listOf("Personal", "Business"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_ACCOUNT_TYPE, it) }
                )

                SettingsNavigationRow(
                    icon = Icons.Default.Verified,
                    title = "Official Verified Badge",
                    subtitle = if (state.isVerified) "Verified Authority Token Active" else "Not Requested",
                    darkTheme = darkTheme,
                    onClick = {}
                )

                SettingsNavigationRow(
                    icon = Icons.Default.CalendarToday,
                    title = "Zaxo Creation Epoch",
                    subtitle = "Session commenced: Jun 12, 2025",
                    darkTheme = darkTheme,
                    onClick = {}
                )

                NeumorphicButton(
                    onClick = { Toast.makeText(context, "GDPR Export Requested. Check email soon.", Toast.LENGTH_LONG).show() },
                    darkTheme = darkTheme,
                    cornerRadius = 12.dp
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = NeuTheme.AccentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Request Account Audit Data Export", color = textColor, fontSize = 13.sp)
                }

                NeumorphicButton(
                    onClick = { Toast.makeText(context, "Data ZIP archive download started...", Toast.LENGTH_LONG).show() },
                    darkTheme = darkTheme,
                    cornerRadius = 12.dp
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = NeuTheme.AccentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Full Offline Data Archive", color = textColor, fontSize = 13.sp)
                }
            }

            // ==========================================
            // SECTION 5: PRIVACY SETTINGS (14 items)
            // ==========================================
            SettingsSection(title = "PRIVACY PRESETS", darkTheme = darkTheme) {
                SettingsPickerRow(
                    icon = Icons.Default.Schedule,
                    title = "Last Active Timestamp",
                    selected = state.privacyLastSeen,
                    options = listOf("Everyone", "My Contacts", "Nobody"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_PRIVACY_LAST_SEEN, it) }
                )

                SettingsPickerRow(
                    icon = Icons.Default.AccountBox,
                    title = "Profile Photo Visibility",
                    selected = state.privacyProfilePhoto,
                    options = listOf("Everyone", "My Contacts", "Nobody"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_PRIVACY_PROFILE_PHOTO, it) }
                )

                SettingsPickerRow(
                    icon = Icons.Default.Share,
                    title = "Status Ephemeral Stream",
                    selected = state.privacyStatus,
                    options = listOf("My Contacts", "My Contacts Except...", "Only Share With..."),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_PRIVACY_STATUS, it) }
                )

                SettingsPickerRow(
                    icon = Icons.Default.Search,
                    title = "Zaxo ID Discovery Link",
                    selected = state.zaxoVisibility,
                    options = listOf("Everyone", "My Contacts", "Nobody"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_ZAXO_VISIBILITY, it) }
                )

                SettingsPickerRow(
                    icon = Icons.Default.Call,
                    title = "Incoming Calls Filter",
                    selected = state.p2pCallingPermission,
                    options = listOf("Everyone", "Contacts", "Nobody"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_P2P_CALLING, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.ContactMail,
                    title = "Caller ID Mask",
                    checked = state.callerIdEnabled,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_CALLER_ID, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.DoneAll,
                    title = "Read Receipts Feedback",
                    subtitle = "Toggle blue confirmation status double ticks",
                    checked = state.privacyReadReceipts,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_PRIVACY_READ_RECEIPTS, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.Keyboard,
                    title = "Typing Status Feed",
                    subtitle = "Show typing indicator alerts dynamically",
                    checked = state.privacyTypingIndicator,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_PRIVACY_TYPING_INDICATOR, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.StayCurrentPortrait,
                    title = "Prevent Host Screenshots",
                    subtitle = "Block host capture devices (Sets FLAG_SECURE)",
                    checked = state.privacyScreenshotsEnabled,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_PRIVACY_SCREENSHOTS, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.Web,
                    title = "Web Link Preview Cards",
                    subtitle = "Allow fetching previews of URLs inside chats",
                    checked = state.privacyLinkPreviews,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_PRIVACY_LINK_PREVIEWS, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.Analytics,
                    title = "Anonymized Analytics Data",
                    subtitle = "Contribute telemetry packets anonymously",
                    checked = state.privacyDataCollection,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_PRIVACY_DATA_COLLECTION, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.LocationOn,
                    title = "Share Live Coarse Location",
                    subtitle = "Enable location packet updates during chats",
                    checked = state.privacyLocationSharing,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_PRIVACY_LOCATION_SHARING, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.Sync,
                    title = "Automatic Contact Book Synchronization",
                    checked = state.privacyContactSyncing,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_PRIVACY_CONTACT_SYNCING, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.Security,
                    title = "Encrypted Cloud Backups",
                    subtitle = "Backup chats using dynamic password keys",
                    checked = state.privacyEncryptedBackup,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_PRIVACY_ENCRYPTED_BACKUP, it) }
                )
            }

            // ==========================================
            // SECTION 6: NOTIFICATIONS (9 items)
            // ==========================================
            SettingsSection(title = "NOTIFICATIONS ENGINE", darkTheme = darkTheme) {
                SettingsToggleRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "Master Push Delivery",
                    subtitle = "Enable low-power FCM push notifications",
                    checked = state.notificationsPushEnabled,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_PUSH, it) }
                )

                DependentSetting(enabled = state.notificationsPushEnabled) {
                    SettingsToggleRow(
                        icon = Icons.Default.MusicNote,
                        title = "Message Tone Sounds",
                        checked = state.notificationsSound,
                        darkTheme = darkTheme,
                        onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_SOUND, it) }
                    )
                }

                DependentSetting(enabled = state.notificationsPushEnabled) {
                    SettingsToggleRow(
                        icon = Icons.Default.Group,
                        title = "Group Notification Alerts",
                        checked = state.notificationsGroup,
                        darkTheme = darkTheme,
                        onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_GROUP, it) }
                    )
                }

                DependentSetting(enabled = state.notificationsPushEnabled) {
                    SettingsToggleRow(
                        icon = Icons.Default.Call,
                        title = "Incoming Calls Fullscreen Overlays",
                        checked = state.notificationsCall,
                        darkTheme = darkTheme,
                        onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_CALL, it) }
                    )
                }

                DependentSetting(enabled = state.notificationsPushEnabled) {
                    SettingsToggleRow(
                        icon = Icons.Default.ChatBubble,
                        title = "Active In-App Overlay HeadsUp",
                        checked = state.notificationsInApp,
                        darkTheme = darkTheme,
                        onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_IN_APP, it) }
                    )
                }

                SettingsToggleRow(
                    icon = Icons.Default.Email,
                    title = "Offline Email Daily Digest",
                    checked = state.notificationsEmail,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_EMAIL, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.MarkEmailRead,
                    title = "Promotional Marketing Digests",
                    checked = state.notificationsMarketing,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_MARKETING, it) }
                )

                DependentSetting(enabled = state.notificationsPushEnabled) {
                    SettingsPickerRow(
                        icon = Icons.Default.Visibility,
                        title = "Notification Privacy Banner",
                        selected = state.notificationsPreview,
                        options = listOf("Always", "When Unlocked", "Never"),
                        darkTheme = darkTheme,
                        onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_NOTIFICATIONS_PREVIEW, it) }
                    )
                }

                DependentSetting(enabled = state.notificationsPushEnabled) {
                    SettingsNavigationRow(
                        icon = Icons.Default.VpnKey,
                        title = "FCM Channel Pairing Key",
                        subtitle = "Configure FCM credentials & routing",
                        darkTheme = darkTheme,
                        onClick = { showFcmKeyDialog = true }
                    )
                }

                NeumorphicButton(
                    onClick = {
                        viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_PUSH, true)
                        viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_SOUND, true)
                        viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_GROUP, true)
                        viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_CALL, true)
                        viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_IN_APP, true)
                        viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_EMAIL, false)
                        viewModel.saveSettingBoolean(SettingsRepository.KEY_NOTIFICATIONS_MARKETING, false)
                        viewModel.saveSettingString(SettingsRepository.KEY_NOTIFICATIONS_PREVIEW, "Always")
                        Toast.makeText(context, "Notification configurations reset!", Toast.LENGTH_SHORT).show()
                    },
                    darkTheme = darkTheme,
                    cornerRadius = 12.dp
                ) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Notifications Channels", color = Color.Red, fontSize = 13.sp)
                }
            }

            // ==========================================
            // SECTION 7: APPEARANCE (7 items)
            // ==========================================
            SettingsSection(title = "AESTHETICS", darkTheme = darkTheme) {
                SettingsPickerRow(
                    icon = Icons.Default.Brightness6,
                    title = "Dark Theme Delegation",
                    selected = state.darkMode,
                    options = listOf("System", "Light", "Dark"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_APPEARANCE_DARK_MODE, it) }
                )

                SettingsNavigationRow(
                    icon = Icons.Default.Wallpaper,
                    title = "Wallpaper Style Picker",
                    subtitle = "Configure background backdrop layout",
                    darkTheme = darkTheme,
                    onClick = { Toast.makeText(context, "Opening wallpaper picker...", Toast.LENGTH_SHORT).show() }
                )

                SettingsPickerRow(
                    icon = Icons.Default.FormatSize,
                    title = "Layout Body Font Size",
                    selected = state.fontSize,
                    options = listOf("Small", "Medium", "Large"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_APPEARANCE_FONT_SIZE, it) }
                )

                SettingsPickerRow(
                    icon = Icons.Default.Message,
                    title = "Chat Bubble Corner Style",
                    selected = state.bubbleStyle,
                    options = listOf("Rounded", "Sharp", "Modern"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_APPEARANCE_BUBBLE_STYLE, it) }
                )

                SettingsPickerRow(
                    icon = Icons.Default.Palette,
                    title = "Primary Theme Accents",
                    selected = state.colorTheme,
                    options = listOf("Blue", "Green", "Purple", "Orange", "Pink"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_APPEARANCE_COLOR_THEME, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.FiberManualRecord,
                    title = "Status Circle Indicator",
                    subtitle = "Display green active indicator on profile card avatar",
                    checked = state.showOnlineStatus,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_APPEARANCE_ONLINE_STATUS, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.ViewList,
                    title = "Compact Density Mode",
                    subtitle = "Minimize padding spacing across active chat cards",
                    checked = state.compactMode,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_APPEARANCE_COMPACT_MODE, it) }
                )
            }

            // ==========================================
            // SECTION 8: STORAGE & DATA (7 items)
            // ==========================================
            SettingsSection(title = "STORAGE MANAGEMENT", darkTheme = darkTheme) {
                // [8.1] PROGRESS BAR
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Zaxo Cloud Host Usage", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${String.format("%.1f", state.totalStorageUsedGb)} GB of ${String.format("%.1f", state.totalStorageAvailableGb)} GB used",
                            color = secondaryText,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { state.totalStorageUsedGb / state.totalStorageAvailableGb },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NeuTheme.AccentColor,
                        trackColor = if (darkTheme) Color(0xFF232128) else Color(0xFFE4DBE6)
                    )
                }

                // [8.2] CHIPS ROW
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StorageChip(label = "Photos: ${String.format("%.1f", state.photosSizeMb)} MB", color = Color.Blue)
                    StorageChip(label = "Videos: ${String.format("%.1f", state.videosSizeMb)} MB", color = Color.Magenta)
                    StorageChip(label = "Statuses: ${String.format("%.1f", state.statusesSizeMb)} MB", color = Color.Green)
                    StorageChip(label = "Docs: ${String.format("%.1f", state.documentsSizeMb)} MB", color = Color.Yellow)
                    StorageChip(label = "Other: ${String.format("%.1f", state.otherSizeMb)} MB", color = Color.Gray)
                }

                SettingsPickerRow(
                    icon = Icons.Default.FileDownload,
                    title = "Media Download Rules",
                    selected = state.autoDownloadMedia,
                    options = listOf("Wi-Fi", "Mobile Data", "Never"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_STORAGE_AUTO_DOWNLOAD, it) }
                )

                // [8.4] CACHE ROW
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Cache Directory", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${String.format("%.2f", state.cacheSizeMb)} MB consumed", color = secondaryText, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { showClearCacheConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Clear", color = Color.White, fontSize = 12.sp)
                    }
                }

                SettingsPickerRow(
                    icon = Icons.Default.HourglassEmpty,
                    title = "Retain History Range",
                    selected = state.keepMessagesDuration,
                    options = listOf("1 Month", "6 Months", "1 Year", "Forever"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_STORAGE_KEEP_MESSAGES, it) }
                )

                SettingsNavigationRow(
                    icon = Icons.Default.InsertChart,
                    title = "Network Diagnostic Statistics",
                    subtitle = "Verify protocol bytes transmitted and received",
                    darkTheme = darkTheme,
                    onClick = {}
                )

                SettingsToggleRow(
                    icon = Icons.Default.PlayCircleOutline,
                    title = "Auto-Play inline GIFs",
                    checked = state.autoPlayGifs,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_STORAGE_AUTO_PLAY, it) }
                )
            }

            // ==========================================
            // SECTION 9: CHATS (4 items)
            // ==========================================
            SettingsSection(title = "CHATS MODULE", darkTheme = darkTheme) {
                SettingsPickerRow(
                    icon = Icons.Default.Timer,
                    title = "Default Disappearing Timer",
                    selected = state.defaultMessageTimer,
                    options = listOf("Off", "24 Hours", "7 Days", "30 Days"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_CHATS_DEFAULT_TIMER, it) }
                )

                SettingsPickerRow(
                    icon = Icons.Default.Backup,
                    title = "Backup Scheduled Frequency",
                    selected = state.chatBackupFrequency,
                    options = listOf("Daily", "Weekly", "Monthly", "Off"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_CHATS_BACKUP_FREQUENCY, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.KeyboardReturn,
                    title = "Enter Key Dispatcher",
                    subtitle = "Execute return key to instantly send chat frames",
                    checked = state.enterKeySends,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_CHATS_ENTER_KEY_SENDS, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.Face,
                    title = "Automatic AI Sticker Picker",
                    subtitle = "Suggest stickers contextually based on keyboard logs",
                    checked = state.stickerSuggestions,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_CHATS_STICKER_SUGGESTIONS, it) }
                )
            }

            // ==========================================
            // SECTION 10: AI & ASSISTANT (4 items)
            // ==========================================
            SettingsSection(title = "AI & EMBEDDED CO-PILOTS", darkTheme = darkTheme) {
                SettingsToggleRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Context Smart Replies",
                    subtitle = "Suggest instantaneous contextual answer tokens",
                    checked = state.aiSmartReplies,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_AI_SMART_REPLIES, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.Mic,
                    title = "Voice Speech Recognition",
                    subtitle = "Allow trigger commands to invoke actions",
                    checked = state.aiVoiceAssistant,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_AI_VOICE_ASSISTANT, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.Summarize,
                    title = "Dynamic Message Syntheses",
                    subtitle = "Condense long chat threads into short visual reports",
                    checked = state.aiMessageSummaries,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_AI_MESSAGE_SUMMARIES, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.QueryStats,
                    title = "Vector Smart Search Engine",
                    subtitle = "Perform natural semantic lookups on history",
                    checked = state.aiSmartSearch,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_AI_SMART_SEARCH, it) }
                )
            }

            // ==========================================
            // SECTION 11: LANGUAGE (2 items)
            // ==========================================
            SettingsSection(title = "REGIONAL DELEGATES", darkTheme = darkTheme) {
                SettingsPickerRow(
                    icon = Icons.Default.Language,
                    title = "Global Application Language",
                    selected = state.appLanguage,
                    options = listOf("English", "Spanish", "French", "German", "Arabic", "Hindi"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_APP_LANGUAGE, it) }
                )

                SettingsPickerRow(
                    icon = Icons.Default.Input,
                    title = "Preferred Input Layout",
                    selected = state.preferredKeyboardLanguage,
                    options = listOf("English", "Spanish", "French", "German", "Arabic", "Hindi"),
                    darkTheme = darkTheme,
                    onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_KEYBOARD_LANGUAGE, it) }
                )
            }

            // ==========================================
            // SECTION 12: SECURITY (4 items)
            // ==========================================
            SettingsSection(title = "SECURITY POLICIES", darkTheme = darkTheme) {
                SettingsNavigationRow(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Lock",
                    subtitle = if (state.biometricLockEnabled) "Enforced" else "Disabled",
                    darkTheme = darkTheme,
                    onClick = { viewModel.toggleBiometricLock(!state.biometricLockEnabled) }
                )

                AnimatedVisibility(visible = state.biometricLockEnabled) {
                    SettingsPickerRow(
                        icon = Icons.Default.AccessTime,
                        title = "App Re-Lock Delay Interval",
                        selected = state.appLockTimeout,
                        options = listOf("1 min", "5 min", "15 min", "1 hour"),
                        darkTheme = darkTheme,
                        onSelected = { viewModel.saveSettingString(SettingsRepository.KEY_LOCK_TIMEOUT, it) }
                    )
                }

                SettingsNavigationRow(
                    icon = Icons.Default.Devices,
                    title = "Active Login Sessions Directory",
                    subtitle = "Inspect and revoke login active keys",
                    darkTheme = darkTheme,
                    onClick = { viewModel.setSubScreen("active_sessions") }
                )

                SettingsNavigationRow(
                    icon = Icons.Default.VpnLock,
                    title = "Manage 2FA Protection",
                    subtitle = if (state.twoStepEnabled) "Active" else "Vulnerable",
                    darkTheme = darkTheme,
                    onClick = { showTwoStepDialog = true }
                )
            }

            // ==========================================
            // SECTION 13: ACCESSIBILITY (4 items)
            // ==========================================
            SettingsSection(title = "ACCESSIBILITY HELPER ENGINE", darkTheme = darkTheme) {
                SettingsToggleRow(
                    icon = Icons.Default.Hearing,
                    title = "TalkBack Mode Optimization",
                    checked = state.talkbackOptimization,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_ACCESSIBILITY_TALKBACK, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.DirectionsRun,
                    title = "Reduce Layout Transitions Speed",
                    checked = state.reduceMotion,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_ACCESSIBILITY_REDUCE_MOTION, it) }
                )

                SettingsToggleRow(
                    icon = Icons.Default.SettingsAccessibility,
                    title = "WCAG High Contrast Layouts",
                    checked = state.highContrastMode,
                    darkTheme = darkTheme,
                    onCheckedChange = { viewModel.saveSettingBoolean(SettingsRepository.KEY_ACCESSIBILITY_HIGH_CONTRAST, it) }
                )

                // Font scale slider
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Dynamic Text Scaler Mode", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = state.fontScale,
                        onValueChange = { viewModel.saveSettingFloat(SettingsRepository.KEY_ACCESSIBILITY_FONT_SCALE, it) },
                        valueRange = 0.8f..1.5f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            activeTrackColor = NeuTheme.AccentColor,
                            thumbColor = NeuTheme.AccentColor
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0.8x (Compact)", color = secondaryText, fontSize = 11.sp)
                        Text("1.5x (Enlarged Limit)", color = secondaryText, fontSize = 11.sp)
                    }
                }
            }

            // ==========================================
            // SECTION 14: PERMISSIONS (11 items)
            // ==========================================
            SettingsSection(title = "HARDWARE PERMISSION GRANTS", darkTheme = darkTheme) {
                PermissionPillRow(name = "Camera Hardware Line", status = state.permissionCamera, darkTheme = darkTheme)
                PermissionPillRow(name = "Microphone Line", status = state.permissionMic, darkTheme = darkTheme)
                PermissionPillRow(name = "Photo Album Access", status = state.permissionPhotos, darkTheme = darkTheme)
                PermissionPillRow(name = "Contact Book Integration", status = state.permissionContacts, darkTheme = darkTheme)
                PermissionPillRow(name = "Geographic Location Coordinates", status = state.permissionLocation, darkTheme = darkTheme)
                PermissionPillRow(name = "Push Notifications Overlay", status = state.permissionNotifications, darkTheme = darkTheme)
                PermissionPillRow(name = "Background Task Refresh Rate", status = state.permissionBackgroundRefresh, darkTheme = darkTheme)
                PermissionPillRow(name = "Zaxo Smart Assistant Trigger", status = state.permissionAssistant, darkTheme = darkTheme)
                PermissionPillRow(name = "Biometric Passcode Integration", status = state.permissionBiometric, darkTheme = darkTheme)
                PermissionPillRow(name = "Cellular Data Transmission", status = state.permissionCellularData, darkTheme = darkTheme)

                NeumorphicButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    darkTheme = darkTheme,
                    cornerRadius = 12.dp
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = NeuTheme.AccentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Hardware Permissions in Android Settings", color = textColor, fontSize = 13.sp)
                }
            }

            // ==========================================
            // SECTION 15: ABOUT & HELP (9 items)
            // ==========================================
            SettingsSection(title = "ZAXO DECORATIVE INDEX", darkTheme = darkTheme) {
                SettingsNavigationRow(icon = Icons.Default.Info, title = "App Core Version", subtitle = "v6.0.0 (Build 12)", darkTheme = darkTheme, onClick = {})
                SettingsNavigationRow(icon = Icons.Default.DocumentScanner, title = "Terms of Service Agreement", subtitle = "Review legal parameters", darkTheme = darkTheme, onClick = {})
                SettingsNavigationRow(icon = Icons.Default.VerifiedUser, title = "Data Privacy Policies", subtitle = "Secure communications parameters", darkTheme = darkTheme, onClick = {})
                SettingsNavigationRow(icon = Icons.Default.Book, title = "Third-Party Oss Licenses List", subtitle = "Read compiled components index", darkTheme = darkTheme, onClick = {})
                SettingsNavigationRow(icon = Icons.Default.Help, title = "Self Help FAQ Hub", subtitle = "Resolve general configuration issues", darkTheme = darkTheme, onClick = {})
                SettingsNavigationRow(icon = Icons.Default.Email, title = "Reach Support Desk", subtitle = "support@zaxo.eu.cc", darkTheme = darkTheme, onClick = {})
                SettingsNavigationRow(icon = Icons.Default.BugReport, title = "Submit Host Bug Logs", subtitle = "Help us fix secure channel bugs", darkTheme = darkTheme, onClick = {})
                SettingsNavigationRow(icon = Icons.Default.Lightbulb, title = "Request Feature Proposals", subtitle = "Suggest capabilities", darkTheme = darkTheme, onClick = {})
                SettingsNavigationRow(icon = Icons.Default.Star, title = "Rate App on Store Directory", subtitle = "Share your thoughts", darkTheme = darkTheme, onClick = {})
            }

            // ==========================================
            // SECTION 16: BOTTOM ACTIONS (5 items)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeumorphicButton(
                    onClick = { viewModel.logout() },
                    darkTheme = darkTheme,
                    cornerRadius = 16.dp,
                    testTag = "logout_btn_neu"
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log out", color = Color.Red, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Your end-to-end local databases will be cleared and credentials erased.",
                    color = secondaryText,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                NeumorphicButton(
                    onClick = {
                        viewModel.saveSettingBoolean(SettingsRepository.KEY_TWO_STEP_PIN_HASH, false)
                        Toast.makeText(context, "Sessions revoked except current device.", Toast.LENGTH_LONG).show()
                    },
                    darkTheme = darkTheme,
                    cornerRadius = 16.dp
                ) {
                    Icon(imageVector = Icons.Default.DevicesOther, contentDescription = null, tint = textColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Revoke other sessions", color = textColor, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showDeleteAccountDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete account", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Warning: This operates a critical destruction cascade on your cloud and local keys.",
                    color = Color.Red,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Subscreens (Overlay sheets or screens based on state)
        if (state.currentSubScreen == "active_sessions") {
            ActiveSessionsOverlay(
                viewModel = ActiveSessionsViewModel(LocalContext.current.applicationContext as android.app.Application),
                darkTheme = darkTheme,
                onBack = { viewModel.setSubScreen("main") }
            )
        }

        // Dialog overlays
        if (editProfileDialog) {
            AlertDialog(
                onDismissRequest = { editProfileDialog = false },
                title = { Text("Edit Profile Info", color = textColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editProfileName,
                            onValueChange = { editProfileName = it },
                            label = { Text("Name") }
                        )
                        OutlinedTextField(
                            value = editProfileBio,
                            onValueChange = { editProfileBio = it },
                            label = { Text("Bio") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateProfile(editProfileName, editProfileBio)
                        editProfileDialog = false
                    }) {
                        Text("Save", color = NeuTheme.AccentColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editProfileDialog = false }) {
                        Text("Cancel", color = textColor)
                    }
                }
            )
        }

        if (showQRDialog) {
            AlertDialog(
                onDismissRequest = { showQRDialog = false },
                title = { Text("Zaxo Contact QR Card", color = textColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ZaxoQRCode(
                                identifier = state.zaxoNumber.ifEmpty { "000-000-000" },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Text("Scan this token to secure communications with ${state.displayName}", color = secondaryText, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQRDialog = false }) {
                        Text("Done", color = NeuTheme.AccentColor)
                    }
                }
            )
        }

        if (showUsernameDialog) {
            AlertDialog(
                onDismissRequest = { showUsernameDialog = false },
                title = { Text("Edit Handle Name", color = textColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.username,
                            onValueChange = { viewModel.onUsernameChanged(it) },
                            label = { Text("Handle Nickname") }
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (usernameCheck) {
                                UsernameCheckState.CHECKING -> {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Validating availability...", color = secondaryText, fontSize = 11.sp)
                                }
                                UsernameCheckState.AVAILABLE -> {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Username available for assignment!", color = Color.Green, fontSize = 11.sp)
                                }
                                UsernameCheckState.TAKEN -> {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Username already taken! Try another.", color = Color.Red, fontSize = 11.sp)
                                }
                                else -> {}
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = usernameCheck == UsernameCheckState.AVAILABLE,
                        onClick = {
                            viewModel.saveUsername()
                            showUsernameDialog = false
                        }
                    ) {
                        Text("Apply", color = if (usernameCheck == UsernameCheckState.AVAILABLE) NeuTheme.AccentColor else Color.Gray)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUsernameDialog = false }) {
                        Text("Cancel", color = textColor)
                    }
                }
            )
        }

        if (showTwoStepDialog) {
            AlertDialog(
                onDismissRequest = { showTwoStepDialog = false },
                title = { Text("Two-Step PIN Lock Setup", color = textColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = when (twoStepState) {
                                TwoStepState.SETUP_ENTER_PIN -> "Step 1: Enter a new 4-digit master passcode."
                                TwoStepState.SETUP_CONFIRM_PIN -> "Step 2: Re-enter passcode to confirm parameters."
                                TwoStepState.ENABLED -> "Security PIN currently Active."
                                TwoStepState.DISABLE_ENTER_PIN -> "Verification: Enter passcode to de-register."
                                else -> "Configure secure Two-Step PIN lock."
                            },
                            color = textColor,
                            fontSize = 13.sp
                        )

                        // Render Dot Indicators for 4 digits
                        val digitsEntered = if (twoStepState == TwoStepState.SETUP_CONFIRM_PIN) twoStepConfirmPin.length else twoStepPin.length
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            repeat(4) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(
                                            if (idx < digitsEntered) NeuTheme.AccentColor else Color.Gray.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                )
                            }
                        }

                        // Number Pad Controls
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("Clear", "0", "")
                            ).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    row.forEach { char ->
                                        if (char.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .background(
                                                        if (darkTheme) Color(0xFF2E2A36) else Color(0xFFF3EDF7),
                                                        CircleShape
                                                    )
                                                    .clickable {
                                                        if (char == "Clear") viewModel.clearLastSetupPinDigit()
                                                        else viewModel.appendSetupPinDigit(char)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(char, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(54.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (twoStepState == TwoStepState.ENABLED) {
                        TextButton(onClick = { viewModel.startTwoStepDisable() }) {
                            Text("Disable PIN Lock", color = Color.Red)
                        }
                    } else if (twoStepState == TwoStepState.DISABLED) {
                        TextButton(onClick = { viewModel.startTwoStepSetup() }) {
                            Text("Setup Lock", color = NeuTheme.AccentColor)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTwoStepDialog = false }) {
                        Text("Close", color = textColor)
                    }
                }
            )
        }

        if (showDeleteAccountDialog) {
            var inputDeleteConfirm by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                title = { Text("Confirm Account Destruction", color = Color.Red, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Are you absolutely sure you want to proceed? This cannot be undone.", color = textColor, fontSize = 13.sp)
                        Text("To confirm execution, type DELETE in the box below:", color = secondaryText, fontSize = 11.sp)
                        OutlinedTextField(
                            value = inputDeleteConfirm,
                            onValueChange = { inputDeleteConfirm = it },
                            label = { Text("Confirmation Key") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = inputDeleteConfirm == "DELETE",
                        onClick = {
                            viewModel.deleteAccountPermanently()
                            showDeleteAccountDialog = false
                        }
                    ) {
                        Text("DESTRUCT", color = if (inputDeleteConfirm == "DELETE") Color.Red else Color.Gray)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }) {
                        Text("Cancel", color = textColor)
                    }
                }
            )
        }

        if (showClearCacheConfirm) {
            AlertDialog(
                onDismissRequest = { showClearCacheConfirm = false },
                title = { Text("Empty Cache Registry?", color = textColor, fontWeight = FontWeight.Bold) },
                text = { Text("Confirm clearing local database caches. Your logged chats will remain untouched.", color = textColor, fontSize = 13.sp) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearAppCache()
                        showClearCacheConfirm = false
                    }) {
                        Text("Empty Cache", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheConfirm = false }) {
                        Text("Cancel", color = textColor)
                    }
                }
            )
        }

        if (showFcmKeyDialog) {
            var inputFcmKey by remember { mutableStateOf(state.fcmPairKey) }
            AlertDialog(
                onDismissRequest = { showFcmKeyDialog = false },
                title = { Text("FCM Channel Pairing", color = textColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Configure the Voluntary Application Server Identification (VAPID) Pair Key below to secure push notification delivery from your decentralized chat hub.",
                            color = secondaryText,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = inputFcmKey,
                            onValueChange = { inputFcmKey = it },
                            label = { Text("FCM Pair Key") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                clipboardManager.setText(AnnotatedString(state.fcmPairKey))
                                Toast.makeText(context, "Pairing Key copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = NeuTheme.AccentColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Key", color = NeuTheme.AccentColor, fontSize = 12.sp)
                            }
                            TextButton(onClick = {
                                inputFcmKey = ""
                            }) {
                                Text("Clear", color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.saveSettingString(SettingsRepository.KEY_FCM_PAIR_KEY, inputFcmKey)
                        showFcmKeyDialog = false
                        Toast.makeText(context, "FCM Pairing Channel secured successfully!", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Save Pairing", color = NeuTheme.AccentColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFcmKeyDialog = false }) {
                        Text("Close", color = textColor)
                    }
                }
            )
        }
    }
}

// ==========================================
// REUSABLE SUB-COMPONENTS
// ==========================================

@Composable
fun SettingsSection(
    title: String,
    darkTheme: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = NeuTheme.AccentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 8.dp)
        )

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            darkTheme = darkTheme
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    darkTheme: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = NeuTheme.AccentColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeuTheme.AccentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(text = title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(text = subtitle, color = secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
                }
            }
        }
        NeumorphicSwitch(checked = checked, onCheckedChange = onCheckedChange, darkTheme = darkTheme)
    }
}

@Composable
fun SettingsPickerRow(
    icon: ImageVector,
    title: String,
    selected: String,
    options: List<String>,
    darkTheme: Boolean,
    onSelected: (String) -> Unit
) {
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = NeuTheme.AccentColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeuTheme.AccentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(text = title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .background(
                    color = if (darkTheme) Color(0xFF2B2633) else Color(0xFFF3EDF7),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = selected, color = NeuTheme.AccentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = NeuTheme.AccentColor, modifier = Modifier.size(16.dp))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(NeuTheme.getBgColor(darkTheme))
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, color = textColor) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = NeuTheme.AccentColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeuTheme.AccentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(text = title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = secondaryText, fontSize = 11.sp)
            }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = secondaryText, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun DependentSetting(
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = false) {}
    ) {
        content()
    }
}

@Composable
fun PermissionPillRow(
    name: String,
    status: String,
    darkTheme: Boolean
) {
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (status == "Granted") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        ) {
            Text(
                text = status,
                color = if (status == "Granted") Color(0xFF2E7D32) else Color(0xFFC62828),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun StorageChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
