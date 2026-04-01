package com.example.planora.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {

        val context = applicationContext

        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Planora 🔔")
            .setContentText("Bhai app open kar, apne tasks pending hai 😄")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(100, notification)
        } catch (e: SecurityException) {
            // Handle permission issue
        }

        return Result.success()
    }
}