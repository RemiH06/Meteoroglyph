package com.irofactory.meteoroglyph.data.glyph

data class GlyphLibrary(
    val meta: GlyphMeta,
    val glyphs: Map<String, Glyph>
)

data class GlyphMeta(
    val version: String,
    val app: String,
    val palette: List<PaletteColor>
)

data class PaletteColor(
    val name: String,
    val hex: String
)

data class Glyph(
    val id: String,
    val name: String,
    val category: String,
    val cols: Int,
    val rows: Int,
    val palette: List<String>,
    val data: List<List<Any?>>   // null | Int | Map("custom" -> String)
)