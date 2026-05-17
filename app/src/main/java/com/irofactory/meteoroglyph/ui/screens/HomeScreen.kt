package com.irofactory.meteoroglyph.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irofactory.meteoroglyph.ui.components.WeatherStrip
import com.irofactory.meteoroglyph.ui.theme.SpaceMono
import com.irofactory.meteoroglyph.ui.theme.AccentGreen
import com.irofactory.meteoroglyph.ui.theme.TextSecondary

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        AppHeader()

        // Weather strip con datos de ejemplo por ahora
        WeatherStrip(
            temp       = 24,
            condition  = "parcialmente nublado",
            rainWindow = "14:00–16:00",
            nextEvent  = "trabajo · 13:30"
        )
    }
}

@Composable
private fun AppHeader() {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Text(
            text       = "meteor",
            fontFamily = SpaceMono,
            fontSize   = 20.sp,
            color      = androidx.compose.ui.graphics.Color(0xFFF0F0F0)
        )
        Text(
            text       = "oglyph",
            fontFamily = SpaceMono,
            fontSize   = 20.sp,
            color      = AccentGreen
        )
    }
    Text(
        text       = "OUTFIT · CLIMATE · TRANSIT",
        fontFamily = SpaceMono,
        fontSize   = 9.sp,
        color      = TextSecondary,
        letterSpacing = 0.14.sp
    )
}