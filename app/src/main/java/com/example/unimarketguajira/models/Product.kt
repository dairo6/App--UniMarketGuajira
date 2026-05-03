package com.example.unimarketguajira.models

data class Product(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val location: String,
    val imageUrls: List<Int>, // Usamos Int por ahora para mockups, pero lista para soportar carrusel
    val category: String,
    val condition: String,
    var isFavorite: Boolean = false
)