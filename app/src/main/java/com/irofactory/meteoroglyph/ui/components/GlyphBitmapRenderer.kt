package com.irofactory.meteoroglyph.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.irofactory.meteoroglyph.data.glyph.ParsedGlyph

object GlyphBitmapRenderer {

    fun render(
        glyph:      ParsedGlyph,
        sizePx:     Int,
        tintArgb:   Int? = null,
        bgArgb:     Int  = android.graphics.Color.TRANSPARENT
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fondo
        if (bgArgb != android.graphics.Color.TRANSPARENT) {
            canvas.drawColor(bgArgb)
        }

        val rows   = glyph.dots.size
        val cols   = glyph.dots.firstOrNull()?.size ?: return bitmap
        val cellSz = sizePx.toFloat() / cols.coerceAtLeast(rows)
        val dotR   = cellSz * 0.38f  // radio del círculo
        val gap    = cellSz * 0.22f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val color = glyph.dots[r][c] ?: continue
                val cx    = c * cellSz + cellSz / 2f
                val cy    = r * cellSz + cellSz / 2f

                paint.color = tintArgb ?: composeColorToArgb(color)
                canvas.drawCircle(cx, cy, dotR, paint)
            }
        }

        return bitmap
    }

    /**
     * Convierte un glifo directamente a un arreglo de brillo (0-255 por celda),
     * uno a uno, sin pasar por Bitmap. Pensado para matrices físicas reales
     * (Glyph Matrix) donde cada celda del JSON es un LED, no un círculo dibujado.
     * El glifo debe tener exactamente `size` columnas y filas.
     *
     * El LED es monocromático, así que el color de paleta no se usa como brillo
     * (algunos colores del set, como el gris de contorno, se ven correctos en
     * pantalla pero saldrían muy tenues si se leyera su luminancia). Cualquier
     * celda no nula se manda a brillo máximo, solo se respeta el alpha si el
     * glifo define explícitamente una celda semitransparente.
     */
    fun renderToMatrixArray(glyph: ParsedGlyph, size: Int): IntArray {
        val result = IntArray(size * size)
        val rows = glyph.dots.size
        val cols = glyph.dots.firstOrNull()?.size ?: return result
        for (r in 0 until minOf(rows, size)) {
            for (c in 0 until minOf(cols, size)) {
                val color = glyph.dots[r][c] ?: continue
                result[r * size + c] = (255f * color.alpha).toInt().coerceIn(0, 255)
            }
        }
        return result
    }

    private fun composeColorToArgb(color: androidx.compose.ui.graphics.Color): Int {
        return android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red   * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue  * 255).toInt()
        )
    }

    // Convierte un Color de metro_theme a ARGB para usar fuera de Compose
    fun metroArgb(hex: String): Int {
        val clean = hex.trimStart('#')
        return android.graphics.Color.parseColor("#$clean")
    }
}