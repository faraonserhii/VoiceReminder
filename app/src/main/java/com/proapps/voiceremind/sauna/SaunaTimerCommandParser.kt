package com.proapps.voiceremind.sauna

import java.util.Locale

data class SaunaTimerCommand(
    val minutes: Int
)

object SaunaTimerCommandParser {

    private val delayMinutesRegex = Regex(
        pattern = """(?:через|in|after)\s+(\d{1,3})\s*(?:мин(?:ут(?:ы|а|у)?)?|minutes?|mins?)""",
        option = RegexOption.IGNORE_CASE
    )
    private val delayHoursRegex = Regex(
        pattern = """(?:через|in|after)\s+(\d{1,2})\s*(?:час(?:а|ов)?|hours?|hrs?)""",
        option = RegexOption.IGNORE_CASE
    )
    private val halfHourRegex = Regex(
        pattern = """(?:через\s+)?пол\s*часа|half\s+an?\s+hour""",
        option = RegexOption.IGNORE_CASE
    )

    fun extract(rawText: String): SaunaTimerCommand? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        val normalized = text.lowercase(Locale.ROOT)
        val mentionsSauna = normalized.contains("саун") || normalized.contains("sauna")
        if (!mentionsSauna) return null

        val minutes = when {
            halfHourRegex.containsMatchIn(normalized) -> 30
            else -> {
                delayMinutesRegex.find(normalized)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                    ?: delayHoursRegex.find(normalized)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.toIntOrNull()
                        ?.times(60)
                    ?: return null
            }
        }

        if (minutes !in 1..720) return null

        return SaunaTimerCommand(minutes = minutes)
    }
}

