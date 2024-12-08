package com.ece475group7.sleepsensor

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel

class SensorDataViewModel : ViewModel() {
    val sensorValues = mutableStateMapOf<String, String>()

    fun updateSensorValue(sensorName: String, value: String) {
        sensorValues[sensorName] = value
    }
}
