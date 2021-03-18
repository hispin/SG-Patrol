package com.sensoguard.detectsensor.classes

import com.sensoguard.detectsensor.global.NORMAL_STATE

class Command(val commandName: String, val commandContent: IntArray?, val icId: Int) {
    var selectionsTitles = ArrayList<String>()
    var selectionsCommands = ArrayList<String>()
    var defaultSelected: Int = 1
    var state: Int = NORMAL_STATE
    var isExpand = false
    var maxTimeout = 60
}