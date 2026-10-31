package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.FleetWarning

@Composable
fun ConfirmLargeOdoDialog(
    busNumber: String,
    odoDiff: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = FleetWarning
            )
        },
        title = {
            Text(text = "High Mileage Increase")
        },
        text = {
            Text(
                text = "That is ${odoDiff.toInt()} km more than the last recorded reading for $busNumber. Please confirm if this reading is correct."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_large_odo_button")
            ) {
                Text("Yes, Correct")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_large_odo_button")
            ) {
                Text("Check Number")
            }
        }
    )
}
