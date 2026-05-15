package com.proapps.voiceremind.expenses

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExpenseLogStoreTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        val file = ExpenseLogStore.getLogFile(app)
        if (file.exists()) {
            file.delete()
        }
    }

    @Test
    fun calculateCurrentWeekSummary_sumsTotalAndRestaurants() {
        val zone = ZoneId.of("Europe/Helsinki")
        val now = ZonedDateTime.of(2026, 5, 15, 12, 0, 0, 0, zone)

        ExpenseLogStore.append(
            app,
            command = ExpenseLogCommand(15.0, "обед", ExpenseCategory.RESTAURANT),
            timestampEpochMillis = now.minusDays(1).toInstant().toEpochMilli()
        )
        ExpenseLogStore.append(
            app,
            command = ExpenseLogCommand(8.0, "bus", ExpenseCategory.TRANSPORT),
            timestampEpochMillis = now.minusDays(2).toInstant().toEpochMilli()
        )
        ExpenseLogStore.append(
            app,
            command = ExpenseLogCommand(99.0, "old", ExpenseCategory.RESTAURANT),
            timestampEpochMillis = now.minusDays(10).toInstant().toEpochMilli()
        )

        val summary = ExpenseLogStore.calculateCurrentWeekSummary(app, now)

        assertEquals(23.0, summary.totalEuro, 0.001)
        assertEquals(15.0, summary.restaurantsEuro, 0.001)
        assertEquals(2, summary.entriesCount)
    }
}

