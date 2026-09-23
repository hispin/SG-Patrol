package com.sensoguard.detectsensor.activities

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.sensoguard.detectsensor.global.configurationLanguage


open class ParentActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        configurationLanguage(this)
        super.onCreate(savedInstanceState)
        //opt in to edge-to-edge; each screen's toolbar is dark, so force light
        //(white) status bar icons instead of relying on the deprecated
        //android:statusBarColor/windowLightStatusBar theme attributes
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        super.onKeyDown(keyCode, event)
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            Log.d("detectKey", "home")
            //The Code Want to Perform.

        }
        return true
    }

}