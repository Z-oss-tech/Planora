package com.example.planora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.planora.navigation.NavGraph
import com.example.planora.ui.theme.PlanoraTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            var darkMode by remember { mutableStateOf(false) }

            PlanoraTheme(
                darkTheme = darkMode
            ) {

                val navController = rememberNavController()

                NavGraph(
                    navController = navController,
                    darkMode = darkMode,
                    onToggleDarkMode = { darkMode = it }
                )

            }
        }
    }
}