package com.proapps.voiceremind.parking

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object ParkingControlScheduler {

    private const val REMINDER_LEAD_MINUTES = 10L

    fun schedule(context: Context, expiresAt: LocalDateTime) {
        val zoneId = ZoneId.systemDefault()
        val expiresEpochMillis = expiresAt.atZone(zoneId).toInstant().toEpochMilli()
        val triggerEpochMillis = expiresAt.minusMinutes(REMINDER_LEAD_MINUTES).atZone(zoneId).toInstant().toEpochMilli()
        val delayMillis = (triggerEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)

        val input = Data.Builder()
            .putLong(ParkingControlWorker.KEY_EXPIRES_EPOCH_MILLIS, expiresEpochMillis)
            .putLong(ParkingControlWorker.KEY_TRIGGER_EPOCH_MILLIS, triggerEpochMillis)
            .putString(ParkingControlWorker.KEY_TIMEZONE_ID, zoneId.id)
            .putLong(ParkingControlWorker.KEY_LEAD_MINUTES, REMINDER_LEAD_MINUTES)
            .build()

        val request = OneTimeWorkRequestBuilder<ParkingControlWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "parking_control_${expiresEpochMillis}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

