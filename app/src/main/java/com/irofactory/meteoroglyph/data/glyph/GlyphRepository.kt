package com.irofactory.meteoroglyph.data.glyph

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

class GlyphRepository(private val context: Context) {

    private val cache = mutableMapOf<String, Map<String, ParsedGlyph>>()

    fun loadCategory(filename: String): Map<String, ParsedGlyph> {
        cache[filename]?.let { return it }

        val json = context.assets
            .open("glyphs/$filename.json")
            .bufferedReader()
            .readText()

        val root     = JSONObject(json)
        val glyphsObj = root.getJSONObject("glyphs")
        val result   = mutableMapOf<String, ParsedGlyph>()

        glyphsObj.keys().forEach { key ->
            val g       = glyphsObj.getJSONObject(key)
            val palette = parsePalette(g.getJSONArray("palette"))
            val data    = parseData(g.getJSONArray("data"), palette)
            result[g.getString("name")] = ParsedGlyph(
                name     = g.getString("name"),
                category = g.getString("category"),
                cols     = g.getInt("cols"),
                rows     = g.getInt("rows"),
                dots     = data
            )
        }

        cache[filename] = result
        return result
    }

    fun getGlyph(filename: String, name: String): ParsedGlyph? =
        loadCategory(filename)[name]

    private fun parsePalette(arr: JSONArray): List<Color> =
        (0 until arr.length()).map { hexToColor(arr.getString(it)) }

    private fun parseData(arr: JSONArray, palette: List<Color>): List<List<Color?>> {
        val result = mutableListOf<List<Color?>>()
        for (r in 0 until arr.length()) {
            val row    = arr.getJSONArray(r)
            val parsed = mutableListOf<Color?>()
            for (c in 0 until row.length()) {
                val cell = row.opt(c)
                parsed.add(when {
                    cell == null || cell == JSONObject.NULL -> null
                    cell is Int    -> palette.getOrNull(cell)
                    cell is JSONObject && cell.has("custom") ->
                        hexToColor(cell.getString("custom"))
                    else -> null
                })
            }
            result.add(parsed)
        }
        return result
    }

    private fun hexToColor(hex: String): Color {
        val clean = hex.trimStart('#')
        val long  = clean.toLong(16)
        return when (clean.length) {
            6 -> Color(
                red   = ((long shr 16) and 0xFF) / 255f,
                green = ((long shr 8)  and 0xFF) / 255f,
                blue  = (long          and 0xFF) / 255f
            )
            8 -> Color(
                alpha = ((long shr 24) and 0xFF) / 255f,
                red   = ((long shr 16) and 0xFF) / 255f,
                green = ((long shr 8)  and 0xFF) / 255f,
                blue  = (long          and 0xFF) / 255f
            )
            else -> Color.White
        }
    }
}

data class ParsedGlyph(
    val name: String,
    val category: String,
    val cols: Int,
    val rows: Int,
    val dots: List<List<Color?>>
)