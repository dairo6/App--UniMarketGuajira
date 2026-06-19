package com.example.unimarketguajira.repository

import com.example.unimarketguajira.models.Product

object CartRepository {
    private val cartItems = mutableListOf<Product>()

    fun addToCart(product: Product) {
        cartItems.add(product)
    }

    fun removeFromCart(product: Product) {
        cartItems.remove(product)
    }

    fun getCartItems(): List<Product> = ArrayList(cartItems)

    fun getTotal(): Double {
        return cartItems.sumOf { it.price }
    }

    fun clearCart() {
        cartItems.clear()
    }
}