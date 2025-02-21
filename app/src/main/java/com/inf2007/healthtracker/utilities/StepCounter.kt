package com.inf2007.healthtracker.utilities

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StepCounter(user: FirebaseUser, onStepCountUpdated: (Int) -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("stepCounterPrefs", Context.MODE_PRIVATE)
    var stepCount by remember { mutableStateOf(0) }
    var lastSyncedSteps by remember { mutableStateOf(0) }
    var initialStepCount by remember { mutableStateOf(sharedPreferences.getInt("initialStepCount", -1)) }

    val firestore = FirebaseFirestore.getInstance()
    val stepsRef = firestore.collection("steps").document("${user.uid}_${getCurrentDate()}")

    // Fetch last recorded steps from Firestore on launch
    LaunchedEffect(user) {
        stepsRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                lastSyncedSteps = document.getLong("steps")?.toInt() ?: 0
                stepCount = lastSyncedSteps
                onStepCountUpdated(stepCount) // Update step count in MainScreen
            }
        }.addOnFailureListener { exception ->
            Log.e("StepCounter", "Failed to retrieve steps: ${exception.message}")
        }
    }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepCounterSensor == null) {
            Log.d("StepCounter", "Step Counter sensor not available!")
            onDispose { }
        } else {
            val sensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event != null) {
                        if (initialStepCount == -1) {
                            initialStepCount = event.values[0].toInt()
                            with(sharedPreferences.edit()) {
                                putInt("initialStepCount", initialStepCount)
                                apply()
                            }
                        }
                        val newStepCount = event.values[0].toInt() - initialStepCount
                        stepCount = lastSyncedSteps + newStepCount
                        onStepCountUpdated(stepCount) // Update step count in MainScreen
                        Log.d("StepCounter", "Step count: $stepCount")
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(sensorEventListener, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)

            onDispose {
                sensorManager.unregisterListener(sensorEventListener)
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Steps: $stepCount", style = MaterialTheme.typography.headlineMedium)
    }
}

// Sync Steps to Firestore, updating only today's record
fun syncStepsToFirestore(user: FirebaseUser, stepCount: Long, stepsRef: DocumentReference) {
    val timestamp = System.currentTimeMillis()

    stepsRef.get().addOnSuccessListener { document ->
        if (document.exists()) {
            stepsRef.update("steps", stepCount, "timestamp", timestamp)
        } else {
            val stepData = hashMapOf(
                "steps" to stepCount,
                "timestamp" to timestamp,
                "userId" to user.uid
            )
            stepsRef.set(stepData)
        }
    }.addOnFailureListener { exception ->
        Log.e("StepCounter", "Failed to sync steps: ${exception.message}")
    }
}

// Function to get the current date as YYYY-MM-DD
fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}
