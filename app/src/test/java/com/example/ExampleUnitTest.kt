package com.example

import com.example.data.model.ApiRequest
import com.example.data.model.DriverSession
import com.example.data.model.LogCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testLogCategories() {
    assertTrue(LogCategory.TRIP_START.isTrip)
    assertTrue(LogCategory.TRIP_END.isTrip)
    assertFalse(LogCategory.FUEL.isTrip)
    assertFalse(LogCategory.MAINTENANCE.isTrip)
    assertFalse(LogCategory.OTHER_EXPENSE.isTrip)

    assertEquals(LogCategory.FUEL, LogCategory.fromDisplayName("Fuel"))
    assertEquals(LogCategory.TRIP_START, LogCategory.fromDisplayName("Trip Start"))
    assertEquals(LogCategory.MAINTENANCE, LogCategory.fromDisplayName("Maintenance"))
  }

  @Test
  fun testDriverSessionAndPayload() {
    val session = DriverSession(
      driverId = "D001",
      driverName = "Demo Driver",
      pin = "1234",
      backendUrl = "https://script.google.com/test",
      reminders = listOf("08:00", "13:00"),
      lastOdo = mapOf("BUS-01" to 14250.0)
    )

    assertEquals("D001", session.driverId)
    assertEquals(14250.0, session.lastOdo["BUS-01"] ?: 0.0, 0.01)

    val request = ApiRequest(
      action = "log",
      driverId = session.driverId,
      pin = session.pin,
      busNumber = "BUS-01",
      category = LogCategory.TRIP_START.displayName,
      odoReading = "14300"
    )

    assertEquals("log", request.action)
    assertEquals("BUS-01", request.busNumber)
    assertEquals("14300", request.odoReading)
  }

  @Test
  fun testRoutePointAndTripRouteState() {
    val point1 = com.example.data.model.RoutePoint(
      latitude = 25.2048,
      longitude = 55.2708,
      altitude = 15.0,
      speedMps = 10f, // 10 m/s = 36 km/h
      bearing = 90f,
      accuracy = 3.5f,
      timestamp = 1000000L
    )

    assertEquals(36f, point1.speedKmh, 0.01f)
    assertEquals(25.2048, point1.latitude, 0.0001)
    assertEquals(55.2708, point1.longitude, 0.0001)

    val point2 = com.example.data.model.RoutePoint(
      latitude = 25.2058,
      longitude = 55.2718,
      altitude = 16.0,
      speedMps = 15f, // 15 m/s = 54 km/h
      bearing = 45f,
      accuracy = 2.0f,
      timestamp = 1005000L
    )

    val tripState = com.example.data.model.TripRouteState(
      isTracking = true,
      isPaused = false,
      currentPoint = point2,
      pathPoints = listOf(point1, point2),
      totalDistanceMeters = 3450.0,
      startTimeMillis = 1000000L,
      durationSeconds = 3665L, // 1h 1m 5s
      busNumber = "BUS-01"
    )

    assertEquals(3.45, tripState.totalDistanceKm, 0.001)
    assertEquals(54f, tripState.currentSpeedKmh, 0.01f)
    assertEquals("01:01:05", tripState.formattedDuration)
    assertEquals(2, tripState.pathPoints.size)
    assertEquals("BUS-01", tripState.busNumber)
  }
}
