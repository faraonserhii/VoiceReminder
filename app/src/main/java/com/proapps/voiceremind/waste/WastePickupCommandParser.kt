package com.proapps.voiceremind.waste

import java.time.DayOfWeek
import java.util.Locale

data class WastePickupCommand(
    val materialLabel: String,
    val dayOfWeek: DayOfWeek,
    val intervalWeeks: Int
)

object WastePickupCommandParser {

    private val dayRegex = Regex(
        pattern = """по\s+(понедельникам|вторникам|средам|четвергам|пятницам|субботам|воскресеньям|monday|tuesday|wednesday|thursday|friday|saturday|sunday)""",
        option = RegexOption.IGNORE_CASE
    )

    fun extract(rawText: String): WastePickupCommand? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        val normalized = text.lowercase(Locale.ROOT)
        val weekdayMatch = dayRegex.find(normalized) ?: return null
        val dayToken = weekdayMatch.groupValues.getOrNull(1).orEmpty()
        val dayOfWeek = resolveDayOfWeek(dayToken) ?: return null

        val intervalWeeks = when {
            normalized.contains("раз в две недели") || normalized.contains("every two weeks") -> 2
            normalized.contains("раз в неделю") || normalized.contains("каждую неделю") || normalized.contains("every week") -> 1
            else -> 1
        }

        val materialLabel = text.substring(0, weekdayMatch.range.first)
            .trim(' ', ',', '.', ':', ';', '-', '!', '?')
            .ifBlank { "мусор" }

        return WastePickupCommand(
            materialLabel = materialLabel,
            dayOfWeek = dayOfWeek,
            intervalWeeks = intervalWeeks
        )
    }

    private fun resolveDayOfWeek(token: String): DayOfWeek? {
        return when (token.lowercase(Locale.ROOT)) {
            "понедельникам", "monday" -> DayOfWeek.MONDAY
            "вторникам", "tuesday" -> DayOfWeek.TUESDAY
            "средам", "wednesday" -> DayOfWeek.WEDNESDAY
            "четвергам", "thursday" -> DayOfWeek.THURSDAY
            "пятницам", "friday" -> DayOfWeek.FRIDAY
            "субботам", "saturday" -> DayOfWeek.SATURDAY
            "воскресеньям", "sunday" -> DayOfWeek.SUNDAY
            else -> null
        }
    }
}

