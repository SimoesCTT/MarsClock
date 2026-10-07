package com.simoesctt.marsclock.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE solDate = :sol ORDER BY mtcHour ASC, id ASC")
    fun forSol(sol: Long): Flow<List<Task>>

    @Query("SELECT solDate, COUNT(*) as c FROM tasks WHERE done = 0 GROUP BY solDate")
    fun pendingCounts(): Flow<List<SolCount>>

    @Query("SELECT * FROM tasks WHERE remind = 1 AND done = 0")
    suspend fun allReminders(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: Task): Long

    @Update
    suspend fun update(t: Task)

    @Delete
    suspend fun delete(t: Task)
}

data class SolCount(val solDate: Long, val c: Int)
