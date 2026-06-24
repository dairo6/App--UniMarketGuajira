package com.example.unimarketguajira.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.unimarketguajira.activities.ChatActivity
import com.example.unimarketguajira.models.ChatRoom
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ChatNotificationService {

    private const val CHANNEL_ID = "chat_notifications"
    private const val CHANNEL_NAME = "Mensajes de Chat"
    private const val CHANNEL_DESC = "Notificaciones de nuevos mensajes del marketplace"

    private var buyerListener: ListenerRegistration? = null
    private var sellerListener: ListenerRegistration? = null

    // Almacena el timestamp de inicio para evitar notificar mensajes viejos al abrir la app
    private var serviceStartTime: Long = 0L

    // Guardar el último mensaje visto por cada chat_id para evitar notificaciones repetidas
    private val lastSeenMessageTimestamps = mutableMapOf<String, Long>()

    fun startListening(context: Context) {
        val loggedEmail = UserManager.getLoggedUserEmail(context) ?: return

        // Evitar duplicar listeners activos
        if (buyerListener != null || sellerListener != null) return

        serviceStartTime = System.currentTimeMillis()
        createNotificationChannel(context)

        val db = FirebaseFirestore.getInstance()

        // 1. Escuchar chats como comprador
        buyerListener = db.collection("chats")
            .whereEqualTo("id_comprador", loggedEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                snapshot?.let { processChatChanges(context, it.toObjects(ChatRoom::class.java), loggedEmail) }
            }

        // 2. Escuchar chats como vendedor
        sellerListener = db.collection("chats")
            .whereEqualTo("id_vendedor", loggedEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                snapshot?.let { processChatChanges(context, it.toObjects(ChatRoom::class.java), loggedEmail) }
            }
    }

    fun stopListening() {
        buyerListener?.remove()
        buyerListener = null
        sellerListener?.remove()
        sellerListener = null
        lastSeenMessageTimestamps.clear()
    }

    private fun processChatChanges(context: Context, chatRooms: List<ChatRoom>, loggedEmail: String) {
        for (chat in chatRooms) {
            val chatId = chat.id
            if (chatId.isEmpty()) continue

            // Guardar chat room localmente en Room
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val localDb = com.example.unimarketguajira.data.db.UniMarketDatabase.getDatabase(context)
                    localDb.chatRoomDao().insertChatRoom(com.example.unimarketguajira.data.entities.ChatRoomEntity.fromModel(chat))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (chat.lastSenderId == loggedEmail || chat.lastSenderId.isEmpty()) {
                continue
            }

            // Si el chat ya está abierto activamente en pantalla, no notificar en el sistema
            if (chatId == ChatActivity.currentOpenChatId) {
                continue
            }

            val lastTime = chat.timestamp
            val prevTime = lastSeenMessageTimestamps[chatId] ?: 0L

            // Solo notificar si es un mensaje nuevo y posterior al inicio del servicio
            if (lastTime > prevTime && lastTime > serviceStartTime) {
                lastSeenMessageTimestamps[chatId] = lastTime
                showNotification(context, chat)
            } else {
                // Registrar el timestamp inicial para futuras comparaciones
                if (prevTime == 0L) {
                    lastSeenMessageTimestamps[chatId] = lastTime
                }
            }
        }
    }

    private fun showNotification(context: Context, chat: ChatRoom) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Formatear el nombre del remitente
        val senderEmail = chat.lastSenderId
        val senderName = senderEmail.split("@").firstOrNull()?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        } ?: "Usuario"

        // Intent para abrir ChatActivity directamente con los parámetros necesarios
        val intent = Intent(context, ChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("CHAT_ID", chat.id)
            putExtra("ID_COMPRADOR", chat.id_comprador)
            putExtra("ID_VENDEDOR", chat.id_vendedor)
            putExtra("ID_PRODUCTO", chat.id_producto)
            putExtra("PRODUCT_NAME", "UniMarket Producto")
        }

        // Configurar PendingIntent compatible con Android 12+ (FLAG_IMMUTABLE o FLAG_MUTABLE)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, chat.id.hashCode(), intent, pendingIntentFlags)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle("Nuevo mensaje de $senderName")
            .setContentText(chat.lastMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        notificationManager.notify(chat.id.hashCode(), builder.build())
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
