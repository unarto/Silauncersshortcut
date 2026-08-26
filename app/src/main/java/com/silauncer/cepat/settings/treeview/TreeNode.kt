package com.silauncer.cepat.settings.treeview

import android.graphics.drawable.Drawable

// [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeNode.kt]: Tipe node dalam TreeView Settings
// [Penjelasan]: Enum yang mendefinisikan jenis node tampilan (PARENT, SUB_PARENT, CHOICE, SWITCH, ACTION)
enum class NodeType {
    PARENT,
    SUB_PARENT,
    CHOICE,
    SWITCH,
    ACTION,
    SLIDER
}

// [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeNode.kt]: Data class model TreeNode
// [Penjelasan]: Model data untuk setiap node hirarki dalam TreeView Settings bertingkat
data class TreeNode(
    val id: String,
    val iconRes: Int? = null,
    val iconDrawable: Drawable? = null,
    var title: String,
    var description: String? = null,
    val nodeType: NodeType,
    var isExpanded: Boolean = false,
    val depth: Int = 0,
    val children: MutableList<TreeNode> = mutableListOf(),
    var isSelected: Boolean = false,
    var value: Any? = null,
    val sliderMin: Float = 0f,
    val sliderMax: Float = 100f,
    val sliderStep: Float = 1f,
    val onSelect: (() -> Unit)? = null,
    val onSwitchChange: ((Boolean) -> Unit)? = null,
    val onAction: (() -> Unit)? = null,
    val onSliderChange: ((Float) -> Unit)? = null
)

