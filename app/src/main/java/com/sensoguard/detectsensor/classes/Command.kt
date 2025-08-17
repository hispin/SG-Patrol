package com.sensoguard.detectsensor.classes

import com.sensoguard.detectsensor.global.NORMAL_STATE

class Command(val commandName: String, val commandContent: IntArray?, val icId: Int) {
    var selectionsTitles = ArrayList<String>()
    var selectionsCommands = ArrayList<String>()
    var defaultSelected: Int = 1
    var state: Int = NORMAL_STATE
    var isExpand = false
    var maxTimeout = 60
    var sensCar = 0
    var sensIntruder = 0
    var snrCar = 0
    var snrIntruder: Float = 0f
    var logicCountCar = 0
    var logicdurationCar = 0
    var logicCountIntruder = 0
    var logicdurationIntruder = 0
    var logicSecomds = 0
    var minPower = 0
}