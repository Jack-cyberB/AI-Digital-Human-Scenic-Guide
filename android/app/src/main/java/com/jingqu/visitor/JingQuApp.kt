package com.jingqu.visitor

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JingQuApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
