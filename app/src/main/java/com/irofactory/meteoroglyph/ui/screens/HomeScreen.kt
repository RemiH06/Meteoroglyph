package com.irofactory.meteoroglyph.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irofactory.meteoroglyph.data.glyph.GlyphRepository
import com.irofactory.meteoroglyph.data.weather.WeatherCondition
import com.irofactory.meteoroglyph.data.weather.WeatherState
import com.irofactory.meteoroglyph.ui.components.*
import com.irofactory.meteoroglyph.ui.theme.*
import com.irofactory.meteoroglyph.viewmodel.HomeViewModel
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit = {},
    vm: HomeViewModel = viewModel()
) {
    val context   = LocalContext.current
    val uiState   by vm.uiState.collectAsStateWithLifecycle()
    val glyphRepo = remember { GlyphRepository(context) }

    // Pedir permiso de calendario
    val calendarPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refresh() }

    LaunchedEffect(Unit) {
        calendarPermission.launch(Manifest.permission.READ_CALENDAR)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppHeader(
            onSettingsClick = onNavigateToSettings,
            glyphRepo       = glyphRepo
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentGreen)
                }
            }
            uiState.error != null -> {
                Text(
                    text       = uiState.error!!,
                    fontFamily = SpaceMono,
                    fontSize   = 11.sp,
                    color      = DangerRed
                )
            }
            uiState.weather != null -> {
                val weather = uiState.weather!!
                val weatherGlyph = remember(weather.condition) {
                    glyphRepo.getGlyph("weather", weather.condition.toGlyphName())
                }
                val nextEventStr = uiState.nextEvent?.let { event ->
                    val time = event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    "${event.title} · $time"
                }

                WeatherStrip(
                    temp       = weather.tempCelsius,
                    condition  = conditionLabel(weather),
                    rainWindow = weather.rainWindow,
                    nextEvent  = nextEventStr,
                    glyph      = weatherGlyph
                )

                OutfitChips(
                    glyphRepo = glyphRepo,
                    items     = uiState.outfitItems
                )
            }
        }
    }
}

private fun conditionLabel(weather: WeatherState): String = when (weather.condition) {
    WeatherCondition.SUNNY               -> "despejado"
    WeatherCondition.CLEAR_NIGHT         -> "noche despejada"
    WeatherCondition.PARTLY_CLOUDY,
    WeatherCondition.PARTLY_CLOUDY_NIGHT -> "parcialmente nublado"
    WeatherCondition.MOSTLY_CLOUDY,
    WeatherCondition.MOSTLY_CLOUDY_NIGHT -> "mayormente nublado"
    WeatherCondition.OVERCAST            -> "nublado"
    WeatherCondition.DRIZZLE             -> "llovizna"
    WeatherCondition.RAIN                -> "lluvia"
    WeatherCondition.HEAVY_RAIN          -> "lluvia fuerte"
    WeatherCondition.STORM               -> "tormenta"
    WeatherCondition.WIND                -> "viento fuerte"
    WeatherCondition.FOG                 -> "neblina"
    WeatherCondition.SNOW                -> "nieve"
    WeatherCondition.SLEET               -> "aguanieve"
    WeatherCondition.HOT                 -> "calor extremo"
    WeatherCondition.COLD                -> "frío"
}

@Composable
private fun AppHeader(
    onSettingsClick: () -> Unit,
    glyphRepo: GlyphRepository
) {
    val mc = metroColors
    val settingsGlyph = remember { glyphRepo.getGlyph("ui", "settings") }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Row {
                    Text("meteoro", fontFamily = SpaceMono, fontSize = 20.sp, color = mc.textPrimary)
                    Text("glyph",   fontFamily = SpaceMono, fontSize = 20.sp, color = mc.accent)
                }
                Text(
                    text  = "OUTFIT · CLIMATE · TRANSIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = mc.textSecondary
                )
            }
            if (settingsGlyph != null) {
                IconButton(onClick = onSettingsClick) {
                    GlyphRenderer(
                        glyph    = settingsGlyph,
                        tint     = mc.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}