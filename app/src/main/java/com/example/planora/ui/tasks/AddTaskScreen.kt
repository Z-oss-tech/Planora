package com.example.planora.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.planora.utils.FirebaseUtils

// --- Using your Midnight Palette ---
private val ThemeBackground = Color.Black
private val SurfaceContainer = Color(0xFF1F1F1F)
private val Primary = Color(0xFFADC6FF)
private val PrimaryContainer = Color(0xFF4D8EFF)
private val OnPrimaryContainer = Color(0xFF00285D)
private val Zinc500 = Color(0xFF71717A)
private val OnSurface = Color(0xFFE2E2E2)

@Composable
fun AddTaskScreen(navController: NavController) {

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subTaskText by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }

    var category by remember { mutableStateOf("Personal") }

    val subtasks = remember { mutableStateListOf<String>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "New Task",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // TITLE
            Text(
                "TITLE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = Zinc500,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("What needs to be done?", color = Zinc500) },
                textStyle = TextStyle(color = Color.White),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceContainer,
                    unfocusedContainerColor = SurfaceContainer,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PRIORITY
            Text(
                "PRIORITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = Zinc500,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Low", "Medium", "High").forEach { level ->
                    val isSelected = priority == level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                color = if (isSelected) Primary else SurfaceContainer,
                                shape = CircleShape
                            )
                            .clickable { priority = level },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = level,
                            color = if (isSelected) OnPrimaryContainer else OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // ✅ CATEGORY (NEW – SAME DESIGN)
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "CATEGORY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = Zinc500,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Study", "Work", "Personal").forEach { cat ->
                    val isSelected = category == cat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                color = if (isSelected) Primary else SurfaceContainer,
                                shape = CircleShape
                            )
                            .clickable { category = cat },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) OnPrimaryContainer else OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))


            Text(
                "SUBTASKS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = Zinc500,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainer, RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {

                subtasks.forEachIndexed { index, subtask ->
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Primary, CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Text(subtask, color = OnSurface, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Zinc500,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { subtasks.removeAt(index) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = subTaskText,
                        onValueChange = { subTaskText = it },
                        placeholder = { Text("Add a step...", color = Zinc500) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (subTaskText.isNotBlank()) {
                                subtasks.add(subTaskText)
                                subTaskText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // BUTTON
        Button(
            onClick = {
                val userId = FirebaseUtils.auth.currentUser?.uid ?: return@Button

                val taskRef = FirebaseUtils.firestore
                    .collection("users")
                    .document(userId)
                    .collection("tasks")
                    .document()

                val task = Task(
                    id = taskRef.id,
                    title = title,
                    completed = false,
                    priority = priority,
                    category = category, // ✅ SAVED
                    subtasks = subtasks.map {
                        SubTask(title = it, completed = false)
                    }
                )

                taskRef.set(task)
                    .addOnSuccessListener {
                        navController.popBackStack()
                    }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth()
                .height(64.dp),
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
                Text(
                    "Create Task",
                    color = OnPrimaryContainer,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}