package com.example.unimarketguajira.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarketguajira.data.entities.ProductEntity

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Query("SELECT * FROM products ORDER BY id DESC")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: Int): ProductEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>): List<Long>

    @Query("SELECT * FROM products WHERE (name LIKE :query OR category LIKE :query OR description LIKE :query) AND status = 'ACTIVE' ORDER BY id DESC")
    suspend fun searchProducts(query: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE ownerEmail = :ownerEmail ORDER BY id DESC")
    suspend fun getProductsByOwner(ownerEmail: String): List<ProductEntity>
}
