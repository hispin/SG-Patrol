package com.sensoguard.detectsensor.services

//import android.support.v4.app.NotificationCompat
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
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.global.CHECK_AVAILABLE_KEY
import com.sensoguard.detectsensor.global.STOP_READ_DATA_KEY


open class ServiceConnectSensor1 : Service() {

    private var availableDrivers:List <MutableList<UsbSerialDriver>>?=null
    var manager:UsbManager?=null
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    var driver: UsbSerialDriver?=null
    private var connection: UsbDeviceConnection?=null
    var isReader:Boolean=false
    private var port:UsbSerialPort?=null
    private val quantity=16

    override fun onCreate() {
        super.onCreate()
        startSysForeGround()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        setFilter()

        //selectDevice()
        checkAvailableDrivers()


        return START_NOT_STICKY
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, arg1: Intent) {
            when {
                arg1.action == CHECK_AVAILABLE_KEY -> checkAvailableDrivers()
                arg1.action == "read.data.command" -> readData()
                arg1.action == STOP_READ_DATA_KEY -> isReader=false
                arg1.action == "handle.read.data.exception" -> {
                    val msg=arg1.getStringExtra("data")
                    Toast.makeText(applicationContext, "handle read bit int: $msg",Toast.LENGTH_LONG).show()

                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isReader=false
        connection?.close()
        unregisterReceiver(usbReceiver)
    }

    private fun setFilter() {
        val filter = IntentFilter(CHECK_AVAILABLE_KEY)
        filter.addAction("read.data.command")
        filter.addAction("handle.read.data")
        filter.addAction(STOP_READ_DATA_KEY)
        filter.addAction("handle.read.data.exception")
        filter.addAction(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
    }

    //check if the drivers (for usb connection) is available
    fun checkAvailableDrivers(){

        // Find all available drivers from attached devices.
        manager = getSystemService(Context.USB_SERVICE) as UsbManager
        Log.d("testCrash",manager.toString())

        //var test = UsbSerialProber.getDefaultProbeTable()

        var usbSerialProber:UsbSerialProber?=null
        try {
            usbSerialProber = UsbSerialProber.getDefaultProber()
            //val usbSerialTable = UsbSerialProber.getDefaultProbeTable()
        }catch (ex:Exception){
            Log.d("testCrash",ex.message)
            Toast.makeText(this,"no driver",Toast.LENGTH_LONG).show()
            return
        }

        //manager?.let{availableDrivers=findAllDrivers(it)}

        usbSerialProber?.let{availableDrivers = listOf(it.findAllDrivers(manager))}

        if (availableDrivers!=null && availableDrivers!!.isEmpty()) {
            Toast.makeText(this,"drivers not available",Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this,"drivers available",Toast.LENGTH_LONG).show()
        selectAvailableDrivers()
    }


   //select  the first driver
    private fun selectAvailableDrivers(){
        // Open a connection to the first available driver.

        if(availableDrivers!=null && availableDrivers?.size!! > 0 && availableDrivers?.get(0)!=null && availableDrivers?.get(0)?.size!! > 0) {
            driver = availableDrivers?.get(0)?.get(0)

            driver?.device?.let { openConnectToDevice(it) }
        }else{
            Toast.makeText(this,"empty",Toast.LENGTH_LONG).show()
        }

    }

    //select  the first driver
    private fun selectDevice(){
        // Open a connection to the first available driver.

        manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevices = manager?.deviceList
        if (usbDevices!=null && usbDevices.isEmpty()) {

            return
        }

        val device = usbDevices?.values?.first()

        device?.let { openConnectToDevice(it) }

    }

    // get permission to connect to usb device
    private fun openConnectToDevice(usbDevice: UsbDevice) {
                try {
                    if(!isReader) {
                        connection = manager?.openDevice(usbDevice)//driver?.device)
                    }
                }catch (e:java.lang.Exception){
                    val inn = Intent("handle.read.data.exception")
                    inn.putExtra("data", "exception try connect "+e.message)
                    sendBroadcast(Intent(inn))
                }
                if (connection == null) {
                    //Toast.makeText(this, "connection failed", Toast.LENGTH_LONG).show()
                    //if connection is failed ,try to fix it by request permission
                    requestPermission(usbDevice)
                } else {
                    Toast.makeText(this, "connection success", Toast.LENGTH_LONG).show()
                    isReader = true
                    try {
                        readData()
                    }catch(e:java.lang.Exception){
                        val inn = Intent("handle.read.data.exception")
                        inn.putExtra("data", "exception start read data "+e.message)
                        sendBroadcast(Intent(inn))
                    }
                }

    }

    // get permission to connect to usb device
    private fun requestPermission(usbDevice: UsbDevice){
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        //if(ContextCompat.checkSelfPermission(this, ACTION_USB_PERMISSION)!=PackageManager.PERMISSION_GRANTED){
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbPermissionReceiver, filter)
        val mPermissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        usbManager.requestPermission(usbDevice, mPermissionIntent)
    }

    private var usbPermissionReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == ACTION_USB_PERMISSION) {


                    // call your method that cleans up and closes communication with the device
                    if (!isReader) {
                        synchronized(this) {

                        //val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        //device?.apply {
                        try{
                            connection = manager?.openDevice(driver?.device)//driver?.device)
                        }catch (e:java.lang.Exception){
                            val inn = Intent("handle.read.data.exception")
                            inn.putExtra("data", "exception try connect "+e.message)
                            sendBroadcast(Intent(inn))
                        }


                            if (connection == null) {
                                Toast.makeText(applicationContext, "connection failed", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(applicationContext, "connection success", Toast.LENGTH_LONG).show()
                                isReader = true
                                try {
                                    readData()
                                }catch(e:java.lang.Exception){
                                    val inn = Intent("handle.read.data.exception")
                                    inn.putExtra("data", "exception start read data "+e.message)
                                    sendBroadcast(Intent(inn))
                                }
                            }
                         //}
                     }
                  }
            }
        }

    }





    fun readData(){
        isReader=true
        Thread {
            if (driver == null) {
                return@Thread
            }
            while(isReader) {
                // Read some data! Most have just one port (port 0).
                port = driver!!.ports[0]

                try {

                    port?.open(connection)

                    port?.setParameters(115200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1 , UsbSerialPort.PARITY_NONE)



                    val arr = ArrayList<Int>()


                    do {
                        val buffer = ByteArray(quantity)
                        val numBytes=port?.read(buffer,1000)
                        if(numBytes!=null && numBytes >0) {
                            for (i in 0 until numBytes) {
                                arr.add(buffer[i].toInt())
                            }
                        }

                    } while (isReader && arr.size < 10)



                    if(arr.size>9) {
                        val inn = Intent("handle.read.data")
                        inn.putExtra("data", arr)
                        sendBroadcast(Intent(inn))
                    }
                    
                } catch (e: Exception) {
                    val inn = Intent("handle.read.data.exception")
                    inn.putExtra("data", "exception in read data"+e.message)
                    sendBroadcast(Intent(inn))
                    isReader=false

                    // Deal with error.
                } finally {
                    port?.close()
                }
            }

            //if stop read close the connection
            connection?.close()


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
                .setContentTitle("")
                .setSmallIcon(getNotificationIcon())
                .setContentText("").build()

            startForeground(1, notification)
        }
    }
}
