package com.irofactory.meteoroglyph.data.calendar

data class CalendarEvent(
    val title: String,
    val startTime: java.time.LocalDateTime,
    val endTime: java.time.LocalDateTime,
    val isOutsideHome: Boolean   // lo determinaremos por ubicación o keywords
)