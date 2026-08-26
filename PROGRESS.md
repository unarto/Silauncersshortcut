# Status Proyek Silauncer

## Sedang Dikerjakan / Selesai:
- [x] Eksekusi Perbaikan Kode audit1.md - FASE 2 (Pembersihan Mock Data, Dynamic Shortcuts AOSP, & Pengaturan Kustom UI) (SELESAI)
  - **Kategori 4 (Pembersihan Data Mock & Integrasi Real Dynamic Shortcuts AOSP)**:
    - Verifikasi dan pembersihan total mock data / data tiruan pada subsistem shortcuts & popup.
    - Integrasi penuh `LauncherApps.getShortcuts()`, `ShortcutKey`, `ShortcutConfigActivityInfo`, dan parsing manifest XML APK asli.
    - Menghilangkan pembatasan tunggal notifikasi pada `SmartPopupView.kt` dengan menambahkan indikator penghitung jumlah notifikasi dinamis (`+N lainnya`).
  - **Kategori 5 (Penghapusan Hardcoded Layout, Strings, & Dynamic Cell Bounds)**:
    - Menghapus hardcoded string pada `SettingsNodeFactory.kt` (menggunakan resource string `pref_grid_columns` dan `pref_grid_rows`).
    - Menstandarkan batas slider (min, max, step) menggunakan konstanta terpusat di `SettingsNodeFactory`.
    - Menghapus dimensi DP hardcoded pada `SmartPopupView.kt` dan menggantinya dengan referensi dimensi XML di `res/values/dimens.xml`.
    - Menghapus hardcoded Google Play URL pada `PopupController.kt` dengan `url_play_store_format` dari `strings.xml`.
    - Mengimplementasikan kalkulasi dinamis tinggi sel di `AppAdapter.kt`: `Tinggi Sel = maxOf(availableHeight / gridRows, Ukuran Ikon + Vertical Padding + Estimasi Tinggi Teks Label + Spacing)`.
    - Memastikan `clipToPadding="false"` dan `clipChildren="false"` aktif pada container `RecyclerView` dan item view.
    - Menambahkan panggilan `requestLayout()` dan `invalidate()` secara real-time pada `onResume()` `LauncherActivity` dan `updateConfig()` `AppAdapter`.
  - **Validasi Build & Unit Test**:
    - `compile_applet`: PASS (Build Succeeded).
    - `gradle :app:testDebugUnitTest`: 100% PASS (BUILD SUCCESSFUL, 34 tasks).
    - Seluruh file tetap di bawah batas wajib 500 baris.

- [x] Eksekusi Perbaikan Kode audit1.md - FASE 1 (Pembersihan Dead/Unused Code & Kode Duplikat) (SELESAI)
  - **Kategori 1 (Pembersihan Kode Mati / Dead Code)**:
    - Menghapus method stub kosong `buildRequest` dan method factory tak terjangkau `fromIntent` pada `ShortcutKey.kt`.
    - Menghapus komponen terisolasi `ShortcutPickerActivity.kt`, `ShortcutPickerAdapter.kt`, `activity_shortcut_picker.xml`, `item_shortcut_picker.xml`, serta menghapus deklarasi dari `AndroidManifest.xml`.
  - **Kategori 2 (Pembersihan Kode & Resource Tidak Digunakan)**:
    - Menghapus string resource tak terpakai dari `res/values/strings.xml` (`pref_grid_layout`, `pref_grid_layout_desc`, `action_app_storage`).
    - Merapikan struktur dan dokumentasi pemetaan pada `IconPackRepository.kt`.
  - **Kategori 3 (Refactor Kode Duplikat)**:
    - Membuat utilitas terpusat `com.silauncer.cepat.util.DensityExtensions.kt` untuk konversi DP/SP ke Pixel.
    - Menghapus duplikasi kalkulasi `dpToPx` lokal pada `LauncherActivity.kt` dan `PopupController.kt`.
    - Menyatukan alur ekstraksi ikon pada `SettingsNodeFactory.kt` agar konsisten menggunakan `IconCache` dan `IconPackRepository`.
    - Menyederhanakan dan menyatukan loop inspeksi metadata XML shortcuts pada `ShortcutFetcher.kt` (`extractShortcutXmlResId`).
  - **Validasi Build & Unit Test**:
    - `compile_applet`: PASS (Build Succeeded).
    - `gradle :app:testDebugUnitTest`: 100% PASS (BUILD SUCCESSFUL).
    - Batas 500 baris per file: Terpenuhi seluruhnya (semua file di bawah 500 baris).

- [x] Perbaikan UI Pengaturan (Ganti Dropdown Kaku ke Slider Kustom)
  - Slider telah diimplementasikan untuk Ukuran Ikon, Jarak Antar Ikon, Ukuran Teks Label, Kolom dan Baris.
- [x] Aturan Penyimpanan Data Strict (Zero Ambiguity)
  - **MMKV**: Menyimpan preferensi UI global seperti tema, icon pack, slider setting (ukuran icon, grid layout preferences).
  - **DB (Room)**: Menyimpan koordinat layout, struktur app grid, dan metadata aplikasi (cell_x, cell_y, page_index). Migration dan setup ksp/room telah diverifikasi dan bebas error.
- [x] Perbaikan Logika Render Grid:
  - cell_height sekarang adaptif.
  - Atribut `clipToPadding="false"` dan `clipChildren="false"` telah dipasang pada activity dan item_app untuk mencegah clipping (kepotong) saat user mendrag (melakukan scale up).
- [x] Real-time UI Updates:
  - Settings Activity dikelola secara terpisah, sehingga begitu user kembali dari screen pengaturan, onResume akan meload dan menerapkan update pengaturan konfigurasi grid & slider secara instan tanpa perlu restart service aplikasi secara penuh.
- [x] Validasi Build & Unit Test:
  - 100% PASS (`gradle :app:testDebugUnitTest` executed with SUCCESSFUL build status).

- [x] Perbaikan Popup Dynamic Shortcuts (ShortcutKey AOSP)
  - Mengimplementasikan `ShortcutKey` AOSP (identifier via componentName & UserHandle) untuk membaca dan membentuk Intent (dengan extra `EXTRA_SHORTCUT_ID` & category `com.android.launcher3.DEEP_SHORTCUT`).
  - Menghapus fallback mock/data tiruan pada `ShortcutFetcher.kt` yang sebelumnya membaca dari manifest XML fallback.
  - Memperbarui `ShortcutLauncher.kt` agar secara native mengeksekusi melalui `LauncherApps.startShortcut` via package name dan shortcut ID.
  - Menambahkan indikator panah dinamis (`arrow_up` & `arrow_down`) pada container Popup (`SmartPopupView`), secara presisi menunjuk X-coordinate dari ikon aplikasi.

- [x] Integrasi Pintasan Konfigurasi (ShortcutConfigActivityInfo AOSP)
  - Mengimplementasikan pembacaan pintasan konfigurasi menggunakan `LauncherApps.getShortcutConfigActivityList()` (API 26+).
  - Mengeksekusi pintasan dengan aman via `Intent.ACTION_CREATE_SHORTCUT` untuk pengguna utama.
  - Menangani eksekusi silang-profil (cross-profile) melalui `LauncherApps.getShortcutConfigActivityIntent()` dan dieksekusi dengan `startIntentSender`.
  - Menambahkan model parsing data baru (`parseConfig`) di `ShortcutParser.kt` agar terintegrasi mulus dengan Popup Controller.

- [x] Pembuatan UI ShortcutPickerActivity
  - Menggunakan RecyclerView untuk merender pintasan aplikasi khusus (Config Shortcuts) secara clean.
  - Melakukan query multi-profil (Iterasi UserManager & LauncherApps).
  - Menampilkan ikon dan label menggunakan model parsing tanpa mock data.
  - Mempertahankan fitur drag and drop / grid dan memverifikasi aplikasi lulus build.

- [x] Perbaikan Tata Letak Dinamis Smart Popup & Ekstraksi Pintasan Manifest Asli (SELESAI)
  - Mengimplementasikan adaptasi cerdas untuk System Actions dan Dynamic Shortcuts sesuai screenshot referensi:
    - **Single Action Pill**: Format compact vertical jika berdiri sendiri tanpa shortcut (Screenshot 2 OKX Wallet), atau format horizontal selebar kartu shortcut jika bersama shortcut (Screenshot 4 YouTube).
    - **Multi Action Card Grid**: 3 tombol horizontal (`Info aplikasi`, `Hapus`, `Bagikan`) untuk aplikasi terpasang (Screenshot 1 Keep & Screenshot 3 ShopeePay).
    - **Dynamic Shortcuts List**: Mengekstrak daftar pintasan nyata dari `LauncherApps` (Dynamic/Pinned/Manifest) dan manifest APK asli `shortcuts.xml` (seperti YouTube: Shorts, Telusuri, Subscription; ShopeePay: Bayar QRIS, QRIS Tap, Isi Saldo, Kirim Uang).
    - **Smart Card Reordering**: Kartu single action ditempatkan di atas daftar shortcuts (Screenshot 4 YouTube), sedangkan kartu multi-action ditempatkan di bawah daftar shortcuts terdekat dengan ikon aplikasi (Screenshot 3 ShopeePay).
    - **Panah Indikator Presisi**: Mengarahkan ujung panah ke koordinat pusat ikon aplikasi dengan clamping tepi kartu.
  - Validasi build: PASS.

## Tertunda:
- Tidak ada tertunda. Pekerjaan yang direquest user telah selesai dan tervalidasi.
