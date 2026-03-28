package com.example.planora.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.planora.utils.FirebaseUtils
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {

    // 🎬 Smooth premium animations
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            1f,
            animationSpec = tween(900, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            1f,
            animationSpec = tween(900)
        )

        delay(1600)

        if (FirebaseUtils.auth.currentUser != null) {
            navController.navigate("tasks") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // 🧡 Reference-style warm gradient
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFB75E), // soft orange
            Color(0xFFFF8C42), // warm orange
            Color(0xFF1F1F1F)  // dark base
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {

            // 🟠 Logo container (modern pill + depth)
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.25f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "P",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFB75E)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ✨ App name
            Text(
                text = "Planora",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ✨ Tagline (soft & elegant)
            Text(
                text = "Plan smart. Work focused.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(44.dp))

            // ✨ Minimal loader (reference style)
            CircularProgressIndicator(
                color = Color(0xFFFFB75E),
                strokeWidth = 3.dp,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}