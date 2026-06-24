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
    val status: String = "ACTIVE",
    val stock: Int = 1
) {
    fun toModel() = Product(id, name, description, price, location, imageUrls, category, condition, isFavorite, ownerEmail, status, stock)
    
    companion object {
        fun fromModel(product: Product, ownerEmail: String) = ProductEntity(
            id = if (product.id == 0) 0 else product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            location = product.location,
            imageUrls = product.imageUrls,
            category = product.category,
            condition = product.condition,
            isFavorite = product.isFavorite,
            ownerEmail = ownerEmail,
            status = product.status,
            stock = product.stock
        )
    }
}
