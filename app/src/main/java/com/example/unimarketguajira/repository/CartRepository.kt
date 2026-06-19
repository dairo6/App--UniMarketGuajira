package com.example.unimarketguajira.repository

import android.content.Context
import com.example.unimarketguajira.data.db.UniMarketDatabase
import com.example.unimarketguajira.data.entities.CartItemEntity
import com.example.unimarketguajira.models.Product

object CartRepository {
    suspend fun addToCart(context: Context, product: Product, userEmail: String) {
        val dao = UniMarketDatabase.getDatabase(context).cartDao()
        dao.addToCart(CartItemEntity(productId = product.id, userEmail = userEmail))
    }

    suspend fun removeFromCart(context: Context, product: Product, userEmail: String) {
        val dao = UniMarketDatabase.getDatabase(context).cartDao()
        dao.removeFromCart(productId = product.id, userEmail = userEmail)
    }

    suspend fun getCartItems(context: Context, userEmail: String): List<Product> {
        val dao = UniMarketDatabase.getDatabase(context).cartDao()
        return dao.getCartProductsForUser(userEmail).map { it.toModel() }
    }

    suspend fun getTotal(context: Context, userEmail: String): Double {
        val items = getCartItems(context, userEmail)
        return items.sumOf { it.price }
    }

    suspend fun clearCart(context: Context, userEmail: String) {
        val dao = UniMarketDatabase.getDatabase(context).cartDao()
        dao.clearCartForUser(userEmail)
    }
}