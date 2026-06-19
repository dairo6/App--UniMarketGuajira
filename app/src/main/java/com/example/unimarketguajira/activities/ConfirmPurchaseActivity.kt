package com.example.unimarketguajira.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
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
import com.example.unimarketguajira.R
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.models.Purchase
import com.example.unimarketguajira.models.loadProductImage
import com.example.unimarketguajira.repository.CartRepository
import com.example.unimarketguajira.repository.PurchaseRepository
import com.example.unimarketguajira.services.UserManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ConfirmPurchaseActivity : AppCompatActivity() {

    private lateinit var rvConfirmProducts: RecyclerView
    private lateinit var spinnerDeliveryPoint: AutoCompleteTextView
    private lateinit var spinnerPaymentMethod: AutoCompleteTextView
    private lateinit var etDeliveryNotes: TextInputEditText
    private lateinit var tvConfirmTotal: TextView
    private lateinit var btnPlaceOrder: Button

    private var groupedCartItems: List<CartItem> = emptyList()
    private var buyerEmail = ""
    private var buyerName = ""

    data class CartItem(val product: Product, val quantity: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_confirm_purchase)

        val appBarLayout = findViewById<View>(R.id.appBarLayout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            appBarLayout.setPadding(0, systemBars.top, 0, 0)
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupToolbar()

        rvConfirmProducts = findViewById(R.id.rvConfirmProducts)
        spinnerDeliveryPoint = findViewById(R.id.spinnerDeliveryPoint)
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod)
        etDeliveryNotes = findViewById(R.id.etDeliveryNotes)
        tvConfirmTotal = findViewById(R.id.tvConfirmTotal)
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder)

        rvConfirmProducts.layoutManager = LinearLayoutManager(this)

        buyerEmail = UserManager.getLoggedUserEmail(this) ?: ""
        if (buyerEmail.isEmpty()) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupSpinners()
        loadCartData()

        btnPlaceOrder.setOnClickListener {
            handlePlaceOrder()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupSpinners() {
        // Puntos de encuentro universitarios
        val deliveryPoints = arrayOf("Biblioteca", "Bloque de Ingeniería", "Cafetería", "Bienestar Universitario", "Laboratorios", "Otro")
        val deliveryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, deliveryPoints)
        spinnerDeliveryPoint.setAdapter(deliveryAdapter)

        // Métodos de pago
        val paymentMethods = arrayOf("Pago presencial al recibir", "Transferencia bancaria", "Nequi", "DaviPlata")
        val paymentAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, paymentMethods)
        spinnerPaymentMethod.setAdapter(paymentAdapter)
    }

    private fun loadCartData() {
        lifecycleScope.launch {
            buyerName = UserManager.getLoggedUser(this@ConfirmPurchaseActivity)?.fullName ?: "Estudiante"
            val rawProducts = CartRepository.getCartItems(this@ConfirmPurchaseActivity, buyerEmail)
            
            groupedCartItems = rawProducts.groupBy { it.id }.map { (_, list) ->
                CartItem(list.first(), list.size)
            }

            val total = CartRepository.getTotal(this@ConfirmPurchaseActivity, buyerEmail)
            
            rvConfirmProducts.adapter = ConfirmProductsAdapter(groupedCartItems)
            
            val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            format.maximumFractionDigits = 0
            tvConfirmTotal.text = format.format(total)
        }
    }

    private fun handlePlaceOrder() {
        val deliveryPoint = spinnerDeliveryPoint.text.toString().trim()
        val paymentMethod = spinnerPaymentMethod.text.toString().trim()
        val notes = etDeliveryNotes.text.toString().trim()

        if (deliveryPoint.isEmpty() || paymentMethod.isEmpty()) {
            Toast.makeText(this, "Completa el punto de encuentro y el método de pago", Toast.LENGTH_SHORT).show()
            return
        }

        if (groupedCartItems.isEmpty()) {
            Toast.makeText(this, "No hay productos que comprar", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Guardar transacciones y notificar a cada vendedor involucrado
            for (item in groupedCartItems) {
                val purchaseId = "purchase_${System.currentTimeMillis()}_${(0..1000).random()}"
                val finalDeliveryPoint = if (notes.isNotEmpty()) "$deliveryPoint ($notes)" else deliveryPoint
                
                val purchase = Purchase(
                    id = purchaseId,
                    productId = item.product.id,
                    sellerId = item.product.ownerEmail,
                    buyerId = buyerEmail,
                    price = item.product.price,
                    quantity = item.quantity,
                    purchaseDate = System.currentTimeMillis(),
                    deliveryPoint = finalDeliveryPoint,
                    paymentMethod = paymentMethod,
                    status = "PENDING"
                )

                PurchaseRepository.createPurchase(
                    context = this@ConfirmPurchaseActivity,
                    purchase = purchase,
                    buyerName = buyerName,
                    productName = item.product.name
                )
            }

            // Vaciar el carrito
            CartRepository.clearCart(this@ConfirmPurchaseActivity, buyerEmail)
            
            Toast.makeText(this@ConfirmPurchaseActivity, "¡Compra confirmada! El vendedor ha sido notificado.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // ── Adapter Interno de Lectura ─────────────────────────────────────────────

    inner class ConfirmProductsAdapter(private val items: List<CartItem>) : RecyclerView.Adapter<ConfirmProductsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val cartItem = items[position]
            val product = cartItem.product

            holder.tvName.text = product.name
            holder.tvCategory.text = product.category
            holder.tvCondition.text = product.condition
            holder.tvLocation.text = product.location

            val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            format.maximumFractionDigits = 0
            holder.tvPrice.text = format.format(product.price)

            holder.ivImage.loadProductImage(product.imageUrls.firstOrNull())
            holder.tvQtyValue.text = cartItem.quantity.toString()

            // Ocultar botones de incremento, decremento y eliminación
            holder.btnQtyDecrease.visibility = View.GONE
            holder.btnQtyIncrease.visibility = View.GONE
            holder.btnRemove.visibility = View.GONE
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivImage: ImageView = view.findViewById(R.id.ivProductImage)
            val tvName: TextView = view.findViewById(R.id.tvProductName)
            val tvCategory: TextView = view.findViewById(R.id.tvProductCategory)
            val tvCondition: TextView = view.findViewById(R.id.tvProductCondition)
            val tvLocation: TextView = view.findViewById(R.id.tvProductLocation)
            val tvPrice: TextView = view.findViewById(R.id.tvProductPrice)
            val tvQtyValue: TextView = view.findViewById(R.id.tvQtyValue)
            val btnQtyDecrease: View = view.findViewById(R.id.btnQtyDecrease)
            val btnQtyIncrease: View = view.findViewById(R.id.btnQtyIncrease)
            val btnRemove: View = view.findViewById(R.id.btnRemove)
        }
    }
}
