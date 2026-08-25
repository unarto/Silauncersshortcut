package com.silauncer.cepat.popup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.shortcut.ParsedShortcut

// [app/src/main/java/com/silauncer/cepat/popup/ShortcutItemAdapter.kt]: Adapter Item Shortcut Dinamis
// [Penjelasan]: Merender daftar pintasan dinamis aplikasi pada kartu popup
class ShortcutItemAdapter(
    private val shortcuts: List<ParsedShortcut>,
    private val onShortcutClick: (ParsedShortcut) -> Unit
) : RecyclerView.Adapter<ShortcutItemAdapter.ShortcutViewHolder>() {

    class ShortcutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon: ImageView = itemView.findViewById(R.id.img_shortcut_icon)
        val tvLabel: TextView = itemView.findViewById(R.id.tv_shortcut_label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_popup_shortcut, parent, false)
        return ShortcutViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, position: Int) {
        val item = shortcuts[position]
        holder.tvLabel.text = item.label

        if (item.icon != null) {
            holder.imgIcon.setImageDrawable(item.icon)
        } else {
            holder.imgIcon.setImageDrawable(
                ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_shortcut_default)
            )
        }

        holder.itemView.setOnClickListener {
            onShortcutClick(item)
        }
    }

    override fun getItemCount(): Int = shortcuts.size
}
