package com.example.unimarketguajira.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.unimarketguajira.models.Message

@Entity(tableName = "chat_messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val messageText: String,
    val timestamp: Long
) {
    fun toModel() = Message(
        id = id,
        senderId = senderId,
        messageText = messageText,
        timestamp = timestamp
    )

    companion object {
        fun fromModel(message: Message, chatId: String) = MessageEntity(
            id = message.id,
            chatId = chatId,
            senderId = message.senderId,
            messageText = message.messageText,
            timestamp = message.timestamp
        )
    }
}
