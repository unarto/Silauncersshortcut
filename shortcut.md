# Laporan Audit & Perbaikan Modul Shortcut (Silauncer)
Tanggal: 2026-08-26
Cakupan: `app/src/main/java/com/silauncer/cepat/shortcut/`

Dokumen ini berisi hasil audit dan status perbaikan menyeluruh pada fitur *Shortcut Pintar* (mencakup `ShortcutFetcher`, `ShortcutParser`, `ShortcutLauncher`, dan `ShortcutKey`).

---

## 1. Temuan yang Sudah Diperbaiki

| ID | File | Deskripsi Perbaikan | Status |
|---|---|---|---|
| **BUG-SC-01** | `ShortcutLauncher.kt` | Ditambahkan penanganan log error terstruktur `Log.e` pada blok catch `directIntent`, `ActivityNotFoundException`, dan `SecurityException` saat meluncurkan config activity shortcut. Context error dicatat secara spesifik tanpa mengubah alur return boolean. | **SELESAI (PASS)** |
| **BUG-SC-02** | `ShortcutLauncher.kt` | Ditambahkan logging error `Log.e` pada blok catch eksekusi `intentSender` untuk config shortcut. | **SELESAI (PASS)** |
| **BUG-SC-03** | `ShortcutParser.kt` | Ditambahkan logging warning `Log.w` saat pemuatan icon shortcut via `LauncherApps.getShortcutIconDrawable` atau `LauncherActivityInfo.getIcon` mengalami kegagalan. | **SELESAI (PASS)** |
| **DEAD-SC-01** | `ShortcutKey.kt` | Method tak terpakai `fun fromIntent(intent: Intent, user: UserHandle)` yang tidak memiliki consumer di seluruh project telah dihapus. | **SELESAI (PASS)** |
| **HARD-SC-01** | `ShortcutFetcher.kt` | Konstanta XML Android (namespace `http://schemas.android.com/apk/res/android`, tag `shortcut`, `intent`, dan atribut `shortcutId`, `enabled`, `shortcutShortLabel`, `shortcutLongLabel`, `icon`, `action`, `targetPackage`, `targetClass`, `data`, `android.app.shortcuts`) telah dirapikan menjadi `private const val` tanpa mengubah fungsionalitas dan perilaku runtime. | **SELESAI (PASS)** |

---

## 2. File & Sumber Daya yang Dihapus

Setelah dilakukan penelusuran menyeluruh ke seluruh codebase launcher dan settings, komponen picker terbukti merupakan sisa kode terisolasi tanpa entry point / consumer. Komponen berikut telah dihapus:

1. `/app/src/main/java/com/silauncer/cepat/shortcut/ShortcutPickerActivity.kt` *(File Kotlin Activity)*
2. `/app/src/main/java/com/silauncer/cepat/shortcut/ShortcutPickerAdapter.kt` *(File Kotlin Adapter)*
3. `/app/src/main/res/layout/activity_shortcut_picker.xml` *(Layout XML Activity)*
4. `/app/src/main/res/layout/item_shortcut_picker.xml` *(Layout XML Item)*
5. Deklarasi `<activity android:name=".shortcut.ShortcutPickerActivity" ... />` pada `/app/src/main/AndroidManifest.xml`
6. Sumber daya string `<string name="title_shortcut_picker">` pada `/app/src/main/res/values/strings.xml`

---

## 3. Temuan yang Sengaja Dipertahankan & Alasannya

| ID | Komponen / Method | Alasan Dipertahankan (Bukan Kode Mati) |
|---|---|---|
| **DEAD-SC-02** | `ShortcutFetcher.getConfigShortcuts()` | **Aktif Digunakan**: Method ini dipanggil secara aktif oleh `PopupController.kt` (baris 44) untuk mengambil daftar pin/config shortcuts pada dialog popup saat ikon aplikasi ditekan lama (long press). |
| **DEAD-SC-03** | `ShortcutParser.parseConfig()` & `parseConfigList()` | **Aktif Digunakan**: Method ini dipanggil secara aktif oleh `PopupController.kt` (baris 45) untuk mengonversi data `LauncherActivityInfo` hasil `getConfigShortcuts()` menjadi `ParsedShortcut` yang siap ditampilkan di `SmartPopupView`. |
| **HARD-SC-02** | `ShortcutKey.INTENT_CATEGORY = "com.android.launcher3.DEEP_SHORTCUT"` | **Standar AOSP**: Kategori intent ini adalah spesifikasi standar AOSP Launcher3 untuk fallback eksekusi deep shortcut lintas vendor, sehingga dipertahankan sebagai konstanta privat di companion object. |

---

## 4. Hasil Validasi Build & Unit Test

- **Compilation (`compile_applet`)**: **PASS** (Applet terkompilasi dengan sempurna tanpa error/warning)
- **Unit Tests (`gradle :app:testDebugUnitTest`)**: **PASS** (34 actionable tasks: 8 executed, 26 up-to-date, 0 failures)
- **Assemble Debug (`gradle :app:assembleDebug`)**: **PASS** (BUILD SUCCESSFUL in 3s, 40 actionable tasks: 40 up-to-date)
- **Aturan Pemrograman**:
  - Komentar wajib `[Jalur Class/Modul]` dan `[Penjelasan]` disertakan pada setiap blok perubahan.
  - SRP dan batasan baris (< 500 baris) tetap terjaga.
  - Zero mock/fake: seluruh shortcut beroperasi langsung di atas API Android asli (`LauncherApps`, `ShortcutInfo`, dan XML Resource Parser).

---

## 5. Audit Bug: Shortcut Gagal Dieksekusi (v2rayNG "Start Services" dll)
Berdasarkan keluhan dan screenshot yang menampilkan shortcut v2rayNG gagal dieksekusi, berikut adalah temuan akar masalahnya:

1. **Silauncer Bukan Default Launcher (Tidak Ada Host Permission)**: 
   Sistem operasi Android mengunci akses `LauncherApps.getShortcuts()` hanya untuk aplikasi yang disetel sebagai "Default Launcher". Karena Silauncer tidak berstatus default launcher, fungsi resmi tersebut gagal dieksekusi.

2. **Keterbatasan Metode Fallback Parsing XML**: 
   Karena API resmi diblokir, Silauncer melakukan *fallback* dengan membaca isi file `AndroidManifest.xml` dan `shortcuts.xml` dari APK target secara paksa (melalui `ShortcutFetcher.getManifestShortcutsFromXml`). Aplikasi kemudian membuat `directIntent` buatan.
   Sayangnya, sebagian besar target dari shortcut statis seperti *Start Services* atau *Stop Services* merupakan Service/Activity yang **TIDAK DIEKSPOR** (`android:exported="false"`). Fitur tersebut didesain hanya boleh dieksekusi oleh OS dan Default Launcher demi alasan keamanan.

3. **SecurityException & Penangkapan Senyap (Silent Catch)**:
   Saat Anda menekan tombol *Start Services* di Silauncer, aplikasi Silauncer berusaha memanggil `context.startActivity(directIntent)`. Karena target tidak diekspor, sistem Android akan langsung menolaknya dengan melempar `SecurityException`.
   Pada file `ShortcutLauncher.kt` (baris 37), error ini hanya ditangkap (*silent catch*) ke dalam sistem log Logcat tanpa memberikan umpan balik (misal: *Toast*) ke layar, sehingga tombol terlihat seperti tidak merespon/rusak.

**Rekomendasi Perbaikan:**
Untuk memperbaiki antarmuka UX tanpa membuat *hack* ilegal yang membahayakan device, saya merekomendasikan hal berikut:
1. **(Solusi UX Utama)** Menambahkan pesan `Toast` peringatan (*"Gagal meluncurkan shortcut. Jadikan Silauncer sebagai Default Launcher"*) di blok catch pada `ShortcutLauncher.kt` jika intent mengalami `SecurityException`.
2. Jika perlu, sistem dapat dimodifikasi untuk menyembunyikan daftar menu shortcut apabila Silauncer terdeteksi belum memiliki akses `hasShortcutHostPermission()`.
