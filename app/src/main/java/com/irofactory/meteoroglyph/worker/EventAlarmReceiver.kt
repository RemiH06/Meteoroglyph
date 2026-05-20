package com.irofactory.meteoroglyph.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.irofactory.meteoroglyph.MainActivity
import com.irofactory.meteoroglyph.R
import com.irofactory.meteoroglyph.data.glyph.GlyphRepository
import com.irofactory.meteoroglyph.data.outfit.OutfitEngine
import com.irofactory.meteoroglyph.data.settings.SettingsRepository
import com.irofactory.meteoroglyph.data.weather.WeatherCondition
import com.irofactory.meteoroglyph.data.weather.WeatherRepository
import com.irofactory.meteoroglyph.data.weather.WeatherState
import com.irofactory.meteoroglyph.ui.components.GlyphBitmapRenderer
import com.irofactory.meteoroglyph.ui.components.OutfitItemState
import com.irofactory.meteoroglyph.ui.components.TextBitmapRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class EventAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID       = "meteoroglyph_events"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_EVENT_LAT   = "event_lat"
        const val EXTRA_EVENT_LON   = "event_lon"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: return
        val lat        = intent.getDoubleExtra(EXTRA_EVENT_LAT, 0.0)
        val lon        = intent.getDoubleExtra(EXTRA_EVENT_LON, 0.0)

        CoroutineScope(Dispatchers.IO).launch {
            val settingsRepo = SettingsRepository(context)
            val weatherRepo  = WeatherRepository()
            val glyphRepo    = GlyphRepository(context)
            val settings     = settingsRepo.settings.first()

            // Usar coordenadas del evento si están disponibles, si no las de casa
            val finalLat = if (lat != 0.0) lat else settings.homeLat
            val finalLon = if (lon != 0.0) lon else settings.homeLon

            val weather     = weatherRepo.getWeather(finalLat, finalLon).getOrNull() ?: return@launch
            val outfitItems = OutfitEngine.recommend(weather, settings)
                .filter { it.state != OutfitItemState.BLOCKED }
                .take(4)
            val transitRec  = OutfitEngine.recommendTransit(weather, settings)

            createNotificationChannel(context)
            showEventNotification(context, eventTitle, weather, outfitItems, transitRec, glyphRepo)
        }
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de eventos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificación 1 hora antes de cada evento con el clima esperado"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
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

    private fun showEventNotification(
        context:     Context,
        eventTitle:  String,
        weather:     com.irofactory.meteoroglyph.data.weather.WeatherState,
        outfitItems: List<com.irofactory.meteoroglyph.ui.components.OutfitItem>,
        transitRec:  com.irofactory.meteoroglyph.data.outfit.TransitRecommendation,
        glyphRepo:   GlyphRepository
    ) {
        val bgArgb          = 0xFF313035.toInt()
        val textPrimaryArgb = 0xFFF0F0F0.toInt()
        val textSecArgb     = 0xFF4A4A4A.toInt()
        val accentArgb      = 0xFF00E5A0.toInt()
        val warnArgb        = 0xFFF5A623.toInt()
        val blueArgb        = 0xFF457BFF.toInt()
        val purpleArgb      = 0xFF9B6DFF.toInt()
        val orangeArgb      = 0xFFFF7A30.toInt()

        val weatherGlyph = glyphRepo.getGlyph("weather", weather.condition.toGlyphName())
        val weatherBmp56 = weatherGlyph?.let { GlyphBitmapRenderer.render(it, 168, accentArgb) }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, eventTitle.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun textBmp(text: String, sizeSp: Float, colorArgb: Int) =
            TextBitmapRenderer.render(
                context, text,
                sizeSp * context.resources.displayMetrics.density,
                colorArgb
            )

        // ── Vista colapsada ───────────────────────────────────────────────────
        val collapsed = RemoteViews(context.packageName, R.layout.notification_collapsed).apply {
            setInt(R.id.notif_collapsed_root, "setBackgroundColor", bgArgb)
            if (weatherBmp56 != null) setImageViewBitmap(R.id.notif_weather_icon, weatherBmp56)
            setImageViewBitmap(R.id.notif_temp_condition,
                textBmp("en 1h · ${weather.tempCelsius}° ${conditionLabel(weather)}", 12f, textPrimaryArgb))
            setImageViewBitmap(R.id.notif_outfit_summary,
                textBmp(eventTitle, 10f, purpleArgb))
            setImageViewBitmap(R.id.notif_status_chip,
                textBmp(if (weather.rainProbability >= 40) "LLUVIA" else "OK",
                    9f, if (weather.rainProbability >= 40) blueArgb else accentArgb))
        }

        // ── Vista expandida ───────────────────────────────────────────────────
        val expanded = RemoteViews(context.packageName, R.layout.notification_expanded).apply {
            setInt(R.id.notif_expanded_root, "setBackgroundColor", bgArgb)
            if (weatherBmp56 != null) setImageViewBitmap(R.id.notif_exp_weather_icon, weatherBmp56)
            setImageViewBitmap(R.id.notif_exp_temp,
                textBmp("${weather.tempCelsius}°", 22f, warnArgb))
            setImageViewBitmap(R.id.notif_exp_condition,
                textBmp("en 1h · ${conditionLabel(weather)}", 10f, textSecArgb))

            if (weather.rainWindow != null) {
                setViewVisibility(R.id.notif_exp_rain, android.view.View.VISIBLE)
                setImageViewBitmap(R.id.notif_exp_rain,
                    textBmp("lluvia ${weather.rainWindow}", 10f, blueArgb))
            }

            val iconIds  = listOf(R.id.notif_icon_0,  R.id.notif_icon_1,  R.id.notif_icon_2,  R.id.notif_icon_3)
            val labelIds = listOf(R.id.notif_label_0, R.id.notif_label_1, R.id.notif_label_2, R.id.notif_label_3)
            val itemIds  = listOf(R.id.notif_item_0,  R.id.notif_item_1,  R.id.notif_item_2,  R.id.notif_item_3)

            outfitItems.forEachIndexed { i, item ->
                val tint = when (item.state) {
                    OutfitItemState.RECOMMENDED -> accentArgb
                    OutfitItemState.CONDITIONAL -> warnArgb
                    OutfitItemState.BLOCKED     -> 0xFFFF4560.toInt()
                    OutfitItemState.NEUTRAL     -> textSecArgb
                }
                val glyph = glyphRepo.getGlyph(item.glyphFile, item.glyphName)
                glyph?.let { setImageViewBitmap(iconIds[i], GlyphBitmapRenderer.render(it, 60, tint)) }
                setImageViewBitmap(labelIds[i], textBmp(item.label, 9f, tint))
            }
            for (i in outfitItems.size until 4) {
                setViewVisibility(itemIds[i], android.view.View.GONE)
            }

            val (transitGlyphName, transitFile, transitColor) = when (transitRec) {
                com.irofactory.meteoroglyph.data.outfit.TransitRecommendation.UBER          -> Triple("car", "transport", orangeArgb)
                com.irofactory.meteoroglyph.data.outfit.TransitRecommendation.CONSIDER_UBER -> Triple("car", "transport", warnArgb)
                com.irofactory.meteoroglyph.data.outfit.TransitRecommendation.PUBLIC_OK     -> Triple("bus", "transport", accentArgb)
            }
            val transitGlyph = glyphRepo.getGlyph(transitFile, transitGlyphName)
            transitGlyph?.let { setImageViewBitmap(R.id.notif_transit_icon, GlyphBitmapRenderer.render(it, 48, transitColor)) }

            val transitText = when (transitRec) {
                com.irofactory.meteoroglyph.data.outfit.TransitRecommendation.UBER          -> "mejor tomar uber/taxi"
                com.irofactory.meteoroglyph.data.outfit.TransitRecommendation.CONSIDER_UBER -> "considera uber si llevas cosas"
                com.irofactory.meteoroglyph.data.outfit.TransitRecommendation.PUBLIC_OK     -> "transporte publico ok"
            }
            setImageViewBitmap(R.id.notif_transit_label, textBmp(transitText, 9f, transitColor))
            setImageViewBitmap(R.id.notif_event, textBmp(eventTitle, 9f, purpleArgb))
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconFor(weather))
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notifId = eventTitle.hashCode()
        context.getSystemService(NotificationManager::class.java)
            .notify(notifId, notification)
    }

    private fun conditionLabel(weather: com.irofactory.meteoroglyph.data.weather.WeatherState): String =
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
}