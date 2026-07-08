package com.zaxo.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Detects cellular network availability and capability to switch between
 * modern Carrier PNV (single-tap eSIM/SIM-retrieval) and standard SMS OTP fallback.
 */
class CarrierVerificationManager(private val context: Context) {

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val pnvRepository = ZaxoPhoneAuthRepository(context)

    /**
     * Inspects active network capabilities, cellular SIM status, and PNV compatibility.
     * Returns true if modern cellular PNV is fully supported, or false if SMS OTP fallback should be utilized.
     */
    suspend fun isCellularPnvSupported(): Boolean {
        try {
            // Check 1: Check physical or eSIM status
            val simState = telephonyManager.simState
            if (simState != TelephonyManager.SIM_STATE_READY) {
                Log.w("CarrierVerificationManager", "No active SIM card detected. SIM state: $simState")
                return false
            }

            // Check 2: Check cellular data network capabilities
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val hasCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            if (!hasCellular) {
                Log.w("CarrierVerificationManager", "Active network is not cellular. Carrier PNV needs direct carrier data channel.")
            }

            // Check 3: Query the modern PNV SDK compatibility
            val isPnvCompatible = pnvRepository.checkPnvCompatibility()
            Log.d("CarrierVerificationManager", "PNV SDK Compatibility check result: $isPnvCompatible")

            return isPnvCompatible
        } catch (e: Exception) {
            Log.e("CarrierVerificationManager", "Error checking cellular PNV support, falling back to SMS OTP.", e)
            return false
        }
    }
}
