package com.proapps.voiceremind

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TireWatchDomainTest {

    @Test
    fun resolveOfficialDate_winter_returnsUpcomingNovember() {
        val decision = TireChangePolicy.resolveOfficialDate(
            now = LocalDate.of(2026, 10, 15),
            requestedSeason = TireSeason.WINTER
        )

        assertEquals(TireSeason.WINTER, decision.season)
        assertEquals(LocalDate.of(2026, 11, 1), decision.officialDate)
    }

    @Test
    fun resolveOfficialDate_unspecified_returnsNearestSeasonDate() {
        val decision = TireChangePolicy.resolveOfficialDate(
            now = LocalDate.of(2026, 3, 20),
            requestedSeason = null
        )

        assertEquals(TireSeason.SUMMER, decision.season)
        assertEquals(LocalDate.of(2026, 4, 1), decision.officialDate)
    }

    @Test
    fun resolveOfficialDate_usesCustomConfiguredDates() {
        val decision = TireChangePolicy.resolveOfficialDate(
            now = LocalDate.of(2026, 9, 10),
            requestedSeason = TireSeason.WINTER,
            summerMonth = 3,
            summerDay = 25,
            winterMonth = 10,
            winterDay = 15
        )

        assertEquals(TireSeason.WINTER, decision.season)
        assertEquals(LocalDate.of(2026, 10, 15), decision.officialDate)
    }

    @Test
    fun hasEarlyFreezeBeforeOfficialDate_detectsFreeze() {
        val zone = ZoneId.of("Europe/Helsinki")
        val officialDate = LocalDate.of(2026, 11, 1)

        val sample1 = LocalDateTime.of(2026, 10, 28, 9, 0).atZone(zone).toEpochSecond()
        val sample2 = LocalDateTime.of(2026, 10, 31, 6, 0).atZone(zone).toEpochSecond()

        val hasFreeze = TireFreezeAdvisor.hasEarlyFreezeBeforeOfficialDate(
            officialDate = officialDate,
            timezoneId = zone.id,
            hourlyEpochSeconds = listOf(sample1, sample2),
            temperatureC = listOf(2.0, -1.0),
            freezeThresholdC = 0.0
        )

        assertTrue(hasFreeze)
    }
}

