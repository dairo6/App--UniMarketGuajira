package com.example.unimarketguajira.repository

import android.content.Context
import android.net.Uri
import com.example.unimarketguajira.R
import com.example.unimarketguajira.data.db.UniMarketDatabase
import com.example.unimarketguajira.data.entities.ProductEntity
import com.example.unimarketguajira.models.Product
import com.example.unimarketguajira.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

object ProductRepository {
    private val initialMockProducts = listOf(
        Product(0, "Calculadora TI-Nspire CX II CAS", "Calculadora gráfica avanzada para ingeniería y matemáticas.", 850000.0, "Riohacha", listOf(R.drawable.ic_launcher_background.toString()), "Tecnología", "Nuevo"),
        Product(0, "Libro: Cálculo de Stewart 8va Ed.", "Libro esencial para ingeniería. En muy buen estado.", 120000.0, "Maicao", listOf(R.drawable.ic_launcher_background.toString()), "Libros", "Usado - Como nuevo", true),
        Product(0, "Bata de Laboratorio XL", "Bata blanca reglamentaria para laboratorio de química.", 45000.0, "Riohacha", listOf(R.drawable.ic_launcher_background.toString()), "Laboratorio", "Usado - Excelente"),
        Product(0, "iPad Air 5ta Gen + Pencil", "Ideal para tomar apuntes digitales. 64GB de almacenamiento.", 2800000.0, "Uribia", listOf(R.drawable.ic_launcher_background.toString()), "Tecnología", "Usado - Como nuevo"),
        Product(0, "Kit de Drawing Técnico", "Tablero y juego de escuadras profesionales.", 95000.0, "Riohacha", listOf(R.drawable.ic_launcher_background.toString()), "Útiles", "Usado - Aceptable", true),
        Product(0, "Apuntes Estructuras de Datos", "Apuntes completos del semestre 2023-2 con ejercicios resueltos.", 15000.0, "Manaure", listOf(R.drawable.ic_launcher_background.toString()), "Apuntes", "Usado - Buen estado")
    )

    suspend fun uploadProductImages(context: Context, productId: Int, imageUrls: List<String>, onProgress: ((Int) -> Unit)? = null): List<String> {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val finalUrls = mutableListOf<String>()
        val maxImages = 5
        val totalImages = imageUrls.take(maxImages).size

        if (totalImages == 0) return emptyList()

        for (i in 0 until totalImages) {
            val path = imageUrls[i]
            val slotName = "image_${i + 1}.jpg"
            val imageRef = storageRef.child("products/$productId/$slotName")

            if (path.startsWith("https://firebasestorage.googleapis.com")) {
                finalUrls.add(path)
                onProgress?.invoke(((i + 1) * 100) / totalImages)
            } else {
                val file = File(path)
                if (file.exists()) {
                    try {
                        val uploadTask = imageRef.putFile(Uri.fromFile(file))
                        uploadTask.addOnProgressListener { taskSnapshot ->
                            if (taskSnapshot.totalByteCount > 0) {
                                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                                val overallProgress = ((i * 100) + progress) / totalImages
                                onProgress?.invoke(overallProgress.coerceIn(0, 99))
                            }
                        }
                        uploadTask.await()
                        val downloadUrl = imageRef.downloadUrl.await().toString()
                        finalUrls.add(downloadUrl)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        finalUrls.add(path)
                    }
                } else {
                    finalUrls.add(path)
                }
            }
        }

        // Limpiar slots sobrantes
        for (i in totalImages until maxImages) {
            val slotName = "image_${i + 1}.jpg"
            val imageRef = storageRef.child("products/$productId/$slotName")
            try {
                imageRef.delete().await()
            } catch (e: Exception) {
                // No existía o falló borrar, ignorar
            }
        }

        return finalUrls
    }

    suspend fun deleteProductImagesFromStorage(productId: Int) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val maxImages = 5
        for (i in 0 until maxImages) {
            val slotName = "image_${i + 1}.jpg"
            val imageRef = storageRef.child("products/$productId/$slotName")
            try {
                imageRef.delete().await()
            } catch (e: Exception) {
                // Ignorar
            }
        }
    }

    suspend fun addProduct(context: Context, product: Product, ownerEmail: String, onProgress: ((Int) -> Unit)? = null) {
        val dao = UniMarketDatabase.getDatabase(context).productDao()
        var finalProduct = product

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val uploadedUrls = uploadProductImages(context, product.id, product.imageUrls, onProgress)
                finalProduct = product.copy(imageUrls = uploadedUrls)

                // Guardar en Firestore
                val db = FirebaseFirestore.getInstance()
                val firestoreProduct = mapOf(
                    "id" to finalProduct.id,
                    "name" to finalProduct.name,
                    "description" to finalProduct.description,
                    "price" to finalProduct.price,
                    "location" to finalProduct.location,
                    "imageUrls" to finalProduct.imageUrls,
                    "category" to finalProduct.category,
                    "condition" to finalProduct.condition,
                    "isFavorite" to finalProduct.isFavorite,
                    "ownerEmail" to ownerEmail,
                    "status" to finalProduct.status
                )
                db.collection("products").document(finalProduct.id.toString()).set(firestoreProduct).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Guardar/Sincronizar en Room local
        dao.insertProduct(ProductEntity.fromModel(finalProduct, ownerEmail))
    }

    suspend fun getAllProducts(context: Context): List<Product> {
        val dao = UniMarketDatabase.getDatabase(context).productDao()

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("products").get().await()
                val productsList = mutableListOf<Product>()

                for (doc in snapshot.documents) {
                    val id = doc.getLong("id")?.toInt() ?: 0
                    val name = doc.getString("name") ?: ""
                    val description = doc.getString("description") ?: ""
                    val price = doc.getDouble("price") ?: 0.0
                    val location = doc.getString("location") ?: ""
                    val imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList()
                    val category = doc.getString("category") ?: ""
                    val condition = doc.getString("condition") ?: ""
                    val isFavorite = doc.getBoolean("isFavorite") ?: false
                    val ownerEmail = doc.getString("ownerEmail") ?: "admin@unimarket.com"
                    val status = doc.getString("status") ?: "ACTIVE"

                    val product = Product(id, name, description, price, location, imageUrls, category, condition, isFavorite, ownerEmail, status)
                    productsList.add(product)

                    // Sincronizar en Room local
                    dao.insertProduct(ProductEntity.fromModel(product, ownerEmail))
                }

                if (productsList.isNotEmpty()) {
                    return productsList.filter { it.status == "ACTIVE" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Carga local si no hay internet o falló Firestore
        var products = dao.getAllProducts()
        if (products.isEmpty()) {
            val entities = initialMockProducts.map { ProductEntity.fromModel(it, "admin@unimarket.com") }
            dao.insertAll(entities)
            products = dao.getAllProducts()
        }
        return products.map { it.toModel() }.filter { it.status == "ACTIVE" }
    }
    
    suspend fun getProductById(context: Context, id: Int): Product? {
        val dao = UniMarketDatabase.getDatabase(context).productDao()

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("products").document(id.toString()).get().await()
                if (doc.exists()) {
                    val name = doc.getString("name") ?: ""
                    val description = doc.getString("description") ?: ""
                    val price = doc.getDouble("price") ?: 0.0
                    val location = doc.getString("location") ?: ""
                    val imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList()
                    val category = doc.getString("category") ?: ""
                    val condition = doc.getString("condition") ?: ""
                    val isFavorite = doc.getBoolean("isFavorite") ?: false
                    val ownerEmail = doc.getString("ownerEmail") ?: "admin@unimarket.com"
                    val status = doc.getString("status") ?: "ACTIVE"

                    val product = Product(id, name, description, price, location, imageUrls, category, condition, isFavorite, ownerEmail, status)
                    // Sincronizar localmente
                    dao.insertProduct(ProductEntity.fromModel(product, ownerEmail))
                    return product
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return dao.getProductById(id)?.toModel()
    }

    suspend fun searchProductsLocal(context: Context, query: String): List<Product> {
        val dao = UniMarketDatabase.getDatabase(context).productDao()
        val sqlQuery = "%$query%"
        return dao.searchProducts(sqlQuery).map { it.toModel() }
    }

    suspend fun toggleFavorite(context: Context, productId: Int, isFavorite: Boolean) {
        val dao = UniMarketDatabase.getDatabase(context).productDao()
        val productEntity = dao.getProductById(productId)
        if (productEntity != null) {
            val updated = productEntity.copy(isFavorite = isFavorite)
            dao.insertProduct(updated)

            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("products").document(productId.toString())
                        .update("isFavorite", isFavorite).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun getMyProducts(context: Context, ownerEmail: String): List<Product> {
        val dao = UniMarketDatabase.getDatabase(context).productDao()
        
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("products").whereEqualTo("ownerEmail", ownerEmail).get().await()
                for (doc in snapshot.documents) {
                    val id = doc.getLong("id")?.toInt() ?: 0
                    val name = doc.getString("name") ?: ""
                    val description = doc.getString("description") ?: ""
                    val price = doc.getDouble("price") ?: 0.0
                    val location = doc.getString("location") ?: ""
                    val imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList()
                    val category = doc.getString("category") ?: ""
                    val condition = doc.getString("condition") ?: ""
                    val isFavorite = doc.getBoolean("isFavorite") ?: false
                    val status = doc.getString("status") ?: "ACTIVE"

                    val product = Product(id, name, description, price, location, imageUrls, category, condition, isFavorite, ownerEmail, status)
                    dao.insertProduct(ProductEntity.fromModel(product, ownerEmail))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return dao.getProductsByOwner(ownerEmail).map { it.toModel() }
    }

    suspend fun updateProductStatus(context: Context, productId: Int, newStatus: String) {
        val dao = UniMarketDatabase.getDatabase(context).productDao()
        val productEntity = dao.getProductById(productId)
        if (productEntity != null) {
            val updated = productEntity.copy(status = newStatus)
            dao.insertProduct(updated)

            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("products").document(productId.toString())
                        .update("status", newStatus).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun updateProduct(context: Context, product: Product, ownerEmail: String, onProgress: ((Int) -> Unit)? = null) {
        val dao = UniMarketDatabase.getDatabase(context).productDao()
        var finalProduct = product

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val uploadedUrls = uploadProductImages(context, product.id, product.imageUrls, onProgress)
                finalProduct = product.copy(imageUrls = uploadedUrls)

                val db = FirebaseFirestore.getInstance()
                val firestoreProduct = mapOf(
                    "id" to finalProduct.id,
                    "name" to finalProduct.name,
                    "description" to finalProduct.description,
                    "price" to finalProduct.price,
                    "location" to finalProduct.location,
                    "imageUrls" to finalProduct.imageUrls,
                    "category" to finalProduct.category,
                    "condition" to finalProduct.condition,
                    "isFavorite" to finalProduct.isFavorite,
                    "ownerEmail" to ownerEmail,
                    "status" to finalProduct.status
                )
                db.collection("products").document(finalProduct.id.toString()).set(firestoreProduct).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        dao.insertProduct(ProductEntity.fromModel(finalProduct, ownerEmail))
    }

    suspend fun addProductToHistory(context: Context, productId: Int, userId: String) {
        val dao = UniMarketDatabase.getDatabase(context).productViewHistoryDao()
        val viewEntity = com.example.unimarketguajira.data.entities.ProductViewHistoryEntity(
            productId = productId,
            userId = userId,
            viewedAt = System.currentTimeMillis()
        )
        dao.insertView(viewEntity)

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val map = hashMapOf(
                    "productId" to productId,
                    "userId" to userId,
                    "viewedAt" to viewEntity.viewedAt
                )
                db.collection("users").document(userId)
                    .collection("history").document(productId.toString())
                    .set(map).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteHistoryItem(context: Context, productId: Int, userId: String) {
        val dao = UniMarketDatabase.getDatabase(context).productViewHistoryDao()
        dao.deleteHistoryItem(productId, userId)

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(userId)
                    .collection("history").document(productId.toString())
                    .delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun clearHistory(context: Context, userId: String) {
        val dao = UniMarketDatabase.getDatabase(context).productViewHistoryDao()
        dao.clearHistory(userId)

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("users").document(userId)
                    .collection("history").get().await()
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getProductHistory(context: Context, userId: String): List<Product> {
        val dao = UniMarketDatabase.getDatabase(context).productViewHistoryDao()
        var historyEntities = dao.getViewHistory(userId)

        if (historyEntities.isEmpty() && NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("users").document(userId)
                    .collection("history").orderBy("viewedAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(10).get().await()
                val fetchedEntities = mutableListOf<com.example.unimarketguajira.data.entities.ProductViewHistoryEntity>()
                for (doc in snapshot.documents) {
                    val pId = doc.getLong("productId")?.toInt() ?: continue
                    val vAt = doc.getLong("viewedAt") ?: System.currentTimeMillis()
                    val entity = com.example.unimarketguajira.data.entities.ProductViewHistoryEntity(
                        productId = pId,
                        userId = userId,
                        viewedAt = vAt
                    )
                    dao.insertView(entity)
                    fetchedEntities.add(entity)
                }
                historyEntities = fetchedEntities
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val products = mutableListOf<Product>()
        for (entity in historyEntities) {
            val product = getProductById(context, entity.productId)
            if (product != null) {
                products.add(product)
            }
        }
        return products
    }
}