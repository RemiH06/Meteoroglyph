package com.irofactory.meteoroglyph.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.irofactory.meteoroglyph.data.calendar.CalendarEvent
import com.irofactory.meteoroglyph.data.calendar.CalendarRepository
import com.irofactory.meteoroglyph.data.location.LocationRepository
import com.irofactory.meteoroglyph.data.outfit.OutfitEngine
import com.irofactory.meteoroglyph.data.outfit.TransitRecommendation
import com.irofactory.meteoroglyph.data.settings.AppSettings
import com.irofactory.meteoroglyph.data.settings.SettingsRepository
import com.irofactory.meteoroglyph.data.weather.WeatherRepository
import com.irofactory.meteoroglyph.data.weather.WeatherState
import com.irofactory.meteoroglyph.fluid.FluidParams
import com.irofactory.meteoroglyph.glyph.GlyphController
import com.irofactory.meteoroglyph.icon.IconUpdater
import com.irofactory.meteoroglyph.ui.components.OutfitItem
import com.irofactory.meteoroglyph.worker.EventAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean                = true,
    val weather: WeatherState?            = null,
    val nextEvent: CalendarEvent?         = null,
    val outfitItems: List<OutfitItem>     = emptyList(),
    val transitRec: TransitRecommendation = TransitRecommendation.PUBLIC_OK,
    val fluidParams: FluidParams          = FluidParams(),
    val error: String?                    = null
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val weatherRepo  = WeatherRepository()
    private val calendarRepo = CalendarRepository(app)
    private val settingsRepo = SettingsRepository(app)
    private val locationRepo = LocationRepository(app)

    // Inyectado desde MainActivity después de crear el ViewModel
    var glyphController: GlyphController? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepo.settings.collect { settings ->
                refresh(settings.homeLat, settings.homeLon, settings)
            }
        }
    }

    fun refresh(
        lat: Double      = 20.6597,
        lon: Double      = -103.3496,
        settings: AppSettings? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val currentSettings = settings ?: settingsRepo.settings.first()

            // ── Coordenadas — GPS si está habilitado, manual si no ────────────
            val (finalLat, finalLon) = if (currentSettings.useGps) {
                try {
                    val loc = locationRepo.getCurrentLocation()
                    if (loc != null) Pair(loc.latitude, loc.longitude)
                    else Pair(currentSettings.homeLat, currentSettings.homeLon)
                } catch (e: Exception) {
                    Pair(currentSettings.homeLat, currentSettings.homeLon)
                }
            } else {
                Pair(currentSettings.homeLat, currentSettings.homeLon)
            }

            // ── Clima ─────────────────────────────────────────────────────────
            val weatherResult = weatherRepo.getWeather(finalLat, finalLon)
            val weather       = weatherResult.getOrNull()
            val error         = if (weatherResult.isFailure) "No se pudo obtener el clima" else null

            // ── Calendario ────────────────────────────────────────────────────
            val nextEvent = try {
                calendarRepo.getNextEvent(currentSettings)
            } catch (e: Exception) { null }

            // ── Outfit ────────────────────────────────────────────────────────
            val outfitItems = weather?.let { OutfitEngine.recommend(it, currentSettings) } ?: emptyList()
            val transitRec  = weather?.let { OutfitEngine.recommendTransit(it, currentSettings) }
                ?: TransitRecommendation.PUBLIC_OK

            // ── Actualizar estado ─────────────────────────────────────────────
            _uiState.value = HomeUiState(
                isLoading   = false,
                weather     = weather,
                nextEvent   = nextEvent,
                outfitItems = outfitItems,
                transitRec  = transitRec,
                fluidParams = FluidParams(
                    fillRatio       = currentSettings.fluidFillRatio,
                    viscosity       = currentSettings.fluidViscosity,
                    stiffness       = currentSettings.fluidStiffness,
                    restitution     = currentSettings.fluidRestitution,
                    smoothingRadius = currentSettings.fluidSmoothingRadius,
                    particleCount   = currentSettings.fluidParticleCount
                ),
                error       = error
            )

            // Actualizar ícono según clima
            weather?.let { IconUpdater.update(getApplication(), it) }

            // ── Glyph LEDs ────────────────────────────────────────────────────
            // Activa el patrón si hay evento próximo Y el clima no es ideal
            if (nextEvent != null && weather != null) {
                glyphController?.notifyUpcomingEvent(weather)
            } else {
                glyphController?.stopPattern()
            }

            // Programar alarmas de eventos
            viewModelScope.launch(Dispatchers.IO) {
                EventAlarmScheduler.scheduleAll(getApplication(), currentSettings)
            }
        }
    }
}