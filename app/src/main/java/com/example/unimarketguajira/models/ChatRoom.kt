package com.example.unimarketguajira.models

data class ChatRoom(
    val id: String = "",
    val id_comprador: String = "",
    val id_vendedor: String = "",
    val id_producto: Int = 0,
    val lastMessage: String = "",
    val timestamp: Long = 0,
    val lastSenderId: String = "",
    val status: String = "ACTIVE"
)
