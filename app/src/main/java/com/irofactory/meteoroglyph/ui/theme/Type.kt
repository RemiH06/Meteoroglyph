package com.irofactory.meteoroglyph.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.irofactory.meteoroglyph.R

// ── Familias tipográficas ────────────────────────────────────────────────────

val SpaceMono = FontFamily(
    Font(R.font.spacemono_regular, FontWeight.Normal),
    Font(R.font.spacemono_bold,    FontWeight.Bold)
)

// Fuente de matriz de puntos — Nothing Ndot-57
// Créditos: github.com/xeji01/nothingfont · Nothing Tech. All Rights Reserved.
val Ndot57 = FontFamily(
    Font(R.font.ndot57_regular, FontWeight.Normal)
)

// Versión caps — para labels, badges y sección titles
val Ndot57Caps = FontFamily(
    Font(R.font.ndot57caps_regular, FontWeight.Normal)
)

// ── Tipografía de la app ─────────────────────────────────────────────────────

val Typography = Typography(

    // Temperatura grande — "24°" en WeatherStrip
    displayLarge = TextStyle(
        fontFamily = Ndot57,
        fontWeight = FontWeight.Normal,
        fontSize   = 48.sp,
        letterSpacing = (-1).sp
    ),

    // Valores numéricos medianos — métricas, horas
    displayMedium = TextStyle(
        fontFamily = Ndot57,
        fontWeight = FontWeight.Normal,
        fontSize   = 28.sp,
        letterSpacing = (-0.5).sp
    ),

    // Título de app — "meteoroglyph"
    headlineLarge = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize   = 20.sp,
        letterSpacing = (-1).sp
    ),

    // Subtítulos de sección
    headlineMedium = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize   = 14.sp,
        letterSpacing = (-0.5).sp
    ),

    // Labels de sección — "OUTFIT · CLIMATE · TRANSIT"
    labelSmall = TextStyle(
        fontFamily    = Ndot57Caps,
        fontWeight    = FontWeight.Normal,
        fontSize      = 9.sp,
        letterSpacing = 0.14.sp
    ),

    // Labels de chips y badges
    labelMedium = TextStyle(
        fontFamily    = Ndot57Caps,
        fontWeight    = FontWeight.Normal,
        fontSize      = 11.sp,
        letterSpacing = 0.08.sp
    ),

    // Cuerpo general — condición del clima, descripciones
    bodyMedium = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp
    ),

    // Cuerpo secundario — metadatos, timestamps
    bodySmall = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Normal,
        fontSize   = 11.sp
    ),

    // Mono pequeño — JSON preview, coords, datos técnicos
    bodyLarge = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Normal,
        fontSize   = 15.sp
    )
)