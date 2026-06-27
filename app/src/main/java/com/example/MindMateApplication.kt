package com.example

import android.app.Application
import com.example.di.AppContainer

/**
 * Custom application class to hold global references and initialize our DI AppContainer.
 */
class MindMateApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
