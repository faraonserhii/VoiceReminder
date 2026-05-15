package com.proapps.voiceremind.waste

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WastePickupSchedulerTest {

    @Test
    fun calculateNextReminderTime_beforeReminder_usesUpcomingCollection() {
        val zone = ZoneId.of("Europe/Helsinki")
        val now = ZonedDateTime.of(2026, 5, 11, 9, 0, 0, 0, zone) // Monday

        val reminder = WastePickupScheduler.calculateNextReminderTime(
            now = now,
            pickupDayOfWeek = DayOfWeek.TUESDAY,
            intervalWeeks = 2,
            reminderHour = 20
        )

        assertEquals(DayOfWeek.MONDAY, reminder.dayOfWeek)
        assertEquals(20, reminder.hour)
        assertEquals(0, reminder.minute)
        assertTrue(reminder.isAfter(now))
    }

    @Test
    fun calculateNextReminderTime_afterReminder_shiftsByInterval() {
        val zone = ZoneId.of("Europe/Helsinki")
        val now = ZonedDateTime.of(2026, 5, 11, 22, 0, 0, 0, zone) // Monday late evening

        val reminder = WastePickupScheduler.calculateNextReminderTime(
            now = now,
            pickupDayOfWeek = DayOfWeek.TUESDAY,
            intervalWeeks = 2,
            reminderHour = 20
        )

        assertEquals(now.plusWeeks(2).toLocalDate(), reminder.toLocalDate())
        assertEquals(20, reminder.hour)
    }
}

