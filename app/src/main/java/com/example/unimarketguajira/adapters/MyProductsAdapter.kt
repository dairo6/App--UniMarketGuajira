package com.example.unimarketguajira.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.models.loadProductImage
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale

class MyProductsAdapter(
    private val products: List<Product>,
    private val onEditClick: (Product) -> Unit,
    private val onHideClick: (Product) -> Unit,
    private val onReactivateClick: (Product) -> Unit,
    private val onMarkSoldClick: (Product) -> Unit
) : RecyclerView.Adapter<MyProductsAdapter.MyProductViewHolder>() {

    class MyProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivMyProductImage)
        val tvName: TextView = view.findViewById(R.id.tvMyProductName)
        val tvPrice: TextView = view.findViewById(R.id.tvMyProductPrice)
        val tvCategory: TextView = view.findViewById(R.id.tvMyProductCategory)
        val tvStatus: TextView = view.findViewById(R.id.tvMyProductStatus)
        val btnEdit: MaterialButton = view.findViewById(R.id.btnMyProductEdit)
        val btnMarkSold: MaterialButton = view.findViewById(R.id.btnMyProductMarkSold)
        val btnReactivate: MaterialButton = view.findViewById(R.id.btnMyProductReactivate)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnMyProductDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_my_product, parent, false)
        return MyProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyProductViewHolder, position: Int) {
        val product = products[position]

        holder.ivImage.loadProductImage(product.imageUrls.firstOrNull())
        holder.tvName.text = product.name

        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        formatter.maximumFractionDigits = 0
        holder.tvPrice.text = formatter.format(product.price)
        holder.tvCategory.text = product.category

        // Configurar Estado del Producto
        when (product.status) {
            "ACTIVE" -> {
                holder.tvStatus.text = "🟢 Activo"
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.primary))
                holder.btnMarkSold.visibility = View.VISIBLE
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnReactivate.visibility = View.GONE
            }
            "INACTIVE" -> {
                holder.tvStatus.text = "🟡 Oculto"
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.secondary))
                holder.btnMarkSold.visibility = View.GONE
                holder.btnDelete.visibility = View.GONE
                holder.btnReactivate.visibility = View.VISIBLE
            }
            "SOLD" -> {
                holder.tvStatus.text = "🔴 Vendido"
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.red))
                holder.btnMarkSold.visibility = View.GONE
                holder.btnDelete.visibility = View.GONE
                holder.btnReactivate.visibility = View.VISIBLE
            }
            else -> {
                holder.tvStatus.text = "🟢 Activo"
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.primary))
                holder.btnMarkSold.visibility = View.VISIBLE
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnReactivate.visibility = View.GONE
            }
        }

        // Click listeners
        holder.btnEdit.setOnClickListener { onEditClick(product) }
        holder.btnMarkSold.setOnClickListener { onMarkSoldClick(product) }
        holder.btnReactivate.setOnClickListener { onReactivateClick(product) }
        holder.btnDelete.setOnClickListener { onHideClick(product) }
    }

    override fun getItemCount(): Int = products.size
}
