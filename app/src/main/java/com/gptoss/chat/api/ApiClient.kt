package com.gptoss.chat.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient {
    companion object {
        private const val BASE_URL = "https://amd-gpt-oss-120b-chatbot.hf.space/"
        private const val PREDICT_ENDPOINT = "api/predict"
        private const val TAG = "ApiClient"
    }

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val gradioApiService = retrofit.create(GradioApiService::class.java)

    suspend fun sendMessage(
        message: String,
        systemPrompt: String = "You are a helpful assistant",
        temperature: Double = 0.7,
        reasoningEffort: String = "medium",
        enableBrowsing: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending message: $message")

            val request = GradioRequest.createChatRequest(
                message = message,
                systemPrompt = systemPrompt,
                temperature = temperature,
                reasoningEffort = reasoningEffort,
                enableBrowsing = enableBrowsing
            )

            val response = gradioApiService.sendMessage(PREDICT_ENDPOINT, request)

            if (response.isSuccessful) {
                val gradioResponse = response.body()
                val responseText = gradioResponse?.getResponseText() ?: "Empty response"
                Log.d(TAG, "Received response: $responseText")
                return@withContext responseText
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API Error: ${response.code()} - $errorBody")
                throw ApiException("API request failed with code: ${response.code()}", response.code())
            }

        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Network error", e)
            throw NetworkException("Network error: ${e.message}", e)
        }
    }

    suspend fun sendMessageWithFallback(message: String): String {
        return try {
            sendMessage(message)
        } catch (e: Exception) {
            Log.w(TAG, "Primary API call failed, trying fallback", e)
            sendMessageDirectHttp(message)
        }
    }

    private suspend fun sendMessageDirectHttp(message: String): String = withContext(Dispatchers.IO) {
        try {
            val requestData = mapOf(
                "fn_index" to 0,
                "data" to listOf(
                    message,
                    "You are a helpful assistant",
                    0.7,
                    "medium",
                    true
                ),
                "session_hash" to GradioRequest.generateDefaultSession()
            )

            val jsonRequest = gson.toJson(requestData)
            val requestBody = jsonRequest.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${BASE_URL}${PREDICT_ENDPOINT}")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "GPT-OSS-Chat/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val gradioResponse = gson.fromJson(responseBody, GradioResponse::class.java)
                    return@withContext gradioResponse.getResponseText()
                }
            }

            throw ApiException("Direct HTTP request failed: ${response.code}", response.code)

        } catch (e: Exception) {
            throw NetworkException("Fallback network error: ${e.message}", e)
        }
    }
}

class ApiException(message: String, val code: Int) : Exception(message)
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
