package com.proapps.voiceremind.expenses

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object ExpenseWeeklyReportScheduler {

    private const val UNIQUE_WORK_NAME = "expense_weekly_report"

    fun ensureScheduled(context: Context) {
        val now = ZonedDateTime.now()
        var nextRun = now
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .withHour(20)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusWeeks(1)
        }

        val delayMillis = Duration.between(now, nextRun).toMillis().coerceAtLeast(0L)

        val request = PeriodicWorkRequestBuilder<ExpenseWeeklyReportWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

