package com.sensoguard.detectsensor.services

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sensoguard.detectsensor.classes.AlarmSensor
import com.sensoguard.detectsensor.classes.Sensor
import com.sensoguard.detectsensor.global.ALARM_FLICKERING_DURATION_DEFAULT_VALUE_SECONDS
import com.sensoguard.detectsensor.global.ALARM_FLICKERING_DURATION_KEY
import com.sensoguard.detectsensor.global.IS_NOTIFICATION_SOUND_KEY
import com.sensoguard.detectsensor.global.IS_VIBRATE_WHEN_ALARM_KEY
import com.sensoguard.detectsensor.global.SELECTED_NOTIFICATION_SOUND_KEY
import com.sensoguard.detectsensor.global.UserSession
import com.sensoguard.detectsensor.global.getBooleanInPreference
import com.sensoguard.detectsensor.global.getLongInPreference
import com.sensoguard.detectsensor.global.getSensorsFromLocally
import com.sensoguard.detectsensor.global.getStringInPreference
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MediaWorker(val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private var rington: Ringtone? = null

    private var scheduleTaskExecutor: ScheduledExecutorService? = null

    override fun doWork(): Result {
        playVibrate()
        if (playAlarmSound()) {
            shutDownTimer()
            startTimer()
        }

        return Result.success()
    }

    fun startTimer() {
        //if there is already at least one alarm ,it is not necessary to initial the timer
        if (scheduleTaskExecutor == null
            || (scheduleTaskExecutor?.isShutdown != null && scheduleTaskExecutor?.isShutdown!!)
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


                    stopPlayingAlarm()
                    //stop the sound alarm
                    shutDownTimer()
                }


                // update screen if showed
                //context.sendBroadcast(Intent(UPDATE_SCREEN_ALARM))
                //this.currentCalendar?.postValue(Calendar.getInstance())

            } catch (ex: Exception) {
                Log.d("testTimer", "exception:" + ex.message)
            }

        }, 0, 1, TimeUnit.SECONDS)
    }

    //check if there is alarm sensor that already timeout
    private fun checkTimeOutSensors() {
        //get sensors from locally
        val sensorsArr = getSensorsFromLocally(context.applicationContext)
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
            context.applicationContext,
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

    private fun playAlarmSound(): Boolean {

        val isNotificationSound = getBooleanInPreference(context, IS_NOTIFICATION_SOUND_KEY, true)
        if (!isNotificationSound) {
            //Toast.makeText(this, "fail start sound", Toast.LENGTH_LONG).show()
            return false
        }

        val selectedSound = getStringInPreference(context, SELECTED_NOTIFICATION_SOUND_KEY, "-1")

        if (!selectedSound.equals("-1")) {

            synchronized(this) {
                try {
                    val uri = Uri.parse(selectedSound)

                    if (rington != null) {//&& rington!!.isPlaying) {
                        //if the sound it is already played,
                        rington?.stop()
                        //Handler().postDelayed({
                        rington = RingtoneManager.getRingtone(context, uri)
                        rington?.play()
                        //Toast.makeText(this, "play sound", Toast.LENGTH_LONG).show()
                        //}, 1)
                    } else {
                        rington = RingtoneManager.getRingtone(context, uri)
                        rington?.play()
                        //Toast.makeText(this, "play sound from Handle alarm", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return true

    }

    //execute vibrate
    private fun playVibrate() {

        val isVibrateWhenAlarm =
            getBooleanInPreference(applicationContext, IS_VIBRATE_WHEN_ALARM_KEY, true)
        if (isVibrateWhenAlarm) {
            // Get instance of Vibrator from current Context
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            // Vibrate for 200 milliseconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        1000,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                vibrator.vibrate(1000)
            }

        }

    }

    private fun stopPlayingAlarm() {
        if (rington != null && rington?.isPlaying!!) {
            rington?.stop()
        }
    }

    override fun onStopped() {
        super.onStopped()
        stopPlayingAlarm()
        shutDownTimer()
    }

}