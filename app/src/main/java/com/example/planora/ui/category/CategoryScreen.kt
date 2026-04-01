package com.example.planora.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.planora.navigation.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private val BG = Color(0xFF060608)
private val Surface1 = Color(0xFF111116)
private val Border = Color(0xFF2A2A38)
private val AccentBlue = Color(0xFF6C8FFF)
private val TextPrimary = Color(0xFFEEEEF6)
private val TextSecondary = Color(0xFF8888A8)

@Composable
fun CategoryScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var selectedCategory by remember { mutableStateOf("All") }

    val tasks = remember { mutableStateListOf<Task>() }

    LaunchedEffect(userId) {
        db.collection("users").document(userId).collection("tasks")
            .addSnapshotListener { snap, _ ->
                tasks.clear()
                snap?.documents?.forEach {
                    it.toObject(Task::class.java)?.copy(id = it.id)?.let { task ->
                        tasks.add(task)
                    }
                }
            }
    }

    val filteredTasks = tasks.filter {
        (selectedCategory == "All" || it.category == selectedCategory) && !it.completed
    }

    // 🔥 DYNAMIC CATEGORY LIST
    val categoryList = remember(tasks) {
        val base = listOf("All", "Study", "Work", "Personal")
        val custom = tasks.map { it.category }
            .filter { it !in base && it.isNotBlank() }
            .distinct()
        base + custom
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
    ) {

        // 🔥 FIXED TOP SPACING (STATUS BAR SAFE)
        Spacer(modifier = Modifier.statusBarsPadding())
        Spacer(modifier = Modifier.height(12.dp))

        // 🔥 TITLE + SUBTITLE (ENHANCED)
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp)
        ) {
            Text(
                text = "Categories",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Organize your tasks smartly",
                fontSize = 12.sp,
                color = TextSecondary.copy(0.8f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 🔥 CATEGORY CHIPS (ENHANCED)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categoryList) { category ->

                val isSelected = selectedCategory == category
                val count = if (category == "All") {
                    tasks.count { !it.completed }
                } else {
                    tasks.count { it.category == category && !it.completed }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected)
                                AccentBlue.copy(alpha = 0.15f)
                            else
                                Surface1.copy(alpha = 0.9f)
                        )
                        .border(
                            1.dp,
                            if (isSelected) AccentBlue else Border,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) AccentBlue else TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )

                        // 🔥 COUNT BADGE
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(AccentBlue.copy(0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = count.toString(),
                                    fontSize = 10.sp,
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🔥 SELECTED CATEGORY HEADER
        Text(
            text = if (selectedCategory == "All") "All Tasks" else "$selectedCategory Tasks",
            color = AccentBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            // 🔥 EMPTY STATE
            if (filteredTasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📭",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tasks here",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 🔥 TASK CARDS (ENHANCED)
            items(filteredTasks) { task ->
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface1)
                        .border(1.dp, Border, RoundedCornerShape(14.dp))
                ) {

                    // 🔥 LEFT ACCENT STRIP
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(AccentBlue.copy(0.7f))
                    )

                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = task.title,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (task.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = task.note,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
