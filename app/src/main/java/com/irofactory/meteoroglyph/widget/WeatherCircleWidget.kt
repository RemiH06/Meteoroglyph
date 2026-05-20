package com.irofactory.meteoroglyph.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.Dimension
import com.irofactory.meteoroglyph.MainActivity
import com.irofactory.meteoroglyph.data.glyph.GlyphRepository
import com.irofactory.meteoroglyph.data.settings.SettingsRepository
import com.irofactory.meteoroglyph.data.weather.WeatherRepository
import com.irofactory.meteoroglyph.ui.components.GlyphBitmapRenderer
import kotlinx.coroutines.flow.first

class WeatherCircleWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // ── Cargar datos ──────────────────────────────────────────────────────
        val settingsRepo = SettingsRepository(context)
        val weatherRepo  = WeatherRepository()
        val glyphRepo    = GlyphRepository(context)
        val settings     = settingsRepo.settings.first()

        val weather     = weatherRepo.getWeather(settings.homeLat, settings.homeLon).getOrNull()
        val condLabel   = weather?.condition?.toGlyphName() ?: "partly_cloudy"

        // ── Glifo de clima ────────────────────────────────────────────────────
        val weatherGlyph = glyphRepo.getGlyph("weather", condLabel)
        // Renderizar con colores originales del JSON (tint = null)
        val glyphBmp = weatherGlyph?.let {
            GlyphBitmapRenderer.render(it, 256, null)
        }

        // ── Colores ───────────────────────────────────────────────────────────
        val bgLight = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#f5edf4"))
        val bgDark  = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#1e1a20"))
        val bgColor = ColorProvider(day = bgLight, night = bgDark)

        provideContent {
            Box(
                modifier          = GlanceModifier
                    .fillMaxSize()
                    .background(bgColor)
                    .cornerRadius(999.dp)
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(12.dp),
                contentAlignment  = Alignment.Center
            ) {
                if (glyphBmp != null) {
                    Image(
                        provider           = ImageProvider(glyphBmp),
                        contentDescription = condLabel,
                        modifier           = GlanceModifier.fillMaxSize(),
                        contentScale       = ContentScale.Fit
                    )
                }
            }
        }
    }
}

class WeatherCircleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherCircleWidget()
}