package com.proapps.voiceremind

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class TireSeason {
    WINTER,
    SUMMER
}

data class TirePolicyDecision(
    val season: TireSeason,
    val officialDate: LocalDate
)

object TireChangePolicy {

    const val DEFAULT_SUMMER_MONTH = 4
    const val DEFAULT_SUMMER_DAY = 1
    const val DEFAULT_WINTER_MONTH = 11
    const val DEFAULT_WINTER_DAY = 1

    fun isTireChangeRequest(rawText: String): Boolean {
        val text = rawText.lowercase(Locale.ROOT)
        val tireKeywords = listOf("шины", "резин", "renka", "tire")
        val actionKeywords = listOf("помен", "смен", "vaih", "change")
        return tireKeywords.any { text.contains(it) } && actionKeywords.any { text.contains(it) }
    }

    fun detectSeason(rawText: String): TireSeason? {
        val text = rawText.lowercase(Locale.ROOT)
        return when {
            listOf("зимн", "talvi", "winter").any { text.contains(it) } -> TireSeason.WINTER
            listOf("летн", "kesä", "summer").any { text.contains(it) } -> TireSeason.SUMMER
            else -> null
        }
    }

    fun resolveOfficialDate(
        now: LocalDate,
        requestedSeason: TireSeason?,
        summerMonth: Int = DEFAULT_SUMMER_MONTH,
        summerDay: Int = DEFAULT_SUMMER_DAY,
        winterMonth: Int = DEFAULT_WINTER_MONTH,
        winterDay: Int = DEFAULT_WINTER_DAY
    ): TirePolicyDecision {
        if (requestedSeason != null) {
            return TirePolicyDecision(
                season = requestedSeason,
                officialDate = nextDateForSeason(now, requestedSeason, summerMonth, summerDay, winterMonth, winterDay)
            )
        }

        val nextSummer = nextDateForSeason(now, TireSeason.SUMMER, summerMonth, summerDay, winterMonth, winterDay)
        val nextWinter = nextDateForSeason(now, TireSeason.WINTER, summerMonth, summerDay, winterMonth, winterDay)
        return if (nextSummer <= nextWinter) {
            TirePolicyDecision(TireSeason.SUMMER, nextSummer)
        } else {
            TirePolicyDecision(TireSeason.WINTER, nextWinter)
        }
    }

    private fun nextDateForSeason(
        now: LocalDate,
        season: TireSeason,
        summerMonth: Int,
        summerDay: Int,
        winterMonth: Int,
        winterDay: Int
    ): LocalDate {
        val candidateThisYear = when (season) {
            TireSeason.SUMMER -> LocalDate.of(now.year, summerMonth, summerDay)
            TireSeason.WINTER -> LocalDate.of(now.year, winterMonth, winterDay)
        }
        return if (!candidateThisYear.isBefore(now)) candidateThisYear else candidateThisYear.plusYears(1)
    }
}

object TireFreezeAdvisor {

    fun hasEarlyFreezeBeforeOfficialDate(
        officialDate: LocalDate,
        timezoneId: String,
        hourlyEpochSeconds: List<Long>,
        temperatureC: List<Double>,
        freezeThresholdC: Double = 0.0
    ): Boolean {
        val zone = runCatching { ZoneId.of(timezoneId) }.getOrDefault(ZoneId.systemDefault())
        for (i in hourlyEpochSeconds.indices) {
            val sampleDate = Instant.ofEpochSecond(hourlyEpochSeconds[i]).atZone(zone).toLocalDate()
            val temp = temperatureC.getOrNull(i) ?: continue
            if (sampleDate.isBefore(officialDate) && temp <= freezeThresholdC) {
                return true
            }
        }
        return false
    }
}

