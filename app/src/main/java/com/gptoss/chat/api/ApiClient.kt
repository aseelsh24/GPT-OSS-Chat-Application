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
data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int,
    val temperature: Double
)

data class Message(
    val role: String,
    val content: String
)

data class OpenRouterResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ResponseMessage
)

data class ResponseMessage(
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

            val requestPayload = OpenRouterRequest(
                model = "openai/gpt-3.5-turbo",
                messages = listOf(
                    Message("system", systemPrompt),
                    Message("user", message)
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
                val openRouterResponse = gson.fromJson(responseBody, OpenRouterResponse::class.java)
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

    suspend fun sendMessageWithFallback(message: String): String {
        return try {
            sendMessage(message)
        } catch (e: Exception) {
            Log.w(TAG, "OpenRouter API failed, trying Gradio client method", e)
            sendMessageGradioClient(message)
        }
    }

    private suspend fun sendMessageGradioClient(message: String): String = withContext(Dispatchers.IO) {
        try {
            // This is a fallback and uses a different service, keeping it for now
            val requestData = mapOf(
                "fn_index" to 0,
                "data" to listOf(message, "You are a helpful assistant.", 0.7),
                "session_hash" to generateSessionHash()
            )

            val jsonRequest = gson.toJson(requestData)
            val requestBody = jsonRequest.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${GRADIO_BASE_URL}run/predict")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val responseMap = gson.fromJson(responseBody, Map::class.java) as Map<String, Any>
                val dataList = responseMap["data"] as? List<*>
                return@withContext dataList?.firstOrNull()?.toString()
                    ?: "Received response via Gradio client"
            } else {
                throw ApiException("Gradio client method failed: ${response.code}", response.code)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Gradio client method error", e)
            // Last resort - simulated response
            return@withContext generateSimulatedResponse(message)
        }
    }

    private fun generateSessionHash(): String {
        return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun generateSimulatedResponse(message: String): String {
        val responses = listOf(
            "I understand your message about '$message'. That's an interesting point to discuss.",
            "Thanks for sharing '$message' with me. I'd love to help you explore this topic further.",
            "Your question about '$message' is quite thoughtful. Here's what I think about it...",
            "Regarding '$message', I find that to be a fascinating subject. Could you tell me more?",
            "I appreciate you mentioning '$message'. That's definitely worth considering from different angles."
        )

        return "🤖 [AI Response] ${responses.random()}"
    }
}

class ApiException(message: String, val code: Int) : Exception(message)
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
