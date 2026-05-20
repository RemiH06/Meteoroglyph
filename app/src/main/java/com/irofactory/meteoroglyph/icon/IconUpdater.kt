package com.irofactory.meteoroglyph.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.irofactory.meteoroglyph.data.weather.WeatherCondition
import com.irofactory.meteoroglyph.data.weather.WeatherState

/**
 * IconUpdater
 * ───────────────────────────────────────────────────────────────────────────
 * Cambia el ícono del launcher según la condición climática actual,
 * activando el ActivityAlias correspondiente y desactivando los demás.
 *
 * Uso:
 *   IconUpdater.update(context, weatherState)
 */
object IconUpdater {

    private const val TAG     = "IconUpdater"
    private const val PKG     = "com.irofactory.meteoroglyph"

    // ── Aliases definidos en el manifest ─────────────────────────────────────
    private val ALL_ALIASES = listOf(
        "$PKG.icon.IconDefault",
        "$PKG.icon.IconSunny",
        "$PKG.icon.IconClearNight",
        "$PKG.icon.IconCloudyDay",
        "$PKG.icon.IconCloudyNight",
        "$PKG.icon.IconOvercast",
        "$PKG.icon.IconRain",
        "$PKG.icon.IconStorm",
        "$PKG.icon.IconWind",
        "$PKG.icon.IconFog",
        "$PKG.icon.IconCold",
        "$PKG.icon.IconHot",
    )

    // ── Mapeo condición → alias ───────────────────────────────────────────────
    private fun aliasFor(weather: WeatherState): String {
        val isDay = weather.isDay
        return when (weather.condition) {
            WeatherCondition.SUNNY                                  -> "$PKG.icon.IconSunny"
            WeatherCondition.CLEAR_NIGHT                            -> "$PKG.icon.IconClearNight"
            WeatherCondition.PARTLY_CLOUDY,
            WeatherCondition.MOSTLY_CLOUDY                          ->
                if (isDay) "$PKG.icon.IconCloudyDay" else "$PKG.icon.IconCloudyNight"
            WeatherCondition.PARTLY_CLOUDY_NIGHT,
            WeatherCondition.MOSTLY_CLOUDY_NIGHT                    -> "$PKG.icon.IconCloudyNight"
            WeatherCondition.OVERCAST                               -> "$PKG.icon.IconOvercast"
            WeatherCondition.DRIZZLE,
            WeatherCondition.RAIN,
            WeatherCondition.HEAVY_RAIN                             -> "$PKG.icon.IconRain"
            WeatherCondition.STORM                                  -> "$PKG.icon.IconStorm"
            WeatherCondition.WIND                                   -> "$PKG.icon.IconWind"
            WeatherCondition.FOG                                    -> "$PKG.icon.IconFog"
            WeatherCondition.COLD,
            WeatherCondition.SNOW,
            WeatherCondition.SLEET                                  -> "$PKG.icon.IconCold"
            WeatherCondition.HOT                                    -> "$PKG.icon.IconHot"
        }
    }

    // ── Actualizar ícono ──────────────────────────────────────────────────────
    fun update(context: Context, weather: WeatherState) {
        val pm          = context.packageManager
        val targetAlias = aliasFor(weather)

        ALL_ALIASES.forEach { alias ->
            val component = ComponentName(PKG, alias)
            val newState  = if (alias == targetAlias)
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED

            try {
                val currentState = pm.getComponentEnabledSetting(component)
                // Solo cambiar si es necesario — evita trabajo innecesario
                if (currentState != newState) {
                    pm.setComponentEnabledSetting(
                        component,
                        newState,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cambiar alias $alias: ${e.message}")
            }
        }

        Log.d(TAG, "Ícono actualizado → $targetAlias")
    }

    // ── Restaurar default ─────────────────────────────────────────────────────
    fun resetToDefault(context: Context) {
        val pm = context.packageManager
        ALL_ALIASES.forEach { alias ->
            val component = ComponentName(PKG, alias)
            val newState  = if (alias == "$PKG.icon.IconDefault")
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            try {
                pm.setComponentEnabledSetting(component, newState, PackageManager.DONT_KILL_APP)
            } catch (e: Exception) {
                Log.e(TAG, "Error al resetear $alias: ${e.message}")
            }
        }
        Log.d(TAG, "Ícono reseteado al default")
    }
}