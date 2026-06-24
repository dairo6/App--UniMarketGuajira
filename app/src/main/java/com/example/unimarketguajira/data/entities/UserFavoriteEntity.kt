package com.example.unimarketguajira.data.entities

import androidx.room.Entity

@Entity(tableName = "user_favorites", primaryKeys = ["userEmail", "productId"])
data class UserFavoriteEntity(
    val userEmail: String,
    val productId: Int
)
