package com.irofactory.meteoroglyph.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irofactory.meteoroglyph.ui.components.OutfitChips
import com.irofactory.meteoroglyph.ui.components.OutfitItem
import com.irofactory.meteoroglyph.ui.components.OutfitItemState
import com.irofactory.meteoroglyph.ui.components.WeatherStrip
import com.irofactory.meteoroglyph.ui.theme.AccentGreen
import com.irofactory.meteoroglyph.ui.theme.SpaceMono
import com.irofactory.meteoroglyph.ui.theme.TextSecondary

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppHeader()

        WeatherStrip(
            temp       = 24,
            condition  = "parcialmente nublado",
            rainWindow = "14:00–16:00",
            nextEvent  = "trabajo · 13:30"
        )

        // Datos de ejemplo — después vendrán del ViewModel
        OutfitChips(
            items = listOf(
                OutfitItem("camisa ligera",    "👕", OutfitItemState.RECOMMENDED),
                OutfitItem("pantalón largo",   "👖", OutfitItemState.RECOMMENDED),
                OutfitItem("tenis",            "👟", OutfitItemState.RECOMMENDED),
                OutfitItem("paraguas",         "☂️", OutfitItemState.CONDITIONAL, "lluvia 14:00"),
                OutfitItem("short",            "🩳", OutfitItemState.BLOCKED,     "lluvia"),
                OutfitItem("falda",            "👗", OutfitItemState.BLOCKED,     "lluvia"),
                OutfitItem("sandalias",        "🩴", OutfitItemState.BLOCKED,     "lluvia"),
                OutfitItem("lentes de sol",    "🕶️", OutfitItemState.NEUTRAL),
            )
        )
    }
}

@Composable
private fun AppHeader() {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row {
            Text(
                text       = "meteor",
                fontFamily = SpaceMono,
                fontSize   = 20.sp,
                color      = Color(0xFFF0F0F0)
            )
            Text(
                text       = "oglyph",
                fontFamily = SpaceMono,
                fontSize   = 20.sp,
                color      = AccentGreen
            )
        }
        Text(
            text          = "OUTFIT · CLIMATE · TRANSIT",
            fontFamily    = SpaceMono,
            fontSize      = 9.sp,
            color         = TextSecondary,
            letterSpacing = 0.14.sp
        )
    }
}