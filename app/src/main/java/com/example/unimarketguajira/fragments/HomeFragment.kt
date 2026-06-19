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

class HomeFragment : Fragment() {

    private lateinit var rvProducts: RecyclerView
    private lateinit var categoriesContainer: LinearLayout
    private var allProducts: List<Product> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        rvProducts = view.findViewById(R.id.rvProducts)
        categoriesContainer = view.findViewById(R.id.categoriesContainer)
        
        setupCategories()
        setupProducts()
        
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

    private fun setupProducts() {
        rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        updateProductsList()
    }

    private fun updateProductsList() {
        allProducts = ProductRepository.getAllProducts()
        rvProducts.adapter = ProductAdapter(allProducts)
    }

    private fun filterProductsByCategory(category: String) {
        val filtered = allProducts.filter { it.category == category }
        rvProducts.adapter = ProductAdapter(filtered)
    }

    override fun onResume() {
        super.onResume()
        updateProductsList()
    }
}