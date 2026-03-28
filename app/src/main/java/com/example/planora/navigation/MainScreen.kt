package com.example.planora.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.planora.ui.habits.HabitsScreen
import com.example.planora.ui.login.LoginScreen
import com.example.planora.ui.login.RegisterScreen
import com.example.planora.ui.settings.SettingsScreen
import androidx.compose.material.icons.filled.Category
import com.example.planora.ui.category.CategoryScreen

// Import your other screens here as needed

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Today : Screen("today", "Today", Icons.Default.CheckCircle)
    object Habits : Screen("habits", "Habits", Icons.Default.DateRange)
    object Category : Screen("category", "Category", Icons.Default.Category)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainScreen(navController: NavHostController, startDestination: String) {
    val items = listOf(Screen.Today, Screen.Habits, Screen.Category , Screen.Settings)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = items.any { it.route == currentRoute }

    // Colours kept consistent with the rest of the app's dark palette
    val navBg       = Color(0xFF111116)
    val navBorder   = Color(0xFF2A2A38)
    val activeColor = Color(0xFF6C8FFF)
    val inactiveColor = Color(0xFF4A4A64)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                // Floating pill nav bar — sits above the system nav bar with breathing room
                NavigationBar(
                    containerColor = navBg,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .height(68.dp)
                        .clip(RoundedCornerShape(34.dp))
                ) {
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        val isSelected =
                            currentDestination?.hierarchy?.any { it.route == screen.route } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector        = screen.icon,
                                    contentDescription = null,
                                    // Active icon is slightly larger visually via the tint contrast
                                    tint               = if (isSelected) activeColor else inactiveColor,
                                    modifier           = Modifier
                                        .padding(bottom = if (isSelected) 0.dp else 0.dp)
                                )
                            },
                            label = {
                                Text(
                                    text       = screen.label,
                                    fontSize   = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color      = if (isSelected) activeColor else inactiveColor,
                                    maxLines   = 1
                                )
                            },
                            selected  = isSelected,
                            onClick   = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                // Subtle pill highlight behind the active icon
                                indicatorColor         = activeColor.copy(alpha = 0.12f),
                                selectedIconColor      = activeColor,
                                selectedTextColor      = activeColor,
                                unselectedIconColor    = inactiveColor,
                                unselectedTextColor    = inactiveColor
                            )
                        )
                    }
                }
            }
        },
        containerColor    = Color(0xFF060608),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable("login")              { LoginScreen(navController) }
            composable("register")           { RegisterScreen(navController) }
            composable(Screen.Today.route)   { TaskListScreen(navController) }
            composable(Screen.Habits.route)  { HabitsScreen(navController) }
            composable(Screen.Category.route) { CategoryScreen(navController) } // ✅ ADDED
            composable(Screen.Settings.route) {  SettingsScreen(navController)  }
        }
    }
}