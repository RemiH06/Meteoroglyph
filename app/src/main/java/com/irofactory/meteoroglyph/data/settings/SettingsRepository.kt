package com.irofactory.meteoroglyph.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "meteoroglyph_settings")

class SettingsRepository(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val USE_GPS              = booleanPreferencesKey("use_gps")
        val HOME_LAT             = doublePreferencesKey("home_lat")
        val HOME_LON             = doublePreferencesKey("home_lon")
        val HOME_LABEL           = stringPreferencesKey("home_label")
        val WORKPLACES           = stringPreferencesKey("workplaces")
        val SCHOOLS              = stringPreferencesKey("schools")
        val CALENDAR_KEYWORDS    = stringPreferencesKey("calendar_keywords")
        val HEAT_THRESHOLD       = intPreferencesKey("heat_threshold")
        val COLD_THRESHOLD       = intPreferencesKey("cold_threshold")
        val RAIN_THRESHOLD       = intPreferencesKey("rain_threshold")
        val WIND_THRESHOLD       = intPreferencesKey("wind_threshold")
        val UBER_RAIN_THRESHOLD  = intPreferencesKey("uber_rain_threshold")
        val NOTIF_HOUR           = intPreferencesKey("notif_hour")
        val NOTIF_MINUTE         = intPreferencesKey("notif_minute")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            useGps           = prefs[Keys.USE_GPS]           ?: true,
            homeLat          = prefs[Keys.HOME_LAT]          ?: 20.6597,
            homeLon          = prefs[Keys.HOME_LON]          ?: -103.3496,
            homeLabel        = prefs[Keys.HOME_LABEL]        ?: "Casa",
            workplaces       = fromJson(prefs[Keys.WORKPLACES]),
            schools          = fromJson(prefs[Keys.SCHOOLS]),
            calendarKeywords = fromJson(prefs[Keys.CALENDAR_KEYWORDS]),
            heatThresholdC   = prefs[Keys.HEAT_THRESHOLD]    ?: 28,
            coldThresholdC   = prefs[Keys.COLD_THRESHOLD]    ?: 14,
            rainThresholdPct = prefs[Keys.RAIN_THRESHOLD]    ?: 40,
            windThresholdKmh = prefs[Keys.WIND_THRESHOLD]    ?: 35,
            uberRainThresholdPct = prefs[Keys.UBER_RAIN_THRESHOLD] ?: 60,
            notificationHour   = prefs[Keys.NOTIF_HOUR]     ?: 7,
            notificationMinute = prefs[Keys.NOTIF_MINUTE]   ?: 0
        )
    }

    suspend fun save(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_GPS]             = settings.useGps
            prefs[Keys.HOME_LAT]            = settings.homeLat
            prefs[Keys.HOME_LON]            = settings.homeLon
            prefs[Keys.HOME_LABEL]          = settings.homeLabel
            prefs[Keys.WORKPLACES]          = toJson(settings.workplaces)
            prefs[Keys.SCHOOLS]             = toJson(settings.schools)
            prefs[Keys.CALENDAR_KEYWORDS]   = toJson(settings.calendarKeywords)
            prefs[Keys.HEAT_THRESHOLD]      = settings.heatThresholdC
            prefs[Keys.COLD_THRESHOLD]      = settings.coldThresholdC
            prefs[Keys.RAIN_THRESHOLD]      = settings.rainThresholdPct
            prefs[Keys.WIND_THRESHOLD]      = settings.windThresholdKmh
            prefs[Keys.UBER_RAIN_THRESHOLD] = settings.uberRainThresholdPct
            prefs[Keys.NOTIF_HOUR]          = settings.notificationHour
            prefs[Keys.NOTIF_MINUTE]        = settings.notificationMinute
        }
    }

    private inline fun <reified T> fromJson(json: String?): List<T> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            gson.fromJson(json, TypeToken.getParameterized(List::class.java, T::class.java).type)
        } catch (e: Exception) { emptyList() }
    }

    private fun toJson(obj: Any): String = gson.toJson(obj)
}