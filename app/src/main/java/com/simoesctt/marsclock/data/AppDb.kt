package com.simoesctt.marsclock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun tasks(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDb? = null

        fun get(ctx: Context): AppDb {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    ctx.applicationContext,
                    AppDb::class.java,
                    "marsclock.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
