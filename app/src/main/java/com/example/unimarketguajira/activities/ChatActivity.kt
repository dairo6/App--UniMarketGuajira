package com.example.unimarketguajira.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.adapters.MessageAdapter
import com.example.unimarketguajira.models.Message
import com.example.unimarketguajira.repository.ChatRepository
import com.example.unimarketguajira.services.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.unimarketguajira.data.db.UniMarketDatabase
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessageInput: EditText
    private lateinit var btnSendMessage: ImageButton
    private lateinit var pbChatLoading: ProgressBar

    private var chatId: String = ""
    private var idComprador: String = ""
    private var idVendedor: String = ""
    private var idProducto: Int = 0
    private var productName: String = ""
    private var otherUserName: String = ""
    private var otherUserEmail: String = ""

    private var currentUserEmail: String = ""
    private var messageListener: ListenerRegistration? = null
    private val messagesList = mutableListOf<Message>()
    private lateinit var adapter: MessageAdapter
    private var isFirstSnapshot = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        val appBarLayout = findViewById<View>(R.id.appBarLayout)
        val mainView = findViewById<View>(R.id.main)

        // Callback unificado para manejar la inserción e inserción animada del teclado e insets del sistema
        val insetsCallback = object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE),
            OnApplyWindowInsetsListener {

            private fun applyInsets(insets: WindowInsetsCompat) {
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                // Elegir el padding inferior máximo entre la barra de navegación del sistema y el teclado (IME)
                val bottomPadding = maxOf(systemBars.bottom, imeHeight)

                // Respetar muescas (notch) y safe area laterales/superiores en horizontal y vertical
                appBarLayout.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
                mainView.setPadding(systemBars.left, 0, systemBars.right, bottomPadding)
            }

            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>
            ): WindowInsetsCompat {
                applyInsets(insets)
                return insets
            }

            override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
                applyInsets(insets)
                return insets
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(mainView, insetsCallback)
        ViewCompat.setWindowInsetsAnimationCallback(mainView, insetsCallback)

        // Recuperar los extras de navegación
        chatId = intent.getStringExtra("CHAT_ID") ?: ""
        idComprador = intent.getStringExtra("ID_COMPRADOR") ?: ""
        idVendedor = intent.getStringExtra("ID_VENDEDOR") ?: ""
        idProducto = intent.getIntExtra("ID_PRODUCTO", 0)
        productName = intent.getStringExtra("PRODUCT_NAME") ?: "Producto"

        currentUserEmail = UserManager.getLoggedUserEmail(this) ?: ""
        if (currentUserEmail.isEmpty()) {
            Toast.makeText(this, "Debes iniciar sesión para acceder al chat", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Definir la otra parte involucrada en el chat
        otherUserEmail = if (currentUserEmail == idComprador) {
            idVendedor
        } else {
            idComprador
        }

        // Formatear nombre fallback
        otherUserName = otherUserEmail.split("@").firstOrNull()?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        } ?: "Usuario"

        setupViews()
        loadOtherUserProfile()
        loadProductDetails()
        listenForMessages()
    }

    private fun setupViews() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Título: Nombre de la otra persona. Subtítulo: Producto en cuestión
        toolbar.title = otherUserName
        toolbar.subtitle = productName

        rvMessages = findViewById(R.id.rvMessages)
        etMessageInput = findViewById(R.id.etMessageInput)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        pbChatLoading = findViewById(R.id.pbChatLoading)

        adapter = MessageAdapter(messagesList, currentUserEmail)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = adapter

        // Desplazarse al último mensaje cuando el tamaño de la lista de mensajes cambia (teclado abierto)
        rvMessages.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                if (messagesList.isNotEmpty()) {
                    rvMessages.post {
                        rvMessages.scrollToPosition(messagesList.size - 1)
                    }
                }
            }
        }

        btnSendMessage.setOnClickListener {
            val text = etMessageInput.text.toString().trim()
            if (text.isNotEmpty() && chatId.isNotEmpty()) {
                etMessageInput.text.clear()
                lifecycleScope.launch {
                    try {
                        ChatRepository.sendMessage(this@ChatActivity, chatId, currentUserEmail, text)
                    } catch (e: Exception) {
                        Toast.makeText(this@ChatActivity, "Error al enviar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun loadOtherUserProfile() {
        lifecycleScope.launch {
            val otherUser = UserManager.getUserByEmail(this@ChatActivity, otherUserEmail)
            if (otherUser != null) {
                otherUserName = otherUser.fullName
                findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).title = otherUserName
            }
        }
    }

    private fun loadProductDetails() {
        if (idProducto > 0) {
            lifecycleScope.launch {
                val product = com.example.unimarketguajira.repository.ProductRepository.getProductById(this@ChatActivity, idProducto)
                if (product != null) {
                    productName = product.name
                    findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).subtitle = productName
                }
            }
        }
    }

    private fun listenForMessages() {
        if (chatId.isEmpty()) return

        // 1. Cargar mensajes locales primero para soporte offline rápido
        lifecycleScope.launch {
            try {
                val localDb = UniMarketDatabase.getDatabase(this@ChatActivity)
                val cached = localDb.messageDao().getMessagesForChat(chatId).map { it.toModel() }
                if (cached.isNotEmpty() && messagesList.isEmpty()) {
                    messagesList.clear()
                    messagesList.addAll(cached)
                    adapter.notifyDataSetChanged()
                    rvMessages.scrollToPosition(messagesList.size - 1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        pbChatLoading.visibility = View.VISIBLE
        val db = FirebaseFirestore.getInstance()
        messageListener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                pbChatLoading.visibility = View.GONE
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val wasNearBottom = isLastItemVisible()

                    messagesList.clear()
                    val parsedMessages = mutableListOf<Message>()
                    for (doc in snapshot.documents) {
                        val msg = doc.toObject(Message::class.java)
                        if (msg != null) {
                            val finalMsg = msg.copy(id = doc.id)
                            parsedMessages.add(finalMsg)
                            messagesList.add(finalMsg)
                        }
                    }

                    // Guardar en Room en segundo plano
                    lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val localDb = UniMarketDatabase.getDatabase(this@ChatActivity)
                            localDb.messageDao().insertAll(parsedMessages.map { com.example.unimarketguajira.data.entities.MessageEntity.fromModel(it, chatId) })
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val lastMessage = messagesList.lastOrNull()
                    val isMyMessage = lastMessage?.senderId == currentUserEmail

                    adapter.notifyDataSetChanged()
                    
                    if (messagesList.isNotEmpty()) {
                        if (isFirstSnapshot) {
                            rvMessages.scrollToPosition(messagesList.size - 1)
                            isFirstSnapshot = false
                        } else if (wasNearBottom || isMyMessage) {
                            rvMessages.smoothScrollToPosition(messagesList.size - 1)
                        }
                    }
                    // Marcar notificaciones de este chat como leídas
                    markChatNotificationsAsRead()
                }
            }
    }

    private fun isLastItemVisible(): Boolean {
        val layoutManager = rvMessages.layoutManager as? LinearLayoutManager ?: return false
        val totalItemCount = layoutManager.itemCount
        if (totalItemCount == 0) return true
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
        return lastVisibleItemPosition >= totalItemCount - 2
    }

    private fun markChatNotificationsAsRead() {
        if (chatId.isEmpty() || currentUserEmail.isEmpty()) return
        lifecycleScope.launch {
            try {
                // Actualizar localmente en Room
                val localDb = UniMarketDatabase.getDatabase(this@ChatActivity)
                val localNotifications = localDb.notificationDao().getNotificationsForUser(currentUserEmail)
                for (notif in localNotifications) {
                    if (notif.chatId == chatId && !notif.isRead) {
                        localDb.notificationDao().markAsRead(notif.notificationId)
                    }
                }

                // Sincronizar con Firestore
                if (com.example.unimarketguajira.utils.NetworkUtils.isNetworkAvailable(this@ChatActivity)) {
                    val db = FirebaseFirestore.getInstance()
                    val unreadNotifs = db.collection("notifications")
                        .whereEqualTo("userId", currentUserEmail)
                        .whereEqualTo("chatId", chatId)
                        .whereEqualTo("isRead", false)
                        .get()
                        .await()
                    
                    val batch = db.batch()
                    for (doc in unreadNotifs.documents) {
                        batch.update(doc.reference, "isRead", true)
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        currentOpenChatId = chatId
    }

    override fun onPause() {
        super.onPause()
        if (currentOpenChatId == chatId) {
            currentOpenChatId = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        messageListener?.remove()
    }

    companion object {
        var currentOpenChatId: String? = null
    }
}
