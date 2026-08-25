package com.silauncer.cepat.iconpack

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

data class IconPackInfo(val packageName: String, val label: String)

object IconPackRepository {
    private val componentToDrawableName = ConcurrentHashMap<String, String>()
    private var loadedIconPack: String? = null

    fun getAvailableIconPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val iconPacks = mutableMapOf<String, IconPackInfo>()

        // Standard third-party themes (Nova, Apex, ADW, etc.)
        val intents = listOf(
            Intent("com.novalauncher.THEME"),
            Intent("org.adw.launcher.THEMES"),
            Intent("com.dlto.atom.launcher.THEME"),
            Intent("android.intent.action.MAIN").addCategory("com.anddoes.launcher.THEME")
        )

        for (intent in intents) {
            try {
                val resolves = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                for (resolve in resolves) {
                    val pkgName = resolve.activityInfo.packageName
                    if (!iconPacks.containsKey(pkgName)) {
                        val label = resolve.loadLabel(pm).toString()
                        iconPacks[pkgName] = IconPackInfo(pkgName, label)
                    }
                }
            } catch (e: Exception) {
                // Ignore exception if query fails
            }
        }

        // OEM specific themes (like OPPO/ColorOS, MIUI, etc.) 
        // that expose icon packs via specific broadcast/action could be added here.
        // For now, Play Store standard icon packs use the above intents.
        
        return iconPacks.values.toList().sortedBy { it.label }
    }

    private fun loadAppFilter(context: Context, iconPackPackage: String) {
        if (loadedIconPack == iconPackPackage) return
        
        componentToDrawableName.clear()
        
        try {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(iconPackPackage)
            
            // Third-party icon packs map components to drawables in res/xml/appfilter.xml
            val appFilterId = res.getIdentifier("appfilter", "xml", iconPackPackage)
            if (appFilterId != 0) {
                val parser = res.getXml(appFilterId)
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (component != null && drawable != null) {
                            componentToDrawableName[component] = drawable
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            // Ignore error, might not have appfilter.xml
        }
        loadedIconPack = iconPackPackage
    }

    fun getIcon(context: Context, iconPackPackage: String, componentName: ComponentName): Drawable? {
        if (iconPackPackage.isEmpty()) return null
        
        loadAppFilter(context, iconPackPackage)
        
        // Match component
        val compString = "ComponentInfo{${componentName.packageName}/${componentName.className}}"
        val drawableName = componentToDrawableName[compString]
        
        try {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(iconPackPackage)
            
            if (drawableName != null) {
                val resId = res.getIdentifier(drawableName, "drawable", iconPackPackage)
                if (resId != 0) {
                    return res.getDrawable(resId, null)
                }
            }
            
            // Fallback for some OEM packs or generic matching: try using the package name or class name directly
            val fallbackResId = res.getIdentifier(componentName.packageName.replace(".", "_"), "drawable", iconPackPackage)
            if (fallbackResId != 0) {
                return res.getDrawable(fallbackResId, null)
            }
            
        } catch (e: Exception) {
            // Fallback if unable to read from pack
        }
        
        return null
    }
}
