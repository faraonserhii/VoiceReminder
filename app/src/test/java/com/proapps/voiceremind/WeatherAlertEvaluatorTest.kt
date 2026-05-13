package com.proapps.voiceremind

import com.proapps.voiceremind.weather.WeatherAlertEvaluator
import com.proapps.voiceremind.weather.WeatherRiskType
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherAlertEvaluatorTest {

    private val zone = ZoneId.of("Europe/Helsinki")

    @Test
    fun evaluateAtEventHour_heavySnow_detected() {
        val event = LocalDateTime.of(2026, 12, 15, 18, 30)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        val hourEpoch = LocalDateTime.of(2026, 12, 15, 18, 0)
            .atZone(zone)
            .toEpochSecond()

        val risk = WeatherAlertEvaluator.evaluateAtEventHour(
            eventEpochMillis = event,
            timezoneId = zone.id,
            hourlyEpochSeconds = listOf(hourEpoch),
            precipitationMm = listOf(2.0),
            snowfallCm = listOf(2.5)
        )

        assertEquals(WeatherRiskType.HEAVY_SNOW, risk?.type)
    }

    @Test
    fun evaluateAtEventHour_noRisk_returnsNull() {
        val event = LocalDateTime.of(2026, 7, 10, 9, 10)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        val hourEpoch = LocalDateTime.of(2026, 7, 10, 9, 0)
            .atZone(zone)
            .toEpochSecond()

        val risk = WeatherAlertEvaluator.evaluateAtEventHour(
            eventEpochMillis = event,
            timezoneId = zone.id,
            hourlyEpochSeconds = listOf(hourEpoch),
            precipitationMm = listOf(1.0),
            snowfallCm = listOf(0.0)
        )

        assertNull(risk)
    }
}

