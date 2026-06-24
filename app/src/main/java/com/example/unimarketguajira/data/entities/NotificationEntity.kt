package com.example.unimarketguajira.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.unimarketguajira.models.Notification

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val notificationId: String,
    val userId: String,
    val chatId: String,
    val productId: Int,
    val senderId: String,
    val title: String,
    val message: String,
    val type: String,
    val createdAt: Long,
    val isRead: Boolean
) {
    fun toModel() = Notification(
        id = notificationId,
        userId = userId,
        chatId = chatId,
        relatedProductId = productId,
        senderId = senderId,
        title = title,
        message = message,
        type = type,
        createdAt = createdAt,
        isRead = isRead,
        notificationId = notificationId,
        productId = productId
    )

    companion object {
        fun fromModel(notification: Notification) = NotificationEntity(
            notificationId = if (notification.notificationId.isNotEmpty()) notification.notificationId else notification.id,
            userId = notification.userId,
            chatId = notification.chatId,
            productId = if (notification.productId > 0) notification.productId else notification.relatedProductId,
            senderId = notification.senderId,
            title = notification.title,
            message = notification.message,
            type = notification.type,
            createdAt = notification.createdAt,
            isRead = notification.isRead
        )
    }
}
