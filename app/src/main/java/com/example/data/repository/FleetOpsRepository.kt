package com.example.data.repository

import android.content.Context
import com.example.data.local.FleetDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.ApiRequest
import com.example.data.model.CachedHistoryEntity
import com.example.data.model.DriverSession
import com.example.data.model.LogEntryItem
import com.example.data.model.PendingLogEntity
import com.example.data.remote.FleetOpsApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FleetOpsRepository(private val context: Context) {

    private val db = FleetDatabase.getDatabase(context)
    private val pendingDao = db.pendingLogDao()
    private val historyDao = db.cachedHistoryDao()
    val preferences = PreferencesManager(context)
    private val apiClient = FleetOpsApiClient()

    companion object {
        val DEMO_BUSES = listOf("BUS-01", "BUS-02", "BUS-03", "BUS-04", "BUS-05")
    }

    fun getPendingLogs(driverId: String): Flow<List<PendingLogEntity>> =
        pendingDao.getPendingLogsForDriver(driverId)

    fun getPendingCount(driverId: String): Flow<Int> =
        pendingDao.getPendingCount(driverId)

    fun getCachedHistory(driverId: String): Flow<List<LogEntryItem>> =
        historyDao.getHistoryForDriver(driverId).map { entities ->
            entities.map { it.toLogEntryItem() }
        }

    suspend fun login(driverId: String, pin: String, url: String): Result<DriverSession> = withContext(Dispatchers.IO) {
        val trimmedId = driverId.trim()
        val trimmedPin = pin.trim()
        val trimmedUrl = url.trim()

        // Demo fallback mode for offline/quick test if URL is empty or demo driver D001
        if (trimmedUrl.isEmpty() || (trimmedId.equals("D001", ignoreCase = true) && trimmedPin == "1234")) {
            val session = DriverSession(
                driverId = trimmedId.ifEmpty { "D001" },
                driverName = if (trimmedId.equals("D001", ignoreCase = true)) "Demo Driver (Alex)" else "Driver $trimmedId",
                pin = trimmedPin,
                backendUrl = trimmedUrl,
                reminders = listOf("08:00", "13:00", "18:00", "22:00"),
                lastOdo = mapOf("BUS-01" to 14250.0, "BUS-02" to 28900.0, "BUS-03" to 8540.0)
            )
            preferences.backendUrl = trimmedUrl
            preferences.savedDriverId = session.driverId
            preferences.savedDriverName = session.driverName
            preferences.savedPin = session.pin
            preferences.saveLastOdoMap(session.lastOdo)
            preferences.saveReminders(session.reminders)
            if (preferences.getCachedBuses().isEmpty()) {
                preferences.saveCachedBuses(DEMO_BUSES)
            }
            return@withContext Result.success(session)
        }

        try {
            val response = apiClient.login(trimmedUrl, trimmedId, trimmedPin)
            if (response.status == "success") {
                val driverName = response.driverName ?: "Driver $trimmedId"
                val reminders = response.reminders?.filter { it.isNotBlank() } ?: emptyList()
                val lastOdo = response.lastOdo ?: emptyMap()

                preferences.backendUrl = trimmedUrl
                preferences.savedDriverId = trimmedId
                preferences.savedDriverName = driverName
                preferences.savedPin = trimmedPin
                preferences.saveLastOdoMap(lastOdo)
                preferences.saveReminders(reminders)

                val session = DriverSession(
                    driverId = trimmedId,
                    driverName = driverName,
                    pin = trimmedPin,
                    backendUrl = trimmedUrl,
                    reminders = reminders,
                    lastOdo = lastOdo
                )
                Result.success(session)
            } else {
                Result.failure(Exception(response.message ?: "Invalid Driver ID or PIN"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot reach backend: ${e.localizedMessage ?: "Connection error"}"))
        }
    }

    suspend fun fetchBuses(url: String): List<String> = withContext(Dispatchers.IO) {
        if (url.isBlank()) {
            return@withContext preferences.getCachedBuses()
        }
        try {
            val response = apiClient.getBuses(url)
            val buses = response.buses?.filter { it.isNotBlank() }
            if (!buses.isNullOrEmpty()) {
                preferences.saveCachedBuses(buses)
                return@withContext buses
            }
        } catch (_: Exception) {}
        preferences.getCachedBuses()
    }

    suspend fun refreshHistory(url: String, driverId: String, pin: String): Result<List<LogEntryItem>> = withContext(Dispatchers.IO) {
        if (url.isBlank()) {
            return@withContext Result.success(emptyList())
        }
        try {
            val response = apiClient.getMyLogs(url, driverId, pin)
            if (response.status == "success" && response.logs != null) {
                val entities = response.logs.map { it.toCachedEntity() }
                historyDao.clearForDriver(driverId)
                historyDao.insertAll(entities)
                Result.success(response.logs)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load logs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitLog(
        session: DriverSession,
        busNumber: String,
        category: String,
        odoReading: String,
        expenseQty: String,
        expenseAmount: String,
        location: String,
        capturedAt: String,
        odoPhotoBase64: String,
        receiptPhotoBase64: String
    ): SubmitResult = withContext(Dispatchers.IO) {
        val odoValue = odoReading.toDoubleOrNull() ?: 0.0

        val request = ApiRequest(
            action = "log",
            driverId = session.driverId,
            pin = session.pin,
            busNumber = busNumber,
            category = category,
            odoReading = odoReading,
            expenseQty = expenseQty,
            expenseAmount = expenseAmount,
            location = location,
            capturedAt = capturedAt,
            odoPhotoBase64 = odoPhotoBase64,
            receiptPhotoBase64 = receiptPhotoBase64
        )

        if (session.backendUrl.isBlank()) {
            // Demo/offline mode simulation: simulate immediate success and save to history
            preferences.updateLastOdoForBus(busNumber, odoValue)
            val localId = "LOG-LOCAL-${System.currentTimeMillis()}"
            val logItem = LogEntryItem(
                id = localId,
                timestamp = capturedAt,
                driverId = session.driverId,
                driverName = session.driverName,
                busNumber = busNumber,
                category = category,
                odoReading = odoReading,
                expenseQty = expenseQty.ifEmpty { null },
                expenseAmount = expenseAmount.ifEmpty { null },
                odoUrl = null,
                receiptUrl = null,
                location = location,
                note = "Saved offline/demo"
            )
            historyDao.insertAll(listOf(logItem.toCachedEntity()))
            return@withContext SubmitResult.Success("Saved locally (Demo mode)")
        }

        try {
            val response = apiClient.submitLog(session.backendUrl, request)
            if (response.status == "success") {
                preferences.updateLastOdoForBus(busNumber, odoValue)
                val newId = response.id ?: "LOG-${System.currentTimeMillis()}"
                val logItem = LogEntryItem(
                    id = newId,
                    timestamp = capturedAt,
                    driverId = session.driverId,
                    driverName = session.driverName,
                    busNumber = busNumber,
                    category = category,
                    odoReading = odoReading,
                    expenseQty = expenseQty.ifEmpty { null },
                    expenseAmount = expenseAmount.ifEmpty { null },
                    odoUrl = response.odoUrl,
                    receiptUrl = response.receiptUrl,
                    location = location
                )
                historyDao.insertAll(listOf(logItem.toCachedEntity()))
                SubmitResult.Success("Log submitted successfully")
            } else {
                SubmitResult.Error(response.message ?: "Submission rejected by server")
            }
        } catch (e: Exception) {
            // Network failure or no signal -> Queue offline!
            val pending = PendingLogEntity(
                driverId = session.driverId,
                pin = session.pin,
                busNumber = busNumber,
                category = category,
                odoReading = odoReading,
                expenseQty = expenseQty,
                expenseAmount = expenseAmount,
                location = location,
                capturedAt = capturedAt,
                odoPhotoBase64 = odoPhotoBase64,
                receiptPhotoBase64 = receiptPhotoBase64,
                timestamp = System.currentTimeMillis()
            )
            pendingDao.insert(pending)
            preferences.updateLastOdoForBus(busNumber, odoValue)
            SubmitResult.QueuedOffline("📡 No signal. Saved on this phone and will send automatically.")
        }
    }

    suspend fun flushPendingQueue(session: DriverSession): Int = withContext(Dispatchers.IO) {
        if (session.backendUrl.isBlank()) return@withContext 0
        val eligible = pendingDao.getEligibleLogsToSend(session.driverId)
        if (eligible.isEmpty()) return@withContext 0

        var sentCount = 0
        for (item in eligible) {
            val request = ApiRequest(
                action = "log",
                driverId = item.driverId,
                pin = item.pin,
                busNumber = item.busNumber,
                category = item.category,
                odoReading = item.odoReading,
                expenseQty = item.expenseQty,
                expenseAmount = item.expenseAmount,
                location = item.location,
                capturedAt = item.capturedAt,
                odoPhotoBase64 = item.odoPhotoBase64,
                receiptPhotoBase64 = item.receiptPhotoBase64
            )
            try {
                val response = apiClient.submitLog(session.backendUrl, request)
                if (response.status == "success") {
                    pendingDao.deleteById(item.qid)
                    sentCount++
                    // Also record to cached history
                    val odo = item.odoReading.toDoubleOrNull() ?: 0.0
                    preferences.updateLastOdoForBus(item.busNumber, odo)
                    val logItem = LogEntryItem(
                        id = response.id ?: "LOG-${item.qid}",
                        timestamp = item.capturedAt,
                        driverId = item.driverId,
                        driverName = session.driverName,
                        busNumber = item.busNumber,
                        category = item.category,
                        odoReading = item.odoReading,
                        expenseQty = item.expenseQty.ifEmpty { null },
                        expenseAmount = item.expenseAmount.ifEmpty { null },
                        odoUrl = response.odoUrl,
                        receiptUrl = response.receiptUrl,
                        location = item.location
                    )
                    historyDao.insertAll(listOf(logItem.toCachedEntity()))
                } else {
                    pendingDao.update(item.copy(errorMessage = response.message ?: "Rejected by server"))
                }
            } catch (_: Exception) {
                // Still no connectivity, keep queued
                break
            }
        }
        sentCount
    }

    suspend fun deletePendingItem(qid: Long) = withContext(Dispatchers.IO) {
        pendingDao.deleteById(qid)
    }

    private fun CachedHistoryEntity.toLogEntryItem() = LogEntryItem(
        id = id,
        timestamp = timestamp,
        driverId = driverId,
        driverName = driverName,
        busNumber = busNumber,
        category = category,
        odoReading = odoReading,
        expenseQty = expenseQty,
        expenseAmount = expenseAmount,
        odoUrl = odoUrl,
        receiptUrl = receiptUrl,
        location = location,
        note = note
    )

    private fun LogEntryItem.toCachedEntity() = CachedHistoryEntity(
        id = id.ifEmpty { "LOG-${System.currentTimeMillis()}" },
        driverId = driverId,
        timestamp = timestamp,
        driverName = driverName,
        busNumber = busNumber,
        category = category,
        odoReading = odoReading,
        expenseQty = expenseQty,
        expenseAmount = expenseAmount,
        odoUrl = odoUrl,
        receiptUrl = receiptUrl,
        location = location,
        note = note
    )
}

sealed class SubmitResult {
    data class Success(val message: String) : SubmitResult()
    data class QueuedOffline(val message: String) : SubmitResult()
    data class Error(val message: String) : SubmitResult()
}
