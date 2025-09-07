package com.gptoss.chat.api

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// Data classes for OpenRouter API
data class OpenRouterApiRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val max_tokens: Int,
    val temperature: Double
)

data class ApiMessage(
    val role: String,
    val content: String
)

data class OpenRouterApiResponse(
    val choices: List<ApiChoice>
)

data class ApiChoice(
    val message: ApiResponseMessage
)

data class ApiResponseMessage(
    val content: String
)


class ApiClient {
    companion object {
        private const val OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"
        private const val API_KEY = "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"
        private const val GRADIO_BASE_URL = "https://amd-gpt-oss-120b-chatbot.hf.space/"
        private const val TAG = "ApiClient"
    }

    private val gson = Gson()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun sendMessage(
        message: String,
        systemPrompt: String = "أنت مساعد عربي ذكي، أجب بدقة واختصار.",
        temperature: Double = 0.8
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending message to OpenRouter API: $message")

            val requestPayload = OpenRouterApiRequest(
                model = "openai/gpt-oss-120b",
                messages = listOf(
                    ApiMessage("system", systemPrompt),
                    ApiMessage("user", message)
                ),
                max_tokens = 250,
                temperature = temperature
            )

            val jsonPayload = gson.toJson(requestPayload)
            val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(OPENROUTER_API_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Response code: ${response.code}")
            Log.d(TAG, "Response body: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                val openRouterResponse = gson.fromJson(responseBody, OpenRouterApiResponse::class.java)
                val content = openRouterResponse.choices.firstOrNull()?.message?.content
                return@withContext content ?: "No response content found."
            } else {
                throw ApiException("OpenRouter API request failed: ${response.code} - $responseBody", response.code)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Network error", e)
            throw NetworkException("Network error: ${e.message}", e)
        }
    }

}

class ApiException(message: String, val code: Int) : Exception(message)
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
class NoInternetException(message: String) : Exception(message)
class ServerTimeoutException(message: String) : Exception(message)
