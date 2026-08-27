package com.silauncer.cepat.settings

import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppDataSource
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.settings.treeview.SettingsNodeFactory
import com.silauncer.cepat.settings.treeview.TreeNode
import com.silauncer.cepat.settings.treeview.TreeViewAdapter
import com.silauncer.cepat.storage.LauncherPreferences
import kotlinx.coroutines.launch

// [app/src/main/java/com/silauncer/cepat/settings/SettingsActivity.kt]: Activity Pengaturan Utama TreeView
// [Penjelasan]: Menampilkan antarmuka TreeView vertikal murni tanpa popup atau dialog apapun
class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: LauncherPreferences
    private lateinit var adapter: TreeViewAdapter
    private var currentTreeNodes: List<TreeNode> = emptyList()
    private var cachedApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // [app/src/main/java/com/silauncer/cepat/settings/SettingsActivity.kt]: Konfigurasi transparansi window & wallpaper
        // [Penjelasan]: Menyembunyikan action bar dan mengaktifkan FLAG_SHOW_WALLPAPER untuk menampilkan wallpaper sistem secara langsung tanpa menembus kisi LauncherActivity
        supportActionBar?.hide()
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        try {
            setContentView(R.layout.activity_settings)
            prefs = LauncherPreferences()

            // [app/src/main/java/com/silauncer/cepat/settings/SettingsActivity.kt]: Inisialisasi tombol kembali
            // [Penjelasan]: Menangani tombol panah kembali untuk menutup SettingsActivity
            val btnBack: ImageView = findViewById(R.id.btn_back)
            btnBack.setOnClickListener {
                finish()
            }

            // [app/src/main/java/com/silauncer/cepat/settings/SettingsActivity.kt]: Inisialisasi RecyclerView TreeView
            // [Penjelasan]: Mengatur LinearLayoutManager dan TreeViewAdapter untuk hirarki pengaturan vertikal
            val rvSettingsTree: RecyclerView = findViewById(R.id.rv_settings_tree)
            rvSettingsTree.layoutManager = LinearLayoutManager(this)

            adapter = TreeViewAdapter()
            rvSettingsTree.adapter = adapter

            // [app/src/main/java/com/silauncer/cepat/settings/SettingsActivity.kt]: Memuat data TreeView awal dan aplikasi terinstal
            loadInitialSettings()

        } catch (e: Exception) {
            android.util.Log.e("SILAUNCER", "SETTINGS CRASH: " + e.message, e)
            android.widget.Toast.makeText(this, getString(R.string.error_settings_crash, e.message ?: ""), android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // [app/src/main/java/com/silauncer/cepat/settings/SettingsActivity.kt]: Memuat data awal pengaturan
    // [Penjelasan]: Menghapus dependensi redundant UseCase, langsung menggunakan AppDataSource untuk mendapatkan list aplikasi
    private fun loadInitialSettings() {
        lifecycleScope.launch {
            val appDataSource = AppDataSource(this@SettingsActivity)
            cachedApps = appDataSource.getInstalledApps(null, android.os.Process.myUserHandle())
            buildAndApplyTree()
        }
    }

    // [app/src/main/java/com/silauncer/cepat/settings/SettingsActivity.kt]: Membangun dan Menerapkan Hirarki Tree
    // [Penjelasan]: Membangun TreeNode baru secara asinkron di Dispatchers.IO untuk menghindari ANR
    private fun buildAndApplyTree() {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            val newTree = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                SettingsNodeFactory.createSettingsTree(
                    context = this@SettingsActivity,
                    prefs = prefs,
                    installedApps = cachedApps,
                    onTreeChanged = {
                        buildAndApplyTree()
                    }
                )
            }
            if (currentTreeNodes.isNotEmpty()) {
                SettingsNodeFactory.restoreExpansionState(newTree, currentTreeNodes)
            }
            currentTreeNodes = newTree
            adapter.setNodes(newTree)
        }
    }
}

