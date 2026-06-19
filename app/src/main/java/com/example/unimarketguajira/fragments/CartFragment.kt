package com.example.unimarketguajira.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.repository.CartRepository
import java.text.NumberFormat
import java.util.Locale

class CartFragment : Fragment() {

    private lateinit var rvCartItems: RecyclerView
    private lateinit var tvTotalPrice: TextView
    private lateinit var btnCheckout: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cart, container, false)

        val statusBarSpacer = view.findViewById<View>(R.id.statusBarSpacer)
        ViewCompat.setOnApplyWindowInsetsListener(statusBarSpacer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.layoutParams.height = systemBars.top
            v.requestLayout()
            insets
        }

        rvCartItems = view.findViewById(R.id.rvCartItems)
        tvTotalPrice = view.findViewById(R.id.tvTotalPrice)
        btnCheckout = view.findViewById(R.id.btnCheckout)

        setupCartList()
        updateTotal()

        btnCheckout.setOnClickListener {
            if (CartRepository.getCartItems().isNotEmpty()) {
                Toast.makeText(requireContext(), "Procesando pedido...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "El carrito está vacío", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun setupCartList() {
        rvCartItems.layoutManager = LinearLayoutManager(requireContext())
        rvCartItems.adapter = CartAdapter(CartRepository.getCartItems())
    }

    private fun updateTotal() {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        tvTotalPrice.text = format.format(CartRepository.getTotal())
    }

    inner class CartAdapter(private var items: List<Product>) :
        RecyclerView.Adapter<CartAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cart, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            
            val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            holder.tvPrice.text = format.format(item.price)
            
            if (item.images.isNotEmpty()) {
                holder.ivImage.setImageResource(item.images[0])
            }

            holder.btnRemove.setOnClickListener {
                CartRepository.removeFromCart(item)
                items = CartRepository.getCartItems()
                notifyDataSetChanged()
                updateTotal()
            }
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivImage: ImageView = view.findViewById(R.id.ivProductImage)
            val tvName: TextView = view.findViewById(R.id.tvProductName)
            val tvPrice: TextView = view.findViewById(R.id.tvProductPrice)
            val btnRemove: ImageButton = view.findViewById(R.id.btnRemove)
        }
    }
}