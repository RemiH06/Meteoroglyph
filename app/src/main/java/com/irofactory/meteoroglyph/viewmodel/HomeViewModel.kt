package com.irofactory.meteoroglyph.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.irofactory.meteoroglyph.data.calendar.CalendarEvent
import com.irofactory.meteoroglyph.data.calendar.CalendarRepository
import com.irofactory.meteoroglyph.data.weather.WeatherCondition
import com.irofactory.meteoroglyph.data.weather.WeatherRepository
import com.irofactory.meteoroglyph.data.weather.WeatherState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean           = true,
    val weather: WeatherState?       = null,
    val nextEvent: CalendarEvent?    = null,
    val error: String?               = null
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val weatherRepo  = WeatherRepository()
    private val calendarRepo = CalendarRepository(app)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Clima
            val weatherResult = weatherRepo.getWeather()
            val weather = weatherResult.getOrNull()
            val error   = if (weatherResult.isFailure)
                "No se pudo obtener el clima" else null

            // Calendario
            val nextEvent = try {
                calendarRepo.getNextEvent()
            } catch (e: Exception) { null }

            _uiState.value = HomeUiState(
                isLoading  = false,
                weather    = weather,
                nextEvent  = nextEvent,
                error      = error
            )
        }
    }
}