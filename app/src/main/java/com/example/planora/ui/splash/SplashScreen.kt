package com.example.planora.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.planora.R
import com.example.planora.utils.FirebaseUtils
import kotlinx.coroutines.delay
import java.util.Random

@Composable
fun SplashScreen(navController: NavController) {

    // 🎬 Animations
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffset = remember { Animatable(30f) }
    
    // 🌀 Ambient Effects
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "starAlpha"
    )

    LaunchedEffect(Unit) {
        // Step 1: Cinematic zoom-in and fade-in
        scale.animateTo(
            1f,
            animationSpec = tween(2000, easing = CubicBezierEasing(0.19f, 1f, 0.22f, 1f))
        )
        alpha.animateTo(
            1f,
            animationSpec = tween(1500)
        )
        
        // Step 2: Text reveal
        textAlpha.animateTo(1f, animationSpec = tween(1200))
        textOffset.animateTo(0f, animationSpec = tween(1200, easing = FastOutSlowInEasing))

        delay(3500)

        // Navigation
        if (FirebaseUtils.auth.currentUser != null) {
            navController.navigate("today") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // 🌌 Midnight Colors
    val midnightBg = Color(0xFF060608)
    val accentBlue = Color(0xFF6C8FFF)
    val accentPurple = Color(0xFFAA80FF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(midnightBg),
        contentAlignment = Alignment.Center
    ) {
        
        // ✨ Starry Backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val random = Random(42)
            repeat(80) {
                val x = random.nextFloat() * size.width
                val y = random.nextFloat() * size.height
                val sAlpha = random.nextFloat() * starAlpha
                drawCircle(
                    color = Color.White.copy(alpha = sAlpha),
                    radius = random.nextFloat() * 1.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // 🌀 Animated Liquid Glows
        Box(
            modifier = Modifier
                .size(650.dp)
                .alpha(glowAlpha * 0.15f)
                .background(Brush.radialGradient(colors = listOf(accentBlue, Color.Transparent)))
                .blur(120.dp)
        )

        Box(
            modifier = Modifier
                .size(450.dp)
                .alpha(glowAlpha * 0.10f)
                .background(Brush.radialGradient(colors = listOf(accentPurple, Color.Transparent)))
                .blur(90.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

            // 🟠 Premium Logo Container (Hyper-Glassmorphism)
            Box(
                modifier = Modifier
                    .size(120.dp) // Further reduced size
                    .scale(scale.value)
                    .graphicsLayer {
                        shadowElevation = 15f
                        shape = RoundedCornerShape(36.dp)
                        clip = true
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(0.3f),
                                Color.Transparent,
                                accentBlue.copy(0.5f)
                            )
                        ),
                        RoundedCornerShape(36.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.planora),
                    contentDescription = "Planora Logo",
                    modifier = Modifier
                        .size(80.dp) // Further reduced size
                        .alpha(alpha.value)
                )
            }

            Spacer(modifier = Modifier.height(32.dp)) // Reduced spacing

            // ✨ Cinematic Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = textOffset.value.dp)
                    .alpha(textAlpha.value)
            ) {
                Text(
                    text = "PLANORA",
                    fontSize = 24.sp, // Reduced from 32.sp
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 8.sp // Balanced spacing
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Plan smart. Work focused.",
                    fontSize = 12.sp, // Reduced from 14.sp
                    color = Color(0xFF8888A8),
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(80.dp)) // Reduced spacing

            // ✨ Signature Minimal Loader
            CircularProgressIndicator(
                color = accentBlue,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(24.dp)
                    .alpha(textAlpha.value * 0.4f)
            )
        }
    }
}
