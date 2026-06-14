package com.example.data.api

import com.example.data.model.PostBody
import com.example.data.model.SyncResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface AppsScriptApi {
    @POST
    suspend fun syncData(
        @Url url: String,
        @Body body: PostBody
    ): retrofit2.Response<SyncResponse>
}

class RedirectInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        // Google Apps Script redirect code is typically 302 Found
        if (response.code == 302 || response.code == 301 || response.code == 307 || response.code == 308) {
            val redirectUrl = response.header("Location")
            if (redirectUrl != null) {
                // Rebuild request, keeping same method (POST) and same body
                val newRequest = request.newBuilder()
                    .url(redirectUrl)
                    .build()
                response.close()
                return chain.proceed(newRequest)
            }
        }
        return response
    }
}

object ApiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loginInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(RedirectInterceptor())
        .addInterceptor(loginInterceptor)
        .build()

    val apiService: AppsScriptApi = Retrofit.Builder()
        // Standard Google Apps Script base placeholder
        .baseUrl("https://script.google.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(AppsScriptApi::class.java)
}
