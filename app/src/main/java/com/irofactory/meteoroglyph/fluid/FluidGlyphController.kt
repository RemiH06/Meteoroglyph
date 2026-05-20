package com.irofactory.meteoroglyph.fluid

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.math.*

/**
 * FluidGlyphController
 * ───────────────────────────────────────────────────────────────────────────
 * Mapa angular real de los arcos (medido en imagen, 0°=derecha antihorario):
 *
 *   Arco C (índices 0–19):  10° a  60° — superior izquierda
 *   Arco A (índices 20–30): 165° a 215° — inferior izquierda / abajo
 *   Arco B (índices 31–35): 305° a 330° — derecha
 *
 * El fluido existe en todo el círculo. Los LEDs son ventanas que muestran
 * la densidad en su posición angular. Si el fluido está donde no hay LED,
 * simplemente no se ve pero sigue existiendo en la simulación.
 *
 * Comportamiento horizontal (teléfono boca arriba/abajo):
 * Cuando |az| domina sobre ax y ay, la gravedad 2D se reduce y se aplica
 * una fuerza de dispersión que reparte las partículas uniformemente.
 * El brillo total se conserva proporcional al fillRatio.
 */
class FluidGlyphController(private val context: Context) {

    private val tag   = "FluidGlyphController"
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job:  Job? = null
    private var running    = false

    // ── Acelerómetro ──────────────────────────────────────────────────────────
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    @Volatile private var rawAx = 0f
    @Volatile private var rawAy = 9.8f
    @Volatile private var rawAz = 0f

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            rawAx = event.values[0]
            rawAy = event.values[1]
            rawAz = event.values[2]
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // ── Dimensiones de la simulación ──────────────────────────────────────────
    private val SIM_COLS   = 12
    private val SIM_ROWS   = 12
    private val SIM_CX     = SIM_COLS / 2f
    private val SIM_CY     = SIM_ROWS / 2f
    private val SIM_RADIUS = SIM_COLS / 2f - 0.8f

    // ── Posiciones cartesianas de cada LED ────────────────────────────────────
    // Ángulos reales medidos en el hardware (grados → radianes)
    // Convenio: 0°=derecha, antihorario, Y invertido para pantalla
    private val ledAngles: DoubleArray = buildAngles()
    private val ledPositions: Array<FloatArray> = buildPositions()

    private fun buildAngles(): DoubleArray {
        val a = DoubleArray(36)
        // Arco C: 10° a 60° — 20 segmentos
        val cStart = Math.toRadians(10.0)
        val cEnd   = Math.toRadians(60.0)
        for (i in 0..19) a[i] = cStart + (cEnd - cStart) * i / 19.0

        // Arco A: 165° a 215° — 11 segmentos
        val aStart = Math.toRadians(165.0)
        val aEnd   = Math.toRadians(215.0)
        for (i in 0..10) a[20 + i] = aStart + (aEnd - aStart) * i / 10.0

        // Arco B: 305° a 330° — 5 segmentos
        val bStart = Math.toRadians(305.0)
        val bEnd   = Math.toRadians(330.0)
        for (i in 0..4) a[31 + i] = bStart + (bEnd - bStart) * i / 4.0

        return a
    }

    private fun buildPositions(): Array<FloatArray> = Array(36) { idx ->
        val ang = ledAngles[idx]
        floatArrayOf(
            (SIM_CX + SIM_RADIUS * cos(ang)).toFloat(),
            (SIM_CY - SIM_RADIUS * sin(ang)).toFloat()  // Y invertido
        )
    }

    // ── Glyph Manager ─────────────────────────────────────────────────────────
    private val glyphManager = GlyphManager.getInstance(context)

    // ── Iniciar ───────────────────────────────────────────────────────────────
    fun start(params: FluidParams? = null) {
        if (running) return
        running = true

        val sim = FluidSimulation(
            cols            = SIM_COLS,
            rows            = SIM_ROWS,
            fillRatio       = params?.fillRatio       ?: 0.30f,
            viscosity       = params?.viscosity       ?: 0.50f,
            stiffness       = params?.stiffness       ?: 2.00f,
            restitution     = params?.restitution     ?: 0.30f,
            smoothingRadius = params?.smoothingRadius ?: 1.80f,
            circularBounds  = true
        )

        sensorManager.registerListener(
            sensorListener, accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )

        job = scope.launch {
            try {
                glyphManager.openSession()

                while (coroutineContext.isActive) {
                    // ── Calcular gravedad efectiva ────────────────────────────
                    val ax = rawAx
                    val ay = rawAy
                    val az = rawAz

                    // Magnitud horizontal vs vertical
                    val hMag = sqrt(ax * ax + ay * ay)  // componente horizontal
                    val vMag = abs(az)                  // componente vertical

                    // Factor de horizontalidad: 0 = vertical, 1 = horizontal
                    val totalMag = sqrt(hMag * hMag + vMag * vMag).coerceAtLeast(0.1f)
                    val horizFactor = (vMag / totalMag).coerceIn(0f, 1f)

                    // Gravedad 2D reducida cuando el teléfono está horizontal
                    sim.gravX = -ax * (1f - horizFactor) * 2.0f
                    sim.gravY =  ay * (1f - horizFactor) * 2.0f

                    // Cuando es horizontal, aplicar fuerza centrífuga suave
                    // que dispersa las partículas hacia el borde del círculo
                    if (horizFactor > 0.3f) {
                        applyDispersionForce(sim, horizFactor)
                    }

                    sim.step(dt = 0.033f)

                    val colors = computeLedBrightness(sim)
                    try {
                        glyphManager.setFrameColors(colors)
                    } catch (e: GlyphException) {
                        Log.e(tag, "Glyph error: ${e.message}")
                    }

                    delay(33)
                }

                glyphManager.closeSession()
            } catch (e: Exception) {
                Log.e(tag, "Error en simulación: ${e.message}")
            }
        }
    }

    // ── Fuerza de dispersión cuando el teléfono está horizontal ───────────────
    // Empuja las partículas hacia afuera desde el centro del círculo,
    // distribuyéndolas uniformemente por el anillo
    private fun applyDispersionForce(sim: FluidSimulation, intensity: Float) {
        val strength = intensity * 0.8f
        for (p in sim.particles) {
            val dx   = p.x - SIM_CX
            val dy   = p.y - SIM_CY
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.1f)
            // Fuerza radial hacia el borde
            val targetDist = SIM_RADIUS * 0.7f
            val forceMag   = (targetDist - dist) * strength * 0.1f
            p.vx += (dx / dist) * forceMag
            p.vy += (dy / dist) * forceMag
        }
    }

    // ── Detener ───────────────────────────────────────────────────────────────
    fun stop() {
        running = false
        job?.cancel()
        job = null
        sensorManager.unregisterListener(sensorListener)
        try { glyphManager.turnOff() } catch (e: Exception) { }
    }

    fun isRunning() = running

    // ── Brillo de cada LED según densidad del fluido ──────────────────────────
    private fun computeLedBrightness(sim: FluidSimulation): IntArray {
        val colors       = IntArray(36) { 0 }
        val maxBright    = 4000
        val sampleRadius = sim.smoothingRadius

        for (ledIdx in 0..35) {
            val lx = ledPositions[ledIdx][0]
            val ly = ledPositions[ledIdx][1]
            var density = 0f

            for (p in sim.particles) {
                val dx   = p.x - lx
                val dy   = p.y - ly
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < sampleRadius) {
                    val w = (1f - dist / sampleRadius).pow(2)
                    density += w
                }
            }

            // Normalizar conservando el fillRatio como brillo máximo
            val norm = (density / (sim.particles.size * 0.12f)).coerceIn(0f, 1f)
            colors[ledIdx] = (norm * maxBright).toInt()
        }

        return colors
    }
}

// ── Parámetros configurables ──────────────────────────────────────────────────
data class FluidParams(
    val fillRatio:       Float = 0.30f,
    val viscosity:       Float = 0.50f,
    val stiffness:       Float = 1.00f,
    val restitution:     Float = 0.40f,
    val smoothingRadius: Float = 2.50f,
    val particleCount:   Int   = 80
) {
    fun applyTo(sim: FluidSimulation) {
        sim.fillRatio       = fillRatio
        sim.viscosity       = viscosity
        sim.stiffness       = stiffness
        sim.restitution     = restitution
        sim.smoothingRadius = smoothingRadius
    }
}