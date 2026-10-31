package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DriverSession
import com.example.data.model.LogCategory
import com.example.ui.theme.FleetBluePrimary
import com.example.ui.theme.FleetError
import com.example.ui.theme.FleetSuccess
import com.example.ui.theme.FleetWarning
import com.example.ui.viewmodel.LogFormUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLogScreen(
    session: DriverSession,
    formState: LogFormUiState,
    buses: List<String>,
    lastOdoMap: Map<String, Double>,
    gpsLocation: String,
    pendingCount: Int,
    onSelectBus: (String) -> Unit,
    onSelectCategory: (LogCategory) -> Unit,
    onOdoChange: (String) -> Unit,
    onExpenseQtyChange: (String) -> Unit,
    onExpenseAmountChange: (String) -> Unit,
    onOpenCamera: (String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onScanOdometerWithAi: () -> Unit,
    onScanReceiptWithAi: () -> Unit,
    onOpenCopilot: () -> Unit,
    onGoToPending: () -> Unit,
    onSubmit: () -> Unit,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()
    var busMenuExpanded by remember { mutableStateOf(false) }

    val lastRecordedOdo = lastOdoMap[formState.selectedBus] ?: 0.0
    val currentOdoNum = formState.odoReading.toDoubleOrNull() ?: 0.0
    val odoDifference = if (currentOdoNum > 0 && lastRecordedOdo > 0) currentOdoNum - lastRecordedOdo else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Driver Header Profile Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = FleetBluePrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = session.driverName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = FleetBluePrimary,
                                fontSize = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = session.driverName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ID: ${session.driverId}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = FleetSuccess.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = FleetSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = gpsLocation,
                                        fontSize = 11.sp,
                                        color = FleetSuccess,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.testTag("driver_logout_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Log out",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // AI Copilot & Pending Queue Quick Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = FleetBluePrimary.copy(alpha = 0.12f),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenCopilot() }
                    .testTag("open_copilot_quick_bar")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = FleetBluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI Driver Copilot",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FleetBluePrimary
                    )
                }
            }

            if (pendingCount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FleetWarning.copy(alpha = 0.15f),
                    modifier = Modifier
                        .clickable { onGoToPending() }
                        .testTag("pending_queue_badge_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = FleetWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$pendingCount Waiting Sync",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FleetWarning
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Entry Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "New Fleet Log Entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Record odometer, trip state, or fuel/maintenance expenses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bus Selection
                Text(
                    text = "Select Bus",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = busMenuExpanded,
                    onExpandedChange = { busMenuExpanded = !busMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = formState.selectedBus.ifEmpty { "Select a bus…" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = busMenuExpanded) },
                        leadingIcon = {
                            Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = FleetBluePrimary)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("bus_dropdown_field")
                    )

                    ExposedDropdownMenu(
                        expanded = busMenuExpanded,
                        onDismissRequest = { busMenuExpanded = false }
                    ) {
                        buses.forEach { bus ->
                            val lastKm = lastOdoMap[bus]
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(bus, fontWeight = FontWeight.SemiBold)
                                        if (lastKm != null && lastKm > 0) {
                                            Text(
                                                "${lastKm.toInt()} km",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectBus(bus)
                                    busMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (lastRecordedOdo > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Last recorded for ${formState.selectedBus}: ${lastRecordedOdo.toInt()} km",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Category Selector
                Text(
                    text = "Log Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LogCategory.entries.take(2).forEach { cat ->
                        FilterChip(
                            selected = formState.category == cat,
                            onClick = { onSelectCategory(cat) },
                            label = { Text(cat.displayName, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (cat == LogCategory.TRIP_START) Icons.Default.DirectionsBus else Icons.Default.SportsScore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FleetBluePrimary,
                                selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                                selectedLeadingIconColor = androidx.compose.ui.graphics.Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cat_chip_${cat.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LogCategory.entries.drop(2).forEach { cat ->
                        FilterChip(
                            selected = formState.category == cat,
                            onClick = { onSelectCategory(cat) },
                            label = { Text(cat.displayName, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (cat) {
                                        LogCategory.FUEL -> Icons.Default.LocalGasStation
                                        LogCategory.MAINTENANCE -> Icons.Default.Warning
                                        else -> Icons.Default.Receipt
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FleetBluePrimary,
                                selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                                selectedLeadingIconColor = androidx.compose.ui.graphics.Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cat_chip_${cat.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Odometer Reading Input
                Text(
                    text = "Odometer Reading (km)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = formState.odoReading,
                    onValueChange = onOdoChange,
                    placeholder = { Text(if (lastRecordedOdo > 0) "e.g. ${(lastRecordedOdo + 45).toInt()}" else "e.g. 15420") },
                    leadingIcon = {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = FleetBluePrimary)
                    },
                    trailingIcon = {
                        Text(
                            "km",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FleetBluePrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("odo_reading_input")
                )

                if (odoDifference != 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (odoDifference > 0) "+${odoDifference.toInt()} km since last log" else "Warning: ${odoDifference.toInt()} km below last log!",
                        fontSize = 12.sp,
                        color = if (odoDifference > 0) FleetSuccess else FleetError,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Odometer Photo Capture Section
                Text(
                    text = "Odometer Photo (Camera Required)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (formState.odoBitmap == null) {
                    OutlinedButton(
                        onClick = { onOpenCamera("odo") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("take_odo_photo_btn")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📷 Take Odometer Photo", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // Preview card of captured odometer
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Image(
                                bitmap = formState.odoBitmap.asImageBitmap(),
                                contentDescription = "Captured Odometer Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = FleetSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Captured & Watermarked", fontSize = 12.sp, color = FleetSuccess)
                                }

                                Row {
                                    // Gemini AI Auto-detect Odometer
                                    Button(
                                        onClick = onScanOdometerWithAi,
                                        enabled = !formState.isAiScanningOdo,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FleetBluePrimary),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("ai_scan_odo_btn")
                                    ) {
                                        if (formState.isAiScanningOdo) {
                                            CircularProgressIndicator(
                                                color = androidx.compose.ui.graphics.Color.White,
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("AI OCR", fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { onRemovePhoto("odo") },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Photo",
                                            tint = FleetError,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Expense Fields (Only for Fuel, Maintenance, Other Expense)
                AnimatedVisibility(visible = !formState.category.isTrip) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (formState.category == LogCategory.FUEL) "Fuel Litres" else "Quantity / Items",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = formState.expenseQty,
                                    onValueChange = onExpenseQtyChange,
                                    placeholder = { Text(if (formState.category == LogCategory.FUEL) "e.g. 60" else "e.g. 1") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("expense_qty_input")
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Amount (AED) *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = formState.expenseAmount,
                                    onValueChange = onExpenseAmountChange,
                                    placeholder = { Text("e.g. 210.50") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("expense_amt_input")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Receipt Photo Section
                        Text(
                            text = "Receipt Photo (Camera Required for Expenses)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (formState.receiptBitmap == null) {
                            OutlinedButton(
                                onClick = { onOpenCamera("rc") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("take_receipt_photo_btn")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("📷 Take Receipt Photo", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Image(
                                        bitmap = formState.receiptBitmap.asImageBitmap(),
                                        contentDescription = "Receipt Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Receipt Stamped & Attached", fontSize = 12.sp, color = FleetSuccess)
                                        Row {
                                            // Gemini AI parse receipt
                                            Button(
                                                onClick = onScanReceiptWithAi,
                                                enabled = !formState.isAiScanningReceipt,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = FleetBluePrimary),
                                                modifier = Modifier
                                                    .height(34.dp)
                                                    .testTag("ai_scan_receipt_btn")
                                            ) {
                                                if (formState.isAiScanningReceipt) {
                                                    CircularProgressIndicator(
                                                        color = androidx.compose.ui.graphics.Color.White,
                                                        modifier = Modifier.size(14.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("AI Parse", fontSize = 11.sp)
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            IconButton(
                                                onClick = { onRemovePhoto("rc") },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove Receipt",
                                                    tint = FleetError,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Status Message Banner
                if (!formState.statusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (formState.isSuccessStatus) FleetSuccess.copy(alpha = 0.12f) else FleetError.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (formState.isSuccessStatus) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (formState.isSuccessStatus) FleetSuccess else FleetError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formState.statusMessage,
                                color = if (formState.isSuccessStatus) FleetSuccess else FleetError,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Submit Button
                Button(
                    onClick = onSubmit,
                    enabled = !formState.isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FleetBluePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_log_entry_button")
                ) {
                    if (formState.isSubmitting) {
                        CircularProgressIndicator(
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Submitting…", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Submit Log Entry", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
