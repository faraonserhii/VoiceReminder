package com.proapps.voiceremind.weather

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration

object WeatherAlertScheduler {

    private const val WARNING_HOURS_BEFORE = 1L

    fun schedule(
        context: Context,
        eventId: String,
        eventTitle: String,
        eventLocation: String,
        fallbackLocation: String,
        eventEpochMillis: Long,
        timezoneId: String
    ) {
        val locationForWeather = eventLocation.trim().ifBlank {
            fallbackLocation.ifBlank { context.getString(com.proapps.voiceremind.R.string.weather_default_location) }
        }

        val delayMillis = eventEpochMillis - Duration.ofHours(WARNING_HOURS_BEFORE).toMillis() - System.currentTimeMillis()
        val safeDelay = delayMillis.coerceAtLeast(0L)

        val input = Data.Builder()
            .putString(WeatherAlertWorker.KEY_LOCATION, locationForWeather)
            .putString(WeatherAlertWorker.KEY_FALLBACK_LOCATION, fallbackLocation)
            .putString(WeatherAlertWorker.KEY_TITLE, eventTitle)
            .putLong(WeatherAlertWorker.KEY_EVENT_EPOCH_MILLIS, eventEpochMillis)
            .putString(WeatherAlertWorker.KEY_TIMEZONE, timezoneId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<WeatherAlertWorker>()
            .setInitialDelay(safeDelay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(input)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "weather_alert_$eventId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

