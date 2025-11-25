package com.ke.sentricall

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ke.sentricall.data.local.ProtectedAppEntity

class ProtectedAppsAdapter(
    private val packageManager: PackageManager,
    private val items: MutableList<ProtectedAppEntity>,
    private val onRemoveClick: (ProtectedAppEntity) -> Unit
) : RecyclerView.Adapter<ProtectedAppsAdapter.ProtectedAppViewHolder>() {

    inner class ProtectedAppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        val tvLabel: TextView = itemView.findViewById(R.id.tvAppLabel)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemoveApp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProtectedAppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_protected_app, parent, false)
        return ProtectedAppViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ProtectedAppViewHolder, position: Int) {
        val item = items[position]

        // Label
        holder.tvLabel.text = item.label.ifBlank { item.packageName }

        // Icon
        holder.ivIcon.setImageDrawable(loadAppIcon(item.packageName))

        // Remove button
        holder.btnRemove.setOnClickListener {
            onRemoveClick(item)
        }
    }

    fun updateData(newItems: List<ProtectedAppEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun loadAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            // fallback to app icon or a default drawable
            null
        }
    }
}