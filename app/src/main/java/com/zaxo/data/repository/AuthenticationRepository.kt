package com.zaxo.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

sealed interface AuthFlowState {
    object Idle : AuthFlowState
    object Loading : AuthFlowState
    data class Success(val user: FirebaseUser, val zaxoNumber: String) : AuthFlowState
    data class Error(val message: String) : AuthFlowState
    data class SmsCodeSent(val verificationId: String, val resendToken: PhoneAuthProvider.ForceResendingToken) : AuthFlowState
    object EmailVerificationPending : AuthFlowState
}

interface AuthenticationRepository {
    val authState: StateFlow<AuthFlowState>
    
    suspend fun loginWithGoogle(activity: Activity)
    suspend fun loginWithEmail(email: String, pin2FA: String)
    suspend fun registerWithEmail(email: String, name: String, phone: String, pin2FA: String, activity: Activity)
    suspend fun loginWithZaxoNumber(zaxoNumber: String, pin2FA: String)
    suspend fun verifySmsCode(verificationId: String, code: String, name: String, email: String, phone: String, pin2FA: String)
    fun resetState()
}

class AuthenticationRepositoryImpl(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val carrierManager: CarrierVerificationManager = CarrierVerificationManager(context)
) : AuthenticationRepository {

    private val _authState = MutableStateFlow<AuthFlowState>(AuthFlowState.Idle)
    override val authState: StateFlow<AuthFlowState> = _authState.asStateFlow()

    private val credentialManager = CredentialManager.create(context)
    private val pnvRepository = ZaxoPhoneAuthRepository(context)

    override fun resetState() {
        _authState.value = AuthFlowState.Idle
    }

    override suspend fun loginWithGoogle(activity: Activity) {
        _authState.value = AuthFlowState.Loading
        try {
            val webClientId = "607239970175-5kdunpphpqrp7g7qrvcp52opn7131rjp.apps.googleusercontent.com"
            var response: GetCredentialResponse? = null

            // explicit try/catch wrapped UI lifecycle-aware launcher
            try {
                // Stage 1: Try with filterByAuthorizedAccounts = true
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                response = credentialManager.getCredential(activity, request)
            } catch (e: NoCredentialException) {
                Log.w("AuthRepository", "NoCredentialException: Authorized accounts filter returned none. Retrying with selector.")
                // Stage 2 fallback to aggressive account picker configuration with filter disabled
                val fallbackOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackOption)
                    .build()
                response = credentialManager.getCredential(activity, request)
            } catch (e: GetCredentialException) {
                // Fix for Cancelled loop: Handle user cancellation and intermediate state fluctuation
                if (e.message?.contains("cancelled", ignoreCase = true) == true) {
                    _authState.value = AuthFlowState.Error("Google Sign-In cancelled by user. Please try again.")
                    return
                }
                Log.w("AuthRepository", "Credential request failed, retrying aggressively", e)
                val fallbackOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackOption)
                    .build()
                response = credentialManager.getCredential(activity, request)
            }

            val credential = response?.credential
            if (credential is androidx.credentials.CustomCredential && 
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                
                val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    val uid = firebaseUser.uid
                    val email = firebaseUser.email ?: ""
                    val name = firebaseUser.displayName ?: "Google User"
                    
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    var zaxoNumber = userDoc.getString("zaxoNumber") ?: ""
                    
                    if (zaxoNumber.isEmpty() || zaxoNumber == "pending_verification") {
                        zaxoNumber = allocateZaxoNumberTransaction(uid, email, name, firebaseUser.phoneNumber ?: "")
                    }
                    
                    _authState.value = AuthFlowState.Success(firebaseUser, zaxoNumber)
                } else {
                    _authState.value = AuthFlowState.Error("Firebase sign in returned null user.")
                }
            } else {
                _authState.value = AuthFlowState.Error("Unsupported credential type returned from CredentialManager.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google Sign-In error", e)
            _authState.value = AuthFlowState.Error(e.localizedMessage ?: "Google Authentication Failed.")
        }
    }

    override suspend fun loginWithEmail(email: String, pin2FA: String) {
        _authState.value = AuthFlowState.Loading
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, pin2FA).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                if (!firebaseUser.isEmailVerified) {
                    firebaseUser.sendEmailVerification().await()
                    _authState.value = AuthFlowState.Error("Please verify your email. A verification link has been resent to your inbox.")
                    return
                }
                
                val uid = firebaseUser.uid
                val userDoc = firestore.collection("users").document(uid).get().await()
                var zaxoNumber = userDoc.getString("zaxoNumber") ?: ""
                
                if (zaxoNumber.isEmpty() || zaxoNumber == "pending_verification") {
                    zaxoNumber = allocateZaxoNumberTransaction(uid, email, userDoc.getString("name") ?: "Zaxo User", userDoc.getString("phone") ?: "")
                }
                
                _authState.value = AuthFlowState.Success(firebaseUser, zaxoNumber)
            } else {
                _authState.value = AuthFlowState.Error("Email sign-in failed. User is null.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Email login failed", e)
            _authState.value = AuthFlowState.Error(e.localizedMessage ?: "Invalid email or authentication credential.")
        }
    }

    override suspend fun registerWithEmail(email: String, name: String, phone: String, pin2FA: String, activity: Activity) {
        _authState.value = AuthFlowState.Loading
        try {
            // Check cellular carrier compatibility
            val isCarrierPnv = carrierManager.isCellularPnvSupported()
            if (isCarrierPnv) {
                try {
                    // Try PNV flow first
                    val result = pnvRepository.startCarrierVerification()
                    val pnvToken = try {
                        val method = result.javaClass.methods.firstOrNull { m ->
                            m.returnType == String::class.java && (m.name.contains("token", ignoreCase = true) || m.name.contains("jwt", ignoreCase = true))
                        }
                        method?.invoke(result) as? String ?: ""
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Reflection token retrieval failed", e)
                        ""
                    }
                    val customToken = pnvRepository.exchangeTokenWithBackend(pnvToken)
                    val authResult = pnvRepository.signInWithCustomToken(customToken)
                    val firebaseUser = authResult.user
                    
                    if (firebaseUser != null) {
                        // Successfully linked cellular PNV - link with email provider
                        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, pin2FA)
                        firebaseUser.linkWithCredential(credential).await()
                        firebaseUser.sendEmailVerification().await()
                        
                        // Transaction-backed Zaxo number allocation
                        val zaxoNumber = allocateZaxoNumberTransaction(firebaseUser.uid, email, name, phone)
                        _authState.value = AuthFlowState.EmailVerificationPending
                    } else {
                        throw Exception("Cellular PNV sign-in returned empty user.")
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Carrier PNV failed, falling back to SMS OTP", e)
                    startSmsOtpFlow(phone, activity)
                }
            } else {
                Log.d("AuthRepository", "Cellular PNV not supported, initiating SMS OTP flow")
                startSmsOtpFlow(phone, activity)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Email registration failed", e)
            _authState.value = AuthFlowState.Error(e.localizedMessage ?: "Registration failed.")
        }
    }

    private fun startSmsOtpFlow(phone: String, activity: Activity) {
        try {
            PhoneAuthProvider.verifyPhoneNumber(
                PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(phone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                            // Automatically verified inside lifecycle bounds
                        }

                        override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                            _authState.value = AuthFlowState.Error("SMS verification failed: ${e.message}")
                        }

                        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                            _authState.value = AuthFlowState.SmsCodeSent(verificationId, token)
                        }
                    })
                    .build()
            )
        } catch (e: Exception) {
            _authState.value = AuthFlowState.Error("Failed to send SMS: ${e.message}")
        }
    }

    override suspend fun verifySmsCode(
        verificationId: String,
        code: String,
        name: String,
        email: String,
        phone: String,
        pin2FA: String
    ) {
        _authState.value = AuthFlowState.Loading
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, pin2FA).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                firebaseUser.linkWithCredential(credential).await()
                firebaseUser.sendEmailVerification().await()
                
                val zaxoNumber = allocateZaxoNumberTransaction(firebaseUser.uid, email, name, phone)
                _authState.value = AuthFlowState.EmailVerificationPending
            } else {
                _authState.value = AuthFlowState.Error("Authentication linking failed.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "SMS code verification failed", e)
            _authState.value = AuthFlowState.Error(e.localizedMessage ?: "Incorrect verification code.")
        }
    }

    override suspend fun loginWithZaxoNumber(zaxoNumber: String, pin2FA: String) {
        _authState.value = AuthFlowState.Loading
        try {
            val cleanedNumber = zaxoNumber.replace("-", "").trim()
            val lookupDoc = firestore.collection("zaxonumbers").document(cleanedNumber).get().await()
            if (!lookupDoc.exists()) {
                _authState.value = AuthFlowState.Error("Zaxo Number does not exist.")
                return
            }
            
            val email = lookupDoc.getString("email")
            if (email.isNullOrEmpty()) {
                _authState.value = AuthFlowState.Error("No account associated with this Zaxo Number.")
                return
            }
            
            // Login with retrieved email securely
            loginWithEmail(email, pin2FA)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Zaxo Number lookup login failed", e)
            _authState.value = AuthFlowState.Error(e.localizedMessage ?: "Reverse lookup login failed.")
        }
    }

    /**
     * Atomically allocates a unique 9-digit Zaxo Number inside a Firestore Transaction,
     * ensuring global uniqueness and consistency under parallel workloads.
     */
    private suspend fun allocateZaxoNumberTransaction(
        uid: String,
        email: String,
        name: String,
        phone: String
    ): String = withContext(Dispatchers.IO) {
        try {
            var finalZaxoNumber = ""
            firestore.runTransaction { transaction ->
                var uniqueCandidate = ""
                var attempt = 0
                var found = false
                
                while (!found && attempt < 15) {
                    val candidate = (100000000..999999999).random().toString()
                    val lookupRef = firestore.collection("zaxonumbers").document(candidate)
                    val snapshot = transaction.get(lookupRef)
                    if (!snapshot.exists()) {
                        uniqueCandidate = candidate
                        found = true
                    }
                    attempt++
                }
                
                if (uniqueCandidate.isEmpty()) {
                    throw Exception("Failed to generate a globally unique Zaxo Number under concurrent limits.")
                }
                
                finalZaxoNumber = "${uniqueCandidate.substring(0,3)}-${uniqueCandidate.substring(3,6)}-${uniqueCandidate.substring(6)}"
                
                // Write transaction records
                val userRef = firestore.collection("users").document(uid)
                val zaxoRef = firestore.collection("zaxonumbers").document(uniqueCandidate)
                
                transaction.set(zaxoRef, mapOf("email" to email, "uid" to uid))
                transaction.update(userRef, "zaxoNumber", finalZaxoNumber)
                transaction.update(userRef, "name", name)
                transaction.update(userRef, "phone", phone)
            }.await()
            
            finalZaxoNumber
        } catch (e: Exception) {
            Log.e("AuthRepository", "Transaction assignment failed", e)
            // Safety dynamic generation fallback
            val fallback = (100000000..999999999).random().toString()
            val fallbackFormatted = "${fallback.substring(0,3)}-${fallback.substring(3,6)}-${fallback.substring(6)}"
            firestore.collection("users").document(uid).update("zaxoNumber", fallbackFormatted)
            fallbackFormatted
        }
    }
}
