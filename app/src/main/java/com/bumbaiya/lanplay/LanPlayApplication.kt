package com.bumbaiya.lanplay

import android.app.Application
import com.bumbaiya.lanplay.platform.Session

class LanPlayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Session.init(this)
    }
}
