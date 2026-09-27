package com.irofactory.meteoroglyph.glyph.toy

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.irofactory.meteoroglyph.data.glyph.GlyphRepository
import com.irofactory.meteoroglyph.data.settings.SettingsRepository
import com.irofactory.meteoroglyph.data.weather.WeatherRepository
import com.irofactory.meteoroglyph.data.weather.WeatherState
import com.irofactory.meteoroglyph.ui.components.GlyphBitmapRenderer
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphToy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * WeatherGlyphToyService
 * ───────────────────────────────────────────────────────────────────────────
 * Glyph Toy para consultar el clima con el botón trasero (Nothing Phone 3).
 *
 * Interacción:
 *   - Al seleccionar el toy (short-press del carrusel) → muestra el glifo
 *     del clima actual (se refresca en ese momento).
 *   - Touch-down (mantener presionado) → muestra la temperatura en texto.
 *   - Touch-up (soltar) → vuelve al glifo.
 *   - Long-press (evento "change") → fuerza un refresh del clima.
 */
class WeatherGlyphToyService : Service() {

    private val tag = "WeatherGlyphToy"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var refreshJob: Job? = null

    private var glyphMatrixManager: GlyphMatrixManager? = null
    private var lastWeather: WeatherState? = null

    private val glyphRepo by lazy { GlyphRepository(applicationContext) }
    private val weatherRepo = WeatherRepository()

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what != GlyphToy.MSG_GLYPH_TOY) { super.handleMessage(msg); return }
            val event = msg.data?.getString(GlyphToy.MSG_GLYPH_TOY_DATA) ?: return
            when (event) {
                GlyphToy.EVENT_ACTION_DOWN -> showTemperature()
                GlyphToy.EVENT_ACTION_UP   -> showIcon()
                GlyphToy.EVENT_CHANGE      -> refreshWeather()
            }
        }
    }
    private val messenger = Messenger(handler)

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            try {
                glyphMatrixManager?.register(Glyph.DEVICE_23112)
            } catch (e: Exception) {
                Log.e(tag, "Error al registrar: ${e.message}")
            }
            refreshWeather()
        }
        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    override fun onBind(intent: Intent?): IBinder {
        glyphMatrixManager = GlyphMatrixManager.getInstance(applicationContext)
        glyphMatrixManager?.init(callback)
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        refreshJob?.cancel()
        try { glyphMatrixManager?.turnOff(); glyphMatrixManager?.unInit() } catch (e: Exception) { }
        glyphMatrixManager = null
        return false
    }

    /** Vuelve a pedir el clima a la API y actualiza el glifo mostrado */
    private fun refreshWeather() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            try {
                val settings = SettingsRepository(applicationContext).settings.first()
                val weather  = weatherRepo.getWeather(settings.homeLat, settings.homeLon).getOrNull()
                if (weather != null) {
                    lastWeather = weather
                    showIcon()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error al obtener clima: ${e.message}")
            }
        }
    }

    private fun showIcon() {
        val weather = lastWeather ?: return
        val glyph = glyphRepo.getGlyph("weather", weather.condition.toGlyphName()) ?: return
        val bitmap = GlyphBitmapRenderer.render(glyph, sizePx = 32, tintArgb = android.graphics.Color.WHITE)
        renderObject(
            GlyphMatrixObject.Builder()
                .setImageSource(bitmap)
                .setScale(100)
                .setPosition(0, 0)
                .setBrightness(255)
                .build()
        )
    }

    private fun showTemperature() {
        val weather = lastWeather ?: return
        renderObject(
            GlyphMatrixObject.Builder()
                .setText("${weather.tempCelsius}°")
                .setPosition(6, 9)
                .setBrightness(255)
                .build()
        )
    }

    private fun renderObject(obj: GlyphMatrixObject) {
        try {
            val frame = GlyphMatrixFrame.Builder().addTop(obj).build(applicationContext)
            glyphMatrixManager?.setMatrixFrame(frame.render())
        } catch (e: GlyphException) {
            Log.e(tag, "Error al dibujar: ${e.message}")
        }
    }
}
