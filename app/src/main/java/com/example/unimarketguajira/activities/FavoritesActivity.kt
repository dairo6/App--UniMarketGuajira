package com.example.unimarketguajira.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.unimarketguajira.R
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.repository.CartRepository
import com.example.unimarketguajira.repository.ProductRepository
import com.example.unimarketguajira.services.UserManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var rvFavorites: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var adapter: FavoritesAdapter
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_favorites)

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

        rvFavorites = findViewById(R.id.rvFavorites)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        rvFavorites.layoutManager = LinearLayoutManager(this)
        adapter = FavoritesAdapter()
        rvFavorites.adapter = adapter

        userEmail = UserManager.getLoggedUserEmail(this) ?: ""
        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Usuario no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    private fun loadFavorites() {
        lifecycleScope.launch {
            val favoritesList = ProductRepository.getFavoritesForUser(this@FavoritesActivity, userEmail)
            adapter.setData(favoritesList)
            layoutEmpty.visibility = if (favoritesList.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    inner class FavoritesAdapter : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {
        private var items = listOf<Product>()

        fun setData(newItems: List<Product>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite, parent, false)
            return FavoriteViewHolder(view)
        }

        override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
            private val tvUnavailableOverlay: TextView = itemView.findViewById(R.id.tvUnavailableOverlay)
            private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
            private val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
            private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
            private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
            private val ibFavorite: ImageButton = itemView.findViewById(R.id.ibFavorite)
            private val btnAddToCart: MaterialButton = itemView.findViewById(R.id.btnAddToCart)

            fun bind(product: Product) {
                tvProductName.text = product.name
                tvProductPrice.text = "$ ${product.price}"
                tvCategory.text = product.category
                tvLocation.text = product.location

                // Load image
                val imageUrl = if (product.imageUrls.isNotEmpty()) product.imageUrls[0] else ""
                Glide.with(itemView.context)
                    .load(if (imageUrl.isNotEmpty()) imageUrl else android.R.drawable.ic_menu_gallery)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivProductImage)

                // Check availability
                val isUnavailable = product.status == "INACTIVE" || product.status == "SOLD"
                
                if (isUnavailable) {
                    tvUnavailableOverlay.visibility = View.VISIBLE
                    if (product.status == "INACTIVE") {
                        tvUnavailableOverlay.text = "Eliminado por el vendedor"
                    } else {
                        tvUnavailableOverlay.text = "No disponible"
                    }
                    itemView.alpha = 0.5f
                    val matrix = android.graphics.ColorMatrix()
                    matrix.setSaturation(0f)
                    ivProductImage.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
                    
                    itemView.setOnClickListener(null)
                    itemView.isClickable = false
                } else {
                    tvUnavailableOverlay.visibility = View.GONE
                    itemView.alpha = 1.0f
                    ivProductImage.colorFilter = null
                    
                    itemView.isClickable = true
                    itemView.setOnClickListener {
                        val intent = Intent(itemView.context, ProductDetailActivity::class.java).apply {
                            putExtra("PRODUCT_ID", product.id)
                        }
                        itemView.context.startActivity(intent)
                    }
                }

                // Remove from favorites
                ibFavorite.setOnClickListener {
                    lifecycleScope.launch {
                        ProductRepository.toggleFavorite(itemView.context, product.id, false)
                        Toast.makeText(itemView.context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                        loadFavorites()
                    }
                }

                // Add to cart
                if (isUnavailable) {
                    btnAddToCart.isEnabled = false
                    btnAddToCart.alpha = 0.5f
                    btnAddToCart.setOnClickListener(null)
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
