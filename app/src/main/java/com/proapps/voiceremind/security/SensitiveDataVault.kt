package com.proapps.voiceremind.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.proapps.voiceremind.SensitiveDataType

object SensitiveDataVault {

    private const val PREFS_NAME = "sensitive_data_vault"

    fun save(context: Context, type: SensitiveDataType, value: String): Boolean {
        val prefs = encryptedPrefs(context) ?: return false
        prefs.edit().putString(type.storageKey(), value).apply()
        return true
    }

    fun load(context: Context, type: SensitiveDataType): String? {
        val prefs = encryptedPrefs(context) ?: return null
        return prefs.getString(type.storageKey(), null)
    }

    private fun encryptedPrefs(context: Context): android.content.SharedPreferences? {
        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()
    }

    private fun SensitiveDataType.storageKey(): String {
        return when (this) {
            SensitiveDataType.TAX_NUMBER -> "tax_number"
            SensitiveDataType.PERSONAL_ID -> "personal_id"
            SensitiveDataType.INSURANCE_NUMBER -> "insurance_number"
        }
    }
}

