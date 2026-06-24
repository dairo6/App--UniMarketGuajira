package com.example.unimarketguajira.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.unimarketguajira.R
import com.example.unimarketguajira.fragments.CartFragment
import com.example.unimarketguajira.fragments.CategoriesFragment
import com.example.unimarketguajira.fragments.HomeFragment
import com.example.unimarketguajira.fragments.MenuFragment
import com.google.android.material.bottomappbar.BottomAppBar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

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
        
        setupBottomNavigation()
        
        // Listener para actualizar la UI cuando se vuelve atrás
        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            currentFragment?.let { 
                updateUIState(it)
                syncBottomNavSelection(it)
            }
        }
        
        // Asegurar que el repositorio tenga datos y sincronizar el carrito
        lifecycleScope.launch {
            com.example.unimarketguajira.repository.ProductRepository.getAllProducts(this@MainActivity)
            val loggedEmail = com.example.unimarketguajira.services.UserManager.getLoggedUserEmail(this@MainActivity)
            if (loggedEmail != null) {
                com.example.unimarketguajira.repository.CartRepository.syncFirebaseCartToLocal(this@MainActivity, loggedEmail)
            }
        }
        
        // Cargar fragment inicial o carrito si se solicita
        if (savedInstanceState == null) {
            val openCart = intent.getBooleanExtra("OPEN_CART", false)
            if (openCart) {
                replaceFragment(CartFragment(), addToBackStack = false)
                findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_cart
            } else {
                replaceFragment(HomeFragment())
            }
        }

        findViewById<FloatingActionButton>(R.id.fabAddProduct).setOnClickListener {
            startActivity(Intent(this, PublishProductActivity::class.java))
        }

        // Iniciar el listener de notificaciones de chat en tiempo real
        com.example.unimarketguajira.services.ChatNotificationService.startListening(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.unimarketguajira.services.ChatNotificationService.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val openCart = intent.getBooleanExtra("OPEN_CART", false)
        if (openCart) {
            replaceFragment(CartFragment(), addToBackStack = false)
            findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_cart
        }
    }

    override fun onBackPressed() {
        val callingActivity = intent.getStringExtra("CALLING_ACTIVITY")
        if (callingActivity != null) {
            try {
                // Reset bottom navigation selection to Home to restore original tab state when returning
                findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_home
                
                val clazz = Class.forName(callingActivity)
                val intent = Intent(this, clazz).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                this.intent.removeExtra("CALLING_ACTIVITY")
                startActivity(intent)
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        super.onBackPressed()
    }

    // Eliminamos initDefaultProducts de aquí, ya que ahora reside en el Repository

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_categories -> {
                    replaceFragment(CategoriesFragment())
                    true
                }
                R.id.nav_cart -> {
                    replaceFragment(CartFragment())
                    true
                }
                R.id.nav_menu -> {
                    replaceFragment(MenuFragment())
                    true
                }
                R.id.nav_placeholder -> false
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
        
        // Si no es el Home, lo añadimos a la pila para poder volver atrás
        if (addToBackStack && fragment !is HomeFragment) {
            transaction.addToBackStack(null)
        } else if (fragment is HomeFragment) {
            // Si volvemos al Home, limpiamos la pila para evitar bucles
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        transaction.commit()
        
        // Actualizar UI inmediatamente (el listener se encargará de los "atrás")
        updateUIState(fragment)
    }

    private fun updateUIState(fragment: Fragment) {
        val bottomAppBar = findViewById<BottomAppBar>(R.id.bottomAppBar)
        val fab = findViewById<FloatingActionButton>(R.id.fabAddProduct)

        if (fragment is CartFragment) {
            bottomAppBar.visibility = View.GONE
            fab.visibility = View.GONE
        } else {
            bottomAppBar.visibility = View.VISIBLE
            fab.visibility = View.VISIBLE
        }
    }

    private fun syncBottomNavSelection(fragment: Fragment) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val itemId = when (fragment) {
            is HomeFragment -> R.id.nav_home
            is CategoriesFragment -> R.id.nav_categories
            is CartFragment -> R.id.nav_cart
            is MenuFragment -> R.id.nav_menu
            else -> null
        }
        itemId?.let {
            bottomNav.menu.findItem(it).isChecked = true
        }
    }

    override fun onResume() {
        super.onResume()
        // Forzar re-cálculo del layout para BottomAppBar y FAB ante cualquier cambio del teclado o ciclo de vida
        findViewById<View>(R.id.fabAddProduct).postDelayed({
            findViewById<View>(R.id.bottomAppBar).requestLayout()
            findViewById<View>(R.id.fabAddProduct).requestLayout()
        }, 150)
    }
}