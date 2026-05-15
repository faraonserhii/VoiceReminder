package com.proapps.voiceremind.parcel

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class ParcelPickupCommand(
    val itemLabel: String,
    val pickupPlace: String,
    val deadlineAt: LocalDateTime
)

object ParcelPickupCommandParser {

    fun extract(rawText: String, now: LocalDateTime = LocalDateTime.now()): ParcelPickupCommand? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        val normalized = text.lowercase(Locale.ROOT)
        val commandPrefix = "напомни забрать "
        val start = normalized.indexOf(commandPrefix)
        if (start < 0) return null

        val afterPrefixStart = start + commandPrefix.length
        val fromIndex = normalized.indexOf(" из ", startIndex = afterPrefixStart)
        if (fromIndex < 0) return null

        val itemLabel = text.substring(afterPrefixStart, fromIndex)
            .trim(' ', ',', '.', ':', ';', '-', '!', '?')
            .ifBlank { "посылку" }

        val afterFromStart = fromIndex + " из ".length
        val byIndex = normalized.indexOf(" до ", startIndex = afterFromStart)

        val pickupPlace = if (byIndex < 0) {
            text.substring(afterFromStart)
        } else {
            text.substring(afterFromStart, byIndex)
        }
            .trim(' ', ',', '.', ':', ';', '-', '!', '?')
            .ifBlank { "Posti" }

        val deadlineToken = if (byIndex < 0) {
            ""
        } else {
            text.substring(byIndex + " до ".length).trim()
        }

        val deadlineAt = resolveDeadline(deadlineToken, now)

        return ParcelPickupCommand(
            itemLabel = itemLabel,
            pickupPlace = pickupPlace,
            deadlineAt = deadlineAt
        )
    }

    private fun resolveDeadline(token: String, now: LocalDateTime): LocalDateTime {
        val normalized = token.lowercase(Locale.ROOT)
        val dayOfWeek = when {
            normalized.contains("понедель") -> DayOfWeek.MONDAY
            normalized.contains("вторник") || normalized.contains("вторн") -> DayOfWeek.TUESDAY
            normalized.contains("сред") -> DayOfWeek.WEDNESDAY
            normalized.contains("четвер") -> DayOfWeek.THURSDAY
            normalized.contains("пятниц") -> DayOfWeek.FRIDAY
            normalized.contains("суббот") -> DayOfWeek.SATURDAY
            normalized.contains("воскрес") -> DayOfWeek.SUNDAY
            else -> null
        }

        val deadlineDate = when (dayOfWeek) {
            null -> now.toLocalDate().plusDays(3)
            else -> now.toLocalDate().with(TemporalAdjusters.nextOrSame(dayOfWeek))
        }

        return LocalDateTime.of(deadlineDate, LocalTime.of(23, 59))
    }
}

