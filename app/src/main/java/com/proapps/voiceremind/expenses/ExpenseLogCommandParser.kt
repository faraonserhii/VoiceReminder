package com.proapps.voiceremind.expenses

import java.util.Locale

enum class ExpenseCategory {
    RESTAURANT,
    GROCERIES,
    TRANSPORT,
    OTHER
}

data class ExpenseLogCommand(
    val amountEuro: Double,
    val note: String,
    val category: ExpenseCategory
)

object ExpenseLogCommandParser {

    private val amountRegex = Regex(
        pattern = """(\d+(?:[.,]\d{1,2})?)\s*(?:€|евро|eur|euro)""",
        option = RegexOption.IGNORE_CASE
    )

    fun extract(rawText: String): ExpenseLogCommand? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        val normalized = text.lowercase(Locale.ROOT)
        val mentionsExpenseIntent = normalized.contains("потрат") ||
            normalized.contains("spent") ||
            normalized.contains("запиши") ||
            normalized.contains("record")
        if (!mentionsExpenseIntent) return null

        val amountMatch = amountRegex.find(text) ?: return null
        val amount = amountMatch.groupValues[1]
            .replace(',', '.')
            .toDoubleOrNull()
            ?.takeIf { it > 0.0 }
            ?: return null

        val tail = text.substring(amountMatch.range.last + 1)
            .trim()
            .removePrefix("на ")
            .removePrefix("for ")
            .trim(' ', ',', '.', ':', ';', '-', '!')
        val note = tail.ifBlank { "expense" }

        return ExpenseLogCommand(
            amountEuro = amount,
            note = note,
            category = detectCategory("$normalized ${note.lowercase(Locale.ROOT)}")
        )
    }

    private fun detectCategory(normalized: String): ExpenseCategory {
        return when {
            normalized.contains("обед") ||
                normalized.contains("ужин") ||
                normalized.contains("кафе") ||
                normalized.contains("ресторан") ||
                normalized.contains("lunch") ||
                normalized.contains("dinner") ||
                normalized.contains("restaurant") ||
                normalized.contains("cafe") -> ExpenseCategory.RESTAURANT

            normalized.contains("продукт") ||
                normalized.contains("магазин") ||
                normalized.contains("grocery") ||
                normalized.contains("supermarket") -> ExpenseCategory.GROCERIES

            normalized.contains("автобус") ||
                normalized.contains("метро") ||
                normalized.contains("такси") ||
                normalized.contains("transport") ||
                normalized.contains("bus") ||
                normalized.contains("taxi") -> ExpenseCategory.TRANSPORT

            else -> ExpenseCategory.OTHER
        }
    }
}

