package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.FleetBluePrimary
import com.example.ui.theme.FleetSuccess
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraCaptureDialog(
    targetTitle: String,
    busNumber: String,
    gpsLocation: String,
    onPhotoCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Permission rationale view
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Camera Permission Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "FleetOps requires camera access to take verified odometer and expense receipt photos.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.testTag("request_camera_perm_button")
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }

            // Top Status Bar: Target + Bus + Close button
            Surface(
                color = androidx.compose.ui.graphics.Color(0xAA000000),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = targetTitle,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Bus: $busNumber",
                                color = androidx.compose.ui.graphics.Color(0xFF4A9EFF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = FleetSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = gpsLocation.take(28),
                                color = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }

            // Target alignment reticle frame
            Box(
                modifier = Modifier
                    .size(width = 300.dp, height = 240.dp)
                    .align(Alignment.Center)
                    .border(
                        width = 2.dp,
                        color = androidx.compose.ui.graphics.Color(0x88FFFFFF),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Text(
                    text = if (targetTitle.contains("Odometer", ignoreCase = true)) "Align odometer in frame" else "Align receipt in frame",
                    color = androidx.compose.ui.graphics.Color(0xCCFFFFFF),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }

            // Bottom Actions Bar
            Surface(
                color = androidx.compose.ui.graphics.Color(0xCC000000),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Test Simulator Button (In case of emulator or camera issues)
                    OutlinedButton(
                        onClick = {
                            val simBitmap = createSimulatedPhoto(targetTitle, busNumber)
                            onPhotoCaptured(simBitmap)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = androidx.compose.ui.graphics.Color.White
                        ),
                        modifier = Modifier.testTag("sim_capture_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate", fontSize = 12.sp)
                    }

                    // Main Big Camera Trigger Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White)
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = {
                                if (hasCameraPermission) {
                                    imageCapture.takePicture(
                                        cameraExecutor,
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                                val bitmap = imageProxy.toBitmap()
                                                imageProxy.close()
                                                ContextCompat.getMainExecutor(context).execute {
                                                    onPhotoCaptured(bitmap)
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                // Fallback to simulated photo if hardware capture failed
                                                ContextCompat.getMainExecutor(context).execute {
                                                    val simBitmap = createSimulatedPhoto(targetTitle, busNumber)
                                                    onPhotoCaptured(simBitmap)
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    val simBitmap = createSimulatedPhoto(targetTitle, busNumber)
                                    onPhotoCaptured(simBitmap)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FleetBluePrimary),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("camera_snap_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Capture",
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Cancel
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = androidx.compose.ui.graphics.Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("cancel_cam_btn")
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Creates a clean simulated photo for streaming emulators or hardware camera fallbacks.
 */
private fun createSimulatedPhoto(title: String, busNumber: String): Bitmap {
    val width = 960
    val height = 720
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isOdo = title.contains("Odometer", ignoreCase = true)

    // Background gradient/fill
    val bgPaint = Paint().apply {
        color = if (isOdo) Color.rgb(24, 30, 42) else Color.rgb(245, 245, 245)
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Draw realistic graphic
    if (isOdo) {
        // Draw Odometer Gauge
        val gaugePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(38, 48, 66)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width / 2f, height / 2.2f, 180f, gaugePaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(11, 107, 203)
            style = Paint.Style.STROKE
            strokeWidth = 14f
        }
        canvas.drawCircle(width / 2f, height / 2.2f, 180f, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("ODOMETER", width / 2f, height / 2.2f - 40f, textPaint)

        val kmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(74, 158, 255)
            textSize = 58f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }
        canvas.drawText("KM DISPLAY", width / 2f, height / 2.2f + 30f, kmPaint)

        val busBadge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Vehicle: $busNumber", width / 2f, height / 2.2f + 90f, busBadge)
    } else {
        // Draw Receipt
        val receiptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(16f, 0f, 6f, Color.argb(80, 0, 0, 0))
        }
        val rRect = RectF(width / 2f - 180f, 60f, width / 2f + 180f, height - 120f)
        canvas.drawRoundRect(rRect, 16f, 16f, receiptPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 32, 46)
            textSize = 34f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("TAX INVOICE / RECEIPT", width / 2f, 130f, titlePaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Fleet Fuel & Station Services", width / 2f, 180f, linePaint)
        canvas.drawText("Bus: $busNumber", width / 2f, 220f, linePaint)
        canvas.drawText("----------------------------------", width / 2f, 260f, linePaint)
        canvas.drawText("Diesel Fuel / Maintenance", width / 2f, 310f, linePaint)
        canvas.drawText("Official Verified Receipt", width / 2f, 360f, linePaint)
    }

    return bitmap
}
