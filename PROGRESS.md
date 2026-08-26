# Status Proyek Silauncer

## Sedang Dikerjakan / Selesai:
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
