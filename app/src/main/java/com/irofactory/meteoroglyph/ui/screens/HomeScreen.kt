package com.irofactory.meteoroglyph.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irofactory.meteoroglyph.data.glyph.GlyphRepository
import com.irofactory.meteoroglyph.ui.components.OutfitChips
import com.irofactory.meteoroglyph.ui.components.OutfitItem
import com.irofactory.meteoroglyph.ui.components.OutfitItemState
import com.irofactory.meteoroglyph.ui.components.WeatherStrip
import com.irofactory.meteoroglyph.ui.theme.AccentGreen
import com.irofactory.meteoroglyph.ui.theme.SpaceMono
import com.irofactory.meteoroglyph.ui.theme.TextSecondary

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val repo    = remember { GlyphRepository(context) }
    val glyph   = remember { repo.getGlyph("weather", "partly_cloudy") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppHeader()

        val allGlyphs = remember { repo.loadCategory("weather") }
        Log.d("GLYPH", "glifo cargado: ${glyph?.name}, rows: ${glyph?.rows}, cols: ${glyph?.cols}")

        WeatherStrip(
            temp       = 24,
            condition  = "parcialmente nublado",
            rainWindow = "14:00–16:00",
            nextEvent  = "trabajo · 13:30",
            glyph      = glyph
        )

        // Datos de ejemplo — después vendrán del ViewModel
        OutfitChips(
            glyphRepo = repo,
            items = listOf(
                OutfitItem("camisa ligera",  "clothes",     "top_shirt",       OutfitItemState.RECOMMENDED),
                OutfitItem("pantalón largo", "clothes",     "bottom_pants",    OutfitItemState.RECOMMENDED),
                OutfitItem("tenis",          "clothes",     "shoe_sneaker",    OutfitItemState.RECOMMENDED),
                OutfitItem("paraguas",       "accessories", "umbrella",        OutfitItemState.CONDITIONAL, "lluvia 14:00"),
                OutfitItem("short",          "clothes",     "bottom_shorts",   OutfitItemState.BLOCKED,     "lluvia"),
                OutfitItem("falda",          "clothes",     "bottom_skirt",    OutfitItemState.BLOCKED,     "lluvia"),
                OutfitItem("sandalias",      "clothes",     "shoe_sandal",     OutfitItemState.BLOCKED,     "lluvia"),
                OutfitItem("lentes de sol",  "accessories", "sunglasses",      OutfitItemState.NEUTRAL),
            )
        )
    }
}

@Composable
private fun AppHeader() {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row {
            Text(
                text  = "meteor",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFFF0F0F0)
            )
            Text(
                text  = "oglyph",
                style = MaterialTheme.typography.headlineLarge,
                color = AccentGreen
            )
        }
        Text(
            text  = "OUTFIT · CLIMATE · TRANSIT",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}