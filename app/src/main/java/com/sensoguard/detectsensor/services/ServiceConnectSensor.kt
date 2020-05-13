package com.sensoguard.detectsensor.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.media.Ringtone
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.classes.Alarm
import com.sensoguard.detectsensor.classes.Sensor
import com.sensoguard.detectsensor.global.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ServiceConnectSensor : Service() {

    var manager:UsbManager?=null

    //private val ACTION_USB_PERMISSION1 = "com.android.USB_PERMISSION"
    var connection:UsbDeviceConnection?=null
    private var usbDevice: UsbDevice? = null
    private var serialPort: UsbSerialDevice? = null
    var numBytesCount=0
    var readData :ArrayList<Int>?=null
    private var rington: Ringtone? = null

    override fun onCreate() {
        super.onCreate()
        startSysForeGround()
        setFilter()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        findUsbDevices()

        return START_NOT_STICKY
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, arg1: Intent) {
            when {
                arg1.action == CHECK_AVAILABLE_KEY -> {
                    findUsbDevices()
                }
                arg1.action == STOP_READ_DATA_KEY -> {
                    //setBooleanInPreference(this@ServiceConnectSensor,USB_DEVICE_CONNECT,false)
                    serialPort?.close()
                    //this@ServiceConnectSensor.stopSelf()
                }
                arg1.action == HANDLE_READ_DATA_EXCEPTION -> {
                    val msg=arg1.getStringExtra("data")
                    Toast.makeText(applicationContext, "handle read bit int: $msg",Toast.LENGTH_LONG).show()
                }
                arg1.action == ACTION_USB_PERMISSION ->{
                    openConnection()
                }
//                arg1.action == CREATE_ALARM_KEY -> {
//                //Toast.makeText(this@MyScreensActivity, "MyScreensActivity start sound", Toast.LENGTH_LONG).show()
//                    playAlarmSound()
//
//                    playVibrate()
//                 }

            }

        }


    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            if (serialPort != null) {
                serialPort!!.close()
            }

            unregisterReceiver(usbReceiver)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun setFilter() {
        val filter = IntentFilter(CHECK_AVAILABLE_KEY)
        filter.addAction(HANDLE_READ_DATA_EXCEPTION)
        filter.addAction(STOP_READ_DATA_KEY)
        filter.addAction(ACTION_USB_PERMISSION)
        //filter.addAction(CREATE_ALARM_KEY)
        registerReceiver(usbReceiver, filter)
    }

    //try to find devices connection to usb
    fun findUsbDevices(){

        // Find all available drivers from attached devices.
        manager = getSystemService(Context.USB_SERVICE) as UsbManager


        val usbDevices = manager?.deviceList

        if (usbDevices!=null && usbDevices.isEmpty()) {
            Toast.makeText(this,"not finding devices",Toast.LENGTH_LONG).show()
            sendBroadcast(Intent(USB_CONNECTION_OFF_UI))
            return
        }

        usbDevice = usbDevices?.values?.first()

        if(usbDevice!=null){
            tryOpenConnection()
        }else{
            Toast.makeText(this,"not finding usbDevice",Toast.LENGTH_LONG).show()
            sendBroadcast(Intent(USB_CONNECTION_OFF_UI))
        }
    }

    //open connection ,if get failed send error message
    private fun openConnection(){
        connection = manager?.openDevice(usbDevice)
        if(connection==null){
            sendBroadcast(Intent(USB_CONNECTION_OFF_UI))
            //Toast.makeText(this@ServiceConnectSensor,"connection failed",Toast.LENGTH_LONG).show()
        }else{
            //setBooleanInPreference(this,USB_DEVICE_CONNECT,true)
            sendBroadcast(Intent(USB_CONNECTION_ON_UI))
            Toast.makeText(this@ServiceConnectSensor, "connection success", Toast.LENGTH_SHORT)
                .show()
            readData()
        }
    }

    //try open connection ,if get failed then request permission
    private fun tryOpenConnection(){
        usbDevice?.let {
            connection = manager?.openDevice(usbDevice)
            if(connection==null){
                sendBroadcast(Intent(USB_CONNECTION_OFF_UI))
                registerUsbPermission(it)
            }else{
                sendBroadcast(Intent(USB_CONNECTION_ON_UI))
                //Toast.makeText(this,"connection success",Toast.LENGTH_LONG).show()
                readData()
            }
        }
    }

   //request usb permission
    private fun registerUsbPermission(usbDevice: UsbDevice) {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val mPermissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        usbManager.requestPermission(usbDevice, mPermissionIntent)
    }


    private fun readData() {
        if (usbDevice == null) {
            return
        }
        Thread {
            // Read some data! Most have just one port (port 0).
            serialPort = UsbSerialDevice.createUsbSerialDevice(usbDevice, connection)




            if (serialPort != null) {
                if (serialPort!!.open()) {
                    serialPort!!.setBaudRate(115200)
                    serialPort!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
                    serialPort!!.setStopBits(UsbSerialInterface.STOP_BITS_1)
                    serialPort!!.setParity(UsbSerialInterface.PARITY_NONE)
                    /**
                     * Current flow control Options:
                     * UsbSerialInterface.FLOW_CONTROL_OFF
                     * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                     * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                     */
                    serialPort!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                    serialPort!!.read(mCallback)
                    serialPort!!.getCTS(ctsCallback)
                    serialPort!!.getDSR(dsrCallback)
                }

            }

        }.start()
    }
    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
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

    var arr=ArrayList<Int>(16)

    private val mCallback = UsbSerialInterface.UsbReadCallback { bytesArray ->

        //sendBroadcast(Intent("handle.read.data"))

        //prevent using arr by two processes in the same time
        synchronized(this) {

            if (bytesArray != null && bytesArray.isNotEmpty()) {
                for (element in bytesArray) {
                    arr.add(element.toInt())
                }
            }

            //general validate of the bits and get the format
            val appCode = validateBitsAndGetFormat(arr)

            if (arr != null && arr.size > 0)

                if ((appCode == TEN_FOTMAT_BITS && arr.size >= 10)
                    || (appCode == SIX_FOTMAT_BITS && arr.size >= 6)
                ) {
                    val inn = Intent(READ_DATA_KEY)
                    //inn.putExtra("size", bytesArray.size)
                    inn.putExtra("data", arr)
                    sendBroadcast(inn)
                    arr = ArrayList(16)
                }

        }

    }


    /*
     * State changes in the CTS line will be received here
     */
    private val ctsCallback =
        UsbSerialInterface.UsbCTSCallback {

        }

    /*
     * State changes in the DSR line will be received here
     */
    private val dsrCallback =
        UsbSerialInterface.UsbDSRCallback {

        }

    //general validate of the bits and get the format
    private fun validateBitsAndGetFormat(bit: java.util.ArrayList<Int>): Int {

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


    //get locally sensor that match to sensor of alarm
    fun getLocallySensorAlarm(alarmSensorId: String): Sensor? {
        val sensors: java.util.ArrayList<Sensor>?
        val sensorsListStr = getStringInPreference(this, DETECTORS_LIST_KEY_PREF, ERROR_RESP)

        sensors = if (sensorsListStr.equals(ERROR_RESP)) {
            java.util.ArrayList()
        } else {
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
        val dateFormat = SimpleDateFormat("kk:mm dd/MM/yy", locale)
        val dateString = dateFormat.format(tmp.time)


        val alarm =
            Alarm(alarmSensorId, alarmSensorName, type, dateString, isArmed, tmp.timeInMillis)
        alarm.isLocallyDefined = isLocallyDefined

        val alarms = populateAlarmsFromLocally()
        alarms?.add(alarm)
        alarms?.let { storeAlarmsToLocally(it) }
    }

    //get the alarms from locally
    private fun populateAlarmsFromLocally(): java.util.ArrayList<Alarm>? {
        val alarms: java.util.ArrayList<Alarm>?
        val alarmListStr = getStringInPreference(this, ALARM_LIST_KEY_PREF, ERROR_RESP)

        alarms = if (alarmListStr.equals(ERROR_RESP)) {
            java.util.ArrayList()
        } else {
            alarmListStr?.let { convertJsonToAlarmList(it) }
        }
        return alarms
    }

    //store the detectors to locally
    private fun storeAlarmsToLocally(alarms: java.util.ArrayList<Alarm>) {
        // sort the list of events by date in descending
        val alarms = java.util.ArrayList(alarms.sortedWith(compareByDescending { it.timeInMillis }))
        if (alarms != null && alarms.size > 0) {
            val alarmsJsonStr = convertToAlarmsGson(alarms)
            setStringInPreference(this, ALARM_LIST_KEY_PREF, alarmsJsonStr)
        }
    }

    //add active alarm to history
    private fun addAlarmToHistory(currentSensorLocally: Sensor, type: String) {
        val tmp = Calendar.getInstance()
        val resources = this.resources
        val locale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) resources.configuration.locales.getFirstMatch(
                resources.assets.locales
            )
            else resources.configuration.locale
        val dateFormat = SimpleDateFormat("kk:mm dd/MM/yy", locale)
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
}
