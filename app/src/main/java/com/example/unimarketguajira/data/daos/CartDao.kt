package com.example.unimarketguajira.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarketguajira.data.entities.CartItemEntity
import com.example.unimarketguajira.data.entities.ProductEntity

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToCart(cartItem: CartItemEntity): Long

    @Query("DELETE FROM cart_items WHERE productId = :productId AND userEmail = :userEmail")
    suspend fun removeFromCart(productId: Int, userEmail: String)

    @Query("SELECT p.* FROM products p INNER JOIN cart_items c ON p.id = c.productId WHERE c.userEmail = :userEmail")
    suspend fun getCartProductsForUser(userEmail: String): List<ProductEntity>

    @Query("DELETE FROM cart_items WHERE userEmail = :userEmail")
    suspend fun clearCartForUser(userEmail: String)

    @Query("SELECT * FROM cart_items WHERE userEmail = :userEmail")
    suspend fun getCartItemsForUser(userEmail: String): List<CartItemEntity>
}
