package com.gptoss.chat.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiManager(private val context: Context) {
    companion object {
        private const val TAG = "ApiManager"
    }

    private val apiClient = ApiClient()

    suspend fun sendChatMessage(message: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isNetworkAvailable()) {
                Log.w(TAG, "No network available, using offline response")
                Result.success("📵 [Offline Mode] I understand your message: '$message'. Unfortunately, I need an internet connection to provide more detailed responses.")
            } else {
                Log.d(TAG, "Network available, sending to chat API")
                val response = apiClient.sendMessageWithFallback(message)
                Result.success(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "All methods failed", e)
            // حتى لو فشل كله، نعطي رد محاكي
            Result.success("⚠️ [Connection Issue] I received your message about '$message'. Please check your connection and try again.")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network", e)
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ApiManager? = null

        fun getInstance(context: Context): ApiManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
