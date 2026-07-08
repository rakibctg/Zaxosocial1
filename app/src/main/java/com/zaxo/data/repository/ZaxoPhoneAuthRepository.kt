package com.zaxo.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.pnv.FirebasePhoneNumberVerification
import com.google.firebase.pnv.VerifiedPhoneNumberTokenResult
import com.google.firebase.pnv.VerificationSupportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Repository managing modern carrier-based Firebase Phone Number Verification (PNV).
 * This SDK performs carrier-based, single-tap, SIM-retrieval verification.
 */
class ZaxoPhoneAuthRepository(private val context: Context) {

    private val pnv = FirebasePhoneNumberVerification.getInstance(context)
    private val firebaseAuth = FirebaseAuth.getInstance()

    /**
     * Checks if the current device, cellular network, and carrier support the modern PNV SDK.
     * If false, callers must fall back to standard SMS OTP.
     */
    suspend fun checkPnvCompatibility(): Boolean = withContext(Dispatchers.IO) {
        try {
            val results: List<VerificationSupportResult> = pnv.getVerificationSupportInfo().await()
            // Check if any available SIM slots/carriers support PNV
            results.any { it.isSupported() }
        } catch (e: Exception) {
            Log.e("ZaxoPhoneAuthRepository", "PNV Compatibility check failed", e)
            false
        }
    }

    /**
     * Initiates the carrier PNV flow which prompts the native carrier consent sheet.
     * Returns the full verified result containing token details upon success.
     */
    suspend fun startCarrierVerification(): VerifiedPhoneNumberTokenResult = withContext(Dispatchers.IO) {
        try {
            pnv.getVerifiedPhoneNumber(context).await()
        } catch (e: Exception) {
            Log.e("ZaxoPhoneAuthRepository", "Carrier verification prompt failed", e)
            throw e
        }
    }

    /**
     * Exchanges the carrier verification token with the secure backend endpoint
     * to obtain a custom Firebase Auth Token.
     */
    suspend fun exchangeTokenWithBackend(pnvToken: String): String = withContext(Dispatchers.IO) {
        val endpointUrl = "https://zaxo.eu.cc/verifyPnvToken"
        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpointUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            // Write payload JSON
            val payload = JSONObject().apply {
                put("pnvToken", pnvToken)
            }
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        reader.readText()
                    }
                }
                val jsonResponse = JSONObject(response)
                jsonResponse.getString("customToken")
            } else {
                val errorMsg = connection.errorStream?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        reader.readText()
                    }
                } ?: "Unknown error"
                throw Exception("Backend exchange failed with code $responseCode: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e("ZaxoPhoneAuthRepository", "Failed to exchange token with backend", e)
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Signs in to Firebase Authentication using the retrieved custom token.
     */
    suspend fun signInWithCustomToken(customToken: String) = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.signInWithCustomToken(customToken).await()
        } catch (e: Exception) {
            Log.e("ZaxoPhoneAuthRepository", "Firebase Custom Token authentication failed", e)
            throw e
        }
    }

    /**
     * Helper to enable test session support during app profiling.
     */
    fun enableTestingSession(testToken: String) {
        pnv.enableTestSession(testToken)
    }
}
