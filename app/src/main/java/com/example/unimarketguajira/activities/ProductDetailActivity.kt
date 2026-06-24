package com.example.unimarketguajira.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import android.content.Intent
import com.example.unimarketguajira.R
import com.example.unimarketguajira.adapters.ImageCarouselAdapter
import com.example.unimarketguajira.adapters.HorizontalProductAdapter
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.repository.ProductRepository
import com.example.unimarketguajira.repository.ChatRepository
import com.example.unimarketguajira.services.UserManager
import com.example.unimarketguajira.repository.CartRepository
import com.example.unimarketguajira.repository.PurchaseRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.unimarketguajira.data.db.UniMarketDatabase
import java.text.NumberFormat
import java.util.Locale
import android.widget.Toast

class ProductDetailActivity : AppCompatActivity() {

    private var currentProduct: Product? = null
    private var cartMenu: android.view.Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_product_detail)

        val appBarLayout = findViewById<View>(R.id.appBarLayout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            appBarLayout.setPadding(0, systemBars.top, 0, 0)
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupToolbar()
        loadProductData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun loadProductData() {
        val productId = intent.getIntExtra("PRODUCT_ID", 0)
        
        // Cargar fallback inicial desde los extras del intent
        val name = intent.getStringExtra("PRODUCT_NAME") ?: "Producto"
        val price = intent.getDoubleExtra("PRODUCT_PRICE", 0.0)
        val description = intent.getStringExtra("PRODUCT_DESCRIPTION") ?: "Sin descripción disponible."
        val location = intent.getStringExtra("PRODUCT_LOCATION") ?: "Ubicación no especificada"
        val condition = intent.getStringExtra("PRODUCT_CONDITION") ?: "No especificado"
        val imagePath = intent.getStringExtra("PRODUCT_IMAGE")
        val category = intent.getStringExtra("PRODUCT_CATEGORY") ?: "Otros"
        val isFavoriteIntent = intent.getBooleanExtra("PRODUCT_FAVORITE", false)

        // Asignar datos iniciales rápidos a la UI
        findViewById<TextView>(R.id.tvDetailName).text = name
        findViewById<TextView>(R.id.tvDetailDescription).text = description
        findViewById<TextView>(R.id.tvDetailLocation).text = location
        findViewById<TextView>(R.id.tvDetailCondition).text = condition

        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        formatter.maximumFractionDigits = 0
        findViewById<TextView>(R.id.tvDetailPrice).text = formatter.format(price)

        // Galería fallback inicial
        val initialImages = if (imagePath.isNullOrEmpty()) emptyList() else listOf(imagePath)
        setupGallery(initialImages)

        // Cargar datos reales y completos desde base de datos de manera asíncrona
        lifecycleScope.launch {
            val dbProduct = ProductRepository.getProductById(this@ProductDetailActivity, productId)
            if (dbProduct != null) {
                currentProduct = dbProduct
                
                // Guardar en el historial de vistos recientemente
                val loggedEmail = UserManager.getLoggedUserEmail(this@ProductDetailActivity)
                if (loggedEmail != null) {
                    ProductRepository.addProductToHistory(this@ProductDetailActivity, productId, loggedEmail)
                }

                // Actualizar UI con datos reales completos
                findViewById<TextView>(R.id.tvDetailName).text = dbProduct.name
                findViewById<TextView>(R.id.tvDetailDescription).text = dbProduct.description
                findViewById<TextView>(R.id.tvDetailLocation).text = dbProduct.location
                findViewById<TextView>(R.id.tvDetailCondition).text = dbProduct.condition
                findViewById<TextView>(R.id.tvDetailPrice).text = formatter.format(dbProduct.price)

                setupGallery(dbProduct.imageUrls)
                setupSellerInfo(dbProduct.ownerEmail, dbProduct.id, dbProduct.name)
                setupMoreSellerProducts(dbProduct.ownerEmail, dbProduct.id)
                setupFavoriteStatus(dbProduct)
            } else {
                // Si no se encuentra en BD local, usar fallback
                setupSellerInfo("admin@unimarket.com", productId, name)
                setupMoreSellerProducts("admin@unimarket.com", productId)
                val fallbackProduct = Product(productId, name, description, price, location, initialImages, category, condition, isFavoriteIntent)
                setupFavoriteStatus(fallbackProduct)
            }
        }

        // Añadir a Carrito
        findViewById<View>(R.id.btnAddToCart).setOnClickListener {
            val productToBuy = currentProduct ?: Product(
                id = productId,
                name = name,
                description = description,
                price = price,
                location = location,
                imageUrls = initialImages,
                category = category,
                condition = condition,
                isFavorite = isFavoriteIntent,
                stock = 1
            )
            lifecycleScope.launch {
                val email = UserManager.getLoggedUserEmail(this@ProductDetailActivity) ?: return@launch
                val currentCart = CartRepository.getCartItems(this@ProductDetailActivity, email)
                val countInCart = currentCart.count { it.id == productToBuy.id }
                if (countInCart >= productToBuy.stock) {
                    Toast.makeText(this@ProductDetailActivity, "Stock máximo alcanzado", Toast.LENGTH_SHORT).show()
                } else {
                    CartRepository.addToCart(this@ProductDetailActivity, productToBuy, email)
                    Toast.makeText(this@ProductDetailActivity, "¡Agregado al carrito!", Toast.LENGTH_SHORT).show()
                    updateCartBadgeCount()
                }
            }
        }
    }

    private fun setupGallery(imageUrls: List<String>) {
        val vpProductImages = findViewById<ViewPager2>(R.id.vpProductImages)
        val cardImageIndicator = findViewById<View>(R.id.cardImageIndicator)
        val tvImageIndicator = findViewById<TextView>(R.id.tvImageIndicator)

        fun getDotsIndicatorString(total: Int, selected: Int): String {
            val sb = StringBuilder()
            for (i in 0 until total) {
                if (i == selected) {
                    sb.append("●")
                } else {
                    sb.append("○")
                }
                if (i < total - 1) sb.append(" ")
            }
            return sb.toString()
        }

        if (imageUrls.isEmpty()) {
            val placeholder = listOf(R.drawable.ic_launcher_background.toString())
            vpProductImages.adapter = ImageCarouselAdapter(placeholder)
            cardImageIndicator.visibility = View.GONE
        } else if (imageUrls.size == 1) {
            vpProductImages.adapter = ImageCarouselAdapter(imageUrls)
            cardImageIndicator.visibility = View.GONE
        } else {
            vpProductImages.adapter = ImageCarouselAdapter(imageUrls)
            cardImageIndicator.visibility = View.VISIBLE
            
            tvImageIndicator.text = getDotsIndicatorString(imageUrls.size, 0)
            
            vpProductImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    tvImageIndicator.text = getDotsIndicatorString(imageUrls.size, position)
                }
            })
        }
    }

    private fun setupSellerInfo(ownerEmail: String, productId: Int, productName: String) {
        val tvSellerName = findViewById<TextView>(R.id.tvSellerName)
        val tvSellerEmail = findViewById<TextView>(R.id.tvSellerEmail)
        
        // Valores por defecto
        val fallbackName = ownerEmail.split("@").firstOrNull()?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Vendedor"
        tvSellerName.text = fallbackName
        tvSellerEmail.text = ownerEmail
        
        lifecycleScope.launch {
            val seller = UserManager.getUserByEmail(this@ProductDetailActivity, ownerEmail)
            if (seller != null) {
                tvSellerName.text = seller.fullName
                tvSellerEmail.text = seller.email
            }

            // Verificar si ya existe una conversación activa para este producto
            val loggedEmail = UserManager.getLoggedUserEmail(this@ProductDetailActivity)
            if (loggedEmail != null && loggedEmail != ownerEmail) {
                try {
                    var exists = false
                    if (com.example.unimarketguajira.utils.NetworkUtils.isNetworkAvailable(this@ProductDetailActivity)) {
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val snap = db.collection("chats")
                            .whereEqualTo("id_comprador", loggedEmail)
                            .whereEqualTo("id_vendedor", ownerEmail)
                            .whereEqualTo("id_producto", productId)
                            .get()
                            .await()
                        exists = !snap.isEmpty
                    }
                    if (!exists) {
                        val localDao = UniMarketDatabase.getDatabase(this@ProductDetailActivity).chatRoomDao()
                        val localChats = localDao.getChatRoomsForUser(loggedEmail)
                        exists = localChats.any {
                            it.id_comprador == loggedEmail && it.id_vendedor == ownerEmail && it.id_producto == productId
                        }
                    }
                    if (exists) {
                        findViewById<View>(R.id.cardActiveChatBanner).visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Configurar botón de contacto en barra fija inferior
        findViewById<View>(R.id.btnContactSeller).setOnClickListener {
            val loggedEmail = UserManager.getLoggedUserEmail(this@ProductDetailActivity)
            if (loggedEmail == null) {
                android.widget.Toast.makeText(this@ProductDetailActivity, "Debes iniciar sesión para contactar al vendedor", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (loggedEmail == ownerEmail) {
                android.widget.Toast.makeText(this@ProductDetailActivity, "No puedes chatear contigo mismo", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val progressDialog = android.app.ProgressDialog(this@ProductDetailActivity).apply {
                        setMessage("Conectando con el vendedor...")
                        setCancelable(false)
                        show()
                    }
                    val chatRoom = ChatRepository.getOrCreateChatRoom(
                        context = this@ProductDetailActivity,
                        idComprador = loggedEmail,
                        idVendedor = ownerEmail,
                        idProducto = productId
                    )
                    progressDialog.dismiss()

                    val intent = Intent(this@ProductDetailActivity, ChatActivity::class.java).apply {
                        putExtra("CHAT_ID", chatRoom.id)
                        putExtra("ID_COMPRADOR", chatRoom.id_comprador)
                        putExtra("ID_VENDEDOR", chatRoom.id_vendedor)
                        putExtra("ID_PRODUCTO", chatRoom.id_producto)
                        putExtra("PRODUCT_NAME", productName)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this@ProductDetailActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupMoreSellerProducts(ownerEmail: String, currentProductId: Int) {
        val cardMoreSellerProducts = findViewById<View>(R.id.cardMoreSellerProducts)
        val rvMoreSellerProducts = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvMoreSellerProducts)

        lifecycleScope.launch {
            val allSellerProducts = ProductRepository.getMyProducts(this@ProductDetailActivity, ownerEmail)
            val otherSellerProducts = allSellerProducts.filter { it.id != currentProductId && it.status == "ACTIVE" }

            if (otherSellerProducts.isNotEmpty()) {
                cardMoreSellerProducts.visibility = View.VISIBLE
                val adapter = HorizontalProductAdapter(this@ProductDetailActivity, otherSellerProducts)
                rvMoreSellerProducts.adapter = adapter
            } else {
                cardMoreSellerProducts.visibility = View.GONE
            }
        }
    }

    private fun setupFavoriteStatus(product: Product) {
        val ibFavorite = findViewById<ImageButton>(R.id.ibDetailFavorite)
        var isFavorite = product.isFavorite

        fun updateFavoriteIcon() {
            val iconRes = if (isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
            ibFavorite.setImageResource(iconRes)
        }

        updateFavoriteIcon()

        ibFavorite.setOnClickListener {
            isFavorite = !isFavorite
            updateFavoriteIcon()

            lifecycleScope.launch {
                ProductRepository.toggleFavorite(this@ProductDetailActivity, product.id, isFavorite)
                val msg = if (isFavorite) "Guardado en favoritos" else "Eliminado de favoritos"
                android.widget.Toast.makeText(this@ProductDetailActivity, msg, android.widget.Toast.LENGTH_SHORT).show()

                if (isFavorite && product.ownerEmail.isNotEmpty()) {
                    val loggedEmail = UserManager.getLoggedUserEmail(this@ProductDetailActivity)
                    // No notificarse a sí mismo
                    if (loggedEmail != product.ownerEmail) {
                        PurchaseRepository.createSystemNotification(
                            context = this@ProductDetailActivity,
                            userId = product.ownerEmail,
                            title = "Favorito guardado",
                            message = "Un estudiante guardó tu publicación \"${product.name}\"",
                            type = "FAVORITO",
                            relatedProductId = product.id
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateCartBadgeCount()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_cart, menu)
        cartMenu = menu

        val cartItem = menu.findItem(R.id.action_cart)
        val actionView = cartItem?.actionView
        actionView?.findViewById<View>(R.id.cart_badge_container)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("OPEN_CART", true)
                putExtra("CALLING_ACTIVITY", this@ProductDetailActivity::class.java.name)
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        updateCartBadgeCount()
        return true
    }

    private fun updateCartBadgeCount() {
        lifecycleScope.launch {
            val email = UserManager.getLoggedUserEmail(this@ProductDetailActivity) ?: return@launch
            val cartItems = CartRepository.getCartItems(this@ProductDetailActivity, email)
            val totalQuantity = cartItems.size

            val menu = cartMenu ?: return@launch
            val cartItem = menu.findItem(R.id.action_cart) ?: return@launch
            val actionView = cartItem.actionView ?: return@launch
            val badgeCard = actionView.findViewById<View>(R.id.cardCartBadge)
            val badgeText = actionView.findViewById<TextView>(R.id.tvCartBadge)

            if (totalQuantity > 0) {
                badgeText.text = totalQuantity.toString()
                badgeCard.visibility = View.VISIBLE
            } else {
                badgeCard.visibility = View.GONE
            }
        }
    }
}