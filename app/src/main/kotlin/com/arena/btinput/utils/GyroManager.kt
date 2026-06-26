package com.arena.btinput.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Gyroscope / Tilt manager for aiming.
 * Uses rotation vector for smooth tilt-to-mouse.
 */
class GyroManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    private val _tiltX = MutableStateFlow(0f)
    val tiltX: StateFlow<Float> = _tiltX

    private val _tiltY = MutableStateFlow(0f)
    val tiltY: StateFlow<Float> = _tiltY

    private var isEnabled = false

    fun setEnabled(enabled: Boolean) {
        if (enabled == isEnabled) return
        isEnabled = enabled

        if (enabled) {
            rotationSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        } else {
            sensorManager.unregisterListener(this)
            _tiltX.value = 0f
            _tiltY.value = 0f
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isEnabled || (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR && event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR)) return

        val values = FloatArray(3)
        SensorManager.getRotationMatrixFromVector(values, event.values)

        val pitch = Math.toDegrees(Math.atan2(values[6].toDouble(), values[10].toDouble())).toFloat()
        val roll = Math.toDegrees(Math.atan2(values[1].toDouble(), values[5].toDouble())).toFloat()

        val normX = (roll / 45f).coerceIn(-1f, 1f)
        val normY = (pitch / 45f).coerceIn(-1f, 1f)

        _tiltX.value = normX
        _tiltY.value = normY
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getMouseDeltas(sensitivity: Float = 8f): Pair<Int, Int> {
        val dx = (_tiltX.value * sensitivity).toInt()
        val dy = (_tiltY.value * sensitivity).toInt()
        return dx to dy
    }
}