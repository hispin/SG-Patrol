package com.sensoguard.detectsensor.global

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.sensoguard.detectsensor.classes.Alarm
import com.sensoguard.detectsensor.classes.Sensor
import org.json.JSONArray
import org.json.JSONException


fun convertToGson(detectorsArr:ArrayList<Sensor>): String? {
    try {
        val gSon= Gson()
        val data=gSon.toJson(detectorsArr)
        val jsonArray= JsonParser().parse(data).asJsonArray
        return gSon.toJson(jsonArray)
        //TODO : how to get response about set shared preference

    } catch (ex: JSONException) {
        ex.message?.let { Log.i("exception", it) }
    } catch (ex: java.lang.Exception) {
        ex.message?.let { Log.i("exception", it) }
    }
    return ERROR_RESP
}

fun convertToAlarmsGson(alarmsArr:ArrayList<Alarm>): String? {
    try {
        val gSon= Gson()
        val data=gSon.toJson(alarmsArr)
        val jsonArray= JsonParser().parse(data).asJsonArray
        return gSon.toJson(jsonArray)
        //TODO : how to get response about set shared preference

    } catch (ex: JSONException) {
        ex.message?.let { Log.i("exception", it) }
    } catch (ex: java.lang.Exception) {
        ex.message?.let { Log.i("exception", it) }
    }
    return ERROR_RESP
}

//convert json to list of uri and list of Sensors
fun convertJsonToSensorList(inputJsonArrayString: String): ArrayList<Sensor>? {

    //if the json string is empty, then return empty array list
    if(inputJsonArrayString.isNullOrEmpty()){
        return ArrayList()
    }

    val mySensors:ArrayList<Sensor>?=ArrayList()
    //mySensors?.add(Sensor("ID","NAME"))

    var jsonArr: JSONArray?=null
    try {
        jsonArr= JSONArray(inputJsonArrayString)
    } catch (e: JSONException) {
        e.printStackTrace()
        e.message?.let { Log.e("convertJsonToUriList", it) }
    }

    try {
        val listType = object : TypeToken<List<Sensor>>() {

        }.type
        mySensors?.addAll(Gson().fromJson(jsonArr.toString(), listType) as ArrayList<Sensor>)
    }catch(e:JsonIOException){
        e.printStackTrace()
        e.message?.let { Log.e("convertJsonToUriList", it) }
    }catch(e:JsonSyntaxException){
        e.printStackTrace()
        e.message?.let { Log.e("convertJsonToUriList", it) }
    }catch (e: JSONException) {
        e.printStackTrace()
        e.message?.let { Log.e("convertJsonToUriList", it) }
    }

    return mySensors
    //when jsonArr is null will return value of new ArrayList<>()
}//convertJsonToUriList

//convert json to list of uri and list of Alarms
fun convertJsonToAlarmList(inputJsonArrayString: String): ArrayList<Alarm>? {

    var myAlarms:ArrayList<Alarm>?=null
    var jsonArr: JSONArray?=null
    try {
        jsonArr= JSONArray(inputJsonArrayString)
    } catch (e: JSONException) {
        e.printStackTrace()
        e.message?.let { Log.e("convertJsonToUriList", it) }
    }

    try {
        val listType = object : TypeToken<List<Alarm>>() {

        }.type
        myAlarms = Gson().fromJson(jsonArr.toString(), listType) as ArrayList<Alarm>
    }catch(e:JsonIOException){
        e.printStackTrace()
        e.message?.let { Log.e("convertJsonToAlarmList", it) }
    }catch(e:JsonSyntaxException){
        e.printStackTrace()
        e.message?.let { Log.e("convertJsonToAlarmList", it) }
    }

    return myAlarms
    //when jsonArr is null will return value of new ArrayList<>()
}