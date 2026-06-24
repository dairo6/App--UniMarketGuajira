package com.example.unimarketguajira.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.unimarketguajira.models.ChatRoom

@Entity(tableName = "chat_rooms")
data class ChatRoomEntity(
    @PrimaryKey val id: String,
    val id_comprador: String,
    val id_vendedor: String,
    val id_producto: Int,
    val lastMessage: String,
    val timestamp: Long,
    val lastSenderId: String,
    val status: String
) {
    fun toModel() = ChatRoom(
        id = id,
        id_comprador = id_comprador,
        id_vendedor = id_vendedor,
        id_producto = id_producto,
        lastMessage = lastMessage,
        timestamp = timestamp,
        lastSenderId = lastSenderId,
        status = status
    )

    companion object {
        fun fromModel(chatRoom: ChatRoom) = ChatRoomEntity(
            id = chatRoom.id,
            id_comprador = chatRoom.id_comprador,
            id_vendedor = chatRoom.id_vendedor,
            id_producto = chatRoom.id_producto,
            lastMessage = chatRoom.lastMessage,
            timestamp = chatRoom.timestamp,
            lastSenderId = chatRoom.lastSenderId,
            status = chatRoom.status
        )
    }
}
