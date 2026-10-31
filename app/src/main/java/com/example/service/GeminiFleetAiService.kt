package com.example.service

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class ReceiptAiScanResult(
    val amount: Double?,
    val quantity: Double?,
    val vendor: String?,
    val detectedCategory: String?
)

class GeminiFleetAiService {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.2f
            }
        )
    }

    suspend fun scanOdometer(bitmap: Bitmap): Result<Double> = withContext(Dispatchers.IO) {
        try {
            withTimeout(45_000L) {
                val prompt = "Look at this vehicle dashboard photo. Find the current odometer mileage reading in kilometers. " +
                        "Return ONLY a JSON object in this format: {\"odometer\": 124567.0}. No markdown formatting, no other text."

                val response = model.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                val text = response.text?.trim() ?: throw IllegalStateException("No text response from Gemini")
                val cleanJson = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(cleanJson)
                val odo = json.optDouble("odometer", -1.0)
                if (odo >= 0) {
                    Result.success(odo)
                } else {
                    Result.failure(Exception("Could not detect clear odometer numbers in photo"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanReceipt(bitmap: Bitmap): Result<ReceiptAiScanResult> = withContext(Dispatchers.IO) {
        try {
            withTimeout(45_000L) {
                val prompt = "Look at this expense receipt/invoice. Extract: " +
                        "1) Total amount in AED or local currency, 2) Quantity in litres if fuel, 3) Vendor or fuel station name, 4) Category (Fuel, Maintenance, or Other Expense). " +
                        "Return ONLY a JSON object in this format: {\"amount\": 250.50, \"quantity\": 65.2, \"vendor\": \"ENOC Petrol Station\", \"category\": \"Fuel\"}. No markdown formatting, no other text."

                val response = model.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                val text = response.text?.trim() ?: throw IllegalStateException("No text response from Gemini")
                val cleanJson = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(cleanJson)
                val amount = if (json.has("amount") && !json.isNull("amount")) json.optDouble("amount") else null
                val qty = if (json.has("quantity") && !json.isNull("quantity")) json.optDouble("quantity") else null
                val vendor = json.optString("vendor", "").ifEmpty { null }
                val cat = json.optString("category", "").ifEmpty { null }

                Result.success(
                    ReceiptAiScanResult(
                        amount = amount,
                        quantity = qty,
                        vendor = vendor,
                        detectedCategory = cat
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun askFleetCopilot(question: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            withTimeout(45_000L) {
                val prompt = "You are FleetOps Copilot, an expert AI assistant for commercial bus and fleet drivers. " +
                        "Provide a clear, practical, concise answer for drivers. Question: $question"

                val response = model.generateContent(prompt)
                val answer = response.text?.trim() ?: "No response received"
                Result.success(answer)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
