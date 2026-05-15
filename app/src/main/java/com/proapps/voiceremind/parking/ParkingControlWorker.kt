package com.proapps.voiceremind.parking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proapps.voiceremind.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ParkingControlWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val expiresEpochMillis = inputData.getLong(KEY_EXPIRES_EPOCH_MILLIS, -1L)
        if (expiresEpochMillis <= 0L) return Result.success()

        val timezoneId = inputData.getString(KEY_TIMEZONE_ID).orEmpty().ifBlank { ZoneId.systemDefault().id }
        val leadMinutes = inputData.getLong(KEY_LEAD_MINUTES, 10L).coerceAtLeast(1L)

        val expiresText = Instant.ofEpochMilli(expiresEpochMillis)
            .atZone(ZoneId.of(timezoneId))
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))

        ParkingControlNotifier.ensureChannel(applicationContext)
        ParkingControlNotifier.show(
            context = applicationContext,
            title = applicationContext.getString(R.string.parking_notification_title),
            body = applicationContext.getString(R.string.parking_notification_body, expiresText, leadMinutes),
            notificationId = buildNotificationId(expiresEpochMillis)
        )

        return Result.success()
    }

    private fun buildNotificationId(expiresEpochMillis: Long): Int {
        return (expiresEpochMillis % Int.MAX_VALUE).toInt().coerceAtLeast(1)
    }

    companion object {
        const val KEY_EXPIRES_EPOCH_MILLIS = "key_expires_epoch_millis"
        const val KEY_TRIGGER_EPOCH_MILLIS = "key_trigger_epoch_millis"
        const val KEY_TIMEZONE_ID = "key_timezone_id"
        const val KEY_LEAD_MINUTES = "key_lead_minutes"
    }
}

