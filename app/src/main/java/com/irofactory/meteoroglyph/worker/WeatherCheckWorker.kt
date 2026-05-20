package com.irofactory.meteoroglyph.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import android.widget.RemoteViews
import androidx.glance.appwidget.updateAll
import com.irofactory.meteoroglyph.data.glyph.GlyphRepository
import com.irofactory.meteoroglyph.data.weather.WeatherCondition
import com.irofactory.meteoroglyph.icon.IconUpdater
import com.irofactory.meteoroglyph.ui.components.GlyphBitmapRenderer
import com.irofactory.meteoroglyph.ui.components.OutfitItem
import com.irofactory.meteoroglyph.ui.components.TextBitmapRenderer
import com.irofactory.meteoroglyph.widget.WeatherInfoWidget
import com.irofactory.meteoroglyph.widget.WeatherCircleWidget
import kotlinx.coroutines.runBlocking

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
        val nextEvent = try {
            calendarRepo.getNextEvent(settings)
        } catch (e: Exception) { null }

        // Generar recomendación
        val outfitItems = OutfitEngine.recommend(weather, settings)
        val transitRec  = OutfitEngine.recommendTransit(weather, settings)

        // Construir y disparar notificación
        createNotificationChannel(context)
        showNotification(context, weather, outfitItems
            .filter { it.state != OutfitItemState.BLOCKED }
            .take(4), transitRec, nextEvent?.title)

        weather?.let { IconUpdater.update(context, it) }

        // Reprogramar alarmas de eventos del día
        EventAlarmScheduler.scheduleAll(context, settings)

        // Actualizar widgets
        WeatherInfoWidget().updateAll(context)
        WeatherCircleWidget().updateAll(context)

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

    private fun textBmp(
        context: Context,
        text: String,
        sizeSp: Float,
        colorArgb: Int,
        maxWidthPx: Int = 0
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx  = sizeSp * density
        return TextBitmapRenderer.render(context, text, sizePx, colorArgb, maxWidthPx)
    }

    private fun smallIconFor(weather: WeatherState): Int = when (weather.condition) {
        WeatherCondition.SUNNY                                  -> R.drawable.ic_notif_sunny
        WeatherCondition.CLEAR_NIGHT                            -> R.drawable.ic_notif_clear_night
        WeatherCondition.PARTLY_CLOUDY,
        WeatherCondition.MOSTLY_CLOUDY                          -> R.drawable.ic_notif_cloudy_day
        WeatherCondition.PARTLY_CLOUDY_NIGHT,
        WeatherCondition.MOSTLY_CLOUDY_NIGHT                    -> R.drawable.ic_notif_cloudy_night
        WeatherCondition.OVERCAST                               -> R.drawable.ic_notif_overcast
        WeatherCondition.DRIZZLE,
        WeatherCondition.RAIN,
        WeatherCondition.HEAVY_RAIN                             -> R.drawable.ic_notif_rain
        WeatherCondition.STORM                                  -> R.drawable.ic_notif_storm
        WeatherCondition.WIND                                   -> R.drawable.ic_notif_wind
        WeatherCondition.FOG                                    -> R.drawable.ic_notif_fog
        WeatherCondition.COLD,
        WeatherCondition.SNOW,
        WeatherCondition.SLEET                                  -> R.drawable.ic_notif_cold
        WeatherCondition.HOT                                    -> R.drawable.ic_notif_hot
    }

    private fun showNotification(
        context: Context,
        weather: WeatherState,
        outfitItems: List<OutfitItem>,
        transitRec: TransitRecommendation,
        nextEventTitle: String?
    ) {
        val settings  = runBlocking { SettingsRepository(context).settings.first() }
        val isDark    = settings.themeMode != "LIGHT"
        val glyphRepo = GlyphRepository(context)

        // Siempre oscuro para que haga match con el sistema de Nothing OS
        val bgArgb          = 0xFF313035.toInt()
        val textPrimaryArgb = 0xFFF0F0F0.toInt()
        val textSecArgb     = 0xFF4A4A4A.toInt()
        val accentArgb      = 0xFF00E5A0.toInt()
        val warnArgb        = 0xFFF5A623.toInt()
        val blueArgb        = 0xFF457BFF.toInt()
        val purpleArgb      = 0xFF9B6DFF.toInt()
        val orangeArgb      = 0xFFFF7A30.toInt()

        // Glifo de clima
        val weatherGlyph = glyphRepo.getGlyph("weather", weather.condition.toGlyphName())
        val weatherBmp40 = weatherGlyph?.let {
            GlyphBitmapRenderer.render(it, 120, accentArgb)
        }
        val weatherBmp56 = weatherGlyph?.let {
            GlyphBitmapRenderer.render(it, 168, accentArgb)
        }

        // Intent al abrir
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val density = context.resources.displayMetrics.density

// ── Vista colapsada ───────────────────────────────────────────────────────────
        val collapsed = RemoteViews(context.packageName, R.layout.notification_collapsed).apply {
            setInt(R.id.notif_collapsed_root, "setBackgroundColor", bgArgb)
            if (weatherBmp40 != null) setImageViewBitmap(R.id.notif_weather_icon, weatherBmp40)

            setImageViewBitmap(R.id.notif_temp_condition,
                textBmp(context, "${weather.tempCelsius}° · ${conditionLabel(weather)}", 13f, textPrimaryArgb))

            setImageViewBitmap(R.id.notif_outfit_summary,
                textBmp(context, outfitItems.take(3).joinToString(" · ") { it.label }, 10f, textSecArgb))

            val chipText   = if (weather.rainProbability >= 40) "LLUVIA" else "OK"
            val chipColor  = if (weather.rainProbability >= 40) blueArgb else accentArgb
            setImageViewBitmap(R.id.notif_status_chip, textBmp(context, chipText, 9f, chipColor))
        }

// ── Vista expandida ───────────────────────────────────────────────────────────
        val expanded = RemoteViews(context.packageName, R.layout.notification_expanded).apply {
            setInt(R.id.notif_expanded_root, "setBackgroundColor", bgArgb)

            if (weatherBmp56 != null) setImageViewBitmap(R.id.notif_exp_weather_icon, weatherBmp56)

            setImageViewBitmap(R.id.notif_exp_temp,
                textBmp(context, "${weather.tempCelsius}°", 22f, warnArgb))

            setImageViewBitmap(R.id.notif_exp_condition,
                textBmp(context, conditionLabel(weather), 10f, textSecArgb))

            if (weather.rainWindow != null) {
                setViewVisibility(R.id.notif_exp_rain, android.view.View.VISIBLE)
                setImageViewBitmap(R.id.notif_exp_rain,
                    textBmp(context, "lluvia ${weather.rainWindow}", 10f, blueArgb))
            }

            val iconIds  = listOf(R.id.notif_icon_0,  R.id.notif_icon_1,  R.id.notif_icon_2,  R.id.notif_icon_3)
            val labelIds = listOf(R.id.notif_label_0, R.id.notif_label_1, R.id.notif_label_2, R.id.notif_label_3)
            val itemIds  = listOf(R.id.notif_item_0,  R.id.notif_item_1,  R.id.notif_item_2,  R.id.notif_item_3)

            outfitItems.take(4).forEachIndexed { i, item ->
                val tint = when (item.state) {
                    OutfitItemState.RECOMMENDED -> accentArgb
                    OutfitItemState.CONDITIONAL -> warnArgb
                    OutfitItemState.BLOCKED     -> 0xFFFF4560.toInt()
                    OutfitItemState.NEUTRAL     -> textSecArgb
                }
                val glyph = glyphRepo.getGlyph(item.glyphFile, item.glyphName)
                val bmp   = glyph?.let { GlyphBitmapRenderer.render(it, 60, tint) }
                if (bmp != null) setImageViewBitmap(iconIds[i], bmp)
                setImageViewBitmap(labelIds[i], textBmp(context, item.label, 9f, tint))
            }

            for (i in outfitItems.size until 4) {
                setViewVisibility(itemIds[i], android.view.View.GONE)
            }

            val (transitGlyphName, transitFile, transitColor) = when (transitRec) {
                TransitRecommendation.UBER          -> Triple("car", "transport", orangeArgb)
                TransitRecommendation.CONSIDER_UBER -> Triple("car", "transport", warnArgb)
                TransitRecommendation.PUBLIC_OK     -> Triple("bus", "transport", accentArgb)
            }
            val transitGlyph = glyphRepo.getGlyph(transitFile, transitGlyphName)
            val transitBmp   = transitGlyph?.let { GlyphBitmapRenderer.render(it, 48, transitColor) }
            if (transitBmp != null) setImageViewBitmap(R.id.notif_transit_icon, transitBmp)

            val transitText = when (transitRec) {
                TransitRecommendation.UBER          -> "mejor tomar uber/taxi"
                TransitRecommendation.CONSIDER_UBER -> "considera uber si llevas cosas"
                TransitRecommendation.PUBLIC_OK     -> "transporte publico ok"
            }
            setImageViewBitmap(R.id.notif_transit_label,
                textBmp(context, transitText, 9f, transitColor))

            if (nextEventTitle != null) {
                setImageViewBitmap(R.id.notif_event,
                    textBmp(context, nextEventTitle, 9f, purpleArgb))
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconFor(weather))
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
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