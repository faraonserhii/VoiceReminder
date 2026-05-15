package com.proapps.voiceremind.medication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proapps.voiceremind.R

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val planId = inputData.getString(KEY_PLAN_ID).orEmpty().ifBlank { return Result.success() }
        val title = inputData.getString(KEY_TITLE).orEmpty().ifBlank {
            applicationContext.getString(R.string.medication_default_title)
        }
        val remainingDays = inputData.getInt(KEY_REMAINING_DAYS, -1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        MedicationReminderNotifier.ensureChannel(applicationContext)
        MedicationReminderNotifier.show(
            context = applicationContext,
            title = applicationContext.getString(R.string.medication_notification_title, title),
            body = applicationContext.getString(R.string.medication_notification_body),
            logId = "$planId:${System.currentTimeMillis()}",
            notificationId = buildNotificationId(planId)
        )

        val nextRemainingDays = if (remainingDays > 0) remainingDays - 1 else remainingDays
        if (nextRemainingDays == 0) {
            return Result.success()
        }

        scheduleNextDay(
            planId = planId,
            title = title,
            remainingDays = nextRemainingDays
        )

        return Result.success()
    }

    private fun scheduleNextDay(planId: String, title: String, remainingDays: Int) {
        val input = androidx.work.Data.Builder()
            .putString(KEY_PLAN_ID, planId)
            .putString(KEY_TITLE, title)
            .putInt(KEY_REMAINING_DAYS, remainingDays)
            .build()

        val request = androidx.work.OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(24, java.util.concurrent.TimeUnit.HOURS)
            .setInputData(input)
            .build()

        androidx.work.WorkManager.getInstance(applicationContext).enqueue(request)
    }

    private fun buildNotificationId(planId: String): Int {
        return planId.hashCode().coerceAtLeast(1)
    }

    companion object {
        const val KEY_PLAN_ID = "key_plan_id"
        const val KEY_TITLE = "key_title"
        const val KEY_REMAINING_DAYS = "key_remaining_days"
    }
}

