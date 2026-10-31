package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class LogCategory(val displayName: String, val isTrip: Boolean) {
    TRIP_START("Trip Start", true),
    TRIP_END("Trip End", true),
    FUEL("Fuel", false),
    MAINTENANCE("Maintenance", false),
    OTHER_EXPENSE("Other Expense", false);

    companion object {
        fun fromDisplayName(name: String): LogCategory {
            return entries.find { it.displayName.equals(name, ignoreCase = true) } ?: TRIP_START
        }
    }
}

data class DriverSession(
    val driverId: String,
    val driverName: String,
    val pin: String,
    val backendUrl: String,
    val reminders: List<String> = emptyList(),
    val lastOdo: Map<String, Double> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class LogEntryItem(
    @Json(name = "id") val id: String = "",
    @Json(name = "timestamp") val timestamp: String = "",
    @Json(name = "driverId") val driverId: String = "",
    @Json(name = "driverName") val driverName: String = "",
    @Json(name = "busNumber") val busNumber: String = "",
    @Json(name = "category") val category: String = "",
    @Json(name = "odoReading") val odoReading: String = "",
    @Json(name = "expenseQty") val expenseQty: String? = null,
    @Json(name = "expenseAmount") val expenseAmount: String? = null,
    @Json(name = "odoUrl") val odoUrl: String? = null,
    @Json(name = "receiptUrl") val receiptUrl: String? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "note") val note: String? = null
)

@Entity(tableName = "pending_logs")
data class PendingLogEntity(
    @PrimaryKey(autoGenerate = true) val qid: Long = 0,
    val driverId: String,
    val pin: String,
    val busNumber: String,
    val category: String,
    val odoReading: String,
    val expenseQty: String,
    val expenseAmount: String,
    val location: String,
    val capturedAt: String,
    val odoPhotoBase64: String,
    val receiptPhotoBase64: String,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

@Entity(tableName = "cached_history")
data class CachedHistoryEntity(
    @PrimaryKey val id: String,
    val driverId: String,
    val timestamp: String,
    val driverName: String,
    val busNumber: String,
    val category: String,
    val odoReading: String,
    val expenseQty: String?,
    val expenseAmount: String?,
    val odoUrl: String?,
    val receiptUrl: String?,
    val location: String?,
    val note: String?
)

// Apps Script API Request & Response structures
@JsonClass(generateAdapter = true)
data class ApiRequest(
    @Json(name = "action") val action: String,
    @Json(name = "driverId") val driverId: String? = null,
    @Json(name = "pin") val pin: String? = null,
    @Json(name = "busNumber") val busNumber: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "odoReading") val odoReading: String? = null,
    @Json(name = "expenseQty") val expenseQty: String? = null,
    @Json(name = "expenseAmount") val expenseAmount: String? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "capturedAt") val capturedAt: String? = null,
    @Json(name = "odoPhotoBase64") val odoPhotoBase64: String? = null,
    @Json(name = "receiptPhotoBase64") val receiptPhotoBase64: String? = null
)

@JsonClass(generateAdapter = true)
data class BaseApiResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginApiResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "message") val message: String? = null,
    @Json(name = "driverName") val driverName: String? = null,
    @Json(name = "reminders") val reminders: List<String>? = null,
    @Json(name = "lastOdo") val lastOdo: Map<String, Double>? = null
)

@JsonClass(generateAdapter = true)
data class BusesApiResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "buses") val buses: List<String>? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class MyLogsApiResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "logs") val logs: List<LogEntryItem>? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class LogSubmitApiResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "id") val id: String? = null,
    @Json(name = "odoUrl") val odoUrl: String? = null,
    @Json(name = "receiptUrl") val receiptUrl: String? = null,
    @Json(name = "message") val message: String? = null
)
