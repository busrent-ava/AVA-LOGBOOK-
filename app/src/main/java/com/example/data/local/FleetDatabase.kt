package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CachedHistoryEntity
import com.example.data.model.PendingLogEntity

@Database(
    entities = [PendingLogEntity::class, CachedHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FleetDatabase : RoomDatabase() {
    abstract fun pendingLogDao(): PendingLogDao
    abstract fun cachedHistoryDao(): CachedHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: FleetDatabase? = null

        fun getDatabase(context: Context): FleetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FleetDatabase::class.java,
                    "fleetops_driver.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
