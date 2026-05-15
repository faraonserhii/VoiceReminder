package com.proapps.voiceremind.waste

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proapps.voiceremind.R

class WastePickupReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val material = inputData.getString(KEY_MATERIAL_LABEL)
            .orEmpty()
            .ifBlank { applicationContext.getString(R.string.waste_default_material) }

        WastePickupNotifier.ensureChannel(applicationContext)
        WastePickupNotifier.show(
            context = applicationContext,
            title = applicationContext.getString(R.string.waste_notification_title),
            body = applicationContext.getString(R.string.waste_notification_body, material),
            notificationId = material.hashCode().coerceAtLeast(1)
        )
        return Result.success()
    }

    companion object {
        const val KEY_MATERIAL_LABEL = "key_material_label"
    }
}

