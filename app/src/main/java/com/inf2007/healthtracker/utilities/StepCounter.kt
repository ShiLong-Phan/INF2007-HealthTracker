package com.inf2007.healthtracker.utilities

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseUser

@Composable
fun StepCounter(user: FirebaseUser) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("stepCounterPrefs", Context.MODE_PRIVATE)
    var stepCount by remember { mutableStateOf(0) }
    var initialStepCount by remember { mutableStateOf(sharedPreferences.getInt("initialStepCount", -1)) }
    val handler = remember { Handler(Looper.getMainLooper()) }

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
                        stepCount = event.values[0].toInt() - initialStepCount
                        Log.d("StepCounter", "Step count: $stepCount")
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(sensorEventListener, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)

            onDispose {
                sensorManager.unregisterListener(sensorEventListener)
                handler.removeCallbacksAndMessages(null)
            }
        }
    }

    Text(text = "Steps: $stepCount")
}

