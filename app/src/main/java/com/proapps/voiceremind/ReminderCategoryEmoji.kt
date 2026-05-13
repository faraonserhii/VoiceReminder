package com.proapps.voiceremind

import java.util.Locale

object ReminderCategoryEmoji {

    private const val SHOP = "🛒"
    private const val SAUNA = "🧖‍♂️"
    private const val WORK = "💼"

    private val knownPrefixes = listOf(SHOP, SAUNA, WORK)

    fun apply(title: String): String {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return title
        if (knownPrefixes.any { trimmed.startsWith("$it ") }) return trimmed

        val lower = trimmed.lowercase(Locale.ROOT)
        val emoji = when {
            containsAny(lower, listOf("магазин", "shop", "store", "kauppa", "market")) -> SHOP
            containsAny(lower, listOf("сауна", "sauna")) -> SAUNA
            containsAny(lower, listOf("работ", "work", "office", "job", "tyo", "työ")) -> WORK
            else -> null
        }

        return if (emoji == null) trimmed else "$emoji $trimmed"
    }

    private fun containsAny(text: String, words: List<String>): Boolean {
        return words.any { text.contains(it) }
    }
}

