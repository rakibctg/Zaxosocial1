package com.zaxo.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.zaxo.data.local.SettingEntity
import com.zaxo.data.local.ZaxoDatabase
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.concurrent.ConcurrentHashMap

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: Exception("Task failure"))
        }
    }
}

class SettingsSyncManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("zaxo_settings_prefs", Context.MODE_PRIVATE)
    private val db = ZaxoDatabase.getDatabase(context)
    private val settingDao = db.settingDao()
    
    private val pendingWrites = ConcurrentHashMap<String, Any>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    private val debounceMs = 2000L  // 2 seconds

    fun onSettingChanged(key: String, value: Any) {
        Log.d("SettingsSyncManager", "Setting changed: $key -> $value")
        
        // Step 1: Write to SharedPreferences immediately (synchronous, < 1ms)
        prefs.edit().apply {
            when (value) {
                is String -> putString(key, value)
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Float -> putFloat(key, value)
                else -> putString(key, value.toString())
            }
        }.apply()

        // Step 2: Queue for Cloud/Database sync (debounced)
        pendingWrites[key] = value
        scheduleSync()
    }

    private fun scheduleSync() {
        syncJob?.cancel()  // Cancel previous debounce timer
        syncJob = coroutineScope.launch {
            delay(debounceMs)  // Wait 2 seconds
            flushWrites()      // Send batch
        }
    }

    private suspend fun flushWrites() {
        if (pendingWrites.isEmpty()) return
        val batch = HashMap(pendingWrites)
        pendingWrites.clear()

        try {
            // Write to Room Database (local offline store)
            batch.forEach { (key, value) ->
                settingDao.insertSetting(SettingEntity(key, value.toString()))
            }
            Log.d("SettingsSyncManager", "Successfully flushed batch of ${batch.size} settings to local DB")
            
            // Sync to the remote user's cloud profile document users/{uid}/settings/preferences
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val settingsRef = firestore.collection("users")
                    .document(uid)
                    .collection("settings")
                    .document("preferences")
                
                val updateData = HashMap<String, Any>()
                batch.forEach { (key, value) ->
                    updateData[key] = value
                }
                settingsRef.set(updateData, com.google.firebase.firestore.SetOptions.merge()).awaitTask()
                Log.d("SettingsSyncManager", "Successfully synced settings to Firestore at users/$uid/settings/preferences")
            }
            
        } catch (e: Exception) {
            Log.e("SettingsSyncManager", "Failed to sync settings, re-queuing...", e)
            // Re-queue failed writes for next flush
            batch.forEach { (key, value) -> pendingWrites[key] = value }
        }
    }
    
    fun getLocalString(key: String, defaultValue: String): String {
        return prefs.getString(key, null) ?: defaultValue
    }
    
    fun getLocalBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }
    
    fun getLocalInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }
    
    fun getLocalFloat(key: String, defaultValue: Float): Float {
        return prefs.getFloat(key, defaultValue)
    }
}
