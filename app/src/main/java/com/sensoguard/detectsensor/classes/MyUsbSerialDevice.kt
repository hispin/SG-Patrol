//package com.sensoguard.detectsensor.classes
//
//import android.hardware.usb.UsbDevice
//import android.hardware.usb.UsbDeviceConnection
//import com.felhr.usbserial.UsbSerialDevice
//import com.felhr.usbserial.UsbSerialInterface
//
//open class MyUsbSerialDevice(device: UsbDevice?, connection: UsbDeviceConnection?) :
//    UsbSerialDevice(device, connection) {
//    override fun open(): Boolean {
//        open()
//        //super.op
//        TODO("Not yet implemented")
//    }
//
//    override fun close() {
//        TODO("Not yet implemented")
//    }
//
//    override fun syncOpen(): Boolean {
//        TODO("Not yet implemented")
//    }
//
//    override fun syncClose() {
//        TODO("Not yet implemented")
//    }
//
//    override fun setBaudRate(baudRate: Int) {
//        TODO("Not yet implemented")
//    }
//
//    override fun setDataBits(dataBits: Int) {
//        TODO("Not yet implemented")
//    }
//
//    override fun setStopBits(stopBits: Int) {
//        TODO("Not yet implemented")
//    }
//
//    override fun setParity(parity: Int) {
//        TODO("Not yet implemented")
//    }
//
//    override fun setFlowControl(flowControl: Int) {
//        TODO("Not yet implemented")
//    }
//
//    override fun setBreak(state: Boolean) {
//        TODO("Not yet implemented")
//    }
//
//    override fun setRTS(state: Boolean) {
//        TODO("Not yet implemented")
//    }
//
//    override fun setDTR(state: Boolean) {
//        TODO("Not yet implemented")
//    }
//
//    override fun getCTS(ctsCallback: UsbSerialInterface.UsbCTSCallback?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun getDSR(dsrCallback: UsbSerialInterface.UsbDSRCallback?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun getBreak(breakCallback: UsbSerialInterface.UsbBreakCallback?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun getFrame(frameCallback: UsbSerialInterface.UsbFrameCallback?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun getOverrun(overrunCallback: UsbSerialInterface.UsbOverrunCallback?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun getParity(parityCallback: UsbSerialInterface.UsbParityCallback?) {
//        TODO("Not yet implemented")
//    }
//
//
//}