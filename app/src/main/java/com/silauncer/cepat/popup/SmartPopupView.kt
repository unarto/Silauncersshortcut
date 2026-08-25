package com.silauncer.cepat.popup

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.shortcut.ParsedShortcut

import com.silauncer.cepat.notification.NotificationItem
import android.widget.TextView

// [app/src/main/java/com/silauncer/cepat/popup/SmartPopupView.kt]: Custom View Container Smart Popup
// [Penjelasan]: Merender popup vertikal terpadu (Dynamic Shortcuts & System Actions) dengan pembatas halus tanpa nilai hardcoded
class SmartPopupView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val cardNotifications: View
    private val tvNotificationTitle: TextView
    private val tvNotificationText: TextView

    private val rvShortcuts: RecyclerView
    private val dividerShortcuts: View
    private val btnActionInfo: View
    private val btnActionStorage: View
    private val btnActionUninstall: View
    private val btnActionShare: View

    private var onInfoClickListener: (() -> Unit)? = null
    private var onStorageClickListener: (() -> Unit)? = null
    private var onUninstallClickListener: (() -> Unit)? = null
    private var onShareClickListener: (() -> Unit)? = null
    private var onShortcutClickListener: ((ParsedShortcut) -> Unit)? = null

    init {
        orientation = VERTICAL
        val view = LayoutInflater.from(context).inflate(R.layout.view_smart_popup, this, true)

        cardNotifications = view.findViewById(R.id.card_notifications)
        tvNotificationTitle = view.findViewById(R.id.tv_notification_title)
        tvNotificationText = view.findViewById(R.id.tv_notification_text)

        rvShortcuts = view.findViewById(R.id.rv_shortcuts)
        dividerShortcuts = view.findViewById(R.id.divider_shortcuts)
        btnActionInfo = view.findViewById(R.id.btn_action_info)
        btnActionStorage = view.findViewById(R.id.btn_action_storage)
        btnActionUninstall = view.findViewById(R.id.btn_action_uninstall)
        btnActionShare = view.findViewById(R.id.btn_action_share)

        rvShortcuts.layoutManager = LinearLayoutManager(context)

        btnActionInfo.setOnClickListener { onInfoClickListener?.invoke() }
        btnActionStorage.setOnClickListener { onStorageClickListener?.invoke() }
        btnActionUninstall.setOnClickListener { onUninstallClickListener?.invoke() }
        btnActionShare.setOnClickListener { onShareClickListener?.invoke() }
    }

    fun setupNotifications(notifications: List<NotificationItem>) {
        if (notifications.isNotEmpty()) {
            val firstNotif = notifications.first()
            tvNotificationTitle.text = firstNotif.title
            tvNotificationText.text = firstNotif.text
            cardNotifications.visibility = VISIBLE
        } else {
            cardNotifications.visibility = GONE
        }
    }

    fun setupShortcuts(shortcuts: List<ParsedShortcut>) {
        if (shortcuts.isNotEmpty()) {
            rvShortcuts.visibility = VISIBLE
            dividerShortcuts.visibility = VISIBLE
            rvShortcuts.adapter = ShortcutItemAdapter(shortcuts) { shortcut ->
                onShortcutClickListener?.invoke(shortcut)
            }
        } else {
            rvShortcuts.visibility = GONE
            dividerShortcuts.visibility = GONE
        }
    }

    fun setOnInfoClickListener(listener: () -> Unit) {
        onInfoClickListener = listener
    }

    fun setOnStorageClickListener(listener: () -> Unit) {
        onStorageClickListener = listener
    }

    fun setOnUninstallClickListener(listener: () -> Unit) {
        onUninstallClickListener = listener
    }

    fun setOnShareClickListener(listener: () -> Unit) {
        onShareClickListener = listener
    }

    fun setOnShortcutClickListener(listener: (ParsedShortcut) -> Unit) {
        onShortcutClickListener = listener
    }
}
