package com.proapps.voiceremind.sahko

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proapps.voiceremind.R

class SahkoVahtiReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val appliance = inputData.getString(KEY_APPLIANCE).orEmpty().ifBlank {
            applicationContext.getString(R.string.sahko_default_appliance)
        }
        val price = inputData.getDouble(KEY_PRICE_CENTS, Double.NaN)

        val body = if (price.isNaN()) {
            applicationContext.getString(R.string.sahko_notification_body_generic, appliance)
        } else {
            applicationContext.getString(R.string.sahko_notification_body, appliance, String.format("%.2f", price))
        }

        SahkoVahtiNotifier.ensureChannel(applicationContext)
        SahkoVahtiNotifier.show(
            context = applicationContext,
            title = applicationContext.getString(R.string.sahko_notification_title),
            body = body,
            notificationId = appliance.hashCode().coerceAtLeast(1)
        )

        return Result.success()
    }

    companion object {
        const val KEY_APPLIANCE = "key_appliance"
        const val KEY_PRICE_CENTS = "key_price_cents"
    }
}

