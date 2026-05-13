package com.proapps.voiceremind

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreGeoReminderMatcherTest {

    @Test
    fun isStoreRelated_detectsConfiguredKeywords() {
        assertTrue(StoreGeoReminderMatcher.isStoreRelated("Зайти в магазин", null))
        assertTrue(StoreGeoReminderMatcher.isStoreRelated("Muista pullo", "Prisma Itis"))
        assertTrue(StoreGeoReminderMatcher.isStoreRelated("Купить сок", "K-Citymarket Jumbo"))
        assertTrue(StoreGeoReminderMatcher.isStoreRelated("Забежать в kcitymarket", null))
    }

    @Test
    fun isStoreRelated_ignoresUnrelatedText() {
        assertFalse(StoreGeoReminderMatcher.isStoreRelated("Позвонить врачу", "Terveystalo"))
    }

    @Test
    fun isStoreRelated_handlesMixedCaseAndBlankLocation() {
        assertTrue(StoreGeoReminderMatcher.isStoreRelated("ЗАЕХАТЬ В PRISMA", "   "))
        assertFalse(StoreGeoReminderMatcher.isStoreRelated("Купить хлеб", "   "))
    }

    @Test
    fun resolvePlaceName_prefersExplicitLocation() {
        val place = StoreGeoReminderMatcher.resolvePlaceName(
            title = "Заехать в Prisma",
            location = "Prisma Sello",
            fallbackCity = "Helsinki"
        )

        assertEquals("Prisma Sello", place)
    }

    @Test
    fun resolvePlaceName_trimsExplicitLocation() {
        val place = StoreGeoReminderMatcher.resolvePlaceName(
            title = "Зайти в магазин",
            location = "  K-Citymarket Myyrmanni  ",
            fallbackCity = "Vantaa"
        )

        assertEquals("K-Citymarket Myyrmanni", place)
    }

    @Test
    fun resolvePlaceName_buildsKnownChainsFromTitle() {
        val prisma = StoreGeoReminderMatcher.resolvePlaceName(
            title = "Зайти в prisma",
            location = null,
            fallbackCity = "Espoo"
        )
        val citymarket = StoreGeoReminderMatcher.resolvePlaceName(
            title = "Muista pullo в k citymarket",
            location = "",
            fallbackCity = "Vantaa"
        )
        val compactCitymarket = StoreGeoReminderMatcher.resolvePlaceName(
            title = "Muista pullo в kcitymarket",
            location = null,
            fallbackCity = "Helsinki"
        )

        assertEquals("Prisma Espoo", prisma)
        assertEquals("K-Citymarket Vantaa", citymarket)
        assertEquals("K-Citymarket Helsinki", compactCitymarket)
    }

    @Test
    fun resolvePlaceName_usesFallbackForGenericStore() {
        val place = StoreGeoReminderMatcher.resolvePlaceName(
            title = "После работы в магазин",
            location = null,
            fallbackCity = "Helsinki"
        )

        assertEquals("Helsinki", place)
    }

    @Test
    fun resolvePlaceName_returnsNullWhenNoStoreSignals() {
        val place = StoreGeoReminderMatcher.resolvePlaceName(
            title = "Позвонить врачу",
            location = null,
            fallbackCity = "Helsinki"
        )

        assertEquals(null, place)
    }
}

