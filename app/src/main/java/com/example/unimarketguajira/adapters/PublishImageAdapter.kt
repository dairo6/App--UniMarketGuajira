package com.example.unimarketguajira.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R

class PublishImageAdapter(
    private val images: MutableList<Uri>,
    private val onAddClick: () -> Unit,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_IMAGE = 0
        private const val TYPE_ADD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < images.size) TYPE_IMAGE else TYPE_ADD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_IMAGE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_publish_image, parent, false)
            ImageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_add_photo, parent, false)
            AddViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ImageViewHolder) {
            holder.ivProductImage.setImageURI(images[position])
            holder.btnRemoveImage.setOnClickListener { onRemoveClick(position) }
        } else if (holder is AddViewHolder) {
            holder.itemView.setOnClickListener { onAddClick() }
        }
    }

    override fun getItemCount(): Int {
        // Show images + add button if images < 5
        return if (images.size < 5) images.size + 1 else 5
    }

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProductImage: ImageView = view.findViewById(R.id.ivProductImage)
        val btnRemoveImage: ImageButton = view.findViewById(R.id.btnRemoveImage)
    }

    class AddViewHolder(view: View) : RecyclerView.ViewHolder(view)
}