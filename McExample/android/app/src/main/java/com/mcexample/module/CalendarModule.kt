package com.mcexample.module

import android.content.Intent
import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.mastercard.mpsdksample.mpausingwul.activity.SplashActivity

class CalendarModule(private val context: ReactApplicationContext?) : ReactContextBaseJavaModule(context) {

    override fun getName(): String {
        return "CalendarModule"
    }

    @ReactMethod
    fun createCalendarEvent(name: String, location: String) {
        Log.d("CalendarModule", "Create event called with name: " + name
                + " and location: " + location)


        // launch activation
        val i = Intent(currentActivity, SplashActivity::class.java)
        currentActivity?.startActivity(i)
    }

}