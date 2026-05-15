package com.proapps.voiceremind.sahko

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.ZoneId

object SahkoVahtiScheduler {

    fun schedule(
        context: Context,
        appliance: String,
        nightHourStart: Int,
        nightHourEndExclusive: Int
    ) {
        val input = Data.Builder()
            .putString(SahkoVahtiWorker.KEY_APPLIANCE, appliance)
            .putString(SahkoVahtiWorker.KEY_TIMEZONE, ZoneId.systemDefault().id)
            .putInt(SahkoVahtiWorker.KEY_NIGHT_HOUR_START, nightHourStart)
            .putInt(SahkoVahtiWorker.KEY_NIGHT_HOUR_END_EXCLUSIVE, nightHourEndExclusive)
            .build()

        val request = OneTimeWorkRequestBuilder<SahkoVahtiWorker>()
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sahko_vahti_${appliance.hashCode()}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

