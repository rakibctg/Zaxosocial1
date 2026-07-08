package com.zaxo.data.security

import android.content.Context
import android.content.SharedPreferences
import com.zaxo.data.security.ZaxoKeyStoreHelper

/**
 * SecureVault utility class using Android KeyStore and AES-256-GCM for encrypting
 * sensitive Zaxo session tokens and local user profile metadata before storage.
 */
object SecureVault {

    private const val PREFS_NAME = "zaxo_secure_vault_prefs"

    private fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Secures a sensitive string value (e.g., session token, profile metadata)
     * using hardware-backed AES-256-GCM encryption and stores it in SharedPreferences.
     */
    fun storeSecureString(context: Context, key: String, value: String) {
        val encrypted = ZaxoKeyStoreHelper.encrypt(value)
        getSharedPrefs(context).edit().putString(key, encrypted).apply()
    }

    /**
     * Retrieves and decrypts a sensitive string value using Android KeyStore AES-256-GCM.
     */
    fun getSecureString(context: Context, key: String, defaultValue: String = ""): String {
        val encrypted = getSharedPrefs(context).getString(key, null) ?: return defaultValue
        val decrypted = ZaxoKeyStoreHelper.decrypt(encrypted)
        return if (decrypted.isEmpty()) defaultValue else decrypted
    }

    /**
     * Clears all secured items from the SecureVault.
     */
    fun clear(context: Context) {
        getSharedPrefs(context).edit().clear().apply()
    }
}
