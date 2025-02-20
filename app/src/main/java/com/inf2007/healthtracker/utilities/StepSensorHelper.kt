package com.inf2007.healthtracker.utilities

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class StepSensorHelper(
    context: Context,
    private val onStepUpdate: (Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var initialStepCount = -1

    init {
        if (stepCounterSensor == null) {
            Log.e("StepSensorHelper", "Step Counter sensor not available!")
        } else {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount == -1) {
                initialStepCount = event.values[0].toInt()
            }
            val stepCount = event.values[0].toInt() - initialStepCount
            onStepUpdate(stepCount)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun stopTracking() {
        sensorManager.unregisterListener(this)
    }
}
