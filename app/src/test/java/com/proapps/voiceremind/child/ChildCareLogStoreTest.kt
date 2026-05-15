package com.proapps.voiceremind.child

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChildCareLogStoreTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        val file = ChildCareLogStore.getLogFile(app)
        if (file.exists()) {
            file.delete()
        }
    }

    @Test
    fun findLatestByType_returnsNewestEntryForType() {
        ChildCareLogStore.append(app, ChildEventType.MEAL, 1000L, "meal1")
        ChildCareLogStore.append(app, ChildEventType.VITAMINS, 2000L, "vit")
        ChildCareLogStore.append(app, ChildEventType.MEAL, 3000L, "meal2")

        val entry = ChildCareLogStore.findLatestByType(app, ChildEventType.MEAL)

        assertNotNull(entry)
        assertEquals(3000L, entry?.timestampEpochMillis)
        assertEquals("meal2", entry?.note)
    }
}

