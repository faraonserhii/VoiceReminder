package com.proapps.voiceremind.waste

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object WastePickupScheduler {

    private const val REMINDER_HOUR = 20

    fun schedule(context: Context, command: WastePickupCommand) {
        val now = ZonedDateTime.now()
        val nextReminderAt = calculateNextReminderTime(
            now = now,
            pickupDayOfWeek = command.dayOfWeek,
            intervalWeeks = command.intervalWeeks,
            reminderHour = REMINDER_HOUR
        )
        val initialDelayMillis = Duration.between(now, nextReminderAt).toMillis().coerceAtLeast(0L)
        val repeatIntervalDays = (command.intervalWeeks * 7).toLong().coerceAtLeast(7L)

        val input = Data.Builder()
            .putString(WastePickupReminderWorker.KEY_MATERIAL_LABEL, command.materialLabel)
            .build()

        val request = PeriodicWorkRequestBuilder<WastePickupReminderWorker>(
            repeatIntervalDays,
            TimeUnit.DAYS
        )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        val uniqueName = "waste_pickup_${command.materialLabel.lowercase()}_${command.dayOfWeek.name}_${command.intervalWeeks}"
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueName,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    internal fun calculateNextReminderTime(
        now: ZonedDateTime,
        pickupDayOfWeek: DayOfWeek,
        intervalWeeks: Int,
        reminderHour: Int
    ): ZonedDateTime {
        val safeInterval = intervalWeeks.coerceAtLeast(1)
        var pickupDate = now.with(TemporalAdjusters.nextOrSame(pickupDayOfWeek)).toLocalDate()
        var reminderAt = ZonedDateTime.of(
            pickupDate.minusDays(1),
            LocalTime.of(reminderHour.coerceIn(0, 23), 0),
            now.zone
        )

        while (!reminderAt.isAfter(now)) {
            pickupDate = pickupDate.plusWeeks(safeInterval.toLong())
            reminderAt = ZonedDateTime.of(
                pickupDate.minusDays(1),
                LocalTime.of(reminderHour.coerceIn(0, 23), 0),
                now.zone
            )
        }

        return reminderAt
    }
}

