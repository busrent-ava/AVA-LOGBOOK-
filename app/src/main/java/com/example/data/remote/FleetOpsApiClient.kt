package com.example.data.remote

import com.example.data.model.ApiRequest
import com.example.data.model.BusesApiResponse
import com.example.data.model.LogSubmitApiResponse
import com.example.data.model.LoginApiResponse
import com.example.data.model.MyLogsApiResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class FleetOpsApiClient {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(35, TimeUnit.SECONDS)
        .build()

    private val mediaType = "text/plain; charset=utf-8".toMediaType()

    suspend fun login(url: String, driverId: String, pin: String): LoginApiResponse = withContext(Dispatchers.IO) {
        val requestPayload = ApiRequest(
            action = "login",
            driverId = driverId,
            pin = pin
        )
        val json = moshi.adapter(ApiRequest::class.java).toJson(requestPayload)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IllegalStateException("Empty response from backend")
            val adapter = moshi.adapter(LoginApiResponse::class.java)
            adapter.fromJson(body) ?: throw IllegalStateException("Failed to parse login response")
        }
    }

    suspend fun getBuses(url: String): BusesApiResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IllegalStateException("Empty response from backend")
            val adapter = moshi.adapter(BusesApiResponse::class.java)
            adapter.fromJson(body) ?: throw IllegalStateException("Failed to parse buses response")
        }
    }

    suspend fun getMyLogs(url: String, driverId: String, pin: String): MyLogsApiResponse = withContext(Dispatchers.IO) {
        val requestPayload = ApiRequest(
            action = "myLogs",
            driverId = driverId,
            pin = pin
        )
        val json = moshi.adapter(ApiRequest::class.java).toJson(requestPayload)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IllegalStateException("Empty response from backend")
            val adapter = moshi.adapter(MyLogsApiResponse::class.java)
            adapter.fromJson(body) ?: throw IllegalStateException("Failed to parse logs response")
        }
    }

    suspend fun submitLog(url: String, requestPayload: ApiRequest): LogSubmitApiResponse = withContext(Dispatchers.IO) {
        val json = moshi.adapter(ApiRequest::class.java).toJson(requestPayload)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IllegalStateException("Empty response from backend")
            val adapter = moshi.adapter(LogSubmitApiResponse::class.java)
            adapter.fromJson(body) ?: throw IllegalStateException("Failed to parse submit response")
        }
    }
}
