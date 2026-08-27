package com.silauncer.cepat.apps

// [app/src/main/java/com/silauncer/cepat/apps/AppSorter.kt]: Pengurut Daftar Aplikasi
// [Penjelasan]: Mengurutkan daftar aplikasi berdasarkan mode (A-Z, Z-A, atau urutan kustom Room DB)
object AppSorter {
    fun sort(apps: List<AppInfo>, sortMode: String, customOrder: List<String> = emptyList()): List<AppInfo> {
        return when (sortMode) {
            "z_a" -> apps.sortedByDescending { it.name.lowercase() }
            "custom" -> {
                val orderMap = customOrder.withIndex().associate { it.value to it.index }
                apps.sortedWith(compareBy({ orderMap[it.componentName.flattenToString()] ?: Int.MAX_VALUE }, { it.name.lowercase() }))
            }
            else -> apps.sortedBy { it.name.lowercase() }
        }
    }
}
