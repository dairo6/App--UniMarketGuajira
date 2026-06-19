package com.example.unimarketguajira.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.example.unimarketguajira.R
import com.example.unimarketguajira.adapters.MyProductsAdapter
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.repository.ProductRepository
import com.example.unimarketguajira.repository.PurchaseRepository
import com.example.unimarketguajira.services.UserManager
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class MyProductsActivity : AppCompatActivity() {

    private lateinit var rvMyProducts: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var tabLayoutFilters: TabLayout
    
    private var allMyProducts: List<Product> = emptyList()
    private var currentFilter = "Todos"
    private var ownerEmail = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_products)

        val appBarLayout = findViewById<View>(R.id.appBarLayout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            appBarLayout.setPadding(0, systemBars.top, 0, 0)
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupToolbar()

        rvMyProducts = findViewById(R.id.rvMyProducts)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tabLayoutFilters = findViewById(R.id.tabLayoutFilters)

        rvMyProducts.layoutManager = LinearLayoutManager(this)

        ownerEmail = UserManager.getLoggedUserEmail(this) ?: ""
        if (ownerEmail.isEmpty()) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupTabLayout()
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupTabLayout() {
        tabLayoutFilters.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilter = tab?.text?.toString() ?: "Todos"
                applyFilterAndDisplay()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadProducts() {
        lifecycleScope.launch {
            allMyProducts = ProductRepository.getMyProducts(this@MyProductsActivity, ownerEmail)
            applyFilterAndDisplay()
        }
    }

    private fun applyFilterAndDisplay() {
        val filteredList = when (currentFilter) {
            "Activos" -> allMyProducts.filter { it.status == "ACTIVE" }
            "Ocultos" -> allMyProducts.filter { it.status == "INACTIVE" }
            "Vendidos" -> allMyProducts.filter { it.status == "SOLD" }
            else -> allMyProducts // "Todos"
        }

        if (filteredList.isEmpty()) {
            rvMyProducts.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        } else {
            tvEmptyState.visibility = View.GONE
            rvMyProducts.visibility = View.VISIBLE
            rvMyProducts.adapter = MyProductsAdapter(
                filteredList,
                onEditClick = { product ->
                    val intent = Intent(this, PublishProductActivity::class.java).apply {
                        putExtra("EDIT_PRODUCT_ID", product.id)
                    }
                    startActivity(intent)
                },
                onHideClick = { product ->
                    showHideConfirmation(product)
                },
                onReactivateClick = { product ->
                    reactivateProduct(product)
                },
                onMarkSoldClick = { product ->
                    showMarkSoldConfirmation(product)
                }
            )
        }
    }

    private fun showHideConfirmation(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar")
            .setMessage("¿Deseas ocultar esta publicación?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Ocultar") { _, _ ->
                lifecycleScope.launch {
                    ProductRepository.updateProductStatus(this@MyProductsActivity, product.id, "INACTIVE")
                    Toast.makeText(this@MyProductsActivity, "Publicación ocultada", Toast.LENGTH_SHORT).show()
                    loadProducts()
                }
            }
            .show()
    }

    private fun reactivateProduct(product: Product) {
        lifecycleScope.launch {
            ProductRepository.updateProductStatus(this@MyProductsActivity, product.id, "ACTIVE")
            Toast.makeText(this@MyProductsActivity, "Publicación reactivada con éxito", Toast.LENGTH_SHORT).show()
            loadProducts()
        }
    }

    private fun showMarkSoldConfirmation(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar")
            .setMessage("¿Confirmas que este producto fue vendido?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                lifecycleScope.launch {
                    ProductRepository.updateProductStatus(this@MyProductsActivity, product.id, "SOLD")
                    Toast.makeText(this@MyProductsActivity, "Publicación marcada como vendida", Toast.LENGTH_SHORT).show()
                    PurchaseRepository.createSystemNotification(
                        context = this@MyProductsActivity,
                        userId = ownerEmail,
                        title = "Publicación vendida",
                        message = "Tu publicación \"${product.name}\" fue marcada como vendida.",
                        type = "SISTEMA",
                        relatedProductId = product.id
                    )
                    loadProducts()
                }
            }
            .show()
    }
}
