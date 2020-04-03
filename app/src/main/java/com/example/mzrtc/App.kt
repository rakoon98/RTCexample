package com.example.mzrtc

import android.app.Application
import com.example.mzrtc.testsampletry.util.CoChannel

class App: Application() {

    companion object{
        val coChannel = CoChannel()
    }

    override fun onCreate() {
        super.onCreate()
    }

}