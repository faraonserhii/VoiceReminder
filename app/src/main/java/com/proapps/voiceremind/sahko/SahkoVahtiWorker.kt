package com.proapps.voiceremind.sahko

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.proapps.voiceremind.R
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class SahkoVahtiWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appliance = inputData.getString(KEY_APPLIANCE).orEmpty().ifBlank { "стиралку" }
        val timezoneId = inputData.getString(KEY_TIMEZONE).orEmpty().ifBlank { ZoneId.systemDefault().id }
        val nightStart = inputData.getInt(KEY_NIGHT_HOUR_START, 22).coerceIn(0, 23)
        val nightEnd = inputData.getInt(KEY_NIGHT_HOUR_END_EXCLUSIVE, 7).coerceIn(0, 23)

        val prices = NordPoolCompatibleClient().loadUpcomingFiPrices()
        if (prices.isNullOrEmpty()) {
            showUnavailablePricesNotification(appliance)
            return Result.success()
        }

        val now = System.currentTimeMillis()
        val bestSlot = ElectricityPriceSelector.findCheapestNightSlot(
            nowEpochMillis = now,
            timezoneId = timezoneId,
            prices = prices,
            nightHourStart = nightStart,
            nightHourEndExclusive = nightEnd
        ) ?: run {
            showUnavailablePricesNotification(appliance)
            return Result.success()
        }

        val delay = (bestSlot.startEpochMillis - now).coerceAtLeast(0L)
        val reminderInput = Data.Builder()
            .putString(SahkoVahtiReminderWorker.KEY_APPLIANCE, appliance)
            .putDouble(SahkoVahtiReminderWorker.KEY_PRICE_CENTS, bestSlot.priceCentsPerKwh)
            .build()

        val request = OneTimeWorkRequestBuilder<SahkoVahtiReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(reminderInput)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(request)
        return Result.success()
    }

    private fun showUnavailablePricesNotification(appliance: String) {
        SahkoVahtiNotifier.ensureChannel(applicationContext)
        SahkoVahtiNotifier.show(
            context = applicationContext,
            title = applicationContext.getString(R.string.sahko_notification_title),
            body = applicationContext.getString(R.string.sahko_unavailable_prices_body, appliance),
            notificationId = appliance.hashCode().coerceAtLeast(1)
        )
    }

    companion object {
        const val KEY_APPLIANCE = "key_appliance"
        const val KEY_TIMEZONE = "key_timezone"
        const val KEY_NIGHT_HOUR_START = "key_night_hour_start"
        const val KEY_NIGHT_HOUR_END_EXCLUSIVE = "key_night_hour_end_exclusive"
    }
}

