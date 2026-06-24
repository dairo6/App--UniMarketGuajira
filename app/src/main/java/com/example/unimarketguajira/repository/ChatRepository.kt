package com.example.unimarketguajira.repository

import android.content.Context
import com.example.unimarketguajira.data.db.UniMarketDatabase
import com.example.unimarketguajira.data.entities.ChatRoomEntity
import com.example.unimarketguajira.data.entities.MessageEntity
import com.example.unimarketguajira.data.entities.NotificationEntity
import com.example.unimarketguajira.models.ChatRoom
import com.example.unimarketguajira.models.Message
import com.example.unimarketguajira.models.Notification
import com.example.unimarketguajira.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object ChatRepository {

    suspend fun getOrCreateChatRoom(
        context: Context,
        idComprador: String,
        idVendedor: String,
        idProducto: Int
    ): ChatRoom {
        val db = FirebaseFirestore.getInstance()
        val localDao = UniMarketDatabase.getDatabase(context).chatRoomDao()

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                // Buscar si existe una sala de chat activa entre comprador, vendedor y producto
                val querySnapshot = db.collection("chats")
                    .whereEqualTo("id_comprador", idComprador)
                    .whereEqualTo("id_vendedor", idVendedor)
                    .whereEqualTo("id_producto", idProducto)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    val chat = doc.toObject(ChatRoom::class.java)
                    if (chat != null) {
                        val finalChat = chat.copy(id = doc.id)
                        localDao.insertChatRoom(ChatRoomEntity.fromModel(finalChat))
                        return finalChat
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback local: Buscar en Room
        val localChats = localDao.getChatRoomsForUser(idComprador)
        val existingLocal = localChats.firstOrNull {
            it.id_comprador == idComprador && it.id_vendedor == idVendedor && it.id_producto == idProducto
        }
        if (existingLocal != null) {
            return existingLocal.toModel()
        }

        // Si no existe, crear una nueva instancia
        val docId = if (NetworkUtils.isNetworkAvailable(context)) {
            db.collection("chats").document().id
        } else {
            "chat_${System.currentTimeMillis()}_${(0..1000).random()}"
        }

        val newChat = ChatRoom(
            id = docId,
            id_comprador = idComprador,
            id_vendedor = idVendedor,
            id_producto = idProducto,
            lastMessage = "Conversación iniciada",
            timestamp = System.currentTimeMillis(),
            lastSenderId = "",
            status = "ACTIVE"
        )

        // Registrar en Firestore si está online
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                db.collection("chats").document(docId).set(newChat).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Guardar localmente
        localDao.insertChatRoom(ChatRoomEntity.fromModel(newChat))
        return newChat
    }

    suspend fun sendMessage(
        context: Context,
        chatId: String,
        senderId: String,
        messageText: String
    ) {
        val db = FirebaseFirestore.getInstance()
        val localDb = UniMarketDatabase.getDatabase(context)

        // Obtener la información de la sala de chat
        var chatRoom = localDb.chatRoomDao().getChatRoomById(chatId)?.toModel()
        if (chatRoom == null && NetworkUtils.isNetworkAvailable(context)) {
            try {
                val doc = db.collection("chats").document(chatId).get().await()
                chatRoom = doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val receiverId = if (chatRoom != null) {
            if (senderId == chatRoom.id_comprador) chatRoom.id_vendedor else chatRoom.id_comprador
        } else {
            "admin@unimarket.com"
        }

        val productId = chatRoom?.id_producto ?: 0

        val msgId = "msg_${System.currentTimeMillis()}_${(0..1000).random()}"
        val message = Message(
            id = msgId,
            senderId = senderId,
            messageText = messageText,
            timestamp = System.currentTimeMillis()
        )

        // 1. Guardar mensaje y actualización de chat localmente en Room
        localDb.messageDao().insertMessage(MessageEntity.fromModel(message, chatId))
        chatRoom?.let {
            val updatedChat = it.copy(
                lastMessage = messageText,
                timestamp = message.timestamp,
                lastSenderId = senderId
            )
            localDb.chatRoomDao().insertChatRoom(ChatRoomEntity.fromModel(updatedChat))
        }

        // 2. Crear Notificación de Chat
        val notifId = "notif_${System.currentTimeMillis()}_${(0..1000).random()}"
        val senderName = senderId.split("@").firstOrNull()?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        } ?: "Usuario"
        
        val notification = Notification(
            notificationId = notifId,
            userId = receiverId,
            chatId = chatId,
            productId = productId,
            senderId = senderId,
            title = "Nuevo mensaje de $senderName",
            message = messageText,
            type = "CHAT",
            createdAt = System.currentTimeMillis(),
            isRead = false
        )

        // Guardar notificación local
        localDb.notificationDao().insertNotification(NotificationEntity.fromModel(notification))

        // 3. Sincronizar en Firebase si hay conexión
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                // Guardar mensaje
                db.collection("chats").document(chatId)
                    .collection("messages").document(msgId).set(message).await()

                // Actualizar chat
                db.collection("chats").document(chatId).update(
                    mapOf(
                        "lastMessage" to messageText,
                        "timestamp" to message.timestamp,
                        "lastSenderId" to senderId
                    )
                ).await()

                // Guardar notificación
                db.collection("notifications").document(notifId).set(notification).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getChatRoomsForUser(context: Context, email: String, status: String = "ACTIVE"): List<ChatRoom> {
        val localDao = UniMarketDatabase.getDatabase(context).chatRoomDao()

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val list = mutableListOf<ChatRoom>()

                // Obtener chats como comprador
                val buyerSnap = db.collection("chats").whereEqualTo("id_comprador", email).get().await()
                for (doc in buyerSnap.documents) {
                    val chat = doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
                    if (chat != null) list.add(chat)
                }

                // Obtener chats como vendedor
                val sellerSnap = db.collection("chats").whereEqualTo("id_vendedor", email).get().await()
                for (doc in sellerSnap.documents) {
                    val chat = doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
                    if (chat != null && list.none { it.id == chat.id }) list.add(chat)
                }

                if (list.isNotEmpty()) {
                    localDao.insertAll(list.map { ChatRoomEntity.fromModel(it) })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return localDao.getChatRoomsForUserByStatus(email, status).map { it.toModel() }
    }

    suspend fun archiveChatRoom(context: Context, chatId: String, archive: Boolean) {
        val localDao = UniMarketDatabase.getDatabase(context).chatRoomDao()
        val localEntity = localDao.getChatRoomById(chatId)
        if (localEntity != null) {
            val newStatus = if (archive) "ARCHIVED" else "ACTIVE"
            val updated = localEntity.copy(status = newStatus)
            localDao.insertChatRoom(updated)

            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("chats").document(chatId).update("status", newStatus).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
