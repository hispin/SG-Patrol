package com.sensoguard.detectsensor.classes

import com.google.android.gms.maps.model.Marker
import java.util.*

class AlarmSensor(
    var alarmSensorId: String,
    var alarmTime: Calendar,
    var type: String,
    var isSensorArmed: Boolean
) {
    var marker: Marker?=null
}