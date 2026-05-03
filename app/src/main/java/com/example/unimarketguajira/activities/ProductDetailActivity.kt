package com.example.unimarketguajira.activities

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.unimarketguajira.R
import com.google.android.material.appbar.MaterialToolbar
import java.text.NumberFormat
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_product_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupToolbar()
        loadProductData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun loadProductData() {
        // En una app real, recibiríamos el ID y cargaríamos de DB/API
        // Por ahora, usamos extras o datos mock si no vienen
        val name = intent.getStringExtra("PRODUCT_NAME") ?: "Producto"
        val price = intent.getDoubleExtra("PRODUCT_PRICE", 0.0)
        val description = intent.getStringExtra("PRODUCT_DESCRIPTION") ?: "Sin descripción disponible."
        val location = intent.getStringExtra("PRODUCT_LOCATION") ?: "Ubicación no especificada"
        val condition = intent.getStringExtra("PRODUCT_CONDITION") ?: "No especificado"
        val imageRes = intent.getIntExtra("PRODUCT_IMAGE", R.drawable.ic_launcher_background)

        findViewById<TextView>(R.id.tvDetailName).text = name
        findViewById<TextView>(R.id.tvDetailDescription).text = description
        findViewById<TextView>(R.id.tvDetailLocation).text = location
        findViewById<TextView>(R.id.tvDetailCondition).text = condition
        findViewById<ImageView>(R.id.ivProductDetailImage).setImageResource(imageRes)

        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        formatter.maximumFractionDigits = 0
        findViewById<TextView>(R.id.tvDetailPrice).text = formatter.format(price)
        
        val isFavorite = intent.getBooleanExtra("PRODUCT_FAVORITE", false)
        val ibFavorite = findViewById<ImageButton>(R.id.ibDetailFavorite)
        ibFavorite.setImageResource(if (isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
    }
}