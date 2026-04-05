package com.mindflow

import android.app.Application
import com.mindflow.di.appModule
import com.mindflow.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * MindFlow Application class
 */
class MindFlowApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Koin DI
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MindFlowApp)
            modules(listOf(appModule, viewModelModule))
        }
    }
}
