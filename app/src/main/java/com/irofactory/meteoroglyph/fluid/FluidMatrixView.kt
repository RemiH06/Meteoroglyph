package com.irofactory.meteoroglyph.fluid

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.irofactory.meteoroglyph.ui.theme.metroColors
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

/**
 * FluidMatrixView
 * ───────────────────────────────────────────────────────────────────────────
 * Composable que muestra la simulación de fluido en una matriz 25×25 circular.
 * Se actualiza en cada frame usando LaunchedEffect + withFrameMillis.
 *
 * @param params         Parámetros de la simulación
 * @param glyphActive    Si true, muestra el aro del borde encendido
 * @param onToggleGlyph  Callback al hacer click (toggle aro + LEDs físicos)
 * @param modifier       Modifier externo
 */
@Composable
fun FluidMatrixView(
    params:         FluidParams       = FluidParams(),
    glyphActive:    Boolean           = false,
    onToggleGlyph:  () -> Unit        = {},
    modifier:       Modifier          = Modifier
) {
    val context = LocalContext.current
    val mc      = metroColors

    // ── Simulación ────────────────────────────────────────────────────────────
    val sim = remember(params.fillRatio, params.particleCount) {
        FluidSimulation(
            cols            = 25,
            rows            = 25,
            fillRatio       = params.fillRatio,
            viscosity       = params.viscosity,
            stiffness       = params.stiffness,
            restitution     = params.restitution,
            smoothingRadius = params.smoothingRadius,
            circularBounds  = true
        )
    }

    // Actualizar parámetros cuando cambien
    LaunchedEffect(params) {
        sim.fillRatio       = params.fillRatio
        sim.viscosity       = params.viscosity
        sim.stiffness       = params.stiffness
        sim.restitution     = params.restitution
        sim.smoothingRadius = params.smoothingRadius
    }

    // ── Acelerómetro ──────────────────────────────────────────────────────────
    val sensorManager = remember {
        context.getSystemService(SensorManager::class.java)
    }
    val accelerometer = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                sim.gravX = -event.values[0]
                sim.gravY = event.values[1]
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // ── Grid rasterizado ──────────────────────────────────────────────────────
    var grid by remember { mutableStateOf(Array(25) { FloatArray(25) }) }
    val mask  = remember { sim.circularMask() }

    // Loop de animación — se ejecuta en cada frame
    LaunchedEffect(Unit) {
        var lastTime = withFrameMillis { it }
        while (isActive) {
            val currentTime = withFrameMillis { it }
            val dt = ((currentTime - lastTime) / 1000f).coerceIn(0.005f, 0.08f)
            lastTime        = currentTime
            sim.step(dt)
            grid = sim.rasterize()
        }
    }

    // ── Colores del fluido según tema ─────────────────────────────────────────
    val fluidColor   = mc.accent
    val emptyColor   = mc.surface2
    val borderColor  = if (glyphActive) mc.accent else mc.border
    val borderWidth  = if (glyphActive) 2.dp else 0.5.dp

    // ── Render ────────────────────────────────────────────────────────────────
    Box(
        modifier        = modifier
            .fillMaxWidth(0.72f)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .border(borderWidth, borderColor, CircleShape)
                .clickable { onToggleGlyph() }
                .padding(4.dp)
        ) {
            val cols    = 25
            val rows    = 25
            val cellW   = size.width  / cols
            val cellH   = size.height / rows
            val dotR    = cellW * 0.38f

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (!mask[r][c]) continue

                    val brightness = grid[r][c]
                    val cx2        = c * cellW + cellW / 2f
                    val cy2        = r * cellH + cellH / 2f

                    val dotColor = if (brightness > 0.01f) {
                        fluidColor.copy(alpha = brightness.coerceIn(0.05f, 1f))
                    } else {
                        emptyColor.copy(alpha = 0.4f)
                    }

                    drawCircle(
                        color  = dotColor,
                        radius = dotR,
                        center = Offset(cx2, cy2)
                    )
                }
            }
        }
    }
}