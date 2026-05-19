package com.irofactory.meteoroglyph.data.weather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ── API de Open-Meteo ─────────────────────────────────────────────────────────
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude")              lat: Double,
        @Query("longitude")             lon: Double,
        @Query("current")               current: String =
            "temperature_2m,relative_humidity_2m,is_day," +
                    "weather_code,wind_speed_10m,precipitation_probability",
        @Query("hourly")                hourly: String =
            "precipitation_probability,weather_code",
        @Query("timezone")              timezone: String = "auto",
        @Query("forecast_days")         days: Int = 1,
        @Query("wind_speed_unit")       windUnit: String = "kmh"
    ): OpenMeteoResponse
}

data class OpenMeteoResponse(
    val current: CurrentWeather,
    val hourly: HourlyWeather
)

data class CurrentWeather(
    val temperature_2m: Double,
    val relative_humidity_2m: Int,
    val is_day: Int,
    val weather_code: Int,
    val wind_speed_10m: Double,
    val precipitation_probability: Int
)

data class HourlyWeather(
    val time: List<String>,
    val precipitation_probability: List<Int>,
    val weather_code: List<Int>
)

// ── Repositorio ───────────────────────────────────────────────────────────────
class WeatherRepository {

    private val api = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenMeteoApi::class.java)

    // Zapopan por defecto — después vendrá de Settings
    suspend fun getWeather(
        lat: Double = 20.6597,
        lon: Double = -103.3496
    ): Result<WeatherState> = runCatching {
        val response = api.getForecast(lat = lat, lon = lon)
        val current  = response.current
        val isDay    = current.is_day == 1
        val temp     = current.temperature_2m.toInt()
        val condition = mapWmoCode(current.weather_code, isDay, temp, current.wind_speed_10m.toInt())
        val rainWindow = findRainWindow(response.hourly)

        WeatherState(
            tempCelsius      = temp,
            condition        = condition,
            rainProbability  = current.precipitation_probability,
            windSpeedKmh     = current.wind_speed_10m.toInt(),
            humidity         = current.relative_humidity_2m,
            rainWindow       = rainWindow,
            isDay            = isDay
        )
    }

    // ── Mapeo WMO → WeatherCondition ─────────────────────────────────────────
    private fun mapWmoCode(
        code: Int,
        isDay: Boolean,
        temp: Int,
        wind: Int
    ): WeatherCondition {
        if (temp >= 35) return WeatherCondition.HOT
        if (temp <= 8)  return WeatherCondition.COLD
        if (wind >= 40) return WeatherCondition.WIND

        return when (code) {
            0            -> if (isDay) WeatherCondition.SUNNY else WeatherCondition.CLEAR_NIGHT
            1            -> if (isDay) WeatherCondition.SUNNY else WeatherCondition.CLEAR_NIGHT
            2            -> if (isDay) WeatherCondition.PARTLY_CLOUDY else WeatherCondition.PARTLY_CLOUDY_NIGHT
            3            -> WeatherCondition.OVERCAST
            45, 48       -> WeatherCondition.FOG
            51, 53       -> WeatherCondition.DRIZZLE
            55           -> WeatherCondition.DRIZZLE
            61, 63       -> WeatherCondition.RAIN
            65           -> WeatherCondition.HEAVY_RAIN
            71, 73, 75   -> WeatherCondition.SNOW
            77           -> WeatherCondition.SNOW
            80, 81       -> WeatherCondition.RAIN
            82           -> WeatherCondition.HEAVY_RAIN
            85, 86       -> WeatherCondition.SNOW
            95           -> WeatherCondition.STORM
            96, 99       -> WeatherCondition.STORM
            in 56..57    -> WeatherCondition.SLEET
            in 66..67    -> WeatherCondition.SLEET
            else         -> if (isDay) WeatherCondition.PARTLY_CLOUDY
            else WeatherCondition.PARTLY_CLOUDY_NIGHT
        }
    }

    // ── Detectar ventana de lluvia en las próximas horas ──────────────────────
    private fun findRainWindow(hourly: HourlyWeather): String? {
        val now   = java.time.LocalDateTime.now()
        val pairs = hourly.time.zip(hourly.precipitation_probability)

        val rainyHours = pairs.filter { (timeStr, prob) ->
            val t = java.time.LocalDateTime.parse(
                timeStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            )
            prob >= 40 && t.isAfter(now) && t.isBefore(now.plusHours(12))
        }

        if (rainyHours.isEmpty()) return null

        val start = rainyHours.first().first.takeLast(5)  // "14:00"
        val end   = rainyHours.last().first.takeLast(5)   // "16:00"
        return if (start == end) start else "$start–$end"
    }
}