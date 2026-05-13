package com.proapps.voiceremind.geofence

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.location.Geofence
import com.proapps.voiceremind.R
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class StoreGeofenceBroadcastReceiverIntegrationTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()
    }

    @After
    fun tearDown() {
        StoreGeofenceBroadcastReceiver.resetEventExtractorForTests()
    }

    @Test
    fun handleEnterTransition_showsNotificationWithPlaceName() {
        val receiver = StoreGeofenceBroadcastReceiver()

        receiver.handleEnterTransition(context, "Prisma Itis")

        val posted = shadowOf(notificationManager).allNotifications
        assertEquals(1, posted.size)
        assertNotificationText(
            notification = posted.first(),
            title = context.getString(R.string.store_geo_notification_title),
            body = context.getString(R.string.store_geo_notification_body, "Prisma Itis")
        )
    }

    @Test
    fun handleEnterTransition_usesGenericPlaceForBlankName() {
        val receiver = StoreGeofenceBroadcastReceiver()

        receiver.handleEnterTransition(context, "   ")

        val posted = shadowOf(notificationManager).allNotifications
        assertEquals(1, posted.size)
        val genericPlace = context.getString(R.string.store_geo_generic_place)
        assertNotificationText(
            notification = posted.first(),
            title = context.getString(R.string.store_geo_notification_title),
            body = context.getString(R.string.store_geo_notification_body, genericPlace)
        )
    }

    @Test
    fun handleTransition_nonEnter_doesNotShowNotification() {
        val receiver = StoreGeofenceBroadcastReceiver()

        receiver.handleTransition(
            context = context,
            transition = Geofence.GEOFENCE_TRANSITION_EXIT,
            rawPlaceName = "Prisma Itis"
        )

        val posted = shadowOf(notificationManager).allNotifications
        assertEquals(0, posted.size)
    }

    @Test
    fun onReceive_eventHasError_doesNotShowNotification() {
        val receiver = StoreGeofenceBroadcastReceiver()
        StoreGeofenceBroadcastReceiver.eventExtractor = {
            StoreGeofenceBroadcastReceiver.ParsedGeofenceEvent(
                hasError = true,
                transition = Geofence.GEOFENCE_TRANSITION_ENTER
            )
        }

        receiver.onReceive(context, Intent().apply {
            putExtra(StoreGeofenceManager.EXTRA_PLACE_NAME, "Prisma Itis")
        })

        val posted = shadowOf(notificationManager).allNotifications
        assertEquals(0, posted.size)
    }

    @Test
    fun onReceive_eventIsNull_doesNotShowNotification() {
        val receiver = StoreGeofenceBroadcastReceiver()
        StoreGeofenceBroadcastReceiver.eventExtractor = { null }

        receiver.onReceive(context, Intent().apply {
            putExtra(StoreGeofenceManager.EXTRA_PLACE_NAME, "Prisma Itis")
        })

        val posted = shadowOf(notificationManager).allNotifications
        assertEquals(0, posted.size)
    }

    private fun assertNotificationText(notification: Notification, title: String, body: String) {
        assertEquals(title, notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(body, notification.extras.getString(Notification.EXTRA_TEXT))
    }
}

