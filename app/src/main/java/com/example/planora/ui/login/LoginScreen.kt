package com.example.planora.ui.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.planora.utils.FirebaseUtils

// --- Colors ---
private val ThemeBackground = Color.Black
private val Primary = Color(0xFFADC6FF)
private val PrimaryContainer = Color(0xFF4D8EFF)
private val OnPrimaryContainer = Color(0xFF00285D)
private val OnSurface = Color(0xFFE2E2E2)
private val OnSurfaceVariant = Color(0xFFC2C6D6)
private val Zinc500 = Color(0xFF71717A)

@Composable
fun LoginScreen(navController: NavController) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 60.dp, bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // --- Logo Icon ---
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryContainer, Primary),
                            start = Offset(0f, 0f),
                            end = Offset(150f, 150f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = OnPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Welcome Text ---
            Text(
                text = "Welcome Back",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                lineHeight = 44.sp
            )

            Text(
                text = "Sign in to continue your journey.",
                fontSize = 18.sp,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(56.dp))

            // --- Form Fields ---
            MidnightTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "your@email.com",
                icon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(24.dp))

            MidnightTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "••••••••••••",
                icon = Icons.Default.Lock,
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordVisibilityChange = { passwordVisible = it }
            )

            // Forgot Password Link
            Text(
                text = "Forgot Password?",
                color = Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 12.dp)
                    .clickable { showForgotDialog = true }
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = Color(0xFFFFB4AB),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // --- Login Button ---
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill in all fields"
                        return@Button
                    }
                    isLoading = true
                    FirebaseUtils.auth.signInWithEmailAndPassword(email.trim(), password.trim())
                        .addOnSuccessListener {
                            isLoading = false
                            // Fixed: Correct route name is "today", not "home"
                            navController.navigate("today") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                        .addOnFailureListener {
                            isLoading = false
                            errorMessage = it.localizedMessage ?: "Login failed"
                        }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(listOf(Primary, PrimaryContainer)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = OnPrimaryContainer, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Sign In", color = OnPrimaryContainer, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- Footer ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("New here? ", color = Zinc500)
                Text(
                    text = "Create Account",
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        navController.navigate("register")
                    }
                )
            }
        }
    }

    if (showForgotDialog) {
        var forgotEmail by remember { mutableStateOf(email) }
        var isSending by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSending) showForgotDialog = false },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Enter your email address to receive a reset link.")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSending,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSending,
                    onClick = {
                        val targetEmail = forgotEmail.trim()
                        if (targetEmail.isEmpty()) {
                            Toast.makeText(context, "Please enter an email", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        isSending = true
                        FirebaseUtils.auth.sendPasswordResetEmail(targetEmail)
                            .addOnCompleteListener { task ->
                                isSending = false
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Reset link sent to $targetEmail", Toast.LENGTH_LONG).show()
                                    showForgotDialog = false
                                } else {
                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    }
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Send Link")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSending,
                    onClick = { showForgotDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
