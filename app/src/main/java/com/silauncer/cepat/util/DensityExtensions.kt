package com.silauncer.cepat.util

import android.content.Context
import android.util.TypedValue
import android.view.View

// [app/src/main/java/com/silauncer/cepat/util/DensityExtensions.kt]: Utilitas Konversi Satuan Layar Terpusat
// [Penjelasan]: Fungsi ekstensi terpadu untuk konversi DP/SP ke Pixel guna mencegah duplikasi kalkulasi di berbagai komponen UI
fun Context.dpToPx(dp: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        resources.displayMetrics
    ).toInt()
}

fun Context.dpToPx(dp: Int): Int = dpToPx(dp.toFloat())

fun View.dpToPx(dp: Float): Int = context.dpToPx(dp)

fun View.dpToPx(dp: Int): Int = context.dpToPx(dp.toFloat())

fun Context.spToPx(sp: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp,
        resources.displayMetrics
    )
}
