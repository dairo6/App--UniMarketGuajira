package com.example.unimarketguajira.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.activities.ProductDetailActivity
import com.example.unimarketguajira.models.Product
import java.text.NumberFormat
import java.util.Locale

import com.example.unimarketguajira.models.loadProductImage

class ProductAdapter(private var products: List<Product>, private val isLoading: Boolean = false) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_SKELETON = 1
    }

    private var lastPosition = -1

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProductImage: ImageView = view.findViewById(R.id.ivProductImage)
        val tvProductPrice: TextView = view.findViewById(R.id.tvProductPrice)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val ibFavorite: ImageButton = view.findViewById(R.id.ibFavorite)
    }

    class SkeletonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        init {
            val animation = AnimationUtils.loadAnimation(view.context, R.anim.pulse_animation)
            view.startAnimation(animation)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoading) VIEW_TYPE_SKELETON else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SKELETON) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_skeleton, parent, false)
            SkeletonViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
            ProductViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ProductViewHolder) {
            val product = products[position]
            
            holder.ivProductImage.loadProductImage(product.imageUrls.firstOrNull())
            
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

            setAnimation(holder.itemView, position)
        }
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(viewToAnimate.context, R.anim.item_animation_fall_up)
            animation.startOffset = (position % 2) * 150L
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    override fun getItemCount(): Int {
        return if (isLoading) 4 else products.size
    }
}