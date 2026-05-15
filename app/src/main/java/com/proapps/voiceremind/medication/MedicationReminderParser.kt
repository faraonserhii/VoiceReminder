package com.proapps.voiceremind.medication

import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

data class MedicationPlan(
    val title: String,
    val firstIntakeDateTime: LocalDateTime,
    val daysCount: Int?,
    val usedDefaultTime: Boolean
)

object MedicationReminderParser {

    private val ruTimeRegex = Regex("""(?:^|\s)в\s+(\d{1,2})(?::(\d{2}))?(?=\s|,|$)""", RegexOption.IGNORE_CASE)
    private val enTimeRegex = Regex("""(?:^|\s)at\s+(\d{1,2})(?::(\d{2}))?(?:\s*(am|pm))?(?=\s|,|$)""", RegexOption.IGNORE_CASE)
    private val ruCourseDaysRegex = Regex("""(?:курс\s+)?[\p{L}\s-]*?на\s*(\d{1,3})\s*дн(?:я|ей|ь)?""", RegexOption.IGNORE_CASE)
    private val enCourseDaysRegex = Regex("""(?:course\s+of\s+)?[a-z\s-]*?for\s*(\d{1,3})\s*days?""", RegexOption.IGNORE_CASE)

    fun extract(
        rawText: String,
        now: LocalDateTime,
        defaultTime: LocalTime
    ): MedicationPlan? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        val normalized = text.lowercase(Locale.ROOT)
        if (!isMedicationRelated(normalized)) return null

        val explicitTime = parseTime(text)
        val hasMorningPhrase = normalized.contains("каждое утро") || normalized.contains("every morning")
        val hasDailyPhrase = hasMorningPhrase || normalized.contains("каждый день") || normalized.contains("every day")
        val daysCount = ruCourseDaysRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: enCourseDaysRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()

        if (!hasDailyPhrase && daysCount == null) return null

        val selectedTime = explicitTime
            ?: if (hasMorningPhrase) LocalTime.of(8, 0) else defaultTime

        var firstIntake = LocalDateTime.of(now.toLocalDate(), selectedTime)
        if (firstIntake.isBefore(now.plusMinutes(1))) {
            firstIntake = firstIntake.plusDays(1)
        }

        return MedicationPlan(
            title = buildTitle(normalized),
            firstIntakeDateTime = firstIntake,
            daysCount = daysCount,
            usedDefaultTime = explicitTime == null
        )
    }

    fun toDailyRRule(daysCount: Int?): String {
        return if (daysCount != null && daysCount > 0) {
            "FREQ=DAILY;COUNT=$daysCount"
        } else {
            "FREQ=DAILY"
        }
    }

    private fun isMedicationRelated(text: String): Boolean {
        val keywords = listOf(
            "витамин", "витамины", "антибиотик", "лекар", "таблет",
            "medicine", "medication", "antibiotic", "vitamin", "pill"
        )
        return keywords.any { text.contains(it) }
    }

    private fun buildTitle(normalized: String): String {
        return when {
            normalized.contains("антибиот") || normalized.contains("antibiotic") -> "Курс антибиотиков"
            normalized.contains("витамин") || normalized.contains("vitamin") -> "Пить витамины"
            else -> "Прием лекарства"
        }
    }

    private fun parseTime(text: String): LocalTime? {
        val match = ruTimeRegex.find(text) ?: enTimeRegex.find(text) ?: return null
        val rawHour = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minute = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        if (minute !in 0..59) return null

        val amPm = match.groupValues.getOrNull(3).orEmpty().lowercase(Locale.ROOT)
        val hour = when (amPm) {
            "am" -> if (rawHour == 12) 0 else rawHour
            "pm" -> if (rawHour == 12) 12 else rawHour + 12
            else -> rawHour
        }
        if (hour !in 0..23) return null

        return LocalTime.of(hour, minute)
    }
}

