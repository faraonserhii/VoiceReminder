package com.proapps.voiceremind.medication

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object MedicationReminderScheduler {

    fun schedule(
        context: Context,
        planId: String,
        title: String,
        firstTriggerEpochMillis: Long,
        daysCount: Int?
    ) {
        val delayMillis = (firstTriggerEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)

        val input = Data.Builder()
            .putString(MedicationReminderWorker.KEY_PLAN_ID, planId)
            .putString(MedicationReminderWorker.KEY_TITLE, title)
            .putInt(MedicationReminderWorker.KEY_REMAINING_DAYS, daysCount ?: -1)
            .build()

        val request = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "medication_intake_$planId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

