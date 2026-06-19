package com.example.unimarketguajira.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.unimarketguajira.R
import com.example.unimarketguajira.activities.LoginActivity
import com.example.unimarketguajira.activities.PublishProductActivity
import com.example.unimarketguajira.services.UserManager

class MenuFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)
        
        setupMenuSections(view)
        
        val userName = UserManager.getLoggedUser(requireContext())?.fullName ?: "Estudiante"
        view.findViewById<TextView>(R.id.tvUserName).text = userName
        
        return view
    }

    private fun setupMenuSections(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.menuSectionsContainer)

        addSection(container, "PRINCIPAL", listOf(
            MenuOption("Inicio", R.drawable.ic_home) { /* Ya estamos en main */ },
            MenuOption("Mi perfil", android.R.drawable.ic_menu_myplaces),
            MenuOption("Mis publicaciones", R.drawable.ic_edit_note),
            MenuOption("Favoritos", android.R.drawable.btn_star_big_on),
            MenuOption("Historial", android.R.drawable.ic_menu_recent_history)
        ))

        addSection(container, "ACTIVIDAD", listOf(
            MenuOption("Productos guardados", R.drawable.ic_cart),
            MenuOption("Ventas", android.R.drawable.ic_menu_send),
            MenuOption("Compras", android.R.drawable.ic_menu_agenda),
            MenuOption("Notificaciones", R.drawable.ic_notifications)
        ))

        addSection(container, "MARKETPLACE", listOf(
            MenuOption("Categorías", R.drawable.ic_categories),
            MenuOption("Publicar producto", android.R.drawable.ic_input_add) {
                startActivity(Intent(requireContext(), PublishProductActivity::class.java))
            },
            MenuOption("Mis productos", R.drawable.ic_school)
        ))

        addSection(container, "UNIVERSIDAD", listOf(
            MenuOption("Artículos académicos", R.drawable.ic_book),
            MenuOption("Libros", R.drawable.ic_book),
            MenuOption("Laboratorio", R.drawable.ic_science),
            MenuOption("Batas y Uniformes", R.drawable.ic_school)
        ))

        addSection(container, "CONFIGURACIÓN", listOf(
            MenuOption("Privacidad", android.R.drawable.ic_lock_idle_lock),
            MenuOption("Ayuda y soporte", android.R.drawable.ic_menu_help),
            MenuOption("Cerrar sesión", android.R.drawable.ic_menu_close_clear_cancel) { logout() }
        ))
    }

    private fun addSection(container: LinearLayout, title: String, options: List<MenuOption>) {
        val sectionTitle = TextView(requireContext()).apply {
            text = title
            textSize = 12f
            setPadding(0, 24.dpToPx(), 0, 8.dpToPx())
            setTextColor(requireContext().getColor(android.R.color.darker_gray))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        container.addView(sectionTitle)

        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            radius = 12.dpToPx().toFloat()
            cardElevation = 2.dpToPx().toFloat()
            useCompatPadding = true
        }

        val optionsContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dpToPx(), 0, 16.dpToPx(), 0)
        }

        options.forEachIndexed { index, option ->
            val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_menu_option, optionsContainer, false)
            view.findViewById<TextView>(R.id.tvOptionTitle).text = option.title
            view.findViewById<ImageView>(R.id.ivOptionIcon).setImageResource(option.icon)
            view.setOnClickListener { option.action?.invoke() ?: Toast.makeText(requireContext(), "${option.title} próximamente", Toast.LENGTH_SHORT).show() }
            
            optionsContainer.addView(view)

            if (index < options.size - 1) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1.dpToPx())
                    setBackgroundColor(requireContext().getColor(android.R.color.darker_gray))
                    alpha = 0.1f
                }
                optionsContainer.addView(divider)
            }
        }

        card.addView(optionsContainer)
        container.addView(card)
    }

    private fun logout() {
        UserManager.logout(requireContext())
        startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        activity?.finish()
    }

    private fun Int.dpToPx() = (this * resources.displayMetrics.density).toInt()

    data class MenuOption(val title: String, val icon: Int, val action: (() -> Unit)? = null)
}