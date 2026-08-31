package com.ham.qso.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ham.qso.data.model.QSOEntity
import com.ham.qso.data.model.SessionEntity

/**
 * Room 数据库主入口
 *
 * version 1 — 初始版本，包含所有 v1 字段：
 *   QSOEntity: timeZoneId, qth, altitudeMeters, theirRig, theirAntenna, theirPowerWatts
 */
@Database(
    entities = [QSOEntity::class, SessionEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun qsoDao(): QSODao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "field_qso.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
