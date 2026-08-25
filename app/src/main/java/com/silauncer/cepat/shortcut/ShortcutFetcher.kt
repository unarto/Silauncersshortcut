package com.silauncer.cepat.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.res.Resources
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import org.xmlpull.v1.XmlPullParser

// [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Mengambil daftar dynamic/pinned/manifest shortcut
// [Penjelasan]: Menggunakan LauncherApps API dengan fallback parsing manifest XML via PackageManager agar shortcut selalu tampil
object ShortcutFetcher {

    fun getShortcuts(context: Context, packageName: String, userHandle: UserHandle? = null): List<ShortcutInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return emptyList()
        }

        val launcherShortcuts = mutableListOf<ShortcutInfo>()

        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            if (launcherApps != null) {
                val targetUser = userHandle ?: Process.myUserHandle()
                val query = LauncherApps.ShortcutQuery().apply {
                    setPackage(packageName)
                    setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                    )
                }
                val result = try {
                    launcherApps.getShortcuts(query, targetUser)
                } catch (e: Exception) {
                    null
                }
                if (!result.isNullOrEmpty()) {
                    launcherShortcuts.addAll(result)
                }
            }
        } catch (e: Exception) {
            // Ignore exception from LauncherApps
        }

        if (launcherShortcuts.isNotEmpty()) {
            return launcherShortcuts.sortedBy { it.rank }
        }

        // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Fallback Parsing Manifest Shortcuts
        // [Penjelasan]: Mengekstrak xml/shortcuts jika launcher belum diset sebagai default home app di sistem
        return parseManifestShortcuts(context, packageName)
    }

    private fun parseManifestShortcuts(context: Context, packageName: String): List<ShortcutInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()

        val shortcuts = mutableListOf<ShortcutInfo>()
        val pm = context.packageManager

        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(
                        (PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA).toLong()
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA)
            }

            val targetResources = pm.getResourcesForApplication(packageName)
            val activities = packageInfo.activities ?: emptyArray()

            for (activityInfo in activities) {
                val metaData = activityInfo.metaData ?: continue
                val resId = metaData.getInt("android.app.shortcuts")
                if (resId != 0) {
                    val parsed = parseShortcutsXml(context, packageName, targetResources, resId)
                    shortcuts.addAll(parsed)
                }
            }

            val appMetaData = packageInfo.applicationInfo?.metaData
            if (appMetaData != null) {
                val resId = appMetaData.getInt("android.app.shortcuts")
                if (resId != 0) {
                    val parsed = parseShortcutsXml(context, packageName, targetResources, resId)
                    shortcuts.addAll(parsed)
                }
            }
        } catch (e: Exception) {
            // Safe fallback
        }

        return shortcuts.distinctBy { it.id }
    }

    private fun parseShortcutsXml(
        context: Context,
        packageName: String,
        resources: Resources,
        resId: Int
    ): List<ShortcutInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        val list = mutableListOf<ShortcutInfo>()

        try {
            val parser = resources.getXml(resId)
            var eventType = parser.eventType

            var currentShortcutId: String? = null
            var currentShortLabel: String? = null
            var currentLongLabel: String? = null
            var currentIconResId = 0
            var currentIntentAction: String? = null
            var currentTargetPackage: String? = null
            var currentTargetClass: String? = null
            var currentData: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                if (eventType == XmlPullParser.START_TAG) {
                    if (tagName == "shortcut") {
                        currentShortcutId = null
                        currentShortLabel = null
                        currentLongLabel = null
                        currentIconResId = 0
                        currentIntentAction = null
                        currentTargetPackage = null
                        currentTargetClass = null
                        currentData = null

                        for (i in 0 until parser.attributeCount) {
                            val attrName = parser.getAttributeName(i)
                            val attrResId = parser.getAttributeResourceValue(i, 0)
                            val attrVal = parser.getAttributeValue(i)

                            when (attrName) {
                                "shortcutId" -> currentShortcutId = attrVal
                                "shortcutShortLabel" -> {
                                    currentShortLabel = if (attrResId != 0) {
                                        try { resources.getString(attrResId) } catch (e: Exception) { attrVal }
                                    } else {
                                        attrVal
                                    }
                                }
                                "shortcutLongLabel" -> {
                                    currentLongLabel = if (attrResId != 0) {
                                        try { resources.getString(attrResId) } catch (e: Exception) { attrVal }
                                    } else {
                                        attrVal
                                    }
                                }
                                "icon" -> currentIconResId = attrResId
                            }
                        }
                    } else if (tagName == "intent" && currentShortcutId != null) {
                        for (i in 0 until parser.attributeCount) {
                            val attrName = parser.getAttributeName(i)
                            val attrVal = parser.getAttributeValue(i)
                            when (attrName) {
                                "action" -> currentIntentAction = attrVal
                                "targetPackage" -> currentTargetPackage = attrVal
                                "targetClass" -> currentTargetClass = attrVal
                                "data" -> currentData = attrVal
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (tagName == "shortcut" && currentShortcutId != null) {
                        val shortcutId = currentShortcutId
                        val label = currentShortLabel ?: currentLongLabel ?: shortcutId

                        val intent = Intent().apply {
                            action = currentIntentAction ?: Intent.ACTION_VIEW
                            if (currentTargetPackage != null && currentTargetClass != null) {
                                setClassName(currentTargetPackage, currentTargetClass)
                            } else {
                                setPackage(packageName)
                            }
                            if (currentData != null) {
                                data = Uri.parse(currentData)
                            }
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                        val builder = ShortcutInfo.Builder(context, shortcutId)
                            .setShortLabel(label)
                            .setIntent(intent)

                        if (currentLongLabel != null) {
                            builder.setLongLabel(currentLongLabel)
                        }

                        if (currentIconResId != 0) {
                            try {
                                builder.setIcon(Icon.createWithResource(packageName, currentIconResId))
                            } catch (e: Exception) {
                                // Safe catch
                            }
                        }

                        list.add(builder.build())
                        currentShortcutId = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Safe xml parse catch
        }

        return list
    }
}

