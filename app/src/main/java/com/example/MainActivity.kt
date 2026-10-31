package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.configureAppCheck
import com.example.ui.components.CameraCaptureDialog
import com.example.ui.components.ConfirmLargeOdoDialog
import com.example.ui.components.ConsoleBottomSheet
import com.example.ui.components.CopilotBottomSheet
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LiveRouteScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NewLogScreen
import com.example.ui.screens.PendingQueueScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.theme.FleetDarkBg
import com.example.ui.theme.FleetOrangePrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FleetViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FleetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Mandatory App Check initialization before setContent
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: FleetViewModel) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val buses by viewModel.buses.collectAsStateWithLifecycle()
    val lastOdoMap by viewModel.lastOdoMap.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val gpsLocation by viewModel.gpsLocation.collectAsStateWithLifecycle()
    val pendingLogs by viewModel.pendingLogs.collectAsStateWithLifecycle()
    val historyLogs by viewModel.historyLogs.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val routeState by viewModel.routeState.collectAsStateWithLifecycle()
    val isSimulationActive by viewModel.isSimulationActive.collectAsStateWithLifecycle()
    val cameraTarget by viewModel.cameraTarget.collectAsStateWithLifecycle()
    val copilotResponse by viewModel.copilotResponse.collectAsStateWithLifecycle()
    val isCopilotLoading by viewModel.isCopilotLoading.collectAsStateWithLifecycle()

    var showConsoleSheet by remember { mutableStateOf(false) }

    // Request permissions (Camera, Fine Location, Notifications)
    val permissionsToRequest = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.locationTracker.startLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        multiplePermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    if (session == null) {
        LoginScreen(
            state = loginState,
            onDriverIdChange = viewModel::onDriverIdChange,
            onPinChange = viewModel::onPinChange,
            onBackendUrlChange = viewModel::onBackendUrlChange,
            onQuickDemoClick = viewModel::setQuickDemoCredentials,
            onLoginClick = viewModel::login
        )
    } else {
        val currentSession = session!!

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FleetDarkBg)
        ) {
            NewLogScreen(
                session = currentSession,
                formState = formState,
                buses = buses,
                lastOdoMap = lastOdoMap,
                gpsLocation = gpsLocation,
                recentLogs = historyLogs,
                pendingLogs = pendingLogs,
                onSelectBus = viewModel::selectBus,
                onSelectCategory = viewModel::selectCategory,
                onOdoChange = viewModel::onOdoReadingChange,
                onExpenseQtyChange = viewModel::onExpenseQtyChange,
                onExpenseAmountChange = viewModel::onExpenseAmountChange,
                onOpenCamera = viewModel::openCamera,
                onRemovePhoto = viewModel::removePhoto,
                onScanOdometerWithAi = viewModel::scanOdometerWithAi,
                onScanReceiptWithAi = viewModel::scanReceiptWithAi,
                onOpenConsole = { showConsoleSheet = true },
                onSubmit = viewModel::submitLog,
                onLogout = viewModel::logout
            )
        }

        // Driver Console (Live GPS Route Map, AI Copilot, Offline Sync Queue, Reminders)
        if (showConsoleSheet) {
            ConsoleBottomSheet(
                routeState = routeState,
                buses = buses,
                selectedBus = formState.selectedBus,
                isSimulationActive = isSimulationActive,
                pendingLogs = pendingLogs,
                isSyncing = isSyncing,
                reminders = reminders,
                lastOdoMap = lastOdoMap,
                copilotResponse = copilotResponse,
                isCopilotLoading = isCopilotLoading,
                onSelectBus = viewModel::selectBus,
                onStartTracking = viewModel::startRouteTracking,
                onPauseTracking = viewModel::pauseRouteTracking,
                onResumeTracking = viewModel::resumeRouteTracking,
                onStopTracking = viewModel::stopRouteTracking,
                onClearRoute = viewModel::clearRoute,
                onToggleSimulation = viewModel::toggleSimulation,
                onApplyDistanceToOdo = viewModel::applyRouteDistanceToOdometer,
                onSyncNow = viewModel::syncPendingLogs,
                onDeletePending = viewModel::deletePendingLog,
                onTestReminder = viewModel::testReminderNotification,
                onAskCopilot = viewModel::askFleetCopilot,
                onDismiss = { showConsoleSheet = false }
            )
        }

        // Camera capture modal
        if (cameraTarget != null) {
            CameraCaptureDialog(
                targetTitle = if (cameraTarget == "odo") "📷 Odometer Photo" else "🧾 Expense Receipt Photo",
                busNumber = formState.selectedBus.ifEmpty { "BUS-01" },
                gpsLocation = gpsLocation,
                onPhotoCaptured = viewModel::onPhotoCaptured,
                onDismiss = viewModel::closeCamera
            )
        }

        // Large odometer increase confirmation dialog
        if (formState.showLargeOdoDialog) {
            ConfirmLargeOdoDialog(
                busNumber = formState.selectedBus,
                odoDiff = formState.pendingSubmitOdoDiff,
                onConfirm = viewModel::confirmLargeOdoSubmit,
                onDismiss = viewModel::cancelLargeOdoSubmit
            )
        }
    }
}
