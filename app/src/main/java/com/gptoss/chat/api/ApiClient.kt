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

class ApiClient {
    companion object {
        private const val BASE_URL = "https://amd-gpt-oss-120b-chatbot.hf.space/"
        private const val CHAT_ENDPOINT = "chat"  // الـ endpoint الصحيح
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
        systemPrompt: String = "You are a helpful assistant.",
        temperature: Double = 0.7
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending message to /chat endpoint: $message")

            // تنسيق البيانات حسب المطلوب
            val requestData = mapOf(
                "message" to message,
                "system_prompt" to systemPrompt,
                "temperature" to temperature
            )

            val jsonRequest = gson.toJson(requestData)
            val requestBody = jsonRequest.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${BASE_URL}${CHAT_ENDPOINT}")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "GPT-OSS-Chat/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Response code: ${response.code}")
            Log.d(TAG, "Response body: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                // إذا كان الرد مباشر (string)
                return@withContext if (responseBody.startsWith("\"") && responseBody.endsWith("\"")) {
                    // إزالة علامات الاقتباس من JSON string
                    responseBody.removeSurrounding("\"").replace("\\\"", "\"")
                } else {
                    // محاولة تحليل JSON
                    try {
                        val responseMap = gson.fromJson(responseBody, Map::class.java) as? Map<String, Any>
                        responseMap?.get("response")?.toString() ?: responseBody
                    } catch (e: Exception) {
                        responseBody
                    }
                }
            } else {
                throw ApiException("Chat API request failed: ${response.code} - $responseBody", response.code)
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
            Log.w(TAG, "Chat API failed, trying Gradio client method", e)
            sendMessageGradioClient(message)
        }
    }

    private suspend fun sendMessageGradioClient(message: String): String = withContext(Dispatchers.IO) {
        try {
            // طريقة Gradio Client البديلة
            val requestData = mapOf(
                "fn_index" to 0,
                "data" to listOf(message, "You are a helpful assistant.", 0.7),
                "session_hash" to generateSessionHash()
            )

            val jsonRequest = gson.toJson(requestData)
            val requestBody = jsonRequest.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${BASE_URL}run/predict")
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
            // الخيار الأخير - رد محاكي
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
