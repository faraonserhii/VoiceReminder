package com.proapps.voiceremind.sahko

import java.time.Instant
import java.time.ZoneId

data class ElectricityPriceSlot(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val priceCentsPerKwh: Double
)

object ElectricityPriceSelector {

    fun findCheapestNightSlot(
        nowEpochMillis: Long,
        timezoneId: String,
        prices: List<ElectricityPriceSlot>,
        nightHourStart: Int = 0,
        nightHourEndExclusive: Int = 7
    ): ElectricityPriceSlot? {
        if (prices.isEmpty()) return null

        val zone = runCatching { ZoneId.of(timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val future = prices.filter { it.startEpochMillis > nowEpochMillis }
        if (future.isEmpty()) return null

        val nightCandidates = future.filter { slot ->
            val hour = Instant.ofEpochMilli(slot.startEpochMillis).atZone(zone).hour
            isHourInWindow(hour, nightHourStart, nightHourEndExclusive)
        }

        val target = if (nightCandidates.isNotEmpty()) nightCandidates else future
        return target.minByOrNull { it.priceCentsPerKwh }
    }

    private fun isHourInWindow(hour: Int, start: Int, endExclusive: Int): Boolean {
        val safeHour = hour.coerceIn(0, 23)
        val safeStart = start.coerceIn(0, 23)
        val safeEnd = endExclusive.coerceIn(0, 23)

        if (safeStart == safeEnd) return true
        return if (safeStart < safeEnd) {
            safeHour in safeStart until safeEnd
        } else {
            safeHour >= safeStart || safeHour < safeEnd
        }
    }
}

