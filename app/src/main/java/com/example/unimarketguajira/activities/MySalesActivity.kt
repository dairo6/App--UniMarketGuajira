package com.example.unimarketguajira.activities

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.unimarketguajira.R
import com.example.unimarketguajira.adapters.MySalesAdapter
import com.example.unimarketguajira.models.Purchase
import com.example.unimarketguajira.repository.PurchaseRepository
import com.example.unimarketguajira.services.UserManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MySalesActivity : AppCompatActivity() {

    private lateinit var rvMySales: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    
    private lateinit var tvDashboardTotal: TextView
    private lateinit var tvDashboardCountRealized: TextView
    private lateinit var tvDashboardCountPending: TextView
    private lateinit var tvDashboardCountCompleted: TextView

    private lateinit var adapter: MySalesAdapter
    private var sellerEmail: String = ""
    private var salesList: List<Purchase> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_sales)

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

        // Dashboard views
        tvDashboardTotal = findViewById(R.id.tvDashboardTotal)
        tvDashboardCountRealized = findViewById(R.id.tvDashboardCountRealized)
        tvDashboardCountPending = findViewById(R.id.tvDashboardCountPending)
        tvDashboardCountCompleted = findViewById(R.id.tvDashboardCountCompleted)

        // List setup
        rvMySales = findViewById(R.id.rvMySales)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        rvMySales.layoutManager = LinearLayoutManager(this)
        
        adapter = MySalesAdapter(
            context = this,
            scope = lifecycleScope,
            purchases = salesList,
            onConfirmClick = { purchase, productName -> showConfirmSaleDialog(purchase, productName) },
            onCancelClick = { purchase, productName -> showCancelSaleDialog(purchase, productName) },
            onDeliverClick = { purchase, productName -> showMarkAsDeliveredDialog(purchase, productName) }
        )
        rvMySales.adapter = adapter

        sellerEmail = UserManager.getLoggedUserEmail(this) ?: ""
        if (sellerEmail.isEmpty()) {
            Toast.makeText(this, "Sesión no válida. Inicia sesión de nuevo.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadSales(forceRefresh = true)
        }

        loadSales(forceRefresh = false)
    }

    private fun loadSales(forceRefresh: Boolean) {
        if (!forceRefresh) {
            swipeRefreshLayout.isRefreshing = true
        }
        lifecycleScope.launch {
            try {
                salesList = PurchaseRepository.getPurchasesForSeller(this@MySalesActivity, sellerEmail)
                adapter.updateData(salesList)
                updateDashboard()
                
                if (salesList.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    rvMySales.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    rvMySales.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MySalesActivity, "Error al cargar las ventas", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateDashboard() {
        var totalGenerated = 0.0
        var countRealized = salesList.size
        var countPending = 0
        var countCompleted = 0

        for (sale in salesList) {
            when (sale.status) {
                "PENDING" -> countPending++
                "CONFIRMED" -> {
                    // La confirmada cuenta para el total generado
                    totalGenerated += (sale.price * sale.quantity)
                }
                "DELIVERED" -> {
                    countCompleted++
                    totalGenerated += (sale.price * sale.quantity)
                }
            }
        }

        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }

        tvDashboardTotal.text = formatter.format(totalGenerated)
        tvDashboardCountRealized.text = countRealized.toString()
        tvDashboardCountPending.text = countPending.toString()
        tvDashboardCountCompleted.text = countCompleted.toString()
    }

    private fun showConfirmSaleDialog(purchase: Purchase, productName: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Venta")
            .setMessage("¿Estás seguro de que deseas confirmar la venta de \"$productName\"?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                updateStatus(purchase.id, "CONFIRMED", productName, "Venta confirmada")
            }
            .show()
    }

    private fun showCancelSaleDialog(purchase: Purchase, productName: String) {
        AlertDialog.Builder(this)
            .setTitle("Cancelar Venta")
            .setMessage("¿Estás seguro de que deseas cancelar la venta de \"$productName\"?")
            .setNegativeButton("No", null)
            .setPositiveButton("Sí, cancelar") { _, _ ->
                updateStatus(purchase.id, "CANCELLED", productName, "Venta cancelada")
            }
            .show()
    }

    private fun showMarkAsDeliveredDialog(purchase: Purchase, productName: String) {
        AlertDialog.Builder(this)
            .setTitle("Marcar como Entregado")
            .setMessage("¿Confirmas que has entregado el producto \"$productName\" al comprador?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Entregado") { _, _ ->
                updateStatus(purchase.id, "DELIVERED", productName, "Venta marcada como entregada")
            }
            .show()
    }

    private fun updateStatus(purchaseId: String, newStatus: String, productName: String, successMsg: String) {
        swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch {
            try {
                PurchaseRepository.updatePurchaseStatus(this@MySalesActivity, purchaseId, newStatus, productName)
                Toast.makeText(this@MySalesActivity, successMsg, Toast.LENGTH_SHORT).show()
                loadSales(forceRefresh = true)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MySalesActivity, "Error al actualizar el estado", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
}
