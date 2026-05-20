package com.irofactory.meteoroglyph.ui.screens

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irofactory.meteoroglyph.data.glyph.GlyphRepository
import com.irofactory.meteoroglyph.data.glyph.ParsedGlyph
import com.irofactory.meteoroglyph.data.settings.CalendarKeyword
import com.irofactory.meteoroglyph.data.settings.Location
import com.irofactory.meteoroglyph.data.weather.WeatherCondition
import com.irofactory.meteoroglyph.data.weather.WeatherState
import com.irofactory.meteoroglyph.ui.components.GlyphRenderer
import com.irofactory.meteoroglyph.ui.theme.*
import com.irofactory.meteoroglyph.viewmodel.SettingsViewModel
import com.nothing.ketchum.GlyphManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    vm: SettingsViewModel = viewModel()
) {
    val context   = LocalContext.current
    val glyphRepo = remember { GlyphRepository(context) }
    val settings by vm.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(top = 16.dp)) {
            val mc = metroColors
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
                        text  = "CONFIGURACIÓN",
                        style = MaterialTheme.typography.labelSmall,
                        color = mc.textSecondary
                    )
                }
                val arrowGlyph = remember { glyphRepo.getGlyph("ui", "arrow_right") }
                if (arrowGlyph != null) {
                    IconButton(onClick = onNavigateBack) {
                        GlyphRenderer(
                            glyph    = arrowGlyph,
                            tint     = mc.textSecondary,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer { rotationZ = 180f }
                        )
                    }
                }
            }
        }

        // ── Ubicación ─────────────────────────────────────────────────────────
        SettingsSection(title = "UBICACIÓN") {
            SettingsRow(label = "Usar GPS automático") {
                Switch(
                    checked         = settings.useGps,
                    onCheckedChange = { vm.setUseGps(it) },
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor   = metroColors.background,
                        checkedTrackColor   = metroColors.accent,
                        uncheckedThumbColor = metroColors.textSecondary,
                        uncheckedTrackColor = metroColors.surface2
                    )
                )
            }
            LocationEditor(
                title    = "Casa",
                location = Location(settings.homeLabel, settings.homeLat, settings.homeLon),
                onSave   = { loc ->
                    vm.setHomeLabel(loc.label)
                    vm.setHomeLat(loc.lat)
                    vm.setHomeLon(loc.lon)
                }
            )
        }

        // ── Trabajos ──────────────────────────────────────────────────────────
        SettingsSection(title = "TRABAJOS (máx. 3)") {
            settings.workplaces.forEachIndexed { i, loc ->
                LocationEditor(
                    title    = "Trabajo ${i + 1}",
                    location = loc,
                    onSave   = { vm.updateWorkplace(i, it) },
                    onDelete = { vm.removeWorkplace(i) }
                )
            }
            if (settings.workplaces.size < 3) {
                AddButton("Agregar trabajo") {
                    vm.addWorkplace(Location("Trabajo ${settings.workplaces.size + 1}", 0.0, 0.0))
                }
            }
        }

        // ── Escuelas ──────────────────────────────────────────────────────────
        SettingsSection(title = "ESCUELAS (máx. 3)") {
            settings.schools.forEachIndexed { i, loc ->
                LocationEditor(
                    title    = "Escuela ${i + 1}",
                    location = loc,
                    onSave   = { vm.updateSchool(i, it) },
                    onDelete = { vm.removeSchool(i) }
                )
            }
            if (settings.schools.size < 3) {
                AddButton("Agregar escuela") {
                    vm.addSchool(Location("Escuela ${settings.schools.size + 1}", 0.0, 0.0))
                }
            }
        }

        // ── Keywords de calendario ────────────────────────────────────────────
        SettingsSection(title = "KEYWORDS DE CALENDARIO") {
            val mc = metroColors
            Text(
                text       = "Asigna ubicación a eventos según su título",
                fontFamily = SpaceMono,
                fontSize   = 9.sp,
                color      = mc.textSecondary
            )
            Spacer(Modifier.height(4.dp))
            settings.calendarKeywords.forEachIndexed { i, kw ->
                KeywordRow(keyword = kw, onDelete = { vm.removeKeyword(i) })
            }
            val allLocations = listOf(
                Location(settings.homeLabel, settings.homeLat, settings.homeLon)
            ) + settings.workplaces + settings.schools
            AddKeywordRow(
                locationOptions = allLocations.map { it.label },
                onAdd           = { vm.addKeyword(it) }
            )
        }

        // ── Umbrales de clima ─────────────────────────────────────────────────
        SettingsSection(title = "UMBRALES DE CLIMA") {
            val hotGlyph  = remember { glyphRepo.getGlyph("weather", "heat") }
            val coldGlyph = remember { glyphRepo.getGlyph("weather", "cold") }
            val rainGlyph = remember { glyphRepo.getGlyph("weather", "rain") }
            val windGlyph = remember { glyphRepo.getGlyph("weather", "wind") }
            val carGlyph  = remember { glyphRepo.getGlyph("transport", "car") }
            val mc = metroColors
            ThresholdRow("Calor (°C)",         settings.heatThresholdC,       15, 45, mc.danger,      hotGlyph,  mc.danger)      { vm.setHeatThreshold(it) }
            ThresholdRow("Frío (°C)",          settings.coldThresholdC,        0, 20, mc.blue,        coldGlyph, mc.blue)        { vm.setColdThreshold(it) }
            ThresholdRow("Lluvia (%)",         settings.rainThresholdPct,     10, 90, mc.blue,        rainGlyph, mc.blue)        { vm.setRainThreshold(it) }
            ThresholdRow("Viento (km/h)",      settings.windThresholdKmh,     10, 80, mc.textPrimary, windGlyph, mc.textPrimary) { vm.setWindThreshold(it) }
            ThresholdRow("Uber si lluvia (%)", settings.uberRainThresholdPct, 10, 90, mc.accent,      carGlyph,  mc.accent)      { vm.setUberRainThreshold(it) }
        }

        // ── Notificación diaria ───────────────────────────────────────────────
        SettingsSection(title = "NOTIFICACIÓN DIARIA") {
            val mc = metroColors
            var hour   by remember(settings.notificationHour)   { mutableStateOf(settings.notificationHour.toString().padStart(2, '0')) }
            var minute by remember(settings.notificationMinute) { mutableStateOf(settings.notificationMinute.toString().padStart(2, '0')) }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetroTextField(value = hour,   onValueChange = { if (it.length <= 2) hour = it },   label = "HH", modifier = Modifier.width(64.dp), keyboardType = KeyboardType.Number)
                Text(":", fontFamily = SpaceMono, color = mc.textSecondary, fontSize = 18.sp)
                MetroTextField(value = minute, onValueChange = { if (it.length <= 2) minute = it }, label = "MM", modifier = Modifier.width(64.dp), keyboardType = KeyboardType.Number)
                Spacer(Modifier.weight(1f))
                MetroButton("guardar") {
                    val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 7
                    val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    vm.setNotifHour(h, m)
                }
            }
        }

        // ── Tema ──────────────────────────────────────────────────────────────
        SettingsSection(title = "TEMA") {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("SYSTEM" to "sistema", "DARK" to "oscuro", "LIGHT" to "claro").forEach { (mode, label) ->
                    val selected = settings.themeMode == mode
                    val mc = metroColors
                    OutlinedButton(
                        onClick  = { vm.setThemeMode(mode) },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(4.dp),
                        border   = androidx.compose.foundation.BorderStroke(0.5.dp, if (selected) mc.accent else mc.border),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) mc.greenSurface else mc.surface1,
                            contentColor   = if (selected) mc.accent       else mc.textSecondary
                        )
                    ) {
                        Text(label, fontFamily = SpaceMono, fontSize = 9.sp)
                    }
                }
            }
        }

        // ── Test Glyph ────────────────────────────────────────────────────────
        SettingsSection(title = "TEST GLYPH") {
            val mc = metroColors
            Text(
                text       = "Prueba cada patrón climático en tiempo real",
                fontFamily = SpaceMono,
                fontSize   = 9.sp,
                color      = mc.textSecondary
            )
            Spacer(Modifier.height(4.dp))

            val glyphController = remember {
                com.irofactory.meteoroglyph.glyph.GlyphController.getInstance(context)
            }

            fun makeState(
                condition: WeatherCondition,
                temp: Int = 22,
                rain: Int = 0,
                wind: Int = 10
            ) = WeatherState(
                tempCelsius     = temp,
                condition       = condition,
                rainProbability = rain,
                windSpeedKmh    = wind,
                humidity        = 60,
                rainWindow      = null,
                isDay           = true
            )

            listOf(
                "lluvia"          to makeState(WeatherCondition.RAIN,       rain = 60),
                "lluvia fuerte"   to makeState(WeatherCondition.HEAVY_RAIN, rain = 85),
                "tormenta"        to makeState(WeatherCondition.STORM,      rain = 90),
                "calor extremo"   to makeState(WeatherCondition.HOT,        temp = 36),
                "viento"          to makeState(WeatherCondition.WIND,       wind = 50),
                "frío"            to makeState(WeatherCondition.COLD,       temp = 5),
                "neblina"         to makeState(WeatherCondition.FOG),
                "aguanieve"       to makeState(WeatherCondition.SLEET,      rain = 60),
                "nieve"           to makeState(WeatherCondition.SNOW),
            ).forEach { (label, state) ->
                MetroButton(label) { glyphController.notifyWeather(state) }
            }

            MetroButton("evento próximo (lluvia)") {
                glyphController.notifyUpcomingEvent(
                    makeState(WeatherCondition.RAIN, rain = 70)
                )
            }

            MetroButton("detener") { glyphController.stopPattern() }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Componentes internos ──────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val mc = metroColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = title, fontFamily = SpaceMono, fontSize = 9.sp, color = mc.textMuted, letterSpacing = 0.14.sp)
        Column(
            modifier            = Modifier.fillMaxWidth().border(0.5.dp, mc.border, RoundedCornerShape(8.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content             = content
        )
    }
}

@Composable
private fun SettingsRow(label: String, content: @Composable () -> Unit) {
    val mc = metroColors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontFamily = SpaceMono, fontSize = 11.sp, color = mc.textPrimary)
        content()
    }
}

@Composable
private fun LocationEditor(title: String, location: Location, onSave: (Location) -> Unit, onDelete: (() -> Unit)? = null) {
    val mc = metroColors
    var label by remember(location.label) { mutableStateOf(location.label) }
    var lat   by remember(location.lat)   { mutableStateOf(location.lat.toString()) }
    var lon   by remember(location.lon)   { mutableStateOf(location.lon.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontFamily = SpaceMono, fontSize = 10.sp, color = mc.accent)
            if (onDelete != null) {
                TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) {
                    Text("eliminar", fontFamily = SpaceMono, fontSize = 9.sp, color = mc.danger)
                }
            }
        }
        MetroTextField(value = label, onValueChange = { label = it }, label = "nombre")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetroTextField(value = lat, onValueChange = { lat = it }, label = "latitud",  modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
            MetroTextField(value = lon, onValueChange = { lon = it }, label = "longitud", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
        }
        MetroButton("guardar ubicación") { onSave(Location(label, lat.toDoubleOrNull() ?: 0.0, lon.toDoubleOrNull() ?: 0.0)) }
    }
}

@Composable
private fun KeywordRow(keyword: CalendarKeyword, onDelete: () -> Unit) {
    val mc = metroColors
    Row(
        modifier              = Modifier.fillMaxWidth().border(0.5.dp, mc.border, RoundedCornerShape(4.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(keyword.keyword,              fontFamily = SpaceMono, fontSize = 10.sp, color = mc.textPrimary)
            Text("→ ${keyword.locationLabel}", fontFamily = SpaceMono, fontSize = 9.sp,  color = mc.textSecondary)
        }
        TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) {
            Text("✕", fontFamily = SpaceMono, fontSize = 10.sp, color = mc.danger)
        }
    }
}

@Composable
private fun AddKeywordRow(locationOptions: List<String>, onAdd: (CalendarKeyword) -> Unit) {
    val mc = metroColors
    var keyword  by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(locationOptions.firstOrNull() ?: "") }
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MetroTextField(value = keyword, onValueChange = { keyword = it }, label = "keyword (ej: Chambeanding)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick  = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(4.dp),
                    border   = androidx.compose.foundation.BorderStroke(0.5.dp, mc.border),
                    colors   = ButtonDefaults.outlinedButtonColors(containerColor = mc.surface1, contentColor = mc.textPrimary)
                ) {
                    Text(selected.ifEmpty { "ubicación" }, fontFamily = SpaceMono, fontSize = 10.sp)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = mc.surface1) {
                    locationOptions.forEach { opt ->
                        DropdownMenuItem(
                            text    = { Text(opt, fontFamily = SpaceMono, fontSize = 10.sp, color = mc.textPrimary) },
                            onClick = { selected = opt; expanded = false }
                        )
                    }
                }
            }
            MetroButton("agregar") {
                if (keyword.isNotBlank() && selected.isNotBlank()) {
                    onAdd(CalendarKeyword(keyword.trim(), selected))
                    keyword = ""
                }
            }
        }
    }
}

@Composable
private fun ThresholdRow(
    label: String, value: Int, min: Int, max: Int,
    trackColor: androidx.compose.ui.graphics.Color,
    glyph: ParsedGlyph?,
    glyphTint: androidx.compose.ui.graphics.Color,
    onValueChange: (Int) -> Unit
) {
    val mc = metroColors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (glyph != null) GlyphRenderer(glyph = glyph, tint = glyphTint, modifier = Modifier.size(16.dp))
                Text(label, fontFamily = SpaceMono, fontSize = 10.sp, color = mc.textPrimary)
            }
            Text("$value", fontFamily = SpaceMono, fontSize = 10.sp, color = trackColor)
        }
        Slider(
            value         = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange    = min.toFloat()..max.toFloat(),
            colors        = SliderDefaults.colors(thumbColor = trackColor, activeTrackColor = trackColor, inactiveTrackColor = mc.surface2)
        )
    }
}

@Composable
private fun AddButton(label: String, onClick: () -> Unit) {
    val mc = metroColors
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("+ $label", fontFamily = SpaceMono, fontSize = 10.sp, color = mc.accent)
    }
}

@Composable
private fun MetroTextField(
    value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, keyboardType: KeyboardType = KeyboardType.Text
) {
    val mc = metroColors
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label         = { Text(label, fontFamily = SpaceMono, fontSize = 9.sp) },
        modifier      = modifier.fillMaxWidth(),
        singleLine    = true,
        shape         = RoundedCornerShape(4.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = mc.accent,   unfocusedBorderColor    = mc.border,
            focusedLabelColor       = mc.accent,   unfocusedLabelColor     = mc.textSecondary,
            cursorColor             = mc.accent,   focusedTextColor        = mc.textPrimary,
            unfocusedTextColor      = mc.textPrimary,
            focusedContainerColor   = mc.surface1, unfocusedContainerColor = mc.surface1
        ),
        textStyle       = androidx.compose.ui.text.TextStyle(fontFamily = SpaceMono, fontSize = 11.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
private fun MetroButton(label: String, onClick: () -> Unit) {
    val mc = metroColors
    Button(
        onClick = onClick,
        shape   = RoundedCornerShape(4.dp),
        colors  = ButtonDefaults.buttonColors(containerColor = mc.greenSurface, contentColor = mc.accent),
        border  = androidx.compose.foundation.BorderStroke(0.5.dp, mc.accent.copy(alpha = 0.3f))
    ) {
        Text(label, fontFamily = SpaceMono, fontSize = 9.sp)
    }
}