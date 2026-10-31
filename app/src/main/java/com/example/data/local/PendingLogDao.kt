package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PendingLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingLogDao {
    @Query("SELECT * FROM pending_logs WHERE LOWER(driverId) = LOWER(:driverId) ORDER BY timestamp DESC")
    fun getPendingLogsForDriver(driverId: String): Flow<List<PendingLogEntity>>

    @Query("SELECT * FROM pending_logs WHERE LOWER(driverId) = LOWER(:driverId) AND errorMessage IS NULL ORDER BY timestamp ASC")
    suspend fun getEligibleLogsToSend(driverId: String): List<PendingLogEntity>

    @Query("SELECT COUNT(*) FROM pending_logs WHERE LOWER(driverId) = LOWER(:driverId)")
    fun getPendingCount(driverId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PendingLogEntity): Long

    @Update
    suspend fun update(item: PendingLogEntity)

    @Query("DELETE FROM pending_logs WHERE qid = :qid")
    suspend fun deleteById(qid: Long)

    @Query("DELETE FROM pending_logs WHERE LOWER(driverId) = LOWER(:driverId)")
    suspend fun clearForDriver(driverId: String)
}
