package com.zaxo.data.call

import com.zaxo.data.local.CallRecordEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CallWaitingManager {
    private val _activeCall = MutableStateFlow<CallRecordEntity?>(null)
    val activeCall: StateFlow<CallRecordEntity?> = _activeCall.asStateFlow()

    private val _heldCall = MutableStateFlow<CallRecordEntity?>(null)
    val heldCall: StateFlow<CallRecordEntity?> = _heldCall.asStateFlow()

    val activeTimer = CallTimer()
    val heldTimer = CallTimer()

    fun onIncomingCallWhileActive(newCall: CallRecordEntity, callWaitingEnabled: Boolean): Boolean {
        if (!callWaitingEnabled) {
            return false // Send busy signal
        }
        _heldCall.value = newCall
        heldTimer.start()
        heldTimer.hold() // Put waiting call on hold initially
        return true
    }

    fun acceptNewCall() {
        val current = _activeCall.value
        val waiting = _heldCall.value ?: return

        // Current call goes to held
        _heldCall.value = current
        activeTimer.hold()
        
        // Swap timers
        val tempElapsed = activeTimer.getElapsedSeconds()
        // Promote waiting to active
        _activeCall.value = waiting
        activeTimer.start()
        // We can just keep timers linked to their respective slots
    }

    fun endCurrentAcceptNew() {
        // active call ends, waiting call becomes active
        val waiting = _heldCall.value ?: return
        _activeCall.value = waiting
        _heldCall.value = null
        activeTimer.start()
    }

    fun declineNewCall() {
        _heldCall.value = null
    }

    fun swapCalls() {
        val active = _activeCall.value ?: return
        val held = _heldCall.value ?: return

        _activeCall.value = held
        _heldCall.value = active

        // Swap timers
        activeTimer.resume()
        heldTimer.hold()
    }

    fun endCall(record: CallRecordEntity) {
        if (_activeCall.value?.id == record.id) {
            _activeCall.value = _heldCall.value
            _heldCall.value = null
            if (_activeCall.value != null) {
                activeTimer.resume()
            } else {
                activeTimer.hold()
            }
        } else if (_heldCall.value?.id == record.id) {
            _heldCall.value = null
            heldTimer.hold()
        }
    }

    fun setInitialActiveCall(record: CallRecordEntity) {
        _activeCall.value = record
        activeTimer.start()
    }

    fun clear() {
        _activeCall.value = null
        _heldCall.value = null
        activeTimer.hold()
        heldTimer.hold()
    }
}
