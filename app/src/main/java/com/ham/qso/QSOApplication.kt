package com.ham.qso

import android.app.Application
import com.ham.qso.data.local.AppDatabase
import com.ham.qso.data.repository.QSORepository

/**
 * Application 全局单例与依赖容器
 */
class QSOApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val appPreferences: com.ham.qso.data.local.AppPreferences by lazy {
        com.ham.qso.data.local.AppPreferences(this)
    }

    val repository: QSORepository by lazy {
        QSORepository(
            qsoDao = database.qsoDao(),
            sessionDao = database.sessionDao(),
            appPreferences = appPreferences
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: QSOApplication
            private set
    }
}
