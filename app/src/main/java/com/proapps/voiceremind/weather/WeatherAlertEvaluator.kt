package com.proapps.voiceremind.weather

import java.time.Instant
import java.time.ZoneId

private const val HEAVY_RAIN_MM_PER_HOUR = 10.0
private const val HEAVY_SNOW_CM_PER_HOUR = 2.0

enum class WeatherRiskType {
    HEAVY_RAIN,
    HEAVY_SNOW
}

data class WeatherRisk(
    val type: WeatherRiskType,
    val value: Double
)

object WeatherAlertEvaluator {

    fun evaluateAtEventHour(
        eventEpochMillis: Long,
        timezoneId: String,
        hourlyEpochSeconds: List<Long>,
        precipitationMm: List<Double>,
        snowfallCm: List<Double>
    ): WeatherRisk? {
        val zone = runCatching { ZoneId.of(timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val eventHour = Instant.ofEpochMilli(eventEpochMillis).atZone(zone).toLocalDateTime().withMinute(0).withSecond(0).withNano(0)

        var matchedIndex = -1
        for (i in hourlyEpochSeconds.indices) {
            val hour = Instant.ofEpochSecond(hourlyEpochSeconds[i]).atZone(zone).toLocalDateTime().withMinute(0).withSecond(0).withNano(0)
            if (hour == eventHour) {
                matchedIndex = i
                break
            }
        }

        if (matchedIndex == -1) return null

        val rain = precipitationMm.getOrNull(matchedIndex) ?: 0.0
        val snow = snowfallCm.getOrNull(matchedIndex) ?: 0.0

        return when {
            snow >= HEAVY_SNOW_CM_PER_HOUR -> WeatherRisk(WeatherRiskType.HEAVY_SNOW, snow)
            rain >= HEAVY_RAIN_MM_PER_HOUR -> WeatherRisk(WeatherRiskType.HEAVY_RAIN, rain)
            else -> null
        }
    }
}

