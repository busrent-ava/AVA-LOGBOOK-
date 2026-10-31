package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DriverSession
import com.example.data.model.LogCategory
import com.example.data.model.LogEntryItem
import com.example.data.model.PendingLogEntity
import com.example.ui.theme.FleetDarkBg
import com.example.ui.theme.FleetDarkCard
import com.example.ui.theme.FleetDarkCardBorder
import com.example.ui.theme.FleetDarkInput
import com.example.ui.theme.FleetDarkInputBorder
import com.example.ui.theme.FleetDarkSubtext
import com.example.ui.theme.FleetOrangeContainer
import com.example.ui.theme.FleetOrangePrimary
import com.example.ui.theme.FleetSuccess
import com.example.ui.theme.FleetWarning
import com.example.ui.viewmodel.LogFormUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Driver Entry Screen:
 * Pixel-accurate implementation matching the FleetOps Driver mobile portal design.
 * Shows:
 * 1. Top bar (Logo, FLEETOPS • DRIVER, driver/company name, Console button, Sign out)
 * 2. Top stat card (BUS: BUS-01, REMINDERS: 3/day)
 * 3. Main "New entry" card (Bus dropdown, Category chips, Odometer reading + photo, Quantity/Amount + photo, Submit entry button)
 * 4. "Recent entry" section displaying recent logged entries for the driver.
 */
@Composable
fun NewLogScreen(
    session: DriverSession,
    formState: LogFormUiState,
    buses: List<String>,
    lastOdoMap: Map<String, Double>,
    gpsLocation: String,
    recentLogs: List<LogEntryItem> = emptyList(),
    pendingLogs: List<PendingLogEntity> = emptyList(),
    onSelectBus: (String) -> Unit,
    onSelectCategory: (LogCategory) -> Unit,
    onOdoChange: (String) -> Unit,
    onExpenseQtyChange: (String) -> Unit,
    onExpenseAmountChange: (String) -> Unit,
    onOpenCamera: (String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onScanOdometerWithAi: () -> Unit,
    onScanReceiptWithAi: () -> Unit,
    onOpenConsole: () -> Unit,
    onSubmit: () -> Unit,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()
    var busDropdownExpanded by remember { mutableStateOf(false) }

    val activeBus = formState.selectedBus.ifEmpty { buses.firstOrNull() ?: "BUS-01" }
    val currentDateStr = remember {
        SimpleDateFormat("dd MMM", Locale.US).format(Date()).uppercase()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FleetDarkBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. Top Bar (Logo, Name, Console, Sign out)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Orange Icon & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(FleetOrangePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = "FleetOps",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Text(
                        text = "FLEETOPS  •  DRIVER",
                        color = FleetDarkSubtext,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = session.driverName.ifEmpty { "avauaebuses" },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right: Console button & Sign out button
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Console Pill Button
                Surface(
                    onClick = onOpenConsole,
                    shape = RoundedCornerShape(20.dp),
                    color = FleetDarkCard,
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF2E384D))),
                    modifier = Modifier.testTag("btn_open_console")
                ) {
                    Text(
                        text = "Console",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Sign Out Button
                Surface(
                    onClick = onLogout,
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    modifier = Modifier.testTag("btn_driver_signout")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out",
                            tint = FleetDarkSubtext,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Sign out",
                            color = FleetDarkSubtext,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 2. Top Stat Header Card (BUS | REMINDERS)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = FleetDarkCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(FleetDarkCardBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Stat: BUS
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BUS",
                        color = FleetDarkSubtext,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = activeBus,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(Color(0xFF212A38))
                )

                // Right Stat: REMINDERS
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "REMINDERS",
                        color = FleetDarkSubtext,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${session.reminders.size.coerceAtLeast(3)}/day",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 3. Main "New entry" Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = FleetDarkCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(FleetDarkCardBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Card Title & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New entry",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentDateStr,
                        color = FleetDarkSubtext,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                // BUS Label & Dropdown
                Text(
                    text = "BUS",
                    color = FleetDarkSubtext,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FleetDarkInput)
                            .border(1.dp, FleetDarkInputBorder, RoundedCornerShape(10.dp))
                            .clickable { busDropdownExpanded = true }
                            .padding(horizontal = 14.dp)
                            .testTag("bus_selector_dropdown"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = activeBus,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Bus",
                            tint = FleetDarkSubtext,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = busDropdownExpanded,
                        onDismissRequest = { busDropdownExpanded = false },
                        modifier = Modifier
                            .background(FleetDarkCard)
                            .border(1.dp, FleetDarkCardBorder, RoundedCornerShape(8.dp))
                    ) {
                        buses.forEach { bus ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = bus,
                                        color = if (bus == activeBus) FleetOrangePrimary else Color.White,
                                        fontWeight = if (bus == activeBus) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectBus(bus)
                                    busDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // CATEGORY Label & Chips
                Text(
                    text = "CATEGORY",
                    color = FleetDarkSubtext,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(8.dp))

                // Row 1: Trip Start, Trip End, Fuel, Maintenance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryPill(
                        category = LogCategory.TRIP_START,
                        selected = formState.category == LogCategory.TRIP_START,
                        onClick = { onSelectCategory(LogCategory.TRIP_START) },
                        modifier = Modifier.weight(1f)
                    )
                    CategoryPill(
                        category = LogCategory.TRIP_END,
                        selected = formState.category == LogCategory.TRIP_END,
                        onClick = { onSelectCategory(LogCategory.TRIP_END) },
                        modifier = Modifier.weight(1f)
                    )
                    CategoryPill(
                        category = LogCategory.FUEL,
                        selected = formState.category == LogCategory.FUEL,
                        onClick = { onSelectCategory(LogCategory.FUEL) },
                        modifier = Modifier.weight(0.8f)
                    )
                    CategoryPill(
                        category = LogCategory.MAINTENANCE,
                        selected = formState.category == LogCategory.MAINTENANCE,
                        onClick = { onSelectCategory(LogCategory.MAINTENANCE) },
                        modifier = Modifier.weight(1.3f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Row 2: Other Expense
                Row {
                    CategoryPill(
                        category = LogCategory.OTHER_EXPENSE,
                        selected = formState.category == LogCategory.OTHER_EXPENSE,
                        onClick = { onSelectCategory(LogCategory.OTHER_EXPENSE) }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ODOMETER READING Label & Input Box
                Text(
                    text = "ODOMETER READING",
                    color = FleetDarkSubtext,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(FleetDarkInput)
                        .border(1.dp, FleetDarkInputBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = formState.odoReading,
                        onValueChange = onOdoChange,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(FleetOrangePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_odometer_reading"),
                        decorationBox = { innerTextField ->
                            if (formState.odoReading.isEmpty()) {
                                Text(
                                    text = "128450",
                                    color = Color(0xFF4A5568),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Optional AI OCR Scan button
                    if (formState.odoBitmap != null) {
                        IconButton(
                            onClick = onScanOdometerWithAi,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_ai_scan_odo_quick")
                        ) {
                            if (formState.isAiScanningOdo) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = FleetOrangePrimary
                                )
                            } else {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "Scan with Gemini",
                                    tint = FleetOrangePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    Text(
                        text = "km",
                        color = FleetDarkSubtext,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ODOMETER PHOTO Label & Box
                Text(
                    text = "ODOMETER PHOTO",
                    color = FleetDarkSubtext,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(6.dp))

                if (formState.odoBitmap == null) {
                    // Dashed Camera Box
                    DashedPhotoUploadBox(
                        label = "Take photo",
                        onClick = { onOpenCamera("odo") },
                        modifier = Modifier.testTag("btn_take_odo_photo")
                    )
                } else {
                    // Stamped Image Preview Card
                    CapturedPhotoPreview(
                        bitmap = formState.odoBitmap,
                        title = "Odometer Verified",
                        onRetake = { onOpenCamera("odo") },
                        onRemove = { onRemovePhoto("odo") },
                        onAiScan = onScanOdometerWithAi,
                        isAiScanning = formState.isAiScanningOdo
                    )
                }

                // If Category is Expense: Quantity & Amount + Receipt Photo
                if (!formState.category.isTrip) {
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // QUANTITY
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "QUANTITY",
                                color = FleetDarkSubtext,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(FleetDarkInput)
                                    .border(1.dp, FleetDarkInputBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = formState.expenseQty,
                                    onValueChange = onExpenseQtyChange,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    cursorBrush = SolidColor(FleetOrangePrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_expense_quantity"),
                                    decorationBox = { innerTextField ->
                                        if (formState.expenseQty.isEmpty()) {
                                            Text(
                                                text = "Litres / items",
                                                color = Color(0xFF4A5568),
                                                fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        // AMOUNT
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AMOUNT",
                                color = FleetDarkSubtext,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(FleetDarkInput)
                                    .border(1.dp, FleetDarkInputBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AED ",
                                    color = FleetDarkSubtext,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                BasicTextField(
                                    value = formState.expenseAmount,
                                    onValueChange = onExpenseAmountChange,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    cursorBrush = SolidColor(FleetOrangePrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_expense_amount"),
                                    decorationBox = { innerTextField ->
                                        if (formState.expenseAmount.isEmpty()) {
                                            Text(
                                                text = "0.00",
                                                color = Color(0xFF4A5568),
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // RECEIPT PHOTO Label & Box
                    Text(
                        text = "RECEIPT PHOTO",
                        color = FleetDarkSubtext,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(6.dp))

                    if (formState.receiptBitmap == null) {
                        DashedPhotoUploadBox(
                            label = "Take photo",
                            onClick = { onOpenCamera("receipt") },
                            modifier = Modifier.testTag("btn_take_receipt_photo")
                        )
                    } else {
                        CapturedPhotoPreview(
                            bitmap = formState.receiptBitmap,
                            title = "Receipt Verified",
                            onRetake = { onOpenCamera("receipt") },
                            onRemove = { onRemovePhoto("receipt") },
                            onAiScan = onScanReceiptWithAi,
                            isAiScanning = formState.isAiScanningReceipt
                        )
                    }
                }

                // Status banner if present
                if (formState.statusMessage != null) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        color = if (formState.isSuccessStatus) FleetSuccess.copy(alpha = 0.15f) else Color(0x33EF4444),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formState.statusMessage,
                            color = if (formState.isSuccessStatus) FleetSuccess else Color(0xFFF87171),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Submit Entry Button (Vibrant Orange Button with ↗ Arrow)
                Button(
                    onClick = onSubmit,
                    enabled = !formState.isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FleetOrangePrimary,
                        disabledContainerColor = FleetOrangePrimary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_submit_entry")
                ) {
                    if (formState.isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Submitting…",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.NorthEast,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Submit entry",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // 4. "Recent entry" Section (Requested explicitly by user)
        RecentEntriesSection(
            activeBus = activeBus,
            recentLogs = recentLogs,
            pendingLogs = pendingLogs
        )

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Category Pill Chip matching screenshot:
 * Unselected: dark surface with subtle border.
 * Selected: orange border, orange background tint, white text.
 */
@Composable
private fun CategoryPill(
    category: LogCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color(0x28FF5722) else Color(0xFF131924),
        border = BorderStrokeOrNull(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) FleetOrangePrimary else Color(0xFF242E3E)
        ),
        modifier = modifier.testTag("chip_cat_${category.name}")
    ) {
        Text(
            text = category.displayName,
            color = if (selected) Color.White else Color(0xFFCBD5E1),
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

/**
 * Dashed photo box matching `[o] Take photo` in the screenshot.
 */
@Composable
private fun DashedPhotoUploadBox(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(FleetDarkInput)
            .dashedBorder(
                width = 1.dp,
                color = Color(0xFF2E3B50),
                cornerRadius = 10.dp
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = FleetDarkSubtext,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = FleetDarkSubtext,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Captured photo preview with watermark indicator and Retake/AI actions.
 */
@Composable
private fun CapturedPhotoPreview(
    bitmap: Bitmap,
    title: String,
    onRetake: () -> Unit,
    onRemove: () -> Unit,
    onAiScan: () -> Unit,
    isAiScanning: Boolean
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = FleetDarkInput),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF212A38))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = title,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = FleetSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "GPS & Timestamp Watermarked",
                    color = FleetDarkSubtext,
                    fontSize = 10.sp
                )
            }

            // AI Scan button
            IconButton(
                onClick = onAiScan,
                modifier = Modifier.size(32.dp)
            ) {
                if (isAiScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = FleetOrangePrimary
                    )
                } else {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI Scan",
                        tint = FleetOrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Retake
            TextButton(
                onClick = onRetake,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Text("Retake", color = FleetOrangePrimary, fontSize = 12.sp)
            }

            // Remove
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * "Recent entry" section matching the user's explicit specification:
 * "Driver app only needed to see this and recent entry."
 */
@Composable
private fun RecentEntriesSection(
    activeBus: String,
    recentLogs: List<LogEntryItem>,
    pendingLogs: List<PendingLogEntity>
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FleetDarkCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(FleetDarkCardBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent entry",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                if (pendingLogs.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FleetWarning.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${pendingLogs.size} queued",
                            color = FleetWarning,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Show pending logs first if any
            if (pendingLogs.isEmpty() && recentLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent entries recorded yet.",
                        color = FleetDarkSubtext,
                        fontSize = 13.sp
                    )
                }
            } else {
                // Show offline pending logs at the top
                pendingLogs.take(2).forEach { pending ->
                    PendingLogSummaryRow(pending)
                    Spacer(Modifier.height(8.dp))
                }

                // Show verified historical logs
                val displayLogs = recentLogs.take(4)
                displayLogs.forEach { log ->
                    RecentLogSummaryRow(log)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PendingLogSummaryRow(pending: PendingLogEntity) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = FleetDarkInput,
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF2A2418))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0x33F59E0B)
                    ) {
                        Text(
                            text = pending.category,
                            color = FleetWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = pending.busNumber,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (pending.expenseAmount.isNotBlank()) {
                        "${pending.odoReading} km • AED ${pending.expenseAmount}"
                    } else {
                        "${pending.odoReading} km"
                    },
                    color = FleetDarkSubtext,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = FleetWarning.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "Waiting sync",
                    color = FleetWarning,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentLogSummaryRow(log: LogEntryItem) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = FleetDarkInput,
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(FleetDarkInputBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val catColor = when (log.category.lowercase()) {
                        "trip start", "trip end" -> Color(0xFF38BDF8)
                        "fuel" -> Color(0xFFA855F7)
                        else -> FleetOrangePrimary
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = catColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = log.category.ifEmpty { "Log" },
                            color = catColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = log.busNumber.ifEmpty { "BUS" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = buildString {
                        if (log.odoReading.isNotBlank()) append("${log.odoReading} km")
                        if (!log.expenseAmount.isNullOrBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append("AED ${log.expenseAmount}")
                        }
                    },
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FleetSuccess.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Synced",
                        color = FleetSuccess,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (log.timestamp.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = log.timestamp.take(16).replace("T", " "),
                        color = FleetDarkSubtext,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Helper to apply a dashed border using Canvas DrawScope.
 */
private fun Modifier.dashedBorder(
    width: Dp = 1.dp,
    color: Color = Color(0xFF2E384D),
    cornerRadius: Dp = 10.dp,
    dashLength: Float = 14f,
    gapLength: Float = 10f
): Modifier = this.drawBehind {
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, gapLength), 0f)
    )
    val r = cornerRadius.toPx()
    drawRoundRect(
        color = color,
        size = size,
        cornerRadius = CornerRadius(r, r),
        style = stroke
    )
}

@Composable
private fun BorderStrokeOrNull(width: Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)
