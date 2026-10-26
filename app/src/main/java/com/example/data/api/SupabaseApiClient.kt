package com.example.data.api

import com.example.BuildConfig
import com.example.data.repository.SettingsRepository
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ProfileResponse(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String? = null,
    @field:Json(name = "role") val role: String? = null
)

/**
 * Strictly restricted to driver assignment and order status fields in `public.orders`.
 * Never modifies `note` or any other order columns.
 */
@JsonClass(generateAdapter = true)
data class OrderPatchBody(
    @field:Json(name = "delivery_person_id") val delivery_person_id: String? = null,
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "delivery_status") val delivery_status: String? = null,
    @field:Json(name = "updated_at") val updated_at: String? = null
)

/**
 * Matches exact columns of `public.orders` table in Supabase for reading order details.
 */
@JsonClass(generateAdapter = true)
data class SupabaseOrder(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "order_form_number") val order_form_number: String? = null,
    @field:Json(name = "customer_name") val customer_name: String? = null,
    @field:Json(name = "address") val address: String? = null,
    @field:Json(name = "contact_number") val contact_number: String? = null,
    @field:Json(name = "table_size") val table_size: String? = null,
    @field:Json(name = "colour") val colour: String? = null,
    @field:Json(name = "quantity") val quantity: Int? = null,
    @field:Json(name = "note") val note: String? = null,
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "delivery_status") val delivery_status: String? = null,
    @field:Json(name = "delivery_person_id") val delivery_person_id: String? = null,
    @field:Json(name = "sales_person_name") val sales_person_name: String? = null,
    @field:Json(name = "delivery_date") val delivery_date: String? = null,
    @field:Json(name = "customer_district") val customer_district: String? = null,
    @field:Json(name = "delivery_type") val delivery_type: String? = null,
    @field:Json(name = "updated_at") val updated_at: String? = null,
    @field:Json(name = "created_at") val created_at: String? = null
)

/**
 * Matches `public.transport` table in Supabase:
 * id, order_id, transport_mode, loaded_at, loaded_by, copy_name, transport_status
 */
@JsonClass(generateAdapter = true)
data class TransportRecord(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "order_id") val order_id: String? = null,
    @field:Json(name = "transport_mode") val transport_mode: String? = null,
    @field:Json(name = "loaded_at") val loaded_at: String? = null,
    @field:Json(name = "loaded_by") val loaded_by: String? = null,
    @field:Json(name = "copy_name") val copy_name: String? = null,
    @field:Json(name = "transport_status") val transport_status: String? = null
)

@JsonClass(generateAdapter = true)
data class TransportInsertBody(
    @field:Json(name = "order_id") val order_id: String,
    @field:Json(name = "transport_mode") val transport_mode: String,
    @field:Json(name = "loaded_at") val loaded_at: String? = null,
    @field:Json(name = "loaded_by") val loaded_by: String? = null,
    @field:Json(name = "copy_name") val copy_name: String? = null,
    @field:Json(name = "transport_status") val transport_status: String? = null
)

@JsonClass(generateAdapter = true)
data class TransportPatchBody(
    @field:Json(name = "transport_mode") val transport_mode: String,
    @field:Json(name = "loaded_at") val loaded_at: String? = null,
    @field:Json(name = "loaded_by") val loaded_by: String? = null,
    @field:Json(name = "copy_name") val copy_name: String? = null,
    @field:Json(name = "transport_status") val transport_status: String? = null
)

/**
 * Matches optional `public.dispatch_return_logs` table in Supabase for granular scan/return audit tracking.
 */
@JsonClass(generateAdapter = true)
data class DispatchReturnLogInsertBody(
    @field:Json(name = "order_id") val order_id: String? = null,
    @field:Json(name = "order_form_number") val order_form_number: String,
    @field:Json(name = "operation_mode") val operation_mode: String,
    @field:Json(name = "vehicle_number") val vehicle_number: String,
    @field:Json(name = "scanner_name") val scanner_name: String,
    @field:Json(name = "device_id") val device_id: String,
    @field:Json(name = "restocked") val restocked: Boolean = false,
    @field:Json(name = "copy_type") val copy_type: String? = null,
    @field:Json(name = "customer_name") val customer_name: String? = null,
    @field:Json(name = "customer_district") val customer_district: String? = null,
    @field:Json(name = "table_size") val table_size: String? = null,
    @field:Json(name = "colour") val colour: String? = null,
    @field:Json(name = "quantity") val quantity: Int = 1,
    @field:Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class DispatchReturnLogResponse(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "order_id") val order_id: String? = null,
    @field:Json(name = "order_form_number") val order_form_number: String? = null,
    @field:Json(name = "operation_mode") val operation_mode: String? = null,
    @field:Json(name = "vehicle_number") val vehicle_number: String? = null,
    @field:Json(name = "scanner_name") val scanner_name: String? = null,
    @field:Json(name = "device_id") val device_id: String? = null,
    @field:Json(name = "restocked") val restocked: Boolean? = null,
    @field:Json(name = "copy_type") val copy_type: String? = null,
    @field:Json(name = "customer_name") val customer_name: String? = null,
    @field:Json(name = "customer_district") val customer_district: String? = null,
    @field:Json(name = "table_size") val table_size: String? = null,
    @field:Json(name = "colour") val colour: String? = null,
    @field:Json(name = "quantity") val quantity: Int? = null,
    @field:Json(name = "created_at") val created_at: String? = null
)

interface SupabaseApi {
    @GET("rest/v1/profiles")
    suspend fun getProfileByName(
        @Query("name") name: String,
        @Query("select") select: String = "id,name,role"
    ): List<ProfileResponse>

    @GET("rest/v1/orders")
    suspend fun getOrderByOrderNumber(
        @Query("order_form_number") orderFormNumber: String,
        @Query("select") select: String = "*"
    ): List<SupabaseOrder>

    @GET("rest/v1/orders")
    suspend fun getOrderById(
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): List<SupabaseOrder>

    @GET("rest/v1/orders")
    suspend fun getRecentOrders(
        @Query("limit") limit: Int = 50,
        @Query("order") order: String = "updated_at.desc",
        @Query("select") select: String = "*"
    ): List<SupabaseOrder>

    @PATCH("rest/v1/orders")
    suspend fun updateOrderStatus(
        @Query("order_form_number") orderFormNumber: String,
        @Body body: OrderPatchBody
    ): retrofit2.Response<List<SupabaseOrder>>

    @PATCH("rest/v1/orders")
    suspend fun updateOrderStatusById(
        @Query("id") idFilter: String,
        @Body body: OrderPatchBody
    ): retrofit2.Response<List<SupabaseOrder>>

    @GET("rest/v1/transport")
    suspend fun getTransportByOrderId(
        @Query("order_id") orderIdFilter: String,
        @Query("select") select: String = "*"
    ): List<TransportRecord>

    @GET("rest/v1/transport")
    suspend fun getRecentTransports(
        @Query("limit") limit: Int = 100,
        @Query("order") order: String = "loaded_at.desc",
        @Query("select") select: String = "*"
    ): List<TransportRecord>

    @POST("rest/v1/transport")
    suspend fun insertTransport(
        @Body body: TransportInsertBody
    ): retrofit2.Response<List<TransportRecord>>

    @PATCH("rest/v1/transport")
    suspend fun updateTransportById(
        @Query("id") idFilter: String,
        @Body body: TransportPatchBody
    ): retrofit2.Response<List<TransportRecord>>

    @PATCH("rest/v1/transport")
    suspend fun updateTransportByOrderId(
        @Query("order_id") orderIdFilter: String,
        @Body body: TransportPatchBody
    ): retrofit2.Response<List<TransportRecord>>

    @GET("rest/v1/dispatch_return_logs")
    suspend fun getRecentDispatchReturnLogs(
        @Query("limit") limit: Int = 100,
        @Query("order") order: String = "created_at.desc",
        @Query("select") select: String = "*"
    ): List<DispatchReturnLogResponse>

    @POST("rest/v1/dispatch_return_logs")
    suspend fun insertDispatchReturnLog(
        @Body body: DispatchReturnLogInsertBody
    ): retrofit2.Response<Unit>
}

object SupabaseApiClient {
    const val PRECONFIGURED_URL = SettingsRepository.DEFAULT_SUPABASE_URL
    const val PRECONFIGURED_KEY = SettingsRepository.DEFAULT_SUPABASE_KEY

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private var cachedUrl: String? = null
    private var cachedKey: String? = null
    private var cachedService: SupabaseApi? = null

    fun getApiService(url: String = "", key: String = ""): SupabaseApi {
        val rawUrl = url.trim().ifEmpty {
            val buildUrl = BuildConfig.SUPABASE_URL
            if (buildUrl.isNotBlank() && !buildUrl.contains("your-project")) buildUrl else PRECONFIGURED_URL
        }
        val effectiveUrl = (if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/")

        val effectiveKey = key.trim().ifEmpty {
            val buildKey = BuildConfig.SUPABASE_KEY
            if (buildKey.isNotBlank() && !buildKey.contains("your-supabase")) buildKey else PRECONFIGURED_KEY
        }

        if (cachedService != null && cachedUrl == effectiveUrl && cachedKey == effectiveKey) {
            return cachedService!!
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("apikey", effectiveKey)
                .addHeader("Authorization", "Bearer $effectiveKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()
            chain.proceed(request)
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        val service = Retrofit.Builder()
            .baseUrl(effectiveUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApi::class.java)

        cachedUrl = effectiveUrl
        cachedKey = effectiveKey
        cachedService = service
        return service
    }

    val apiService: SupabaseApi
        get() = getApiService()
}
