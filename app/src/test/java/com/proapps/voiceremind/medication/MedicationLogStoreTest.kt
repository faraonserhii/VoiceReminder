package com.proapps.voiceremind.medication

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MedicationLogStoreTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        val file = MedicationLogStore.getLogFile(app)
        if (file.exists()) {
            file.delete()
        }
    }

    @Test
    fun readLatestEntries_returnsNewestFirstAndAppliesLimit() {
        MedicationLogStore.append(app, "l1", "Витамины")
        MedicationLogStore.append(app, "l2", "Антибиотик")
        MedicationLogStore.append(app, "l3", "Омега-3")

        val entries = MedicationLogStore.readLatestEntries(app, limit = 2)

        assertEquals(2, entries.size)
        assertTrue(entries[0].contains("Омега-3"))
        assertTrue(entries[1].contains("Антибиотик"))
    }

    @Test
    fun hasEntries_falseForMissingOrHeaderOnlyFile() {
        assertEquals(false, MedicationLogStore.hasEntries(app))

        MedicationLogStore.getLogFile(app).writeText("timestamp,log_id,title\n")
        assertEquals(false, MedicationLogStore.hasEntries(app))
    }
}

