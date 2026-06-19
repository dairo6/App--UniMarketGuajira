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
import com.example.unimarketguajira.R
import com.example.unimarketguajira.adapters.ImageCarouselAdapter
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.repository.ProductRepository
import com.example.unimarketguajira.services.UserManager
import com.example.unimarketguajira.repository.CartRepository
import com.example.unimarketguajira.repository.PurchaseRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {

    private var currentProduct: Product? = null

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
                setupSellerInfo(dbProduct.ownerEmail)
                setupFavoriteStatus(dbProduct)
            } else {
                // Si no se encuentra en BD local, usar fallback
                setupSellerInfo("admin@unimarket.com")
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
                isFavorite = isFavoriteIntent
            )
            lifecycleScope.launch {
                val email = UserManager.getLoggedUserEmail(this@ProductDetailActivity) ?: return@launch
                CartRepository.addToCart(this@ProductDetailActivity, productToBuy, email)
                android.widget.Toast.makeText(this@ProductDetailActivity, "¡Agregado al carrito!", android.widget.Toast.LENGTH_SHORT).show()
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

    private fun setupSellerInfo(ownerEmail: String) {
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
        }

        // Configurar botones de contacto
        val contactAction = View.OnClickListener {
            val email = tvSellerEmail.text.toString()
            val name = tvSellerName.text.toString()
            
            androidx.appcompat.app.AlertDialog.Builder(this@ProductDetailActivity)
                .setTitle("Contacto del Vendedor")
                .setMessage("Nombre: $name\nCorreo: $email\n\nEl chat interno estará disponible en la próxima actualización.")
                .setPositiveButton("Aceptar", null)
                .show()
        }
        findViewById<View>(R.id.btnContactSeller).setOnClickListener(contactAction)
        findViewById<View>(R.id.btnContactSellerInline).setOnClickListener(contactAction)
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
}