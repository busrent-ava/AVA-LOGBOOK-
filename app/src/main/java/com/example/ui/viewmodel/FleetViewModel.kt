package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DriverSession
import com.example.data.model.LogCategory
import com.example.data.model.LogEntryItem
import com.example.data.model.PendingLogEntity
import com.example.data.repository.FleetOpsRepository
import com.example.data.repository.SubmitResult
import com.example.service.ImageStamper
import com.example.service.LocationTracker
import com.example.service.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class LoginUiState(
    val driverId: String = "",
    val pin: String = "",
    val backendUrl: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class LogFormUiState(
    val selectedBus: String = "",
    val category: LogCategory = LogCategory.TRIP_START,
    val odoReading: String = "",
    val expenseQty: String = "",
    val expenseAmount: String = "",
    val odoBitmap: Bitmap? = null,
    val odoBase64: String = "",
    val receiptBitmap: Bitmap? = null,
    val receiptBase64: String = "",
    val isSubmitting: Boolean = false,
    val statusMessage: String? = null,
    val isSuccessStatus: Boolean = false,
    val showLargeOdoDialog: Boolean = false,
    val pendingSubmitOdoDiff: Double = 0.0,
    val isAiScanningOdo: Boolean = false,
    val isAiScanningReceipt: Boolean = false
)

class FleetViewModel(application: Application) : AndroidViewModel(application) {

    val repository = FleetOpsRepository(application)
    val locationTracker = LocationTracker(application)
    val reminderScheduler = ReminderScheduler(application)
    val geminiAi = com.example.service.GeminiFleetAiService()
    val firestoreService = com.example.service.FirestoreFleetService(application)

    private val _copilotResponse = MutableStateFlow<String?>(null)
    val copilotResponse: StateFlow<String?> = _copilotResponse.asStateFlow()

    private val _isCopilotLoading = MutableStateFlow(false)
    val isCopilotLoading: StateFlow<Boolean> = _isCopilotLoading.asStateFlow()

    private val _session = MutableStateFlow<DriverSession?>(null)
    val session: StateFlow<DriverSession?> = _session.asStateFlow()

    private val _loginState = MutableStateFlow(
        LoginUiState(
            driverId = repository.preferences.savedDriverId.ifEmpty { "D001" },
            pin = repository.preferences.savedPin,
            backendUrl = repository.preferences.backendUrl
        )
    )
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _formState = MutableStateFlow(LogFormUiState())
    val formState: StateFlow<LogFormUiState> = _formState.asStateFlow()

    private val _buses = MutableStateFlow<List<String>>(repository.preferences.getCachedBuses())
    val buses: StateFlow<List<String>> = _buses.asStateFlow()

    private val _lastOdoMap = MutableStateFlow<Map<String, Double>>(repository.preferences.getLastOdoMap())
    val lastOdoMap: StateFlow<Map<String, Double>> = _lastOdoMap.asStateFlow()

    private val _reminders = MutableStateFlow<List<String>>(repository.preferences.getReminders())
    val reminders: StateFlow<List<String>> = _reminders.asStateFlow()

    val gpsLocation: StateFlow<String> = locationTracker.currentLocationString

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Offline pending logs
    val pendingLogs: StateFlow<List<PendingLogEntity>> = repository.getPendingLogs(
        repository.preferences.savedDriverId.ifEmpty { "D001" }
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History logs
    val historyLogs: StateFlow<List<LogEntryItem>> = repository.getCachedHistory(
        repository.preferences.savedDriverId.ifEmpty { "D001" }
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active screen navigation
    private val _activeTab = MutableStateFlow(0) // 0: New Log, 1: History, 2: Reminders & Buses, 3: Pending Queue
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    // Camera capture modal state
    private val _cameraTarget = MutableStateFlow<String?>(null) // "odo" or "receipt"
    val cameraTarget: StateFlow<String?> = _cameraTarget.asStateFlow()

    init {
        locationTracker.startLocationUpdates()
        // If previously logged in, restore session automatically
        val savedId = repository.preferences.savedDriverId
        val savedPin = repository.preferences.savedPin
        if (savedId.isNotBlank() && savedPin.isNotBlank()) {
            val restored = DriverSession(
                driverId = savedId,
                driverName = repository.preferences.savedDriverName.ifEmpty { "Driver $savedId" },
                pin = savedPin,
                backendUrl = repository.preferences.backendUrl,
                reminders = repository.preferences.getReminders(),
                lastOdo = repository.preferences.getLastOdoMap()
            )
            _session.value = restored
            refreshAfterLogin(restored)
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopLocationUpdates()
    }

    fun setActiveTab(index: Int) {
        _activeTab.value = index
    }

    fun onDriverIdChange(value: String) {
        _loginState.value = _loginState.value.copy(driverId = value, errorMessage = null)
    }

    fun onPinChange(value: String) {
        if (value.length <= 4) {
            _loginState.value = _loginState.value.copy(pin = value, errorMessage = null)
        }
    }

    fun onBackendUrlChange(value: String) {
        _loginState.value = _loginState.value.copy(backendUrl = value, errorMessage = null)
    }

    fun setQuickDemoCredentials() {
        _loginState.value = _loginState.value.copy(
            driverId = "D001",
            pin = "1234",
            errorMessage = null
        )
    }

    fun login() {
        val state = _loginState.value
        val driverId = state.driverId.trim()
        val pin = state.pin.trim()
        if (driverId.isEmpty()) {
            _loginState.value = state.copy(errorMessage = "Please enter Driver ID")
            return
        }
        if (pin.isEmpty()) {
            _loginState.value = state.copy(errorMessage = "Please enter 4-digit PIN")
            return
        }

        viewModelScope.launch {
            _loginState.value = state.copy(isLoading = true, errorMessage = null)
            val result = repository.login(driverId, pin, state.backendUrl)
            result.fold(
                onSuccess = { session ->
                    _loginState.value = state.copy(isLoading = false, errorMessage = null)
                    _session.value = session
                    refreshAfterLogin(session)
                },
                onFailure = { error ->
                    _loginState.value = state.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Login failed"
                    )
                }
            )
        }
    }

    private fun refreshAfterLogin(session: DriverSession) {
        viewModelScope.launch {
            val busList = repository.fetchBuses(session.backendUrl)
            _buses.value = busList
            if (_formState.value.selectedBus.isEmpty() && busList.isNotEmpty()) {
                _formState.value = _formState.value.copy(selectedBus = busList.first())
            }
            _lastOdoMap.value = repository.preferences.getLastOdoMap()
            _reminders.value = repository.preferences.getReminders()

            // Fetch recent logs
            repository.refreshHistory(session.backendUrl, session.driverId, session.pin)

            // Auto-flush pending queue
            syncPendingLogs()
        }
    }

    fun logout() {
        repository.preferences.clearSession()
        _session.value = null
        _loginState.value = _loginState.value.copy(pin = "", errorMessage = null)
        resetForm()
    }

    // Form operations
    fun selectBus(bus: String) {
        _formState.value = _formState.value.copy(selectedBus = bus, statusMessage = null)
    }

    fun selectCategory(cat: LogCategory) {
        _formState.value = _formState.value.copy(category = cat, statusMessage = null)
    }

    fun onOdoReadingChange(value: String) {
        _formState.value = _formState.value.copy(odoReading = value, statusMessage = null)
    }

    fun onExpenseQtyChange(value: String) {
        _formState.value = _formState.value.copy(expenseQty = value, statusMessage = null)
    }

    fun onExpenseAmountChange(value: String) {
        _formState.value = _formState.value.copy(expenseAmount = value, statusMessage = null)
    }

    fun openCamera(target: String) {
        if (_formState.value.selectedBus.isEmpty()) {
            _formState.value = _formState.value.copy(
                statusMessage = "Please select a bus first",
                isSuccessStatus = false
            )
            return
        }
        _cameraTarget.value = target
    }

    fun closeCamera() {
        _cameraTarget.value = null
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        val target = _cameraTarget.value ?: return
        val session = _session.value ?: return
        val bus = _formState.value.selectedBus

        // Apply watermarking stamp (Driver ID, Bus, GPS Location, Timestamp)
        val stamped = ImageStamper.stampImage(
            original = bitmap,
            driverId = session.driverId,
            busNumber = bus,
            location = gpsLocation.value,
            timestamp = Date()
        )

        if (target == "odo") {
            _formState.value = _formState.value.copy(
                odoBitmap = stamped.bitmap,
                odoBase64 = stamped.base64DataUrl,
                statusMessage = null
            )
        } else {
            _formState.value = _formState.value.copy(
                receiptBitmap = stamped.bitmap,
                receiptBase64 = stamped.base64DataUrl,
                statusMessage = null
            )
        }
        _cameraTarget.value = null
    }

    fun removePhoto(target: String) {
        if (target == "odo") {
            _formState.value = _formState.value.copy(odoBitmap = null, odoBase64 = "")
        } else {
            _formState.value = _formState.value.copy(receiptBitmap = null, receiptBase64 = "")
        }
    }

    fun submitLog() {
        val form = _formState.value
        val session = _session.value ?: return

        if (form.selectedBus.isBlank()) {
            _formState.value = form.copy(statusMessage = "Select a bus first.", isSuccessStatus = false)
            return
        }
        if (form.odoReading.isBlank()) {
            _formState.value = form.copy(statusMessage = "Odometer reading is required.", isSuccessStatus = false)
            return
        }
        if (form.odoBase64.isBlank()) {
            _formState.value = form.copy(statusMessage = "Camera photo of the odometer is required.", isSuccessStatus = false)
            return
        }

        val odo = form.odoReading.toDoubleOrNull()
        if (odo == null || odo < 0) {
            _formState.value = form.copy(statusMessage = "Enter a valid odometer reading.", isSuccessStatus = false)
            return
        }

        // Check if odometer is lower than last recorded reading for this bus
        val lastOdo = lastOdoMap.value[form.selectedBus] ?: 0.0
        if (lastOdo > 0 && odo < lastOdo) {
            _formState.value = form.copy(
                statusMessage = "Odometer ($odo km) is lower than the last reading for ${form.selectedBus} ($lastOdo km).",
                isSuccessStatus = false
            )
            return
        }

        // For expenses, amount and receipt photo are required
        if (!form.category.isTrip) {
            val amount = form.expenseAmount.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                _formState.value = form.copy(statusMessage = "Expenses need a valid amount (AED).", isSuccessStatus = false)
                return
            }
            if (form.receiptBase64.isBlank()) {
                _formState.value = form.copy(statusMessage = "Expenses need a camera photo of the receipt.", isSuccessStatus = false)
                return
            }
        }

        // Check if difference > 1000 km -> Show confirmation dialog
        if (lastOdo > 0 && (odo - lastOdo) > 1000) {
            _formState.value = form.copy(
                showLargeOdoDialog = true,
                pendingSubmitOdoDiff = odo - lastOdo
            )
            return
        }

        executeLogSubmission()
    }

    fun confirmLargeOdoSubmit() {
        _formState.value = _formState.value.copy(showLargeOdoDialog = false)
        executeLogSubmission()
    }

    fun cancelLargeOdoSubmit() {
        _formState.value = _formState.value.copy(showLargeOdoDialog = false)
    }

    private fun executeLogSubmission() {
        val form = _formState.value
        val session = _session.value ?: return
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val capturedAt = isoFormat.format(Date())

        viewModelScope.launch {
            _formState.value = form.copy(isSubmitting = true, statusMessage = "Uploading…")

            val result = repository.submitLog(
                session = session,
                busNumber = form.selectedBus,
                category = form.category.displayName,
                odoReading = form.odoReading,
                expenseQty = if (form.category.isTrip) "" else form.expenseQty,
                expenseAmount = if (form.category.isTrip) "" else form.expenseAmount,
                location = gpsLocation.value,
                capturedAt = capturedAt,
                odoPhotoBase64 = form.odoBase64,
                receiptPhotoBase64 = if (form.category.isTrip) "" else form.receiptBase64
            )

            when (result) {
                is SubmitResult.Success -> {
                    _lastOdoMap.value = repository.preferences.getLastOdoMap()
                    _formState.value = LogFormUiState(
                        selectedBus = form.selectedBus,
                        category = form.category,
                        statusMessage = "✅ Log saved successfully",
                        isSuccessStatus = true
                    )
                }
                is SubmitResult.QueuedOffline -> {
                    _lastOdoMap.value = repository.preferences.getLastOdoMap()
                    _formState.value = LogFormUiState(
                        selectedBus = form.selectedBus,
                        category = form.category,
                        statusMessage = result.message,
                        isSuccessStatus = true
                    )
                }
                is SubmitResult.Error -> {
                    _formState.value = form.copy(
                        isSubmitting = false,
                        statusMessage = "❌ ${result.message}",
                        isSuccessStatus = false
                    )
                }
            }
        }
    }

    fun resetForm() {
        val currentBus = _formState.value.selectedBus
        _formState.value = LogFormUiState(selectedBus = currentBus)
    }

    fun syncPendingLogs() {
        val session = _session.value ?: return
        if (_isSyncing.value) return

        viewModelScope.launch {
            _isSyncing.value = true
            val sent = repository.flushPendingQueue(session)
            if (sent > 0) {
                _lastOdoMap.value = repository.preferences.getLastOdoMap()
                repository.refreshHistory(session.backendUrl, session.driverId, session.pin)
            }
            _isSyncing.value = false
        }
    }

    fun deletePendingLog(qid: Long) {
        viewModelScope.launch {
            repository.deletePendingItem(qid)
        }
    }

    fun refreshHistory() {
        val session = _session.value ?: return
        viewModelScope.launch {
            repository.refreshHistory(session.backendUrl, session.driverId, session.pin)
        }
    }

    fun scanOdometerWithAi() {
        val bitmap = _formState.value.odoBitmap ?: return
        viewModelScope.launch {
            _formState.value = _formState.value.copy(
                isAiScanningOdo = true,
                statusMessage = "✨ Gemini AI analyzing odometer photo…"
            )
            val result = geminiAi.scanOdometer(bitmap)
            result.fold(
                onSuccess = { detectedKm ->
                    val kmInt = detectedKm.toInt().toString()
                    _formState.value = _formState.value.copy(
                        isAiScanningOdo = false,
                        odoReading = kmInt,
                        statusMessage = "✨ AI Detected: $kmInt km",
                        isSuccessStatus = true
                    )
                },
                onFailure = { err ->
                    _formState.value = _formState.value.copy(
                        isAiScanningOdo = false,
                        statusMessage = "AI Scan: ${err.localizedMessage ?: "Could not detect numbers"}. Please type reading.",
                        isSuccessStatus = false
                    )
                }
            )
        }
    }

    fun scanReceiptWithAi() {
        val bitmap = _formState.value.receiptBitmap ?: return
        viewModelScope.launch {
            _formState.value = _formState.value.copy(
                isAiScanningReceipt = true,
                statusMessage = "✨ Gemini AI parsing receipt details…"
            )
            val result = geminiAi.scanReceipt(bitmap)
            result.fold(
                onSuccess = { res ->
                    val currentForm = _formState.value
                    _formState.value = currentForm.copy(
                        isAiScanningReceipt = false,
                        expenseAmount = res.amount?.toString() ?: currentForm.expenseAmount,
                        expenseQty = res.quantity?.toString() ?: currentForm.expenseQty,
                        statusMessage = "✨ AI Parsed: ${res.amount?.let { "AED $it" } ?: ""} ${res.vendor ?: ""}".trim(),
                        isSuccessStatus = true
                    )
                },
                onFailure = { err ->
                    _formState.value = _formState.value.copy(
                        isAiScanningReceipt = false,
                        statusMessage = "AI Scan: ${err.localizedMessage ?: "Could not parse receipt"}",
                        isSuccessStatus = false
                    )
                }
            )
        }
    }

    fun askFleetCopilot(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _isCopilotLoading.value = true
            _copilotResponse.value = null
            val result = geminiAi.askFleetCopilot(question)
            result.fold(
                onSuccess = { answer ->
                    _copilotResponse.value = answer
                    _isCopilotLoading.value = false
                },
                onFailure = { err ->
                    _copilotResponse.value = "Copilot unavailable: ${err.localizedMessage}"
                    _isCopilotLoading.value = false
                }
            )
        }
    }

    fun clearCopilotResponse() {
        _copilotResponse.value = null
    }

    fun testReminderNotification() {
        reminderScheduler.showReminderNotification(
            title = "FleetOps Reminder",
            message = "Time to update your log (odometer / trip / expenses)."
        )
    }
}
