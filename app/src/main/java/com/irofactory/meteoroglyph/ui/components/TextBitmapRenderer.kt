package com.irofactory.meteoroglyph.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.irofactory.meteoroglyph.R

object TextBitmapRenderer {

    private var ndot57: Typeface? = null

    private fun getTypeface(context: Context): Typeface {
        if (ndot57 == null) {
            ndot57 = ResourcesCompat.getFont(context, R.font.ndot57_regular)
        }
        return ndot57 ?: Typeface.MONOSPACE
    }

    fun render(
        context:   Context,
        text:      String,
        textSizePx: Float,
        colorArgb: Int,
        maxWidthPx: Int = 0
    ): Bitmap {
        val typeface = getTypeface(context)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface  = typeface
            this.textSize  = textSizePx
            this.color     = colorArgb
        }

        val textWidth  = paint.measureText(text).toInt()
        val fm         = paint.fontMetrics
        val textHeight = (fm.descent - fm.ascent).toInt()
        val width      = if (maxWidthPx > 0) minOf(textWidth, maxWidthPx) else textWidth
        val height     = textHeight

        if (width <= 0 || height <= 0) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText(text, 0f, -fm.ascent, paint)
        return bitmap
    }
}