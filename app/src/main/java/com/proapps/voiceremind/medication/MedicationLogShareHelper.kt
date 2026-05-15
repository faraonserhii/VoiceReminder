package com.proapps.voiceremind.medication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider

object MedicationLogShareHelper {

    fun buildShareIntent(context: Context): Intent? {
        if (!MedicationLogStore.hasEntries(context)) return null

        val file = MedicationLogStore.getLogFile(context)
        val uri = if (isRobolectric()) {
            Uri.fromFile(file)
        } else {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(com.proapps.voiceremind.R.string.medication_export_subject))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun isRobolectric(): Boolean {
        return Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
    }
}

