package com.mcexample.module

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class CalendarModule(context: ReactApplicationContext?) : ReactContextBaseJavaModule(context) {

    override fun getName(): String {
        return "CalendarModule"
    }

    @ReactMethod
    fun createCalendarEvent(name: String, location: String) {
        Log.d("CalendarModule", "Create event called with name: " + name
                + " and location: " + location)
    }

}