package com.silauncer.cepat.launcher

import android.content.Intent
import android.os.Process
import com.silauncer.cepat.apps.AppDataSource
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.apps.AppStateHolder
import com.silauncer.cepat.apps.AppSorter
import com.silauncer.cepat.cache.IconCache
import com.silauncer.cepat.storage.LauncherPreferences
import com.silauncer.cepat.storage.db.AppItemEntity
import com.silauncer.cepat.storage.db.LauncherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LauncherAppController(
    private val appDataSource: AppDataSource,
    private val appStateHolder: AppStateHolder,
    private val prefs: LauncherPreferences,
    private val repository: LauncherRepository
) {
    suspend fun loadAppsInitial(): List<AppInfo> {
        migrateIfNeeded()
        val user = Process.myUserHandle()
        val installedApps = appDataSource.getInstalledApps(null, user)
        appStateHolder.setApps(installedApps)
        return getSortedVisibleApps()
    }

    private suspend fun migrateIfNeeded() {
        val dbItems = repository.getAllItems()
        if (dbItems.isEmpty() && prefs.appOrder.isNotEmpty()) {
            val gridCols = prefs.gridColumns
            val entities = prefs.appOrder.mapIndexed { index, compStr ->
                val pkg = compStr.substringBefore('/')
                AppItemEntity(
                    packageName = pkg,
                    componentName = compStr,
                    cellX = index % gridCols,
                    cellY = index / gridCols,
                    pageIndex = 0
                )
            }
            repository.saveCustomOrder(entities)
        }
    }

    suspend fun refreshApps(): List<AppInfo> {
        return getSortedVisibleApps()
    }

    suspend fun saveCustomAppOrder(visibleApps: List<AppInfo>) {
        val allApps = appStateHolder.getApps()
        val currentOrderKeys = repository.getAllItems().map { it.componentName }
        val newOrder = calculateMergedOrder(allApps, visibleApps, currentOrderKeys)
        
        val gridCols = prefs.gridColumns
        val entities = newOrder.mapIndexed { index, compStr ->
            val pkg = compStr.substringBefore('/')
            AppItemEntity(
                packageName = pkg,
                componentName = compStr,
                cellX = index % gridCols,
                cellY = index / gridCols,
                pageIndex = 0
            )
        }
        repository.saveCustomOrder(entities)
        
        if (prefs.sortMode != "custom") {
            prefs.sortMode = "custom"
        }
    }

    private suspend fun getSortedVisibleApps(): List<AppInfo> {
        val apps = appStateHolder.getApps()
        return withContext(Dispatchers.Default) {
            val hidden = prefs.hiddenApps
            val visibleApps = apps.filter { !hidden.contains(it.componentName.packageName) }
            val dbItems = repository.getAllItems()
            val dbOrder = dbItems.map { it.componentName }
            AppSorter.sort(visibleApps, prefs.sortMode, dbOrder)
        }
    }

    suspend fun handlePackageEvent(action: String?, packageName: String?, replacing: Boolean): Boolean {
        if (action == null || packageName == null) return false
        val user = Process.myUserHandle()

        var changed = false
        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                val newApps = appDataSource.getInstalledApps(packageName, user)
                val added = appStateHolder.addApps(newApps)
                if (added.isNotEmpty()) changed = true
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                if (!replacing) {
                    val removed = appStateHolder.removePackage(packageName, user)
                    IconCache.removePackage(packageName)
                    
                    val currentHidden = prefs.hiddenApps
                    if (currentHidden.contains(packageName)) {
                        prefs.hiddenApps = currentHidden - packageName
                    }
                    
                    repository.deleteItem(packageName)
                    
                    changed = removed
                }
            }
            Intent.ACTION_PACKAGE_CHANGED, Intent.ACTION_PACKAGE_REPLACED -> {
                val removed = appStateHolder.removePackage(packageName, user)
                val newApps = appDataSource.getInstalledApps(packageName, user)
                val updated = appStateHolder.addApps(newApps)
                IconCache.removePackage(packageName)
                changed = removed || updated.isNotEmpty()
            }
        }
        return changed
    }

    companion object {
        fun calculateMergedOrder(
            allApps: List<AppInfo>,
            visibleReordered: List<AppInfo>,
            currentSavedOrder: List<String>
        ): List<String> {
            val allInstalledKeys = allApps.map { it.componentName.flattenToString() }.toSet()
            val visibleKeys = visibleReordered.map { it.componentName.flattenToString() }
            val visibleKeySet = visibleKeys.toSet()

            val baseOrder = if (currentSavedOrder.isNotEmpty()) {
                val pruned = currentSavedOrder.filter { allInstalledKeys.contains(it) }
                val missing = allApps.map { it.componentName.flattenToString() }.filter { !pruned.contains(it) }
                pruned + missing
            } else {
                allApps.sortedBy { it.name.lowercase() }.map { it.componentName.flattenToString() }
            }

            var visibleIndex = 0
            val result = mutableListOf<String>()
            for (key in baseOrder) {
                if (visibleKeySet.contains(key)) {
                    if (visibleIndex < visibleKeys.size) {
                        result.add(visibleKeys[visibleIndex])
                        visibleIndex++
                    }
                } else {
                    result.add(key)
                }
            }
            while (visibleIndex < visibleKeys.size) {
                result.add(visibleKeys[visibleIndex])
                visibleIndex++
            }
            return result
        }
    }
}
