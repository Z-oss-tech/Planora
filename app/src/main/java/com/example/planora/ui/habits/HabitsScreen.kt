package com.example.planora.ui.habits

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

// ─── Design tokens (shared with TaskListScreen) ────────────────────────────────
private val HBG       = Color(0xFF060608)
private val HSurface1 = Color(0xFF111116)
private val HSurface2 = Color(0xFF1A1A22)
private val HSurface3 = Color(0xFF22222E)
private val HBorder   = Color(0xFF2A2A38)

private val HBlue   = Color(0xFF6C8FFF)
private val HGreen  = Color(0xFF34D48C)
private val HRose   = Color(0xFFFF6B8A)
private val HAmber  = Color(0xFFFFB547)
private val HPurple = Color(0xFFAA80FF)
private val HTeal   = Color(0xFF34C4C4)

private val HTextPrimary   = Color(0xFFEEEEF6)
private val HTextSecondary = Color(0xFF8888A8)
private val HTextTertiary  = Color(0xFF444458)

// ─── Available icons & accent colours ─────────────────────────────────────────
data class IconOption(val name: String, val vector: ImageVector, val tint: Color)

val HABIT_ICONS = listOf(
    IconOption("star",           Icons.Filled.Star,           HAmber),
    IconOption("fitness_center", Icons.Filled.FitnessCenter,  HRose),
    IconOption("menu_book",      Icons.Filled.MenuBook,       HBlue),
    IconOption("water_drop",     Icons.Filled.WaterDrop,      HTeal),
    IconOption("bedtime",        Icons.Filled.Bedtime,        HPurple),
    IconOption("directions_run", Icons.Filled.DirectionsRun,  HGreen),
    IconOption("self_improvement",Icons.Filled.SelfImprovement,HPurple),
    IconOption("restaurant",     Icons.Filled.Restaurant,     HAmber),
    IconOption("favorite",       Icons.Filled.Favorite,       HRose),
    IconOption("music_note",     Icons.Filled.MusicNote,      HBlue),
    IconOption("code",           Icons.Filled.Code,           HGreen),
    IconOption("brush",          Icons.Filled.Brush,          HRose),
    IconOption("pets",           Icons.Filled.Pets,           HAmber),
    IconOption("eco",            Icons.Filled.Eco,            HGreen),
    IconOption("local_cafe",     Icons.Filled.LocalCafe,      HAmber),
    IconOption("psychology",     Icons.Filled.Psychology,     HPurple),
    IconOption("hiking",         Icons.Filled.Hiking,         HGreen),
    IconOption("spa",            Icons.Filled.Spa,            HRose),
    IconOption("sunny",          Icons.Filled.WbSunny,        HAmber),
    IconOption("savings",        Icons.Filled.Savings,        HGreen),
)

fun iconOptionByName(name: String): IconOption =
    HABIT_ICONS.firstOrNull { it.name == name } ?: HABIT_ICONS.first()

// ─── Data models ───────────────────────────────────────────────────────────────
data class Habit(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val icon: String = "star",
    val createdAt: Long = System.currentTimeMillis(),
    val active: Boolean = true
)

// log: date string → completed bool
data class HabitLog(val date: String = "", val completed: Boolean = false)

data class HabitWithLogs(
    val habit: Habit,
    val logs: Map<String, Boolean> = emptyMap()   // "yyyy-MM-dd" → completed
) {
    val streak: Int get() {
        var count = 0
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        // start from yesterday if today not done, else today
        val todayStr = fmt.format(cal.time)
        if (logs[todayStr] != true) cal.add(Calendar.DAY_OF_YEAR, -1)
        while (true) {
            val key = fmt.format(cal.time)
            if (logs[key] == true) { count++; cal.add(Calendar.DAY_OF_YEAR, -1) }
            else break
        }
        return count
    }
    val totalCompleted: Int get() = logs.values.count { it }
}

// ─── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(navController: NavController) {
    val auth    = FirebaseAuth.getInstance()
    val db      = FirebaseFirestore.getInstance()
    val userId  = auth.currentUser?.uid ?: ""
    val context = LocalContext.current
    val focus   = LocalFocusManager.current

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // State
    val habitsWithLogs = remember { mutableStateListOf<HabitWithLogs>() }
    var showAddDialog  by remember { mutableStateOf(false) }
    var editingHabit   by remember { mutableStateOf<Habit?>(null) }
    var deletingHabit  by remember { mutableStateOf<Habit?>(null) }

    // ── Firestore reference — users/{userId}/habits (same pattern as tasks) ──────
    val habitsRef = db.collection("users").document(userId).collection("habits")

    // ── Load habits + their logs ───────────────────────────────────────────────
    LaunchedEffect(userId) {
        if (userId.isEmpty()) return@LaunchedEffect

        // Only filter active=true. No orderBy to avoid needing a composite index.
        // Sort by createdAt in-memory instead.
        habitsRef
            .whereEqualTo("active", true)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Toast.makeText(context, "Failed to load habits: ${err.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val habits = snap?.documents
                    ?.mapNotNull { doc -> doc.toObject(Habit::class.java)?.copy(id = doc.id) }
                    ?.sortedBy { it.createdAt }
                    ?: emptyList()

                if (habits.isEmpty()) {
                    habitsWithLogs.clear()
                    return@addSnapshotListener
                }

                // Seed list immediately so the UI shows habits right away
                val seeded = habits.map { habit ->
                    HabitWithLogs(
                        habit = habit,
                        logs  = habitsWithLogs.firstOrNull { it.habit.id == habit.id }?.logs ?: emptyMap()
                    )
                }
                habitsWithLogs.clear()
                habitsWithLogs.addAll(seeded)

                // Load logs subcollection for each habit
                // Path: users/{userId}/habits/{habitId}/logs
                habits.forEach { habit ->
                    habitsRef.document(habit.id).collection("logs")
                        .limit(90)
                        .get()
                        .addOnSuccessListener { logSnap ->
                            // Document ID is the date string (yyyy-MM-dd)
                            val logsMap = logSnap.documents.associate { d ->
                                d.id to (d.getBoolean("completed") ?: false)
                            }
                            val idx = habitsWithLogs.indexOfFirst { it.habit.id == habit.id }
                            if (idx >= 0) habitsWithLogs[idx] = habitsWithLogs[idx].copy(logs = logsMap)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Logs error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    fun toggleLog(habit: Habit, dateStr: String, currentlyDone: Boolean) {
        // Path: users/{userId}/habits/{habitId}/logs/{date}
        val logRef = habitsRef.document(habit.id).collection("logs").document(dateStr)
        if (currentlyDone) {
            logRef.delete()
        } else {
            logRef.set(mapOf("date" to dateStr, "completed" to true))
        }
        // Optimistic UI update
        val idx = habitsWithLogs.indexOfFirst { it.habit.id == habit.id }
        if (idx >= 0) {
            val current = habitsWithLogs[idx]
            val newLogs = current.logs.toMutableMap()
            if (currentlyDone) newLogs.remove(dateStr) else newLogs[dateStr] = true
            habitsWithLogs[idx] = current.copy(logs = newLogs)
        }
    }

    fun saveHabit(title: String, icon: String, editId: String?) {
        if (title.isBlank()) return
        if (editId != null) {
            // Edit: update in place at users/{userId}/habits/{editId}
            habitsRef.document(editId)
                .update(mapOf("title" to title.trim(), "icon" to icon))
                .addOnFailureListener { Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show() }
        } else {
            // Create: add to users/{userId}/habits
            val doc = hashMapOf(
                "userId"    to userId,
                "title"     to title.trim(),
                "icon"      to icon,
                "createdAt" to System.currentTimeMillis(),
                "active"    to true
            )
            habitsRef.add(doc)
                .addOnFailureListener { Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show() }
        }
    }

    fun archiveHabit(habitId: String) {
        // Soft-delete: set active=false, preserves log history
        habitsRef.document(habitId)
            .update("active", false)
            .addOnFailureListener { Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show() }
    }

    // ── UI ─────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HBG)
            .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } }
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            // Top bar
            HabitsTopBar(onAdd = { showAddDialog = true })

            if (habitsWithLogs.isEmpty()) {
                EmptyHabitsState(onAdd = { showAddDialog = true })
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Today summary card
                    item {
                        val doneToday = habitsWithLogs.count { it.logs[todayStr] == true }
                        val total     = habitsWithLogs.size
                        TodaySummaryCard(done = doneToday, total = total)
                    }

                    // Section header
                    item {
                        Text(
                            "MY HABITS",
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color         = HTextTertiary,
                            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Habit cards
                    items(habitsWithLogs, key = { it.habit.id }) { hwl ->
                        HabitCard(
                            hwl       = hwl,
                            todayStr  = todayStr,
                            onToggle  = { date, done -> toggleLog(hwl.habit, date, done) },
                            onEdit    = { editingHabit = hwl.habit },
                            onDelete  = { deletingHabit = hwl.habit }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick        = { showAddDialog = true },
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
            shape          = CircleShape,
            containerColor = HBlue,
            contentColor   = Color.White,
            elevation      = FloatingActionButtonDefaults.elevation(6.dp)
        ) {
            Icon(Icons.Filled.Add, "Add habit", modifier = Modifier.size(24.dp))
        }

        // Add / Edit dialog
        if (showAddDialog || editingHabit != null) {
            AddEditHabitDialog(
                existingHabit = editingHabit,
                onDismiss     = { showAddDialog = false; editingHabit = null },
                onConfirm     = { title, icon ->
                    saveHabit(title, icon, editingHabit?.id)
                    showAddDialog = false; editingHabit = null
                }
            )
        }

        // Delete confirmation
        if (deletingHabit != null) {
            DeleteHabitDialog(
                habit     = deletingHabit!!,
                onDismiss = { deletingHabit = null },
                onConfirm = { archiveHabit(deletingHabit!!.id); deletingHabit = null }
            )
        }
    }
}

// ─── Top Bar ───────────────────────────────────────────────────────────────────
@Composable
private fun HabitsTopBar(onAdd: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text("Habits", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = HTextPrimary)
        IconButton(onClick = onAdd) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, "Add", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─── Today Summary ─────────────────────────────────────────────────────────────
@Composable
private fun TodaySummaryCard(done: Int, total: Int) {
    val progress = if (total == 0) 0f else done.toFloat() / total

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(HBlue.copy(0.18f), HPurple.copy(0.12f)))
            )
            .border(1.dp, HBlue.copy(0.25f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    "Today's Progress",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = HTextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$done of $total done",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = HTextPrimary
                )
                Spacer(Modifier.height(12.dp))
                // Progress bar
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(HSurface3)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(HBlue, HPurple)))
                    )
                }
            }

            // Circle ring
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 7.dp.toPx()
                    val style    = androidx.compose.ui.graphics.drawscope.Stroke(strokePx, cap = StrokeCap.Round)
                    val inset    = strokePx / 2f
                    val arcSize  = androidx.compose.ui.geometry.Size(size.width - strokePx, size.height - strokePx)
                    val arcOff   = androidx.compose.ui.geometry.Offset(inset, inset)
                    drawArc(color = HSurface3, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = arcOff, size = arcSize, style = style)
                    if (progress > 0f) {
                        drawArc(
                            brush     = Brush.sweepGradient(listOf(HBlue, HPurple)),
                            startAngle = -90f, sweepAngle = 360f * progress,
                            useCenter  = false, topLeft = arcOff, size = arcSize, style = style
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(progress * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HBlue)
                }
            }
        }
    }
}

// ─── Habit Card ────────────────────────────────────────────────────────────────
@Composable
private fun HabitCard(
    hwl:      HabitWithLogs,
    todayStr: String,
    onToggle: (date: String, currentlyDone: Boolean) -> Unit,
    onEdit:   () -> Unit,
    onDelete: () -> Unit
) {
    val habit       = hwl.habit
    val iconOpt     = iconOptionByName(habit.icon)
    val isDoneToday = hwl.logs[todayStr] == true

    // Build last 21 days for calendar strip
    val last21: List<Pair<String, Boolean>> = remember(hwl.logs) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        (0 until 21).map {
            val key  = fmt.format(cal.time)
            val done = hwl.logs[key] == true
            cal.add(Calendar.DAY_OF_YEAR, -1)
            key to done
        }.reversed()
    }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HSurface1)
            .border(1.dp, HBorder, RoundedCornerShape(18.dp))
    ) {
        // ── Header row ────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon bubble
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(iconOpt.tint.copy(0.13f))
                    .border(1.dp, iconOpt.tint.copy(0.3f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconOpt.vector, null, tint = iconOpt.tint, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(habit.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = HTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Streak
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = HAmber, modifier = Modifier.size(13.dp))
                        Text("${hwl.streak} day streak", fontSize = 11.sp, color = HTextSecondary)
                    }
                    // Total
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Filled.CheckCircle, null, tint = HGreen, modifier = Modifier.size(11.dp))
                        Text("${hwl.totalCompleted} total", fontSize = 11.sp, color = HTextSecondary)
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Today toggle button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDoneToday) HGreen.copy(0.15f) else HSurface2)
                    .border(1.5.dp, if (isDoneToday) HGreen.copy(0.5f) else HBorder, CircleShape)
                    .clickable { onToggle(todayStr, isDoneToday) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isDoneToday) Icons.Filled.Check else Icons.Outlined.Check,
                    null,
                    tint     = if (isDoneToday) HGreen else HTextTertiary,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(Modifier.width(6.dp))

            // Expand chevron
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                null,
                tint     = HTextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }

        // ── Expanded: history + actions ───────────────────────────────────────
        if (expanded) {
            HorizontalDivider(color = HBorder, modifier = Modifier.padding(horizontal = 14.dp))

            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {

                // 21-day calendar strip label
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Last 21 Days", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HTextSecondary)
                    Text("Tap to toggle", fontSize = 10.sp, color = HTextTertiary)
                }
                Spacer(Modifier.height(10.dp))

                // Calendar strip — 7 columns x 3 rows
                val rows = last21.chunked(7)
                val dayLabels = listOf("S","M","T","W","T","F","S")

                // Day-of-week headers
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    dayLabels.forEach { label ->
                        Text(label, fontSize = 9.sp, color = HTextTertiary, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(4.dp))

                rows.forEach { week ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Pad first row if needed (up to 7 cells)
                        val cells = if (week.size < 7) week + List(7 - week.size) { "" to false } else week
                        cells.forEach { (date, done) ->
                            CalendarDay(
                                date      = date,
                                done      = done,
                                isToday   = date == todayStr,
                                isEmpty   = date.isEmpty(),
                                iconTint  = iconOpt.tint,
                                onToggle  = { if (date.isNotEmpty()) onToggle(date, done) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Stats row
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HSurface2)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill(label = "Streak",   value = "${hwl.streak}d",            color = HAmber)
                    VerticalDivider(color = HBorder, modifier = Modifier.height(28.dp))
                    StatPill(label = "Total",    value = "${hwl.totalCompleted}",      color = HBlue)
                    VerticalDivider(color = HBorder, modifier = Modifier.height(28.dp))
                    val last7Done = last21.takeLast(7).count { it.second }
                    StatPill(label = "This Week", value = "$last7Done / 7",            color = HGreen)
                }

                Spacer(Modifier.height(12.dp))

                // Edit / Delete actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick  = onEdit,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        border   = BorderStroke(1.dp, HBorder),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = HTextSecondary)
                    ) {
                        Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick  = onDelete,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        border   = BorderStroke(1.dp, HRose.copy(0.35f)),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = HRose)
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─── Calendar Day Cell ─────────────────────────────────────────────────────────
@Composable
private fun CalendarDay(
    date:     String,
    done:     Boolean,
    isToday:  Boolean,
    isEmpty:  Boolean,
    iconTint: Color,
    onToggle: () -> Unit
) {
    val dayNum = if (date.isNotEmpty()) {
        runCatching { date.split("-")[2].trimStart('0').ifEmpty { "0" } }.getOrDefault("")
    } else ""

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .then(
                if (!isEmpty) Modifier
                    .background(
                        when {
                            done    -> iconTint.copy(0.18f)
                            isToday -> HSurface2
                            else    -> Color.Transparent
                        }
                    )
                    .border(
                        width = if (isToday) 1.5.dp else if (done) 0.dp else 0.dp,
                        color = if (isToday && !done) HBlue.copy(0.5f) else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onToggle() }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isEmpty) {
            if (done) {
                Icon(Icons.Filled.Check, null, tint = iconTint, modifier = Modifier.size(14.dp))
            } else {
                Text(
                    dayNum,
                    fontSize   = 11.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color      = if (isToday) HBlue else HTextTertiary,
                    textAlign  = TextAlign.Center
                )
            }
        }
    }
}

// ─── Stat Pill ─────────────────────────────────────────────────────────────────
@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = HTextTertiary)
    }
}

// ─── Empty State ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyHabitsState(onAdd: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(HBlue.copy(0.08f))
                .border(1.dp, HBlue.copy(0.15f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Repeat, null, tint = HBlue.copy(0.5f), modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("No habits yet", fontWeight = FontWeight.Bold, color = HTextSecondary, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Build a streak by tracking daily habits.\nTap + to add your first one.",
            textAlign  = TextAlign.Center,
            color      = HTextTertiary,
            fontSize   = 13.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick  = onAdd,
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = HBlue, contentColor = Color.White)
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add First Habit", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Add / Edit Habit Dialog ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditHabitDialog(
    existingHabit: Habit?,
    onDismiss:     () -> Unit,
    onConfirm:     (title: String, icon: String) -> Unit
) {
    var title       by remember { mutableStateOf(existingHabit?.title ?: "") }
    var selectedIcon by remember { mutableStateOf(existingHabit?.icon ?: "star") }
    val focus       = LocalFocusManager.current
    val isEdit      = existingHabit != null

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(HSurface1)
                .border(1.dp, HBorder, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {

                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val selOpt = iconOptionByName(selectedIcon)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(selOpt.tint.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(selOpt.vector, null, tint = selOpt.tint, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        if (isEdit) "Edit Habit" else "New Habit",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = HTextPrimary
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Title field
                Text("Name", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HTextSecondary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    placeholder   = { Text("e.g. Morning run", color = HTextTertiary, fontSize = 13.sp) },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = HBlue.copy(0.6f),
                        unfocusedBorderColor    = HBorder,
                        focusedTextColor        = HTextPrimary,
                        unfocusedTextColor      = HTextPrimary,
                        cursorColor             = HBlue,
                        focusedContainerColor   = HSurface2,
                        unfocusedContainerColor = HSurface2
                    ),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() })
                )

                Spacer(Modifier.height(18.dp))

                // Icon picker
                Text("Icon", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HTextSecondary)
                Spacer(Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns            = GridCells.Fixed(5),
                    modifier           = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                    userScrollEnabled  = false
                ) {
                    items(HABIT_ICONS) { opt ->
                        val isSelected = opt.name == selectedIcon
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) opt.tint.copy(0.2f) else HSurface2)
                                .border(
                                    1.5.dp,
                                    if (isSelected) opt.tint.copy(0.6f) else HBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedIcon = opt.name },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(opt.vector, null, tint = if (isSelected) opt.tint else HTextTertiary, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, HBorder),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = HTextSecondary)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick  = { if (title.isNotBlank()) onConfirm(title, selectedIcon) },
                        enabled  = title.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = HBlue,
                            contentColor           = Color.White,
                            disabledContainerColor = HSurface3,
                            disabledContentColor   = HTextTertiary
                        )
                    ) {
                        Text(if (isEdit) "Save" else "Add Habit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Delete Confirmation Dialog ────────────────────────────────────────────────
@Composable
private fun DeleteHabitDialog(habit: Habit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(HSurface1)
                .border(1.dp, HBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HRose.copy(0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Delete, null, tint = HRose, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("Delete Habit?", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = HTextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "\"${habit.title}\" and all its history will be archived. This cannot be undone.",
                    fontSize   = 13.sp,
                    color      = HTextSecondary,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, HBorder),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = HTextSecondary)
                    ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick  = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = HRose, contentColor = Color.White)
                    ) { Text("Delete", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ─── Import for StrokeCap ──────────────────────────────────────────────────────
private val StrokeCap = androidx.compose.ui.graphics.StrokeCap