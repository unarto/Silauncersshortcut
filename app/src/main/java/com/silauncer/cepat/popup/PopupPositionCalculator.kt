package com.silauncer.cepat.popup

import android.view.View

// [app/src/main/java/com/silauncer/cepat/popup/PopupPositionCalculator.kt]: Kalkulator Posisi Popup
// [Penjelasan]: Menghitung koordinat X dan Y popup relatif terhadap posisi ikon di layar agar tidak terpotong
data class PopupPosition(
    val x: Int,
    val y: Int,
    val showAbove: Boolean
)

object PopupPositionCalculator {

    fun calculate(
        targetView: View,
        popupWidth: Int,
        popupHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        marginPx: Int
    ): PopupPosition {
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)

        val targetX = location[0]
        val targetY = location[1]
        val targetWidth = targetView.width
        val targetHeight = targetView.height

        // X coordinate calculation (center or align safely)
        var x = targetX + (targetWidth - popupWidth) / 2
        if (x < marginPx) {
            x = marginPx
        } else if (x + popupWidth > screenWidth - marginPx) {
            x = screenWidth - marginPx - popupWidth
        }

        // Y coordinate calculation (prefer above, if not enough space show below)
        val spaceAbove = targetY - marginPx
        val spaceBelow = screenHeight - (targetY + targetHeight) - marginPx

        val showAbove = if (spaceAbove >= popupHeight) {
            true
        } else if (spaceBelow >= popupHeight) {
            false
        } else {
            spaceAbove > spaceBelow
        }

        val y = if (showAbove) {
            targetY - popupHeight - marginPx
        } else {
            targetY + targetHeight + marginPx
        }

        return PopupPosition(x, y, showAbove)
    }
}
