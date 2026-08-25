package com.silauncer.cepat.settings.treeview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// [app/src/test/java/com/silauncer/cepat/settings/treeview/SettingsNodeFactoryTest.kt]: Unit Test untuk struktur TreeNode TreeView Vertikal
// [Penjelasan]: Memastikan model data TreeNode, hierarki bertingkat multi-level, dan pemulihan status ekspansi berjalan presisi
class SettingsNodeFactoryTest {

    @Test
    fun testTreeNodeHierarchyMultiLevel() {
        val choice1 = TreeNode(
            id = "choice_1",
            title = "4x5",
            nodeType = NodeType.CHOICE,
            depth = 2,
            isSelected = true
        )

        val choice2 = TreeNode(
            id = "choice_2",
            title = "5x6",
            nodeType = NodeType.CHOICE,
            depth = 2,
            isSelected = false
        )

        val subParent = TreeNode(
            id = "subparent_grid",
            title = "Tata Letak Kisi",
            nodeType = NodeType.SUB_PARENT,
            depth = 1,
            value = "4x5",
            children = mutableListOf(choice1, choice2)
        )

        val switchNode = TreeNode(
            id = "switch_labels",
            title = "Tampilkan Label",
            nodeType = NodeType.SWITCH,
            depth = 1,
            value = true
        )

        val parent = TreeNode(
            id = "parent_grid",
            title = "Grid & Tata Letak",
            nodeType = NodeType.PARENT,
            depth = 0,
            isExpanded = true,
            children = mutableListOf(subParent, switchNode)
        )

        assertEquals("parent_grid", parent.id)
        assertEquals(NodeType.PARENT, parent.nodeType)
        assertTrue(parent.isExpanded)
        assertEquals(2, parent.children.size)

        assertEquals("subparent_grid", parent.children[0].id)
        assertEquals(NodeType.SUB_PARENT, parent.children[0].nodeType)
        assertEquals("4x5", parent.children[0].value)
        assertEquals(2, parent.children[0].children.size)

        val firstChoice = parent.children[0].children[0]
        assertEquals(NodeType.CHOICE, firstChoice.nodeType)
        assertTrue(firstChoice.isSelected)

        assertEquals("switch_labels", parent.children[1].id)
        assertEquals(NodeType.SWITCH, parent.children[1].nodeType)
        assertEquals(true, parent.children[1].value)
    }

    @Test
    fun testRestoreExpansionState() {
        val oldChoice = TreeNode(id = "c1", title = "C1", nodeType = NodeType.CHOICE)
        val oldSubParent = TreeNode(id = "sub1", title = "Sub1", nodeType = NodeType.SUB_PARENT, isExpanded = true, children = mutableListOf(oldChoice))
        val oldParent = TreeNode(id = "root1", title = "Root1", nodeType = NodeType.PARENT, isExpanded = true, children = mutableListOf(oldSubParent))
        val oldTree = listOf(oldParent)

        val newChoice = TreeNode(id = "c1", title = "C1", nodeType = NodeType.CHOICE)
        val newSubParent = TreeNode(id = "sub1", title = "Sub1", nodeType = NodeType.SUB_PARENT, isExpanded = false, children = mutableListOf(newChoice))
        val newParent = TreeNode(id = "root1", title = "Root1", nodeType = NodeType.PARENT, isExpanded = false, children = mutableListOf(newSubParent))
        val newTree = listOf(newParent)

        assertFalse(newParent.isExpanded)
        assertFalse(newSubParent.isExpanded)

        SettingsNodeFactory.restoreExpansionState(newTree, oldTree)

        assertTrue(newParent.isExpanded)
        assertTrue(newSubParent.isExpanded)
    }
}

