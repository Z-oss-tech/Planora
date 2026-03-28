package com.example.planora.utils

import java.util.Calendar

fun shouldResetTask(recurrence: String, lastReset: Long): Boolean {

    if (recurrence == "NONE") return false

    val now = Calendar.getInstance()
    val last = Calendar.getInstance().apply { timeInMillis = lastReset }

    return when (recurrence) {
        "DAILY" ->
            now.get(Calendar.DAY_OF_YEAR) != last.get(Calendar.DAY_OF_YEAR)

        "WEEKLY" ->
            now.get(Calendar.WEEK_OF_YEAR) != last.get(Calendar.WEEK_OF_YEAR)

        else -> false
    }
}