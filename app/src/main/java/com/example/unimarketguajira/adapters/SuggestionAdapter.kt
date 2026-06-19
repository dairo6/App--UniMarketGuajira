package com.example.unimarketguajira.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarketguajira.R

class SuggestionAdapter(
    private val suggestions: List<String>,
    private val onSuggestionClicked: (String) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder>() {

    class SuggestionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSuggestionText: TextView = view.findViewById(R.id.tvSuggestionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.tvSuggestionText.text = suggestion
        holder.itemView.setOnClickListener {
            onSuggestionClicked(suggestion)
        }
    }

    override fun getItemCount(): Int = suggestions.size
}
