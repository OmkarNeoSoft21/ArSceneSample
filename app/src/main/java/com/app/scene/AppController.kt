package com.app.scene

import android.app.Application
import com.google.android.filament.Filament

class AppController: Application() {

    override fun onCreate() {
        super.onCreate()
        Filament.init()
    }
}