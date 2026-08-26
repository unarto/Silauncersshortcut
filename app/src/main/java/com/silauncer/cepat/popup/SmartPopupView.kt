package com.silauncer.cepat.popup

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.shortcut.ParsedShortcut
import com.silauncer.cepat.notification.NotificationItem

// [app/src/main/java/com/silauncer/cepat/popup/SmartPopupView.kt]: Custom View Container Smart Popup
// [Penjelasan]: Merender popup terpadu (Dynamic Shortcuts & System Actions) dengan kartu terpisah dan susunan dinamis AOSP style
class SmartPopupView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val cardContainer: LinearLayout
    private val cardNotificationsContainer: View
    private val tvNotificationTitle: TextView
    private val tvNotificationText: TextView
    private val cardShortcutsContainer: View
    private val rvShortcuts: RecyclerView
    
    // Single system action card (pill horizontal)
    private val cardSystemActionSingle: View
    private val imgSingleAction: ImageView
    private val tvSingleAction: TextView
    
    // Multi system actions card (grid horizontal 3 tombol)
    private val cardSystemActionsMulti: View
    private val btnActionInfo: View
    private val btnActionUninstall: View
    private val btnActionShare: View
    
    private val arrowUp: View
    private val arrowDown: View

    private var onInfoClickListener: (() -> Unit)? = null
    private var onUninstallClickListener: (() -> Unit)? = null
    private var onShareClickListener: (() -> Unit)? = null
    private var onShortcutClickListener: ((ParsedShortcut) -> Unit)? = null

    init {
        orientation = VERTICAL
        val view = LayoutInflater.from(context).inflate(R.layout.view_smart_popup, this, true)

        cardContainer = view.findViewById(R.id.card_container)
        cardNotificationsContainer = view.findViewById(R.id.card_notifications_container)
        tvNotificationTitle = view.findViewById(R.id.tv_notification_title)
        tvNotificationText = view.findViewById(R.id.tv_notification_text)
        
        cardShortcutsContainer = view.findViewById(R.id.card_shortcuts_container)
        rvShortcuts = view.findViewById(R.id.rv_shortcuts)
        
        cardSystemActionSingle = view.findViewById(R.id.card_system_action_single)
        imgSingleAction = view.findViewById(R.id.img_single_action)
        tvSingleAction = view.findViewById(R.id.tv_single_action)
        
        cardSystemActionsMulti = view.findViewById(R.id.card_system_actions_multi)
        btnActionInfo = view.findViewById(R.id.btn_action_info)
        btnActionUninstall = view.findViewById(R.id.btn_action_uninstall)
        btnActionShare = view.findViewById(R.id.btn_action_share)
        
        arrowUp = view.findViewById(R.id.arrow_up)
        arrowDown = view.findViewById(R.id.arrow_down)

        rvShortcuts.layoutManager = LinearLayoutManager(context)

        cardSystemActionSingle.setOnClickListener { onInfoClickListener?.invoke() }
        btnActionInfo.setOnClickListener { onInfoClickListener?.invoke() }
        btnActionUninstall.setOnClickListener { onUninstallClickListener?.invoke() }
        btnActionShare.setOnClickListener { onShareClickListener?.invoke() }
    }

    // [app/src/main/java/com/silauncer/cepat/popup/SmartPopupView.kt]: Konfigurasi Aksi Sistem Pintar
    // [Penjelasan]: Memilih antara format Single Pill (horizontal jika ada shortcuts, vertical compact jika berdiri sendiri) atau Multi Action Row
    fun setupSystemActions(showInfo: Boolean, showUninstall: Boolean, showShare: Boolean, hasShortcuts: Boolean = false) {
        val visibleCount = (if (showInfo) 1 else 0) + (if (showUninstall) 1 else 0) + (if (showShare) 1 else 0)
        
        if (visibleCount <= 1 && showInfo) {
            // Tampilkan single action pill
            cardSystemActionsMulti.visibility = GONE
            cardSystemActionSingle.visibility = VISIBLE
            imgSingleAction.setImageResource(R.drawable.ic_info)
            tvSingleAction.setText(R.string.action_app_info)

            val params = cardSystemActionSingle.layoutParams as LayoutParams
            val tvParams = tvSingleAction.layoutParams as LayoutParams

            if (hasShortcuts) {
                // Format Horizontal (Screenshot 4 YouTube)
                (cardSystemActionSingle as LinearLayout).orientation = HORIZONTAL
                (cardSystemActionSingle as LinearLayout).gravity = android.view.Gravity.CENTER_VERTICAL
                params.width = resources.getDimensionPixelSize(R.dimen.popup_card_width)
                params.height = resources.getDimensionPixelSize(R.dimen.popup_single_action_height)
                val padHoriz = resources.getDimensionPixelSize(R.dimen.popup_single_action_padding_horizontal)
                cardSystemActionSingle.setPadding(padHoriz, 0, padHoriz, 0)
                tvParams.marginStart = resources.getDimensionPixelSize(R.dimen.popup_single_action_margin_start)
                tvParams.topMargin = 0
            } else {
                // Format Vertical Compact (Screenshot 2 OKX Wallet)
                (cardSystemActionSingle as LinearLayout).orientation = VERTICAL
                (cardSystemActionSingle as LinearLayout).gravity = android.view.Gravity.CENTER
                params.width = LayoutParams.WRAP_CONTENT
                params.height = LayoutParams.WRAP_CONTENT
                val padHoriz = resources.getDimensionPixelSize(R.dimen.popup_single_action_vertical_padding_horizontal)
                val padVert = resources.getDimensionPixelSize(R.dimen.popup_single_action_vertical_padding_vertical)
                cardSystemActionSingle.setPadding(padHoriz, padVert, padHoriz, padVert)
                tvParams.marginStart = 0
                tvParams.topMargin = resources.getDimensionPixelSize(R.dimen.popup_single_action_vertical_margin_top)
            }
            tvSingleAction.layoutParams = tvParams
            cardSystemActionSingle.layoutParams = params
        } else if (visibleCount > 1) {
            // Tampilkan multi action card (horizontal grid 3 tombol: Screenshot 1 Keep, Screenshot 3 ShopeePay)
            cardSystemActionSingle.visibility = GONE
            cardSystemActionsMulti.visibility = VISIBLE
            btnActionInfo.visibility = if (showInfo) VISIBLE else GONE
            btnActionUninstall.visibility = if (showUninstall) VISIBLE else GONE
            btnActionShare.visibility = if (showShare) VISIBLE else GONE

            val params = cardSystemActionsMulti.layoutParams
            params.width = resources.getDimensionPixelSize(R.dimen.popup_card_width)
            cardSystemActionsMulti.layoutParams = params
        } else {
            cardSystemActionSingle.visibility = GONE
            cardSystemActionsMulti.visibility = GONE
        }
    }

    // [app/src/main/java/com/silauncer/cepat/popup/SmartPopupView.kt]: Penataan Kartu Notifikasi Nyata
    // [Penjelasan]: Menampilkan judul dan isi pesan notifikasi teratas serta indikator jumlah bila terdapat lebih dari satu notifikasi aktif
    fun setupNotifications(notifications: List<NotificationItem>) {
        if (notifications.isNotEmpty()) {
            val firstNotif = notifications.first()
            if (notifications.size > 1) {
                val extraCount = notifications.size - 1
                tvNotificationTitle.text = "${firstNotif.title} (${context.getString(R.string.notifications_count_more, extraCount)})"
            } else {
                tvNotificationTitle.text = firstNotif.title
            }
            tvNotificationText.text = firstNotif.text
            cardNotificationsContainer.visibility = VISIBLE
        } else {
            cardNotificationsContainer.visibility = GONE
        }
    }

    fun setupShortcuts(shortcuts: List<ParsedShortcut>) {
        if (shortcuts.isNotEmpty()) {
            cardShortcutsContainer.visibility = VISIBLE
            rvShortcuts.adapter = ShortcutItemAdapter(shortcuts) { shortcut ->
                onShortcutClickListener?.invoke(shortcut)
            }
        } else {
            cardShortcutsContainer.visibility = GONE
        }
    }

    private fun getActiveSystemCard(): View? {
        return if (cardSystemActionSingle.visibility == VISIBLE) {
            cardSystemActionSingle
        } else if (cardSystemActionsMulti.visibility == VISIBLE) {
            cardSystemActionsMulti
        } else {
            null
        }
    }

    // [app/src/main/java/com/silauncer/cepat/popup/SmartPopupView.kt]: Pengurutan Kartu dan Posisi Panah Sesuai Screenshot
    // [Penjelasan]: Menyusun kartu secara dinamis: jika single action & shortcuts, single action di atas; jika multi action, shortcuts di atas
    fun setupArrow(showAbove: Boolean, arrowOffset: Float) {
        val activeSystemCard = getActiveSystemCard()
        val hasShortcuts = cardShortcutsContainer.visibility == VISIBLE
        val isSingleSystemAction = cardSystemActionSingle.visibility == VISIBLE
        
        cardContainer.removeAllViews()

        if (showAbove) {
            arrowUp.visibility = GONE
            arrowDown.visibility = VISIBLE
            arrowDown.translationX = arrowOffset

            if (cardNotificationsContainer.visibility == VISIBLE) {
                cardContainer.addView(cardNotificationsContainer)
            }

            if (hasShortcuts && isSingleSystemAction) {
                // Sesuai Screenshot 4 (YouTube): Single Action di Atas, Shortcuts di Bawah (dekat ikon)
                if (activeSystemCard != null) cardContainer.addView(activeSystemCard)
                cardContainer.addView(cardShortcutsContainer)
            } else {
                // Sesuai Screenshot 3 (ShopeePay): Shortcuts di Atas, Multi Actions di Bawah (dekat ikon)
                if (hasShortcuts) cardContainer.addView(cardShortcutsContainer)
                if (activeSystemCard != null) cardContainer.addView(activeSystemCard)
            }
        } else {
            // Popup muncul di bawah target icon
            arrowUp.visibility = VISIBLE
            arrowDown.visibility = GONE
            arrowUp.translationX = arrowOffset

            if (hasShortcuts && isSingleSystemAction) {
                cardContainer.addView(cardShortcutsContainer)
                if (activeSystemCard != null) cardContainer.addView(activeSystemCard)
            } else {
                if (activeSystemCard != null) cardContainer.addView(activeSystemCard)
                if (hasShortcuts) cardContainer.addView(cardShortcutsContainer)
            }

            if (cardNotificationsContainer.visibility == VISIBLE) {
                cardContainer.addView(cardNotificationsContainer)
            }
        }
        
        updateMargins()
    }
    
    private fun updateMargins() {
        var isFirst = true
        for (i in 0 until cardContainer.childCount) {
            val child = cardContainer.getChildAt(i)
            val params = child.layoutParams as LinearLayout.LayoutParams
            if (isFirst) {
                params.topMargin = 0
                isFirst = false
            } else {
                params.topMargin = resources.getDimensionPixelSize(R.dimen.spacing_small)
            }
            child.layoutParams = params
        }
    }

    fun setOnInfoClickListener(listener: () -> Unit) {
        onInfoClickListener = listener
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
