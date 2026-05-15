package com.proapps.voiceremind.sahko

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ElectricityPriceSelectorTest {

    @Test
    fun findCheapestNightSlot_prefersNightWindow() {
        val zone = ZoneId.of("Europe/Helsinki")
        val now = LocalDateTime.of(2026, 5, 14, 21, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        val slot1 = slot(zone, 2026, 5, 14, 22, 0, 3.2)
        val slot2 = slot(zone, 2026, 5, 15, 1, 0, 1.8)
        val slot3 = slot(zone, 2026, 5, 15, 2, 0, 2.4)

        val result = ElectricityPriceSelector.findCheapestNightSlot(
            nowEpochMillis = now,
            timezoneId = "Europe/Helsinki",
            prices = listOf(slot1, slot2, slot3)
        )

        assertNotNull(result)
        assertEquals(1.8, result!!.priceCentsPerKwh, 0.001)
    }

    @Test
    fun findCheapestNightSlot_fallsBackToAnyFutureHour() {
        val zone = ZoneId.of("Europe/Helsinki")
        val now = LocalDateTime.of(2026, 5, 14, 10, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        val slot1 = slot(zone, 2026, 5, 14, 12, 0, 4.2)
        val slot2 = slot(zone, 2026, 5, 14, 13, 0, 2.1)

        val result = ElectricityPriceSelector.findCheapestNightSlot(
            nowEpochMillis = now,
            timezoneId = "Europe/Helsinki",
            prices = listOf(slot1, slot2),
            nightHourStart = 0,
            nightHourEndExclusive = 7
        )

        assertNotNull(result)
        assertEquals(2.1, result!!.priceCentsPerKwh, 0.001)
    }

    @Test
    fun findCheapestNightSlot_supportsWindowAcrossMidnight() {
        val zone = ZoneId.of("Europe/Helsinki")
        val now = LocalDateTime.of(2026, 5, 14, 20, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        val slot1 = slot(zone, 2026, 5, 14, 21, 0, 1.1) // outside window 22-07
        val slot2 = slot(zone, 2026, 5, 14, 23, 0, 2.2)
        val slot3 = slot(zone, 2026, 5, 15, 3, 0, 0.9)

        val result = ElectricityPriceSelector.findCheapestNightSlot(
            nowEpochMillis = now,
            timezoneId = "Europe/Helsinki",
            prices = listOf(slot1, slot2, slot3),
            nightHourStart = 22,
            nightHourEndExclusive = 7
        )

        assertNotNull(result)
        assertEquals(0.9, result!!.priceCentsPerKwh, 0.001)
    }

    private fun slot(
        zoneId: ZoneId,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        price: Double
    ): ElectricityPriceSlot {
        val start = LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val end = LocalDateTime.of(year, month, day, hour, minute)
            .plusHours(1)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        return ElectricityPriceSlot(start, end, price)
    }
}

