package com.proapps.voiceremind.geofence

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class StoreGeofenceManagerPermissionTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun hasRequiredLocationPermission_returnsFalseWhenLocationNotGranted() {
        assertFalse(StoreGeofenceManager.hasRequiredLocationPermission(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun hasRequiredLocationPermission_preQ_returnsTrueWithFineOnly() {
        grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        assertTrue(StoreGeofenceManager.hasRequiredLocationPermission(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun hasRequiredLocationPermission_q_returnsFalseWithoutBackground() {
        grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        assertFalse(StoreGeofenceManager.hasRequiredLocationPermission(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun hasRequiredLocationPermission_q_returnsTrueWithFineAndBackground() {
        grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        assertTrue(StoreGeofenceManager.hasRequiredLocationPermission(context))
    }

    private fun grantPermissions(vararg permissions: String) {
        shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
            .grantPermissions(*permissions)
    }
}

