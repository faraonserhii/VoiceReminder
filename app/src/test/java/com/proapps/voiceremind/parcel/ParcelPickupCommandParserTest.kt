package com.proapps.voiceremind.parcel

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ParcelPickupCommandParserTest {

    @Test
    fun extract_ruCommand_parsesItemPlaceAndDeadlineWeekday() {
        val now = LocalDateTime.of(2026, 5, 15, 10, 0) // Friday

        val command = ParcelPickupCommandParser.extract(
            rawText = "Напомни забрать кроссовки из Posti до четверга",
            now = now
        )

        assertNotNull(command)
        val parsed = command!!
        assertEquals("кроссовки", parsed.itemLabel)
        assertEquals("Posti", parsed.pickupPlace)
        assertEquals(DayOfWeek.THURSDAY, parsed.deadlineAt.dayOfWeek)
        assertEquals(LocalDate.of(2026, 5, 21), parsed.deadlineAt.toLocalDate())
    }

    @Test
    fun extract_withoutDeadline_usesFallbackDate() {
        val now = LocalDateTime.of(2026, 5, 15, 10, 0)

        val command = ParcelPickupCommandParser.extract(
            rawText = "Напомни забрать посылку из Matkahuolto",
            now = now
        )

        assertNotNull(command)
        assertEquals(LocalDate.of(2026, 5, 18), command!!.deadlineAt.toLocalDate())
    }

    @Test
    fun extract_nonParcelPhrase_returnsNull() {
        val command = ParcelPickupCommandParser.extract("Напомни купить молоко")

        assertNull(command)
    }
}

