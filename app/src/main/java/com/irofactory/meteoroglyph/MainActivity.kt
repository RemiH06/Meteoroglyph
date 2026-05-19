package com.irofactory.meteoroglyph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irofactory.meteoroglyph.data.settings.AppSettings
import com.irofactory.meteoroglyph.data.settings.SettingsRepository
import com.irofactory.meteoroglyph.ui.navigation.NavGraph
import com.irofactory.meteoroglyph.ui.screens.HomeScreen
import com.irofactory.meteoroglyph.ui.theme.MeteoroglyphTheme
import com.irofactory.meteoroglyph.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsRepo = remember { SettingsRepository(this@MainActivity) }
            val settingsState = settingsRepo.settings.collectAsStateWithLifecycle(
                initialValue  = AppSettings(),
                lifecycle     = this@MainActivity.lifecycle
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
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MeteoroglyphTheme {
        Greeting("Android")
    }
}