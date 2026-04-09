package com.example.planora.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.success()
        val db = FirebaseFirestore.getInstance()

        return try {
            // Check Firestore for pending tasks (not completed)
            val querySnapshot = db.collection("users")
                .document(userId)
                .collection("tasks")
                .whereEqualTo("completed", false)
                .get()
                .await()

            // Only show notification if there are pending tasks
            if (!querySnapshot.isEmpty) {
                val notification = NotificationCompat.Builder(context, "reminder_channel")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Planora 🔔")
                    .setContentText("Bhai app open kar, apne tasks pending hai 😄")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                NotificationManagerCompat.from(context).notify(100, notification)
            }
            Result.success()
        } catch (e: SecurityException) {
            // Handle permission issue gracefully
            Result.success()
        } catch (e: Exception) {
            // Retry on connection errors
            Result.retry()
        }
    }
}
