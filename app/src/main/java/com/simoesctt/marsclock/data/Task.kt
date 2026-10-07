package com.simoesctt.marsclock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val solDate: Long,
    val title: String,
    val note: String = "",
    val done: Boolean = false,
    val mtcHour: Int = -1,
    val mtcMinute: Int = -1,
    val remind: Boolean = false,
    val recurrenceSols: Int = 0,   // 0 = no repeat, N = every N sols
    val createdAt: Long = System.currentTimeMillis()
)
