package com.example.unimarketguajira.models

import com.google.firebase.firestore.PropertyName

data class Notification(
    val id: String = "",
    val userId: String = "",
    val chatId: String = "",
    val relatedProductId: Int = 0,
    val senderId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "SISTEMA", // COMPRA, VENTA, FAVORITO, CHAT, SISTEMA
    val createdAt: Long = 0L,
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val notificationId: String = id,
    val productId: Int = relatedProductId
) {
    @get:PropertyName("read")
    @set:PropertyName("read")
    var read: Boolean
        @JvmName("getCompatRead") get() = isRead
        @JvmName("setCompatRead") set(value) {
            isRead = value
        }
}
