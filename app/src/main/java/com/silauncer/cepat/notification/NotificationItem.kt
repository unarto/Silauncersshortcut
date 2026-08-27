package com.silauncer.cepat.notification

import android.os.Parcel
import android.os.Parcelable

// [app/src/main/java/com/silauncer/cepat/notification/NotificationItem.kt]: Model Data Notifikasi
// [Penjelasan]: Mengimplementasikan Parcelable standar Android untuk transfer data antar komponen tanpa library tambahan
data class NotificationItem(
    val id: Int,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(packageName)
        parcel.writeString(title)
        parcel.writeString(text)
        parcel.writeLong(postTime)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<NotificationItem> {
        override fun createFromParcel(parcel: Parcel): NotificationItem = NotificationItem(parcel)
        override fun newArray(size: Int): Array<NotificationItem?> = arrayOfNulls(size)
    }
}
