package com.proapps.voiceremind.medication

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MedicationLogShareHelperTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        val logFile = MedicationLogStore.getLogFile(app)
        if (logFile.exists()) {
            logFile.delete()
        }
    }

    @Test
    fun buildShareIntent_noEntries_returnsNull() {
        val shareIntent = MedicationLogShareHelper.buildShareIntent(app)

        assertNull(shareIntent)
    }

    @Test
    fun buildShareIntent_withEntries_returnsSendIntent() {
        MedicationLogStore.append(app, "log-1", "Витамины")

        val shareIntent = MedicationLogShareHelper.buildShareIntent(app)

        assertNotNull(shareIntent)
        assertEquals(Intent.ACTION_SEND, shareIntent?.action)
        assertEquals("text/csv", shareIntent?.type)
        assertNotNull(shareIntent?.getParcelableExtra(Intent.EXTRA_STREAM))
    }
}

