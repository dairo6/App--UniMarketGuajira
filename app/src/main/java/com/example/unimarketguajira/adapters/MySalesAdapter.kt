package com.example.unimarketguajira.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.models.Purchase
import com.example.unimarketguajira.models.loadProductImage
import com.example.unimarketguajira.repository.ProductRepository
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MySalesAdapter(
    private val context: Context,
    private val scope: CoroutineScope,
    private var purchases: List<Purchase>,
    private val onConfirmClick: (Purchase, String) -> Unit,
    private val onCancelClick: (Purchase, String) -> Unit,
    private val onDeliverClick: (Purchase, String) -> Unit
) : RecyclerView.Adapter<MySalesAdapter.SaleViewHolder>() {

    private val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

    fun updateData(newPurchases: List<Purchase>) {
        purchases = newPurchases
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_my_sale, parent, false)
        return SaleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        val purchase = purchases[position]
        holder.bind(purchase)
    }

    override fun getItemCount(): Int = purchases.size

    inner class SaleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProductImage: ImageView = itemView.findViewById(R.id.ivSaleProductImage)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvSaleProductName)
        private val tvProductPrice: TextView = itemView.findViewById(R.id.tvSaleProductPrice)
        private val cardSaleStatus: MaterialCardView = itemView.findViewById(R.id.cardSaleStatus)
        private val tvSaleStatus: TextView = itemView.findViewById(R.id.tvSaleStatus)
        
        private val tvSaleBuyer: TextView = itemView.findViewById(R.id.tvSaleBuyer)
        private val tvSaleDate: TextView = itemView.findViewById(R.id.tvSaleDate)
        private val tvSalePaymentMethod: TextView = itemView.findViewById(R.id.tvSalePaymentMethod)
        private val tvSaleDeliveryPoint: TextView = itemView.findViewById(R.id.tvSaleDeliveryPoint)
        
        private val layoutSaleActions: View = itemView.findViewById(R.id.layoutSaleActions)
        private val btnSaleCancel: View = itemView.findViewById(R.id.btnSaleCancel)
        private val btnSaleConfirm: View = itemView.findViewById(R.id.btnSaleConfirm)
        private val btnSaleMarkDelivered: View = itemView.findViewById(R.id.btnSaleMarkDelivered)

        fun bind(purchase: Purchase) {
            tvProductPrice.text = formatter.format(purchase.price * purchase.quantity)
            tvSaleBuyer.text = purchase.buyerId
            tvSaleDate.text = dateFormat.format(Date(purchase.purchaseDate))
            tvSalePaymentMethod.text = purchase.paymentMethod
            tvSaleDeliveryPoint.text = purchase.deliveryPoint

            // Cargar el nombre e imagen del producto de forma asíncrona
            tvProductName.text = "Cargando producto..."
            ivProductImage.setImageResource(R.drawable.ic_launcher_background)
            
            var resolvedProductName = "Producto"

            scope.launch {
                val product = ProductRepository.getProductById(context, purchase.productId)
                if (product != null) {
                    resolvedProductName = product.name
                    tvProductName.text = product.name
                    if (product.imageUrls.isNotEmpty()) {
                        ivProductImage.loadProductImage(product.imageUrls.first())
                    } else {
                        ivProductImage.loadProductImage(null)
                    }
                } else {
                    resolvedProductName = "Producto #${purchase.productId}"
                    tvProductName.text = resolvedProductName
                    ivProductImage.loadProductImage(null)
                }
            }

            // Configurar el Badge de Estado según el estado
            when (purchase.status) {
                "PENDING" -> {
                    tvSaleStatus.text = "🟡 Pendiente"
                    tvSaleStatus.setTextColor(Color.parseColor("#F57C00"))
                    cardSaleStatus.setCardBackgroundColor(Color.parseColor("#FFF9C4"))
                    
                    layoutSaleActions.visibility = View.VISIBLE
                    btnSaleCancel.visibility = View.VISIBLE
                    btnSaleConfirm.visibility = View.VISIBLE
                    btnSaleMarkDelivered.visibility = View.GONE
                }
                "CONFIRMED" -> {
                    tvSaleStatus.text = "🔵 Confirmada"
                    tvSaleStatus.setTextColor(Color.parseColor("#0288D1"))
                    cardSaleStatus.setCardBackgroundColor(Color.parseColor("#E1F5FE"))
                    
                    layoutSaleActions.visibility = View.VISIBLE
                    btnSaleCancel.visibility = View.GONE
                    btnSaleConfirm.visibility = View.GONE
                    btnSaleMarkDelivered.visibility = View.VISIBLE
                }
                "DELIVERED" -> {
                    tvSaleStatus.text = "🟢 Entregada"
                    tvSaleStatus.setTextColor(Color.parseColor("#388E3C"))
                    cardSaleStatus.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                    
                    layoutSaleActions.visibility = View.GONE
                }
                "CANCELLED" -> {
                    tvSaleStatus.text = "🔴 Cancelada"
                    tvSaleStatus.setTextColor(Color.parseColor("#D32F2F"))
                    cardSaleStatus.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                    
                    layoutSaleActions.visibility = View.GONE
                }
                else -> {
                    tvSaleStatus.text = purchase.status
                    tvSaleStatus.setTextColor(Color.BLACK)
                    cardSaleStatus.setCardBackgroundColor(Color.LTGRAY)
                    layoutSaleActions.visibility = View.GONE
                }
            }

            // Click Listeners
            btnSaleConfirm.setOnClickListener {
                onConfirmClick(purchase, resolvedProductName)
            }
            btnSaleCancel.setOnClickListener {
                onCancelClick(purchase, resolvedProductName)
            }
            btnSaleMarkDelivered.setOnClickListener {
                onDeliverClick(purchase, resolvedProductName)
            }
        }
    }
}
