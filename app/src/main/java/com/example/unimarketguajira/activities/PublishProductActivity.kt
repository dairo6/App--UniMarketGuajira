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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText

class PublishProductActivity : AppCompatActivity() {

    private val selectedImages = mutableListOf<Uri>()
    private lateinit var imageAdapter: PublishImageAdapter

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
    }

    private fun handlePublish() {
        val name = findViewById<TextInputEditText>(R.id.etProductName).text.toString().trim()
        val priceText = findViewById<TextInputEditText>(R.id.etPrice).text.toString().trim()
        val category = findViewById<AutoCompleteTextView>(R.id.spinnerCategory).text.toString()
        val condition = findViewById<AutoCompleteTextView>(R.id.spinnerCondition).text.toString()
        val description = findViewById<TextInputEditText>(R.id.etDescription).text.toString().trim()

        if (name.isEmpty() || priceText.isEmpty() || category.isEmpty() || condition.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Agrega al menos una foto", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceText.toDoubleOrNull() ?: 0.0
        
        // Usamos un placeholder de recurso (Int) ya que el modelo se revirtió a List<Int>
        val newProduct = Product(
            id = System.currentTimeMillis().toInt(),
            name = name,
            description = description,
            price = price,
            location = "Riohacha",
            imageUrls = listOf(R.drawable.ic_launcher_background),
            category = category,
            condition = condition
        )

        ProductRepository.addProduct(newProduct)

        Toast.makeText(this, "Producto publicado con éxito", Toast.LENGTH_LONG).show()
        finish()
    }
}