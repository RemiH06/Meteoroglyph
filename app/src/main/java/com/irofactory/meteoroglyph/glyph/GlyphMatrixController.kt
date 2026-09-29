package com.irofactory.meteoroglyph.glyph

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.irofactory.meteoroglyph.data.glyph.GlyphRepository
import com.irofactory.meteoroglyph.data.weather.WeatherState
import com.irofactory.meteoroglyph.ui.components.GlyphBitmapRenderer
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphMatrixManager

/**
 * GlyphMatrixController
 * ───────────────────────────────────────────────────────────────────────────
 * Controla la Glyph Matrix 25x25 del Nothing Phone 3 (DEVICE_23112).
 * Usa setAppMatrixFrame para no chocar con los Glyph Toys del sistema
 * (que tienen prioridad de despliegue sobre el contenido de la app).
 *
 * Muestra el glifo climático correspondiente (mismo set de weather.json
 * usado en el resto de la app) escalado por el propio SDK al bitmap 1:1.
 */
class GlyphMatrixController(private val context: Context) {

    companion object {
        @Volatile private var instance: GlyphMatrixController? = null

        fun getInstance(context: Context): GlyphMatrixController {
            return instance ?: synchronized(this) {
                instance ?: GlyphMatrixController(context.applicationContext).also {
                    instance = it
                }
            }
        }

        private const val MATRIX_SIZE = 25
    }

    private val tag = "GlyphMatrixController"
    private val glyphRepo = GlyphRepository(context)

    private var manager: GlyphMatrixManager? = null
    private var isConnected  = false
    private var isRegistered = false

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            isConnected = true
            try {
                isRegistered = manager?.register(Glyph.DEVICE_23112) ?: false
                Log.d(tag, "Glyph Matrix registrada: $isRegistered en ${android.os.Build.MODEL}")
            } catch (e: Exception) {
                Log.e(tag, "Error al registrar: ${e.message}")
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            isConnected  = false
            isRegistered = false
        }
    }

    fun init() {
        if (!Common.is23112()) {
            Log.d(tag, "Dispositivo no soportado: ${android.os.Build.MODEL}")
            return
        }
        manager = GlyphMatrixManager.getInstance(context)
        manager?.init(callback)
    }

    fun close() {
        try { manager?.closeAppMatrix(); manager?.unInit() } catch (e: Exception) { }
        isConnected = false; isRegistered = false
    }

    /** Muestra el glifo correspondiente al clima actual */
    fun notifyWeather(weather: WeatherState) {
        if (!isReady()) return
        val glyph = glyphRepo.getGlyph("weather25x25", weather.condition.toGlyphName()) ?: return
        try {
            val array = GlyphBitmapRenderer.renderToMatrixArray(glyph, MATRIX_SIZE)
            manager?.setAppMatrixFrame(array)
        } catch (e: GlyphException) {
            Log.e(tag, "Error al mostrar glifo: ${e.message}")
        }
    }

    fun turnOff() {
        if (!isReady()) return
        try { manager?.closeAppMatrix() } catch (e: Exception) { Log.e(tag, "turnOff error: ${e.message}") }
    }

    private fun isReady() = isConnected && isRegistered
}
