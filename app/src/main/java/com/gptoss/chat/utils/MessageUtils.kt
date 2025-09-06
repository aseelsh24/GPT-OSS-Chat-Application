package com.gptoss.chat.utils

import com.gptoss.chat.models.Message
import java.text.SimpleDateFormat
import java.util.*

object MessageUtils {
    
    fun createUserMessage(text: String): Message {
        return Message(
            text = text,
            type = MessageType.USER,
            timestamp = getCurrentTime()
        )
    }
    
    fun createBotMessage(text: String): Message {
        return Message(
            text = text,
            type = MessageType.BOT,
            timestamp = getCurrentTime()
        )
    }
    
    fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
    
    fun formatTimestamp(timestamp: String): String {
        return try {
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(timestamp)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(time!!)
        } catch (e: Exception) {
            timestamp
        }
    }
    
    fun isValidMessage(text: String): Boolean {
        return text.isNotBlank() && text.trim().length > 0
    }
    
    fun sanitizeMessage(text: String): String {
        return text.trim().replace(Regex("\\s+"), " ")
    }
}
