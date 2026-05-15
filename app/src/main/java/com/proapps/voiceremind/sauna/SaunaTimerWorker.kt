package com.proapps.voiceremind.sauna

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proapps.voiceremind.R

class SaunaTimerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val minutes = inputData.getInt(KEY_MINUTES, 0).coerceAtLeast(1)
        val triggerEpochMillis = inputData.getLong(KEY_TRIGGER_EPOCH_MILLIS, System.currentTimeMillis())

        SaunaTimerNotifier.ensureChannel(applicationContext)
        SaunaTimerNotifier.show(
            context = applicationContext,
            title = applicationContext.getString(R.string.sauna_notification_title),
            body = applicationContext.getString(R.string.sauna_notification_body, minutes),
            notificationId = buildNotificationId(triggerEpochMillis)
        )

        return Result.success()
    }

    private fun buildNotificationId(triggerEpochMillis: Long): Int {
        return (triggerEpochMillis % Int.MAX_VALUE).toInt().coerceAtLeast(1)
    }

    companion object {
        const val KEY_MINUTES = "key_minutes"
        const val KEY_TRIGGER_EPOCH_MILLIS = "key_trigger_epoch_millis"
    }
}

