package com.silauncer.cepat.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.cache.IconLoader
import com.silauncer.cepat.util.dpToPx
import com.silauncer.cepat.util.spToPx
import kotlinx.coroutines.CoroutineScope

class AppAdapter(
    private val coroutineScope: CoroutineScope,
    private var iconSizePx: Int,
    private var showAppLabel: Boolean,
    private var labelSizeSp: Float,
    private var iconSpacingPx: Int,
    private var gridRows: Int,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    private val apps = mutableListOf<AppInfo>()
    private val iconLoader = IconLoader(coroutineScope)
    private var notificationCounts = emptyMap<String, Int>()

    fun updateNotifications(newCounts: Map<String, Int>) {
        val oldCounts = notificationCounts
        notificationCounts = newCounts
        
        apps.forEachIndexed { index, app ->
            val pkg = app.packageName
            val oldC = oldCounts[pkg] ?: 0
            val newC = newCounts[pkg] ?: 0
            if (oldC != newC) {
                notifyItemChanged(index, "badge")
            }
        }
    }
    private var recyclerView: RecyclerView? = null
    private var lastHeight = 0

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val rv = recyclerView ?: return@OnGlobalLayoutListener
        val newHeight = rv.measuredHeight
        if (newHeight > 0 && newHeight != lastHeight) {
            lastHeight = newHeight
            rv.post {
                if (recyclerView != null) {
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        this.recyclerView = null
    }

    override fun onViewRecycled(holder: AppViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    fun submitList(newList: List<AppInfo>) {
        val oldSnapshot = ArrayList(apps)
        val newSnapshot = ArrayList(newList)
        val diffResult = DiffUtil.calculateDiff(AppDiffCallback(oldSnapshot, newSnapshot))
        apps.clear()
        apps.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= apps.size || toPosition >= apps.size) return
        val item = apps.removeAt(fromPosition)
        apps.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getItems(): List<AppInfo> = apps

    private var currentIconPack: String = ""

    fun updateConfig(newIconSizePx: Int, newShowLabel: Boolean, newLabelSizeSp: Float, newIconSpacingPx: Int, newGridRows: Int, newIconPack: String) {
        var changed = false
        if (iconSizePx != newIconSizePx) {
            iconSizePx = newIconSizePx
            changed = true
        }
        if (showAppLabel != newShowLabel) {
            showAppLabel = newShowLabel
            changed = true
        }
        if (labelSizeSp != newLabelSizeSp) {
            labelSizeSp = newLabelSizeSp
            changed = true
        }
        if (iconSpacingPx != newIconSpacingPx) {
            iconSpacingPx = newIconSpacingPx
            changed = true
        }
        if (gridRows != newGridRows) {
            gridRows = newGridRows
            changed = true
        }
        if (currentIconPack != newIconPack) {
            currentIconPack = newIconPack
            changed = true
        }
        if (changed) {
            notifyDataSetChanged()
            recyclerView?.requestLayout()
            recyclerView?.invalidate()
        }
    }

    override fun getItemCount(): Int = apps.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("badge")) {
            val app = apps[position]
            holder.bindBadge(notificationCounts[app.packageName] ?: 0)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app)
        holder.bindBadge(notificationCounts[app.packageName] ?: 0)
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.app_icon)
        private val nameView: TextView = itemView.findViewById(R.id.app_name)
        private val badgeView: TextView = itemView.findViewById(R.id.notification_badge)

        fun bindBadge(count: Int) {
            if (count > 0) {
                badgeView.visibility = View.VISIBLE
                badgeView.text = if (count > 99) "99+" else count.toString()
            } else {
                badgeView.visibility = View.GONE
            }
        }

        // [app/src/main/java/com/silauncer/cepat/home/AppAdapter.kt]: Dynamic Cell Bounds Calculation
        // [Penjelasan]: Menghitung tinggi sel dinamis (Ukuran Ikon + Vertical Padding + Estimasi Tinggi Teks + Spacing) untuk mencegah ikon atau teks label terpotong
        fun bind(app: AppInfo) {
            val rv = recyclerView
            if (rv != null && lastHeight > 0 && gridRows > 0) {
                val availableHeight = lastHeight - rv.paddingTop - rv.paddingBottom
                val baseGridCellHeight = availableHeight / gridRows
                val labelEstimatedHeightPx = if (showAppLabel) {
                    (itemView.context.spToPx(labelSizeSp) * 2.5f).toInt()
                } else 0
                val minRequiredHeight = iconSizePx + (iconSpacingPx * 2) + labelEstimatedHeightPx + itemView.context.dpToPx(8)
                val dynamicCellHeight = maxOf(baseGridCellHeight, minRequiredHeight)
                if (itemView.layoutParams.height != dynamicCellHeight) {
                    itemView.layoutParams = itemView.layoutParams.apply {
                        height = dynamicCellHeight
                    }
                }
            }
            
            itemView.setPadding(iconSpacingPx, iconSpacingPx, iconSpacingPx, iconSpacingPx)
            
            if (iconView.layoutParams.width != iconSizePx || iconView.layoutParams.height != iconSizePx) {
                iconView.layoutParams = iconView.layoutParams.apply {
                    width = iconSizePx
                    height = iconSizePx
                }
            }
            
            val currentCacheKey = app.cacheKey
            iconView.tag = currentCacheKey
            iconLoader.loadIconAsync(itemView.context, app) { drawable, loadedKey ->
                if (iconView.tag == loadedKey) {
                    iconView.setImageDrawable(drawable)
                }
            }
            
            if (showAppLabel) {
                nameView.visibility = View.VISIBLE
                nameView.text = app.name
                nameView.textSize = labelSizeSp
            } else {
                nameView.visibility = View.GONE
            }
            
            itemView.setOnClickListener { onClick(app) }

            // [app/src/main/java/com/silauncer/cepat/home/AppAdapter.kt]: Animasi sentuhan
            // [Penjelasan]: Menambahkan efek scale (memantul/shrink) pada saat item disentuh
            itemView.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    }
                }
                false
            }
        }

        fun unbind() {
            iconView.tag = null
            iconView.setImageDrawable(null)
            itemView.setOnClickListener(null)
            itemView.setOnTouchListener(null)
            badgeView.visibility = View.GONE
        }
    }
}

class AppDiffCallback(
    private val oldList: List<AppInfo>,
    private val newList: List<AppInfo>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].componentName == newList[newItemPosition].componentName && 
               oldList[oldItemPosition].user == newList[newItemPosition].user
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition] &&
               oldList[oldItemPosition].cacheKey == newList[newItemPosition].cacheKey
    }
}
