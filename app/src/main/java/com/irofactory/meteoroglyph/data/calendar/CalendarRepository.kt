package com.irofactory.meteoroglyph.data.calendar

import android.content.Context
import android.provider.CalendarContract
import java.time.LocalDateTime
import java.time.ZoneId

class CalendarRepository(private val context: Context) {

    fun getTodayEvents(
        settings: com.irofactory.meteoroglyph.data.settings.AppSettings? = null
    ): List<CalendarEvent> {
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
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_COLOR
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND " +
                "${CalendarContract.Events.DTSTART} <= ?"
        val selArgs   = arrayOf(startMs.toString(), endMs.toString())

        try {
            context.contentResolver.query(uri, projection, selection, selArgs, null)
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val title  = cursor.getString(0) ?: continue
                        val start  = cursor.getLong(1)
                        val end    = cursor.getLong(2)
                        val color  = if (cursor.isNull(3)) null else cursor.getInt(3)

                        events.add(CalendarEvent(
                            title         = title,
                            startTime     = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(start), ZoneId.systemDefault()),
                            endTime       = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(end), ZoneId.systemDefault()),
                            isOutsideHome = resolveLocation(title, color, settings)
                        ))
                    }
                }
        } catch (e: SecurityException) { }

        return events.sortedBy { it.startTime }
    }

    fun getNextEvent(
        settings: com.irofactory.meteoroglyph.data.settings.AppSettings? = null
    ): CalendarEvent? {
        val now = LocalDateTime.now()
        return getTodayEvents(settings)
            .filter { it.startTime.isAfter(now) }
            .minByOrNull { it.startTime }
    }

    private fun resolveLocation(
        title: String,
        colorArgb: Int?,
        settings: com.irofactory.meteoroglyph.data.settings.AppSettings?
    ): Boolean {
        // 1. Color morado de Google Calendar → evento social, sale de casa
        if (colorArgb != null) {
            val r = (colorArgb shr 16) and 0xFF
            val g = (colorArgb shr 8)  and 0xFF
            val b =  colorArgb         and 0xFF
            // Grape #8E24AA y Lavender #9E69AF
            if (r in 120..180 && b in 150..200 && g < 130) return true
        }

        // 2. Keywords configuradas por el usuario
        if (settings != null) {
            val titleLower = title.lowercase()
            settings.calendarKeywords.forEach { kw ->
                if (titleLower.contains(kw.keyword.lowercase())) return true
            }
        }

        // 3. Heurística de fallback
        return isOutsideHome(title)
    }

    private fun isOutsideHome(title: String): Boolean {
        val keywords = listOf(
            "trabajo", "work", "oficina", "office", "gym", "gimnasio",
            "doctor", "médico", "cita", "appointment", "escuela", "school",
            "universidad", "clase", "class", "reunión", "meeting"
        )
        return keywords.any { title.lowercase().contains(it) }
    }
}