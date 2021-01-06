package com.sensoguard.detectsensor.controler

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sensoguard.detectsensor.classes.AlarmSensor
import com.sensoguard.detectsensor.classes.Sensor
import com.sensoguard.detectsensor.global.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


class ViewModelListener(application: Application) : AndroidViewModel(application) {

    private var t: ScheduledFuture<*>?=null
    private var scheduleTaskExecutor: ScheduledExecutorService?=null


    //listener to changes when the timer interval is time out
    private var currentCalendar: MutableLiveData<Calendar>?=null

    fun startCurrentCalendarListener(): LiveData<Calendar>? {

        if (currentCalendar == null) {
            currentCalendar = MutableLiveData()
        }

//        if (UserSession.instance.alarmSensors == null || UserSession.instance.alarmSensors?.isEmpty()!!) {
//            shutDownTimer()
//            getApplication<Application>().applicationContext.sendBroadcast(Intent(STOP_ALARM_SOUND))
//            //stopPlayingAlarm()
//        }

        return currentCalendar
    }


    fun startTimer(){
        //if there is already at least one alarm ,it is not necessary to initial the timer
        if(scheduleTaskExecutor==null
            || (scheduleTaskExecutor?.isShutdown!=null && scheduleTaskExecutor?.isShutdown!!)
        ) {
            scheduleTaskExecutor = Executors.newScheduledThreadPool(1)
            executeTimer()
        }
    }

    // execute the time
    private fun executeTimer() {
        //Log.d("testTimer", "initial timer")

        // This schedule a task to run every 10 minutes:
        scheduleTaskExecutor?.scheduleAtFixedRate({
            Log.d("testTimer", "tick")
            try {

                //check if there is alarm sensor that already timeout
                checkTimeOutSensors()

                //if there is no alarm sensor the stop timer and sound of the alarm
                if (UserSession.instance.alarmSensors == null || UserSession.instance.alarmSensors?.isEmpty()!!) {
                    //stop the timer
                    shutDownTimer()
                    //stop the sound alarm
                    getApplication<Application>().applicationContext.sendBroadcast(
                        Intent(
                            STOP_ALARM_SOUND
                        )
                    )
                }
                // update screen if showed
                this.currentCalendar?.postValue(Calendar.getInstance())

            } catch (ex: Exception) {
                Log.d("testTimer", "exception:" + ex.message)
            }

        }, 0, 1, TimeUnit.SECONDS)
    }

    //check if there is alarm sensor that already timeout
    private fun checkTimeOutSensors() {
        //get sensors from locally
        val sensorsArr = getSensorsFromLocally(getApplication<Application>().applicationContext)
        val iteratorList = sensorsArr?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            val sensorAlarm = getSensorAlarmBySensor(sensorItem)
            if (sensorAlarm != null) {
                //if time out then remove the sensor from alarm list
                if (isSensorAlarmTimeout(sensorAlarm)) {
                    UserSession.instance.alarmSensors?.remove(sensorAlarm)
                }
            }
        }
    }

    //shut down the timer
    fun shutDownTimer() {
        Log.d("testTimer", "shutDownTimer")
        scheduleTaskExecutor?.shutdownNow()
    }

    //check if the sensor is in alarm process
    private fun getSensorAlarmBySensor(sensor: Sensor): AlarmSensor? {

        val iteratorList = UserSession.instance.alarmSensors?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            if (sensorItem.alarmSensorId == sensor.getId()) {
                return sensorItem
            }
        }
        return null
    }

    //check if the alarm sensor is in duration
    private fun isSensorAlarmTimeout(alarmProcess: AlarmSensor?): Boolean {

        val timeout = getLongInPreference(
            getApplication<Application>().applicationContext,
            ALARM_FLICKERING_DURATION_KEY,
            ALARM_FLICKERING_DURATION_DEFAULT_VALUE_SECONDS
        )
        val futureTimeout = timeout?.let { alarmProcess?.alarmTime?.timeInMillis?.plus(it * 1000) }

        if (futureTimeout != null) {
            val calendar = Calendar.getInstance()
            return when {
                futureTimeout < calendar.timeInMillis -> true
                else -> false
            }
        }
        return true
    }
}