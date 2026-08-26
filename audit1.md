# LAPORAN AUDIT MENYELURUH BASIS KODE (AUDIT 1)

Dokumen ini berisi hasil audit komprehensif terhadap seluruh modul, arsitektur kelas, dependensi, dan resource pada proyek **Silauncer**. Temuan dikelompokkan ke dalam 5 kategori fokus audit:
1. **Kode Mati (Dead Code / Unreachable Code)**
2. **Kode yang Tidak Digunakan (Unused Code / Variables / Imports / Methods)**
3. **Kode Duplikat (Duplicate Logic / Redundant Implementation)**
4. **Placeholder, Simulasi Tiruan, Mock, dan Fake Data**
5. **Data Hardcoded (String, Dimensi, Parameter Tetap)**

---

## 1. Kategori: Kode Mati (Dead Code / Unreachable Code)

### Temuan 1.1: Method Stub Kosong / Placeholder pada `ShortcutKey`
- **Lokasi Indikasi**: `/app/src/main/java/com/silauncer/cepat/shortcut/ShortcutKey.kt` (Baris 24–26)
- **Detail Deskripsi Masalah**:
  Terdapat method `fun buildRequest(context: android.content.Context): Any?` yang selalu mengembalikan `null` dengan komentar `// Placeholder if needed`. Method ini tidak memiliki implementasi fungsional dan tidak pernah dipanggil di mana pun dalam seluruh alur peluncuran shortcut atau launcher.
- **Rekomendasi**: Hapus method `buildRequest` agar tidak menyisakan fungsi kosong tanpa tujuan (dead code).

---

### Temuan 1.2: Method Factory `fromIntent` yang Tidak Terjangkau pada `ShortcutKey`
- **Lokasi Indikasi**: `/app/src/main/java/com/silauncer/cepat/shortcut/ShortcutKey.kt` (Baris 40–43)
- **Detail Deskripsi Masalah**:
  Method `fromIntent(intent: Intent, user: UserHandle): ShortcutKey` didefinisikan di dalam companion object `ShortcutKey`, namun tidak ada satu pun kelas (termasuk `LauncherActivity`, `ShortcutLauncher`, `PopupController`) yang memanggil method ini.
- **Rekomendasi**: Hapus atau evaluasi apakah intent shortcut masuk ke Activity launcher membutuhkan parsing via method ini.

---

### Temuan 1.3: Activity Terisolasi (Orphaned / Unreachable Component) `ShortcutPickerActivity`
- **Lokasi Indikasi**:
  - `/app/src/main/java/com/silauncer/cepat/shortcut/ShortcutPickerActivity.kt`
  - `/app/src/main/java/com/silauncer/cepat/shortcut/ShortcutPickerAdapter.kt`
  - `/app/src/main/res/layout/activity_shortcut_picker.xml`
  - `/app/src/main/res/layout/item_shortcut_picker.xml`
  - `/app/src/main/AndroidManifest.xml` (Baris 34)
- **Detail Deskripsi Masalah**:
  Activity `ShortcutPickerActivity` beserta adapter dan layout terkait telah dideklarasikan dan terdaftar di Manifest (`android:exported="false"`), namun tidak ada intent, menu pengaturan, tombol, atau alur pengguna mana pun yang memulai (`startActivity`) komponen ini. Kode ini sepenuhnya terisolasi (dead component).
- **Rekomendasi**: Hubungkan ke menu pengaturan (misal opsi "Pilih Pintasan Aplikasi") atau hapus jika fungsinya sudah sepenuhnya digantikan oleh `SmartPopupView` dan `ShortcutConfigActivityInfo` langsung.

---

### Temuan 1.4: Implementasi Pasif / Dead Wrap pada `OverScroll.kt`
- **Lokasi Indikasi**: `/app/src/main/java/com/silauncer/cepat/home/OverScroll.kt` (Baris 1–22)
- **Detail Deskripsi Masalah**:
  Objek `OverScroll` hanya menjalankan `recyclerView.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS`. Seluruh struktur kelas, interface callback terisolasi, atau wrapper objek ini menjadi overhead berlebih tanpa efek overscroll kustom nyata (seperti stretch effect dinamis).
- **Rekomendasi**: Sederhanakan konfigurasi `overScrollMode` langsung ke inisialisasi RecyclerView pada `LauncherActivity.kt` dan hapus file `OverScroll.kt` yang tidak memberikan nilai tambah arsitektur.

---

## 2. Kategori: Kode yang Tidak Digunakan (Unused Code / Variables / Imports / Methods)

### Temuan 2.1: Properti Legacy `appOrder` pada `LauncherPreferences`
- **Lokasi Indikasi**: `/app/src/main/java/com/silauncer/cepat/storage/LauncherPreferences.kt` (Baris 47–56)
- **Detail Deskripsi Masalah**:
  Properti `appOrder: List<String>` disimpan di MMKV dan hanya dibaca satu kali pada fungsi migrasi `LauncherAppController.migrateIfNeeded()`. Nilai `appOrder` di MMKV tidak lagi pernah ditulis/disinkronkan kembali saat pengguna mengubah susunan aplikasi, karena persistensi grid saat ini sudah 100% menggunakan database Room (`LauncherRepository` / `AppItemEntity`).
- **Rekomendasi**: Setelah fase migrasi data Room selesai, properti dan key MMKV `KEY_APP_ORDER` dapat dibersihkan untuk menghindari kebingungan antara penyimpanan MMKV dan Room DB.

---

### Temuan 2.2: String Resource yang Didefinisikan tapi Tidak Pernah Digunakan
- **Lokasi Indikasi**: `/app/src/main/res/values/strings.xml`
  - Baris 21–22: `<string name="pref_grid_layout">Tata Letak Kisi</string>` & `<string name="pref_grid_layout_desc">Jumlah kolom dan baris aplikasi pada layar utama</string>`
  - Baris 66: `<string name="action_app_storage">Penyimpanan</string>`
- **Detail Deskripsi Masalah**:
  Resource string di atas didefinisikan dalam `strings.xml`, namun tidak pernah direferensikan dalam layout XML maupun di kelas Kotlin (`SettingsNodeFactory.kt`, `SmartPopupView.kt`, dsb.).
- **Rekomendasi**: Hapus entri string yang tidak terpakai atau gunakan pada komponen yang relevan agar resource XML tetap bersih.

---

### Temuan 2.3: Integer Resource Animasi yang Terabaikan
- **Lokasi Indikasi**: `/app/src/main/res/values/integers.xml` (Baris 5–8)
  - `motion_duration_app_remove` (200)
  - `motion_duration_app_add` (280)
  - `motion_duration_app_move` (320)
  - `motion_duration_app_change` (240)
- **Detail Deskripsi Masalah**:
  Resource `integers.xml` didefinisikan secara khusus untuk durasi Material Motion, tetapi `MaterialGridItemAnimator.kt` tidak menggunakannya sama sekali dan justru mengunci nilai durasi secara hardcoded (`addDuration = 220`, `removeDuration = 180`, `moveDuration = 250`, `changeDuration = 200`).
- **Rekomendasi**: Hubungkan `MaterialGridItemAnimator.kt` dengan resource `R.integer.motion_duration_*` atau hapus file `integers.xml` jika nilai animasi ingin diatur terpusat di kode.

---

### Temuan 2.4: Fallback Matching Nama Drawable yang Tidak Efektif pada `IconPackRepository`
- **Lokasi Indikasi**: `/app/src/main/java/com/silauncer/cepat/iconpack/IconPackRepository.kt` (Baris 103–106)
- **Detail Deskripsi Masalah**:
  `val fallbackResId = res.getIdentifier(componentName.packageName.replace(".", "_"), "drawable", iconPackPackage)`
  Strategi fallback ini hampir tidak pernah cocok dengan konvensi penamaan drawable dalam paket ikon pihak ketiga yang standar (yang berbasis `appfilter.xml`).
- **Rekomendasi**: Rapikan alur pemetaan ikon dan sediakan mekanisme logging/fallback yang sesuai spesifikasi paket ikon modern.

---

## 3. Kategori: Kode Duplikat (Duplicate Logic / Redundant Implementation)

### Temuan 3.1: Duplikasi Konversi DP ke PX Manual di Berbagai File UI
- **Lokasi Indikasi**:
  - `/app/src/main/java/com/silauncer/cepat/launcher/LauncherActivity.kt` (Method `dpToPx`)
  - `/app/src/main/java/com/silauncer/cepat/popup/SmartPopupView.kt` (Baris 102, 104, 106, 109, 118–121, 124)
  - `/app/src/main/java/com/silauncer/cepat/popup/PopupController.kt` (Baris 135)
  - `/app/src/main/java/com/silauncer/cepat/home/AppAdapter.kt` (Baris 111, 139)
- **Detail Deskripsi Masalah**:
  Perhitungan konversi `(dp * density).toInt()` dilakukan secara berulang-ulang dan tersebar di berbagai kelas view/adapter.
- **Rekomendasi**: Buat satu Kotlin Extension function terpusat (misal `Number.dpToPx(context)` atau `Context.dpToPx(dp)`) atau manfaatkan resource `dimens.xml` secara seragam.

---

### Temuan 3.2: Duplikasi Logika Ekstraksi Ikon dan Query Package Manager
- **Lokasi Indikasi**:
  - `/app/src/main/java/com/silauncer/cepat/apps/AppDataSource.kt`
  - `/app/src/main/java/com/silauncer/cepat/cache/IconLoader.kt`
  - `/app/src/main/java/com/silauncer/cepat/settings/treeview/SettingsNodeFactory.kt` (Baris 301–305)
- **Detail Deskripsi Masalah**:
  Pemuatan ikon aplikasi dilakukan dengan cara yang berbeda di 3 tempat: `AppDataSource` mengambil lewat `LauncherActivityInfo`, `IconLoader` menggabungkan `IconPackRepository` + `LruCache`, sedangkan `SettingsNodeFactory` langsung memanggil `pm.getActivityIcon(app.componentName)`.
- **Rekomendasi**: Satukan seluruh alur pemuatan ikon melalui `IconLoader` / `IconCache` agar konsisten mendukung caching dan Icon Pack di seluruh aplikasi (termasuk pada menu pengaturan aplikasi tersembunyi).

---

### Temuan 3.3: Duplikasi Pengecekan Manifest Shortcut Resource
- **Lokasi Indikasi**: `/app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt` (Baris 65–101)
- **Detail Deskripsi Masalah**:
  Fungsi `getManifestShortcutsFromXml` melakukan dua iterasi terpisah: pertama melalui `pm.queryIntentActivities(...)`, lalu jika kosong melakukan iterasi ulang kedua melalui `pm.getPackageInfo(..., GET_ACTIVITIES)`. Keduanya mengeksekusi logika pencarian metadata `"android.app.shortcuts"` yang identik.
- **Rekomendasi**: Satukan logika ekstraksi ID XML shortcuts ke dalam satu helper method privat agar tidak terjadi pengulangan loop inspeksi metadata.

---

## 4. Kategori: Placeholder, Simulasi Tiruan, Mock, dan Fake Data

### Temuan 4.1: Pengambilan Notifikasi Terbatas Hanya pada Elemen Pertama
- **Lokasi Indikasi**:
  - `/app/src/main/java/com/silauncer/cepat/popup/SmartPopupView.kt` (Baris 146–154)
- **Detail Deskripsi Masalah**:
  `NotificationService` mengumpulkan seluruh notifikasi aktif (`List<NotificationItem>`), namun `SmartPopupView.setupNotifications()` hanya mengambil elemen pertama (`val firstNotif = notifications.first()`) dan mengabaikan notifikasi lainnya. Jika ada lebih dari satu notifikasi, tidak ada indikator jumlah atau scrolling/stacking kartu notifikasi.
- **Rekomendasi**: Implementasikan tampilan multi-notifikasi atau adapter ringkas untuk daftar notifikasi pada SmartPopup jika jumlah notifikasi > 1.

---

### Temuan 4.2: Komentar dan Stubbing pada OEM Themes / Icon Pack
- **Lokasi Indikasi**: `/app/src/main/java/com/silauncer/cepat/iconpack/IconPackRepository.kt` (Baris 44–47)
- **Detail Deskripsi Masalah**:
  Terdapat catatan komentar mengenai integrasi OEM themes khusus (ColorOS/MIUI) yang belum diimplementasikan dan dibiarkan sebagai catatan terbuka.
- **Rekomendasi**: Pastikan dokumentasi jelas bahwa pemindaian intent pihak ketiga standar (ADW, Nova, Go, Apex) sudah mencakup 99% paket ikon di Google Play Store.

---

## 5. Kategori: Data Hardcoded (String, Dimensi, Parameter Tetap)

### Temuan 5.1: Nilai Dimensi & Padding Hardcoded di dalam Kode Kotlin UI
- **Lokasi Indikasi**: `/app/src/main/java/com/silauncer/cepat/popup/SmartPopupView.kt`
  - Baris 102: Tinggi single action horizontal dikunci `44 * density`.
  - Baris 104, 106: Padding horizontal single action dikunci `16 * density`.
  - Baris 109: Margin start label single action dikunci `10 * density`.
  - Baris 118, 120: Padding horizontal single action vertical dikunci `20 * density`.
  - Baris 119, 121: Padding vertical single action vertical dikunci `12 * density`.
  - Baris 124: Margin top label single action vertical dikunci `4 * density`.
  - Baris 135: Lebar panah indikator dikunci `24 * density`.
- **Detail Deskripsi Masalah**:
  Nilai-nilai dimensi tampilan popup di atas dihitung dengan angka literal langsung di dalam kelas Kotlin tanpa membaca dari `dimens.xml`.
- **Rekomendasi**: Pindahkan seluruh nilai dimensi tersebut ke `res/values/dimens.xml` (misal `@dimen/popup_single_action_height`, `@dimen/popup_arrow_width`, dsb.) agar konsisten dengan aturan *No Hardcoded Dimensions*.

---

### Temuan 5.2: String Literal Bahasa Indonesia Hardcoded pada Menu Settings
- **Lokasi Indikasi**: `/app/src/main/java/com/silauncer/cepat/settings/treeview/SettingsNodeFactory.kt`
  - Baris 53: `title = "Kolom (Columns)"`
  - Baris 66: `title = "Baris (Rows)"`
- **Detail Deskripsi Masalah**:
  Judul untuk slider kolom dan baris ditulis langsung dalam bentuk string literal bahasa Indonesia/Inggris tanpa menggunakan `context.getString(R.string.*)`. Hal ini menyebabkan teks tidak dapat diterjemahkan secara dinamis saat pengguna mengganti bahasa aplikasi ke English.
- **Rekomendasi**: Tambahkan `<string name="pref_grid_columns">Kolom</string>` dan `<string name="pref_grid_rows">Baris</string>` ke dalam `strings.xml` dan panggil via `context.getString(...)`.

---

### Temuan 5.3: Nilai Batas Slider (Min, Max, Step) Hardcoded
- **Lokasi Indikasi**: `/app/src/main/java/com/silauncer/cepat/settings/treeview/SettingsNodeFactory.kt` (Baris 57–59, 70–72, 83–85, 165–167, 189–191)
- **Detail Deskripsi Masalah**:
  Nilai minimum, maksimum, dan kelipatan step untuk slider (kolom 3–8, baris 3–10, icon spacing 0–48, icon size 32–96, label size 8–24) ditulis langsung sebagai angka floating point pada pembentukan TreeNode.
- **Rekomendasi**: Definisikan konstanta batas konfigurasi di tingkat objek konfigurasi atau resource terpusat untuk mempermudah penyesuaian di masa mendatang.

---

### Temuan 5.4: URL Play Store Hardcoded pada Fitur Bagikan Aplikasi
- **Lokasi Indikasi**: `/app/src/main/java/com/silauncer/cepat/popup/PopupController.kt` (Baris 183)
- **Detail Deskripsi Masalah**:
  `"https://play.google.com/store/apps/details?id=$packageName"` ditulis langsung sebagai string hardcoded.
- **Rekomendasi**: Pindahkan format URL ke `strings.xml` sebagai `<string name="url_play_store_format">https://play.google.com/store/apps/details?id=%1$s</string>`.

---

### Temuan 5.5: Durasi dan Skala Animasi Hardcoded pada Drag & Drop Handler
- **Lokasi Indikasi**: `/app/src/main/java/com/silauncer/cepat/launcher/GridDragAndDropHandler.kt` (Baris 30, 62–66, 109)
- **Detail Deskripsi Masalah**:
  - Nilai pengali timeout long press `0.75f` di-hardcode.
  - Nilai animasi drag `scaleX(1.15f)`, `scaleY(1.15f)`, `alpha(0.85f)`, dan durasi `200ms` di-hardcode di kode Kotlin.
- **Rekomendasi**: Gunakan resource integer dan interpolator standar untuk menjaga konsistensi motion tokens di seluruh aplikasi.

---

## 6. Ringkasan & Matriks Audit

| No | Kategori Temuan | Total Indikasi | Dampak Arsitektur / Kinerja |
|---|---|---|---|
| 1 | Kode Mati (Dead / Unreachable Code) | 4 Lokasi | Menambah ukuran APK & kebingungan alur pemanggilan kelas |
| 2 | Kode Tidak Digunakan (Unused Code & Resources) | 4 Lokasi | Polusi resource XML dan redundansi field data |
| 3 | Kode Duplikat (Duplicate Logic) | 3 Pola | Inkonsistensi implementasi dan beban maintenance berlebih |
| 4 | Placeholder / Simulasi / Mock | 2 Lokasi | Fungsionalitas terbatas pada notifikasi bertumpuk |
| 5 | Data Hardcoded (String & Dimensi) | 5 Lokasi | Menghambat lokalisasi multi-bahasa & adaptasi layar dinamis |

---
*Laporan ini disusun secara objektif dan mendalam berdasarkan penelusuran seluruh berkas kode aktual proyek Silauncer.*
