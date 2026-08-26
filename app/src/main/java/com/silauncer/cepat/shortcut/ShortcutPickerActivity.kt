package com.silauncer.cepat.shortcut

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShortcutPickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shortcut_picker)

        val rvShortcuts = findViewById<RecyclerView>(R.id.rv_shortcuts)
        rvShortcuts.layoutManager = LinearLayoutManager(this)

        loadShortcuts { shortcuts ->
            val adapter = ShortcutPickerAdapter(shortcuts) { item ->
                ShortcutLauncher.launch(this, item)
                // We should probably finish the activity after launching the config
                // finish()
            }
            rvShortcuts.adapter = adapter
        }
    }

    private fun loadShortcuts(onResult: (List<ParsedShortcut>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = mutableListOf<ParsedShortcut>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                    val userManager = getSystemService(Context.USER_SERVICE) as? UserManager
                    
                    if (launcherApps != null && userManager != null) {
                        val userProfiles = userManager.userProfiles
                        
                        for (user in userProfiles) {
                            val configActivities = launcherApps.getShortcutConfigActivityList(null, user)
                            if (configActivities != null) {
                                val parsed = ShortcutParser.parseConfigList(this@ShortcutPickerActivity, configActivities)
                                result.addAll(parsed)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Safe catch
                }
            }
            
            withContext(Dispatchers.Main) {
                // Sort by label
                onResult(result.sortedBy { it.label })
            }
        }
    }
}
