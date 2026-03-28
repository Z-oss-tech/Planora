package com.example.planora.ui.tasks

import androidx.compose.ui.graphics.Color

data class SubTask(
    val title: String = "",
    val completed: Boolean = false
)

data class Task(
    val id: String = "",
    val title: String = "",
    val completed: Boolean = false,
    val date: Long = System.currentTimeMillis(),
    val priority: String = "Normal",
    val category: String = "Personal", // ✅ Category (safe default for old data)
    val recurrence: String = "None",   // None / Daily / Weekly
    val lastReset: Long = System.currentTimeMillis(),
    val subtasks: List<SubTask> = emptyList()
) {

    // 🔥 Priority color helper
    fun getPriorityColor(): Color {
        return when (priority.lowercase()) {
            "high" -> Color(0xFFFF4B91)   // Urgent Pink/Red
            "medium" -> Color(0xFFADC6FF) // Primary Blue
            "low" -> Color(0xFF34D48C)    // Optional improvement (safe)
            else -> Color(0xFF71717A)     // Default Zinc Gray
        }
    }
}