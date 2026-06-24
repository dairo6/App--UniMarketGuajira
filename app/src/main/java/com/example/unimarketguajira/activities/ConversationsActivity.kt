package com.example.unimarketguajira.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.unimarketguajira.R
import com.example.unimarketguajira.adapters.ConversationAdapter
import com.example.unimarketguajira.data.db.UniMarketDatabase
import com.example.unimarketguajira.data.entities.ChatRoomEntity
import com.example.unimarketguajira.models.ChatRoom
import com.example.unimarketguajira.repository.ChatRepository
import com.example.unimarketguajira.services.UserManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class ConversationsActivity : AppCompatActivity() {

    private lateinit var rvConversations: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tabLayoutFilters: TabLayout
    private lateinit var layoutEmptyState: View

    private lateinit var adapter: ConversationAdapter
    private var conversationsList: List<ChatRoom> = emptyList()
    private var currentUserEmail: String = ""
    private var currentFilter = "Todas"

    private var compradorListener: ListenerRegistration? = null
    private var vendedorListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_conversations)

        val appBarLayout = findViewById<View>(R.id.appBarLayout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            appBarLayout.setPadding(0, systemBars.top, 0, 0)
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Toolbar setup
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Binding
        rvConversations = findViewById(R.id.rvConversations)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        tabLayoutFilters = findViewById(R.id.tabLayoutFilters)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)

        currentUserEmail = UserManager.getLoggedUserEmail(this) ?: ""
        if (currentUserEmail.isEmpty()) {
            Toast.makeText(this, "Inicia sesión para ver tus mensajes", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        rvConversations.layoutManager = LinearLayoutManager(this)
        adapter = ConversationAdapter(
            context = this,
            chatRooms = emptyList(),
            currentUserEmail = currentUserEmail,
            onItemClick = { chatRoom ->
                // Abrir pantalla de chat
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("CHAT_ID", chatRoom.id)
                    putExtra("ID_COMPRADOR", chatRoom.id_comprador)
                    putExtra("ID_VENDEDOR", chatRoom.id_vendedor)
                    putExtra("ID_PRODUCTO", chatRoom.id_producto)
                    putExtra("PRODUCT_NAME", "Cargando...")
                }
                startActivity(intent)
            },
            onArchiveClick = { chatRoom ->
                // Archivar / Desarchivar conversación
                lifecycleScope.launch {
                    val newArchived = chatRoom.status != "ARCHIVED"
                    ChatRepository.archiveChatRoom(this@ConversationsActivity, chatRoom.id, newArchived)
                    Toast.makeText(
                        this@ConversationsActivity,
                        if (newArchived) "Conversación archivada" else "Conversación restaurada",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadLocalConversations()
                }
            }
        )
        rvConversations.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            refreshConversations()
        }

        setupFilters()
        startRealtimeListeners()
        loadLocalConversations()
    }

    private fun setupFilters() {
        tabLayoutFilters.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilter = tab?.text?.toString() ?: "Todas"
                filterAndDisplay()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun startRealtimeListeners() {
        val db = FirebaseFirestore.getInstance()

        // Listener para chats como comprador
        compradorListener = db.collection("chats")
            .whereEqualTo("id_comprador", currentUserEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
                    }
                    lifecycleScope.launch {
                        // Guardar en Room
                        UniMarketDatabase.getDatabase(this@ConversationsActivity)
                            .chatRoomDao().insertAll(chats.map { ChatRoomEntity.fromModel(it) })
                        loadLocalConversations()
                    }
                }
            }

        // Listener para chats como vendedor
        vendedorListener = db.collection("chats")
            .whereEqualTo("id_vendedor", currentUserEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
                    }
                    lifecycleScope.launch {
                        // Guardar en Room
                        UniMarketDatabase.getDatabase(this@ConversationsActivity)
                            .chatRoomDao().insertAll(chats.map { ChatRoomEntity.fromModel(it) })
                        loadLocalConversations()
                    }
                }
            }
    }

    private fun loadLocalConversations() {
        lifecycleScope.launch {
            val localDao = UniMarketDatabase.getDatabase(this@ConversationsActivity).chatRoomDao()
            val localEntities = localDao.getChatRoomsForUser(currentUserEmail)
            conversationsList = localEntities.map { it.toModel() }.sortedByDescending { it.timestamp }
            filterAndDisplay()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun filterAndDisplay() {
        val filtered = when (currentFilter) {
            "Todas" -> conversationsList.filter { it.status == "ACTIVE" }
            "Compras" -> conversationsList.filter { it.id_comprador == currentUserEmail && it.status == "ACTIVE" }
            "Ventas" -> conversationsList.filter { it.id_vendedor == currentUserEmail && it.status == "ACTIVE" }
            "Archivadas" -> conversationsList.filter { it.status == "ARCHIVED" }
            else -> conversationsList.filter { it.status == "ACTIVE" }
        }

        adapter.updateData(filtered)

        if (filtered.isEmpty()) {
            layoutEmptyState.visibility = View.VISIBLE
            rvConversations.visibility = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            rvConversations.visibility = View.VISIBLE
        }
    }

    private fun refreshConversations() {
        lifecycleScope.launch {
            try {
                ChatRepository.getChatRoomsForUser(this@ConversationsActivity, currentUserEmail)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            loadLocalConversations()
        }
    }

    override fun onResume() {
        super.onResume()
        loadLocalConversations()
    }

    override fun onDestroy() {
        super.onDestroy()
        compradorListener?.remove()
        vendedorListener?.remove()
    }
}
