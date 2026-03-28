package com.example.planora.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ☀️ LIGHT THEME
private val LightColorScheme = lightColorScheme(
    primary = PurpleStart,
    secondary = OrangePrimary,
    background = BackgroundLight,
    surface = CardLight,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

// 🌙 DARK THEME
private val DarkColorScheme = darkColorScheme(
    primary = PurpleStart,
    secondary = OrangePrimary,
    background = BackgroundDark,
    surface = CardDark,
    onPrimary = Color.White,
    onBackground = TextDarkPrimary,
    onSurface = TextDarkPrimary
)

@Composable
fun PlanoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {

    val colorScheme =
        if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}