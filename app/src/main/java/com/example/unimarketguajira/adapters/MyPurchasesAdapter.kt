package com.example.unimarketguajira.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.activities.ProductDetailActivity
import com.example.unimarketguajira.models.Purchase
import com.example.unimarketguajira.models.loadProductImage
import com.example.unimarketguajira.repository.ProductRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPurchasesAdapter(
    private val context: Context,
    private val scope: CoroutineScope,
    private var purchases: List<Purchase>
) : RecyclerView.Adapter<MyPurchasesAdapter.PurchaseViewHolder>() {

    private val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

    fun updateData(newPurchases: List<Purchase>) {
        purchases = newPurchases
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_my_purchase, parent, false)
        return PurchaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: PurchaseViewHolder, position: Int) {
        val purchase = purchases[position]
        holder.bind(purchase)
    }

    override fun getItemCount(): Int = purchases.size

    inner class PurchaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProductImage: ImageView = itemView.findViewById(R.id.ivPurchaseProductImage)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvPurchaseProductName)
        private val tvProductPrice: TextView = itemView.findViewById(R.id.tvPurchaseProductPrice)
        private val cardPurchaseStatus: MaterialCardView = itemView.findViewById(R.id.cardPurchaseStatus)
        private val tvPurchaseStatus: TextView = itemView.findViewById(R.id.tvPurchaseStatus)
        
        private val tvPurchaseSeller: TextView = itemView.findViewById(R.id.tvPurchaseSeller)
        private val tvPurchaseQuantity: TextView = itemView.findViewById(R.id.tvPurchaseQuantity)
        private val tvPurchaseUnitPrice: TextView = itemView.findViewById(R.id.tvPurchaseUnitPrice)
        private val tvPurchaseDate: TextView = itemView.findViewById(R.id.tvPurchaseDate)
        private val tvPurchasePaymentMethod: TextView = itemView.findViewById(R.id.tvPurchasePaymentMethod)
        private val tvPurchaseDeliveryPoint: TextView = itemView.findViewById(R.id.tvPurchaseDeliveryPoint)
        
        private val btnPurchaseDetails: MaterialButton = itemView.findViewById(R.id.btnPurchaseDetails)
        private val btnPurchaseContactSeller: MaterialButton = itemView.findViewById(R.id.btnPurchaseContactSeller)
        private val btnPurchaseViewProduct: MaterialButton = itemView.findViewById(R.id.btnPurchaseViewProduct)

        fun bind(purchase: Purchase) {
            val totalPriceStr = formatter.format(purchase.price * purchase.quantity)
            tvProductPrice.text = totalPriceStr
            tvPurchaseSeller.text = purchase.sellerId
            tvPurchaseQuantity.text = purchase.quantity.toString()
            tvPurchaseUnitPrice.text = formatter.format(purchase.price)
            tvPurchaseDate.text = dateFormat.format(Date(purchase.purchaseDate))
            tvPurchasePaymentMethod.text = purchase.paymentMethod
            tvPurchaseDeliveryPoint.text = purchase.deliveryPoint

            // Fetch product info asynchronously
            tvProductName.text = "Cargando producto..."
            ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)

            scope.launch {
                val product = ProductRepository.getProductById(context, purchase.productId)
                if (product != null) {
                    tvProductName.text = product.name
                    if (product.imageUrls.isNotEmpty()) {
                        ivProductImage.loadProductImage(product.imageUrls.first())
                    } else {
                        ivProductImage.loadProductImage(null)
                    }
                } else {
                    tvProductName.text = "Producto #${purchase.productId}"
                    ivProductImage.loadProductImage(null)
                }
            }

            // Configure Status Badge
            when (purchase.status) {
                "PENDING" -> {
                    tvPurchaseStatus.text = "🟡 PENDIENTE"
                    tvPurchaseStatus.setTextColor(Color.parseColor("#F57C00"))
                    cardPurchaseStatus.setCardBackgroundColor(Color.parseColor("#FFF9C4"))
                }
                "CONFIRMED" -> {
                    tvPurchaseStatus.text = "🔵 CONFIRMADA"
                    tvPurchaseStatus.setTextColor(Color.parseColor("#0288D1"))
                    cardPurchaseStatus.setCardBackgroundColor(Color.parseColor("#E1F5FE"))
                }
                "DELIVERED" -> {
                    tvPurchaseStatus.text = "🟢 ENTREGADA"
                    tvPurchaseStatus.setTextColor(Color.parseColor("#388E3C"))
                    cardPurchaseStatus.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                }
                "CANCELLED" -> {
                    tvPurchaseStatus.text = "🔴 CANCELADA"
                    tvPurchaseStatus.setTextColor(Color.parseColor("#D32F2F"))
                    cardPurchaseStatus.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                }
                else -> {
                    tvPurchaseStatus.text = purchase.status
                    tvPurchaseStatus.setTextColor(Color.BLACK)
                    cardPurchaseStatus.setCardBackgroundColor(Color.LTGRAY)
                }
            }

            // Button actions
            btnPurchaseViewProduct.setOnClickListener {
                val intent = Intent(context, ProductDetailActivity::class.java).apply {
                    putExtra("PRODUCT_ID", purchase.productId)
                }
                context.startActivity(intent)
            }

            btnPurchaseContactSeller.setOnClickListener {
                scope.launch {
                    try {
                        val progressDialog = android.app.ProgressDialog(context).apply {
                            setMessage("Conectando con el vendedor...")
                            setCancelable(false)
                            show()
                        }
                        val chatRoom = com.example.unimarketguajira.repository.ChatRepository.getOrCreateChatRoom(
                            context = context,
                            idComprador = purchase.buyerId,
                            idVendedor = purchase.sellerId,
                            idProducto = purchase.productId
                        )
                        progressDialog.dismiss()

                        val intent = Intent(context, com.example.unimarketguajira.activities.ChatActivity::class.java).apply {
                            putExtra("CHAT_ID", chatRoom.id)
                            putExtra("ID_COMPRADOR", chatRoom.id_comprador)
                            putExtra("ID_VENDEDOR", chatRoom.id_vendedor)
                            putExtra("ID_PRODUCTO", chatRoom.id_producto)
                            putExtra("PRODUCT_NAME", tvProductName.text.toString())
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            btnPurchaseDetails.setOnClickListener {
                val detailsMessage = """
                    Producto: ${tvProductName.text}
                    Vendedor: ${purchase.sellerId}
                    Fecha: ${tvPurchaseDate.text}
                    Cantidad: ${purchase.quantity}
                    Precio unitario: ${formatter.format(purchase.price)}
                    Total pagado: $totalPriceStr
                    Método de pago: ${purchase.paymentMethod}
                    Punto de entrega: ${purchase.deliveryPoint}
                    Estado actual: ${tvPurchaseStatus.text}
                """.trimIndent()

                MaterialAlertDialogBuilder(context)
                    .setTitle("Detalle de Compra")
                    .setMessage(detailsMessage)
                    .setPositiveButton("Aceptar", null)
                    .show()
            }
        }
    }
}
