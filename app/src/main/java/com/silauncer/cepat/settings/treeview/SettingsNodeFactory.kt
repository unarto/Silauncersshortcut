package com.silauncer.cepat.settings.treeview

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.cache.IconCache
import com.silauncer.cepat.iconpack.IconPackRepository
import com.silauncer.cepat.storage.LauncherPreferences

// [app/src/main/java/com/silauncer/cepat/settings/treeview/SettingsNodeFactory.kt]: Pabrik pembuat struktur TreeView Multi-Level
// [Penjelasan]: Membangun seluruh hirarki vertikal settings (zero dialog/popup) dari LauncherPreferences dan Context
object SettingsNodeFactory {

    // [app/src/main/java/com/silauncer/cepat/settings/treeview/SettingsNodeFactory.kt]: Penerapan Bahasa Aplikasi
    // [Penjelasan]: Mengubah locale aplikasi secara langsung menggunakan AppCompatDelegate setApplicationLocales
    fun applyAppLanguage(languageCode: String) {
        val localeList = if (languageCode == "system" || languageCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    // [app/src/main/java/com/silauncer/cepat/settings/treeview/SettingsNodeFactory.kt]: Pembuat Pohon Pengaturan Lengkap
    // [Penjelasan]: Menghasilkan daftar TreeNode multi-tingkat untuk dirender oleh TreeViewAdapter
    fun createSettingsTree(
        context: Context,
        prefs: LauncherPreferences,
        installedApps: List<AppInfo>,
        onTreeChanged: () -> Unit
    ): List<TreeNode> {
        val tree = mutableListOf<TreeNode>()

        // ==========================================
        // 1. GROUP GRID & TATA LETAK
        // ==========================================
        val currentGridStr = context.getString(R.string.format_grid, prefs.gridColumns, prefs.gridRows)
        
        val gridNode = TreeNode(
            id = "group_grid",
            iconRes = R.drawable.ic_grid,
            title = context.getString(R.string.pref_category_grid),
            description = context.getString(R.string.pref_category_grid_desc),
            nodeType = NodeType.PARENT,
            isExpanded = true,
            children = mutableListOf(
                TreeNode(
                    id = "slider_grid_columns",
                    title = "Kolom (Columns)",
                    nodeType = NodeType.SLIDER,
                    depth = 1,
                    value = prefs.gridColumns.toFloat(),
                    sliderMin = 3f,
                    sliderMax = 8f,
                    sliderStep = 1f,
                    onSliderChange = { value ->
                        prefs.gridColumns = value.toInt()
                    }
                ),
                TreeNode(
                    id = "slider_grid_rows",
                    title = "Baris (Rows)",
                    nodeType = NodeType.SLIDER,
                    depth = 1,
                    value = prefs.gridRows.toFloat(),
                    sliderMin = 3f,
                    sliderMax = 10f,
                    sliderStep = 1f,
                    onSliderChange = { value ->
                        prefs.gridRows = value.toInt()
                    }
                ),
                TreeNode(
                    id = "slider_icon_spacing",
                    title = context.getString(R.string.pref_icon_spacing),
                    nodeType = NodeType.SLIDER,
                    depth = 1,
                    value = prefs.iconSpacing.toFloat(),
                    sliderMin = 0f,
                    sliderMax = 48f,
                    sliderStep = 2f,
                    onSliderChange = { value ->
                        prefs.iconSpacing = value.toInt()
                    }
                )
            )
        )
        tree.add(gridNode)

        // ==========================================
        // 2. GROUP IKON & LABEL APLIKASI
        // ==========================================
        val availablePacks = IconPackRepository.getAvailableIconPacks(context)
        val defaultPackLabel = context.getString(R.string.pref_icon_pack_default)
        val currentPackPkg = prefs.selectedIconPack
        val currentPackLabel = availablePacks.find { it.packageName == currentPackPkg }?.label ?: defaultPackLabel

        val iconPackChoices = mutableListOf<TreeNode>()
        // Opsi Bawaan Sistem
        iconPackChoices.add(
            TreeNode(
                id = "choice_iconpack_default",
                title = defaultPackLabel,
                description = context.getString(R.string.pref_icon_pack_desc),
                nodeType = NodeType.CHOICE,
                depth = 2,
                isSelected = currentPackPkg.isEmpty(),
                onSelect = {
                    if (prefs.selectedIconPack.isNotEmpty()) {
                        prefs.selectedIconPack = ""
                        IconCache.clear()
                        onTreeChanged()
                    }
                }
            )
        )
        // Opsi Paket Ikon Pihak Ketiga / OEM
        for (pack in availablePacks) {
            iconPackChoices.add(
                TreeNode(
                    id = "choice_iconpack_${pack.packageName}",
                    title = pack.label,
                    description = pack.packageName,
                    nodeType = NodeType.CHOICE,
                    depth = 2,
                    isSelected = (currentPackPkg == pack.packageName),
                    onSelect = {
                        if (prefs.selectedIconPack != pack.packageName) {
                            prefs.selectedIconPack = pack.packageName
                            IconCache.clear()
                            onTreeChanged()
                        }
                    }
                )
            )
        }

        val iconsNode = TreeNode(
            id = "group_icons",
            iconRes = R.drawable.ic_icons,
            title = context.getString(R.string.pref_category_icons),
            description = context.getString(R.string.pref_category_icons_desc),
            nodeType = NodeType.PARENT,
            isExpanded = false,
            children = mutableListOf(
                TreeNode(
                    id = "subparent_icon_pack",
                    title = context.getString(R.string.pref_icon_pack),
                    description = context.getString(R.string.pref_icon_pack_desc),
                    nodeType = NodeType.SUB_PARENT,
                    depth = 1,
                    value = currentPackLabel,
                    children = iconPackChoices
                ),
                TreeNode(
                    id = "slider_icon_size",
                    title = context.getString(R.string.pref_icon_size),
                    nodeType = NodeType.SLIDER,
                    depth = 1,
                    value = prefs.iconSize.toFloat(),
                    sliderMin = 32f,
                    sliderMax = 96f,
                    sliderStep = 4f,
                    onSliderChange = { value ->
                        prefs.iconSize = value.toInt()
                    }
                ),
                TreeNode(
                    id = "item_show_labels",
                    title = context.getString(R.string.pref_show_labels),
                    description = context.getString(R.string.pref_show_labels_desc),
                    nodeType = NodeType.SWITCH,
                    depth = 1,
                    value = prefs.showAppLabel,
                    onSwitchChange = { isChecked ->
                        prefs.showAppLabel = isChecked
                    }
                ),
                TreeNode(
                    id = "slider_label_size",
                    title = context.getString(R.string.pref_label_size),
                    nodeType = NodeType.SLIDER,
                    depth = 1,
                    value = prefs.labelSize,
                    sliderMin = 8f,
                    sliderMax = 24f,
                    sliderStep = 1f,
                    onSliderChange = { value ->
                        prefs.labelSize = value
                    }
                )
            )
        )
        tree.add(iconsNode)

        // ==========================================
        // 3. GROUP BAHASA APLIKASI
        // ==========================================
        val currentLangCode = prefs.appLanguage
        val currentLangDisplay = when (currentLangCode) {
            "in", "id" -> context.getString(R.string.pref_lang_indonesian)
            "en" -> context.getString(R.string.pref_lang_english)
            else -> context.getString(R.string.pref_lang_system_default)
        }

        val languageChoices = listOf(
            Triple("choice_lang_system", context.getString(R.string.pref_lang_system_default), "system"),
            Triple("choice_lang_in", context.getString(R.string.pref_lang_indonesian), "in"),
            Triple("choice_lang_en", context.getString(R.string.pref_lang_english), "en")
        ).map { (id, label, code) ->
            val isSelected = when (code) {
                "system" -> (currentLangCode == "system" || currentLangCode.isEmpty())
                "in" -> (currentLangCode == "in" || currentLangCode == "id")
                "en" -> (currentLangCode == "en")
                else -> false
            }
            TreeNode(
                id = id,
                title = label,
                nodeType = NodeType.CHOICE,
                depth = 2,
                isSelected = isSelected,
                onSelect = {
                    if (prefs.appLanguage != code) {
                        prefs.appLanguage = code
                        applyAppLanguage(code)
                        onTreeChanged()
                    }
                }
            )
        }

        val languageNode = TreeNode(
            id = "group_language",
            iconRes = R.drawable.ic_language,
            title = context.getString(R.string.pref_category_language),
            description = context.getString(R.string.pref_category_language_desc),
            nodeType = NodeType.PARENT,
            isExpanded = false,
            children = mutableListOf(
                TreeNode(
                    id = "subparent_app_language",
                    title = context.getString(R.string.pref_app_language),
                    description = context.getString(R.string.pref_app_language_desc),
                    nodeType = NodeType.SUB_PARENT,
                    depth = 1,
                    value = currentLangDisplay,
                    children = languageChoices.toMutableList()
                )
            )
        )
        tree.add(languageNode)

        // ==========================================
        // 4. GROUP LACI APLIKASI & URUTAN
        // ==========================================
        val sortModeCode = prefs.sortMode
        val currentSortDisplay = when (sortModeCode) {
            "z_a" -> context.getString(R.string.pref_sort_za)
            "custom" -> context.getString(R.string.pref_sort_custom)
            else -> context.getString(R.string.pref_sort_az)
        }

        val sortChoices = listOf(
            Triple("choice_sort_az", context.getString(R.string.pref_sort_az), "a_z"),
            Triple("choice_sort_za", context.getString(R.string.pref_sort_za), "z_a"),
            Triple("choice_sort_custom", context.getString(R.string.pref_sort_custom), "custom")
        ).map { (id, label, mode) ->
            TreeNode(
                id = id,
                title = label,
                nodeType = NodeType.CHOICE,
                depth = 2,
                isSelected = (sortModeCode == mode),
                onSelect = {
                    if (prefs.sortMode != mode) {
                        prefs.sortMode = mode
                        onTreeChanged()
                    }
                }
            )
        }

        val hiddenSet = prefs.hiddenApps
        val hiddenCount = hiddenSet.size
        val hiddenDisplay = if (hiddenCount > 0) {
            context.getString(R.string.pref_hidden_apps_count, hiddenCount)
        } else {
            context.getString(R.string.pref_hidden_apps_empty)
        }

        val sortedApps = installedApps.sortedBy { it.name.lowercase() }
        val pm = context.packageManager
        val hiddenAppNodes = sortedApps.map { app ->
            val pkg = app.componentName.packageName
            val isHidden = hiddenSet.contains(pkg)
            val appIcon = IconCache.get(app.cacheKey) ?: try {
                pm.getActivityIcon(app.componentName)
            } catch (e: Exception) {
                null
            }
            TreeNode(
                id = "hidden_app_$pkg",
                title = app.name,
                description = pkg,
                iconDrawable = appIcon,
                nodeType = NodeType.SWITCH,
                depth = 2,
                value = isHidden,
                onSwitchChange = { isChecked ->
                    val newSet = prefs.hiddenApps.toMutableSet()
                    if (isChecked) {
                        newSet.add(pkg)
                    } else {
                        newSet.remove(pkg)
                    }
                    prefs.hiddenApps = newSet
                    onTreeChanged()
                }
            )
        }

        val drawerNode = TreeNode(
            id = "group_drawer",
            iconRes = R.drawable.ic_drawer,
            title = context.getString(R.string.pref_category_drawer),
            description = context.getString(R.string.pref_category_drawer_desc),
            nodeType = NodeType.PARENT,
            isExpanded = false,
            children = mutableListOf(
                TreeNode(
                    id = "subparent_sort_mode",
                    title = context.getString(R.string.pref_sort_mode),
                    description = context.getString(R.string.pref_sort_mode_desc),
                    nodeType = NodeType.SUB_PARENT,
                    depth = 1,
                    value = currentSortDisplay,
                    children = sortChoices.toMutableList()
                ),
                TreeNode(
                    id = "subparent_hidden_apps",
                    title = context.getString(R.string.pref_hidden_apps),
                    description = context.getString(R.string.pref_hidden_apps_desc),
                    nodeType = NodeType.SUB_PARENT,
                    depth = 1,
                    value = hiddenDisplay,
                    children = hiddenAppNodes.toMutableList()
                ),
                TreeNode(
                    id = "action_reset_layout",
                    title = context.getString(R.string.pref_reset_layout),
                    description = context.getString(R.string.pref_reset_layout_desc),
                    nodeType = NodeType.ACTION,
                    depth = 1,
                    onAction = {
                        prefs.resetToDefaults()
                        applyAppLanguage("system")
                        IconCache.clear()
                        Toast.makeText(
                            context,
                            context.getString(R.string.pref_reset_layout_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        onTreeChanged()
                    }
                )
            )
        )
        tree.add(drawerNode)

        return tree
    }

    // [app/src/main/java/com/silauncer/cepat/settings/treeview/SettingsNodeFactory.kt]: Sinkronisasi Status Ekspansi
    // [Penjelasan]: Mempertahankan status isExpanded dari setiap node agar tidak tertutup saat state preferensi di-update
    fun restoreExpansionState(newTree: List<TreeNode>, previousTree: List<TreeNode>) {
        val expandedIds = mutableSetOf<String>()
        fun collectExpanded(nodes: List<TreeNode>) {
            for (node in nodes) {
                if (node.isExpanded) {
                    expandedIds.add(node.id)
                }
                collectExpanded(node.children)
            }
        }
        collectExpanded(previousTree)

        fun applyExpanded(nodes: List<TreeNode>) {
            for (node in nodes) {
                if (expandedIds.contains(node.id)) {
                    node.isExpanded = true
                }
                applyExpanded(node.children)
            }
        }
        applyExpanded(newTree)
    }
}
