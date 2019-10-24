package com.sensoguard.detectsensor.global

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

//Get the width of current screen
fun getScreenWidth(context: Context?): Int {

    if (context == null) {
        return -1
    }

    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val metrics = DisplayMetrics()

    wm.defaultDisplay.getMetrics(metrics)

    return metrics.widthPixels
}