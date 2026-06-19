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
import com.example.unimarketguajira.adapters.NotificationAdapter
import com.example.unimarketguajira.models.Notification
import com.example.unimarketguajira.repository.PurchaseRepository
import com.example.unimarketguajira.services.UserManager
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var layoutEmptyState: View
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tabLayoutFilters: TabLayout

    private lateinit var adapter: NotificationAdapter
    private var notificationsList: List<Notification> = emptyList()
    private var userEmail: String = ""
    private var currentFilter = "Todas"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

        val appBarLayout = findViewById<View>(R.id.appBarLayout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            appBarLayout.setPadding(0, systemBars.top, 0, 0)
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Toolbar Setup
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // View Binding
        rvNotifications = findViewById(R.id.rvNotifications)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        tabLayoutFilters = findViewById(R.id.tabLayoutFilters)

        rvNotifications.layoutManager = LinearLayoutManager(this)

        adapter = NotificationAdapter(this, emptyList()) { notification ->
            handleNotificationClick(notification)
        }
        rvNotifications.adapter = adapter

        userEmail = UserManager.getLoggedUserEmail(this) ?: ""
        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Sesión no válida. Inicia sesión de nuevo.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadNotifications(forceRefresh = true)
        }

        setupFilters()
        loadNotifications(forceRefresh = false)
    }

    private fun loadNotifications(forceRefresh: Boolean) {
        if (!forceRefresh) {
            swipeRefreshLayout.isRefreshing = true
        }
        lifecycleScope.launch {
            try {
                notificationsList = PurchaseRepository.getNotificationsForUser(this@NotificationsActivity, userEmail)
                filterAndDisplay()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@NotificationsActivity, "Error al cargar las notificaciones", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
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

    private fun filterAndDisplay() {
        val filtered = when (currentFilter) {
            "Todas" -> notificationsList
            "Ventas" -> notificationsList.filter { it.type == "VENTA" }
            "Compras" -> notificationsList.filter { it.type == "COMPRA" }
            "Favoritos" -> notificationsList.filter { it.type == "FAVORITO" }
            "Sistema" -> notificationsList.filter { it.type == "SISTEMA" }
            else -> notificationsList
        }

        adapter.updateData(filtered)

        if (filtered.isEmpty()) {
            layoutEmptyState.visibility = View.VISIBLE
            rvNotifications.visibility = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            rvNotifications.visibility = View.VISIBLE
        }
    }

    private fun handleNotificationClick(notification: Notification) {
        lifecycleScope.launch {
            if (!notification.isRead) {
                try {
                    PurchaseRepository.markNotificationAsRead(this@NotificationsActivity, notification.id)
                    // Actualizar en memoria
                    notificationsList = notificationsList.map {
                        if (it.id == notification.id) it.copy(isRead = true) else it
                    }
                    filterAndDisplay()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Si hay un producto relacionado, abrir el detalle
            if (notification.relatedProductId > 0) {
                val intent = Intent(this@NotificationsActivity, ProductDetailActivity::class.java).apply {
                    putExtra("PRODUCT_ID", notification.relatedProductId)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this@NotificationsActivity, "Notificación leída", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
