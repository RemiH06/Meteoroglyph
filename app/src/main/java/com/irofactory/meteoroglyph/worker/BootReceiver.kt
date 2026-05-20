package com.irofactory.meteoroglyph.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.irofactory.meteoroglyph.data.settings.AppSettings
import com.irofactory.meteoroglyph.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        CoroutineScope(Dispatchers.IO).launch {
            val settings = SettingsRepository(context).settings.first()
            WeatherCheckWorker.schedule(
                context,
                settings.notificationHour,
                settings.notificationMinute
            )
        }
    }
}