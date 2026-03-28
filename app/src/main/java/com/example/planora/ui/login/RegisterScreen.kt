package com.example.planora.ui.login

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.planora.utils.FirebaseUtils
import com.google.firebase.auth.FirebaseAuthUserCollisionException

// --- Colors ---
private val ThemeBackground = Color.Black
private val SurfaceContainer = Color(0xFF1F1F1F)
private val Primary = Color(0xFFADC6FF)
private val PrimaryContainer = Color(0xFF4D8EFF)
private val OnPrimaryContainer = Color(0xFF00285D)
private val Secondary = Color(0xFF45F798)
private val OnSurface = Color(0xFFE2E2E2)
private val OnSurfaceVariant = Color(0xFFC2C6D6)
private val Zinc500 = Color(0xFF71717A)
private val Zinc600 = Color(0xFF52525B)

@Composable
fun RegisterScreen(navController: NavController) {

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // --- Decorative Background Blurs ---


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryContainer, Primary),
                            start = Offset(0f, 0f),
                            end = Offset(100f, 100f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Logo",
                    tint = OnPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Planora",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                lineHeight = 40.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Join the sanctuary of focus.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceVariant,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // --- Form ---
            MidnightTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                placeholder = "Elias Vance",
                icon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(24.dp))

            MidnightTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                placeholder = "hello@midnight.com",
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

            Spacer(modifier = Modifier.height(24.dp))

            MidnightTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password",
                placeholder = "••••••••••••",
                icon = Icons.Default.Lock,
                isPassword = true,
                passwordVisible = confirmPasswordVisible,
                onPasswordVisibilityChange = { confirmPasswordVisible = it }
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = it,
                    color = Color(0xFFFFB4AB),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Register Button ---
            Button(
                onClick = {
                    errorMessage = null

                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Email and password cannot be empty"
                        return@Button
                    }

                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        return@Button
                    }

                    isLoading = true

                    FirebaseUtils.auth
                        .createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->

                            val user = authResult.user
                            val uid = user?.uid

                            val userMap = hashMapOf(
                                "email" to email,
                                "name" to name,
                                "createdAt" to com.google.firebase.Timestamp.now()
                            )

                            FirebaseUtils.firestore
                                .collection("users")
                                .document(uid!!)
                                .set(userMap)
                                .addOnSuccessListener {
                                    FirebaseUtils.auth.signOut()
                                    isLoading = false

                                    navController.navigate("login") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = e.localizedMessage ?: "Failed to save user data"
                                }
                        }
                        .addOnFailureListener { exception ->
                            isLoading = false
                            errorMessage = when (exception) {
                                is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                                    "This email is already registered"
                                else -> exception.localizedMessage ?: "Registration failed"
                            }
                        }
                },
                enabled = !isLoading,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
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
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            color = OnPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Text(
                            text = "Create Account",
                            color = OnPrimaryContainer,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Already have an account? ", color = Zinc500, fontWeight = FontWeight.Medium)
                Text(
                    text = "Log In",
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MidnightTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityChange: ((Boolean) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            color = Zinc500,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text(placeholder, color = Zinc600) },
            leadingIcon = {
                Icon(imageVector = icon, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
            },
            trailingIcon = {
                if (isPassword && onPasswordVisibilityChange != null) {
                    val visibilityIcon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                        Icon(imageVector = visibilityIcon, contentDescription = null, tint = Zinc600)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceContainer,
                unfocusedContainerColor = SurfaceContainer,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = Primary,
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface
            )
        )
    }
}