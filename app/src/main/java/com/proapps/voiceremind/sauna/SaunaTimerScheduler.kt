package com.proapps.voiceremind.sauna

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SaunaTimerScheduler {

    fun schedule(context: Context, minutes: Int) {
        val safeMinutes = minutes.coerceIn(1, 720)
        val triggerEpochMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(safeMinutes.toLong())

        val input = Data.Builder()
            .putInt(SaunaTimerWorker.KEY_MINUTES, safeMinutes)
            .putLong(SaunaTimerWorker.KEY_TRIGGER_EPOCH_MILLIS, triggerEpochMillis)
            .build()

        val request = OneTimeWorkRequestBuilder<SaunaTimerWorker>()
            .setInitialDelay(safeMinutes.toLong(), TimeUnit.MINUTES)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}

