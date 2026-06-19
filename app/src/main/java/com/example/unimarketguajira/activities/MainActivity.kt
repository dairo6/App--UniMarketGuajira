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
        
        // Asegurar que el repositorio tenga datos
        lifecycleScope.launch {
            com.example.unimarketguajira.repository.ProductRepository.getAllProducts(this@MainActivity)
        }
        
        // Cargar fragment inicial
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        findViewById<FloatingActionButton>(R.id.fabAddProduct).setOnClickListener {
            startActivity(Intent(this, PublishProductActivity::class.java))
        }
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

    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
        
        // Si no es el Home, lo añadimos a la pila para poder volver atrás
        if (fragment !is HomeFragment) {
            transaction.addToBackStack(null)
        } else {
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