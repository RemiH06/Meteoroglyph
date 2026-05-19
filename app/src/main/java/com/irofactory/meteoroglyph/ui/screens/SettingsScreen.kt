package com.irofactory.meteoroglyph.ui.screens

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
import androidx.compose.ui.graphics.Color
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
import com.irofactory.meteoroglyph.ui.components.GlyphRenderer
import com.irofactory.meteoroglyph.ui.theme.*
import com.irofactory.meteoroglyph.viewmodel.SettingsViewModel
import com.irofactory.meteoroglyph.ui.theme.metroColors

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
        // Header
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Row {
                        Text("meteoro", fontFamily = SpaceMono, fontSize = 20.sp, color = Color(0xFFF0F0F0))
                        Text("glyph",   fontFamily = SpaceMono, fontSize = 20.sp, color = AccentGreen)
                    }
                    Text(
                        text  = "CONFIGURACIÓN",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onNavigateBack) {
                    Text("←", fontSize = 18.sp, color = TextSecondary)
                }
            }
        }

        // ── Ubicación ─────────────────────────────────────────────────────────
        SettingsSection(title = "UBICACIÓN") {

            // GPS toggle
            SettingsRow(label = "Usar GPS automático") {
                Switch(
                    checked         = settings.useGps,
                    onCheckedChange = { vm.setUseGps(it) },
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor       = Background,
                        checkedTrackColor       = AccentGreen,
                        uncheckedThumbColor     = TextSecondary,
                        uncheckedTrackColor     = Surface2
                    )
                )
            }

            // Casa
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
            Text(
                text       = "Asigna ubicación a eventos según su título",
                fontFamily = SpaceMono,
                fontSize   = 9.sp,
                color      = TextSecondary
            )
            Spacer(Modifier.height(4.dp))

            settings.calendarKeywords.forEachIndexed { i, kw ->
                KeywordRow(
                    keyword  = kw,
                    onDelete = { vm.removeKeyword(i) }
                )
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

            ThresholdRow("Calor (°C)",         settings.heatThresholdC,      15, 45, mc.danger,  hotGlyph,  mc.danger)  { vm.setHeatThreshold(it) }
            ThresholdRow("Frío (°C)",          settings.coldThresholdC,       0, 20, mc.blue,    coldGlyph, mc.blue)    { vm.setColdThreshold(it) }
            ThresholdRow("Lluvia (%)",         settings.rainThresholdPct,    10, 90, mc.blue,    rainGlyph, mc.blue)    { vm.setRainThreshold(it) }
            ThresholdRow("Viento (km/h)",      settings.windThresholdKmh,    10, 80, mc.textPrimary, windGlyph, mc.textPrimary) { vm.setWindThreshold(it) }
            ThresholdRow("Uber si lluvia (%)", settings.uberRainThresholdPct,10, 90, mc.accent,  carGlyph,  mc.accent)  { vm.setUberRainThreshold(it) }
        }

        // ── Notificación diaria ───────────────────────────────────────────────
        SettingsSection(title = "NOTIFICACIÓN DIARIA") {
            var hour   by remember { mutableStateOf(settings.notificationHour.toString().padStart(2, '0')) }
            var minute by remember { mutableStateOf(settings.notificationMinute.toString().padStart(2, '0')) }

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetroTextField(
                    value         = hour,
                    onValueChange = { if (it.length <= 2) hour = it },
                    label         = "HH",
                    modifier      = Modifier.width(64.dp),
                    keyboardType  = KeyboardType.Number
                )
                Text(":", fontFamily = SpaceMono, color = TextSecondary, fontSize = 18.sp)
                MetroTextField(
                    value         = minute,
                    onValueChange = { if (it.length <= 2) minute = it },
                    label         = "MM",
                    modifier      = Modifier.width(64.dp),
                    keyboardType  = KeyboardType.Number
                )
                Spacer(Modifier.weight(1f))
                MetroButton("guardar") {
                    val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 7
                    val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    vm.setNotifHour(h, m)
                }
            }
        }

        // ── Tema ──────────────────────────────────────────────────────────────────────
        SettingsSection(title = "TEMA") {
            val options = listOf(
                Triple("SYSTEM", "sistema",   "ui"),
                Triple("DARK",   "oscuro",    "ui"),
                Triple("LIGHT",  "claro",     "ui")
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (mode, label, _) ->
                    val selected = settings.themeMode == mode
                    val mc = metroColors
                    OutlinedButton(
                        onClick  = { vm.setThemeMode(mode) },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(4.dp),
                        border   = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            if (selected) mc.accent else mc.border
                        ),
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

        Spacer(Modifier.height(32.dp))
    }
}

// ── Componentes internos ──────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text          = title,
            fontFamily    = SpaceMono,
            fontSize      = 9.sp,
            color         = TextMuted,
            letterSpacing = 0.14.sp
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, Border, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = SpaceMono, fontSize = 11.sp, color = TextPrimary)
        content()
    }
}

@Composable
private fun LocationEditor(
    title: String,
    location: Location,
    onSave: (Location) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var label by remember(location.label) { mutableStateOf(location.label) }
    var lat   by remember(location.lat)   { mutableStateOf(location.lat.toString()) }
    var lon   by remember(location.lon)   { mutableStateOf(location.lon.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(title, fontFamily = SpaceMono, fontSize = 10.sp, color = AccentGreen)
            if (onDelete != null) {
                TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) {
                    Text("eliminar", fontFamily = SpaceMono, fontSize = 9.sp, color = DangerRed)
                }
            }
        }
        MetroTextField(value = label, onValueChange = { label = it }, label = "nombre")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetroTextField(
                value         = lat,
                onValueChange = { lat = it },
                label         = "latitud",
                modifier      = Modifier.weight(1f),
                keyboardType  = KeyboardType.Decimal
            )
            MetroTextField(
                value         = lon,
                onValueChange = { lon = it },
                label         = "longitud",
                modifier      = Modifier.weight(1f),
                keyboardType  = KeyboardType.Decimal
            )
        }
        MetroButton("guardar ubicación") {
            onSave(Location(label, lat.toDoubleOrNull() ?: 0.0, lon.toDoubleOrNull() ?: 0.0))
        }
    }
}

@Composable
private fun KeywordRow(keyword: CalendarKeyword, onDelete: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Border, RoundedCornerShape(4.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(keyword.keyword,       fontFamily = SpaceMono, fontSize = 10.sp, color = TextPrimary)
            Text("→ ${keyword.locationLabel}", fontFamily = SpaceMono, fontSize = 9.sp, color = TextSecondary)
        }
        TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) {
            Text("✕", fontFamily = SpaceMono, fontSize = 10.sp, color = DangerRed)
        }
    }
}

@Composable
private fun AddKeywordRow(
    locationOptions: List<String>,
    onAdd: (CalendarKeyword) -> Unit
) {
    var keyword  by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(locationOptions.firstOrNull() ?: "") }
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MetroTextField(
            value         = keyword,
            onValueChange = { keyword = it },
            label         = "keyword (ej: Chambeanding)"
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape  = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text(
                        text       = selected.ifEmpty { "ubicación" },
                        fontFamily = SpaceMono,
                        fontSize   = 10.sp
                    )
                }
                DropdownMenu(
                    expanded        = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor  = Surface1
                ) {
                    locationOptions.forEach { opt ->
                        DropdownMenuItem(
                            text    = { Text(opt, fontFamily = SpaceMono, fontSize = 10.sp, color = TextPrimary) },
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
    label:         String,
    value:         Int,
    min:           Int,
    max:           Int,
    trackColor:    androidx.compose.ui.graphics.Color,
    glyph:         ParsedGlyph?,
    glyphTint:     androidx.compose.ui.graphics.Color,
    onValueChange: (Int) -> Unit
) {
    val mc = metroColors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (glyph != null) {
                    GlyphRenderer(
                        glyph    = glyph,
                        tint     = glyphTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(label, fontFamily = SpaceMono, fontSize = 10.sp, color = mc.textPrimary)
            }
            Text("$value", fontFamily = SpaceMono, fontSize = 10.sp, color = trackColor)
        }
        Slider(
            value         = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange    = min.toFloat()..max.toFloat(),
            colors        = SliderDefaults.colors(
                thumbColor         = trackColor,
                activeTrackColor   = trackColor,
                inactiveTrackColor = mc.surface2
            )
        )
    }
}

@Composable
private fun AddButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text       = "+ $label",
            fontFamily = SpaceMono,
            fontSize   = 10.sp,
            color      = AccentGreen
        )
    }
}

@Composable
private fun MetroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, fontFamily = SpaceMono, fontSize = 9.sp) },
        modifier      = modifier.fillMaxWidth(),
        singleLine    = true,
        shape         = RoundedCornerShape(4.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = AccentGreen,
            unfocusedBorderColor = Border,
            focusedLabelColor    = AccentGreen,
            unfocusedLabelColor  = TextSecondary,
            cursorColor          = AccentGreen,
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary
        ),
        textStyle     = androidx.compose.ui.text.TextStyle(
            fontFamily = SpaceMono,
            fontSize   = 11.sp
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
private fun MetroButton(label: String, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        shape    = RoundedCornerShape(4.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = GreenSurface,
            contentColor   = AccentGreen
        ),
        border   = androidx.compose.foundation.BorderStroke(0.5.dp, AccentGreen.copy(alpha = 0.3f))
    ) {
        Text(label, fontFamily = SpaceMono, fontSize = 9.sp)
    }
}