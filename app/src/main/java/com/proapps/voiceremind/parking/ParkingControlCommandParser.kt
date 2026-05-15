package com.proapps.voiceremind.parking

import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

data class ParkingControlCommand(
    val expiresAt: LocalDateTime,
    val remindBeforeMinutes: Int = 10
)

object ParkingControlCommandParser {

    private val untilTimeRegex = Regex(
        pattern = """(?:до|until|till)\s*(\d{1,2})[:.](\d{2})""",
        option = RegexOption.IGNORE_CASE
    )
    private val durationHourRegex = Regex(
        pattern = """на\s+(?:(\d{1,2})\s*час(?:а|ов)?|час)(?:\s|$)|for\s+(?:(\d{1,2})\s*hours?|an\s+hour)(?:\s|$)""",
        option = RegexOption.IGNORE_CASE
    )
    private val durationMinuteRegex = Regex(
        pattern = """на\s+(\d{1,3})\s*мин(?:ут(?:ы|а|у)?)?(?:\s|$)|for\s+(\d{1,3})\s*minutes?(?:\s|$)""",
        option = RegexOption.IGNORE_CASE
    )

    fun extract(rawText: String, now: LocalDateTime = LocalDateTime.now()): ParkingControlCommand? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        val normalized = text.lowercase(Locale.ROOT)
        val mentionsParking = normalized.contains("парков") ||
            normalized.contains("parking") ||
            normalized.contains("parkki") ||
            normalized.contains("parkkikiekko")
        if (!mentionsParking) return null

        val untilTime = untilTimeRegex.find(normalized)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return@let null
            val minute = match.groupValues[2].toIntOrNull() ?: return@let null
            if (hour !in 0..23 || minute !in 0..59) return@let null

            var target = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour, minute))
            if (!target.isAfter(now.plusMinutes(1))) {
                target = target.plusDays(1)
            }
            target
        }
        if (untilTime != null) {
            return ParkingControlCommand(expiresAt = untilTime)
        }

        val minutesDuration = durationMinuteRegex.find(normalized)
            ?.groupValues
            ?.drop(1)
            ?.firstOrNull { it.isNotBlank() }
            ?.toIntOrNull()
        if (minutesDuration != null && minutesDuration in 1..1440) {
            return ParkingControlCommand(expiresAt = now.plusMinutes(minutesDuration.toLong()))
        }

        val hoursDuration = durationHourRegex.find(normalized)?.let { match ->
            val explicit = listOf(match.groupValues.getOrNull(1), match.groupValues.getOrNull(2))
                .firstOrNull { !it.isNullOrBlank() }
                ?.toIntOrNull()
            (explicit ?: 1)
        }
        if (hoursDuration != null && hoursDuration in 1..24) {
            return ParkingControlCommand(expiresAt = now.plusHours(hoursDuration.toLong()))
        }

        return null
    }
}

