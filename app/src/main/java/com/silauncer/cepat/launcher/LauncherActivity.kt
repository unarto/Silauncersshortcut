package com.silauncer.cepat.launcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppActionHandler
import com.silauncer.cepat.apps.AppChangeReceiver
import com.silauncer.cepat.apps.AppDataSource
import com.silauncer.cepat.apps.AppStateHolder
import com.silauncer.cepat.home.AppAdapter
import com.silauncer.cepat.home.MaterialGridItemAnimator
import com.silauncer.cepat.home.OverScroll
import com.silauncer.cepat.storage.LauncherPreferences
import com.silauncer.cepat.notification.NotificationStateManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppAdapter
    private lateinit var prefs: LauncherPreferences
    private lateinit var appChangeReceiver: AppChangeReceiver
    
    private lateinit var appController: LauncherAppController
    private lateinit var actionHandler: AppActionHandler
    private lateinit var dragHandler: GridDragAndDropHandler
    
    private var isLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        
        prefs = LauncherPreferences()
        val appDataSource = AppDataSource(applicationContext)
        val appStateHolder = AppStateHolder()
        
        val db = com.silauncer.cepat.storage.db.LauncherDatabase.getDatabase(this)
        val repository = com.silauncer.cepat.storage.db.LauncherRepository(db.appItemDao())
        
        appController = LauncherAppController(appDataSource, appStateHolder, prefs, repository)
        actionHandler = AppActionHandler(this)

        recyclerView = findViewById(R.id.app_grid)
        recyclerView.layoutManager = GridLayoutManager(this, prefs.gridColumns)
        // [app/src/main/java/com/silauncer/cepat/launcher/LauncherActivity.kt]: Material Design Motion ItemAnimator
        // [Penjelasan]: Menerapkan transisi gerak Material Design saat aplikasi ditambah, dihapus, atau diorganisasi ulang
        recyclerView.itemAnimator = MaterialGridItemAnimator(this)
        OverScroll.setup(recyclerView)

        val iconSizePx = dpToPx(prefs.iconSize)
        val spacingPx = dpToPx(prefs.iconSpacing)
        
        adapter = AppAdapter(
            lifecycleScope,
            iconSizePx,
            prefs.showAppLabel,
            prefs.labelSize,
            spacingPx,
            prefs.gridRows,
            onClick = { app ->
                if (app.packageName == applicationContext.packageName) {
                    try {
                        startActivity(android.content.Intent(this, com.silauncer.cepat.settings.SettingsActivity::class.java))
                    } catch (e: Exception) {
                        android.util.Log.e("SILAUNCER", "CRASH: " + e.message, e)
                    }
                } else {
                    actionHandler.launchApp(app)
                }
            }
        )
        recyclerView.adapter = adapter
        
        dragHandler = GridDragAndDropHandler(
            context = this,
            recyclerView = recyclerView,
            adapter = adapter,
            appController = appController,
            actionHandler = actionHandler,
            coroutineScope = lifecycleScope
        )
        
        appChangeReceiver = AppChangeReceiver { action, packageName, replacing ->
            lifecycleScope.launch {
                val changed = appController.handlePackageEvent(action, packageName, replacing)
                if (changed) {
                    refreshAppsUI()
                }
            }
        }
        appChangeReceiver.register(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing on back button as this is a launcher
            }
        })

        loadAppsInitialUI()
        
        lifecycleScope.launch {
            NotificationStateManager.notifications.collectLatest { notifMap ->
                val counts = notifMap.mapValues { it.value.size }
                adapter.updateNotifications(counts)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (recyclerView.layoutManager is GridLayoutManager) {
            val currentColumns = (recyclerView.layoutManager as GridLayoutManager).spanCount
            if (currentColumns != prefs.gridColumns) {
                recyclerView.layoutManager = GridLayoutManager(this, prefs.gridColumns)
            }
        }
        val currentIconSizePx = dpToPx(prefs.iconSize)
        val currentSpacingPx = dpToPx(prefs.iconSpacing)
        adapter.updateConfig(currentIconSizePx, prefs.showAppLabel, prefs.labelSize, currentSpacingPx, prefs.gridRows, prefs.selectedIconPack)
        
        if (isLoaded) {
            refreshAppsUI()
        }
    }

    // [app/src/main/java/com/silauncer/cepat/launcher/LauncherActivity.kt]: Utilitas Konversi Satuan DP ke PX
    // [Penjelasan]: Helper terpusat untuk menghitung konversi dimensi dp ke pixel berdasarkan kepadatan layar
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        appChangeReceiver.unregister(this)
    }

    private fun loadAppsInitialUI() {
        lifecycleScope.launch {
            val sortedApps = appController.loadAppsInitial()
            adapter.submitList(sortedApps)
            // [app/src/main/java/com/silauncer/cepat/launcher/LauncherActivity.kt]: Jalankan animasi layout
            // [Penjelasan]: Memberikan efek transisi (fall down) pada awal render item
            val resId = R.anim.layout_animation_fall_down
            val animation = android.view.animation.AnimationUtils.loadLayoutAnimation(this@LauncherActivity, resId)
            recyclerView.layoutAnimation = animation
            recyclerView.scheduleLayoutAnimation()
            
            isLoaded = true
        }
    }
    
    private fun refreshAppsUI() {
        lifecycleScope.launch {
            val sortedApps = appController.refreshApps()
            adapter.submitList(sortedApps)
        }
    }
}
