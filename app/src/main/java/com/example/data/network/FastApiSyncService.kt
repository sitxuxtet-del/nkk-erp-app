package com.example.data.network

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ApiOrderSyncRequest(
    val orderId: String,
    val customerName: String?,
    val cashierName: String,
    val grandTotal: Double,
    val paymentMethod: String,
    val timestamp: Long,
    val items: List<ApiOrderItem>
)

@JsonClass(generateAdapter = true)
data class ApiOrderItem(
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val itemTotal: Double
)

@JsonClass(generateAdapter = true)
data class ApiSyncResponse(
    val status: String,
    val message: String,
    val syncedCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class ApiHealthResponse(
    val status: String,
    val version: String,
    val uptimeSeconds: Long = 0
)

interface FastApiSyncService {
    @GET("/api/v1/health")
    suspend fun checkHealth(): Response<ApiHealthResponse>

    @POST("/api/v1/sync/orders")
    suspend fun syncOrders(
        @Body orders: List<ApiOrderSyncRequest>
    ): Response<ApiSyncResponse>
}

object NetworkClient {
    fun createService(baseUrl: String): FastApiSyncService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(FastApiSyncService::class.java)
    }
}
