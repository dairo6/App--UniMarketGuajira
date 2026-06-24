package com.example.unimarketguajira.activities

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R
import com.example.unimarketguajira.adapters.PublishImageAdapter
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.repository.ProductRepository
import com.example.unimarketguajira.repository.PurchaseRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText

import java.io.File
import java.io.FileOutputStream

class PublishProductActivity : AppCompatActivity() {

    private val selectedImages = mutableListOf<Uri>()
    private lateinit var imageAdapter: PublishImageAdapter
    private var isEditMode = false
    private var editingProduct: Product? = null

    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        if (uris.isNotEmpty()) {
            val availableSlots = 5 - selectedImages.size
            selectedImages.addAll(uris.take(availableSlots))
            imageAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_publish_product)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout).setPadding(0, systemBars.top, 0, 0)
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupImageRecyclerView()
        setupSpinners()

        findViewById<Button>(R.id.btnPublishTop).setOnClickListener {
            handlePublish()
        }

        val editProductId = intent.getIntExtra("EDIT_PRODUCT_ID", -1)
        if (editProductId != -1) {
            setupEditMode(editProductId)
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupImageRecyclerView() {
        val rvImages = findViewById<RecyclerView>(R.id.rvImages)
        imageAdapter = PublishImageAdapter(
            selectedImages,
            onAddClick = {
                pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemoveClick = { position ->
                selectedImages.removeAt(position)
                imageAdapter.notifyDataSetChanged()
            }
        )
        rvImages.adapter = imageAdapter

        // Soporte de reordenamiento premium por drag-and-drop
        val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
            androidx.recyclerview.widget.ItemTouchHelper.LEFT or androidx.recyclerview.widget.ItemTouchHelper.RIGHT, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition

                if (fromPos >= selectedImages.size || toPos >= selectedImages.size) {
                    return false
                }

                java.util.Collections.swap(selectedImages, fromPos, toPos)
                imageAdapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        itemTouchHelper.attachToRecyclerView(rvImages)
    }

    private fun setupSpinners() {
        // Categorías
        val categories = arrayOf(
            getString(R.string.cat_books),
            getString(R.string.cat_supplies),
            getString(R.string.cat_lab),
            getString(R.string.cat_tech),
            getString(R.string.cat_notes),
            getString(R.string.cat_others)
        )
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        findViewById<AutoCompleteTextView>(R.id.spinnerCategory).setAdapter(catAdapter)

        // Estado del producto
        val conditions = arrayOf(
            getString(R.string.condition_new),
            getString(R.string.condition_used_like_new),
            getString(R.string.condition_used_good),
            getString(R.string.condition_used_acceptable)
        )
        val condAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, conditions)
        findViewById<AutoCompleteTextView>(R.id.spinnerCondition).setAdapter(condAdapter)

        // Ubicación
        val locations = arrayOf("Riohacha", "Maicao", "Fonseca", "Villanueva")
        val locAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, locations)
        findViewById<AutoCompleteTextView>(R.id.spinnerLocation).setAdapter(locAdapter)
    }

    private fun copyUriToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "img_${System.currentTimeMillis()}_${(0..1000).random()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun handlePublish() {
        val name = findViewById<TextInputEditText>(R.id.etProductName).text.toString().trim()
        val priceText = findViewById<TextInputEditText>(R.id.etPrice).text.toString().trim()
        val category = findViewById<AutoCompleteTextView>(R.id.spinnerCategory).text.toString()
        val condition = findViewById<AutoCompleteTextView>(R.id.spinnerCondition).text.toString()
        val description = findViewById<TextInputEditText>(R.id.etDescription).text.toString().trim()
        val location = findViewById<AutoCompleteTextView>(R.id.spinnerLocation).text.toString()
        val quantityText = findViewById<TextInputEditText>(R.id.etQuantity).text.toString().trim()

        if (name.isEmpty() || priceText.isEmpty() || category.isEmpty() || condition.isEmpty() || description.isEmpty() || location.isEmpty() || quantityText.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Agrega al menos una foto", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceText.toDoubleOrNull() ?: 0.0
        val stock = quantityText.toIntOrNull() ?: 1

        val savedImagePaths = selectedImages.mapNotNull { uri ->
            if (uri.scheme == "file" || uri.scheme == "http" || uri.scheme == "https") {
                if (uri.scheme == "file") uri.path ?: uri.toString() else uri.toString()
            } else {
                copyUriToInternalStorage(uri)
            }
        }

        if (savedImagePaths.isEmpty()) {
            Toast.makeText(this, "Error al procesar las imágenes", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_upload_progress, null)
        val progressIndicator = dialogView.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressIndicator)
        val tvUploadPercentage = dialogView.findViewById<android.widget.TextView>(R.id.tvUploadPercentage)

        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCancelable(false)
            .setView(dialogView)
            .create()

        progressDialog.show()

        lifecycleScope.launch {
            val email = com.example.unimarketguajira.services.UserManager.getLoggedUserEmail(this@PublishProductActivity) ?: run {
                progressDialog.dismiss()
                return@launch
            }

            try {
                if (isEditMode && editingProduct != null) {
                    val updatedProduct = editingProduct!!.copy(
                        name = name,
                        description = description,
                        price = price,
                        location = location,
                        imageUrls = savedImagePaths,
                        category = category,
                        condition = condition,
                        stock = stock
                    )
                    ProductRepository.updateProduct(this@PublishProductActivity, updatedProduct, email) { progress ->
                        runOnUiThread {
                            progressIndicator.progress = progress
                            tvUploadPercentage.text = "$progress%"
                        }
                    }
                    progressDialog.dismiss()
                    Toast.makeText(this@PublishProductActivity, "Producto publicado correctamente", Toast.LENGTH_LONG).show()
                    PurchaseRepository.createSystemNotification(
                        context = this@PublishProductActivity,
                        userId = email,
                        title = "Publicación editada",
                        message = "Tu publicación \"$name\" fue actualizada con éxito.",
                        type = "SISTEMA",
                        relatedProductId = updatedProduct.id
                    )
                } else {
                    val newProduct = Product(
                        id = System.currentTimeMillis().toInt(),
                        name = name,
                        description = description,
                        price = price,
                        location = location,
                        imageUrls = savedImagePaths,
                        category = category,
                        condition = condition,
                        status = "ACTIVE",
                        stock = stock
                    )
                    ProductRepository.addProduct(this@PublishProductActivity, newProduct, email) { progress ->
                        runOnUiThread {
                            progressIndicator.progress = progress
                            tvUploadPercentage.text = "$progress%"
                        }
                    }
                    progressDialog.dismiss()
                    Toast.makeText(this@PublishProductActivity, "Producto publicado correctamente", Toast.LENGTH_LONG).show()
                    PurchaseRepository.createSystemNotification(
                        context = this@PublishProductActivity,
                        userId = email,
                        title = "Publicación exitosa",
                        message = "Tu publicación \"$name\" fue creada con éxito.",
                        type = "SISTEMA",
                        relatedProductId = newProduct.id
                    )
                }
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                progressDialog.dismiss()
                Toast.makeText(this@PublishProductActivity, "Fallo al publicar el producto: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupEditMode(productId: Int) {
        isEditMode = true
        findViewById<Button>(R.id.btnPublishTop).text = "Guardar"
        findViewById<MaterialToolbar>(R.id.toolbar).title = "Editar Publicación"

        lifecycleScope.launch {
            val product = ProductRepository.getProductById(this@PublishProductActivity, productId)
            if (product != null) {
                editingProduct = product
                findViewById<TextInputEditText>(R.id.etProductName).setText(product.name)
                findViewById<TextInputEditText>(R.id.etPrice).setText(product.price.toString())
                findViewById<TextInputEditText>(R.id.etDescription).setText(product.description)
                findViewById<TextInputEditText>(R.id.etQuantity).setText(product.stock.toString())
                
                findViewById<AutoCompleteTextView>(R.id.spinnerCategory).setText(product.category, false)
                findViewById<AutoCompleteTextView>(R.id.spinnerCondition).setText(product.condition, false)
                findViewById<AutoCompleteTextView>(R.id.spinnerLocation).setText(product.location, false)

                selectedImages.clear()
                product.imageUrls.forEach { path ->
                    try {
                        val uri = if (path.startsWith("/") || path.startsWith("file:")) {
                            Uri.fromFile(File(path))
                        } else {
                            Uri.parse(path)
                        }
                        selectedImages.add(uri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                imageAdapter.notifyDataSetChanged()
            }
        }
    }
}