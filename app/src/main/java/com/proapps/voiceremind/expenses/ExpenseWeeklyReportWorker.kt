package com.proapps.voiceremind.expenses

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proapps.voiceremind.R
import java.util.Locale

class ExpenseWeeklyReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val summary = ExpenseLogStore.calculateCurrentWeekSummary(applicationContext)
        if (summary.entriesCount == 0) {
            return Result.success()
        }

        val restaurants = String.format(Locale.US, "%.2f", summary.restaurantsEuro)
        val total = String.format(Locale.US, "%.2f", summary.totalEuro)

        ExpenseWeeklyReportNotifier.ensureChannel(applicationContext)
        ExpenseWeeklyReportNotifier.show(
            context = applicationContext,
            title = applicationContext.getString(R.string.expense_weekly_notification_title),
            body = applicationContext.getString(
                R.string.expense_weekly_notification_body,
                restaurants,
                total
            ),
            notificationId = WEEKLY_REPORT_NOTIFICATION_ID
        )

        return Result.success()
    }

    companion object {
        private const val WEEKLY_REPORT_NOTIFICATION_ID = 55_010
    }
}

