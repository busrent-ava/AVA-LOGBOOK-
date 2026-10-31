package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CachedHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedHistoryDao {
    @Query("SELECT * FROM cached_history WHERE LOWER(driverId) = LOWER(:driverId) ORDER BY timestamp DESC")
    fun getHistoryForDriver(driverId: String): Flow<List<CachedHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedHistoryEntity>)

    @Query("DELETE FROM cached_history WHERE LOWER(driverId) = LOWER(:driverId)")
    suspend fun clearForDriver(driverId: String)
}
