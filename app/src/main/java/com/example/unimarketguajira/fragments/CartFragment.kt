package com.example.unimarketguajira.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.activities.ConfirmPurchaseActivity
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.models.loadProductImage
import com.example.unimarketguajira.repository.CartRepository
import com.example.unimarketguajira.repository.ProductRepository
import com.example.unimarketguajira.services.UserManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CartFragment : Fragment() {

    private lateinit var rvCartItems: RecyclerView
    private lateinit var rvRecommendations: RecyclerView
    
    private lateinit var tvTotalPrice: TextView
    private lateinit var tvItemsCount: TextView
    private lateinit var btnCheckout: Button
    
    private lateinit var layoutCartItems: LinearLayout
    private lateinit var layoutEmptyCart: LinearLayout
    private lateinit var bottomTotalCard: View

    private lateinit var tvSummarySubtotalLabel: TextView
    private lateinit var tvSummarySubtotal: TextView
    private lateinit var tvSummaryTotal: TextView

    private lateinit var cartAdapter: CartAdapter
    private var groupedCartItems: List<CartItem> = emptyList()

    data class CartItem(val product: Product, var quantity: Int)

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

        rvCartItems       = view.findViewById(R.id.rvCartItems)
        rvRecommendations = view.findViewById(R.id.rvRecommendations)
        tvTotalPrice      = view.findViewById(R.id.tvTotalPrice)
        tvItemsCount      = view.findViewById(R.id.tvItemsCount)
        btnCheckout       = view.findViewById(R.id.btnCheckout)
        layoutCartItems   = view.findViewById(R.id.layoutCartItems)
        layoutEmptyCart   = view.findViewById(R.id.layoutEmptyCart)
        bottomTotalCard   = view.findViewById(R.id.bottomTotalCard)

        tvSummarySubtotalLabel = view.findViewById(R.id.tvSummarySubtotalLabel)
        tvSummarySubtotal = view.findViewById(R.id.tvSummarySubtotal)
        tvSummaryTotal    = view.findViewById(R.id.tvSummaryTotal)

        view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }

        setupCartList()

        btnCheckout.setOnClickListener {
            if (groupedCartItems.isNotEmpty()) {
                val intent = Intent(requireContext(), ConfirmPurchaseActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "El carrito está vacío", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<Button>(R.id.btnExplore)?.setOnClickListener {
            (activity as? com.example.unimarketguajira.activities.MainActivity)
                ?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
                ?.selectedItemId = R.id.nav_home
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadCartData()
    }

    private fun setupCartList() {
        rvCartItems.layoutManager = LinearLayoutManager(requireContext())
        rvCartItems.isNestedScrollingEnabled = false
        cartAdapter = CartAdapter()
        rvCartItems.adapter = cartAdapter
    }

    private fun loadCartData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val email = UserManager.getLoggedUserEmail(requireContext()) ?: return@launch
            val rawProducts = CartRepository.getCartItems(requireContext(), email)
            
            // Agrupar productos por ID en memoria para calcular cantidades
            groupedCartItems = rawProducts.groupBy { it.id }.map { (_, list) ->
                CartItem(list.first(), list.size)
            }

            val total = CartRepository.getTotal(requireContext(), email)
            
            cartAdapter.updateItems(groupedCartItems)
            updateSummary(groupedCartItems, total)
            updateEmptyState(groupedCartItems)

            // Cargar recomendaciones inteligentes basadas en los productos del carrito
            loadRecommendations(rawProducts)
        }
    }

    private fun loadRecommendations(cartProducts: List<Product>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val allProducts = ProductRepository.getAllProducts(requireContext())
            val suggestions = generateRecommendations(cartProducts, allProducts)

            rvRecommendations.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            rvRecommendations.isNestedScrollingEnabled = false
            rvRecommendations.adapter = com.example.unimarketguajira.adapters.ProductSmallAdapter(suggestions)
        }
    }

    private fun generateRecommendations(cartProducts: List<Product>, allProducts: List<Product>): List<Product> {
        val cartIds = cartProducts.map { it.id }.toSet()
        val candidates = allProducts.filter { it.id !in cartIds }

        if (cartProducts.isEmpty()) {
            val academicKeywords = listOf("calculadora", "libro", "bata", "laboratorio", "apuntes", "tecnologia")
            return candidates.filter { prod ->
                academicKeywords.any { keyword -> prod.name.lowercase().contains(keyword) }
            }.shuffled().take(6).ifEmpty { candidates.shuffled().take(6) }
        }

        val cartCategories = cartProducts.map { it.category.lowercase() }.toSet()
        val cartKeywords = cartProducts.flatMap { it.name.lowercase().split(" ", "-", ":") }
            .filter { it.length > 3 && it != "para" && it != "libro" && it != "bata" }
            .toSet()

        val matchingProducts = candidates.filter { prod ->
            val isSameCategory = prod.category.lowercase() in cartCategories
            val nameWords = prod.name.lowercase().split(" ", "-", ":")
            val hasMatchingKeyword = nameWords.any { it in cartKeywords }
            
            val isCrossRecommendation = cartProducts.any { cartProd ->
                val cartName = cartProd.name.lowercase()
                val prodName = prod.name.lowercase()
                (cartName.contains("calculadora") && prodName.contains("calculo")) ||
                (cartName.contains("bata") && prodName.contains("laboratorio")) ||
                (cartName.contains("laboratorio") && prodName.contains("quimica"))
            }

            isSameCategory || hasMatchingKeyword || isCrossRecommendation
        }

        return if (matchingProducts.size < 4) {
            val filledList = (matchingProducts + candidates.shuffled()).distinctBy { it.id }
            filledList.take(6)
        } else {
            matchingProducts.distinctBy { it.id }.take(6)
        }
    }

    private fun updateSummary(items: List<CartItem>, total: Double) {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        format.maximumFractionDigits = 0
        
        val count = items.sumOf { it.quantity }
        tvSummarySubtotalLabel.text = "Productos ($count)"
        tvSummarySubtotal.text = format.format(total)
        tvSummaryTotal.text = format.format(total)
        tvTotalPrice.text = format.format(total)
        
        tvItemsCount.text = "$count ${if (count == 1) "artículo" else "artículos"}"
    }

    private fun updateEmptyState(items: List<CartItem>) {
        if (items.isEmpty()) {
            layoutCartItems.visibility = View.GONE
            layoutEmptyCart.visibility = View.VISIBLE
            bottomTotalCard.visibility = View.GONE
        } else {
            layoutCartItems.visibility = View.VISIBLE
            layoutEmptyCart.visibility = View.GONE
            bottomTotalCard.visibility = View.VISIBLE
        }
    }

    private fun updateCartItemQuantity(item: CartItem, newQty: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val email = UserManager.getLoggedUserEmail(requireContext()) ?: return@launch
            
            val dao = com.example.unimarketguajira.data.db.UniMarketDatabase.getDatabase(requireContext()).cartDao()
            val currentItems = dao.getCartProductsForUser(email).filter { it.id == item.product.id }
            val currentQty = currentItems.size
            
            if (newQty > currentQty) {
                val diff = newQty - currentQty
                for (i in 0 until diff) {
                    CartRepository.addToCart(requireContext(), item.product, email)
                }
            } else if (newQty < currentQty) {
                val diff = currentQty - newQty
                dao.removeFromCart(productId = item.product.id, userEmail = email)
                for (i in 0 until newQty) {
                    CartRepository.addToCart(requireContext(), item.product, email)
                }
            }
            loadCartData()
        }
    }

    private fun removeCartItem(item: CartItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val email = UserManager.getLoggedUserEmail(requireContext()) ?: return@launch
            val dao = com.example.unimarketguajira.data.db.UniMarketDatabase.getDatabase(requireContext()).cartDao()
            dao.removeFromCart(productId = item.product.id, userEmail = email)
            loadCartData()
        }
    }

    // ── Adapter ────────────────────────────────────────────────────────────────

    inner class CartAdapter : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

        private var itemsList: List<CartItem> = emptyList()

        fun updateItems(newItems: List<CartItem>) {
            itemsList = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val cartItem = itemsList[position]
            val product = cartItem.product

            holder.tvName.text = product.name
            holder.tvCategory.text = product.category
            holder.tvCondition.text = product.condition
            holder.tvLocation.text = product.location

            val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            format.maximumFractionDigits = 0
            holder.tvPrice.text = format.format(product.price)

            holder.ivImage.loadProductImage(product.imageUrls.firstOrNull())
            holder.tvQtyValue.text = cartItem.quantity.toString()

            holder.btnQtyDecrease.setOnClickListener {
                if (cartItem.quantity > 1) {
                    updateCartItemQuantity(cartItem, cartItem.quantity - 1)
                } else {
                    removeCartItem(cartItem)
                }
            }

            holder.btnQtyIncrease.setOnClickListener {
                updateCartItemQuantity(cartItem, cartItem.quantity + 1)
            }

            holder.btnRemove.setOnClickListener {
                removeCartItem(cartItem)
            }
        }

        override fun getItemCount() = itemsList.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivImage: ImageView = view.findViewById(R.id.ivProductImage)
            val tvName: TextView = view.findViewById(R.id.tvProductName)
            val tvCategory: TextView = view.findViewById(R.id.tvProductCategory)
            val tvCondition: TextView = view.findViewById(R.id.tvProductCondition)
            val tvLocation: TextView = view.findViewById(R.id.tvProductLocation)
            val tvPrice: TextView = view.findViewById(R.id.tvProductPrice)
            val tvQtyValue: TextView = view.findViewById(R.id.tvQtyValue)
            val btnQtyDecrease: View = view.findViewById(R.id.btnQtyDecrease)
            val btnQtyIncrease: View = view.findViewById(R.id.btnQtyIncrease)
            val btnRemove: View = view.findViewById(R.id.btnRemove)
        }
    }
}
