package com.example.unimarketguajira.models

import android.net.Uri
import android.widget.ImageView
import com.example.unimarketguajira.R
import java.io.File

data class Product(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val location: String,
    val imageUrls: List<String>, // Cambiado a List<String> para soportar tanto recursos locales como URIs reales
    val category: String,
    val condition: String,
    var isFavorite: Boolean = false,
    val ownerEmail: String = "",
    val status: String = "ACTIVE",
    val stock: Int = 1
)

fun ImageView.loadProductImage(imagePath: String?) {
    if (imagePath.isNullOrEmpty()) {
        com.bumptech.glide.Glide.with(context)
            .load(R.drawable.ic_launcher_background)
            .into(this)
        return
    }
    val resId = imagePath.toIntOrNull()
    if (resId != null) {
        com.bumptech.glide.Glide.with(context)
            .load(resId)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(this)
    } else {
        val file = java.io.File(imagePath)
        val loadTarget: Any = if (file.exists()) file else imagePath
        com.bumptech.glide.Glide.with(context)
            .load(loadTarget)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .into(this)
    }
}