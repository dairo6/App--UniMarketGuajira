package com.example.unimarketguajira.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.unimarketguajira.R
import com.example.unimarketguajira.services.UserManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        setupBottomNavigation()
        setupMenuSections()
        
        findViewById<FloatingActionButton>(R.id.fabAddProduct).setOnClickListener {
            startActivity(Intent(this, PublishProductActivity::class.java))
        }
        
        findViewById<TextView>(R.id.tvUserName).text = UserManager.getUser(this)?.username ?: "Estudiante"
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_menu
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
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
                R.id.nav_menu -> true
                else -> false
            }
        }
    }

    private fun setupMenuSections() {
        val container = findViewById<LinearLayout>(R.id.menuSectionsContainer)

        addSection(container, "PRINCIPAL", listOf(
            MenuOption("Inicio", R.drawable.ic_home) { finish() },
            MenuOption("Mi perfil", android.R.drawable.ic_menu_myplaces),
            MenuOption("Mis publicaciones", R.drawable.ic_edit_note),
            MenuOption("Favoritos", android.R.drawable.btn_star_big_on),
            MenuOption("Historial", android.R.drawable.ic_menu_recent_history)
        ))

        addSection(container, "ACTIVIDAD", listOf(
            MenuOption("Productos guardados", R.drawable.ic_cart),
            MenuOption("Ventas", android.R.drawable.ic_menu_send),
            MenuOption("Compras", android.R.drawable.ic_menu_agenda),
            MenuOption("Notificaciones", R.drawable.ic_notifications)
        ))

        addSection(container, "MARKETPLACE", listOf(
            MenuOption("Categorías", R.drawable.ic_categories),
            MenuOption("Publicar producto", android.R.drawable.ic_input_add) {
                startActivity(Intent(this, PublishProductActivity::class.java))
            },
            MenuOption("Mis productos", R.drawable.ic_school)
        ))

        addSection(container, "UNIVERSIDAD", listOf(
            MenuOption("Artículos académicos", R.drawable.ic_book),
            MenuOption("Libros", R.drawable.ic_book),
            MenuOption("Laboratorio", R.drawable.ic_science),
            MenuOption("Batas y Uniformes", R.drawable.ic_school)
        ))

        addSection(container, "CONFIGURACIÓN", listOf(
            MenuOption("Privacidad", android.R.drawable.ic_lock_idle_lock),
            MenuOption("Ayuda y soporte", android.R.drawable.ic_menu_help),
            MenuOption("Cerrar sesión", android.R.drawable.ic_menu_close_clear_cancel) { logout() }
        ))
    }

    private fun addSection(container: LinearLayout, title: String, options: List<MenuOption>) {
        val sectionTitle = TextView(this).apply {
            text = title
            textSize = 12spToPx().toFloat()
            setPadding(0, 24.dpToPx(), 0, 8.dpToPx())
            setTextColor(getColor(android.R.color.darker_gray))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        container.addView(sectionTitle)

        val card = com.google.android.material.card.MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.match_parent,
                LinearLayout.LayoutParams.wrap_content
            )
            radius = 12.dpToPx().toFloat()
            cardElevation = 2.dpToPx().toFloat()
            useCompatPadding = true
        }

        val optionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            paddingHorizontal = 16.dpToPx()
        }

        options.forEachIndexed { index, option ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_menu_option, optionsContainer, false)
            view.findViewById<TextView>(R.id.tvOptionTitle).text = option.title
            view.findViewById<ImageView>(R.id.ivOptionIcon).setImageResource(option.icon)
            view.setOnClickListener { option.action?.invoke() ?: Toast.makeText(this, "${option.title} próximamente", Toast.LENGTH_SHORT).show() }
            
            optionsContainer.addView(view)

            if (index < options.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.match_parent, 1.dpToPx())
                    setBackgroundColor(getColor(android.R.color.darker_gray))
                    alpha = 0.1f
                }
                optionsContainer.addView(divider)
            }
        }

        card.addView(optionsContainer)
        container.addView(card)
    }

    private fun logout() {
        UserManager.logout(this)
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun Int.dpToPx() = (this * resources.displayMetrics.density).toInt()
    private fun Int.spToPx() = (this * resources.displayMetrics.scaledDensity).toInt()

    data class MenuOption(val title: String, val icon: Int, val action: (() -> Unit)? = null)
}