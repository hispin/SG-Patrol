package com.sensoguard.detectsensor.global

import android.app.Activity
import android.content.Context
import com.sensoguard.detectsensor.classes.Sensor

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