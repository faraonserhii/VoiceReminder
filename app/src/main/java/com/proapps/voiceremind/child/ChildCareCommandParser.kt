package com.proapps.voiceremind.child

import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

enum class ChildEventType {
    MEAL,
    VITAMINS,
    OTHER
}

sealed class ChildCareCommand {
    data class LogEvent(
        val eventType: ChildEventType,
        val happenedAt: LocalDateTime,
        val note: String
    ) : ChildCareCommand()

    data class QueryLastEvent(
        val eventType: ChildEventType
    ) : ChildCareCommand()
}

object ChildCareCommandParser {

    private val timeRegex = Regex("""(?:^|\s)в\s*(\d{1,2})[:.](\d{2})(?:\s|$)""", RegexOption.IGNORE_CASE)

    fun extract(rawText: String, now: LocalDateTime = LocalDateTime.now()): ChildCareCommand? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        val normalized = text.lowercase(Locale.ROOT)
        val eventType = detectEventType(normalized)

        if (isQuery(normalized)) {
            return ChildCareCommand.QueryLastEvent(eventType)
        }

        if (!isLogIntent(normalized)) {
            return null
        }

        val happenedAt = parseTime(text, now) ?: now
        val note = text
            .removePrefix("Запиши:")
            .removePrefix("запиши:")
            .trim()
            .ifBlank { defaultNote(eventType) }

        return ChildCareCommand.LogEvent(
            eventType = eventType,
            happenedAt = happenedAt,
            note = note
        )
    }

    private fun parseTime(text: String, now: LocalDateTime): LocalDateTime? {
        val match = timeRegex.find(text) ?: return null
        val hour = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minute = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour, minute))
    }

    private fun isQuery(normalized: String): Boolean {
        return normalized.contains("когда") || normalized.contains("when")
    }

    private fun isLogIntent(normalized: String): Boolean {
        return normalized.contains("запиши") ||
            normalized.contains("ребен") ||
            normalized.contains("ребён") ||
            normalized.contains("витамин") ||
            normalized.contains("child")
    }

    private fun detectEventType(normalized: String): ChildEventType {
        return when {
            normalized.contains("ел") ||
                normalized.contains("поел") ||
                normalized.contains("покорм") ||
                normalized.contains("ate") ||
                normalized.contains("meal") ||
                normalized.contains("fed") -> ChildEventType.MEAL

            normalized.contains("витамин") ||
                normalized.contains("vitamin") -> ChildEventType.VITAMINS

            else -> ChildEventType.OTHER
        }
    }

    private fun defaultNote(type: ChildEventType): String {
        return when (type) {
            ChildEventType.MEAL -> "Ребенок поел"
            ChildEventType.VITAMINS -> "Витамины дала"
            ChildEventType.OTHER -> "Событие ребенка"
        }
    }
}

