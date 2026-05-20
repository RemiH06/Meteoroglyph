package com.irofactory.meteoroglyph.glyph

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.irofactory.meteoroglyph.data.weather.WeatherCondition
import com.irofactory.meteoroglyph.data.weather.WeatherState
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * GlyphController
 * ───────────────────────────────────────────────────────────────────────────
 * Controla los LEDs traseros del Nothing Phone 3a (A059, DEVICE_24111).
 *
 * Mapa físico de los arcos:
 *   Grupo A → A_1..A_11  (índices 20–30) — arco DERECHO
 *   Grupo B → B_1..B_5   (índices 31–35) — arco ESQUINA INFERIOR IZQUIERDA
 *   Grupo C → C_1..C_20  (índices 0–19)  — arco ESQUINA SUPERIOR DERECHA
 *
 * Patrones disponibles:
 *   rain()        — goteo C→A→B segmento a segmento
 *   storm()       — relámpago eléctrico pares/impares
 *   heat()        — pulso lento solo arco A
 *   wind()        — barrido rápido C→A→B y vuelta
 *   cold()        — respiración lenta los tres arcos
 *   fog()         — tenue estático los tres arcos
 *   sleet()       — alternancia irregular C vs A+B
 *   snow()        — destellos dispersos
 *   upcomingEvent() — secuencial suave C→A→B
 */
class GlyphController(private val context: Context) {

    companion object {
        @Volatile private var instance: GlyphController? = null

        fun getInstance(context: Context): GlyphController {
            return instance ?: synchronized(this) {
                instance ?: GlyphController(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val tag = "GlyphController"

    private var manager: GlyphManager? = null
    private var isConnected            = false
    private var isRegistered           = false
    private var patternJob: Job?       = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // ── Constantes ────────────────────────────────────────────────────────────
    private val MAX   = 4000
    private val DIM   = (MAX * 0.25).toInt()
    private val MID   = (MAX * 0.60).toInt()
    private val SIZE  = 36

    // ── Índices por grupo ─────────────────────────────────────────────────────
    private val A   = listOf(20,21,22,23,24,25,26,27,28,29,30)  // arco derecho
    private val B   = listOf(31,32,33,34,35)                     // inf izquierda
    private val C   = (0..19).toList()                           // sup derecha

    private val A_ODD  = listOf(20,22,24,26,28,30)   // impares de A (A_1,A_3...)
    private val A_EVEN = listOf(21,23,25,27,29)       // pares de A
    private val C_ODD  = listOf(0,2,4,6,8,10,12,14,16,18)   // impares de C
    private val C_EVEN = listOf(1,3,5,7,9,11,13,15,17,19)   // pares de C

    // ── Callback ──────────────────────────────────────────────────────────────
    private val callback = object : GlyphManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            Log.d(tag, "Glyph conectado")
            isConnected = true
            try {
                isRegistered = manager?.register() ?: false
                Log.d(tag, "Registrado: $isRegistered en ${android.os.Build.MODEL}")
            } catch (e: Exception) {
                Log.e(tag, "Error al registrar: ${e.message}")
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(tag, "Glyph desconectado")
            isConnected  = false
            isRegistered = false
        }
    }

    // ── Init / Close ──────────────────────────────────────────────────────────
    fun init() {
        if (!Common.is24111()) {
            Log.d(tag, "Dispositivo no soportado: ${android.os.Build.MODEL}")
            return
        }
        manager = GlyphManager.getInstance(context)
        manager?.init(callback)
    }

    fun close() {
        stopPattern()
        try { manager?.closeSession(); manager?.unInit() } catch (e: Exception) { }
        isConnected = false; isRegistered = false
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /** Selecciona y activa el patrón según el clima */
    fun notifyWeather(weather: WeatherState) {
        if (!isReady()) return
        stopPattern()
        patternJob = scope.launch {
            try {
                manager?.openSession()
                when {
                    weather.condition == WeatherCondition.STORM                   -> storm()
                    weather.condition == WeatherCondition.HEAVY_RAIN              -> rain(fast = true)
                    weather.condition == WeatherCondition.RAIN                    -> rain()
                    weather.condition == WeatherCondition.DRIZZLE                 -> rain(dim = true)
                    weather.condition == WeatherCondition.SNOW                    -> snow()
                    weather.condition == WeatherCondition.SLEET                   -> sleet()
                    weather.condition == WeatherCondition.FOG                     -> fog()
                    weather.condition == WeatherCondition.WIND                    -> wind()
                    weather.condition == WeatherCondition.HOT || weather.tempCelsius >= 35 -> heat()
                    weather.condition == WeatherCondition.COLD || weather.tempCelsius <= 8 -> cold()
                }
                manager?.closeSession()
            } catch (e: GlyphException) {
                Log.e(tag, "Error en patrón: ${e.message}")
            }
        }
    }

    /** Activa el patrón de evento próximo si el clima no es ideal */
    fun notifyUpcomingEvent(weather: WeatherState) {
        if (!isReady()) return
        if (!isWeatherNotIdeal(weather)) { stopPattern(); return }
        stopPattern()
        patternJob = scope.launch {
            try {
                manager?.openSession()
                upcomingEvent()
                manager?.closeSession()
            } catch (e: GlyphException) {
                Log.e(tag, "Error en patrón evento: ${e.message}")
            }
        }
    }

    /** Activa un patrón de test por nombre de grupo */
    fun testGroup(indices: List<Int>) {
        if (!isReady()) return
        stopPattern()
        patternJob = scope.launch {
            try {
                manager?.openSession()
                setIndices(indices, MAX)
                delay(2000)
                setAll(0)
                manager?.closeSession()
            } catch (e: Exception) {
                Log.e(tag, "Error en test: ${e.message}")
            }
        }
    }

    fun stopPattern() {
        patternJob?.cancel()
        patternJob = null
        turnOff()
    }

    // ── Patrones ──────────────────────────────────────────────────────────────

    /** Anillo CAB — 35 posiciones en orden C_1..C_20 → A_1..A_11 → B_1..B_5 */
    private val RING = C + A + B   // 35 índices en orden de rotación
    private val RING_SIZE = RING.size  // 35

    /**
     * Rotación de N puntos desfasados en el anillo CAB.
     * Cada punto deja un rastro de [tail] segmentos que se va apagando.
     * stepMs — ms entre cada posición del anillo.
     * reps   — vueltas completas al anillo.
     */
    private suspend fun ringRotation(
        points: Int,
        stepMs: Long,
        reps: Int,
        brightness: Int = MAX,
        tail: Int = 3
    ) {
        val offsets = (0 until points).map { i -> (RING_SIZE / points) * i }
        val totalSteps = RING_SIZE * reps

        for (step in 0 until totalSteps) {
            if (!coroutineContext.isActive) return
            val colors = IntArray(SIZE) { 0 }
            offsets.forEach { offset ->
                val head = (step + offset) % RING_SIZE
                // cabeza al máximo
                colors[RING[head]] = brightness
                // cola con fade
                for (t in 1..tail) {
                    val tailIdx = (head - t + RING_SIZE) % RING_SIZE
                    colors[RING[tailIdx]] = (brightness * (tail - t + 1) / (tail + 1))
                }
            }
            try { manager?.setFrameColors(colors) } catch (e: Exception) { }
            delay(stepMs)
        }
        setAll(0)
    }

    /** Lluvia — 2 puntos girando en el anillo */
    private suspend fun rain(fast: Boolean = false, dim: Boolean = false) {
        val brightness = if (dim) MID else MAX
        val stepMs     = if (fast) 25L else 45L
        val reps       = if (fast) 3 else 2
        ringRotation(points = 2, stepMs = stepMs, reps = reps, brightness = brightness, tail = 4)
    }

    /** Tormenta — relámpago eléctrico alternando pares/impares */
    private suspend fun storm() {
        repeat(3) {
            if (!coroutineContext.isActive) return
            setIndices(A_ODD + C_ODD, MAX); delay(80)
            setAll(0); delay(60)
            setIndices(A_EVEN + C_EVEN + B, MAX); delay(80)
            setAll(0); delay(120)
        }
        setIndices(A + B + C, MAX); delay(150)
        setAll(0)
    }

    /** Calor extremo — pulso lento solo arco A */
    private suspend fun heat() {
        repeat(3) {
            if (!coroutineContext.isActive) return
            for (step in 0..30) {
                if (!coroutineContext.isActive) return
                setIndices(A, MAX * step / 30); delay(50)
            }
            delay(500)
            for (step in 30 downTo 0) {
                if (!coroutineContext.isActive) return
                setIndices(A, MAX * step / 30); delay(50)
            }
            delay(300)
        }
    }

    /** Viento — 1 punto girando rápido en el anillo */
    private suspend fun wind() {
        ringRotation(points = 1, stepMs = 20L, reps = 4, brightness = MAX, tail = 5)
    }

    /** Frío — respiración lenta los tres arcos */
    private suspend fun cold() {
        if (!coroutineContext.isActive) return
        for (step in 0..40) {
            if (!coroutineContext.isActive) return
            setIndices(A + B + C, MID * step / 40); delay(50)
        }
        delay(1500)
        for (step in 40 downTo 0) {
            if (!coroutineContext.isActive) return
            setIndices(A + B + C, MID * step / 40); delay(50)
        }
    }

    /** Neblina — tenue estático los tres arcos */
    private suspend fun fog() {
        if (!coroutineContext.isActive) return
        for (step in 0..60) {
            if (!coroutineContext.isActive) return
            setIndices(A + B + C, DIM * step / 60); delay(50)
        }
        delay(4000)
        for (step in 60 downTo 0) {
            if (!coroutineContext.isActive) return
            setIndices(A + B + C, DIM * step / 60); delay(50)
        }
    }

    /** Aguanieve — 1 destello a la vez en posición aleatoria del anillo */
    private suspend fun sleet() {
        val random = kotlin.random.Random
        repeat(12) {
            if (!coroutineContext.isActive) return
            val pos = random.nextInt(RING_SIZE)
            val colors = IntArray(SIZE) { 0 }
            colors[RING[pos]] = MAX
            // pequeño halo de 1 vecino
            colors[RING[(pos - 1 + RING_SIZE) % RING_SIZE]] = MID / 2
            colors[RING[(pos + 1) % RING_SIZE]]             = MID / 2
            try { manager?.setFrameColors(colors) } catch (e: Exception) { }
            delay(180)
            setAll(0)
            delay(random.nextLong(80, 220))
        }
    }

    /** Nieve — destellos dispersos, más lento */
    private suspend fun snow() {
        val random  = kotlin.random.Random
        val allSegs = RING.indices.toList().shuffled(random)

        repeat(4) {
            if (!coroutineContext.isActive) return
            // Encender de a 3 segmentos aleatorios con pausa larga
            allSegs.chunked(3).take(6).forEach { chunk ->
                if (!coroutineContext.isActive) return
                val colors = IntArray(SIZE) { 0 }
                chunk.forEach { pos -> colors[RING[pos]] = MAX }
                try { manager?.setFrameColors(colors) } catch (e: Exception) { }
                delay(350)
                setAll(0)
                delay(200)
            }
        }
    }

    /** Evento próximo — secuencial suave C→A→B */
    private suspend fun upcomingEvent() {
        repeat(3) {
            if (!coroutineContext.isActive) return
            fadeInGroup(C);  if (!coroutineContext.isActive) return; delay(200); fadeOutGroup(C); delay(150)
            fadeInGroup(A);  if (!coroutineContext.isActive) return; delay(200); fadeOutGroup(A); delay(150)
            fadeInGroup(B);  if (!coroutineContext.isActive) return; delay(200); fadeOutGroup(B); delay(500)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun fadeInGroup(group: List<Int>, steps: Int = 20, stepMs: Long = 40L) {
        for (step in 0..steps) {
            if (!coroutineContext.isActive) return
            setIndices(group, MAX * step / steps); delay(stepMs)
        }
    }

    private suspend fun fadeOutGroup(group: List<Int>, steps: Int = 20, stepMs: Long = 40L) {
        for (step in steps downTo 0) {
            if (!coroutineContext.isActive) return
            setIndices(group, MAX * step / steps); delay(stepMs)
        }
    }

    private suspend fun fadeOutAll(fromBrightness: Int, steps: Int = 20, stepMs: Long = 40L) {
        for (step in steps downTo 0) {
            if (!coroutineContext.isActive) return
            setAll(fromBrightness * step / steps); delay(stepMs)
        }
    }

    private fun setIndex(index: Int, brightness: Int) {
        if (!isReady() || index !in 0 until SIZE) return
        try {
            val colors = IntArray(SIZE) { 0 }
            colors[index] = brightness
            manager?.setFrameColors(colors)
        } catch (e: Exception) { Log.e(tag, "setIndex error: ${e.message}") }
    }

    private fun setIndices(indices: List<Int>, brightness: Int) {
        if (!isReady()) return
        try {
            val colors = IntArray(SIZE) { 0 }
            indices.forEach { if (it in 0 until SIZE) colors[it] = brightness }
            manager?.setFrameColors(colors)
        } catch (e: Exception) { Log.e(tag, "setIndices error: ${e.message}") }
    }

    private fun setAll(brightness: Int) {
        if (!isReady()) return
        try {
            manager?.setFrameColors(IntArray(SIZE) { brightness })
        } catch (e: Exception) { Log.e(tag, "setAll error: ${e.message}") }
    }

    private fun turnOff() {
        if (!isReady()) return
        try { manager?.turnOff() } catch (e: Exception) { Log.e(tag, "turnOff error: ${e.message}") }
    }

    private fun isReady() = isConnected && isRegistered

    private fun isWeatherNotIdeal(weather: WeatherState): Boolean = when {
        weather.rainProbability >= 40                    -> true
        weather.windSpeedKmh    >= 35                    -> true
        weather.tempCelsius     >= 35                    -> true
        weather.tempCelsius     <= 8                     -> true
        weather.condition == WeatherCondition.STORM      -> true
        weather.condition == WeatherCondition.HEAVY_RAIN -> true
        weather.condition == WeatherCondition.FOG        -> true
        weather.condition == WeatherCondition.SNOW       -> true
        weather.condition == WeatherCondition.SLEET      -> true
        else                                             -> false
    }
}