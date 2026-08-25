package com.silauncer.cepat.settings.treeview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R

// [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeViewAdapter.kt]: Adapter RecyclerView khusus TreeView Multi-Level
// [Penjelasan]: Pengelola rendering hirarki node bertingkat dengan mekanisme expand/collapse rekursif yang mulus
class TreeViewAdapter(
    private var rootNodes: List<TreeNode> = emptyList()
) : RecyclerView.Adapter<BaseTreeNodeViewHolder>() {

    private val visibleNodes = mutableListOf<TreeNode>()

    init {
        rebuildVisibleNodes()
    }

    // [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeViewAdapter.kt]: Memperbarui data root nodes
    // [Penjelasan]: Mengatur ulang daftar root dan membangun kembali seluruh hirarki node yang terlihat
    fun setNodes(nodes: List<TreeNode>) {
        rootNodes = nodes
        rebuildVisibleNodes()
        notifyDataSetChanged()
    }

    // [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeViewAdapter.kt]: Refresh Tampilan TreeView
    // [Penjelasan]: Membangun ulang list node yang tampak tanpa mereset status expand/collapse
    fun refresh() {
        rebuildVisibleNodes()
        notifyDataSetChanged()
    }

    // [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeViewAdapter.kt]: Rekonstruksi daftar node tampak
    // [Penjelasan]: Memflatkan hirarki tree berdasarkan status isExpanded secara rekursif
    private fun rebuildVisibleNodes() {
        visibleNodes.clear()
        for (root in rootNodes) {
            addNodeAndVisibleChildren(root)
        }
    }

    // [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeViewAdapter.kt]: Penambahan Node Rekursif
    // [Penjelasan]: Memasukkan node dan seluruh anak-anaknya secara berurutan sesuai status isExpanded
    private fun addNodeAndVisibleChildren(node: TreeNode) {
        visibleNodes.add(node)
        if (node.isExpanded) {
            for (child in node.children) {
                addNodeAndVisibleChildren(child)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return visibleNodes[position].nodeType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseTreeNodeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        // [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeViewAdapter.kt]: Akses Enum Modern
        // [Penjelasan]: Menggunakan NodeType.entries untuk menghindari alokasi Array enum baru pada setiap inflasi ViewHolder
        return when (NodeType.entries[viewType]) {
            NodeType.PARENT -> {
                val view = inflater.inflate(R.layout.item_tree_parent, parent, false)
                ParentViewHolder(view)
            }
            NodeType.SUB_PARENT -> {
                val view = inflater.inflate(R.layout.item_tree_subparent, parent, false)
                SubParentViewHolder(view)
            }
            NodeType.CHOICE -> {
                val view = inflater.inflate(R.layout.item_tree_child_choice, parent, false)
                ChoiceViewHolder(view)
            }
            NodeType.SWITCH -> {
                val view = inflater.inflate(R.layout.item_tree_child_switch, parent, false)
                SwitchViewHolder(view)
            }
            NodeType.ACTION -> {
                val view = inflater.inflate(R.layout.item_tree_child_action, parent, false)
                ActionViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: BaseTreeNodeViewHolder, position: Int) {
        val node = visibleNodes[position]
        holder.bind(node) { clickedNode ->
            if (clickedNode.nodeType == NodeType.PARENT || clickedNode.nodeType == NodeType.SUB_PARENT) {
                toggleExpandCollapse(clickedNode)
            }
        }
    }

    // [app/src/main/java/com/silauncer/cepat/settings/treeview/TreeViewAdapter.kt]: Aksi Expand/Collapse Node
    // [Penjelasan]: Mengubah status isExpanded parent/subparent node dan membangun kembali daftar tampak dengan animasi mulus
    private fun toggleExpandCollapse(node: TreeNode) {
        val startPosition = visibleNodes.indexOf(node)
        if (startPosition == -1) return
        
        node.isExpanded = !node.isExpanded
        val oldSize = visibleNodes.size
        rebuildVisibleNodes()
        val newSize = visibleNodes.size
        
        if (node.isExpanded) {
            notifyItemChanged(startPosition)
            notifyItemRangeInserted(startPosition + 1, newSize - oldSize)
        } else {
            notifyItemRangeRemoved(startPosition + 1, oldSize - newSize)
            notifyItemChanged(startPosition)
        }
    }

    override fun getItemCount(): Int = visibleNodes.size
}

