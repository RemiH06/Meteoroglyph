package com.irofactory.meteoroglyph.worker

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.irofactory.meteoroglyph.data.calendar.CalendarRepository
import com.irofactory.meteoroglyph.data.settings.AppSettings
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * EventAlarmScheduler
 * ───────────────────────────────────────────────────────────────────────────
 * Programa una alarma exacta 1 hora antes de cada evento del día
 * que esté fuera de casa y ocurra en más de 1 hora desde ahora.
 */
object EventAlarmScheduler {

    private const val TAG = "EventAlarmScheduler"

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAll(context: Context, settings: AppSettings) {
        val alarmManager  = context.getSystemService(AlarmManager::class.java)
        val calendarRepo  = CalendarRepository(context)
        val now           = LocalDateTime.now()
        val events        = try { calendarRepo.getTodayEvents(settings) } catch (e: Exception) { emptyList() }

        // Cancelar alarmas previas del día
        cancelAll(context, events.map { it.title })

        events.forEach { event ->
            // Solo eventos fuera de casa y que empiecen en más de 1 hora
            if (!event.isOutsideHome) return@forEach
            val alertTime = event.startTime.minusHours(1)
            if (!alertTime.isAfter(now)) return@forEach

            val alertMs = alertTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            // Coordenadas del evento — por ahora usamos casa, después se puede
            // resolver a la ubicación del keyword
            val lat = settings.homeLat
            val lon = settings.homeLon

            val intent = Intent(context, EventAlarmReceiver::class.java).apply {
                putExtra(EventAlarmReceiver.EXTRA_EVENT_TITLE, event.title)
                putExtra(EventAlarmReceiver.EXTRA_EVENT_LAT,   lat)
                putExtra(EventAlarmReceiver.EXTRA_EVENT_LON,   lon)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                event.title.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alertMs,
                    pendingIntent
                )
                Log.d(TAG, "Alarma programada: '${event.title}' a las $alertTime")
            } catch (e: SecurityException) {
                Log.e(TAG, "Sin permiso para alarmas exactas: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al programar alarma: ${e.message}")
            }
        }

        Log.d(TAG, "Alarmas programadas para ${events.size} eventos")
    }

    fun cancelAll(context: Context, eventTitles: List<String>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        eventTitles.forEach { title ->
            val intent = Intent(context, EventAlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                title.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let { alarmManager.cancel(it) }
        }
    }
}