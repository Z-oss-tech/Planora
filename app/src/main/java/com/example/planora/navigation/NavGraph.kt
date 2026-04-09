package com.example.planora.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun NavGraph(
    navController: NavHostController,
    darkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    // We delegate everything to MainScreen to avoid nested NavHosts
    MainScreen(navController, "splash")
}
