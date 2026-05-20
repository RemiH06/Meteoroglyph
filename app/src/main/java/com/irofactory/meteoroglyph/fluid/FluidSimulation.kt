package com.irofactory.meteoroglyph.fluid

import kotlin.math.*

/**
 * FluidSimulation
 * ───────────────────────────────────────────────────────────────────────────
 * Motor SPH (Smoothed Particle Hydrodynamics) simplificado para simular
 * un fluido en una grilla 2D.
 *
 * Coordenadas: (0,0) = esquina superior izquierda
 * Unidades: celdas de la grilla (ej. 25×25 para la pantalla, 6×6 para LEDs)
 *
 * @param cols            ancho del recipiente en celdas
 * @param rows            alto del recipiente en celdas
 * @param fillRatio       fracción del recipiente llena de fluido (0.0–1.0)
 * @param viscosity       coeficiente de viscosidad μ
 * @param stiffness       rigidez del fluido k
 * @param restitution     coeficiente de rebote en paredes (0=absorbe, 1=elástico)
 * @param smoothingRadius radio de influencia h entre partículas
 * @param particleCount   número de partículas (null = calculado por fillRatio)
 * @param circularBounds  true = recipiente circular (para la pantalla), false = rectangular (LEDs)
 */
class FluidSimulation(
    val cols:            Int     = 25,
    val rows:            Int     = 25,
    var fillRatio:       Float   = 0.30f,
    var viscosity:       Float   = 0.50f,
    var stiffness:       Float   = 1.00f,
    var restitution:     Float   = 0.40f,
    var smoothingRadius: Float   = 2.50f,
    particleCount:       Int?    = null,
    val circularBounds:  Boolean = true
) {
    // ── Partículas ────────────────────────────────────────────────────────────
    data class Particle(
        var x: Float, var y: Float,
        var vx: Float = 0f, var vy: Float = 0f,
        var density: Float = 0f,
        var pressure: Float = 0f
    )

    private val N: Int = particleCount ?: (cols * rows * fillRatio).toInt().coerceAtLeast(10)
    val particles: List<Particle>

    // ── Gravedad (actualizada desde el acelerómetro) ───────────────────────────
    var gravX: Float = 0f
    var gravY: Float = 9.8f
    var gravScale: Float = 3.5f

    // ── Densidad de reposo ────────────────────────────────────────────────────
    private val restDensity: Float get() = (N.toFloat() / (cols * rows * fillRatio)).coerceAtLeast(0.5f)

    // ── Centro y radio para bounds circulares ──────────────────────────────────
    private val cx = cols / 2f
    private val cy = rows / 2f
    private val radius = minOf(cols, rows) / 2f - 0.5f

    init {
        // Inicializar partículas distribuidas uniformemente
        particles = buildList {
            repeat(N) { i ->
                val angle = (i.toFloat() / N) * 2 * PI.toFloat()
                val r     = sqrt(i.toFloat() / N) * radius * 0.8f
                val px    = if (circularBounds) cx + r * cos(angle) else (i % cols).toFloat() + 0.5f
                val py    = if (circularBounds) cy + r * sin(angle) else (i / cols).toFloat() + 0.5f
                add(Particle(
                    x  = px.coerceIn(0.1f, cols - 0.1f),
                    y  = py.coerceIn(0.1f, rows - 0.1f)
                ))
            }
        }
    }

    // ── Kernel de suavizado W(r, h) — Poly6 ───────────────────────────────────
    private fun kernelPoly6(r: Float, h: Float): Float {
        if (r > h) return 0f
        val coeff = 315f / (64f * PI.toFloat() * h.pow(9))
        return coeff * (h * h - r * r).pow(3)
    }

    // ── Gradiente del kernel — Spiky (para presión) ───────────────────────────
    private fun kernelSpikyGrad(r: Float, h: Float): Float {
        if (r > h || r < 1e-6f) return 0f
        val coeff = -45f / (PI.toFloat() * h.pow(6))
        return coeff * (h - r).pow(2)
    }

    // ── Kernel de viscosidad ──────────────────────────────────────────────────
    private fun kernelViscosity(r: Float, h: Float): Float {
        if (r > h) return 0f
        val coeff = 45f / (PI.toFloat() * h.pow(6))
        return coeff * (h - r)
    }

    // ── Paso de simulación ────────────────────────────────────────────────────
    fun step(dt: Float = 0.016f) {
        val h = smoothingRadius

        // 1. Calcular densidades
        for (i in particles.indices) {
            val pi = particles[i]
            var density = 0f
            for (j in particles.indices) {
                val pj = particles[j]
                val dx = pi.x - pj.x
                val dy = pi.y - pj.y
                val r  = sqrt(dx * dx + dy * dy)
                density += kernelPoly6(r, h)
            }
            pi.density = density.coerceAtLeast(1e-6f)
            pi.pressure = stiffness * (pi.density - restDensity)
        }

        // 2. Calcular fuerzas y actualizar velocidades
        for (i in particles.indices) {
            val pi = particles[i]
            var fx = 0f
            var fy = 0f

            for (j in particles.indices) {
                if (i == j) continue
                val pj = particles[j]
                val dx = pi.x - pj.x
                val dy = pi.y - pj.y
                val r  = sqrt(dx * dx + dy * dy)
                if (r < 1e-6f || r > h) continue

                val nx = dx / r
                val ny = dy / r

                // Fuerza de presión
                val pressureForce = -(pi.pressure + pj.pressure) / (2f * pj.density) *
                        kernelSpikyGrad(r, h)
                fx += pressureForce * nx
                fy += pressureForce * ny

                // Fuerza de viscosidad
                val viscForce = viscosity / pj.density * kernelViscosity(r, h)
                fx += viscForce * (pj.vx - pi.vx)
                fy += viscForce * (pj.vy - pi.vy)
            }

            // Gravedad
            fx += gravX * gravScale
            fy += gravY * gravScale

            // Actualizar velocidad
            pi.vx += fx * dt / pi.density.coerceAtLeast(1e-6f)
            pi.vy += fy * dt / pi.density.coerceAtLeast(1e-6f)

            // Damping (evita velocidades infinitas)
            pi.vx *= 0.98f
            pi.vy *= 0.98f
        }

        // 3. Integrar posiciones y resolver colisiones
        for (pi in particles) {
            pi.x += pi.vx * dt
            pi.y += pi.vy * dt
            resolveCollision(pi)
        }
    }

    // ── Colisiones con paredes ────────────────────────────────────────────────
    private fun resolveCollision(p: Particle) {
        if (circularBounds) {
            // Recipiente circular
            val dx = p.x - cx
            val dy = p.y - cy
            val r  = sqrt(dx * dx + dy * dy)
            if (r > radius) {
                val nx = dx / r
                val ny = dy / r
                // Reposicionar dentro del círculo
                p.x = cx + nx * (radius - 0.01f)
                p.y = cy + ny * (radius - 0.01f)
                // Reflejar velocidad
                val dot = p.vx * nx + p.vy * ny
                p.vx = (p.vx - 2f * dot * nx) * restitution
                p.vy = (p.vy - 2f * dot * ny) * restitution
            }
        } else {
            // Recipiente rectangular
            if (p.x < 0.1f)         { p.x = 0.1f;             p.vx = abs(p.vx) * restitution }
            if (p.x > cols - 0.1f)  { p.x = cols - 0.1f;      p.vx = -abs(p.vx) * restitution }
            if (p.y < 0.1f)         { p.y = 0.1f;             p.vy = abs(p.vy) * restitution }
            if (p.y > rows - 0.1f)  { p.y = rows - 0.1f;      p.vy = -abs(p.vy) * restitution }
        }
    }

    // ── Rasterizar: partículas → mapa de brillo por celda ────────────────────
    /**
     * Devuelve un array [rows][cols] con valores 0.0–1.0 representando
     * la densidad normalizada en cada celda.
     * La suma total de brillo se conserva proporcional al fillRatio.
     */
    fun rasterize(): Array<FloatArray> {
        val grid = Array(rows) { FloatArray(cols) { 0f } }
        val h    = smoothingRadius.coerceAtMost(2.0f)  // kernel más pequeño para rasterización

        for (p in particles) {
            val minC = (p.x - h).toInt().coerceAtLeast(0)
            val maxC = (p.x + h).toInt().coerceAtMost(cols - 1)
            val minR = (p.y - h).toInt().coerceAtLeast(0)
            val maxR = (p.y + h).toInt().coerceAtMost(rows - 1)

            for (r in minR..maxR) {
                for (c in minC..maxC) {
                    val dx   = p.x - (c + 0.5f)
                    val dy   = p.y - (r + 0.5f)
                    val dist = sqrt(dx * dx + dy * dy)
                    grid[r][c] += kernelPoly6(dist, h)
                }
            }
        }

        // Normalizar: el brillo máximo posible es cuando todas las partículas
        // están concentradas en un punto
        val maxVal = grid.flatMap { it.toList() }.maxOrNull()?.takeIf { it > 0f } ?: 1f
        for (r in 0 until rows) for (c in 0 until cols) {
            grid[r][c] = (grid[r][c] / maxVal).coerceIn(0f, 1f)
        }

        return grid
    }

    // ── Máscara circular para la pantalla ─────────────────────────────────────
    fun circularMask(): Array<BooleanArray> {
        val mask = Array(rows) { BooleanArray(cols) { false } }
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val dx = c + 0.5f - cx
                val dy = r + 0.5f - cy
                mask[r][c] = sqrt(dx * dx + dy * dy) <= radius
            }
        }
        return mask
    }
}