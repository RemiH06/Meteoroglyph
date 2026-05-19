package com.irofactory.meteoroglyph.data.calendar

import android.content.Context
import android.provider.CalendarContract
import java.time.LocalDateTime
import java.time.ZoneId

class CalendarRepository(private val context: Context) {

    fun getTodayEvents(): List<CalendarEvent> {
        val events   = mutableListOf<CalendarEvent>()
        val now      = LocalDateTime.now()
        val startMs  = now.withHour(0).withMinute(0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs    = now.withHour(23).withMinute(59)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val uri        = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )
        val selection  = "${CalendarContract.Events.DTSTART} >= ? AND " +
                "${CalendarContract.Events.DTSTART} <= ?"
        val selArgs    = arrayOf(startMs.toString(), endMs.toString())

        try {
            context.contentResolver.query(uri, projection, selection, selArgs, null)
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val title  = cursor.getString(0) ?: continue
                        val start  = cursor.getLong(1)
                        val end    = cursor.getLong(2)
                        events.add(CalendarEvent(
                            title          = title,
                            startTime      = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(start), ZoneId.systemDefault()),
                            endTime        = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(end), ZoneId.systemDefault()),
                            isOutsideHome  = isOutsideHome(title)
                        ))
                    }
                }
        } catch (e: SecurityException) {
            // Permiso no concedido todavía
        }

        return events.sortedBy { it.startTime }
    }

    fun getNextEvent(): CalendarEvent? {
        val now = LocalDateTime.now()
        return getTodayEvents()
            .filter { it.startTime.isAfter(now) }
            .minByOrNull { it.startTime }
    }

    // Heurística simple — después configurable en Settings
    private fun isOutsideHome(title: String): Boolean {
        val keywords = listOf(
            "trabajo", "work", "oficina", "office", "gym", "gimnasio",
            "doctor", "médico", "cita", "appointment", "escuela", "school",
            "universidad", "clase", "class", "reunión", "meeting"
        )
        return keywords.any { title.lowercase().contains(it) }
    }
}