package com.sensoguard.detectsensor.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.classes.Alarm
import com.sensoguard.detectsensor.classes.AlarmSensor
import com.sensoguard.detectsensor.classes.Sensor
import com.sensoguard.detectsensor.global.*
import java.text.SimpleDateFormat
import java.util.*


class ServiceHandleAlarms : ParentService() {
    private val TAG = "ServiceHandleAlarms"


//        override fun onBind(intent: Intent?): IBinder? {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }


    override fun onCreate() {
        super.onCreate()
        startSysForeGround()
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopPlayingAlarm()
        //setBooleanInPreference(applicationContext, USB_DEVICE_CONNECT_STATUS, false)
        this@ServiceHandleAlarms.stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        //FusedLocationProviderClient is for interacting with the location using fused location
        setFilter()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }

    private fun setFilter() {
        val filter = IntentFilter(READ_DATA_KEY)
        filter.addAction(READ_DATA_KEY_TEST)
        filter.addAction(STOP_ALARM_SOUND)
        filter.addAction(CREATE_ALARM_KEY)
        filter.addAction(CREATE_ALARM_NOT_DEFINED_KEY)
        registerReceiver(usbReceiver, filter)
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG,"accept alarm")
            if (intent.action == CREATE_ALARM_KEY) {
                val alarmSensorId = intent.getStringExtra(CREATE_ALARM_ID_KEY)
                val type = intent.getStringExtra(CREATE_ALARM_TYPE_KEY)
                Toast.makeText(context, "$type alarm from unit $alarmSensorId ", Toast.LENGTH_LONG)
                    .show()
                //play sound and vibrate
                playAlarmSound()
                playVibrate()

            } else if (intent.action == CREATE_ALARM_NOT_DEFINED_KEY) {
                val alarmSensorId = intent.getStringExtra(CREATE_ALARM_ID_KEY)
                val type = intent.getStringExtra(CREATE_ALARM_TYPE_KEY)
                Toast.makeText(context, "$type alarm from unit $alarmSensorId ", Toast.LENGTH_LONG)
                    .show()
                //accept test alarm (for testing)
            } else if (intent.action == READ_DATA_KEY_TEST) {
                val bit = intent.getIntegerArrayListExtra("data")

                Log.d("testMulti", "ServiceHandleAlarms size:" + bit?.size)

                val stateTypes = resources?.getStringArray(R.array.state_types)

//                val idx = bit[5].toUByte().toInt()-1
//
//                if (stateTypes != null && idx >= stateTypes.size) {
//                    return
//                }

                //general validate of the bits and get the format
                val appCode = bit?.let { validateBitsAndGetFormat(it) }
                Log.d("testBits", "" + appCode)
                if (appCode == NONE_VALIDATE_BITS) {
                    Log.d("testMulti", "the bits are failed")
                    Toast.makeText(context, "the bits are failed", Toast.LENGTH_LONG)
                        .show()
                    return
                }

                var typeIdx = -1
                if (appCode == SIX_FOTMAT_BITS) {
                    typeIdx = 4
                } else if (appCode == TEN_FOTMAT_BITS) {
                    typeIdx = 5
                }

                //no validate format
                if (typeIdx == -1) {
                    return
                }

                val typeIndex = bit?.get(typeIdx)?.toUByte()?.toInt()?.minus(1)
                if (typeIndex != null) {
                    if (stateTypes != null && typeIndex >= stateTypes.size) {
                        return
                    }
                }

                val type = typeIndex?.let { stateTypes?.get(it) }
                //Log.d("testIconAlarm", type)
                val alarmSensorId = bit?.get(1)?.toUByte().toString()

                //get locally sensor that match to sensor of alarm
                val currentSensorLocally = getLocallySensorAlarm(alarmSensorId)

                Toast.makeText(context, "$type alarm from unit $alarmSensorId ", Toast.LENGTH_LONG)
                    .show()

                //add alarm to history and send alarm if active
                if(currentSensorLocally==null){
                    sendBroadcast(Intent(RESET_MARKERS_KEY))
                    addAlarmToHistory(false,"undefined", isArmed = false, alarmSensorId = alarmSensorId, type = type)
                }else if (!currentSensorLocally.isArmed()) {
                    sendBroadcast(Intent(RESET_MARKERS_KEY))
                    currentSensorLocally.getName()?.let {
                        addAlarmToHistory(
                            true,
                            it, isArmed = false, alarmSensorId = alarmSensorId, type = type
                        )
                    }
                    // the sensor id exist but is not located
                } else if (currentSensorLocally.getLatitude() == null
                    || currentSensorLocally.getLongtitude() == null
                ) {
                    sendBroadcast(Intent(RESET_MARKERS_KEY))
                    currentSensorLocally.getName()?.let {
                        addAlarmToHistory(
                            true,
                            it, isArmed = false, alarmSensorId = alarmSensorId, type = type
                        )
                    }
                } else {
                    type?.let { addAlarmToHistory(currentSensorLocally, it) }


                    //////////////add alarm to queue
                    //prevent duplicate alarm at the same sensor at the same time
                    removeSensorAlarmById(currentSensorLocally.getId())

                    if (type != null) {
                        val sensorAlarm = AlarmSensor(
                            currentSensorLocally.getId(),
                            Calendar.getInstance(),
                            type,
                            currentSensorLocally.isArmed()
                        )
                        sensorAlarm.typeIdx = typeIndex
                        UserSession.instance.alarmSensors?.add(sensorAlarm)
                    }
                    /// end add to queue

                    //send to create alarm :map,sound ect...
                    val inn = Intent(CREATE_ALARM_KEY)
                    inn.putExtra(CREATE_ALARM_ID_KEY, currentSensorLocally.getId())
                    inn.putExtra(CREATE_ALARM_NAME_KEY, currentSensorLocally.getName())
                    inn.putExtra(CREATE_ALARM_IS_ARMED, currentSensorLocally.isArmed())
                    //inn.putExtra(CREATE_ALARM_LATITUDE_KEY,currentSensorLocally.getLatitude())
                    //inn.putExtra(CREATE_ALARM_LONGTITUDE_KEY,currentSensorLocally.getLongtitude())
                    inn.putExtra(CREATE_ALARM_TYPE_KEY, type)
                    inn.putExtra(CREATE_ALARM_TYPE_INDEX_KEY, typeIndex)
                    sendBroadcast(inn)

                    //play sound and vibrate
                    playAlarmSound()
                    playVibrate()
                }
                sendBroadcast(Intent(HANDLE_ALARM_KEY))
            } else if (intent.action == STOP_ALARM_SOUND) {
                stopPlayingAlarm()
            }
        }
    }

    //general validate of the bits and get the format
    private fun validateBitsAndGetFormat(bit: ArrayList<Int>): Int {

        if (bit == null || bit.size < 4) {
            return NONE_VALIDATE_BITS
        }

        val stx = bit[0].toUByte().toString()
        val length = bit[3].toUByte().toString()
        try {
            val etx = bit[length.toInt() - 1].toUByte().toString()
            if (stx != "2" || etx != "3") {
                return NONE_VALIDATE_BITS
            }
            val apCode = bit[2].toUByte().toString()
            return apCode.toInt()
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            return NONE_VALIDATE_BITS
        }

    }

    //add active alarm to history
    private fun addAlarmToHistory(currentSensorLocally: Sensor,type:String) {
        val tmp = Calendar.getInstance()
        val resources = this.resources
        val locale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) resources.configuration.locales.getFirstMatch(
                resources.assets.locales
            )
            else resources.configuration.locale
        val dateFormat = SimpleDateFormat("kk:mm:ss dd/MM/yy", locale)
        val dateString = dateFormat.format(tmp.time)

        val alarm = Alarm(
            currentSensorLocally.getId(),
            currentSensorLocally.getName(),
            type,
            dateString,
            currentSensorLocally.isArmed(),
            tmp.timeInMillis
        )
        alarm.latitude = currentSensorLocally.getLatitude()
        alarm.longitude = currentSensorLocally.getLongtitude()

        alarm.isLocallyDefined = true

        val alarms = populateAlarmsFromLocally()
        alarms?.add(alarm)
        alarms?.let { storeAlarmsToLocally(it) }
    }


    //add not active alarm to history
    private fun addAlarmToHistory(
        isLocallyDefined: Boolean,
        alarmSensorName: String,
        isArmed: Boolean,
        alarmSensorId: String,
        type: String?
    ) {
        val tmp = Calendar.getInstance()
        val resources = this.resources
        val locale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) resources.configuration.locales.getFirstMatch(
                resources.assets.locales
            )
            else resources.configuration.locale
        val dateFormat = SimpleDateFormat("kk:mm:ss dd/MM/yy", locale)
        val dateString = dateFormat.format(tmp.time)


        val alarm =
            Alarm(alarmSensorId, alarmSensorName, type, dateString, isArmed, tmp.timeInMillis)
        alarm.isLocallyDefined = isLocallyDefined

        val alarms = populateAlarmsFromLocally()
        alarms?.add(alarm)
        alarms?.let { storeAlarmsToLocally(it) }
    }

    //get locally sensor that match to sensor of alarm
    fun getLocallySensorAlarm(alarmSensorId: String): Sensor? {
        val sensors: ArrayList<Sensor>?
        val sensorsListStr= getStringInPreference(this, DETECTORS_LIST_KEY_PREF, ERROR_RESP)

        sensors = if(sensorsListStr.equals(ERROR_RESP)){
            ArrayList()
        }else {
            sensorsListStr?.let { convertJsonToSensorList(it) }
        }

        val iteratorList = sensors?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val detectorItem = iteratorList.next()
            if (alarmSensorId == detectorItem.getId()) {
                return detectorItem
            }
        }
        return null
    }

//    //get the detectors from locally
//    private fun populateDetectorsFromLocally(): ArrayList<Sensor>?  {
//        val detectors: ArrayList<Sensor>?
//        val detectorListStr= getStringInPreference(this, DETECTORS_LIST_KEY_PREF, ERROR_RESP)
//
//        detectors = if(detectorListStr.equals(ERROR_RESP)){
//            ArrayList()
//        }else {
//            detectorListStr?.let { convertJsonToSensorList(it) }
//        }
//        return detectors
//    }

    //get the alarms from locally
    private fun populateAlarmsFromLocally(): ArrayList<Alarm>? {
        val alarms: ArrayList<Alarm>?
        val alarmListStr = getStringInPreference(this, ALARM_LIST_KEY_PREF, ERROR_RESP)

        alarms = if (alarmListStr.equals(ERROR_RESP)) {
            ArrayList()
        } else {
            alarmListStr?.let { convertJsonToAlarmList(it) }
        }
        return alarms
    }

    //store the detectors to locally
    private fun storeAlarmsToLocally(alarms: ArrayList<Alarm>){
        // sort the list of events by date in descending
        val alarms=ArrayList(alarms.sortedWith(compareByDescending { it.timeInMillis }))
        if(alarms!=null && alarms.size>0){
            val alarmsJsonStr= convertToAlarmsGson(alarms)
            setStringInPreference(this, ALARM_LIST_KEY_PREF,alarmsJsonStr)
        }
    }

    private var rington: Ringtone? = null

    private fun stopPlayingAlarm() {
        if (rington != null && rington?.isPlaying!!) {
            rington?.stop()
        }
    }

    private fun playAlarmSound() {

        val isNotificationSound = getBooleanInPreference(this, IS_NOTIFICATION_SOUND_KEY, true)
        if (!isNotificationSound) {
            //Toast.makeText(this, "fail start sound", Toast.LENGTH_LONG).show()
            return
        }

        val selectedSound = getStringInPreference(this, SELECTED_NOTIFICATION_SOUND_KEY, "-1")

        if (!selectedSound.equals("-1")) {

            synchronized(this) {
                try {
                    val uri = Uri.parse(selectedSound)

                    if (rington != null) {//&& rington!!.isPlaying) {
                        //if the sound it is already played,
                        rington?.stop()
                        //Handler().postDelayed({
                        rington = RingtoneManager.getRingtone(this, uri)
                        rington?.play()
                        //Toast.makeText(this, "play sound", Toast.LENGTH_LONG).show()
                        //}, 1)
                    } else {
                        rington = RingtoneManager.getRingtone(this, uri)
                        rington?.play()
                        //Toast.makeText(this, "play sound from Handle alarm", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "exception play sound", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }

    }

    //execute vibrate
    private fun playVibrate() {

        val isVibrateWhenAlarm =
            getBooleanInPreference(applicationContext, IS_VIBRATE_WHEN_ALARM_KEY, true)
        if (isVibrateWhenAlarm) {
            // Get instance of Vibrator from current Context
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

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


        //The system allows apps to call Context.startForegroundService() even while the app is in the background. However, the app must call that service's startForeground() method within five seconds after the service is created
        private fun startSysForeGround() {
            fun getNotificationIcon(): Int {
                val useWhiteIcon =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                return if (useWhiteIcon) R.drawable.ic_app_notification else R.mipmap.ic_launcher
            }
            if (Build.VERSION.SDK_INT >= 26) {
                val CHANNEL_ID = "my_channel_01"
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT
                )

                val `object` = getSystemService(Context.NOTIFICATION_SERVICE)
                if (`object` != null && `object` is NotificationManager) {
                    `object`.createNotificationChannel(channel)
                }

                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentText("SG-Patrol is running")
                    .setSmallIcon(getNotificationIcon())
                    .build()

                startForeground(1, notification)
            }
        }

    //remove alarm sensor if exist (for testing)
    private fun removeSensorAlarmById(alarmId: String) {

        val iteratorList = UserSession.instance.alarmSensors?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            if (sensorItem.alarmSensorId == alarmId) {
                iteratorList.remove()
            }
        }
    }

}