package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TripRouteState
import com.example.ui.screens.LiveRouteScreen
import com.example.ui.screens.PendingQueueScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.theme.FleetDarkBg
import com.example.ui.theme.FleetDarkCard
import com.example.ui.theme.FleetOrangePrimary

/**
 * Driver Console Bottom Sheet:
 * Hosts advanced driver tools (Live GPS Route Map, AI Copilot, Offline Sync, Shift Reminders)
 * without cluttering the primary driver screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleBottomSheet(
    routeState: TripRouteState,
    buses: List<String>,
    selectedBus: String,
    isSimulationActive: Boolean,
    pendingLogs: List<com.example.data.model.PendingLogEntity>,
    isSyncing: Boolean,
    reminders: List<String>,
    lastOdoMap: Map<String, Double>,
    copilotResponse: String?,
    isCopilotLoading: Boolean,
    onSelectBus: (String) -> Unit,
    onStartTracking: (String) -> Unit,
    onPauseTracking: () -> Unit,
    onResumeTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onClearRoute: () -> Unit,
    onToggleSimulation: (String) -> Unit,
    onApplyDistanceToOdo: (Double) -> Unit,
    onSyncNow: () -> Unit,
    onDeletePending: (Long) -> Unit,
    onTestReminder: () -> Unit,
    onAskCopilot: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedConsoleTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FleetDarkBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(FleetOrangePrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Route,
                            contentDescription = null,
                            tint = FleetOrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "FleetOps Console",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_console")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                }
            }

            // Console Tabs (Live Route Map, AI Copilot, Offline Sync, Reminders)
            val tabs = listOf("Live Route Map", "AI Copilot", "Sync Queue", "Reminders")
            ScrollableTabRow(
                selectedTabIndex = selectedConsoleTab,
                containerColor = FleetDarkCard,
                contentColor = FleetOrangePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedConsoleTab]),
                        color = FleetOrangePrimary
                    )
                },
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedConsoleTab == index,
                        onClick = { selectedConsoleTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedConsoleTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedConsoleTab == index) FleetOrangePrimary else Color(0xFF94A3B8)
                            )
                        },
                        modifier = Modifier.testTag("console_tab_$index")
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp)
            ) {
                when (selectedConsoleTab) {
                    0 -> LiveRouteScreen(
                        routeState = routeState,
                        buses = buses,
                        selectedBus = selectedBus,
                        isSimulationActive = isSimulationActive,
                        onSelectBus = onSelectBus,
                        onStartTracking = onStartTracking,
                        onPauseTracking = onPauseTracking,
                        onResumeTracking = onResumeTracking,
                        onStopTracking = onStopTracking,
                        onClearRoute = onClearRoute,
                        onToggleSimulation = onToggleSimulation,
                        onApplyDistanceToOdo = { dist ->
                            onApplyDistanceToOdo(dist)
                            onDismiss()
                        }
                    )
                    1 -> CopilotConsoleView(
                        response = copilotResponse,
                        isLoading = isCopilotLoading,
                        onAsk = onAskCopilot
                    )
                    2 -> PendingQueueScreen(
                        pendingLogs = pendingLogs,
                        isSyncing = isSyncing,
                        onSyncNow = onSyncNow,
                        onDeletePending = onDeletePending
                    )
                    3 -> RemindersScreen(
                        reminders = reminders,
                        buses = buses,
                        lastOdoMap = lastOdoMap,
                        onTestNotification = onTestReminder
                    )
                }
            }
        }
    }
}

@Composable
private fun CopilotConsoleView(
    response: String?,
    isLoading: Boolean,
    onAsk: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        CopilotBottomSheet(
            copilotResponse = response,
            isLoading = isLoading,
            onAsk = onAsk,
            onDismiss = {}
        )
    }
}
