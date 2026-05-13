package com.proapps.voiceremind

import java.util.Locale

object StoreGeoReminderMatcher {

    private val storeKeywords = listOf(
        "магазин",
        "prisma",
        "k-citymarket",
        "k citymarket",
        "kcitymarket"
    )

    fun isStoreRelated(title: String, location: String?): Boolean {
        val source = "$title ${location.orEmpty()}".lowercase(Locale.ROOT)
        return storeKeywords.any { source.contains(it) }
    }

    fun resolvePlaceName(title: String, location: String?, fallbackCity: String): String? {
        val normalizedLocation = location.orEmpty().trim()
        if (normalizedLocation.isNotBlank()) return normalizedLocation

        val normalizedTitle = title.lowercase(Locale.ROOT)
        return when {
            normalizedTitle.contains("prisma") -> "Prisma $fallbackCity"
            normalizedTitle.contains("k-citymarket") ||
                normalizedTitle.contains("k citymarket") ||
                normalizedTitle.contains("kcitymarket") -> "K-Citymarket $fallbackCity"
            normalizedTitle.contains("магазин") -> fallbackCity
            else -> null
        }
    }
}

