package com.example.unimarketguajira.repository

import android.content.Context
import com.example.unimarketguajira.data.db.UniMarketDatabase
import com.example.unimarketguajira.data.entities.NotificationEntity
import com.example.unimarketguajira.data.entities.PurchaseEntity
import com.example.unimarketguajira.models.Notification
import com.example.unimarketguajira.models.Purchase
import com.example.unimarketguajira.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object PurchaseRepository {

    suspend fun createPurchase(
        context: Context,
        purchase: Purchase,
        buyerName: String,
        productName: String
    ) {
        val db = UniMarketDatabase.getDatabase(context)
        val purchaseDao = db.purchaseDao()
        val notificationDao = db.notificationDao()

        // Crear notificación para el vendedor: VENTA
        val notifId = "notif_${System.currentTimeMillis()}_${(0..1000).random()}"
        val notifTitle = "¡Nueva venta!"
        val notifMessage = "$buyerName compró tu producto \"$productName\". Cantidad: ${purchase.quantity}. Punto de encuentro: ${purchase.deliveryPoint}."
        val notification = Notification(
            id = notifId,
            userId = purchase.sellerId,
            title = notifTitle,
            message = notifMessage,
            type = "VENTA",
            relatedProductId = purchase.productId,
            createdAt = System.currentTimeMillis(),
            isRead = false
        )

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("purchases").document(purchase.id).set(purchase).await()
                firestore.collection("notifications").document(notification.id).set(notification).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        purchaseDao.insertPurchase(PurchaseEntity.fromModel(purchase))
        notificationDao.insertNotification(NotificationEntity.fromModel(notification))
    }

    suspend fun getPurchasesForBuyer(context: Context, buyerId: String): List<Purchase> {
        val purchaseDao = UniMarketDatabase.getDatabase(context).purchaseDao()
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("purchases").whereEqualTo("buyerId", buyerId).get().await()
                for (doc in snapshot.documents) {
                    val purchase = doc.toObject(Purchase::class.java)
                    if (purchase != null) {
                        purchaseDao.insertPurchase(PurchaseEntity.fromModel(purchase))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return purchaseDao.getPurchasesForBuyer(buyerId).map { it.toModel() }
    }

    suspend fun getPurchasesForSeller(context: Context, sellerId: String): List<Purchase> {
        val purchaseDao = UniMarketDatabase.getDatabase(context).purchaseDao()
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("purchases").whereEqualTo("sellerId", sellerId).get().await()
                for (doc in snapshot.documents) {
                    val purchase = doc.toObject(Purchase::class.java)
                    if (purchase != null) {
                        purchaseDao.insertPurchase(PurchaseEntity.fromModel(purchase))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return purchaseDao.getPurchasesForSeller(sellerId).map { it.toModel() }
    }

    suspend fun updatePurchaseStatus(context: Context, purchaseId: String, newStatus: String, productName: String) {
        val db = UniMarketDatabase.getDatabase(context)
        val purchaseDao = db.purchaseDao()
        val notificationDao = db.notificationDao()

        val purchaseEntity = purchaseDao.getPurchaseById(purchaseId)
        if (purchaseEntity != null) {
            val updatedPurchase = purchaseEntity.copy(status = newStatus)
            purchaseDao.insertPurchase(updatedPurchase)

            // Crear notificación para el comprador: COMPRA
            val notifId = "notif_${System.currentTimeMillis()}_${(0..1000).random()}"
            val notifTitle = when (newStatus) {
                "CONFIRMED" -> "¡Compra confirmada!"
                "DELIVERED" -> "¡Producto entregado!"
                "CANCELLED" -> "Compra cancelada"
                else -> "Actualización de compra"
            }
            val notifMessage = when (newStatus) {
                "CONFIRMED" -> "El vendedor confirmó tu compra de \"$productName\". Coordina la entrega en el campus."
                "DELIVERED" -> "Tu compra de \"$productName\" ha sido marcada como entregada."
                "CANCELLED" -> "Lamentablemente, la venta de \"$productName\" fue cancelada."
                else -> "El estado de tu compra ha cambiado a $newStatus."
            }
            val notification = Notification(
                id = notifId,
                userId = purchaseEntity.buyerId,
                title = notifTitle,
                message = notifMessage,
                type = "COMPRA",
                relatedProductId = purchaseEntity.productId,
                createdAt = System.currentTimeMillis(),
                isRead = false
            )

            // Crear notificación de confirmación para el vendedor: VENTA
            val sellerNotifId = "notif_${System.currentTimeMillis()}_${(0..1000).random()}"
            val sellerNotifTitle = when (newStatus) {
                "CONFIRMED" -> "Confirmaste la venta"
                "DELIVERED" -> "Venta marcada como entregada"
                "CANCELLED" -> "Venta cancelada"
                else -> "Estado de venta actualizado"
            }
            val sellerNotifMessage = when (newStatus) {
                "CONFIRMED" -> "Confirmaste la venta de \"$productName\"."
                "DELIVERED" -> "La venta de \"$productName\" fue marcada como entregada."
                "CANCELLED" -> "Cancelaste la venta de \"$productName\"."
                else -> "El estado de tu venta cambió a $newStatus."
            }
            val sellerNotification = Notification(
                id = sellerNotifId,
                userId = purchaseEntity.sellerId,
                title = sellerNotifTitle,
                message = sellerNotifMessage,
                type = "VENTA",
                relatedProductId = purchaseEntity.productId,
                createdAt = System.currentTimeMillis(),
                isRead = false
            )

            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("purchases").document(purchaseId).update("status", newStatus).await()
                    firestore.collection("notifications").document(notification.id).set(notification).await()
                    firestore.collection("notifications").document(sellerNotification.id).set(sellerNotification).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            notificationDao.insertNotification(NotificationEntity.fromModel(notification))
            notificationDao.insertNotification(NotificationEntity.fromModel(sellerNotification))
        }
    }

    suspend fun getNotificationsForUser(context: Context, email: String): List<Notification> {
        val notificationDao = UniMarketDatabase.getDatabase(context).notificationDao()

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("notifications").whereEqualTo("userId", email).get().await()
                for (doc in snapshot.documents) {
                    val notif = doc.toObject(Notification::class.java)
                    if (notif != null) {
                        notificationDao.insertNotification(NotificationEntity.fromModel(notif))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return notificationDao.getNotificationsForUser(email).map { it.toModel() }
    }

    suspend fun markNotificationAsRead(context: Context, notificationId: String) {
        val notificationDao = UniMarketDatabase.getDatabase(context).notificationDao()
        notificationDao.markAsRead(notificationId)

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("notifications").document(notificationId).update("isRead", true).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getUnreadNotificationsCount(context: Context, userId: String): Int {
        val notificationDao = UniMarketDatabase.getDatabase(context).notificationDao()
        return notificationDao.getUnreadCount(userId)
    }

    suspend fun createSystemNotification(
        context: Context,
        userId: String,
        title: String,
        message: String,
        type: String,
        relatedProductId: Int
    ) {
        val notificationDao = UniMarketDatabase.getDatabase(context).notificationDao()
        val notifId = "notif_${System.currentTimeMillis()}_${(0..1000).random()}"
        val notification = Notification(
            id = notifId,
            userId = userId,
            title = title,
            message = message,
            type = type,
            relatedProductId = relatedProductId,
            createdAt = System.currentTimeMillis(),
            isRead = false
        )

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("notifications").document(notification.id).set(notification).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        notificationDao.insertNotification(NotificationEntity.fromModel(notification))
    }
}
