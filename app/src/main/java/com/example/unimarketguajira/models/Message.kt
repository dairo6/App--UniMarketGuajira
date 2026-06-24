package com.example.unimarketguajira.models

data class Message(
    val id: String = "",
    val senderId: String = "",
    val messageText: String = "",
    val timestamp: Long = 0
)
