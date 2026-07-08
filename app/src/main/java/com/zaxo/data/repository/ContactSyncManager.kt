package com.zaxo.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.zaxo.data.local.ContactEntity
import com.zaxo.data.local.ZaxoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ContactSyncManager(private val context: Context) {
    private val db = ZaxoDatabase.getDatabase(context)
    private val contactDao = db.contactDao()

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: Exception("Firestore query task failure"))
            }
        }
    }

    suspend fun syncContacts(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val phoneToNameMap = mutableMapOf<String, String>()
                
                try {
                    val cursor = resolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        ),
                        null, null, null
                    )
                    
                    cursor?.use { c ->
                        val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        while (c.moveToNext()) {
                            val name = if (nameIdx >= 0) c.getString(nameIdx) else "Unknown"
                            val number = if (numIdx >= 0) c.getString(numIdx) else ""
                            if (number.isNotBlank()) {
                                val normalized = number.replace(Regex("[^0-9+]"), "")
                                val e164 = if (normalized.startsWith("+")) normalized else if (normalized.length == 10) "+1$normalized" else "+$normalized"
                                phoneToNameMap[e164] = name
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    android.util.Log.e("ContactSyncManager", "Contacts permission not granted", e)
                }

                if (phoneToNameMap.isEmpty()) {
                    return@withContext Result.success(0)
                }

                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val matchedContacts = mutableListOf<ContactEntity>()

                val phoneList = phoneToNameMap.keys.toList()
                val chunks = phoneList.chunked(10)
                for (chunk in chunks) {
                    try {
                        val querySnapshot = firestore.collection("users")
                           .whereIn("phone", chunk)
                           .get()
                           .awaitTask()
                        
                        for (doc in querySnapshot.documents) {
                            val uid = doc.id
                            val name = doc.getString("name") ?: phoneToNameMap[doc.getString("phone")] ?: "Zaxo User"
                            val zaxoNumber = doc.getString("zaxoNumber") ?: "555-019-283"
                            val avatar = doc.getString("avatar") ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
                            
                            matchedContacts.add(ContactEntity(
                                id = uid,
                                name = name,
                                zaxoNumber = zaxoNumber,
                                avatar = avatar,
                                isFromContacts = true,
                                isOnline = true,
                                lastActive = System.currentTimeMillis()
                            ))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ContactSyncManager", "Error querying chunk from Firestore", e)
                    }
                }

                if (matchedContacts.isNotEmpty()) {
                    contactDao.insertContacts(matchedContacts)
                }
                Result.success(matchedContacts.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun disableContactSyncing() {
        withContext(Dispatchers.IO) {
            contactDao.deleteContact("sarah_smith")
            contactDao.deleteContact("mike_johnson")
        }
    }
}
