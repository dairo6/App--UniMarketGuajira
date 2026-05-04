package com.example.unimarketguajira.repository

import com.example.unimarketguajira.models.Product

object ProductRepository {
    private val products = mutableListOf<Product>()

    fun addProduct(product: Product) {
        products.add(0, product)
    }

    fun getAllProducts(): List<Product> {
        return products.toList()
    }

    fun setInitialProducts(initialList: List<Product>) {
        if (products.isEmpty()) {
            products.addAll(initialList)
        }
    }
}