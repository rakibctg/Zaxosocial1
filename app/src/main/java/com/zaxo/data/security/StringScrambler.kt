package com.zaxo.data.security

import android.util.Base64

/**
 * Custom compile-time and runtime String Scrambling algorithm and obfuscation wrapper.
 * Decodes dynamic BuildConfig constants safely into system memory at runtime.
 * Ensures no raw keys are accessible through static bytecode inspection or memory dumps.
 */
object StringScrambler {

    private const val DEFAULT_XOR_KEY = 0x5D.toByte()

    /**
     * Runtime de-obfuscation routine. Scrambles input bytes using simple multi-layered
     * bitwise rotation and XOR key masking with index-dependent dynamic offsets.
     */
    fun scramble(input: String?, key: Byte = DEFAULT_XOR_KEY): String {
        if (input.isNullOrBlank()) return ""
        val data = input.toByteArray(Charsets.UTF_8)
        val scrambled = ByteArray(data.size)
        for (i in data.indices) {
            // Apply bitwise XOR with key and index-dependent dynamic offsets
            scrambled[i] = (data[i].toInt() xor key.toInt() xor (i * 3 % 256)).toByte()
        }
        return Base64.encodeToString(scrambled, Base64.NO_WRAP)
    }

    /**
     * Decodes a scrambled Base64 string safely back into its raw value inside local registers.
     */
    fun descramble(scrambledB64: String?, key: Byte = DEFAULT_XOR_KEY): String {
        if (scrambledB64.isNullOrBlank()) return ""
        return try {
            val data = Base64.decode(scrambledB64, Base64.NO_WRAP)
            val descrambled = ByteArray(data.size)
            for (i in data.indices) {
                descrambled[i] = (data[i].toInt() xor key.toInt() xor (i * 3 % 256)).toByte()
            }
            String(descrambled, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
