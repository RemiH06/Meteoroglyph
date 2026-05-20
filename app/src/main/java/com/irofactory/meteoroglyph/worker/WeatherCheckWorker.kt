package com.irofactory.meteoroglyph.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.irofactory.meteoroglyph.MainActivity
import com.irofactory.meteoroglyph.R
import com.irofactory.meteoroglyph.data.calendar.CalendarRepository
import com.irofactory.meteoroglyph.data.outfit.OutfitEngine
import com.irofactory.meteoroglyph.data.outfit.TransitRecommendation
import com.irofactory.meteoroglyph.data.settings.SettingsRepository
import com.irofactory.meteoroglyph.data.weather.WeatherRepository
import com.irofactory.meteoroglyph.data.weather.WeatherState
import com.irofactory.meteoroglyph.ui.components.OutfitItemState
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class WeatherCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context      = applicationContext
        val settingsRepo = SettingsRepository(context)
        val weatherRepo  = WeatherRepository()
        val calendarRepo = CalendarRepository(context)
        val settings     = settingsRepo.settings.first()

        // Obtener clima
        val weatherResult = weatherRepo.getWeather(settings.homeLat, settings.homeLon)
        val weather       = weatherResult.getOrNull() ?: return Result.retry()

        // Obtener siguiente evento
        val nextEvent = try { calendarRepo.getNextEvent() } catch (e: Exception) { null }

        // Generar recomendación
        val outfitItems  = OutfitEngine.recommend(weather)
        val transitRec   = OutfitEngine.recommendTransit(weather)

        // Construir y disparar notificación
        createNotificationChannel(context)
        showNotification(context, weather, outfitItems
            .filter { it.state != OutfitItemState.BLOCKED }
            .take(4), transitRec, nextEvent?.title)

        return Result.success()
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Resumen diario",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Recomendación de outfit y clima del día"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun showNotification(
        context: Context,
        weather: WeatherState,
        outfitItems: List<com.irofactory.meteoroglyph.ui.components.OutfitItem>,
        transitRec: TransitRecommendation,
        nextEventTitle: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Línea principal — temperatura y condición
        val tempLine = "${weather.tempCelsius}° · ${conditionEmoji(weather)} ${conditionLabel(weather)}"

        // Prendas recomendadas
        val outfitLine = outfitItems.joinToString(" · ") { it.label }

        // Lluvia
        val rainLine = weather.rainWindow?.let { "☂ lluvia $it" }

        // Transporte
        val transitLine = when (transitRec) {
            TransitRecommendation.UBER          -> "🚕 mejor tomar uber/taxi hoy"
            TransitRecommendation.CONSIDER_UBER -> "🚌 considera uber si llevas cosas"
            TransitRecommendation.PUBLIC_OK     -> null
        }

        // Evento siguiente
        val eventLine = nextEventTitle?.let { "📅 próximo: $it" }

        val lines = listOfNotNull(outfitLine, rainLine, transitLine, eventLine)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(tempLine)
            .setContentText(lines.firstOrNull())
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(lines.joinToString("\n")))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun conditionEmoji(weather: WeatherState): String = when {
        weather.rainProbability >= 70 -> "🌧"
        weather.rainProbability >= 40 -> "🌦"
        weather.windSpeedKmh >= 35    -> "💨"
        else                          -> "☀"
    }

    private fun conditionLabel(weather: WeatherState): String =
        when (weather.condition) {
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.SUNNY               -> "despejado"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.CLEAR_NIGHT         -> "noche despejada"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.PARTLY_CLOUDY,
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.PARTLY_CLOUDY_NIGHT -> "parcialmente nublado"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.MOSTLY_CLOUDY,
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.MOSTLY_CLOUDY_NIGHT -> "mayormente nublado"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.OVERCAST            -> "nublado"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.DRIZZLE             -> "llovizna"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.RAIN                -> "lluvia"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.HEAVY_RAIN          -> "lluvia fuerte"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.STORM               -> "tormenta"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.WIND                -> "viento fuerte"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.FOG                 -> "neblina"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.SNOW                -> "nieve"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.SLEET               -> "aguanieve"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.HOT                 -> "calor extremo"
            com.irofactory.meteoroglyph.data.weather.WeatherCondition.COLD                -> "frío"
        }

    companion object {
        const val CHANNEL_ID      = "meteoroglyph_daily"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME       = "weather_check"

        fun schedule(context: Context, hour: Int, minute: Int) {
            val now     = java.util.Calendar.getInstance()
            val target  = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                if (before(now)) add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            val delay = target.timeInMillis - now.timeInMillis

            val request = OneTimeWorkRequestBuilder<WeatherCheckWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}