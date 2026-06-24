package com.example.unimarketguajira.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.data.db.UniMarketDatabase
import com.example.unimarketguajira.models.ChatRoom
import com.example.unimarketguajira.models.loadProductImage
import com.example.unimarketguajira.repository.ProductRepository
import com.example.unimarketguajira.services.UserManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private val context: Context,
    private var chatRooms: List<ChatRoom>,
    private val currentUserEmail: String,
    private val onItemClick: (ChatRoom) -> Unit,
    private val onArchiveClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    fun updateData(newRooms: List<ChatRoom>) {
        chatRooms = newRooms
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chatRoom = chatRooms[position]
        holder.bind(chatRoom)
    }

    override fun getItemCount(): Int = chatRooms.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProductPhoto: ImageView = itemView.findViewById(R.id.ivProductPhoto)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvLastMessageTime: TextView = itemView.findViewById(R.id.tvLastMessageTime)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val tvProductStatus: TextView = itemView.findViewById(R.id.tvProductStatus)
        private val cardProductStatus: View = itemView.findViewById(R.id.cardProductStatus)
        private val cardUnreadBadge: View = itemView.findViewById(R.id.cardUnreadBadge)
        private val tvUnreadCount: TextView = itemView.findViewById(R.id.tvUnreadCount)
        private val ibArchive: ImageButton = itemView.findViewById(R.id.ibArchive)

        fun bind(chatRoom: ChatRoom) {
            val otherUserEmail = if (currentUserEmail == chatRoom.id_comprador) chatRoom.id_vendedor else chatRoom.id_comprador

            // Nombre de fallback
            val fallbackName = otherUserEmail.split("@").firstOrNull()?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            } ?: "Usuario"

            tvUserName.text = fallbackName
            tvLastMessage.text = chatRoom.lastMessage
            tvLastMessageTime.text = formatTime(chatRoom.timestamp)
            tvProductName.text = "Producto #${chatRoom.id_producto}"

            // Cambiar icono de archivo según estado
            if (chatRoom.status == "ARCHIVED") {
                ibArchive.setImageResource(android.R.drawable.ic_menu_revert)
                ibArchive.contentDescription = "Desarchivar"
            } else {
                ibArchive.setImageResource(android.R.drawable.ic_menu_save)
                ibArchive.contentDescription = "Archivar"
            }

            ibArchive.setOnClickListener {
                onArchiveClick(chatRoom)
            }

            itemView.setOnClickListener {
                onItemClick(chatRoom)
            }

            // Cargar datos asíncronos mediante coroutines locales vinculadas al ciclo de vida de la vista
            val lifecycleOwner = itemView.findViewTreeLifecycleOwner()
            lifecycleOwner?.lifecycleScope?.launch {
                // 1. Cargar nombre del otro usuario
                val otherUser = UserManager.getUserByEmail(context, otherUserEmail)
                if (otherUser != null) {
                    tvUserName.text = otherUser.fullName
                }

                // 2. Cargar detalles del producto
                val product = ProductRepository.getProductById(context, chatRoom.id_producto)
                if (product != null) {
                    tvProductName.text = product.name
                    if (product.imageUrls.isNotEmpty()) {
                        ivProductPhoto.loadProductImage(product.imageUrls.first())
                    } else {
                        ivProductPhoto.loadProductImage(null)
                    }

                    // Configurar el estado visual del producto
                    val statusText = when (product.status) {
                        "ACTIVE" -> "Activo"
                        "INACTIVE" -> "Inactivo"
                        "SOLD" -> "Producto vendido"
                        else -> product.status
                    }
                    tvProductStatus.text = statusText

                    // Colores del Badge de Estado
                    val colorHex = when (product.status) {
                        "SOLD" -> "#D32F2F" // Rojo
                        "ACTIVE" -> "#2E7D32" // Verde
                        else -> "#757575" // Gris
                    }
                    cardProductStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))
                    tvProductStatus.setTextColor(Color.WHITE)
                } else {
                    ivProductPhoto.loadProductImage(null)
                    tvProductStatus.text = "Desconocido"
                }

                // 3. Cargar cantidad de mensajes sin leer
                val localDb = UniMarketDatabase.getDatabase(context)
                val unreadCount = localDb.notificationDao().getUnreadCountForChat(currentUserEmail, chatRoom.id)
                if (unreadCount > 0) {
                    cardUnreadBadge.visibility = View.VISIBLE
                    tvUnreadCount.text = unreadCount.toString()

                    // Resaltar visualmente conversación no leída
                    itemView.setBackgroundColor(Color.parseColor("#F1F8E9")) // Tonalidad verde muy claro
                    tvLastMessage.textStyleBold(true)
                } else {
                    cardUnreadBadge.visibility = View.GONE
                    itemView.setBackgroundColor(Color.WHITE)
                    tvLastMessage.textStyleBold(false)
                }
            }
        }

        private fun TextView.textStyleBold(bold: Boolean) {
            val style = if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
            this.setTypeface(null, style)
        }

        private fun formatTime(timestamp: Long): String {
            if (timestamp == 0L) return ""
            val sdf = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}
