package com.proapps.voiceremind.medication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.proapps.voiceremind.R

class MedicationTakenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val logId = intent.getStringExtra(EXTRA_LOG_ID).orEmpty().ifBlank { "unknown" }
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank {
            context.getString(R.string.medication_default_title)
        }

        MedicationLogStore.append(
            context = context,
            logId = logId,
            title = title
        )
        Toast.makeText(context, context.getString(R.string.medication_taken_logged), Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_LOG_ID = "extra_log_id"
        const val EXTRA_TITLE = "extra_title"
    }
}

