package com.example.unimarketguajira.adapters

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
import java.text.NumberFormat
import java.util.Locale

import com.example.unimarketguajira.models.loadProductImage

/**
 * Adapter compacto para mostrar productos en la sección de Recomendaciones del Carrito.
 * Usa item_product_small.xml (140dp ancho) y navega a ProductDetailActivity.
 */
class ProductSmallAdapter(private val products: List<Product>) :
    RecyclerView.Adapter<ProductSmallAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView   = view.findViewById(R.id.ivProductImage)
        val tvPrice: TextView    = view.findViewById(R.id.tvProductPrice)
        val tvName: TextView     = view.findViewById(R.id.tvProductName)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_small, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        holder.ivImage.loadProductImage(product.imageUrls.firstOrNull())

        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        formatter.maximumFractionDigits = 0
        holder.tvPrice.text    = formatter.format(product.price)
        holder.tvName.text     = product.name
        holder.tvCategory.text = product.category
        holder.tvLocation.text = product.location

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ProductDetailActivity::class.java).apply {
                putExtra("PRODUCT_ID", product.id)
                putExtra("PRODUCT_NAME", product.name)
                putExtra("PRODUCT_PRICE", product.price)
                putExtra("PRODUCT_DESCRIPTION", product.description)
                putExtra("PRODUCT_LOCATION", product.location)
                putExtra("PRODUCT_CONDITION", product.condition)
                putExtra("PRODUCT_CATEGORY", product.category)
                putExtra("PRODUCT_IMAGE", product.imageUrls.firstOrNull())
                putExtra("PRODUCT_FAVORITE", product.isFavorite)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = products.size
}
