# Laporan Audit: Layout Pengaturan Laci Aplikasi & Urutan
Tanggal: 2026-08-27
Cakupan: `app/src/main/res/layout/item_tree_subparent.xml` dan `app/src/main/res/values/strings.xml`

Berdasarkan keluhan dan screenshot yang diberikan (tampilan teks berantakan dan terjepit pada menu *Aplikasi Tersembunyi*), berikut adalah hasil audit dan perbaikan yang telah dilakukan:

## 1. Identifikasi Masalah Utama
* **Gejala:** Teks judul "Aplikasi Tersembunyi" dan deskripsinya tergulung menjadi satu kolom vertikal yang sangat sempit (terjepit).
* **Akar Penyebab (Root Cause):** Pada file `item_tree_subparent.xml`, komponen badge status (`tv_subparent_value`) menggunakan atribut `android:layout_width="wrap_content"` di dalam sebuah `LinearLayout` horizontal. String terjemahan untuk nilai tersebut sangat panjang (yaitu: *"Tidak ada aplikasi yang disembunyikan"* dengan panjang 35 karakter). Akibatnya, saat Android merender layout, TextView tersebut "memakan" hampir seluruh lebar layar agar teks dapat dimuat dalam satu baris, sehingga sisa ruang untuk judul menjadi 0 dan teks judul terpaksa di-wrap ke bawah huruf-demi-huruf.

## 2. Tindakan Perbaikan (Tetap Mempertahankan TreeView)

Untuk memperbaiki cacat visual ini tanpa merusak arsitektur *TreeView* yang sudah ada, dilakukan dua tingkat perbaikan (UI & Data):

### A. Perbaikan Robustness Layout (`item_tree_subparent.xml`)
Komponen pill/badge status sekarang dibatasi agar tidak dapat merusak tata letak *parent*:
* Ditambahkan `android:maxWidth="120dp"` agar badge tidak mengembang tak terbatas.
* Ditambahkan `android:maxLines="1"` dan `android:ellipsize="end"` sehingga apabila teks terpaksa melebihi batas 120dp, teks akan dipotong rapi dengan titik-titik (...) tanpa menekan teks judul.

### B. Perbaikan UX String (`strings.xml`)
String terjemahan yang terlalu panjang tidak cocok untuk sebuah *badge* indikator status yang berukuran kecil. String telah dipersingkat agar lebih padat dan estetis:
* `pref_hidden_apps_empty`: Diubah dari *"Tidak ada aplikasi yang disembunyikan"* menjadi **"0 Aplikasi"**.
* `pref_hidden_apps_count`: Diubah dari *"%1$d aplikasi disembunyikan"* menjadi **"%1$d Aplikasi"**.

## 3. Hasil Validasi
Dengan kombinasi pembatasan `maxWidth` dan string yang lebih singkat, tampilan menu "Laci Aplikasi & Urutan" akan kembali rapi. Teks judul memiliki ruang yang luas untuk membentang, dan indikator jumlah aplikasi tersembunyi kini tampil sebagai *badge* kecil yang presisi di sebelah kanan.
