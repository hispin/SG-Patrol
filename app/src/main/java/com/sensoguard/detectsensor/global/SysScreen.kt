package com.sensoguard.detectsensor.global

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.WindowManager
import kotlin.math.roundToInt


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

fun dpToPx(dp: Int, context: Context): Int {
    val r: Resources = context.resources
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        r.displayMetrics
    ).roundToInt()
}