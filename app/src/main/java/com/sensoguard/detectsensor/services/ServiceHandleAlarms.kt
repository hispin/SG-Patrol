package com.sensoguard.detectsensor.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.classes.Alarm
import com.sensoguard.detectsensor.classes.AlarmSensor
import com.sensoguard.detectsensor.classes.EmailService
import com.sensoguard.detectsensor.classes.Sensor
import com.sensoguard.detectsensor.global.ALARM_LIST_KEY_PREF
import com.sensoguard.detectsensor.global.CREATE_ALARM_ID_KEY
import com.sensoguard.detectsensor.global.CREATE_ALARM_IS_ARMED
import com.sensoguard.detectsensor.global.CREATE_ALARM_KEY
import com.sensoguard.detectsensor.global.CREATE_ALARM_NAME_KEY
import com.sensoguard.detectsensor.global.CREATE_ALARM_NOT_DEFINED_KEY
import com.sensoguard.detectsensor.global.CREATE_ALARM_TYPE_INDEX_KEY
import com.sensoguard.detectsensor.global.CREATE_ALARM_TYPE_KEY
import com.sensoguard.detectsensor.global.DETECTORS_LIST_KEY_PREF
import com.sensoguard.detectsensor.global.ERROR_RESP
import com.sensoguard.detectsensor.global.HANDLE_ALARM_KEY
import com.sensoguard.detectsensor.global.IS_FORWARD_ALARM_EMAIL
import com.sensoguard.detectsensor.global.IS_SSL_MAIL
import com.sensoguard.detectsensor.global.MEDIA_WORKER
import com.sensoguard.detectsensor.global.NONE_VALIDATE_BITS
import com.sensoguard.detectsensor.global.PASSWORD_MAIL
import com.sensoguard.detectsensor.global.PIR_TYPE
import com.sensoguard.detectsensor.global.PORT_MAIL
import com.sensoguard.detectsensor.global.RADAR_TYPE
import com.sensoguard.detectsensor.global.READ_DATA_KEY
import com.sensoguard.detectsensor.global.READ_DATA_KEY_TEST
import com.sensoguard.detectsensor.global.RECIPIENT_MAIL
import com.sensoguard.detectsensor.global.RESET_MARKERS_KEY
import com.sensoguard.detectsensor.global.SEISMIC_TYPE
import com.sensoguard.detectsensor.global.SENSOR_TYPE_INDEX_KEY
import com.sensoguard.detectsensor.global.SERVER_MAIL
import com.sensoguard.detectsensor.global.SIX_FOTMAT_BITS
import com.sensoguard.detectsensor.global.STOP_ALARM_SOUND
import com.sensoguard.detectsensor.global.TEN_FOTMAT_BITS
import com.sensoguard.detectsensor.global.USER_NAME_MAIL
import com.sensoguard.detectsensor.global.UserSession
import com.sensoguard.detectsensor.global.VIBRATION_TYPE
import com.sensoguard.detectsensor.global.convertJsonToAlarmList
import com.sensoguard.detectsensor.global.convertJsonToSensorList
import com.sensoguard.detectsensor.global.convertToAlarmsGson
import com.sensoguard.detectsensor.global.getBooleanInPreference
import com.sensoguard.detectsensor.global.getIntInPreference
import com.sensoguard.detectsensor.global.getStrDateTimeByMilliSeconds
import com.sensoguard.detectsensor.global.getStringInPreference
import com.sensoguard.detectsensor.global.setStringInPreference
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.mail.internet.InternetAddress


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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, AppCompatActivity.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "accept alarm")
            when (intent.action) {
                CREATE_ALARM_KEY -> {
                    val alarmSensorId = intent.getStringExtra(CREATE_ALARM_ID_KEY)
                    val type = intent.getStringExtra(CREATE_ALARM_TYPE_KEY)
                    val sensorTypeId = intent.getLongExtra(SENSOR_TYPE_INDEX_KEY, -1)
                    val date = getStrDateTimeByMilliSeconds(
                        Calendar.getInstance().timeInMillis,
                        "dd/MM/yy kk:mm:ss",
                        context
                    )

                    var msg: String? = null
                    if (sensorTypeId == SEISMIC_TYPE) {
                        msg = resources.getString(
                            R.string.email_content,
                            type,
                            alarmSensorId,
                            date
                        )// "$type alarm from unit $alarmSensorId "
                        Toast.makeText(
                            context,
                            "$type alarm from unit $alarmSensorId ",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    } else {
                        val values = resources.getStringArray(R.array.sensor_type)

                        val idx = sensorTypeId.toInt()
                        var sensorType = ""
                        if (idx >= 0 && idx < values.size) {
                            sensorType = values[idx]
                        }

                        msg = resources.getString(
                            R.string.email_content,
                            sensorType,
                            alarmSensorId,
                            date
                        )
                        Toast.makeText(
                            context,
                            "$sensorType alarm from unit $alarmSensorId ",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                    sendEmailBakground(msg)
                    //play sound&vibrate
                    startWorkerMedia()

                }
                CREATE_ALARM_NOT_DEFINED_KEY -> {
                    val alarmSensorId = intent.getStringExtra(CREATE_ALARM_ID_KEY)
                    val type = intent.getStringExtra(CREATE_ALARM_TYPE_KEY)
                    val sensorTypeId = intent.getLongExtra(SENSOR_TYPE_INDEX_KEY, -1)
                    if (sensorTypeId == SEISMIC_TYPE) {
                        Toast.makeText(
                            context,
                            "$type alarm from unit $alarmSensorId ",
                            Toast.LENGTH_LONG
                        )
                            .show()
                        //accept test alarm (for testing)
                    } else {
                        val values = resources.getStringArray(R.array.sensor_type)

                        val idx = sensorTypeId.toInt()
                        var sensorType = ""
                        if (idx >= 0 && idx < values.size) {
                            sensorType = values[idx]
                        }

                        Toast.makeText(
                            context,
                            "$sensorType alarm from unit $alarmSensorId ",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                    //play sound and vibrate
                    //playAlarmSound()
                    //playVibrate()
                }
                READ_DATA_KEY_TEST -> {
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

                    val date = getStrDateTimeByMilliSeconds(
                        Calendar.getInstance().timeInMillis,
                        "dd/MM/yy kk:mm:ss",
                        context
                    )
                    val msg = resources.getString(R.string.email_content, type, alarmSensorId, date)
                    //val msg = "$type alarm from unit $alarmSensorId "
                    Toast.makeText(
                        context,
                        "$type alarm from unit $alarmSensorId ",
                        Toast.LENGTH_LONG
                    )
                        .show()
                    sendEmailBakground(msg)

                    //add alarm to history and send alarm if active
                    if (currentSensorLocally == null) {
                        sendBroadcast(Intent(RESET_MARKERS_KEY))
                        addAlarmToHistory(
                            false,
                            "undefined",
                            isArmed = false,
                            alarmSensorId = alarmSensorId,
                            type = type
                        )
                    } else if (!currentSensorLocally.isArmed()) {
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
                        //Bug fixed:set car or intruder when the type of sensor is Seismic
                        if (currentSensorLocally.getTypeID() == SEISMIC_TYPE) {
                            type?.let { addAlarmToHistory(currentSensorLocally, it) }
                            //otherwise set the type of sensor as type of alarm
                        } else if (currentSensorLocally.getTypeID() == PIR_TYPE
                            || currentSensorLocally.getTypeID() == RADAR_TYPE
                            || currentSensorLocally.getTypeID() == VIBRATION_TYPE
                        ) {
                            currentSensorLocally.getType()
                                ?.let { addAlarmToHistory(currentSensorLocally, it) }
                        }


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
                        inn.putExtra(SENSOR_TYPE_INDEX_KEY, currentSensorLocally.getTypeID())
                        sendBroadcast(inn)

                        //play sound and vibrate
                        startWorkerMedia()
                    }
                    sendBroadcast(Intent(HANDLE_ALARM_KEY))
                }

                STOP_ALARM_SOUND -> {
                    stopPlayingAlarm()
                }
            }
        }
    }

    /**
     * stop playing alarm
     */
    private fun stopPlayingAlarm() {
        WorkManager.getInstance(applicationContext).cancelAllWorkByTag(MEDIA_WORKER)
    }

    /**
     * start media worker
     */
    private fun startWorkerMedia() {
        val mediaWorkRequest =
            OneTimeWorkRequest.Builder(MediaWorker::class.java).addTag(MEDIA_WORKER)
                .build() //OneTimeWorkRequestBuilder < MediaWorker > ().build();

        WorkManager.getInstance(this)
            .enqueue(mediaWorkRequest)
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
    private fun storeAlarmsToLocally(alarms: ArrayList<Alarm>) {
        // sort the list of events by date in descending
        val alarms = ArrayList(alarms.sortedWith(compareByDescending { it.timeInMillis }))
        if (alarms != null && alarms.size > 0) {
            val alarmsJsonStr = convertToAlarmsGson(alarms)
            setStringInPreference(this, ALARM_LIST_KEY_PREF, alarmsJsonStr)
        }
    }

    //send alarm email
    private fun sendEmailBakground(msg: String) {

        //check if forward alarm email is active
        val isForwardAlarmEmail = getBooleanInPreference(this, IS_FORWARD_ALARM_EMAIL, false)
        if (!isForwardAlarmEmail) {
            return
        }

        val userName = getStringInPreference(this, USER_NAME_MAIL, "-1")
        val password = getStringInPreference(this, PASSWORD_MAIL, "-1")
        val recipient = getStringInPreference(this, RECIPIENT_MAIL, "-1")
        val server = getStringInPreference(this, SERVER_MAIL, "-1")
        val port = getIntInPreference(this, PORT_MAIL, -1)
        val isSSL = getBooleanInPreference(this, IS_SSL_MAIL, false)

        //check if the account mail has been filled
        if (userName.equals("-1") || password.equals("-1")
            || recipient.equals("-1") || server.equals("-1")
            || port == -1
        ) {
            //Bugs fixed : cancel this message
            //showToast(this, resources.getString(R.string.no_fill_account))
            return
        }

        val auth = EmailService.UserPassAuthenticator(userName!!, password!!)//sg-patrol@sgsmtp.com
        val to = listOf(InternetAddress(recipient))
        val from = InternetAddress(userName)
        val email = EmailService.Email(
            auth,
            to,
            from,
            resources.getString(R.string.an_alert_was_received),
            msg
        )
        val emailService = EmailService(server!!, port!!)//("mail.sgsmtp.com", port!!)


        //TODO ssl=0
        //use CoroutineScope to prevent blocking main thread
        GlobalScope.launch { // or however you do background threads
            emailService.send(email, isSSL)
        }
    }

//    //send alarm email
//    private fun sendEmailBakground(msg:String) {
//        val auth = EmailService.UserPassAuthenticator("sg-patrol@sgsmtp.com", "SensoGuard1234")//sg-patrol@sgsmtp.com
//        val to = listOf(InternetAddress("tomer@sensoguard.com"),InternetAddress("hag.swead@gmail.com"))
//        val from = InternetAddress("sg-patrol@sgsmtp.com")
//        val email = EmailService.Email(auth, to, from, resources.getString(R.string.an_alert_was_received), msg)
//        val emailService = EmailService("mail.sgsmtp.com", 587)
//        //TODO ssl=0
//        //use CoroutineScope to prevent blocking main thread
//        GlobalScope.launch { // or however you do background threads
//            emailService.send(email)
//        }
//    }



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