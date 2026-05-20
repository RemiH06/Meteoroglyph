package com.irofactory.meteoroglyph

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irofactory.meteoroglyph.data.settings.AppSettings
import com.irofactory.meteoroglyph.data.settings.SettingsRepository
import com.irofactory.meteoroglyph.glyph.GlyphController
import com.irofactory.meteoroglyph.ui.navigation.NavGraph
import com.irofactory.meteoroglyph.ui.theme.MeteoroglyphTheme
import com.irofactory.meteoroglyph.ui.theme.ThemeMode
import com.irofactory.meteoroglyph.viewmodel.HomeViewModel
import com.irofactory.meteoroglyph.worker.WeatherCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var glyphController: GlyphController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── Glyph ─────────────────────────────────────────────────────────────
        glyphController = GlyphController.getInstance(this)
        glyphController.init()
        homeViewModel.glyphController = glyphController

        // ── Permiso de notificaciones ─────────────────────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0
            )
        }

        // ── Programar worker diario ───────────────────────────────────────────
        CoroutineScope(Dispatchers.IO).launch {
            val settings = SettingsRepository(this@MainActivity).settings.first()
            WeatherCheckWorker.schedule(
                this@MainActivity,
                settings.notificationHour,
                settings.notificationMinute
            )
        }

        setContent {
            val settingsRepo  = remember { SettingsRepository(this@MainActivity) }
            val settingsState = settingsRepo.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
                lifecycle    = this@MainActivity.lifecycle
            )
            val themeMode = when (settingsState.value.themeMode) {
                "DARK"  -> ThemeMode.DARK
                "LIGHT" -> ThemeMode.LIGHT
                else    -> ThemeMode.SYSTEM
            }
            MeteoroglyphTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        glyphController.close()
    }
}