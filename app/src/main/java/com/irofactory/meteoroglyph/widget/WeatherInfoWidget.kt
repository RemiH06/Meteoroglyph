package com.irofactory.meteoroglyph.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.Dimension
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
import kotlinx.coroutines.flow.first

class WeatherInfoWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settingsRepo = SettingsRepository(context)
        val weatherRepo  = WeatherRepository()
        val glyphRepo    = GlyphRepository(context)
        val settings     = settingsRepo.settings.first()

        val weather     = weatherRepo.getWeather(settings.homeLat, settings.homeLon).getOrNull()
        val outfitItems = weather?.let {
            OutfitEngine.recommend(it, settings)
                .filter { item -> item.state != OutfitItemState.BLOCKED }
                .take(3)
        } ?: emptyList()

        // ── Colores ARGB ──────────────────────────────────────────────────────
        val bgLightArgb     = android.graphics.Color.parseColor("#f5edf4")
        val bgDarkArgb      = android.graphics.Color.parseColor("#1e1a20")
        val textDarkArgb    = android.graphics.Color.parseColor("#f0f0f0")
        val textSecDarkArgb = android.graphics.Color.parseColor("#4a4a4a")

        // ── Bitmaps de clima ──────────────────────────────────────────────────
        val weatherGlyph = weather?.let {
            glyphRepo.getGlyph("weather", it.condition.toGlyphName())
        }
        val weatherBmp = weatherGlyph?.let { GlyphBitmapRenderer.render(it, 160, null) }

        fun textBmp(text: String, sizeSp: Float, colorArgb: Int): Bitmap {
            val density = context.resources.displayMetrics.density
            val sizePx  = sizeSp * density
            val tf      = ResourcesCompat.getFont(context, R.font.ndot57_regular)
            val paint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = tf; textSize = sizePx; color = colorArgb
            }
            val w  = paint.measureText(text).toInt().coerceAtLeast(1)
            val fm = paint.fontMetrics
            val h  = (fm.descent - fm.ascent).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(bmp).drawText(text, 0f, -fm.ascent, paint)
            return bmp
        }

        val condLabel = weather?.let { conditionLabel(it) } ?: "—"

        provideContent {
            val bgColor = ColorProvider(
                day   = androidx.compose.ui.graphics.Color(bgLightArgb),
                night = androidx.compose.ui.graphics.Color(bgDarkArgb)
            )

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgColor)
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(12.dp)
            ) {
                Row(
                    modifier          = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ── Ícono de clima ────────────────────────────────────────
                    if (weatherBmp != null) {
                        Box(modifier = GlanceModifier.size(64.dp)) {
                            Image(
                                provider           = ImageProvider(weatherBmp),
                                contentDescription = condLabel,
                                modifier           = GlanceModifier.fillMaxSize(),
                                contentScale       = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(12.dp))
                    }

                    // ── Info ──────────────────────────────────────────────────
                    Column(
                        modifier = GlanceModifier.fillMaxHeight().defaultWeight()
                    ) {
                        if (weather != null) {
                            Image(
                                provider           = ImageProvider(textBmp("${weather.tempCelsius}°", 22f, textDarkArgb)),
                                contentDescription = "${weather.tempCelsius}°",
                                modifier           = GlanceModifier.wrapContentHeight()
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Image(
                                provider           = ImageProvider(textBmp(condLabel, 10f, textSecDarkArgb)),
                                contentDescription = condLabel,
                                modifier           = GlanceModifier.wrapContentHeight()
                            )
                            if (weather.rainWindow != null) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Image(
                                    provider           = ImageProvider(textBmp("lluvia ${weather.rainWindow}", 9f, 0xFF457BFF.toInt())),
                                    contentDescription = "lluvia ${weather.rainWindow}",
                                    modifier           = GlanceModifier.wrapContentHeight()
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // ── Chips de outfit ───────────────────────────────────
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            outfitItems.forEach { item ->
                                val tintArgb = when (item.state) {
                                    OutfitItemState.RECOMMENDED -> 0xFF00E5A0.toInt()
                                    OutfitItemState.CONDITIONAL -> 0xFFF5A623.toInt()
                                    else                        -> 0xFF888888.toInt()
                                }
                                val glyph = glyphRepo.getGlyph(item.glyphFile, item.glyphName)
                                if (glyph != null) {
                                    Image(
                                        provider           = ImageProvider(GlyphBitmapRenderer.render(glyph, 48, tintArgb)),
                                        contentDescription = item.label,
                                        modifier           = GlanceModifier.size(24.dp)
                                    )
                                    Spacer(modifier = GlanceModifier.width(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun conditionLabel(weather: WeatherState): String = when (weather.condition) {
        WeatherCondition.SUNNY               -> "despejado"
        WeatherCondition.CLEAR_NIGHT         -> "noche despejada"
        WeatherCondition.PARTLY_CLOUDY,
        WeatherCondition.PARTLY_CLOUDY_NIGHT -> "parcialmente nublado"
        WeatherCondition.MOSTLY_CLOUDY,
        WeatherCondition.MOSTLY_CLOUDY_NIGHT -> "mayormente nublado"
        WeatherCondition.OVERCAST            -> "nublado"
        WeatherCondition.DRIZZLE             -> "llovizna"
        WeatherCondition.RAIN                -> "lluvia"
        WeatherCondition.HEAVY_RAIN          -> "lluvia fuerte"
        WeatherCondition.STORM               -> "tormenta"
        WeatherCondition.WIND                -> "viento fuerte"
        WeatherCondition.FOG                 -> "neblina"
        WeatherCondition.SNOW                -> "nieve"
        WeatherCondition.SLEET               -> "aguanieve"
        WeatherCondition.HOT                 -> "calor extremo"
        WeatherCondition.COLD                -> "frío"
    }
}

class WeatherInfoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherInfoWidget()
}