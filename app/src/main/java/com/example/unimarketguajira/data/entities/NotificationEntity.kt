package com.example.unimarketguajira.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.unimarketguajira.models.Notification

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val relatedProductId: Int,
    val createdAt: Long,
    val isRead: Boolean
) {
    fun toModel() = Notification(id, userId, title, message, type, relatedProductId, createdAt, isRead)

    companion object {
        fun fromModel(notification: Notification) = NotificationEntity(
            notification.id,
            notification.userId,
            notification.title,
            notification.message,
            notification.type,
            notification.relatedProductId,
            notification.createdAt,
            notification.isRead
        )
    }
}
