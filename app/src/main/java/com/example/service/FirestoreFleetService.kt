package com.example.service

import android.content.Context
import com.example.R
import com.example.data.model.LogEntryItem
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreFleetService(private val context: Context) {

    private val firestore by lazy {
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            if (dbId.isNotEmpty()) {
                FirebaseFirestore.getInstance(FirebaseApp.getInstance(), dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (_: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    suspend fun saveLogToFirestore(item: LogEntryItem): Result<Unit> {
        return try {
            val docId = item.id.ifEmpty { "LOG-${System.currentTimeMillis()}" }
            val map = hashMapOf(
                "id" to docId,
                "timestamp" to item.timestamp,
                "driverId" to item.driverId,
                "driverName" to item.driverName,
                "busNumber" to item.busNumber,
                "category" to item.category,
                "odoReading" to item.odoReading,
                "expenseQty" to (item.expenseQty ?: ""),
                "expenseAmount" to (item.expenseAmount ?: ""),
                "location" to (item.location ?: ""),
                "syncedAt" to System.currentTimeMillis()
            )
            firestore.collection("fleet_logs").document(docId).set(map).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
