package com.gptoss.chat.models

import com.gptoss.chat.utils.MessageType

data class Message(
    val text: String,
    val type: MessageType,
    val timestamp: String
) {
    fun isFromUser(): Boolean = type == MessageType.USER
    fun isFromBot(): Boolean = type == MessageType.BOT
}
