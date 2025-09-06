package com.gptoss.chat.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface GradioApiService {
    @Headers("Content-Type: application/json")
    @POST
    suspend fun sendMessage(
        @Url url: String,
        @Body request: GradioRequest
    ): Response<GradioResponse>
}

data class GradioRequest(
    val fn_index: Int = 0,
    val data: List<Any>,
    val session_hash: String = generateDefaultSession(),
    val event_data: Any? = null
) {
    companion object {
        fun generateDefaultSession(): String = "session_${System.currentTimeMillis()}"
        
        fun createChatRequest(
            message: String,
            systemPrompt: String = "You are a helpful assistant",
            temperature: Double = 0.7,
            reasoningEffort: String = "medium",
            enableBrowsing: Boolean = true
        ): GradioRequest {
            return GradioRequest(
                fn_index = 0,
                data = listOf(message, systemPrompt, temperature, reasoningEffort, enableBrowsing),
                session_hash = generateDefaultSession()
            )
        }
    }
}

data class GradioResponse(
    val data: List<String>? = null,
    val error: String? = null,
    val success: Boolean? = null,
    val duration: Double? = null
) {
    fun getResponseText(): String {
        return when {
            error != null -> "Error: $error"
            data?.isNotEmpty() == true -> data.first()
            else -> "No response received from AI"
        }
    }
}
