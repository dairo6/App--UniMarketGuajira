package com.example.unimarketguajira.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_view_history")
data class ProductViewHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val userId: String,
    val viewedAt: Long
)
