package com.example.unimarketguajira.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.adapters.ProductAdapter
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.repository.ProductRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.EditText
import android.view.inputmethod.InputMethodManager
import android.content.Context
import com.example.unimarketguajira.adapters.SuggestionAdapter

class HomeFragment : Fragment() {

    private lateinit var rvProducts: RecyclerView
    private lateinit var categoriesContainer: LinearLayout
    private var allProducts: List<Product> = listOf()

    private lateinit var etSearch: EditText
    private lateinit var cardSuggestions: View
    private lateinit var rvSuggestions: RecyclerView
    private lateinit var tvNoSuggestions: TextView
    private var isSearchingProgrammatically = false
    private var searchJob: kotlinx.coroutines.Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        val selectedCategory = arguments?.getString("CATEGORY")

        val searchBarContainer = view.findViewById<View>(R.id.searchBarContainer)
        cardSuggestions = view.findViewById<View>(R.id.cardSuggestions)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(searchBarContainer) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            
            // Ajustar dinámicamente el margen superior del buscador para quedar debajo de la barra verde
            val lp = cardSuggestions.layoutParams as? androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
            if (lp != null) {
                // 72dp es el alto de searchBarContainer (48dp buscador + 24dp padding vertical acumulado)
                val searchBarHeight = (72 * v.resources.displayMetrics.density).toInt()
                lp.topMargin = systemBars.top + searchBarHeight
                cardSuggestions.layoutParams = lp
            }
            insets
        }

        rvProducts = view.findViewById(R.id.rvProducts)
        categoriesContainer = view.findViewById(R.id.categoriesContainer)
        
        etSearch = view.findViewById(R.id.etSearch)
        rvSuggestions = view.findViewById(R.id.rvSuggestions)
        tvNoSuggestions = view.findViewById(R.id.tvNoSuggestions)

        rvSuggestions.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        
        setupCategories()
        setupProducts()
        setupSearch()

        selectedCategory?.let {
            filterProductsByCategory(it)
        }

        view.findViewById<View>(R.id.flNotificationIcon).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), com.example.unimarketguajira.activities.NotificationsActivity::class.java))
        }

        return view
    }

    private fun setupCategories() {
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
            try {
                view.findViewById<ImageView>(R.id.ivCategoryIcon).setImageResource(category.second)
            } catch (e: Exception) {
                view.findViewById<ImageView>(R.id.ivCategoryIcon).setImageResource(android.R.drawable.ic_menu_gallery)
            }
            
            view.setOnClickListener {
                isSearchingProgrammatically = true
                etSearch.setText("")
                isSearchingProgrammatically = false
                hideSuggestionsLayout()

                for (i in 0 until categoriesContainer.childCount) {
                    categoriesContainer.getChildAt(i).alpha = 0.5f
                }
                view.alpha = 1.0f
                arguments?.remove("SHOW_FAVORITES")
                arguments?.remove("OWNER_EMAIL")
                arguments?.putString("CATEGORY", category.first)
                filterProductsByCategory(category.first)
            }
            
            categoriesContainer.addView(view)
        }
    }

    private fun setupProducts() {
        rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        updateProductsList()
    }

    private fun updateProductsList() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Mostrar Skeleton Screen inicial
            rvProducts.adapter = ProductAdapter(emptyList(), isLoading = true)
            
            // Simular un retraso de red para poder apreciar la animación del skeleton
            delay(1500)
            
            allProducts = ProductRepository.getAllProducts(requireContext())
            
            val showFavorites = arguments?.getBoolean("SHOW_FAVORITES", false) ?: false
            val ownerEmail = arguments?.getString("OWNER_EMAIL")
            val selectedCategory = arguments?.getString("CATEGORY")

            val filteredList = when {
                showFavorites -> allProducts.filter { it.isFavorite }
                ownerEmail != null -> allProducts.filter { it.ownerEmail == ownerEmail }
                selectedCategory != null -> allProducts.filter { it.category == selectedCategory }
                else -> allProducts
            }

            rvProducts.adapter = ProductAdapter(filteredList, isLoading = false)
        }
    }

    private fun filterProductsByCategory(category: String) {
        val filtered = allProducts.filter { it.category == category }
        rvProducts.adapter = ProductAdapter(filtered, isLoading = false)
    }

    override fun onResume() {
        super.onResume()
        updateProductsList()
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        val email = com.example.unimarketguajira.services.UserManager.getLoggedUserEmail(requireContext()) ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val count = com.example.unimarketguajira.repository.PurchaseRepository.getUnreadNotificationsCount(requireContext(), email)
                val cardBadge = view?.findViewById<View>(R.id.cardNotificationBadge)
                val tvBadgeCount = view?.findViewById<TextView>(R.id.tvNotificationBadgeCount)
                if (cardBadge != null && tvBadgeCount != null) {
                    if (count > 0) {
                        tvBadgeCount.text = count.toString()
                        cardBadge.visibility = View.VISIBLE
                    } else {
                        cardBadge.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupSearch() {
        rvProducts.setOnTouchListener { _, _ ->
            etSearch.clearFocus()
            hideKeyboard()
            hideSuggestionsLayout()
            false
        }

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isSearchingProgrammatically) return
                
                val query = s?.toString()?.trim() ?: ""
                searchJob?.cancel()
                if (query.isEmpty()) {
                    hideSuggestionsLayout()
                    restoreOriginalProducts()
                } else {
                    searchJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(300)
                        showSuggestionsForQuery(query)
                    }
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                hideKeyboard()
                true
            } else {
                false
            }
        }
        
        etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    showSuggestionsForQuery(query)
                }
            } else {
                hideSuggestionsLayout()
            }
        }
    }

    private fun showSuggestionsForQuery(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val suggestions = getSuggestions(query)
            
            if (suggestions.isEmpty()) {
                rvSuggestions.visibility = View.GONE
                tvNoSuggestions.visibility = View.VISIBLE
            } else {
                tvNoSuggestions.visibility = View.GONE
                rvSuggestions.visibility = View.VISIBLE
                rvSuggestions.adapter = SuggestionAdapter(suggestions) { selectedSuggestion ->
                    performSearch(selectedSuggestion)
                    hideKeyboard()
                }
            }
            showSuggestionsLayout()
        }
    }

    private fun performSearch(query: String) {
        hideSuggestionsLayout()
        
        isSearchingProgrammatically = true
        etSearch.setText(query)
        etSearch.setSelection(query.length)
        isSearchingProgrammatically = false

        resetCategoriesSelection()

        viewLifecycleOwner.lifecycleScope.launch {
            val results = ProductRepository.searchProductsLocal(requireContext(), query)
            rvProducts.adapter = ProductAdapter(results, isLoading = false)
        }
    }

    private fun restoreOriginalProducts() {
        val showFavorites = arguments?.getBoolean("SHOW_FAVORITES", false) ?: false
        val ownerEmail = arguments?.getString("OWNER_EMAIL")
        val selectedCategory = arguments?.getString("CATEGORY")

        val filteredList = when {
            showFavorites -> allProducts.filter { it.isFavorite }
            ownerEmail != null -> allProducts.filter { it.ownerEmail == ownerEmail }
            selectedCategory != null -> allProducts.filter { it.category == selectedCategory }
            else -> allProducts
        }
        rvProducts.adapter = ProductAdapter(filteredList, isLoading = false)
    }

    private fun resetCategoriesSelection() {
        for (i in 0 until categoriesContainer.childCount) {
            categoriesContainer.getChildAt(i).alpha = 1.0f
        }
        arguments?.remove("CATEGORY")
    }

    private suspend fun getSuggestions(query: String): List<String> {
        val normQuery = query.normalize()
        if (normQuery.isEmpty()) return emptyList()

        // 1. Contexto académico - Prioridad
        val academicTerms = listOf(
            "Calculadora",
            "Calculadora Científica",
            "Calculadora Gráfica",
            "Calculadora Casio",
            "Calculadora TI-Nspire",
            "Libro de Cálculo",
            "Libro de Física",
            "Libro de Álgebra",
            "Libro de Química",
            "Libro de Estadística",
            "Libros Académicos",
            "Física",
            "Química",
            "Álgebra",
            "Estadística",
            "Bata de Laboratorio",
            "Bata",
            "Instrumentos de Laboratorio",
            "Tecnología",
            "Dibujo Técnico",
            "Apuntes de Programación",
            "Apuntes de Electrónica",
            "Apuntes de Álgebra",
            "Electrónica",
            "Programación"
        )
        val matchedAcademic = academicTerms.filter { it.normalize().contains(normQuery) }

        // 2. Categorías
        val categories = listOf("Libros", "Útiles", "Laboratorio", "Tecnología", "Apuntes", "Otros")
        val matchedCategories = categories.filter { it.normalize().contains(normQuery) }

        // 3. Productos en Room / Firebase
        val dbProducts = ProductRepository.searchProductsLocal(requireContext(), query)
        val matchedProductNames = dbProducts.map { it.name }.filter { it.normalize().contains(normQuery) }

        val rawSuggestions = mutableListOf<String>()
        rawSuggestions.addAll(matchedAcademic)
        rawSuggestions.addAll(matchedCategories)
        rawSuggestions.addAll(matchedProductNames)

        val finalSuggestions = mutableListOf<String>()
        val seenNormalized = mutableSetOf<String>()
        for (item in rawSuggestions) {
            val norm = item.normalize()
            if (!seenNormalized.contains(norm)) {
                seenNormalized.add(norm)
                finalSuggestions.add(item)
            }
        }

        return finalSuggestions.take(8)
    }

    private fun String.normalize(): String {
        return this.lowercase()
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('í', 'i')
            .replace('ó', 'o')
            .replace('ú', 'u')
            .replace('ñ', 'n')
            .trim()
    }

    private fun showSuggestionsLayout() {
        if (cardSuggestions.visibility == View.VISIBLE) return
        
        cardSuggestions.alpha = 0f
        cardSuggestions.visibility = View.VISIBLE
        cardSuggestions.animate()
            .alpha(1f)
            .setDuration(250)
            .setListener(null)
    }

    private fun hideSuggestionsLayout() {
        if (cardSuggestions.visibility == View.GONE) return
        
        cardSuggestions.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                cardSuggestions.visibility = View.GONE
            }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}