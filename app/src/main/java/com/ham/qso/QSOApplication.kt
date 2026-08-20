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

    val repository: QSORepository by lazy {
        QSORepository(
            qsoDao = database.qsoDao(),
            sessionDao = database.sessionDao()
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
