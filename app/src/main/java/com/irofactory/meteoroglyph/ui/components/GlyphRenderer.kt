package com.irofactory.meteoroglyph.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.irofactory.meteoroglyph.data.glyph.ParsedGlyph

@Composable
fun GlyphRenderer(
    modifier: Modifier = Modifier,
    glyph: ParsedGlyph,
    tint: Color? = null,
    emptyColor: Color = Color(0x00000000)
) {
    Canvas(modifier = modifier) {
        val rows   = glyph.dots.size
        val cols   = glyph.dots.firstOrNull()?.size ?: return@Canvas
        // Calcula dotSize y gap para llenar el espacio disponible
        val dotSize = (size.width / cols) * 0.78f
        val gap     = (size.width / cols) * 0.22f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x    = c * (dotSize + gap)
                val y    = r * (dotSize + gap)
                val color = glyph.dots[r][c]
                val paintColor = when {
                    color != null && tint != null -> tint
                    color != null                 -> color
                    else                          -> emptyColor
                }
                drawCircle(
                    color  = paintColor,
                    radius = dotSize / 2f,
                    center = androidx.compose.ui.geometry.Offset(
                        x + dotSize / 2f,
                        y + dotSize / 2f
                    )
                )
            }
        }
    }
}

// Calcula el tamaño total que ocupa el glifo — útil para el modifier
fun glyphSize(cols: Int, rows: Int, dotSize: Float = 8f, gap: Float = 2f) =
    Pair(
        cols * (dotSize + gap) - gap,
        rows * (dotSize + gap) - gap
    )