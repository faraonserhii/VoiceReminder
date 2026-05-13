package com.proapps.voiceremind

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReminderParserFinnishLocationTest {

    private val parser = ReminderParser(
        nowProvider = { LocalDateTime.of(2026, 5, 4, 10, 0) }
    )

    @Test
    fun parse_finnishInessiveLocation_normalizesToBaseForm() {
        val parsed = parser.parse("tavataan 15/04 19:30 helsingissa")

        assertNotNull(parsed)
        assertEquals("Helsinki", parsed?.location)
    }

    @Test
    fun parse_finnishInessiveWithUmlaut_normalizesToBaseForm() {
        val parsed = parser.parse("tavataan 15/04 19:30 Helsingissä")

        assertNotNull(parsed)
        assertEquals("Helsinki", parsed?.location)
    }

    @Test
    fun parse_espooCase_normalizesToBaseForm() {
        val parsed = parser.parse("tavataan 15/04 19:30 Espoossa")

        assertNotNull(parsed)
        assertEquals("Espoo", parsed?.location)
    }

    @Test
    fun parse_vantaaCase_normalizesToBaseForm() {
        val parsed = parser.parse("tavataan 15/04 19:30 Vantaalla")

        assertNotNull(parsed)
        assertEquals("Vantaa", parsed?.location)
    }

    @Test
    fun parse_tampereCase_normalizesToBaseForm() {
        val parsed = parser.parse("tavataan 15/04 19:30 Tampereelta")

        assertNotNull(parsed)
        assertEquals("Tampere", parsed?.location)
    }

    @Test
    fun parse_turkuCase_normalizesToBaseForm() {
        val parsed = parser.parse("tavataan 15/04 19:30 Turusta")

        assertNotNull(parsed)
        assertEquals("Turku", parsed?.location)
    }
}

