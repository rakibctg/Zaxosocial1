package com.zaxo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaxo.ui.viewmodel.CallState
import com.zaxo.ui.viewmodel.CallViewModel
import com.zaxo.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun CallOverlayScreen(
    callViewModel: CallViewModel,
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val callState by callViewModel.callState.collectAsState()
    val activeCallRecord by callViewModel.activeCallRecord.collectAsState()
    val toastMessage by callViewModel.toastMessage.collectAsState()

    val record = activeCallRecord

    // Auto dismiss check when state returns to IDLE
    LaunchedEffect(callState) {
        if (callState == CallState.IDLE) {
            onNavigateBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF141218))
    ) {
        if (record != null) {
            when (callState) {
                CallState.VALIDATING,
                CallState.CREATING_ROOM,
                CallState.SENDING_PUSH,
                CallState.DIALING,
                CallState.RINGING -> {
                    OutgoingCallScreen(viewModel = callViewModel, record = record)
                }
                CallState.INCOMING -> {
                    IncomingCallScreen(viewModel = callViewModel, record = record)
                }
                CallState.CONNECTING,
                CallState.ACTIVE,
                CallState.HELD,
                CallState.RECONNECTING,
                CallState.CALL_WAITING -> {
                    ActiveCallScreen(viewModel = callViewModel, record = record)
                }
                CallState.GROUP_CREATING,
                CallState.GROUP_RINGING,
                CallState.GROUP_ACTIVE,
                CallState.GROUP_PARTICIPANT_JOINED,
                CallState.GROUP_PARTICIPANT_LEFT -> {
                    GroupCallScreen(viewModel = callViewModel, groupId = record.id, isVideo = record.isVideo)
                }
                CallState.POST_CALL -> {
                    PostCallScreen(
                        viewModel = callViewModel,
                        record = record,
                        onNavigateToChat = { peerId ->
                            mainViewModel.selectChat(peerId)
                            callViewModel.dismissPostCall()
                        },
                        onDismiss = {
                            callViewModel.dismissPostCall()
                        }
                    )
                }
                // Terminal states with simple informative text
                CallState.CALL_FAILED,
                CallState.CALL_DECLINED,
                CallState.CALL_CANCELLED,
                CallState.PRIVACY_BLOCKED,
                CallState.LINE_BUSY,
                CallState.NO_ANSWER,
                CallState.USER_OFFLINE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1A1D23)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = when (callState) {
                                    CallState.CALL_FAILED -> "Call Failed"
                                    CallState.CALL_DECLINED -> "Call Declined"
                                    CallState.CALL_CANCELLED -> "Call Cancelled"
                                    CallState.PRIVACY_BLOCKED -> "Blocked by Privacy Gate"
                                    CallState.LINE_BUSY -> "Line Busy"
                                    CallState.NO_ANSWER -> "No Answer"
                                    CallState.USER_OFFLINE -> "User Offline"
                                    else -> "Call Terminated"
                                },
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Returning to main screen...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                    LaunchedEffect(Unit) {
                        delay(2000)
                        callViewModel.dismissPostCall()
                    }
                }
                else -> {
                    // Default fallback
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Initializing...", color = Color.White)
                    }
                }
            }
        } else {
            // Null record fallback
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No active call session found.", color = Color.White)
            }
            LaunchedEffect(Unit) {
                delay(1500)
                onNavigateBack()
            }
        }

        // Animated overlay toast banner
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        ) {
            toastMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(text = msg, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
