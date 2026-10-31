package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fleetops_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BACKEND_URL = "fo_api_url"
        private const val KEY_SAVED_DRIVER_ID = "fo_driver_id"
        private const val KEY_SAVED_DRIVER_NAME = "fo_driver_name"
        private const val KEY_SAVED_PIN = "fo_pin"
        private const val KEY_LAST_ODO_JSON = "fo_last_odo_json"
        private const val KEY_REMINDERS_CSV = "fo_reminders_csv"
        private const val KEY_BUSES_CSV = "fo_buses_csv"
        
        const val DEFAULT_DEMO_URL = "https://script.google.com/macros/s/AKfycbz_FLEETOPS_PRO_DEMO/exec"
    }

    var backendUrl: String
        get() = prefs.getString(KEY_BACKEND_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BACKEND_URL, value).apply()

    var savedDriverId: String
        get() = prefs.getString(KEY_SAVED_DRIVER_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SAVED_DRIVER_ID, value).apply()

    var savedDriverName: String
        get() = prefs.getString(KEY_SAVED_DRIVER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SAVED_DRIVER_NAME, value).apply()

    var savedPin: String
        get() = prefs.getString(KEY_SAVED_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SAVED_PIN, value).apply()

    fun getLastOdoMap(): Map<String, Double> {
        val jsonStr = prefs.getString(KEY_LAST_ODO_JSON, "{}") ?: "{}"
        val map = mutableMapOf<String, Double>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optDouble(key, 0.0)
            }
        } catch (_: Exception) {}
        return map
    }

    fun saveLastOdoMap(map: Map<String, Double>) {
        val current = getLastOdoMap().toMutableMap()
        current.putAll(map)
        val json = JSONObject()
        current.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString(KEY_LAST_ODO_JSON, json.toString()).apply()
    }

    fun updateLastOdoForBus(busNumber: String, odo: Double) {
        val map = getLastOdoMap().toMutableMap()
        map[busNumber] = odo
        saveLastOdoMap(map)
    }

    fun getReminders(): List<String> {
        val csv = prefs.getString(KEY_REMINDERS_CSV, "") ?: ""
        if (csv.isBlank()) return emptyList()
        return csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun saveReminders(reminders: List<String>) {
        prefs.edit().putString(KEY_REMINDERS_CSV, reminders.joinToString(",")).apply()
    }

    fun getCachedBuses(): List<String> {
        val csv = prefs.getString(KEY_BUSES_CSV, "") ?: ""
        if (csv.isBlank()) return listOf("BUS-01", "BUS-02", "BUS-03", "BUS-04", "BUS-05")
        return csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun saveCachedBuses(buses: List<String>) {
        prefs.edit().putString(KEY_BUSES_CSV, buses.joinToString(",")).apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_SAVED_PIN)
            .apply()
    }
}
