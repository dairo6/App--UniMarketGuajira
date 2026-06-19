package com.example.unimarketguajira.models

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "SISTEMA", // COMPRA, VENTA, FAVORITO, NUEVA_PUB, SISTEMA
    val relatedProductId: Int = 0,
    val createdAt: Long = 0L,
    val isRead: Boolean = false
)
