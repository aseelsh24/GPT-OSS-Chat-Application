package com.gptoss.chat.utils

enum class MessageType {
    USER, 
    BOT;
    
    companion object {
        fun fromString(type: String): MessageType {
            return when (type.uppercase()) {
                "USER" -> USER
                "BOT" -> BOT
                else -> BOT
            }
        }
    }
}
