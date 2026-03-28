package com.example.planora.navigation

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import com.example.planora.R
import java.util.*

// ─── Design Tokens ─────────────────────────────────────────────────────────────
private val BG       = Color(0xFF060608)
private val Surface1 = Color(0xFF111116)
private val Surface2 = Color(0xFF1A1A22)
private val Surface3 = Color(0xFF22222E)
private val Border   = Color(0xFF2A2A38)

private val AccentBlue   = Color(0xFF6C8FFF)
private val AccentGreen  = Color(0xFF34D48C)
private val AccentRose   = Color(0xFFFF6B8A)
private val AccentAmber  = Color(0xFFFFB547)
private val AccentPurple = Color(0xFFAA80FF)

private val TextPrimary   = Color(0xFFEEEEF6)
private val TextSecondary = Color(0xFF8888A8)
private val TextTertiary  = Color(0xFF444458)

// ─── Priority ──────────────────────────────────────────────────────────────────
enum class Priority(val label: String, val color: Color) {
    LOW("Low", AccentGreen),
    NORMAL("Normal", AccentBlue),
    HIGH("High", AccentAmber),
    URGENT("Urgent", AccentRose)
}

// ─── Data Models ───────────────────────────────────────────────────────────────
data class SubTask(
    val id: String = "",
    val title: String = "",
    val completed: Boolean = false
)

data class HabitData(
    val id: String = "",
    val title: String = "",
    val icon: String = "star",
    val active: Boolean = true
)

data class Task(
    val id: String = "",
    val title: String = "",
    val note: String = "",
    val completed: Boolean = false,
    val date: Long = System.currentTimeMillis(),
    val priority: String = "Normal",
    val category: String = "Personal",
    val recurrence: String = "None",
    val lastReset: Long = System.currentTimeMillis(),
    val subtasks: List<SubTask> = emptyList()
)

// ─── Main Screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(navController: NavController) {
    val auth     = FirebaseAuth.getInstance()
    val db       = FirebaseFirestore.getInstance()
    val userId   = auth.currentUser?.uid ?: ""
    var username by remember { mutableStateOf("there") }
    val context  = LocalContext.current
    val focusMgr = LocalFocusManager.current

    val todayDate  = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
    val todayDocId = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    var quickAddText    by remember { mutableStateOf("") }
    var showAddDialog   by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") } // ✅ ADDED

    val habits          = remember { mutableStateListOf<HabitData>() }
    val tasks           = remember { mutableStateListOf<Task>() }
    val completedHabits = remember { mutableStateListOf<String>() }
    val listState       = rememberLazyListState()

    // ── Firebase Listeners ────────────────────────────────────────────────────
    LaunchedEffect(userId) {
        if (userId.isEmpty()) return@LaunchedEffect

        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                val nameFromFirestore = snapshot?.getString("name")
                    ?: snapshot?.getString("username")
                    ?: auth.currentUser?.displayName
                    ?: "there"
                username = nameFromFirestore
            }

        db.collection("users").document(userId).collection("habits")
            .whereEqualTo("active", true)
            .addSnapshotListener { snap, err ->
                if (err != null) { Toast.makeText(context, "Habits load failed", Toast.LENGTH_SHORT).show(); return@addSnapshotListener }
                habits.clear()
                snap?.documents?.forEach { doc ->
                    doc.toObject(HabitData::class.java)?.copy(id = doc.id)?.let { habits.add(it) }
                }
            }

        db.collection("users").document(userId).collection("tasks")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { Toast.makeText(context, "Tasks load failed", Toast.LENGTH_SHORT).show(); return@addSnapshotListener }
                tasks.clear()
                snap?.documents?.forEach { doc ->
                    doc.toObject(Task::class.java)?.copy(id = doc.id)?.let { tasks.add(it) }
                }
            }

        db.collection("users").document(userId).collection("habit_logs")
            .document(todayDocId)
            .addSnapshotListener { snap, _ ->
                completedHabits.clear()
                snap?.data?.forEach { (id, v) -> if (v == true) completedHabits.add(id) }
            }
    }

    // ── Filtering ─────────────────────────────────────────────────────────────
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
    }
    val todayStart = calendar.timeInMillis

// ✅ CATEGORY FILTER
    val filteredTasks = tasks.filter {
        selectedCategory == "All" || it.category == selectedCategory
    }

    val overdueTasks = filteredTasks.filter {
        !it.completed && it.date < todayStart
    }

    val todayTasks = filteredTasks.filter {
        it.date >= todayStart && !it.completed
    }

    val completedTasks = filteredTasks.filter {
        it.completed
    }

    val totalToday = todayTasks.size + completedTasks.count { it.date >= todayStart }
    val doneToday  = completedTasks.count { it.date >= todayStart }
    val taskProg   = if (totalToday == 0) 0f else doneToday.toFloat() / totalToday
    val habitProg  = if (habits.isEmpty()) 0f else completedHabits.size.toFloat() / habits.size
    // ── Task Helpers ──────────────────────────────────────────────────────────
    fun addTask(
        title: String,
        note: String = "",
        priority: Priority = Priority.NORMAL,
        recurrence: String = "None",
        subtasks: List<SubTask> = emptyList(),
        category: String = "Personal"
    ){
        if (title.isBlank()) return

        val subtasksData = subtasks.map {
            mapOf(
                "id" to it.id,
                "title" to it.title,
                "completed" to it.completed
            )
        }

        val doc = hashMapOf(
            "title"      to title.trim(),
            "note"       to note.trim(),
            "completed"  to false,
            "date"       to System.currentTimeMillis(),
            "priority"   to priority.label,
            "category"   to category, // ✅ FIXED (DYNAMIC)
            "recurrence" to recurrence,
            "lastReset"  to System.currentTimeMillis(),
            "subtasks"   to subtasksData
        )

        db.collection("users").document(userId).collection("tasks").add(doc)
            .addOnFailureListener { Toast.makeText(context, "Couldn't save task", Toast.LENGTH_SHORT).show() }
    }

    fun updateTask(taskId: String, fields: Map<String, Any>) {
        db.collection("users").document(userId).collection("tasks").document(taskId)
            .update(fields)
            .addOnFailureListener { Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show() }
    }

    fun deleteTask(taskId: String) {
        db.collection("users").document(userId).collection("tasks").document(taskId)
            .delete()
            .addOnFailureListener { Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show() }
    }

    // ── UI Root ───────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .pointerInput(Unit) { detectTapGestures { focusMgr.clearFocus() } }
    ) {
        // Subtle ambient glow - static, no animation
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.08f),
                    radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.85f, size.height * 0.08f)
            )
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            TopBar(username = username, onProfileClick = { showProfileMenu = true })

            LazyColumn(
                state          = listState,
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {

                // Greeting + stat cards
                item {
                    GreetingSection(
                        todayDate  = todayDate,
                        username   = username,
                        taskProg   = taskProg,
                        doneToday  = doneToday,
                        totalToday = totalToday,
                        habitProg  = habitProg,
                        habitDone  = completedHabits.size,
                        habitTotal = habits.size
                    )
                }

                // Habits
                if (habits.isNotEmpty()) {
                    item {
                        HabitStrip(
                            habits       = habits,
                            completedIds = completedHabits,
                            onToggle     = { habit ->
                                val nowDone = !completedHabits.contains(habit.id)
                                db.collection("users").document(userId)
                                    .collection("habit_logs").document(todayDocId)
                                    .update(habit.id, nowDone)
                                    .addOnFailureListener {
                                        db.collection("users").document(userId)
                                            .collection("habit_logs").document(todayDocId)
                                            .set(mapOf(habit.id to nowDone))
                                    }
                            }
                        )
                    }
                }

                // Quick add
                item {
                    QuickAddBar(
                        value         = quickAddText,
                        onValueChange = { quickAddText = it },
                        onAdd         = {
                            addTask(quickAddText)
                            quickAddText = ""
                            focusMgr.clearFocus()
                        }
                    )
                }

                // Overdue
                if (overdueTasks.isNotEmpty()) {
                    item {
                        SectionLabel(
                            title       = "Overdue",
                            count       = overdueTasks.size,
                            accentColor = AccentRose,
                            icon        = Icons.Filled.Warning,
                            modifier    = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(overdueTasks, key = { it.id }) { task ->
                        SwipeableTaskCard(
                            task             = task,
                            accentColor      = AccentRose,
                            onDelete         = { deleteTask(task.id) },
                            onToggleComplete = { done -> updateTask(task.id, mapOf("completed" to done)) },
                            onSubtaskToggle  = { subtaskId, done ->
                                val updated = task.subtasks.map {
                                    if (it.id == subtaskId) it.copy(completed = done) else it
                                }
                                val data = updated.map { mapOf("id" to it.id, "title" to it.title, "completed" to it.completed) }
                                updateTask(task.id, mapOf("subtasks" to data))
                            }
                        )
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                // Today
                item {
                    SectionLabel(
                        title       = "Today's Focus",
                        count       = todayTasks.size,
                        accentColor = AccentBlue,
                        icon        = Icons.Filled.Star,
                        modifier    = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                if (todayTasks.isEmpty()) {
                    item { EmptyState() }
                } else {
                    items(todayTasks, key = { it.id }) { task ->
                        SwipeableTaskCard(
                            task             = task,
                            accentColor      = priorityColor(task.priority),
                            onDelete         = { deleteTask(task.id) },
                            onToggleComplete = { done -> updateTask(task.id, mapOf("completed" to done)) },
                            onSubtaskToggle  = { subtaskId, done ->
                                val updated = task.subtasks.map {
                                    if (it.id == subtaskId) it.copy(completed = done) else it
                                }
                                val data = updated.map { mapOf("id" to it.id, "title" to it.title, "completed" to it.completed) }
                                updateTask(task.id, mapOf("subtasks" to data))
                            }
                        )
                    }
                }

                // Completed
                if (completedTasks.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        SectionLabel(
                            title       = "Completed",
                            count       = completedTasks.size,
                            accentColor = AccentGreen,
                            icon        = Icons.Filled.CheckCircle,
                            modifier    = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(completedTasks, key = { it.id }) { task ->
                        SwipeableTaskCard(
                            task             = task,
                            accentColor      = AccentGreen,
                            onDelete         = { deleteTask(task.id) },
                            onToggleComplete = { done -> updateTask(task.id, mapOf("completed" to done)) },
                            onSubtaskToggle  = { subtaskId, done ->
                                val updated = task.subtasks.map {
                                    if (it.id == subtaskId) it.copy(completed = done) else it
                                }
                                val data = updated.map { mapOf("id" to it.id, "title" to it.title, "completed" to it.completed) }
                                updateTask(task.id, mapOf("subtasks" to data))
                            }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick            = { showAddDialog = true },
            modifier           = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
            shape              = CircleShape,
            containerColor     = AccentBlue,
            contentColor       = Color.White,
            elevation          = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add task", modifier = Modifier.size(24.dp))
        }

        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, note, priority, recurrence, subtasks, category ->
                    addTask(
                        title = title,
                        note = note,
                        priority = priority,
                        category = category,
                        recurrence = recurrence,
                        subtasks = subtasks
                    )
                    showAddDialog = false
                }
            )
        }

        if (showProfileMenu) {
            ProfileMenu(
                username  = username,
                email     = auth.currentUser?.email ?: "",
                onDismiss = { showProfileMenu = false },
                onSignOut = {
                    auth.signOut()
                    showProfileMenu = false
                    navController.navigate("login") { popUpTo(0) }
                }
            )
        }
    }
}

// ─── Top Bar ───────────────────────────────────────────────────────────────────
@Composable
private fun TopBar(username: String, onProfileClick: () -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 🔥 LOGO SECTION (PREMIUM)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 🔥 GLASS LOGO CONTAINER
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF6C8FFF).copy(0.25f),
                                Color(0xFFAA80FF).copy(0.25f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Color.White.copy(0.08f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                // 👉 USE YOUR LOGO HERE
                Image(
                    painter = painterResource(id = R.drawable.planora),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(50.dp)
                )
            }

            Column {
                Text(
                    text = "Planora",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary,
                    letterSpacing = 0.3.sp
                )

                Text(
                    text = "Stay organized",
                    fontSize = 11.sp,
                    color = TextSecondary.copy(0.7f)
                )
            }
        }

        // 🔥 PROFILE (ENHANCED)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Surface2,
                            Surface3
                        )
                    )
                )
                .border(1.dp, Border, CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (username.firstOrNull() ?: '?').uppercaseChar().toString(),
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
// ─── Greeting Section ──────────────────────────────────────────────────────────
@Composable
private fun GreetingSection(
    todayDate:  String,
    username:   String,
    taskProg:   Float,
    doneToday:  Int,
    totalToday: Int,
    habitProg:  Float,
    habitDone:  Int,
    habitTotal: Int
) {
    val hour      = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting  = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
    val firstName = username.split(" ").firstOrNull() ?: username

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            todayDate.uppercase(),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color         = TextTertiary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$greeting, $firstName",
            fontSize      = 26.sp,
            fontWeight    = FontWeight.Bold,
            color         = TextPrimary,
            letterSpacing = (-0.5).sp
        )
        Spacer(Modifier.height(16.dp))

        // Stat cards row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularStatCard(
                modifier   = Modifier.weight(1f),
                label      = "Tasks",
                value      = "$doneToday / $totalToday",
                subtitle   = "done today",
                progress   = taskProg,
                ringColor  = AccentBlue
            )
            if (habitTotal > 0) {
                CircularStatCard(
                    modifier   = Modifier.weight(1f),
                    label      = "Habits",
                    value      = "$habitDone / $habitTotal",
                    subtitle   = "completed",
                    progress   = habitProg,
                    ringColor  = AccentGreen
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ─── Circular Stat Card ────────────────────────────────────────────────────────
@Composable
private fun CircularStatCard(
    modifier:  Modifier,
    label:     String,
    value:     String,
    subtitle:  String,
    progress:  Float,
    ringColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .border(1.dp, Border, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 5.dp.toPx()
                    val stroke   = Stroke(width = strokePx, cap = StrokeCap.Round)
                    val inset    = strokePx / 2f
                    val arcRect  = Size(size.width - strokePx, size.height - strokePx)
                    val arcOff   = Offset(inset, inset)
                    drawArc(color = Surface3, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = arcOff, size = arcRect, style = stroke)
                    if (progress > 0f) {
                        drawArc(color = ringColor, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false, topLeft = arcOff, size = arcRect, style = stroke)
                    }
                }
                Text(
                    text       = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = ringColor
                )
            }

            Column {
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(Modifier.height(1.dp))
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, fontSize = 10.sp, color = TextTertiary)
            }
        }
    }
}

// ─── Habit Strip ───────────────────────────────────────────────────────────────
@Composable
private fun HabitStrip(habits: List<HabitData>, completedIds: List<String>, onToggle: (HabitData) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            "DAILY HABITS",
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color         = TextTertiary,
            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(habits, key = { it.id }) { habit ->
                HabitPill(habit = habit, isDone = completedIds.contains(habit.id), onToggle = { onToggle(habit) })
            }
        }
    }
}

@Composable
private fun HabitPill(habit: HabitData, isDone: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDone) AccentGreen.copy(0.12f) else Surface1)
            .border(1.dp, if (isDone) AccentGreen.copy(0.4f) else Border, RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector        = if (isDone) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint               = if (isDone) AccentGreen else TextTertiary,
                modifier           = Modifier.size(14.dp)
            )
            Text(
                habit.title,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                color      = if (isDone) AccentGreen else TextSecondary
            )
        }
    }
}

// ─── Quick Add Bar ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddBar(value: String, onValueChange: (String) -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text("Quick add a task...", color = TextTertiary, fontSize = 14.sp) },
            modifier      = Modifier.weight(1f),
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary,
                cursorColor             = AccentBlue
            ),
            textStyle       = TextStyle(fontSize = 14.sp),
            singleLine      = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() })
        )
        if (value.isNotBlank()) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentBlue)
                    .clickable { onAdd() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Send, "Add", tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

// ─── Section Label ─────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(title: String, count: Int, accentColor: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = accentColor, modifier = Modifier.size(13.dp))
        Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor, letterSpacing = 1.5.sp)
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accentColor.copy(0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(count.toString(), fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Swipeable Task Card ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskCard(
    task:             Task,
    accentColor:      Color,
    onDelete:         () -> Unit,
    onToggleComplete: (Boolean) -> Unit,
    onSubtaskToggle:  (subtaskId: String, done: Boolean) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onToggleComplete(!task.completed); false }
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state             = dismissState,
        modifier          = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        backgroundContent = {
            val dir = dismissState.dismissDirection
            val (bg, icon, align) = when (dir) {
                SwipeToDismissBoxValue.StartToEnd -> Triple(AccentGreen.copy(0.13f), Icons.Filled.CheckCircle, Alignment.CenterStart)
                SwipeToDismissBoxValue.EndToStart -> Triple(AccentRose.copy(0.13f),  Icons.Filled.Delete,      Alignment.CenterEnd)
                else                             -> Triple(Color.Transparent, Icons.Filled.CheckCircle, Alignment.Center)
            }
            Box(
                modifier         = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(bg),
                contentAlignment = align
            ) {
                Icon(
                    icon, null,
                    tint     = if (dir == SwipeToDismissBoxValue.EndToStart) AccentRose else AccentGreen,
                    modifier = Modifier.padding(horizontal = 18.dp).size(20.dp)
                )
            }
        }
    ) {
        TaskCard(task = task, accentColor = accentColor, onToggleComplete = onToggleComplete, onSubtaskToggle = onSubtaskToggle)
    }
}

// ─── Task Card (One UI style) ─────────────────────────────────────────────────
@Composable
private fun TaskCard(
    task:             Task,
    accentColor:      Color,
    onToggleComplete: (Boolean) -> Unit,
    onSubtaskToggle:  (subtaskId: String, done: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
    ) {
        // ── Main row ────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(if (task.completed) 0.3f else 0.85f))
            )

            Spacer(Modifier.width(12.dp))

            // Checkbox
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (task.completed) AccentGreen.copy(0.15f) else Color.Transparent)
                    .border(1.5.dp, if (task.completed) AccentGreen else TextTertiary, CircleShape)
                    .clickable { onToggleComplete(!task.completed) },
                contentAlignment = Alignment.Center
            ) {
                if (task.completed) {
                    Icon(Icons.Filled.Check, null, tint = AccentGreen, modifier = Modifier.size(12.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    color          = if (task.completed) TextSecondary else TextPrimary,
                    fontWeight     = FontWeight.Medium,
                    fontSize       = 15.sp,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    maxLines       = 2,
                    overflow       = TextOverflow.Ellipsis
                )
                if (task.note.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(task.note, color = TextTertiary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))

                // Badges row
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    val pEnum = Priority.values().firstOrNull { it.label == task.priority } ?: Priority.NORMAL
                    SmallBadge(text = task.priority, color = pEnum.color)
                    if (task.recurrence != "None") SmallBadge(text = task.recurrence, color = AccentPurple)
                    val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(task.date))
                    Text(dateStr, fontSize = 10.sp, color = TextTertiary)
                }

                // Subtask inline progress (if any)
                if (task.subtasks.isNotEmpty()) {
                    val subDone  = task.subtasks.count { it.completed }
                    val subTotal = task.subtasks.size
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(Surface3)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (subTotal == 0) 0f else subDone.toFloat() / subTotal)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(AccentBlue)
                            )
                        }
                        Text("$subDone/$subTotal", fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ── Subtask list ──────────────────────────────────────────────────────
        if (task.subtasks.isNotEmpty()) {
            HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 14.dp))
            Column(modifier = Modifier.padding(start = 41.dp, end = 14.dp, top = 6.dp, bottom = 10.dp)) {
                task.subtasks.forEach { subtask ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable { onSubtaskToggle(subtask.id, !subtask.completed) }
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (subtask.completed) AccentGreen.copy(0.15f) else Color.Transparent)
                                .border(1.dp, if (subtask.completed) AccentGreen else TextTertiary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (subtask.completed) {
                                Icon(Icons.Filled.Check, null, tint = AccentGreen, modifier = Modifier.size(9.dp))
                            }
                        }
                        Text(
                            subtask.title,
                            fontSize       = 13.sp,
                            color          = if (subtask.completed) TextTertiary else TextSecondary,
                            textDecoration = if (subtask.completed) TextDecoration.LineThrough else null,
                            modifier       = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(0.10f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

// ─── Empty State ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyState() {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier         = Modifier.size(56.dp).clip(CircleShape).background(AccentBlue.copy(0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.CheckCircle, null, tint = AccentBlue.copy(0.4f), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("All clear!", fontWeight = FontWeight.SemiBold, color = TextSecondary, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Your focus list is empty.\nAdd a task to get started.",
            textAlign  = TextAlign.Center,
            color      = TextTertiary,
            fontSize   = 13.sp,
            lineHeight = 18.sp
        )
    }
}

// ─── Add Task Dialog ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        note: String,
        priority: Priority,
        recurrence: String,
        subtasks: List<SubTask>,
        category: String
    ) -> Unit
)
{
    var title            by remember { mutableStateOf("") }
    var note             by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.NORMAL) }
    var selectedRecur    by remember { mutableStateOf("None") }
    val subtasks         = remember { mutableStateListOf<SubTask>() }
    var subtaskInput     by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Personal") } // ✅ ADD
    val focusMgr         = LocalFocusManager.current
    val recurrenceOpts   = listOf("None", "Daily", "Weekly")

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Surface1)
                .border(1.dp, Border, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {

                // Header
                Text("New Task", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                Spacer(Modifier.height(16.dp))

                // Title
                DialogLabel("Title")
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    placeholder   = { Text("e.g. Buy groceries", color = TextTertiary, fontSize = 13.sp) },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = outlinedFieldColors(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(12.dp))

                // Note
                DialogLabel("Note")
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it },
                    placeholder   = { Text("Optional note...", color = TextTertiary, fontSize = 13.sp) },
                    modifier      = Modifier.fillMaxWidth().height(80.dp),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = outlinedFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusMgr.clearFocus() })
                )
                Spacer(Modifier.height(14.dp))

                // Priority
                DialogLabel("Priority")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Priority.values().forEach { p ->
                        SelectChip(
                            text     = p.label,
                            selected = p == selectedPriority,
                            color    = p.color,
                            onClick  = { selectedPriority = p }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                DialogLabel("Category")
                Spacer(Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Study", "Work", "Personal").forEach { cat ->
                        SelectChip(
                            text = cat,
                            selected = cat == selectedCategory,
                            color = AccentBlue,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Recurrence
                DialogLabel("Repeat")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    recurrenceOpts.forEach { opt ->
                        SelectChip(
                            text     = opt,
                            selected = opt == selectedRecur,
                            color    = AccentPurple,
                            onClick  = { selectedRecur = opt }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                // Subtasks
                DialogLabel("Subtasks")
                Spacer(Modifier.height(6.dp))

                // Subtask input row
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value         = subtaskInput,
                        onValueChange = { subtaskInput = it },
                        placeholder   = { Text("Add subtask...", color = TextTertiary, fontSize = 13.sp) },
                        modifier      = Modifier.weight(1f),
                        shape         = RoundedCornerShape(10.dp),
                        colors        = outlinedFieldColors(),
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (subtaskInput.isNotBlank()) {
                                subtasks.add(SubTask(id = UUID.randomUUID().toString(), title = subtaskInput.trim()))
                                subtaskInput = ""
                            }
                        })
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (subtaskInput.isNotBlank()) AccentBlue else Surface3)
                            .clickable(enabled = subtaskInput.isNotBlank()) {
                                if (subtaskInput.isNotBlank()) {
                                    subtasks.add(SubTask(id = UUID.randomUUID().toString(), title = subtaskInput.trim()))
                                    subtaskInput = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, null, tint = if (subtaskInput.isNotBlank()) Color.White else TextTertiary, modifier = Modifier.size(18.dp))
                    }
                }

                // Subtask list
                if (subtasks.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface2)
                            .border(1.dp, Border, RoundedCornerShape(12.dp))
                    ) {
                        subtasks.forEachIndexed { index, subtask ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AccentBlue.copy(0.5f))
                                )
                                Text(subtask.title, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint               = TextTertiary,
                                    modifier           = Modifier
                                        .size(16.dp)
                                        .clickable { subtasks.removeAt(index) }
                                )
                            }
                            if (index < subtasks.lastIndex) {
                                HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 12.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Action buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = { onDismiss() },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, Border),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(
                                    title,
                                    note,
                                    selectedPriority,
                                    selectedRecur,
                                    subtasks.toList(),
                                    selectedCategory
                                )
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = Color.White,
                            disabledContainerColor = Surface3,
                            disabledContentColor = TextTertiary
                        )
                    ) {
                        Text("Add Task", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
}

@Composable
private fun SelectChip(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(0.15f) else Surface2)
            .border(1.dp, if (selected) color.copy(0.45f) else Border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            fontSize   = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color      = if (selected) color else TextTertiary
        )
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor        = AccentBlue.copy(0.6f),
    unfocusedBorderColor      = Border,
    focusedTextColor          = TextPrimary,
    unfocusedTextColor        = TextPrimary,
    cursorColor               = AccentBlue,
    focusedContainerColor     = Surface2,
    unfocusedContainerColor   = Surface2
)

// ─── Profile Menu ──────────────────────────────────────────────────────────────
@Composable
private fun ProfileMenu(username: String, email: String, onDismiss: () -> Unit, onSignOut: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Surface1)
                .border(1.dp, Border, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Surface2)
                            .border(1.dp, Border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = (username.firstOrNull() ?: '?').uppercaseChar().toString(),
                            color      = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp
                        )
                    }
                    Column {
                        Text(username, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text(email, fontSize = 12.sp, color = TextSecondary)
                    }
                }

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = Border)
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentRose.copy(0.07f))
                        .border(1.dp, AccentRose.copy(0.15f), RoundedCornerShape(12.dp))
                        .clickable { onSignOut() }
                        .padding(14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.ExitToApp, null, tint = AccentRose, modifier = Modifier.size(17.dp))
                    Text("Sign Out", color = AccentRose, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ─── Helpers ───────────────────────────────────────────────────────────────────
private fun priorityColor(priority: String): Color =
    Priority.values().firstOrNull { it.label == priority }?.color ?: AccentBlue