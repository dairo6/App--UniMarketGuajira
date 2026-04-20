package com.example.unimarketguajira

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import android.widget.HorizontalScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.widget.TextView

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupCategories()

        val rvUsers = findViewById<RecyclerView>(R.id.rvUsers)
        rvUsers.layoutManager = LinearLayoutManager(this)
        rvUsers.adapter = UserAdapter(UserManager.getAllUsers(this))
    }

    private fun setupCategories() {
        val hsvCategories = findViewById<HorizontalScrollView>(R.id.hsvCategories)
        val categoriesContainer = hsvCategories.getChildAt(0) as LinearLayout
        categoriesContainer.removeAllViews()

        val categories = listOf(
            Pair("Tecnología", R.drawable.ic_categories),
            Pair("Libros", R.drawable.ic_menu),
            Pair("Comida", R.drawable.ic_notifications),
            Pair("Moda", R.drawable.ic_home),
            Pair("Servicios", R.drawable.ic_cart)
        )

        for (category in categories) {
            val view = layoutInflater.inflate(R.layout.item_category, categoriesContainer, false)
            view.findViewById<TextView>(R.id.tvCategoryName).text = category.first
            view.findViewById<ImageView>(R.id.ivCategoryIcon).setImageResource(category.second)
            categoriesContainer.addView(view)
        }
    }
}