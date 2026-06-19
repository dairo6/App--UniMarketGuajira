package com.example.unimarketguajira.models

data class Purchase(
    val id: String = "",
    val productId: Int = 0,
    val sellerId: String = "",
    val buyerId: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val purchaseDate: Long = 0L,
    val deliveryPoint: String = "",
    val paymentMethod: String = "",
    val status: String = "PENDING"
)
