package com.sensoguard.detectsensor.interfaces

import com.sensoguard.detectsensor.classes.Sensor

interface OnAdapterListener {
    fun saveNameSensor(detector: Sensor)
    fun saveSensors(detector: Sensor)
}