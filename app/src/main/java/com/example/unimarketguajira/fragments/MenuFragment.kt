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
import com.example.unimarketguajira.activities.MyProductsActivity
import com.example.unimarketguajira.activities.MySalesActivity
import com.example.unimarketguajira.activities.NotificationsActivity
import com.example.unimarketguajira.activities.ProfileActivity
import com.example.unimarketguajira.activities.MyPurchasesActivity
import com.example.unimarketguajira.activities.ViewHistoryActivity
import com.example.unimarketguajira.activities.FavoritesActivity
import com.example.unimarketguajira.activities.ConversationsActivity
import com.example.unimarketguajira.services.UserManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MenuFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)
        
        val headerContainer = view.findViewById<View>(R.id.headerContainer)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(headerContainer) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top + 16.dpToPx(), v.paddingRight, 16.dpToPx())
            insets
        }

        setupMenuSections(view)
        
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val ivUserProfile = view.findViewById<ImageView>(R.id.ivUserProfile)

        viewLifecycleOwner.lifecycleScope.launch {
            UserManager.observeLoggedUser(requireContext()).collect { user ->
                tvUserName.text = user?.fullName ?: "Estudiante"
                if (user != null && ivUserProfile != null) {
                    if (user.profilePhotoUrl.isNotEmpty()) {
                        ivUserProfile.clearColorFilter()
                        com.bumptech.glide.Glide.with(this@MenuFragment)
                            .load(user.profilePhotoUrl)
                            .placeholder(android.R.drawable.ic_menu_myplaces)
                            .into(ivUserProfile)
                    } else {
                        ivUserProfile.setImageResource(android.R.drawable.ic_menu_myplaces)
                    }
                }
            }
        }
        
        return view
    }

    private fun openFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openCategory(categoryName: String) {
        val fragment = HomeFragment().apply {
            arguments = Bundle().apply {
                putString("CATEGORY", categoryName)
            }
        }
        openFragment(fragment)
    }

    private fun showProfileDialog(user: com.example.unimarketguajira.models.User) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_profile, null)
        dialogView.findViewById<TextView>(R.id.tvProfileName).text = user.fullName
        dialogView.findViewById<TextView>(R.id.tvProfileEmail).text = user.email
        dialogView.findViewById<TextView>(R.id.tvProfileRole).text = "Estudiante UniGuajira"
        dialogView.findViewById<TextView>(R.id.tvProfileUniversity).text = "Universidad de La Guajira"
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun setupMenuSections(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.menuSectionsContainer)

        addSection(container, "PRINCIPAL", listOf(
            MenuOption("Inicio", R.drawable.ic_home) {
                openFragment(HomeFragment())
            },
            MenuOption("Mi perfil", android.R.drawable.ic_menu_myplaces) {
                startActivity(Intent(requireContext(), ProfileActivity::class.java))
            },
            MenuOption("Mis publicaciones", R.drawable.ic_edit_note) {
                startActivity(Intent(requireContext(), MyProductsActivity::class.java))
            },
            MenuOption("Favoritos", android.R.drawable.btn_star_big_on) {
                startActivity(Intent(requireContext(), FavoritesActivity::class.java))
            },
            MenuOption("Historial", android.R.drawable.ic_menu_recent_history) {
                startActivity(Intent(requireContext(), ViewHistoryActivity::class.java))
            }
        ))

        addSection(container, "ACTIVIDAD", listOf(
            MenuOption("Compras", android.R.drawable.ic_menu_agenda) {
                startActivity(Intent(requireContext(), MyPurchasesActivity::class.java))
            },
            MenuOption("Ventas", android.R.drawable.ic_menu_send) {
                startActivity(Intent(requireContext(), MySalesActivity::class.java))
            },
            MenuOption("Mensajes", android.R.drawable.sym_action_chat) {
                startActivity(Intent(requireContext(), ConversationsActivity::class.java))
            },
            MenuOption("Notificaciones", R.drawable.ic_notifications) {
                startActivity(Intent(requireContext(), NotificationsActivity::class.java))
            }
        ))

        addSection(container, "MARKETPLACE", listOf(
            MenuOption("Categorías", R.drawable.ic_categories) {
                openFragment(CategoriesFragment())
            },
            MenuOption("Publicar producto", android.R.drawable.ic_input_add) {
                startActivity(Intent(requireContext(), PublishProductActivity::class.java))
            },
            MenuOption("Mis productos", R.drawable.ic_school) {
                startActivity(Intent(requireContext(), MyProductsActivity::class.java))
            }
        ))

        addSection(container, "UNIVERSIDAD", listOf(
            MenuOption("Artículos académicos", R.drawable.ic_book) {
                openCategory(getString(R.string.cat_notes))
            },
            MenuOption("Libros", R.drawable.ic_book) {
                openCategory(getString(R.string.cat_books))
            },
            MenuOption("Laboratorio", R.drawable.ic_science) {
                openCategory(getString(R.string.cat_lab))
            },
            MenuOption("Batas y Uniformes", R.drawable.ic_school) {
                openCategory(getString(R.string.cat_supplies))
            }
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