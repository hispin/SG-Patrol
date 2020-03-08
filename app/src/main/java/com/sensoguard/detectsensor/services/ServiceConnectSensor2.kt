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
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.sensoguard.detectsensor.R


class ServiceConnectSensor2 : Service() {

    var manager:UsbManager?=null
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    var connection:UsbDeviceConnection?=null
    private var device: UsbDevice? = null
    private var serialPort: UsbSerialDevice? = null
    var numBytesCount=0
    var readData :ArrayList<Int>?=null

    override fun onCreate() {
        super.onCreate()
        startSysForeGround()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        setFilter()

        findDriversForDevices()

        return return START_NOT_STICKY
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, arg1: Intent) {
            if (arg1.action == "find.drivers.command") {
                findDriversForDevices()
            }else if (arg1.action == "read.data.command") {
                readData()
            } else if (arg1.action == "handle.read.data") {
                Toast.makeText(applicationContext,"accept data",Toast.LENGTH_LONG).show()
//                val buffer=arg1.getByteArrayExtra("data")
////                val data = String(buffer)
////                Toast.makeText(applicationContext, "data reader $data",Toast.LENGTH_LONG).show()
//                val b1=buffer[5]
//                for(item in 0 until buffer.size){
//                    Toast.makeText(applicationContext,"handle read bl int "+buffer[item].toInt()+" item:"+item,Toast.LENGTH_LONG).show()
//                }

            } else if(arg1.action == "stop.data.command"){
                serialPort!!.close()
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serialPort!!.close()
//        if(port!=null ){
//            port?.close()
//        }
        unregisterReceiver(usbReceiver)
    }

    private fun setFilter() {
        val filter = IntentFilter("find.drivers.command")
        filter.addAction("read.data.command")
        filter.addAction("handle.read.data")
        filter.addAction("stop.data.command")
        filter.addAction(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
    }

    fun findDriversForDevices(){

        // Find all available drivers from attached devices.
        manager = getSystemService(Context.USB_SERVICE) as UsbManager

        val usbDevices = manager?.deviceList

        if (usbDevices!=null && usbDevices.isEmpty()) {
            Toast.makeText(this,"is empty",Toast.LENGTH_SHORT).show()
            return
        }

        device = usbDevices?.values?.first()

        if(device!=null){
            openConnection()
        }else{
            Toast.makeText(this,"not find devices",Toast.LENGTH_SHORT).show()
        }
    }


    fun openConnection(){
        device?.let { registerUsbPermission(it) }
    }


    fun registerUsbPermission(usbDevice: UsbDevice) {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction("handle.read.data")
        registerReceiver(usbPermissionReceiver, filter)
        val mPermissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        usbManager.requestPermission(usbDevice, mPermissionIntent)
        connection = manager?.openDevice(device)
        if(connection==null){
            Toast.makeText(this,"connection failed",Toast.LENGTH_SHORT).show()

        }else{
            Toast.makeText(this,"connection success",Toast.LENGTH_SHORT).show()
            readData()
        }
    }


    private var usbPermissionReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == ACTION_USB_PERMISSION) {

//                if(!isReader) {
//                    synchronized(this) {
//                        val connection = manager?.openDevice(driver?.device)
//                        if (connection == null) {
//                            Toast.makeText(applicationContext, "connection failed", Toast.LENGTH_LONG).show()
//                        } else {
//                            Toast.makeText(applicationContext, "connection success", Toast.LENGTH_LONG).show()
//                            isReader = true
//                            //readData()
//                        }
//                    }
//                }
            }
        }

    }



    fun readData() {
        if (device == null) {
            return
        }
        Thread {
            // Read some data! Most have just one port (port 0).
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection)




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
    protected fun startSysForeGround() {
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

    private val mCallback = UsbSerialInterface.UsbReadCallback { bytesArray ->

        sendBroadcast(Intent("handle.read.data"))


//        numBytesCount+=bytesArray.size
//
//        for(i in 0 until bytesArray.size){
//            if(readData!=null && readData?.size!! <16) {
//                readData?.add(bytesArray[i].toInt())
//            }
//            //sb.append("\n"+buffer[i].toString())
//        }
//
//        if(readData!=null && readData?.size!!>=8 && readData?.get(7)==3){
//            val inn = Intent("handle.read.data")
//            inn.putExtra("data",readData)
//
//            //clear the array
//            readData= ArrayList()
//            //inn.putExtra("data1",arg0[1].toString())
//            sendBroadcast(Intent(inn))
//        }
//        //Toast.makeText(applicationContext, "data reader" +arg0[0].toString(),Toast.LENGTH_LONG).show()
////       val inn = Intent("handle.read.data")
////       inn.putExtra("data",arg0[0].toString())
////       //inn.putExtra("data1",arg0[1].toString())
////       sendBroadcast(Intent(inn))
////        bs=StringBuilder()
////        bs?.append("\n data:"+arg0[0].toString())
////        tvShowProgress?.text=bs.toString()
//         // sendBroadcast(Intent("handle.read.data"))
//        try {
//
//            //val data = String(arg0)
////            bs?.append("\n data:$data")
////            tvShowProgress?.text=bs.toString()
//            //mHandler?.obtainMessage(MESSAGE_FROM_SERIAL_PORT, data)?.sendToTarget()
//        } catch (e: UnsupportedEncodingException) {
//            e.printStackTrace()
//        }
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
}
