package com.zaxo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaxo.ui.screens.AuthScreen
import com.zaxo.ui.screens.CallOverlayScreen
import com.zaxo.ui.screens.ChatDetailScreen
import com.zaxo.ui.screens.MainScreen
import com.zaxo.ui.theme.MyApplicationTheme
import com.zaxo.ui.viewmodel.CallState
import com.zaxo.ui.viewmodel.MainViewModel
import com.zaxo.ui.viewmodel.CallViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val callViewModel: CallViewModel = viewModel()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val activeChatId by viewModel.activeChatId.collectAsState()
            val callState by callViewModel.callState.collectAsState()
            val appDarkThemeState by viewModel.darkTheme.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = appDarkThemeState ?: systemDark

            MyApplicationTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        !isLoggedIn -> {
                            AuthScreen(viewModel = viewModel)
                        }
                        callState != CallState.IDLE -> {
                            CallOverlayScreen(
                                callViewModel = callViewModel,
                                mainViewModel = viewModel,
                                onNavigateBack = { /* Handled dynamically by ViewModel CallState resetting to IDLE */ }
                            )
                        }
                        activeChatId != null -> {
                            ChatDetailScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.selectChat(null) }
                            )
                        }
                        else -> {
                            MainScreen(
                                viewModel = viewModel,
                                onNavigateToChat = { chatId -> viewModel.selectChat(chatId) },
                                onNavigateToCall = { /* Handled dynamically by PlaceCall transitions */ }
                            )
                        }
                    }
                }
            }
        }
    }
}
