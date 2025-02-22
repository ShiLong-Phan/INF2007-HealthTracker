package com.inf2007.healthtracker.utilities

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StepCounterService : Service() {
    private lateinit var stepSensorHelper: StepSensorHelper
    private val firestore = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreate() {
        super.onCreate()
        Log.d("StepService", "StepCounterService started")

        startForeground(1, createNotification())

        // Start the step sensor helper
        stepSensorHelper = StepSensorHelper(this) { stepCount ->
            user?.let { syncStepsToFirestore(it.uid, stepCount) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("StepService", "StepCounterService stopped")
        stepSensorHelper.stopTracking()
    }

    private fun createNotification(): Notification {
        val channelId = "step_tracker_channel"
        val channelName = "Step Tracker Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Step Tracker Running")
            .setContentText("Tracking your steps in the background...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun syncStepsToFirestore(userId: String, stepCount: Int) {
        val stepsRef = firestore.collection("steps").document("${user?.uid}_${Date()}")

        stepsRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                stepsRef.update("steps", stepCount)
            } else {
                val stepData = hashMapOf(
                    "steps" to stepCount,
                    "timestamp" to Date(),
                    "userId" to userId
                )
                stepsRef.set(stepData)
            }
        }.addOnFailureListener { exception ->
            Log.e("StepService", "Failed to sync steps: ${exception.message}")
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
