package com.sensoguard.detectsensor.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.media.Ringtone
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.classes.Alarm
import com.sensoguard.detectsensor.classes.AlarmSensor
import com.sensoguard.detectsensor.classes.Command
import com.sensoguard.detectsensor.classes.Sensor
import com.sensoguard.detectsensor.global.ACTION_INTERVAL
import com.sensoguard.detectsensor.global.ACTION_SEND_CMD
import com.sensoguard.detectsensor.global.ACTION_USB_PERMISSION
import com.sensoguard.detectsensor.global.ACTION_USB_RESPONSE_CACHE
import com.sensoguard.detectsensor.global.ALARM_LIST_KEY_PREF
import com.sensoguard.detectsensor.global.CHECK_AVAILABLE_KEY
import com.sensoguard.detectsensor.global.CHECK_USB_CONN_SW
import com.sensoguard.detectsensor.global.COMMAND_TYPE
import com.sensoguard.detectsensor.global.CREATE_ALARM_ID_KEY
import com.sensoguard.detectsensor.global.CREATE_ALARM_IS_ARMED
import com.sensoguard.detectsensor.global.CREATE_ALARM_KEY
import com.sensoguard.detectsensor.global.CREATE_ALARM_NAME_KEY
import com.sensoguard.detectsensor.global.CREATE_ALARM_NOT_DEFINED_KEY
import com.sensoguard.detectsensor.global.CREATE_ALARM_TYPE_INDEX_KEY
import com.sensoguard.detectsensor.global.CREATE_ALARM_TYPE_KEY
import com.sensoguard.detectsensor.global.DETECTORS_LIST_KEY_PREF
import com.sensoguard.detectsensor.global.DISCONNECT_USB_PROCESS_KEY
import com.sensoguard.detectsensor.global.ERROR_RESP
import com.sensoguard.detectsensor.global.GET_SENS_LEVEL
import com.sensoguard.detectsensor.global.HANDLE_ALARM_KEY
import com.sensoguard.detectsensor.global.HANDLE_READ_DATA_EXCEPTION
import com.sensoguard.detectsensor.global.NONE_VALIDATE_BITS
import com.sensoguard.detectsensor.global.PIR_TYPE
import com.sensoguard.detectsensor.global.RADAR_TYPE
import com.sensoguard.detectsensor.global.RESET_MARKERS_KEY
import com.sensoguard.detectsensor.global.SEISMIC_TYPE
import com.sensoguard.detectsensor.global.SENSOR_TYPE_INDEX_KEY
import com.sensoguard.detectsensor.global.SET_RF_ON_TIMER
import com.sensoguard.detectsensor.global.SET_SENS_LEVEL
import com.sensoguard.detectsensor.global.SET_TIME_SYSTEM
import com.sensoguard.detectsensor.global.SIX_FOTMAT_BITS
import com.sensoguard.detectsensor.global.STOP_GENERAL_TIMER
import com.sensoguard.detectsensor.global.STOP_READ_DATA_KEY
import com.sensoguard.detectsensor.global.STOP_TIMER
import com.sensoguard.detectsensor.global.TEN_FOTMAT_BITS
import com.sensoguard.detectsensor.global.USB_CACHE_RESPONSE_KEY
import com.sensoguard.detectsensor.global.USB_DEVICES_EMPTY
import com.sensoguard.detectsensor.global.USB_DEVICES_NOT_EMPTY
import com.sensoguard.detectsensor.global.USB_DEVICE_CONNECT_STATUS
import com.sensoguard.detectsensor.global.UserSession
import com.sensoguard.detectsensor.global.VIBRATION_TYPE
import com.sensoguard.detectsensor.global.convertJsonToAlarmList
import com.sensoguard.detectsensor.global.convertJsonToSensorList
import com.sensoguard.detectsensor.global.convertToAlarmsGson
import com.sensoguard.detectsensor.global.getStringInPreference
import com.sensoguard.detectsensor.global.setBooleanInPreference
import com.sensoguard.detectsensor.global.setStringInPreference
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess


class ServiceConnectSensor : ParentService() {

    //private var serialPortIn: UsbSerialDevice? = null
    var manager: UsbManager? = null

    //private val ACTION_USB_PERMISSION1 = "com.android.USB_PERMISSION"
    var connection: UsbDeviceConnection? = null
    private var usbDevice: UsbDevice? = null
    private var serialPort: UsbSerialDevice? = null
    var numBytesCount = 0
    var readData: ArrayList<Int>? = null
    private var rington: Ringtone? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        setBooleanInPreference(this@ServiceConnectSensor, USB_DEVICE_CONNECT_STATUS, false)
        serialPort?.close()
        serialPort = null
        sendBroadcast(Intent(STOP_GENERAL_TIMER))
        //serialPortIn?.syncClose()

        //bug fixed : the device does not accept alarm after reopen the application after kill all
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(10)
        //this@ServiceConnectSensor.stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        startSysForeGround()
        try {
            unregisterReceiver(usbReceiver)
        } catch (ex: java.lang.Exception) {
        }
        setFilter()
        //start the timer that check the connection
        startTimerGeneralService()
    }

    //start timer to supervise the usb software connection
    private fun startTimerGeneralService() {
        val intent = Intent(this, TimerGeneralService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        //close the connection
//        connection = null
//        serialPort?.close()
//        serialPort = null

        findUsbDevices()

        return START_NOT_STICKY
    }


    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, inn: Intent) {
            when {

                inn.action == UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    //sendBroadcast(Intent("yes_connection"))
                    findUsbDevices()
                }
                //when disconnect the device from USB
                inn.action == UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    //sendBroadcast(Intent("no_connection"))
                    stopConnectConfiguration()
                }

                inn.action == CHECK_AVAILABLE_KEY -> {
                    findUsbDevices()
                }
                inn.action == DISCONNECT_USB_PROCESS_KEY -> {

                    stopConnectConfiguration()
//                    setBooleanInPreference(
//                        this@ServiceConnectSensor,
//                        USB_DEVICE_CONNECT_STATUS,
//                        false
//                    )
//                    serialPort?.close()
//                    serialPort = null
//                    //bug fixed :stop timer of commands if needed
//                    sendBroadcast(Intent(STOP_TIMER))
//                    //serialPortIn?.syncClose()
//                    this@ServiceConnectSensor.stopSelf()
                }
                inn.action == STOP_READ_DATA_KEY -> {
                    //setBooleanInPreference(this@ServiceConnectSensor,USB_DEVICE_CONNECT,false)
                    //showShortToast(context,serialPort?.isOpen.toString())
                    serialPort?.close()
                    //serialPortIn?.syncClose()
                    //this@ServiceConnectSensor.stopSelf()
                }
                inn.action == HANDLE_READ_DATA_EXCEPTION -> {
                    val msg = inn.getStringExtra("data")
                    Toast.makeText(
                        applicationContext,
                        "handle read bit int: $msg",
                        Toast.LENGTH_LONG
                    ).show()
                }
                inn.action == ACTION_USB_PERMISSION -> {
                    openConnection()
                }
                inn.action == Intent.ACTION_SCREEN_OFF -> {
                    //Toast.makeText(this@ServiceConnectSensor, "MyScreensActivity start sound", Toast.LENGTH_LONG).show()
//                    setBooleanInPreference(
//                        this@ServiceConnectSensor,
//                        USB_DEVICE_CONNECT_STATUS,
//                        false
//                    )
//                    serialPort?.close()
//                    sendBroadcast(Intent(USB_CONNECTION_OFF_UI))
//                    this@ServiceConnectSensor.stopSelf()
                }

                //Bug fixed : accept trigger directly from timer and not from other receiver
                inn.action == ACTION_INTERVAL -> {

                    val commandType = inn.getStringExtra(COMMAND_TYPE)
                    //if the interval is belong to the command "set ref timer"
                    if (commandType != null && commandType == resources.getString(R.string.set_ref_timer)) {

                        //send another command
                        if (UserSession.instance.myCommand?.commandName.equals(resources.getString(R.string.set_ref_timer))) {
                            sendData()
                        }
                    }

                }
                //send manual command after time out of others commands
                inn.action == ACTION_SEND_CMD -> {
                    sendData()
                }
                //if stop timer refresh broadcast receiver registration to prevent multiple triggers
                inn.action == STOP_TIMER -> {
                    //showToast(this@ServiceConnectSensor,"unregister")
                    unregisterReceiver(this)
                    setFilter()
                }

                inn.action == CHECK_USB_CONN_SW -> {


//                    if(connection==null){
//                        sendBroadcast(Intent("not_connection"))
//                    }else{
//                        sendBroadcast(Intent("yes_connection"))
//                    }


                    //check SW connection
                    if (serialPort != null) {
                        if (serialPort?.isOpen == false) {
                            stopConnectConfiguration()
                            return
                        }
                    }

                    manager = getSystemService(Context.USB_SERVICE) as UsbManager


                    //check if find devices via USB
                    //Bug fixed: if usb has been disconnected during sleep
                    //mode the button still green
                    val usbDevices = manager?.deviceList


                    if ((usbDevices == null) || usbDevices.isEmpty()) {
                        stopConnectConfiguration()
                    } else {
                        sendBroadcast(Intent(USB_DEVICES_NOT_EMPTY))
                        //if(connection!=null && serialPort!=null && usbDevices != null) {
                        //onConnectConfiguration()
                        //}
                    }

                }

            }

        }


    }

    //change to on the UI of USB connection
    private fun onConnectConfiguration() {
        setBooleanInPreference(
            this@ServiceConnectSensor,
            USB_DEVICE_CONNECT_STATUS,
            true
        )
        sendBroadcast(Intent(USB_DEVICES_NOT_EMPTY))
    }

    private fun stopConnectConfiguration() {

        //close the connection
        connection = null
        serialPort?.close()
        serialPort = null

        setBooleanInPreference(
            this@ServiceConnectSensor,
            USB_DEVICE_CONNECT_STATUS,
            false
        )

        //update UI
        sendBroadcast(Intent(USB_DEVICES_EMPTY))

        //bug fixed :stop timer of commands if needed
        sendBroadcast(Intent(STOP_TIMER))
        //serialPortIn?.syncClose()
        //this@ServiceConnectSensor.stopSelf()
    }


    var prevCalendar = Calendar.getInstance()

    override fun onDestroy() {
        super.onDestroy()

        try {
            if (serialPort != null) {
                serialPort!!.close()
                sendBroadcast(Intent(STOP_GENERAL_TIMER))
                //serialPortIn?.syncClose()
            }

            unregisterReceiver(usbReceiver)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun setFilter() {
        val filter = IntentFilter(CHECK_AVAILABLE_KEY)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(HANDLE_READ_DATA_EXCEPTION)
        filter.addAction(STOP_READ_DATA_KEY)
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(DISCONNECT_USB_PROCESS_KEY)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(ACTION_SEND_CMD)
        filter.addAction(ACTION_INTERVAL)
        filter.addAction(CHECK_USB_CONN_SW)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, AppCompatActivity.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }

    }

    //try to find devices connection to usb
    fun findUsbDevices() {

        // Find all available drivers from attached devices.
        manager = getSystemService(Context.USB_SERVICE) as UsbManager


        val usbDevices = manager?.deviceList

        if (usbDevices!=null && usbDevices.isEmpty()) {
            Toast.makeText(this, "not finding devices", Toast.LENGTH_LONG).show()
            stopConnectConfiguration()
            //sendBroadcast(Intent(USB_CONNECTION_OFF_UI))
            return
        }

        usbDevice = usbDevices?.values?.first()

        if(usbDevice!=null){
            tryOpenConnection()
        }else {
            Toast.makeText(this, "not finding usbDevice", Toast.LENGTH_LONG).show()
            stopConnectConfiguration()
            //sendBroadcast(Intent(USB_CONNECTION_OFF_UI))
        }
    }

    //open connection ,if get failed send error message
    private fun openConnection(){
        connection = manager?.openDevice(usbDevice)
        if(connection==null) {
            stopConnectConfiguration()
            //sendBroadcast(Intent(USB_CONNECTION_OFF_UI))
            //Toast.makeText(this@ServiceConnectSensor,"connection failed",Toast.LENGTH_LONG).show()
        }else{
            //setBooleanInPreference(this,USB_DEVICE_CONNECT,true)
            //sendBroadcast(Intent(USB_CONNECTION_ON_UI))
            Toast.makeText(this@ServiceConnectSensor, "connection success", Toast.LENGTH_SHORT)
                .show()
            readData()
        }
    }

    //try open connection ,if get failed then request permission
    private fun tryOpenConnection(){
        usbDevice?.let {
            connection = manager?.openDevice(usbDevice)

            if(connection==null) {
                sendBroadcast(Intent(USB_DEVICES_EMPTY))
                setBooleanInPreference(
                    this@ServiceConnectSensor,
                    USB_DEVICE_CONNECT_STATUS,
                    false
                )
                registerUsbPermission(it)
            }else{
                //sendBroadcast(Intent(USB_CONNECTION_ON_UI))
                //Toast.makeText(this,"connection success",Toast.LENGTH_LONG).show()
                readData()
            }
        }
    }

   //request usb permission
    private fun registerUsbPermission(usbDevice: UsbDevice) {
       val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
       val mPermissionIntent = PendingIntent.getBroadcast(
           this,
           0,
           //Bug fixed:cannot get permission for USB
           Intent(ACTION_USB_PERMISSION),
           PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
       )
       usbManager.requestPermission(usbDevice, mPermissionIntent)
   }

    private fun sendData() {
        //Toast.makeText(this, "start send data", Toast.LENGTH_LONG).show()
        if (usbDevice == null) {
            return
        }

        //val cmds = arg1.getIntArrayExtra(CURRENT_COMMAND)
        val cmds = UserSession.instance.commandContent

        if (cmds == null || cmds.size < 4 || cmds[3] != cmds.size) {
            Toast.makeText(this, "command formatting error", Toast.LENGTH_LONG).show()
            return
        }

        //Toast.makeText(this, "start send data cmds[2]"+ cmds[2], Toast.LENGTH_LONG).show()


        Thread {


            if (serialPort != null) {

                //open sync fro writing
                if (serialPort!!.syncOpen()) {


                    //val cmds: IntArray = intArrayOf(2,1,240,7,45,0,3)
                    val myBytes = ByteArray(cmds.size)
                    var i = 0
                    val iteratorList = cmds.iterator()
                    while (iteratorList != null && iteratorList.hasNext()) {
                        val cmd = iteratorList.next()
                        if (i < myBytes.size) {
                            myBytes[i++] = cmd.toUByte().toByte()
                        }
                    }

                    val mOutputStream = serialPort?.outputStream



                    mOutputStream?.setTimeout(1000)
                    mOutputStream?.write(myBytes)
                    //hag : bug fixed : unwanted data when write to USB
                    mOutputStream?.flush()
                    mOutputStream?.close()
                }

            }
            //Toast.makeText(this, "start send data2", Toast.LENGTH_LONG).show()
//            val cmd = "31CE"//""sfd\n"
//
//            val mOutputStream = serialPortIn?.outputStream
//            val bytes: ByteArray = cmd.toByteArray()
//
//            mOutputStream?.setTimeout(1000)
//            mOutputStream?.write(bytes)


        }.start()
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
                    onConnectConfiguration()
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

    var arr = ArrayList<Int>()

    //define timer delay to clear the buffer
    private val mHandler = Handler()
    val runnable: Runnable = Runnable {
        arr = ArrayList()
    }

    //runnable
    private val mCallback = UsbSerialInterface.UsbReadCallback { bytesArray ->


        //prevent using arr by two processes in the same time
        synchronized(this) {

            //Log.d("testMulti","start")
            if (bytesArray != null && bytesArray.isNotEmpty()) {
                for (element in bytesArray) {
                    arr.add(element.toInt())
                }
            }

            //general validate of the bits and get the format
            val appCode = validateBitsAndGetFormat(arr)

//            val inn = Intent("test.brod")
//            inn.putExtra("size",arr.size)
//            inn.putExtra("appcode",appCode)
//            sendBroadcast(inn)
//            arr = ArrayList()
            //Log.d("testMulti","ServiceConnect size:"+arr.size+" appCode:"+appCode)

            if (arr != null && arr.size > 0)

                if (appCode == TEN_FOTMAT_BITS && arr.size % 10 == 0) {

                    while (arr.size >= 10) {
                        val arrTen = ArrayList<Int>()

                        //make queue for each ten bits
                        var i = 0
                        val iteratorList = arr.listIterator()
                        while (iteratorList != null && iteratorList.hasNext() && i < 10) {
                            i++
                            val bitsItem = iteratorList.next()
                            arrTen.add(bitsItem)
                            iteratorList.remove()
                        }
                        parsingBits(arrTen)
                    }
                    arr = ArrayList()

                } else if (appCode == SIX_FOTMAT_BITS && arr.size % 6 == 0) {
                    while (arr.size >= 6) {
                        val arrSix = ArrayList<Int>()

                        //make queue for each six bits
                        var i = 0
                        val iteratorList = arr.listIterator()
                        while (iteratorList != null && iteratorList.hasNext() && i < 6) {
                            i++
                            val bitsItem = iteratorList.next()
                            arrSix.add(bitsItem)
                            iteratorList.remove()
                        }
                        parsingBits(arrSix)
                    }
                    arr = ArrayList()
                } else if (appCode == SET_RF_ON_TIMER && arr.size % 7 == 0) {
                    while (arr.size >= 7) {
                        val arrSeven = ArrayList<Int>()

                        var i = 0
                        val iteratorList = arr.listIterator()
                        while (iteratorList != null && iteratorList.hasNext() && i < 7) {
                            i++
                            val bitsItem = iteratorList.next()
                            arrSeven.add(bitsItem)
                            iteratorList.remove()
                        }

                        //send the response
                        val inn = Intent(ACTION_USB_RESPONSE_CACHE)
                        //inn.putIntegerArrayListExtra("myImage",myBytesInt)
                        inn.putExtra(USB_CACHE_RESPONSE_KEY, arrSeven)
                        sendBroadcast(inn)
//                        //TODO :test if success or failed
//                        //parsingBits(arrSeven)
                    }
                    arr = ArrayList()
                } else if (appCode == GET_SENS_LEVEL && arr.size % 8 == 0) {
                    while (arr.size >= 8) {
                        val arrEight = ArrayList<Int>()

                        var i = 0
                        val iteratorList = arr.listIterator()
                        while (iteratorList != null && iteratorList.hasNext() && i < 8) {
                            i++
                            val bitsItem = iteratorList.next()
                            arrEight.add(bitsItem)
                            iteratorList.remove()
                        }

                        //send the response
                        val inn = Intent(ACTION_USB_RESPONSE_CACHE)
                        //inn.putIntegerArrayListExtra("myImage",myBytesInt)
                        inn.putExtra(USB_CACHE_RESPONSE_KEY, arrEight)
                        sendBroadcast(inn)
//                        //TODO :test if success or failed
//                        //parsingBits(arrSeven)
                    }
                    arr = ArrayList()
                } else if (appCode == SET_SENS_LEVEL && arr.size % 7 == 0) {
                    while (arr.size >= 7) {
                        val arrSeven = ArrayList<Int>()

                        var i = 0
                        val iteratorList = arr.listIterator()
                        while (iteratorList != null && iteratorList.hasNext() && i < 7) {
                            i++
                            val bitsItem = iteratorList.next()
                            arrSeven.add(bitsItem)
                            iteratorList.remove()
                        }

                        //send the response
                        val inn = Intent(ACTION_USB_RESPONSE_CACHE)
                        //inn.putIntegerArrayListExtra("myImage",myBytesInt)
                        inn.putExtra(USB_CACHE_RESPONSE_KEY, arrSeven)
                        sendBroadcast(inn)
//                        //TODO :test if success or failed
//                        //parsingBits(arrSeven)
                    }
                    arr = ArrayList()
                } else if (appCode == SET_TIME_SYSTEM && arr.size % 7 == 0) {
                    while (arr.size >= 7) {
                        val arrSeven = ArrayList<Int>()

                        var i = 0
                        val iteratorList = arr.listIterator()
                        while (iteratorList != null && iteratorList.hasNext() && i < 7) {
                            i++
                            val bitsItem = iteratorList.next()
                            arrSeven.add(bitsItem)
                            iteratorList.remove()
                        }

                        //send the response
                        val inn = Intent(ACTION_USB_RESPONSE_CACHE)
                        inn.putExtra(USB_CACHE_RESPONSE_KEY, arrSeven)
                        sendBroadcast(inn)
                    }
                    arr = ArrayList()
                }


            //define timer delay to clear the buffer
            mHandler.removeCallbacks { runnable }
            mHandler.postDelayed(runnable, 200)
        }

    }

    private fun parsingBits(bit: ArrayList<Int>) {
        val stateTypes = resources?.getStringArray(R.array.state_types)


        //general validate of the bits and get the format
        val appCode = validateBitsAndGetFormat(bit)

        if (appCode == NONE_VALIDATE_BITS) {
            Log.d("testMulti", "the bits are failed")
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

        val typeIndex = bit[typeIdx].toUByte().toInt() - 1
        if (stateTypes != null && typeIndex >= stateTypes.size) {
            return
        }


        val alarmSensorId = bit[1].toUByte().toString()

        //get locally sensor that match to sensor of alarm
        val currentSensorLocally = getLocallySensorAlarm(alarmSensorId)
        //currentSensorLocally.

        var type: String? = null//stateTypes?.get(typeIndex)

        //Bug fixed:set car or intruder when the type of sensor is Seismic
        if (currentSensorLocally?.getTypeID() == SEISMIC_TYPE) {
            type = stateTypes?.get(typeIndex)
            //otherwise set the type of sensor as type of alarm
        } else if (currentSensorLocally?.getTypeID() == PIR_TYPE
            || currentSensorLocally?.getTypeID() == RADAR_TYPE
            || currentSensorLocally?.getTypeID() == VIBRATION_TYPE
        ) {
            type = currentSensorLocally.getType()
        }


        // for  toast ,that cannot showed here because it is not UI thread
        val innAlarmNotDefined = Intent(CREATE_ALARM_NOT_DEFINED_KEY)
        innAlarmNotDefined.putExtra(CREATE_ALARM_ID_KEY, alarmSensorId)
        innAlarmNotDefined.putExtra(CREATE_ALARM_TYPE_KEY, type)
        innAlarmNotDefined.putExtra(CREATE_ALARM_TYPE_INDEX_KEY, typeIndex)
        //add alarm to history and send alarm if active
        if (currentSensorLocally == null) {
            sendBroadcast(Intent(RESET_MARKERS_KEY))
            // for  toast ,that cannot showed here because it is not UI thread
            sendBroadcast(innAlarmNotDefined)


            addAlarmToHistory(
                false,
                "undefined",
                isArmed = false,
                alarmSensorId = alarmSensorId,
                type = type
            )
        } else if (!currentSensorLocally.isArmed()) {
            sendBroadcast(Intent(RESET_MARKERS_KEY))
            // for  toast ,that cannot showed here because it is not UI thread
            sendBroadcast(innAlarmNotDefined)
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
            // for  toast ,that cannot showed here because it is not UI thread
            sendBroadcast(innAlarmNotDefined)
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
            inn.putExtra(CREATE_ALARM_TYPE_KEY, type)
            inn.putExtra(CREATE_ALARM_TYPE_INDEX_KEY, typeIndex)
            inn.putExtra(SENSOR_TYPE_INDEX_KEY, currentSensorLocally.getTypeID())
            sendBroadcast(inn)
        }
        sendBroadcast(Intent(HANDLE_ALARM_KEY))
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
    private fun validateBitsAndGetFormat(bit: ArrayList<Int>): Int {

        if (bit == null || bit.size < 4) {
            return NONE_VALIDATE_BITS
        }

        val stx = bit[0].toUByte().toString()
        Log.d("testMulti", "stx:$stx")
        val length = bit[3].toUByte().toString()
        Log.d("testMulti", "length:$length")
        try {
            val etx = bit[length.toInt() - 1].toUByte().toString()
            Log.d("testMulti", "etx:$etx")
            if (stx != "2" || etx != "3") {
                return NONE_VALIDATE_BITS
            }
            val apCode = bit[2].toUByte().toString()
            Log.d("testMulti", "apCode:$apCode")
            return apCode.toInt()
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            return NONE_VALIDATE_BITS
        }

    }


    //get locally sensor that match to sensor of alarm
    fun getLocallySensorAlarm(alarmSensorId: String): Sensor? {
        val sensors: ArrayList<Sensor>?
        val sensorsListStr = getStringInPreference(this, DETECTORS_LIST_KEY_PREF, ERROR_RESP)

        sensors = if (sensorsListStr.equals(ERROR_RESP)) {
            ArrayList()
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

    //add active alarm to history
    private fun addAlarmToHistory(currentSensorLocally: Sensor, type: String) {
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

    //remove alarm sensor if exist
    private fun removeSensorAlarmById(alarmId: String) {

        val iteratorList = UserSession.instance.alarmSensors?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            if (sensorItem.alarmSensorId == alarmId) {
                iteratorList.remove()
            }
        }
    }

    //sen command to sensor
    private fun sendCommand(command: Command) {

        UserSession.instance.commandContent = command.commandContent

    }

}
