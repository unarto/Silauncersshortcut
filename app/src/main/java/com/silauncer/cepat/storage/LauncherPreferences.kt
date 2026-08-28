package com.silauncer.cepat.storage

import com.tencent.mmkv.MMKV

// [app/src/main/java/com/silauncer/cepat/storage/LauncherPreferences.kt]: Penyimpanan Preferensi MMKV
// [Penjelasan]: Mengelola persistensi konfigurasi launcher dengan kunci konstanta terdefinisi dan fungsi reset bawaan
class LauncherPreferences {

    companion object {
        private const val MMKV_ID = "silauncer_launcher"
        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_GRID_ROWS = "grid_rows"
        private const val KEY_ICON_SIZE = "icon_size"
        private const val KEY_SORT_MODE = "sort_mode"
        private const val KEY_SHOW_APP_LABEL = "show_app_label"
        private const val KEY_LABEL_SIZE = "label_size"
        private const val KEY_ICON_SPACING = "icon_spacing"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
        private const val KEY_APP_ORDER = "app_order"
        private const val KEY_SELECTED_ICON_PACK = "selected_icon_pack"
        private const val KEY_APP_LANGUAGE = "app_language"

        // [app/src/main/java/com/silauncer/cepat/storage/LauncherPreferences.kt]: Nilai Konfigurasi Bawaan Launcher
        // [Penjelasan]: Mengatur nilai default kolom=5, baris=5, ukuran ikon=40, dan ukuran teks label=10f
        const val DEFAULT_GRID_COLUMNS = 5
        const val DEFAULT_GRID_ROWS = 5
        const val DEFAULT_ICON_SIZE = 40
        const val DEFAULT_SORT_MODE = "a_z"
        const val DEFAULT_SHOW_APP_LABEL = true
        const val DEFAULT_LABEL_SIZE = 10f
        const val DEFAULT_ICON_SPACING = 4
        const val DEFAULT_APP_LANGUAGE = "system"
    }

    private val kv: MMKV = checkNotNull(MMKV.mmkvWithID(MMKV_ID)) {
        "MMKV initialization failed for $MMKV_ID"
    }

    var gridColumns: Int
        get() = kv.decodeInt(KEY_GRID_COLUMNS, DEFAULT_GRID_COLUMNS)
        set(value) { kv.encode(KEY_GRID_COLUMNS, value) }

    var gridRows: Int
        get() = kv.decodeInt(KEY_GRID_ROWS, DEFAULT_GRID_ROWS)
        set(value) { kv.encode(KEY_GRID_ROWS, value) }

    var iconSize: Int
        get() = kv.decodeInt(KEY_ICON_SIZE, DEFAULT_ICON_SIZE)
        set(value) { kv.encode(KEY_ICON_SIZE, value) }

    var sortMode: String
        get() = kv.decodeString(KEY_SORT_MODE, DEFAULT_SORT_MODE) ?: DEFAULT_SORT_MODE
        set(value) { kv.encode(KEY_SORT_MODE, value) }

    var showAppLabel: Boolean
        get() = kv.decodeBool(KEY_SHOW_APP_LABEL, DEFAULT_SHOW_APP_LABEL)
        set(value) { kv.encode(KEY_SHOW_APP_LABEL, value) }

    var labelSize: Float
        get() = kv.decodeFloat(KEY_LABEL_SIZE, DEFAULT_LABEL_SIZE)
        set(value) { kv.encode(KEY_LABEL_SIZE, value) }

    var iconSpacing: Int
        get() = kv.decodeInt(KEY_ICON_SPACING, DEFAULT_ICON_SPACING)
        set(value) { kv.encode(KEY_ICON_SPACING, value) }

    var hiddenApps: Set<String>
        get() = kv.decodeStringSet(KEY_HIDDEN_APPS, emptySet()) ?: emptySet()
        set(value) { kv.encode(KEY_HIDDEN_APPS, value) }

    var appOrder: List<String>
        get() = kv.decodeString(KEY_APP_ORDER, "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        set(value) { kv.encode(KEY_APP_ORDER, value.joinToString(",")) }

    var selectedIconPack: String
        get() = kv.decodeString(KEY_SELECTED_ICON_PACK, "") ?: ""
        set(value) { kv.encode(KEY_SELECTED_ICON_PACK, value) }

    // [app/src/main/java/com/silauncer/cepat/storage/LauncherPreferences.kt]: Preferensi Bahasa Tampilan
    // [Penjelasan]: Menyimpan kode bahasa tampilan yang dipilih pengguna ("system", "in", atau "en")
    var appLanguage: String
        get() = kv.decodeString(KEY_APP_LANGUAGE, DEFAULT_APP_LANGUAGE) ?: DEFAULT_APP_LANGUAGE
        set(value) { kv.encode(KEY_APP_LANGUAGE, value) }

    fun resetToDefaults() {
        gridColumns = DEFAULT_GRID_COLUMNS
        gridRows = DEFAULT_GRID_ROWS
        iconSize = DEFAULT_ICON_SIZE
        sortMode = DEFAULT_SORT_MODE
        showAppLabel = DEFAULT_SHOW_APP_LABEL
        labelSize = DEFAULT_LABEL_SIZE
        iconSpacing = DEFAULT_ICON_SPACING
        appOrder = emptyList()
        selectedIconPack = ""
        appLanguage = DEFAULT_APP_LANGUAGE
    }
}

