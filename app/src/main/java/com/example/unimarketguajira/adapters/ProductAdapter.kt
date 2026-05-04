package com.example.unimarketguajira.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.activities.ProductDetailActivity
import com.example.unimarketguajira.models.Product
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(private val products: List<Product>) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProductImage: ImageView = view.findViewById(R.id.ivProductImage)
        val tvProductPrice: TextView = view.findViewById(R.id.tvProductPrice)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val ibFavorite: ImageButton = view.findViewById(R.id.ibFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        
        holder.ivProductImage.setImageResource(product.imageUrls.firstOrNull() ?: R.drawable.ic_launcher_background)
        
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        formatter.maximumFractionDigits = 0
        holder.tvProductPrice.text = formatter.format(product.price)
        
        holder.tvProductName.text = product.name
        holder.tvLocation.text = product.location
        holder.tvCategory.text = product.category

        val favoriteIcon = if (product.isFavorite) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }
        holder.ibFavorite.setImageResource(favoriteIcon)

        holder.ibFavorite.setOnClickListener {
            product.isFavorite = !product.isFavorite
            notifyItemChanged(position)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ProductDetailActivity::class.java).apply {
                putExtra("PRODUCT_NAME", product.name)
                putExtra("PRODUCT_PRICE", product.price)
                putExtra("PRODUCT_DESCRIPTION", product.description)
                putExtra("PRODUCT_LOCATION", product.location)
                putExtra("PRODUCT_CONDITION", product.condition)
                putExtra("PRODUCT_IMAGE", product.imageUrls.firstOrNull() ?: R.drawable.ic_launcher_background)
                putExtra("PRODUCT_FAVORITE", product.isFavorite)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = products.size
}