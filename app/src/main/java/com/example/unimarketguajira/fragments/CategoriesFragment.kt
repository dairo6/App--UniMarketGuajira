package com.example.unimarketguajira.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R

class CategoriesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_categories, container, false)

        val statusBarSpacer = view.findViewById<View>(R.id.statusBarSpacer)
        ViewCompat.setOnApplyWindowInsetsListener(statusBarSpacer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.layoutParams.height = systemBars.top
            v.requestLayout()
            insets
        }

        val rvCategories = view.findViewById<RecyclerView>(R.id.rvCategories)
        rvCategories.layoutManager = LinearLayoutManager(requireContext())
        
        val categories = listOf(
            CategoryItem(getString(R.string.cat_books), R.drawable.ic_book),
            CategoryItem(getString(R.string.cat_supplies), R.drawable.ic_school),
            CategoryItem(getString(R.string.cat_lab), R.drawable.ic_science),
            CategoryItem(getString(R.string.cat_tech), R.drawable.ic_devices),
            CategoryItem(getString(R.string.cat_notes), R.drawable.ic_edit_note),
            CategoryItem(getString(R.string.cat_others), R.drawable.ic_more_horiz)
        )

        rvCategories.adapter = CategoriesAdapter(categories)

        return view
    }

    data class CategoryItem(val name: String, val iconRes: Int)

    inner class CategoriesAdapter(private val items: List<CategoryItem>) :
        RecyclerView.Adapter<CategoriesAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_large, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.ivIcon.setImageResource(item.iconRes)
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvCategoryName)
            val ivIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
        }
    }
}