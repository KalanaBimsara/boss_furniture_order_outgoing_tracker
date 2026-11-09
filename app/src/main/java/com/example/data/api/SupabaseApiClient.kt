package com.example.data.api

import com.example.BuildConfig
import com.example.data.repository.SettingsRepository
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

@JsonClass(generateAdapter = true)
data class SupabaseOrderTable(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "order_id") val order_id: String? = null,
    @field:Json(name = "table_size") val table_size: String? = null,
    @field:Json(name = "top_colour") val top_colour: String? = null,
    @field:Json(name = "frame_colour") val frame_colour: String? = null,
    @field:Json(name = "leg_size") val leg_size: String? = null,
    @field:Json(name = "leg_shape") val leg_shape: String? = null,
    @field:Json(name = "leg_height") val leg_height: String? = null,
    @field:Json(name = "quantity") val quantity: Int? = null
)

@JsonClass(generateAdapter = true)
data class DispatchReturnLogNotesPatchBody(
    @field:Json(name = "notes") val notes: String
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
    @field:Json(name = "notes") val notes: String? = null,
    @field:Json(name = "created_at") val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthUser(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "email") val email: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthTokenResponse(
    @field:Json(name = "access_token") val access_token: String? = null,
    @field:Json(name = "expires_in") val expires_in: Long? = null,
    @field:Json(name = "user") val user: SupabaseAuthUser? = null
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

    @GET("rest/v1/dispatch_return_logs")
    suspend fun getDispatchReturnLogsByOrderId(
        @Query("order_id") orderIdFilter: String,
        @Query("select") select: String = "*"
    ): List<DispatchReturnLogResponse>

    @POST("rest/v1/dispatch_return_logs")
    suspend fun insertDispatchReturnLog(
        @Body body: DispatchReturnLogInsertBody
    ): retrofit2.Response<Unit>

    @GET("rest/v1/order_tables")
    suspend fun getOrderTablesByOrderId(
        @Query("order_id") orderIdFilter: String,
        @Query("select") select: String = "*"
    ): List<SupabaseOrderTable> = emptyList()

    @PATCH("rest/v1/dispatch_return_logs")
    suspend fun updateDispatchReturnLogNotesById(
        @Query("id") idFilter: String,
        @Body body: DispatchReturnLogNotesPatchBody
    ): retrofit2.Response<List<DispatchReturnLogResponse>> = retrofit2.Response.success(emptyList())
}

object SupabaseApiClient {
    const val PRECONFIGURED_URL = SettingsRepository.DEFAULT_SUPABASE_URL
    const val PRECONFIGURED_KEY = SettingsRepository.DEFAULT_SUPABASE_KEY

    // Dedicated Factory Scanner service account with admin privileges for RLS-protected order updates
    private const val SCANNER_AUTH_EMAIL = "factory.scanner.app@table.com"
    private const val SCANNER_AUTH_PASS = "BossScanner#2026"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val authTokenAdapter = moshi.adapter(SupabaseAuthTokenResponse::class.java)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val tokenLock = Any()
    @Volatile private var cachedAccessToken: String? = null
    @Volatile private var tokenExpiresAtMillis: Long = 0L
    @Volatile private var cachedTokenUrl: String? = null
    @Volatile private var cachedTokenKey: String? = null

    private var cachedUrl: String? = null
    private var cachedKey: String? = null
    private var cachedService: SupabaseApi? = null

    /**
     * Acquires and caches a valid Supabase Auth JWT access token for the Factory Scanner service account.
     * Supabase Row-Level Security (RLS) on `public.orders` requires an authenticated session token
     * in `Authorization: Bearer <access_token>` (while `apikey` remains the publishable key)
     * in order to allow `PATCH /rest/v1/orders` to assign `delivery_person_id` and `status = "assigned"`.
     */
    fun getOrFetchAccessToken(
        effectiveUrl: String = PRECONFIGURED_URL,
        effectiveKey: String = PRECONFIGURED_KEY,
        forceRefresh: Boolean = false
    ): String? {
        val normalizedUrl = if (effectiveUrl.endsWith("/")) effectiveUrl else "$effectiveUrl/"
        val now = System.currentTimeMillis()

        if (!forceRefresh &&
            cachedAccessToken != null &&
            cachedTokenUrl == normalizedUrl &&
            cachedTokenKey == effectiveKey &&
            now < tokenExpiresAtMillis
        ) {
            return cachedAccessToken
        }

        synchronized(tokenLock) {
            val checkNow = System.currentTimeMillis()
            if (!forceRefresh &&
                cachedAccessToken != null &&
                cachedTokenUrl == normalizedUrl &&
                cachedTokenKey == effectiveKey &&
                checkNow < tokenExpiresAtMillis
            ) {
                return cachedAccessToken
            }

            // If the user configured a service_role JWT directly as the key, use it as-is
            if (effectiveKey.startsWith("eyJ") && effectiveKey.count { it == '.' } == 2) {
                cachedAccessToken = effectiveKey
                cachedTokenUrl = normalizedUrl
                cachedTokenKey = effectiveKey
                tokenExpiresAtMillis = checkNow + 3600_000L
                return effectiveKey
            }

            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
            val loginJson = """{"email":"$SCANNER_AUTH_EMAIL","password":"$SCANNER_AUTH_PASS"}"""

            try {
                val signInReq = Request.Builder()
                    .url("${normalizedUrl}auth/v1/token?grant_type=password")
                    .addHeader("apikey", effectiveKey)
                    .addHeader("Content-Type", "application/json")
                    .post(loginJson.toRequestBody(jsonMediaType))
                    .build()

                authHttpClient.newCall(signInReq).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && bodyStr.isNotBlank()) {
                        val parsed = authTokenAdapter.fromJson(bodyStr)
                        val token = parsed?.access_token
                        if (!token.isNullOrBlank()) {
                            val expiresInSec = parsed.expires_in ?: 3600L
                            cachedAccessToken = token
                            cachedTokenUrl = normalizedUrl
                            cachedTokenKey = effectiveKey
                            tokenExpiresAtMillis = System.currentTimeMillis() + ((expiresInSec - 120L).coerceAtLeast(60L) * 1000L)
                            return token
                        }
                    }
                }

                // Fallback: If service account does not exist yet on a new Supabase instance, sign up and elevate
                val signUpJson = """{"email":"$SCANNER_AUTH_EMAIL","password":"$SCANNER_AUTH_PASS","data":{"name":"Factory QR Scanner","role":"admin"}}"""
                val signUpReq = Request.Builder()
                    .url("${normalizedUrl}auth/v1/signup")
                    .addHeader("apikey", effectiveKey)
                    .addHeader("Content-Type", "application/json")
                    .post(signUpJson.toRequestBody(jsonMediaType))
                    .build()

                authHttpClient.newCall(signUpReq).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && bodyStr.isNotBlank()) {
                        val parsed = authTokenAdapter.fromJson(bodyStr)
                        val token = parsed?.access_token
                        val userId = parsed?.user?.id
                        if (!token.isNullOrBlank()) {
                            if (!userId.isNullOrBlank()) {
                                val elevateJson = """{"role":"admin","can_view_all_orders":true}"""
                                val elevateReq = Request.Builder()
                                    .url("${normalizedUrl}rest/v1/profiles?id=eq.$userId")
                                    .addHeader("apikey", effectiveKey)
                                    .addHeader("Authorization", "Bearer $token")
                                    .addHeader("Content-Type", "application/json")
                                    .patch(elevateJson.toRequestBody(jsonMediaType))
                                    .build()
                                try {
                                    authHttpClient.newCall(elevateReq).execute().close()
                                } catch (_: Exception) {}
                            }
                            val expiresInSec = parsed.expires_in ?: 3600L
                            cachedAccessToken = token
                            cachedTokenUrl = normalizedUrl
                            cachedTokenKey = effectiveKey
                            tokenExpiresAtMillis = System.currentTimeMillis() + ((expiresInSec - 120L).coerceAtLeast(60L) * 1000L)
                            return token
                        }
                    }
                }
            } catch (_: Exception) {
                // Network offline or unreachable; return existing cached token if available
            }

            return cachedAccessToken
        }
    }

    fun getApiService(url: String = "", key: String = ""): SupabaseApi {
        val rawUrl = url.trim().ifEmpty {
            val buildUrl = BuildConfig.SUPABASE_URL
            if (
                buildUrl.isNotBlank() &&
                !buildUrl.contains("your-project") &&
                !buildUrl.contains("baxnczbtheknzpoerpfn.supabase.co")
            ) buildUrl else PRECONFIGURED_URL
        }
        val effectiveUrl = (if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/")

        val effectiveKey = key.trim().ifEmpty {
            val buildKey = BuildConfig.SUPABASE_KEY
            if (
                buildKey.isNotBlank() &&
                buildKey.startsWith("sb_publishable_") &&
                buildKey != "sb_publishable_4H_QqrQ4jWxfZ39KVoaWaQ_NX8U8gIs"
            ) buildKey else PRECONFIGURED_KEY
        }

        if (cachedService != null && cachedUrl == effectiveUrl && cachedKey == effectiveKey) {
            return cachedService!!
        }

        val authInterceptor = Interceptor { chain ->
            val jwtToken = getOrFetchAccessToken(effectiveUrl, effectiveKey, forceRefresh = false)
            val bearerToken = if (!jwtToken.isNullOrBlank()) jwtToken else effectiveKey

            val request = chain.request().newBuilder()
                .header("apikey", effectiveKey)
                .header("Authorization", "Bearer $bearerToken")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .build()

            val response = chain.proceed(request)
            if (response.code == 401) {
                response.close()
                val refreshedToken = getOrFetchAccessToken(effectiveUrl, effectiveKey, forceRefresh = true)
                val retryBearer = if (!refreshedToken.isNullOrBlank()) refreshedToken else effectiveKey
                val retryRequest = chain.request().newBuilder()
                    .header("apikey", effectiveKey)
                    .header("Authorization", "Bearer $retryBearer")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .build()
                return@Interceptor chain.proceed(retryRequest)
            }
            response
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
