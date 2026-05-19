package com.irofactory.meteoroglyph.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.irofactory.meteoroglyph.ui.screens.HomeScreen
import com.irofactory.meteoroglyph.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    object Home     : Screen("home")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(onNavigateToSettings = {
                navController.navigate(Screen.Settings.route)
            })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = {
                navController.popBackStack()
            })
        }
    }
}