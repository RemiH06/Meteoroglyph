package com.irofactory.meteoroglyph.data.weather

data class WeatherState(
    val tempCelsius: Int,
    val condition: WeatherCondition,
    val rainProbability: Int,        // 0–100
    val windSpeedKmh: Int,
    val humidity: Int,
    val rainWindow: String?,         // "14:00–16:00" si hay lluvia próxima
    val isDay: Boolean
)

enum class WeatherCondition {
    SUNNY, CLEAR_NIGHT,
    PARTLY_CLOUDY, PARTLY_CLOUDY_NIGHT,
    MOSTLY_CLOUDY, MOSTLY_CLOUDY_NIGHT,
    OVERCAST,
    DRIZZLE, RAIN, HEAVY_RAIN,
    STORM, WIND, FOG,
    SNOW, SLEET,
    HOT, COLD;

    // Mapea el WMO code de Open-Meteo al glyph name
    fun toGlyphName(): String = when (this) {
        SUNNY               -> "sunny"
        CLEAR_NIGHT         -> "clear_night"
        PARTLY_CLOUDY       -> "partly_cloudy"
        PARTLY_CLOUDY_NIGHT -> "partly_cloudy_night"
        MOSTLY_CLOUDY       -> "mostly_cloudy"
        MOSTLY_CLOUDY_NIGHT -> "mostly_cloudy_night"
        OVERCAST            -> "overcast"
        DRIZZLE             -> "drizzle"
        RAIN                -> "rain"
        HEAVY_RAIN          -> "heavy_rain"
        STORM               -> "storm"
        WIND                -> "wind"
        FOG                 -> "fog"
        SNOW                -> "snow"
        SLEET               -> "sleet"
        HOT                 -> "heat"
        COLD                -> "cold"
    }
}