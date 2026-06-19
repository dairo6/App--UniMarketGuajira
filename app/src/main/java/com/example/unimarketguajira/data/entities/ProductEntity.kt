package com.example.unimarketguajira.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.unimarketguajira.models.Product

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val price: Double,
    val location: String,
    val imageUrls: List<String>,
    val category: String,
    val condition: String,
    val isFavorite: Boolean,
    val ownerEmail: String,
    val status: String = "ACTIVE"
) {
    fun toModel() = Product(id, name, description, price, location, imageUrls, category, condition, isFavorite, ownerEmail, status)
    
    companion object {
        fun fromModel(product: Product, ownerEmail: String) = ProductEntity(
            if (product.id == 0) 0 else product.id,
            product.name,
            product.description,
            product.price,
            product.location,
            product.imageUrls,
            product.category,
            product.condition,
            product.isFavorite,
            ownerEmail,
            product.status
        )
    }
}
