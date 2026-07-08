package com.zaxo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zaxo.data.local.DeviceEntity
import com.zaxo.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ActiveSessionsState(
    val sessions: List<DeviceEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ActiveSessionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    
    private val _state = MutableStateFlow(ActiveSessionsState())
    val state: StateFlow<ActiveSessionsState> = _state.asStateFlow()

    init {
        observeSessions()
    }

    private fun observeSessions() {
        _state.value = _state.value.copy(isLoading = true)
        repository.getActiveDevices()
            .onEach { list ->
                _state.value = _state.value.copy(
                    sessions = list.sortedByDescending { it.lastActive },
                    isLoading = false
                )
            }
            .catch { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to retrieve active sessions"
                )
            }
            .launchIn(viewModelScope)
    }

    fun revokeSession(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeDevice(deviceId)
            // Simulating FCM remote logout push delivery and token revocation
        }
    }

    fun revokeAllOtherSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearOtherDevices()
            // Simulating broadcast/FCM revocation on all secondary device instances
        }
    }
    
    fun refreshSessions() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            kotlinx.coroutines.delay(1000) // Simulating network refresh query
            _state.value = _state.value.copy(isLoading = false)
        }
    }
}
