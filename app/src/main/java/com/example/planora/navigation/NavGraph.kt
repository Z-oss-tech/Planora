package com.example.planora.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.example.planora.utils.FirebaseUtils
@Composable
fun NavGraph(
    navController: NavHostController,
    darkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {

    val startRoute = if (FirebaseUtils.auth.currentUser != null) "today" else "login"
    MainScreen(navController, startRoute)

}