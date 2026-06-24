package com.example.unimarketguajira.repository

import android.content.Context
import com.example.unimarketguajira.data.db.UniMarketDatabase
import com.example.unimarketguajira.data.entities.CartItemEntity
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object CartRepository {
    suspend fun addToCart(context: Context, product: Product, userEmail: String) {
        val dao = UniMarketDatabase.getDatabase(context).cartDao()
        dao.addToCart(CartItemEntity(productId = product.id, userEmail = userEmail))
        syncLocalCartToFirebase(context, userEmail)
    }

    suspend fun removeFromCart(context: Context, product: Product, userEmail: String) {
        val dao = UniMarketDatabase.getDatabase(context).cartDao()
        dao.removeFromCart(productId = product.id, userEmail = userEmail)
        syncLocalCartToFirebase(context, userEmail)
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
        syncLocalCartToFirebase(context, userEmail)
    }

    suspend fun syncLocalCartToFirebase(context: Context, userEmail: String) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val dao = UniMarketDatabase.getDatabase(context).cartDao()
                val productIds = dao.getCartItemsForUser(userEmail).map { it.productId }
                db.collection("carts").document(userEmail).set(mapOf("productIds" to productIds)).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncFirebaseCartToLocal(context: Context, userEmail: String) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("carts").document(userEmail).get().await()
                if (snapshot.exists()) {
                    val productIds = snapshot.get("productIds") as? List<Long> ?: emptyList()
                    val dao = UniMarketDatabase.getDatabase(context).cartDao()
                    
                    // Limpiar localmente primero
                    dao.clearCartForUser(userEmail)
                    
                    // Insertar todos los del Firebase
                    for (productId in productIds) {
                        dao.addToCart(CartItemEntity(productId = productId.toInt(), userEmail = userEmail))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}