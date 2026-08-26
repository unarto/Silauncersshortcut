package com.silauncer.cepat.settings.treeview

import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R

// [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeNodeViewHolder.kt]: Base ViewHolder untuk TreeNode
// [Penjelasan]: Abstract ViewHolder dasar untuk semua tipe node dalam TreeView Settings
abstract class BaseTreeNodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(node: TreeNode, onNodeClick: (TreeNode) -> Unit)
}

// [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeNodeViewHolder.kt]: ViewHolder untuk Parent Node (Depth 0)
// [Penjelasan]: ViewHolder yang menampilkan grup parent tingkat atas dengan ikon kategori, judul, deskripsi, dan panah animasi
class ParentViewHolder(itemView: View) : BaseTreeNodeViewHolder(itemView) {
    private val iconContainer: View = itemView.findViewById(R.id.icon_container)
    private val imgIcon: ImageView = itemView.findViewById(R.id.img_parent_icon)
    private val tvTitle: TextView = itemView.findViewById(R.id.tv_parent_title)
    private val tvDescription: TextView = itemView.findViewById(R.id.tv_parent_description)
    private val imgChevron: ImageView = itemView.findViewById(R.id.img_chevron)

    override fun bind(node: TreeNode, onNodeClick: (TreeNode) -> Unit) {
        tvTitle.text = node.title
        if (node.description.isNullOrEmpty()) {
            tvDescription.visibility = View.GONE
        } else {
            tvDescription.visibility = View.VISIBLE
            tvDescription.text = node.description
        }

        if (node.iconRes != null) {
            iconContainer.visibility = View.VISIBLE
            imgIcon.setImageResource(node.iconRes)
        } else {
            iconContainer.visibility = View.GONE
        }

        val targetRotation = if (node.isExpanded) 180f else 0f
        imgChevron.rotation = targetRotation

        itemView.setOnClickListener {
            val nextRotation = if (node.isExpanded) 0f else 180f
            ObjectAnimator.ofFloat(imgChevron, View.ROTATION, imgChevron.rotation, nextRotation).apply {
                duration = 200
                start()
            }
            onNodeClick(node)
        }
    }
}

// [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeNodeViewHolder.kt]: ViewHolder untuk SubParent Node (Depth 1)
// [Penjelasan]: ViewHolder yang menampilkan subgrup expandable dengan indikator badge nilai dan panah animasi
class SubParentViewHolder(itemView: View) : BaseTreeNodeViewHolder(itemView) {
    private val tvTitle: TextView = itemView.findViewById(R.id.tv_subparent_title)
    private val tvDescription: TextView = itemView.findViewById(R.id.tv_subparent_description)
    private val tvValue: TextView = itemView.findViewById(R.id.tv_subparent_value)
    private val imgChevron: ImageView = itemView.findViewById(R.id.img_subparent_chevron)

    override fun bind(node: TreeNode, onNodeClick: (TreeNode) -> Unit) {
        tvTitle.text = node.title
        if (node.description.isNullOrEmpty()) {
            tvDescription.visibility = View.GONE
        } else {
            tvDescription.visibility = View.VISIBLE
            tvDescription.text = node.description
        }

        if (node.value != null && node.value.toString().isNotEmpty()) {
            tvValue.visibility = View.VISIBLE
            tvValue.text = node.value.toString()
        } else {
            tvValue.visibility = View.GONE
        }

        val targetRotation = if (node.isExpanded) 180f else 0f
        imgChevron.rotation = targetRotation

        itemView.setOnClickListener {
            val nextRotation = if (node.isExpanded) 0f else 180f
            ObjectAnimator.ofFloat(imgChevron, View.ROTATION, imgChevron.rotation, nextRotation).apply {
                duration = 200
                start()
            }
            onNodeClick(node)
        }
    }
}

// [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeNodeViewHolder.kt]: ViewHolder untuk Choice Node (Depth 2 / Leaf)
// [Penjelasan]: ViewHolder yang menampilkan opsi pilihan tunggal dengan ikon centang aktif dan latar aksen
class ChoiceViewHolder(itemView: View) : BaseTreeNodeViewHolder(itemView) {
    private val imgIcon: ImageView = itemView.findViewById(R.id.img_choice_icon)
    private val tvTitle: TextView = itemView.findViewById(R.id.tv_choice_title)
    private val tvDescription: TextView = itemView.findViewById(R.id.tv_choice_description)
    private val imgCheck: ImageView = itemView.findViewById(R.id.img_choice_check)

    override fun bind(node: TreeNode, onNodeClick: (TreeNode) -> Unit) {
        tvTitle.text = node.title
        if (node.description.isNullOrEmpty()) {
            tvDescription.visibility = View.GONE
        } else {
            tvDescription.visibility = View.VISIBLE
            tvDescription.text = node.description
        }

        if (node.iconDrawable != null) {
            imgIcon.visibility = View.VISIBLE
            imgIcon.setImageDrawable(node.iconDrawable)
        } else if (node.iconRes != null) {
            imgIcon.visibility = View.VISIBLE
            imgIcon.setImageResource(node.iconRes)
        } else {
            imgIcon.visibility = View.GONE
        }

        if (node.isSelected) {
            itemView.setBackgroundResource(R.drawable.bg_settings_card_choice_selected)
            tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_accent))
            imgCheck.visibility = View.VISIBLE
        } else {
            itemView.setBackgroundResource(R.drawable.bg_settings_card_choice)
            tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            imgCheck.visibility = View.INVISIBLE
        }

        itemView.setOnClickListener {
            node.onSelect?.invoke()
            onNodeClick(node)
        }
    }
}

// [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeNodeViewHolder.kt]: ViewHolder untuk Child Switch Node
// [Penjelasan]: ViewHolder yang menampilkan sakelar On/Off (Switch) dengan dukungan ikon aplikasi
class SwitchViewHolder(itemView: View) : BaseTreeNodeViewHolder(itemView) {
    private val imgIcon: ImageView = itemView.findViewById(R.id.img_switch_icon)
    private val tvTitle: TextView = itemView.findViewById(R.id.tv_child_switch_title)
    private val tvDescription: TextView = itemView.findViewById(R.id.tv_child_switch_description)
    private val switchControl: SwitchCompat = itemView.findViewById(R.id.switch_control)

    override fun bind(node: TreeNode, onNodeClick: (TreeNode) -> Unit) {
        tvTitle.text = node.title
        if (node.description.isNullOrEmpty()) {
            tvDescription.visibility = View.GONE
        } else {
            tvDescription.visibility = View.VISIBLE
            tvDescription.text = node.description
        }

        if (node.iconDrawable != null) {
            imgIcon.visibility = View.VISIBLE
            imgIcon.setImageDrawable(node.iconDrawable)
        } else {
            imgIcon.visibility = View.GONE
        }

        val isChecked = (node.value as? Boolean) ?: false
        switchControl.isChecked = isChecked

        itemView.setOnClickListener {
            val newChecked = !switchControl.isChecked
            switchControl.isChecked = newChecked
            node.value = newChecked
            node.onSwitchChange?.invoke(newChecked)
            onNodeClick(node)
        }
    }
}

// [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeNodeViewHolder.kt]: ViewHolder untuk Child Action Node
// [Penjelasan]: ViewHolder yang menangani aksi klik sederhana seperti reset tata letak
class ActionViewHolder(itemView: View) : BaseTreeNodeViewHolder(itemView) {
    private val tvTitle: TextView = itemView.findViewById(R.id.tv_child_action_title)
    private val tvDescription: TextView = itemView.findViewById(R.id.tv_child_action_description)

    override fun bind(node: TreeNode, onNodeClick: (TreeNode) -> Unit) {
        tvTitle.text = node.title
        if (node.description.isNullOrEmpty()) {
            tvDescription.visibility = View.GONE
        } else {
            tvDescription.visibility = View.VISIBLE
            tvDescription.text = node.description
        }

        itemView.setOnClickListener {
            node.onAction?.invoke()
            onNodeClick(node)
        }
    }
}

class SliderViewHolder(itemView: View) : BaseTreeNodeViewHolder(itemView) {
    private val tvTitle: TextView = itemView.findViewById(R.id.tv_slider_title)
    private val tvValue: TextView = itemView.findViewById(R.id.tv_slider_value)
    private val seekBar: android.widget.SeekBar = itemView.findViewById(R.id.seek_bar_control)

    override fun bind(node: TreeNode, onNodeClick: (TreeNode) -> Unit) {
        tvTitle.text = node.title
        
        val min = node.sliderMin
        val max = node.sliderMax
        val step = node.sliderStep
        val range = max - min
        val steps = (range / step).toInt()
        
        seekBar.max = steps
        
        val currentValue = (node.value as? Float) ?: min
        val currentStep = ((currentValue - min) / step).toInt()
        seekBar.progress = currentStep
        tvValue.text = String.format("%.0f", currentValue)

        seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newValue = min + (progress * step)
                    tvValue.text = String.format("%.0f", newValue)
                    node.value = newValue
                    node.onSliderChange?.invoke(newValue)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                onNodeClick(node)
            }
        })
    }
}

