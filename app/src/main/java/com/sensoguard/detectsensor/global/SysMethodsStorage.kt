package com.sensoguard.detectsensor.global

import android.app.Activity
import android.content.Context
import com.sensoguard.detectsensor.classes.Alarm
import com.sensoguard.detectsensor.classes.Sensor
import java.lang.ref.WeakReference

//store the sensors to locally
fun storeSensorsToLocally(sensors:ArrayList<Sensor>,context: Context){

    var detectorsJsonStr:String?=""
    if(sensors!=null && sensors.size>0){
        detectorsJsonStr= convertToGson(sensors)
    }
    setStringInPreference(context,DETECTORS_LIST_KEY_PREF,detectorsJsonStr)
}

//get the sensors from locally
fun getSensorsFromLocally(activity:Activity): ArrayList<Sensor>?  {
    val sensors: ArrayList<Sensor>?
    val detectorListStr = getStringInPreference(activity, DETECTORS_LIST_KEY_PREF, ERROR_RESP)

    sensors = if (detectorListStr.equals(ERROR_RESP)) {
        ArrayList()
    } else {
        detectorListStr?.let { convertJsonToSensorList(it) }
    }
    return sensors
}

//get the sensors from locally
fun getSensorsFromLocally(context: Context): ArrayList<Sensor>? {
    val sensors: ArrayList<Sensor>?
    val detectorListStr = getStringInPreference(context, DETECTORS_LIST_KEY_PREF, ERROR_RESP)

    sensors = if (detectorListStr.equals(ERROR_RESP)) {
        ArrayList()
    } else {
        detectorListStr?.let { convertJsonToSensorList(it) }
    }
    return sensors
}

//get the alarms from locally
fun populateAlarmsFromLocally(context: Context): ArrayList<Alarm>? {
    val alarms: ArrayList<Alarm>?
    val alarmListStr = getStringInPreference(context, ALARM_LIST_KEY_PREF, ERROR_RESP)

    alarms = if (alarmListStr.equals(ERROR_RESP)) {
        ArrayList()
    } else {
        alarmListStr?.let { convertJsonToAlarmList(it) }
    }
    return alarms
}

//store the detectors to locally
fun storeAlarmsToLocally(alarms: java.util.ArrayList<Alarm>, context: Context) {
    //use WeakReference if the activity is no longer alive
    val wContext: WeakReference<Context> =
        WeakReference(context)
    // sort the list of events by date in descending
    val alarms = java.util.ArrayList(alarms.sortedWith(compareByDescending { it.timeInMillis }))
    if (alarms != null) {
        val alarmsJsonStr = convertToAlarmsGson(alarms)
        setStringInPreference(wContext.get(), ALARM_LIST_KEY_PREF, alarmsJsonStr)
    }
}

