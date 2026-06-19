package com.example.unimarketguajira.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
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
import com.example.unimarketguajira.adapters.MyPurchasesAdapter
import com.example.unimarketguajira.models.Purchase
import com.example.unimarketguajira.repository.PurchaseRepository
import com.example.unimarketguajira.services.UserManager
import kotlinx.coroutines.launch

class MyPurchasesActivity : AppCompatActivity() {

    private lateinit var rvMyPurchases: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var tvTotalPurchases: TextView
    private lateinit var tvPendingPurchases: TextView
    private lateinit var tvCompletedPurchases: TextView

    private lateinit var adapter: MyPurchasesAdapter
    private var buyerEmail: String = ""
    private var purchasesList: List<Purchase> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_purchases)

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
        toolbar.setNavigationOnClickListener { finish() }

        // Views
        rvMyPurchases = findViewById(R.id.rvMyPurchases)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        tvTotalPurchases = findViewById(R.id.tvTotalPurchases)
        tvPendingPurchases = findViewById(R.id.tvPendingPurchases)
        tvCompletedPurchases = findViewById(R.id.tvCompletedPurchases)

        rvMyPurchases.layoutManager = LinearLayoutManager(this)
        adapter = MyPurchasesAdapter(this, lifecycleScope, emptyList())
        rvMyPurchases.adapter = adapter

        buyerEmail = UserManager.getLoggedUserEmail(this) ?: ""
        if (buyerEmail.isEmpty()) {
            Toast.makeText(this, "Sesión no válida.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadPurchases(forceRefresh = true)
        }

        loadPurchases(forceRefresh = false)
    }

    private fun loadPurchases(forceRefresh: Boolean) {
        if (!forceRefresh) {
            swipeRefreshLayout.isRefreshing = true
        }
        lifecycleScope.launch {
            try {
                purchasesList = PurchaseRepository.getPurchasesForBuyer(this@MyPurchasesActivity, buyerEmail)
                adapter.updateData(purchasesList)

                val total = purchasesList.size
                val pending = purchasesList.count { it.status == "PENDING" || it.status == "CONFIRMED" }
                val completed = purchasesList.count { it.status == "DELIVERED" }

                tvTotalPurchases.text = total.toString()
                tvPendingPurchases.text = pending.toString()
                tvCompletedPurchases.text = completed.toString()

                if (purchasesList.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    rvMyPurchases.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    rvMyPurchases.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MyPurchasesActivity, "Error al cargar las compras", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
}
