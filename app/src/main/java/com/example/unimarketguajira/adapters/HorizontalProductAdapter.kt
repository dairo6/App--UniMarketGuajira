package com.example.unimarketguajira.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.activities.ProductDetailActivity
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.models.loadProductImage
import java.text.NumberFormat
import java.util.Locale

class HorizontalProductAdapter(
    private val context: Context,
    private var products: List<Product>
) : RecyclerView.Adapter<HorizontalProductAdapter.ViewHolder>() {

    private val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_product_small, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int = products.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)

        fun bind(product: Product) {
            tvProductPrice.text = formatter.format(product.price)
            tvProductName.text = product.name
            tvCategory.text = product.category
            tvLocation.text = product.location

            if (product.imageUrls.isNotEmpty()) {
                ivProductImage.loadProductImage(product.imageUrls.first())
            } else {
                ivProductImage.loadProductImage(null)
            }

            itemView.setOnClickListener {
                val intent = Intent(context, ProductDetailActivity::class.java).apply {
                    putExtra("PRODUCT_ID", product.id)
                }
                context.startActivity(intent)
            }
        }
    }
}
