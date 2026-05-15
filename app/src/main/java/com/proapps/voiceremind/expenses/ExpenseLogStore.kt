package com.proapps.voiceremind.expenses

import android.content.Context
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

data class ExpenseLogEntry(
    val timestampEpochMillis: Long,
    val amountEuro: Double,
    val note: String,
    val category: ExpenseCategory
)

data class ExpenseWeekSummary(
    val totalEuro: Double,
    val restaurantsEuro: Double,
    val entriesCount: Int
)

object ExpenseLogStore {

    private const val LOG_FILE = "expense_log.csv"

    fun getLogFile(context: Context): File {
        return File(context.filesDir, LOG_FILE)
    }

    fun append(context: Context, command: ExpenseLogCommand, timestampEpochMillis: Long = System.currentTimeMillis()) {
        val safeNote = command.note.replace(',', ' ')
        val line = "$timestampEpochMillis,${command.amountEuro},${command.category.name},$safeNote\n"

        val file = getLogFile(context)
        if (!file.exists()) {
            file.writeText("timestamp_epoch,amount_eur,category,note\n")
        }
        file.appendText(line)
    }

    fun readAll(context: Context): List<ExpenseLogEntry> {
        val file = getLogFile(context)
        if (!file.exists()) return emptyList()

        return file.useLines { lines ->
            lines.drop(1)
                .filter { it.isNotBlank() }
                .mapNotNull { parseCsvLine(it) }
                .toList()
        }
    }

    fun calculateCurrentWeekSummary(
        context: Context,
        now: ZonedDateTime = ZonedDateTime.now()
    ): ExpenseWeekSummary {
        val zoneId = now.zone
        val weekStart = now
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toLocalDate()
            .atStartOfDay(zoneId)
        val weekEndExclusive = weekStart.plusDays(7)

        val weekEntries = readAll(context).filter { entry ->
            val ts = Instant.ofEpochMilli(entry.timestampEpochMillis).atZone(zoneId)
            !ts.isBefore(weekStart) && ts.isBefore(weekEndExclusive)
        }

        val total = weekEntries.sumOf { it.amountEuro }
        val restaurants = weekEntries
            .filter { it.category == ExpenseCategory.RESTAURANT }
            .sumOf { it.amountEuro }

        return ExpenseWeekSummary(
            totalEuro = total,
            restaurantsEuro = restaurants,
            entriesCount = weekEntries.size
        )
    }

    private fun parseCsvLine(line: String): ExpenseLogEntry? {
        val parts = line.split(',', limit = 4)
        if (parts.size < 4) return null

        val ts = parts[0].trim().toLongOrNull() ?: return null
        val amount = parts[1].trim().toDoubleOrNull() ?: return null
        val category = runCatching { ExpenseCategory.valueOf(parts[2].trim()) }.getOrNull() ?: ExpenseCategory.OTHER
        val note = parts[3].trim().ifBlank { "expense" }

        return ExpenseLogEntry(
            timestampEpochMillis = ts,
            amountEuro = amount,
            note = note,
            category = category
        )
    }
}

