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
}
