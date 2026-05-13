package com.proapps.voiceremind.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object StoreGeofenceManager {

    const val EXTRA_PLACE_NAME = "extra_place_name"
    private const val GEOFENCE_RADIUS_METERS = 250f

    fun hasRequiredLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fine) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true

        val background = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return background
    }

    @Suppress("MissingPermission")
    fun registerStoreEnterGeofence(
        context: Context,
        requestId: String,
        latitude: Double,
        longitude: Double,
        placeName: String
    ) {
        val geofence = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(latitude, longitude, GEOFENCE_RADIUS_METERS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
        geofencingClient.addGeofences(request, pendingIntent(context, placeName))
    }

    private fun pendingIntent(context: Context, placeName: String): PendingIntent {
        val intent = Intent(context, StoreGeofenceBroadcastReceiver::class.java).apply {
            putExtra(EXTRA_PLACE_NAME, placeName)
        }
        return PendingIntent.getBroadcast(
            context,
            placeName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

