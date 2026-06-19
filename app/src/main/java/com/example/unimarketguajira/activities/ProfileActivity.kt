package com.example.unimarketguajira.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.unimarketguajira.R
import com.example.unimarketguajira.adapters.HorizontalProductAdapter
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.repository.ProductRepository
import com.example.unimarketguajira.repository.PurchaseRepository
import com.example.unimarketguajira.services.UserManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileRole: TextView
    private lateinit var tvProfilePhone: TextView
    private lateinit var tvProfileUniversity: TextView
    private lateinit var ivProfilePhoto: ImageView

    private lateinit var tvStatActive: TextView
    private lateinit var tvStatSold: TextView
    private lateinit var tvStatPurchases: TextView
    private lateinit var tvStatFavorites: TextView

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvHistoryEmpty: View
    private lateinit var adapterHistory: HorizontalProductAdapter

    private lateinit var rvFavorites: RecyclerView
    private lateinit var tvFavoritesEmpty: View
    private lateinit var adapterFavorites: HorizontalProductAdapter

    private lateinit var rvPurchases: RecyclerView
    private lateinit var tvPurchasesEmpty: View
    private lateinit var adapterPurchases: HorizontalProductAdapter

    private lateinit var rvPublications: RecyclerView
    private lateinit var tvPublicationsEmpty: View
    private lateinit var adapterPublications: HorizontalProductAdapter

    private var userEmail: String = ""

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            uploadProfilePhoto(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        val appBarLayout = findViewById<View>(R.id.appBarLayout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            appBarLayout.setPadding(0, systemBars.top, 0, 0)
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Views
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvProfileRole = findViewById(R.id.tvProfileRole)
        tvProfilePhone = findViewById(R.id.tvProfilePhone)
        tvProfileUniversity = findViewById(R.id.tvProfileUniversity)
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)

        tvStatActive = findViewById(R.id.tvStatActive)
        tvStatSold = findViewById(R.id.tvStatSold)
        tvStatPurchases = findViewById(R.id.tvStatPurchases)
        tvStatFavorites = findViewById(R.id.tvStatFavorites)

        rvHistory = findViewById(R.id.rvViewHistoryHorizontal)
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty)

        rvFavorites = findViewById(R.id.rvFavoritesHorizontal)
        tvFavoritesEmpty = findViewById(R.id.tvFavoritesEmpty)

        rvPurchases = findViewById(R.id.rvPurchasesHorizontal)
        tvPurchasesEmpty = findViewById(R.id.tvPurchasesEmpty)

        rvPublications = findViewById(R.id.rvPublicationsHorizontal)
        tvPublicationsEmpty = findViewById(R.id.tvPublicationsEmpty)

        // Setup layouts
        setupRecyclerViews()

        userEmail = UserManager.getLoggedUserEmail(this) ?: ""
        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Usuario no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<View>(R.id.btnEditProfile).setOnClickListener {
            showEditProfileDialog()
        }

        findViewById<View>(R.id.btnChangePassword).setOnClickListener {
            showChangePasswordDialog()
        }

        findViewById<View>(R.id.btnLogout).setOnClickListener {
            performLogout()
        }

        findViewById<View>(R.id.cardAvatar).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        loadProfileData()
        loadActivityData()
    }

    private fun setupRecyclerViews() {
        val lm1 = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val lm2 = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val lm3 = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val lm4 = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        rvHistory.layoutManager = lm1
        rvFavorites.layoutManager = lm2
        rvPurchases.layoutManager = lm3
        rvPublications.layoutManager = lm4

        adapterHistory = HorizontalProductAdapter(this, emptyList())
        adapterFavorites = HorizontalProductAdapter(this, emptyList())
        adapterPurchases = HorizontalProductAdapter(this, emptyList())
        adapterPublications = HorizontalProductAdapter(this, emptyList())

        rvHistory.adapter = adapterHistory
        rvFavorites.adapter = adapterFavorites
        rvPurchases.adapter = adapterPurchases
        rvPublications.adapter = adapterPublications
    }

    private fun loadProfileData() {
        lifecycleScope.launch {
            val user = UserManager.getLoggedUser(this@ProfileActivity)
            if (user != null) {
                tvProfileName.text = user.fullName
                tvProfileEmail.text = user.email
                tvProfileRole.text = if (user.role.isNotEmpty()) user.role else "Estudiante"
                tvProfilePhone.text = if (user.phoneNumber.isNotEmpty()) user.phoneNumber else "Sin teléfono registrado"
                tvProfileUniversity.text = if (user.university.isNotEmpty()) user.university else "Universidad de La Guajira"

                if (user.profilePhotoUrl.isNotEmpty()) {
                    ivProfilePhoto.clearColorFilter()
                    Glide.with(this@ProfileActivity)
                        .load(user.profilePhotoUrl)
                        .placeholder(android.R.drawable.ic_menu_myplaces)
                        .into(ivProfilePhoto)
                } else {
                    ivProfilePhoto.setImageResource(android.R.drawable.ic_menu_myplaces)
                    ivProfilePhoto.setColorFilter(getColor(R.color.primary))
                }
            }
        }
    }

    private fun loadActivityData() {
        lifecycleScope.launch {
            // 1. Historial
            val historyList = ProductRepository.getProductHistory(this@ProfileActivity, userEmail)
            adapterHistory.updateData(historyList)
            tvHistoryEmpty.visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE

            // 2. Favoritos
            val allProducts = ProductRepository.getAllProducts(this@ProfileActivity)
            val favoritesList = allProducts.filter { it.isFavorite }
            adapterFavorites.updateData(favoritesList)
            tvFavoritesEmpty.visibility = if (favoritesList.isEmpty()) View.VISIBLE else View.GONE

            // 3. Compras
            val purchasesList = PurchaseRepository.getPurchasesForBuyer(this@ProfileActivity, userEmail)
            val purchasedProducts = mutableListOf<Product>()
            for (purchase in purchasesList) {
                val product = ProductRepository.getProductById(this@ProfileActivity, purchase.productId)
                if (product != null) {
                    purchasedProducts.add(product)
                }
            }
            adapterPurchases.updateData(purchasedProducts)
            tvPurchasesEmpty.visibility = if (purchasedProducts.isEmpty()) View.VISIBLE else View.GONE

            // 4. Publicaciones creadas
            val myProducts = ProductRepository.getMyProducts(this@ProfileActivity, userEmail)
            adapterPublications.updateData(myProducts)
            tvPublicationsEmpty.visibility = if (myProducts.isEmpty()) View.VISIBLE else View.GONE

            // 5. Actualizar Estadísticas Dashboard
            val activeCount = myProducts.count { it.status == "ACTIVE" }
            val soldCount = myProducts.count { it.status == "SOLD" }
            
            tvStatActive.text = activeCount.toString()
            tvStatSold.text = soldCount.toString()
            tvStatPurchases.text = purchasesList.size.toString()
            tvStatFavorites.text = favoritesList.size.toString()
        }
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_profile, null)
        val tilName = dialogView.findViewById<TextInputLayout>(R.id.tilProfileName)
        val tilRole = dialogView.findViewById<TextInputLayout>(R.id.tilProfileRole)
        val tilPhone = dialogView.findViewById<TextInputLayout>(R.id.tilProfilePhone)
        val tilUniversity = dialogView.findViewById<TextInputLayout>(R.id.tilProfileUniversity)

        // Ocultar elementos informativos estáticos
        dialogView.findViewById<View>(R.id.tvProfileName).visibility = View.GONE
        dialogView.findViewById<View>(R.id.tvProfileRole).visibility = View.GONE
        dialogView.findViewById<View>(R.id.tvProfileEmail).visibility = View.GONE
        dialogView.findViewById<View>(R.id.tvProfileUniversity).visibility = View.GONE

        // Mostrar TextInputLayouts
        tilName.visibility = View.VISIBLE
        tilRole.visibility = View.VISIBLE
        tilPhone.visibility = View.VISIBLE
        tilUniversity.visibility = View.VISIBLE

        lifecycleScope.launch {
            val user = UserManager.getLoggedUser(this@ProfileActivity)
            user?.let {
                tilName.editText?.setText(it.fullName)
                tilRole.editText?.setText(it.role)
                tilPhone.editText?.setText(it.phoneNumber)
                tilUniversity.editText?.setText(it.university)

                val ivDialogAvatar = dialogView.findViewById<ImageView>(R.id.ivDialogAvatar)
                if (it.profilePhotoUrl.isNotEmpty()) {
                    ivDialogAvatar.clearColorFilter()
                    Glide.with(this@ProfileActivity)
                        .load(it.profilePhotoUrl)
                        .placeholder(android.R.drawable.ic_menu_myplaces)
                        .into(ivDialogAvatar)
                } else {
                    ivDialogAvatar.setImageResource(android.R.drawable.ic_menu_myplaces)
                }
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Perfil")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = tilName.editText?.text.toString().trim()
                val newRole = tilRole.editText?.text.toString().trim()
                val newPhone = tilPhone.editText?.text.toString().trim()
                val newUniversity = tilUniversity.editText?.text.toString().trim()

                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        val user = UserManager.getLoggedUser(this@ProfileActivity)
                        if (user != null) {
                            val updatedUser = user.copy(
                                fullName = newName,
                                role = newRole,
                                phoneNumber = newPhone,
                                university = newUniversity
                            )
                            val success = UserManager.updateUser(this@ProfileActivity, updatedUser)
                            if (success) {
                                Toast.makeText(this@ProfileActivity, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                                loadProfileData()
                                // Notificación automática al cambiar perfil
                                PurchaseRepository.createSystemNotification(
                                    context = this@ProfileActivity,
                                    userId = userEmail,
                                    title = "Perfil actualizado",
                                    message = "Tu información de perfil ha sido actualizada exitosamente.",
                                    type = "SISTEMA",
                                    relatedProductId = 0
                                )
                            } else {
                                Toast.makeText(this@ProfileActivity, "Error al actualizar perfil", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this@ProfileActivity, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val tilCurrent = dialogView.findViewById<TextInputLayout>(R.id.tilCurrentPassword)
        val tilNew = dialogView.findViewById<TextInputLayout>(R.id.tilNewPassword)
        val tilConfirm = dialogView.findViewById<TextInputLayout>(R.id.tilConfirmPassword)

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Cambiar") { _, _ ->
                val current = tilCurrent.editText?.text.toString()
                val newPass = tilNew.editText?.text.toString()
                val confirm = tilConfirm.editText?.text.toString()

                if (newPass.isEmpty() || confirm.isEmpty() || current.isEmpty()) {
                    Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPass != confirm) {
                    Toast.makeText(this, "Las contraseñas nuevas no coinciden", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val user = UserManager.getLoggedUser(this@ProfileActivity)
                    if (user != null) {
                        if (user.password != current) {
                            Toast.makeText(this@ProfileActivity, "La contraseña actual es incorrecta", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val updatedUser = user.copy(password = newPass)
                        val success = UserManager.updateUser(this@ProfileActivity, updatedUser)
                        if (success) {
                            Toast.makeText(this@ProfileActivity, "Contraseña cambiada exitosamente", Toast.LENGTH_SHORT).show()
                            PurchaseRepository.createSystemNotification(
                                context = this@ProfileActivity,
                                userId = userEmail,
                                title = "Contraseña cambiada",
                                message = "Actualizaste la contraseña de tu cuenta de UniMarket Guajira.",
                                type = "SISTEMA",
                                relatedProductId = 0
                            )
                        } else {
                            Toast.makeText(this@ProfileActivity, "Error al cambiar contraseña", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun performLogout() {
        UserManager.logout(this)
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun uploadProfilePhoto(uri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference.child("profiles/$userEmail.jpg")
        Toast.makeText(this, "Subiendo foto...", Toast.LENGTH_SHORT).show()

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val photoUrl = downloadUri.toString()
                    lifecycleScope.launch {
                        val user = UserManager.getLoggedUser(this@ProfileActivity)
                        if (user != null) {
                            val updatedUser = user.copy(profilePhotoUrl = photoUrl)
                            val success = UserManager.updateUser(this@ProfileActivity, updatedUser)
                            if (success) {
                                Toast.makeText(this@ProfileActivity, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                                loadProfileData()
                                PurchaseRepository.createSystemNotification(
                                    context = this@ProfileActivity,
                                    userId = userEmail,
                                    title = "Foto de perfil cambiada",
                                    message = "Tu foto de perfil ha sido actualizada exitosamente.",
                                    type = "SISTEMA",
                                    relatedProductId = 0
                                )
                            }
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Error al obtener URL de descarga", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al subir foto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
