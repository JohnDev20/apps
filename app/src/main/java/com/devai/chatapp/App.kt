package com.devai.chatapp

import android.app.Application
import com.devai.chatapp.data.local.AppDatabase

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.getInstance(this) // init
    }
}
