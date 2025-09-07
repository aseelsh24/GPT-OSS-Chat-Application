package com.gptoss.chat.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException

class ApiManager(private val context: Context) {
    private val apiClient = ApiClient()

    suspend fun sendChatMessage(message: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isNetworkAvailable()) {
                throw NoInternetException("No internet connection")
            }
            val response = apiClient.sendMessage(message)
            Result.success(response)
        } catch (e: SocketTimeoutException) {
            throw ServerTimeoutException("Server timed out")
        } catch (e: Exception) {
            Log.e(TAG, "All methods failed", e)
            Result.failure(e)
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
        private const val TAG = "ApiManager"
        @Volatile
        private var INSTANCE: ApiManager? = null

        fun getInstance(context: Context): ApiManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
