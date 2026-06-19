package com.example.unimarketguajira.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.models.Notification
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val context: Context,
    private var notifications: List<Notification>,
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    fun updateData(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.bind(notification)
    }

    override fun getItemCount(): Int = notifications.size

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardIconBg: MaterialCardView = itemView.findViewById(R.id.cardNotificationIconBg)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivNotificationIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvNotificationTime)
        private val cardUnreadDot: MaterialCardView = itemView.findViewById(R.id.cardUnreadDot)
        private val layoutContainer: View = itemView.findViewById(R.id.layoutNotificationContainer)

        fun bind(notification: Notification) {
            tvTitle.text = notification.title
            tvMessage.text = notification.message
            tvTime.text = formatRelativeTime(notification.createdAt)

            // Configurar aspecto según lectura
            if (notification.isRead) {
                cardUnreadDot.visibility = View.GONE
                tvTitle.setTypeface(null, Typeface.NORMAL)
                layoutContainer.setBackgroundColor(Color.TRANSPARENT)
            } else {
                cardUnreadDot.visibility = View.VISIBLE
                tvTitle.setTypeface(null, Typeface.BOLD)
                // Color de fondo sutil para no leídas
                layoutContainer.setBackgroundColor(Color.parseColor("#0F2E7D32")) // 6% Green tint
            }

            // Configurar icono según tipo
            when (notification.type) {
                "VENTA" -> {
                    ivIcon.setImageResource(android.R.drawable.ic_menu_send)
                    ivIcon.setColorFilter(Color.parseColor("#2E7D32")) // Forest green
                    cardIconBg.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                }
                "COMPRA" -> {
                    ivIcon.setImageResource(android.R.drawable.ic_menu_agenda)
                    ivIcon.setColorFilter(Color.parseColor("#1976D2")) // Blue
                    cardIconBg.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                }
                "FAVORITO" -> {
                    ivIcon.setImageResource(android.R.drawable.btn_star_big_on)
                    ivIcon.setColorFilter(Color.parseColor("#F57C00")) // Orange
                    cardIconBg.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
                }
                else -> { // SISTEMA o otros
                    ivIcon.setImageResource(android.R.drawable.ic_popup_reminder)
                    ivIcon.setColorFilter(Color.parseColor("#616161")) // Grey
                    cardIconBg.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
                }
            }

            itemView.setOnClickListener {
                onNotificationClick(notification)
            }
        }

        private fun formatRelativeTime(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                diff < 0 -> "Ahora mismo"
                seconds < 60 -> "Hace unos instantes"
                minutes < 60 -> "Hace $minutes min"
                hours < 24 -> "Hace $hours h"
                days < 7 -> "Hace $days d"
                else -> {
                    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("es", "CO"))
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}
