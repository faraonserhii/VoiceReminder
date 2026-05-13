package com.proapps.voiceremind.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proapps.voiceremind.R

class WeatherAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val location = inputData.getString(KEY_LOCATION).orEmpty().trim()
        val fallbackFromSettings = inputData.getString(KEY_FALLBACK_LOCATION).orEmpty().trim()
        val title = inputData.getString(KEY_TITLE).orEmpty().ifBlank {
            applicationContext.getString(R.string.default_event_title)
        }
        val eventEpochMillis = inputData.getLong(KEY_EVENT_EPOCH_MILLIS, -1L)
        val timezoneId = inputData.getString(KEY_TIMEZONE).orEmpty().ifBlank { java.util.TimeZone.getDefault().id }

        if (eventEpochMillis <= 0L) {
            return Result.success()
        }

        val client = OpenMeteoClient()
        val fallbackLocation = fallbackFromSettings.ifBlank {
            applicationContext.getString(R.string.weather_default_location)
        }
        val primaryLocation = location.ifBlank { fallbackLocation }
        val point = client.geocode(primaryLocation)
            ?: if (primaryLocation != fallbackLocation) client.geocode(fallbackLocation) else null
        if (point == null) {
            return Result.success()
        }
        val forecast = client.loadForecast(point.latitude, point.longitude, timezoneId) ?: return Result.retry()

        val risk = WeatherAlertEvaluator.evaluateAtEventHour(
            eventEpochMillis = eventEpochMillis,
            timezoneId = timezoneId,
            hourlyEpochSeconds = forecast.hourlyEpochSeconds,
            precipitationMm = forecast.precipitationMm,
            snowfallCm = forecast.snowfallCm
        ) ?: return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        WeatherAlertNotifier.ensureChannel(applicationContext)
        val body = when (risk.type) {
            WeatherRiskType.HEAVY_SNOW -> applicationContext.getString(R.string.weather_alert_snow_body)
            WeatherRiskType.HEAVY_RAIN -> applicationContext.getString(R.string.weather_alert_rain_body)
        }
        val notificationText = applicationContext.getString(
            R.string.weather_alert_body_template,
            title,
            body
        )

        WeatherAlertNotifier.show(
            context = applicationContext,
            title = applicationContext.getString(R.string.weather_alert_title),
            body = notificationText,
            notificationId = buildNotificationId(eventEpochMillis)
        )

        return Result.success()
    }

    private fun buildNotificationId(eventEpochMillis: Long): Int {
        return (eventEpochMillis % Int.MAX_VALUE).toInt().coerceAtLeast(1)
    }

    companion object {
        const val KEY_LOCATION = "key_location"
        const val KEY_FALLBACK_LOCATION = "key_fallback_location"
        const val KEY_TITLE = "key_title"
        const val KEY_EVENT_EPOCH_MILLIS = "key_event_epoch_millis"
        const val KEY_TIMEZONE = "key_timezone"
    }
}

