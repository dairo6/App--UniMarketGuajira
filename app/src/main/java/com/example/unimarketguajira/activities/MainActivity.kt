package com.example.unimarketguajira.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.adapters.ProductAdapter
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.repository.ProductRepository
import com.example.unimarketguajira.services.UserManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0) // El padding inferior lo maneja el BottomNav
            insets
        }
        
        setupCategories()
        setupProducts()
        setupBottomNavigation()

        findViewById<FloatingActionButton>(R.id.fabAddProduct).setOnClickListener {
            startActivity(Intent(this, PublishProductActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val categoriesContainer = findViewById<LinearLayout>(R.id.categoriesContainer)
                    for (i in 0 until categoriesContainer.childCount) {
                        categoriesContainer.getChildAt(i).alpha = 1.0f
                    }
                    findViewById<RecyclerView>(R.id.rvProducts).adapter = ProductAdapter(allProducts)
                    true
                }
                R.id.nav_categories -> {
                    Toast.makeText(this, "Categorías próximamente", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_cart -> {
                    Toast.makeText(this, "Carrito próximamente", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_menu -> {
                    showOptionsMenu()
                    true
                }
                R.id.nav_placeholder -> false
                else -> false
            }
        }
    }

    private fun showOptionsMenu() {
        val options = arrayOf("Mi Perfil", "Mis Publicaciones", "Cerrar Sesión")
        AlertDialog.Builder(this)
            .setTitle("Opciones")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Perfil próximamente", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "Mis publicaciones próximamente", Toast.LENGTH_SHORT).show()
                    2 -> logout()
                }
            }
            .show()
    }

    private fun logout() {
        UserManager.logout(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setupCategories() {
        val categoriesContainer = findViewById<LinearLayout>(R.id.categoriesContainer)
        categoriesContainer.removeAllViews()

        val categories = listOf(
            Pair(getString(R.string.cat_books), R.drawable.ic_book),
            Pair(getString(R.string.cat_supplies), R.drawable.ic_school),
            Pair(getString(R.string.cat_lab), R.drawable.ic_science),
            Pair(getString(R.string.cat_tech), R.drawable.ic_devices),
            Pair(getString(R.string.cat_notes), R.drawable.ic_edit_note),
            Pair(getString(R.string.cat_others), R.drawable.ic_more_horiz)
        )

        for (category in categories) {
            val view = layoutInflater.inflate(R.layout.item_category, categoriesContainer, false)
            view.findViewById<TextView>(R.id.tvCategoryName).text = category.first
            view.findViewById<ImageView>(R.id.ivCategoryIcon).setImageResource(category.second)
            
            view.setOnClickListener {
                for (i in 0 until categoriesContainer.childCount) {
                    categoriesContainer.getChildAt(i).alpha = 0.5f
                }
                view.alpha = 1.0f
                filterProductsByCategory(category.first)
            }
            
            categoriesContainer.addView(view)
        }
    }

    private var allProducts: List<Product> = listOf()

    override fun onResume() {
        super.onResume()
        updateProductsList()
    }

    private fun updateProductsList() {
        allProducts = ProductRepository.getAllProducts()
        findViewById<RecyclerView>(R.id.rvProducts).adapter = ProductAdapter(allProducts)
    }

    private fun setupProducts() {
        val rvProducts = findViewById<RecyclerView>(R.id.rvProducts)
        
        // Grid de 2 columnas
        rvProducts.layoutManager = GridLayoutManager(this, 2)
        
        val initialProducts = listOf(
            Product(1, "Calculadora TI-Nspire CX II CAS", "Calculadora gráfica avanzada para ingeniería y matemáticas.", 850000.0, "Riohacha", listOf(R.drawable.ic_launcher_background), getString(R.string.cat_tech), getString(R.string.condition_new)),
            Product(2, "Libro: Cálculo de Stewart 8va Ed.", "Libro esencial para ingeniería. En muy buen estado.", 120000.0, "Maicao", listOf(R.drawable.ic_launcher_background), getString(R.string.cat_books), getString(R.string.condition_used_good), true),
            Product(3, "Bata de Laboratorio XL", "Bata blanca reglamentaria para laboratorio de química.", 45000.0, "Riohacha", listOf(R.drawable.ic_launcher_background), getString(R.string.cat_lab), getString(R.string.condition_used_like_new)),
            Product(4, "iPad Air 5ta Gen + Pencil", "Ideal para tomar apuntes digitales. 64GB de almacenamiento.", 2800000.0, "Uribia", listOf(R.drawable.ic_launcher_background), getString(R.string.cat_tech), getString(R.string.condition_used_like_new)),
            Product(5, "Kit de Drawing Técnico", "Tablero y juego de escuadras profesionales.", 95000.0, "Riohacha", listOf(R.drawable.ic_launcher_background), getString(R.string.cat_supplies), getString(R.string.condition_used_acceptable), true),
            Product(6, "Apuntes Estructuras de Datos", "Apuntes completos del semestre 2023-2 con ejercicios resueltos.", 15000.0, "Manaure", listOf(R.drawable.ic_launcher_background), getString(R.string.cat_notes), getString(R.string.condition_used_good))
        )

        ProductRepository.setInitialProducts(initialProducts)
        updateProductsList()
    }

    private fun filterProductsByCategory(category: String) {
        val filtered = allProducts.filter { it.category == category }
        findViewById<RecyclerView>(R.id.rvProducts).adapter = ProductAdapter(filtered)
    }
}