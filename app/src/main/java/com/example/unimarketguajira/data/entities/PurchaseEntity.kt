package com.example.unimarketguajira.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.unimarketguajira.models.Purchase

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey val id: String,
    val productId: Int,
    val sellerId: String,
    val buyerId: String,
    val price: Double,
    val quantity: Int,
    val purchaseDate: Long,
    val deliveryPoint: String,
    val paymentMethod: String,
    val status: String,
    val purchaseStatus: String = "PENDING"
) {
    fun toModel() = Purchase(id, productId, sellerId, buyerId, price, quantity, purchaseDate, deliveryPoint, paymentMethod, status, purchaseStatus)

    companion object {
        fun fromModel(purchase: Purchase) = PurchaseEntity(
            purchase.id,
            purchase.productId,
            purchase.sellerId,
            purchase.buyerId,
            purchase.price,
            purchase.quantity,
            purchase.purchaseDate,
            purchase.deliveryPoint,
            purchase.paymentMethod,
            purchase.status,
            purchase.purchaseStatus
          )
     }
}
