package com.example.unimarketguajira.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.unimarketguajira.R
import com.example.unimarketguajira.data.db.UniMarketDatabase
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.repository.CartRepository
import com.example.unimarketguajira.repository.ProductRepository
import com.example.unimarketguajira.repository.PurchaseRepository
import com.example.unimarketguajira.services.UserManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ViewHistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var chipGroupFilters: ChipGroup

    private lateinit var adapter: HistoryAdapter
    private var userEmail: String = ""
    private var rawHistoryItems: List<HistoryItem> = emptyList()

    data class HistoryItem(
        val product: Product,
        val viewedAt: Long
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_history)

        val appBarLayout = findViewById<View>(R.id.appBarLayout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            appBarLayout.setPadding(0, systemBars.top, 0, 0)
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Views
        rvHistory = findViewById(R.id.rvHistory)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        chipGroupFilters = findViewById(R.id.chipGroupFilters)

        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter()
        rvHistory.adapter = adapter

        userEmail = UserManager.getLoggedUserEmail(this) ?: ""
        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadHistory(forceRefresh = true)
        }

        chipGroupFilters.setOnCheckedChangeListener { _, _ ->
            applyFilters()
        }

        loadHistory(forceRefresh = false)
    }

    override fun onResume() {
        super.onResume()
        loadHistory(forceRefresh = false)
    }

    private fun loadHistory(forceRefresh: Boolean) {
        if (!forceRefresh) {
            swipeRefreshLayout.isRefreshing = true
        }
        lifecycleScope.launch {
            try {
                // Sincronizar historial con Firebase
                ProductRepository.getProductHistory(this@ViewHistoryActivity, userEmail)

                // Obtener registros con timestamp desde Room
                val db = UniMarketDatabase.getDatabase(this@ViewHistoryActivity)
                val entities = db.productViewHistoryDao().getViewHistory(userEmail)

                val items = mutableListOf<HistoryItem>()
                for (entity in entities) {
                    val product = ProductRepository.getProductById(this@ViewHistoryActivity, entity.productId)
                    if (product != null) {
                        items.add(HistoryItem(product, entity.viewedAt))
                    }
                }
                rawHistoryItems = items
                applyFilters()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ViewHistoryActivity, "Error al cargar el historial", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun applyFilters() {
        val checkedId = chipGroupFilters.checkedChipId
        val now = System.currentTimeMillis()
        
        val filtered = when (checkedId) {
            R.id.chipToday -> rawHistoryItems.filter { it.viewedAt >= now - (24L * 60 * 60 * 1000) }
            R.id.chipWeek -> rawHistoryItems.filter { it.viewedAt >= now - (7L * 24L * 60 * 60 * 1000) }
            R.id.chipMonth -> rawHistoryItems.filter { it.viewedAt >= now - (30L * 24L * 60 * 60 * 1000) }
            else -> rawHistoryItems // Todos
        }

        adapter.setData(filtered)

        if (filtered.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear_history) {
            confirmClearHistory()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirmClearHistory() {
        if (rawHistoryItems.isEmpty()) {
            Toast.makeText(this, "El historial ya está vacío", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Limpiar Historial")
            .setMessage("¿Estás seguro de que deseas borrar todo tu historial de productos vistos?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Borrar todo") { _, _ ->
                lifecycleScope.launch {
                    try {
                        ProductRepository.clearHistory(this@ViewHistoryActivity, userEmail)
                        Toast.makeText(this@ViewHistoryActivity, "Historial borrado", Toast.LENGTH_SHORT).show()
                        loadHistory(forceRefresh = true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@ViewHistoryActivity, "Error al limpiar el historial", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    inner class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
        private var items = listOf<HistoryItem>()
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

        fun setData(newItems: List<HistoryItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
            return HistoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
            private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
            private val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
            private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
            private val tvViewedDate: TextView = itemView.findViewById(R.id.tvViewedDate)
            private val ibDelete: ImageButton = itemView.findViewById(R.id.ibDelete)
            private val ibFavorite: ImageButton = itemView.findViewById(R.id.ibFavorite)
            private val btnAddToCart: MaterialButton = itemView.findViewById(R.id.btnAddToCart)

            fun bind(item: HistoryItem) {
                val product = item.product
                tvProductName.text = product.name
                tvProductPrice.text = "$ ${product.price}"
                tvCategory.text = product.category
                tvViewedDate.text = "Visto: ${dateFormat.format(Date(item.viewedAt))}"

                // Load image
                val imageUrl = if (product.imageUrls.isNotEmpty()) product.imageUrls[0] else ""
                Glide.with(itemView.context)
                    .load(if (imageUrl.isNotEmpty()) imageUrl else android.R.drawable.ic_menu_gallery)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivProductImage)

                // Favorite Icon
                ibFavorite.setImageResource(
                    if (product.isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
                )

                // Click to view product
                itemView.setOnClickListener {
                    val intent = Intent(itemView.context, ProductDetailActivity::class.java).apply {
                        putExtra("PRODUCT_ID", product.id)
                    }
                    itemView.context.startActivity(intent)
                }

                // Delete item
                ibDelete.setOnClickListener {
                    lifecycleScope.launch {
                        ProductRepository.deleteHistoryItem(itemView.context, product.id, userEmail)
                        Toast.makeText(itemView.context, "Eliminado del historial", Toast.LENGTH_SHORT).show()
                        loadHistory(forceRefresh = false)
                    }
                }

                // Toggle favorite
                ibFavorite.setOnClickListener {
                    lifecycleScope.launch {
                        val newFavoriteState = !product.isFavorite
                        ProductRepository.toggleFavorite(itemView.context, product.id, newFavoriteState)
                        val msg = if (newFavoriteState) "Agregado a favoritos" else "Eliminado de favoritos"
                        Toast.makeText(itemView.context, msg, Toast.LENGTH_SHORT).show()
                        
                        // Notificación si se agrega favorito
                        if (newFavoriteState) {
                            PurchaseRepository.createSystemNotification(
                                context = itemView.context,
                                userId = userEmail,
                                title = "Nuevo favorito agregado",
                                message = "Agregaste \"${product.name}\" a tus favoritos.",
                                type = "FAVORITOS",
                                relatedProductId = product.id
                            )
                        }
                        
                        loadHistory(forceRefresh = false)
                    }
                }

                // Add to cart
                val isUnavailable = product.status == "INACTIVE" || product.status == "SOLD"
                if (isUnavailable) {
                    btnAddToCart.isEnabled = false
                    btnAddToCart.alpha = 0.5f
                } else {
                    btnAddToCart.isEnabled = true
                    btnAddToCart.alpha = 1.0f
                    btnAddToCart.setOnClickListener {
                        lifecycleScope.launch {
                            CartRepository.addToCart(itemView.context, product, userEmail)
                            Toast.makeText(itemView.context, "¡Agregado al carrito!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
