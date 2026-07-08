package com.zaxo.data.call

import android.content.Context
import com.zaxo.data.local.CallRecordEntity
import com.zaxo.data.repository.ZaxoRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

import kotlinx.coroutines.flow.first

class CallHistoryRepository(context: Context) {
    private val repository = ZaxoRepository(context)

    fun getCallHistory(): Flow<List<CallRecordEntity>> {
        return repository.getCallHistory()
    }

    suspend fun saveCallRecord(
        callerId: String,
        callerName: String,
        calleeId: String,
        calleeName: String,
        callerAvatar: String,
        calleeAvatar: String,
        isVideo: Boolean,
        isGroup: Boolean,
        durationSeconds: Long,
        status: String
    ): CallRecordEntity {
        val record = CallRecordEntity(
            id = UUID.randomUUID().toString(),
            callerId = callerId,
            callerName = callerName,
            calleeId = calleeId,
            calleeName = calleeName,
            callerAvatar = callerAvatar,
            calleeAvatar = calleeAvatar,
            isVideo = isVideo,
            isGroup = isGroup,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            status = status
        )
        repository.addCallRecord(record)
        // Simulated Firestore remote write dual sync
        syncToRemoteFirestore(record)
        return record
    }

    private suspend fun syncToRemoteFirestore(record: CallRecordEntity) {
        // Log remote sync
        android.util.Log.d("CallHistoryRepository", "Call history synchronized to Firestore: ${record.id}")
    }

    suspend fun getRecentCallsFromUser(userId: String, timeframeMs: Long): List<CallRecordEntity> {
        val allRecords = repository.getCallHistory().first()
        val cutoff = System.currentTimeMillis() - timeframeMs
        return allRecords.filter {
            (it.callerId == userId || it.calleeId == userId) && it.timestamp >= cutoff
        }
    }

    suspend fun clearHistory() {
        repository.clearCallHistory()
    }
}
