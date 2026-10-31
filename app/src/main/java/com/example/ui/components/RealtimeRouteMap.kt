package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RoutePoint
import com.example.data.model.TripRouteState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

/**
 * High-performance real-time vector route map component.
 * Uses Web Mercator projection to convert GPS lat/long to Cartesian screen coordinates.
 * Supports multi-touch pan, pinch-to-zoom, auto-follow, polyline route rendering,
 * start waypoint pin, pulsating vehicle location puck with compass heading,
 * and GPS telemetry HUD overlay.
 */
@Composable
fun RealtimeRouteMap(
    routeState: TripRouteState,
    modifier: Modifier = Modifier,
    busNumber: String = "BUS-01"
) {
    val currentPoint = routeState.currentPoint
    val pathPoints = routeState.pathPoints

    // Center coordinates (default: current point or Dubai City Center fallback)
    var centerLat by remember { mutableDoubleStateOf(currentPoint?.latitude ?: 25.20485) }
    var centerLng by remember { mutableDoubleStateOf(currentPoint?.longitude ?: 55.27078) }

    // Map viewport state
    var zoomLevel by remember { mutableFloatStateOf(15.5f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var followVehicle by remember { mutableStateOf(true) }

    // Auto-center on vehicle when followVehicle is enabled and new coordinates arrive
    LaunchedEffect(currentPoint, followVehicle) {
        if (followVehicle && currentPoint != null) {
            centerLat = currentPoint.latitude
            centerLng = currentPoint.longitude
            panOffsetX = 0f
            panOffsetY = 0f
        }
    }

    // Pulsating animation for the driver's GPS location puck
    val pulseTransition = rememberInfiniteTransition(label = "puckPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A)) // High-tech dark slate GPS cockpit background
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        // Canvas handling rendering and multi-touch gestures
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("route_map_canvas")
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // If user interacts manually, disengage auto-follow until recentered
                        followVehicle = false
                        zoomLevel = (zoomLevel * zoom).coerceIn(10f, 19f)
                        panOffsetX += pan.x
                        panOffsetY += pan.y
                    }
                }
        ) {
            val cx = screenWidth / 2f + panOffsetX
            val cy = screenHeight / 2f + panOffsetY

            // 1. Draw GPS Coordinates Grid & Map Styling
            drawMapGrid(centerLat, centerLng, zoomLevel, cx, cy, screenWidth, screenHeight)

            // 2. Draw Route Polyline
            if (pathPoints.isNotEmpty()) {
                val screenPoints = pathPoints.map { pt ->
                    projectToScreen(
                        lat = pt.latitude,
                        lng = pt.longitude,
                        centerLat = centerLat,
                        centerLng = centerLng,
                        zoom = zoomLevel,
                        cx = cx,
                        cy = cy
                    )
                }

                // A. Outer glowing aura
                val glowPath = Path().apply {
                    screenPoints.forEachIndexed { index, pt ->
                        if (index == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = glowPath,
                    color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                    style = Stroke(
                        width = 16f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // B. Core Route Path (Electric Cyan -> Fleet Blue)
                drawPath(
                    path = glowPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF00E5FF), Color(0xFF0B6BCB), Color(0xFF2563EB)),
                        start = screenPoints.first(),
                        end = screenPoints.last()
                    ),
                    style = Stroke(
                        width = 7f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // C. Breadcrumb dots along path
                screenPoints.forEachIndexed { i, pt ->
                    if (i > 0 && i % 4 == 0) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.7f),
                            radius = 3.5f,
                            center = pt
                        )
                    }
                }

                // D. Draw Start Waypoint Pin (Green circle with inner core)
                val startScreenPt = screenPoints.first()
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.35f),
                    radius = 16f,
                    center = startScreenPt
                )
                drawCircle(
                    color = Color(0xFF10B981),
                    radius = 9f,
                    center = startScreenPt
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = startScreenPt
                )
            }

            // 3. Draw Live Vehicle Location Puck
            if (currentPoint != null) {
                val vehicleScreenPt = projectToScreen(
                    lat = currentPoint.latitude,
                    lng = currentPoint.longitude,
                    centerLat = centerLat,
                    centerLng = centerLng,
                    zoom = zoomLevel,
                    cx = cx,
                    cy = cy
                )

                // A. Accuracy circle (if accuracy reported)
                if (currentPoint.accuracy > 0f) {
                    val metersPerPixel = getMetersPerPixel(currentPoint.latitude, zoomLevel)
                    val accuracyRadiusPixels = (currentPoint.accuracy / metersPerPixel.toFloat()).coerceIn(12f, 90f)
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                        radius = accuracyRadiusPixels,
                        center = vehicleScreenPt,
                        style = Fill
                    )
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                        radius = accuracyRadiusPixels,
                        center = vehicleScreenPt,
                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    )
                }

                // B. Animated pulse ring
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = pulseAlpha),
                    radius = 14f * pulseScale,
                    center = vehicleScreenPt,
                    style = Stroke(width = 2.5f)
                )

                // C. Vehicle Outer Ring & Shadow
                drawCircle(
                    color = Color(0xFF0A192F),
                    radius = 15f,
                    center = vehicleScreenPt
                )
                drawCircle(
                    color = Color.White,
                    radius = 12f,
                    center = vehicleScreenPt
                )
                drawCircle(
                    color = Color(0xFF0B6BCB),
                    radius = 9f,
                    center = vehicleScreenPt
                )

                // D. Heading Direction Wedge
                drawHeadingWedge(
                    center = vehicleScreenPt,
                    bearing = currentPoint.bearing,
                    radius = 24f,
                    color = Color(0xFF00E5FF)
                )
            }
        }

        // Top Floating Telemetry Bar
        TopTelemetryOverlay(
            routeState = routeState,
            busNumber = busNumber,
            currentPoint = currentPoint,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
        )

        // Floating Map Controls (Zoom In, Zoom Out, Recenter, Fit Route)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { zoomLevel = (zoomLevel + 1f).coerceAtMost(19f) },
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("map_zoom_in")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }

            SmallFloatingActionButton(
                onClick = { zoomLevel = (zoomLevel - 1f).coerceAtLeast(10f) },
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("map_zoom_out")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }

            if (pathPoints.size > 1) {
                SmallFloatingActionButton(
                    onClick = {
                        // Fit map to show all route points
                        val minLat = pathPoints.minOf { it.latitude }
                        val maxLat = pathPoints.maxOf { it.latitude }
                        val minLng = pathPoints.minOf { it.longitude }
                        val maxLng = pathPoints.maxOf { it.longitude }
                        centerLat = (minLat + maxLat) / 2.0
                        centerLng = (minLng + maxLng) / 2.0
                        panOffsetX = 0f
                        panOffsetY = 0f
                        followVehicle = false
                        zoomLevel = 14f
                    },
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("map_fit_route")
                ) {
                    Icon(Icons.Default.CropFree, contentDescription = "Fit Route", modifier = Modifier.size(18.dp))
                }
            }

            SmallFloatingActionButton(
                onClick = {
                    currentPoint?.let {
                        centerLat = it.latitude
                        centerLng = it.longitude
                        panOffsetX = 0f
                        panOffsetY = 0f
                        followVehicle = true
                    }
                },
                containerColor = if (followVehicle) Color(0xFF0B6BCB) else Color(0xFF1E293B),
                contentColor = if (followVehicle) Color.White else Color(0xFF94A3B8),
                modifier = Modifier
                    .size(42.dp)
                    .testTag("map_recenter")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center on Vehicle", modifier = Modifier.size(18.dp))
            }
        }

        // Bottom Left Compass & Scale Bar
        BottomMapMetrics(
            zoomLevel = zoomLevel,
            centerLat = centerLat,
            bearing = currentPoint?.bearing ?: 0f,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )
    }
}

@Composable
private fun TopTelemetryOverlay(
    routeState: TripRouteState,
    busNumber: String,
    currentPoint: RoutePoint?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bus & Status
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    routeState.isTracking && !routeState.isPaused -> Color(0xFF10B981)
                                    routeState.isPaused -> Color(0xFFF59E0B)
                                    else -> Color(0xFF94A3B8)
                                }
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = busNumber,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            routeState.isTracking && !routeState.isPaused -> "LIVE"
                            routeState.isPaused -> "PAUSED"
                            else -> "STANDBY"
                        },
                        color = when {
                            routeState.isTracking && !routeState.isPaused -> Color(0xFF10B981)
                            routeState.isPaused -> Color(0xFFF59E0B)
                            else -> Color(0xFF94A3B8)
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = if (currentPoint != null) {
                        String.format("%.4f° N, %.4f° E", currentPoint.latitude, currentPoint.longitude)
                    } else {
                        "Acquiring GPS coordinates…"
                    },
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Live Speed & Distance badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = String.format("%.0f km/h", currentPoint?.speedKmh ?: 0f),
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = String.format("%.2f km", routeState.totalDistanceKm),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = routeState.formattedDuration,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomMapMetrics(
    zoomLevel: Float,
    centerLat: Double,
    bearing: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xCC0F172A),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B))))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compass Arrow
            Icon(
                Icons.Default.Navigation,
                contentDescription = "Compass",
                tint = Color(0xFFEF4444),
                modifier = Modifier
                    .size(14.dp)
                    .rotate(bearing)
            )
            Spacer(Modifier.width(6.dp))
            val headingDirection = getCompassDirection(bearing)
            Text(
                text = "$headingDirection ${bearing.toInt()}°",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.width(10.dp))
            Text(
                text = "•",
                color = Color(0xFF64748B),
                fontSize = 10.sp
            )
            Spacer(Modifier.width(10.dp))

            // Scale representation
            val mPerPx = getMetersPerPixel(centerLat, zoomLevel)
            val scaleMeters = (mPerPx * 50).toInt()
            Text(
                text = if (scaleMeters >= 1000) "${scaleMeters / 1000} km" else "$scaleMeters m",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Draws geographic grid lines and subtle urban transit block patterns.
 */
private fun DrawScope.drawMapGrid(
    centerLat: Double,
    centerLng: Double,
    zoom: Float,
    cx: Float,
    cy: Float,
    width: Float,
    height: Float
) {
    // Grid spacing based on zoom level
    val gridDegrees = when {
        zoom >= 17f -> 0.001
        zoom >= 15f -> 0.002
        zoom >= 13f -> 0.005
        else -> 0.01
    }

    val minLat = centerLat - 0.05
    val maxLat = centerLat + 0.05
    val minLng = centerLng - 0.05
    val maxLng = centerLng + 0.05

    // Rounded start coordinates
    val startLat = (minLat / gridDegrees).toInt() * gridDegrees
    val startLng = (minLng / gridDegrees).toInt() * gridDegrees

    // Draw longitude lines (vertical)
    var curLng = startLng
    while (curLng <= maxLng) {
        val ptTop = projectToScreen(centerLat + 0.05, curLng, centerLat, centerLng, zoom, cx, cy)
        val ptBottom = projectToScreen(centerLat - 0.05, curLng, centerLat, centerLng, zoom, cx, cy)
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(ptTop.x, 0f),
            end = Offset(ptBottom.x, height),
            strokeWidth = 1f
        )
        curLng += gridDegrees
    }

    // Draw latitude lines (horizontal)
    var curLat = startLat
    while (curLat <= maxLat) {
        val ptLeft = projectToScreen(curLat, centerLng - 0.05, centerLat, centerLng, zoom, cx, cy)
        val ptRight = projectToScreen(curLat, centerLng + 0.05, centerLat, centerLng, zoom, cx, cy)
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(0f, ptLeft.y),
            end = Offset(width, ptRight.y),
            strokeWidth = 1f
        )
        curLat += gridDegrees
    }

    // Subtle transit radial accents around center
    drawCircle(
        color = Color(0xFF1E293B).copy(alpha = 0.4f),
        radius = 80f,
        center = Offset(cx, cy),
        style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f)))
    )
    drawCircle(
        color = Color(0xFF1E293B).copy(alpha = 0.25f),
        radius = 180f,
        center = Offset(cx, cy),
        style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f)))
    )
}

/**
 * Draws heading triangle wedge showing vehicle direction.
 */
private fun DrawScope.drawHeadingWedge(
    center: Offset,
    bearing: Float,
    radius: Float,
    color: Color
) {
    val bearingRad = Math.toRadians((bearing - 90.0)).toFloat()
    val tip = Offset(
        x = center.x + radius * cos(bearingRad),
        y = center.y + radius * sin(bearingRad)
    )

    val leftRad = Math.toRadians((bearing - 90.0 + 140.0)).toFloat()
    val left = Offset(
        x = center.x + (radius * 0.5f) * cos(leftRad),
        y = center.y + (radius * 0.5f) * sin(leftRad)
    )

    val rightRad = Math.toRadians((bearing - 90.0 - 140.0)).toFloat()
    val right = Offset(
        x = center.x + (radius * 0.5f) * cos(rightRad),
        y = center.y + (radius * 0.5f) * sin(rightRad)
    )

    val wedgePath = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(center.x, center.y)
        lineTo(right.x, right.y)
        close()
    }

    drawPath(
        path = wedgePath,
        color = color.copy(alpha = 0.85f),
        style = Fill
    )
}

/**
 * Standard Web Mercator Projection (EPSG:3857) converting latitude & longitude into pixel coordinates.
 */
private fun projectToScreen(
    lat: Double,
    lng: Double,
    centerLat: Double,
    centerLng: Double,
    zoom: Float,
    cx: Float,
    cy: Float
): Offset {
    val scale = (256.0 * Math.pow(2.0, zoom.toDouble())).toFloat()

    // World X and Y for target coordinate
    val targetX = (lng + 180.0) / 360.0 * scale
    val sinLat = sin(Math.toRadians(lat)).coerceIn(-0.9999, 0.9999)
    val targetY = (0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * PI)) * scale

    // World X and Y for center coordinate
    val centerX = (centerLng + 180.0) / 360.0 * scale
    val sinCenterLat = sin(Math.toRadians(centerLat)).coerceIn(-0.9999, 0.9999)
    val centerY = (0.5 - ln((1.0 + sinCenterLat) / (1.0 - sinCenterLat)) / (4.0 * PI)) * scale

    val screenX = (targetX - centerX).toFloat() + cx
    val screenY = (targetY - centerY).toFloat() + cy

    return Offset(screenX, screenY)
}

private fun getMetersPerPixel(lat: Double, zoom: Float): Double {
    return (156543.03392 * cos(Math.toRadians(lat))) / Math.pow(2.0, zoom.toDouble())
}

private fun getCompassDirection(bearing: Float): String {
    val normalized = ((bearing % 360) + 360) % 360
    return when (normalized) {
        in 337.5..360.0, in 0.0..22.5 -> "N"
        in 22.5..67.5 -> "NE"
        in 67.5..112.5 -> "E"
        in 112.5..157.5 -> "SE"
        in 157.5..202.5 -> "S"
        in 202.5..247.5 -> "SW"
        in 247.5..292.5 -> "W"
        in 292.5..337.5 -> "NW"
        else -> "N"
    }
}
