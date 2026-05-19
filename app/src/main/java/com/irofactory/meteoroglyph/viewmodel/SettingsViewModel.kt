package com.irofactory.meteoroglyph.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.irofactory.meteoroglyph.data.settings.AppSettings
import com.irofactory.meteoroglyph.data.settings.CalendarKeyword
import com.irofactory.meteoroglyph.data.settings.Location
import com.irofactory.meteoroglyph.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val settings: StateFlow<AppSettings> = repo.settings.stateIn(
        scope         = viewModelScope,
        started       = SharingStarted.WhileSubscribed(5_000),
        initialValue  = AppSettings()
    )

    fun setUseGps(value: Boolean)       = save { it.copy(useGps = value) }
    fun setHomeLabel(value: String)     = save { it.copy(homeLabel = value) }
    fun setHomeLat(value: Double)       = save { it.copy(homeLat = value) }
    fun setHomeLon(value: Double)       = save { it.copy(homeLon = value) }

    fun setHeatThreshold(v: Int)        = save { it.copy(heatThresholdC = v) }
    fun setColdThreshold(v: Int)        = save { it.copy(coldThresholdC = v) }
    fun setRainThreshold(v: Int)        = save { it.copy(rainThresholdPct = v) }
    fun setWindThreshold(v: Int)        = save { it.copy(windThresholdKmh = v) }
    fun setUberRainThreshold(v: Int)    = save { it.copy(uberRainThresholdPct = v) }
    fun setNotifHour(h: Int, m: Int)    = save { it.copy(notificationHour = h, notificationMinute = m) }

    // ── Workplaces ────────────────────────────────────────────────────────────
    fun addWorkplace(loc: Location) = save { s ->
        if (s.workplaces.size >= 3) s
        else s.copy(workplaces = s.workplaces + loc)
    }
    fun removeWorkplace(index: Int) = save { s ->
        s.copy(workplaces = s.workplaces.toMutableList().also { it.removeAt(index) })
    }
    fun updateWorkplace(index: Int, loc: Location) = save { s ->
        s.copy(workplaces = s.workplaces.toMutableList().also { it[index] = loc })
    }

    // ── Schools ───────────────────────────────────────────────────────────────
    fun addSchool(loc: Location) = save { s ->
        if (s.schools.size >= 3) s
        else s.copy(schools = s.schools + loc)
    }
    fun removeSchool(index: Int) = save { s ->
        s.copy(schools = s.schools.toMutableList().also { it.removeAt(index) })
    }
    fun updateSchool(index: Int, loc: Location) = save { s ->
        s.copy(schools = s.schools.toMutableList().also { it[index] = loc })
    }

    // ── Calendar keywords ─────────────────────────────────────────────────────
    fun addKeyword(kw: CalendarKeyword)     = save { s ->
        s.copy(calendarKeywords = s.calendarKeywords + kw)
    }
    fun removeKeyword(index: Int)           = save { s ->
        s.copy(calendarKeywords = s.calendarKeywords.toMutableList().also { it.removeAt(index) })
    }

    private fun save(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            repo.save(transform(settings.value))
        }
    }
}