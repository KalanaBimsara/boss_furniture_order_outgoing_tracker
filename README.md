# Boss Furniture QR Scanner — Factory Dispatch & Return App

An industrial-grade, offline-first Android application built with **Kotlin** and **Jetpack Compose** for **Boss Furniture** factory floor workers to scan multi-copy order QR labels, assign delivery vehicles/drivers, manage customer returns and inventory restocking, and synchronize transport records in real time with **Supabase PostgreSQL**.

---

## Key Features

### 1. Dual Operational Modes (Dispatch & Return)
- **Dispatch Mode (Indigo `#4F46E5`)**: Scans outgoing factory furniture orders as they are loaded onto delivery vehicles.
- **Return Mode (Amber `#D97706`)**: Logs incoming returned items from customers back to the factory floor.
- **Restock to Inventory Toggle**: Available in Return Mode (on both the main screen and the scan confirmation sheet) to mark whether returned units should be added back to available stock (`restocked = true`).

### 2. Maximized Live CameraX + ML Kit QR Scanner
- **Expanded `360.dp` Camera Viewport**: Tall, wide camera window with a `230.dp` animated target reticle so workers can quickly align QR labels at arm's length without precision aiming.
- **Real-Time ML Kit Barcode Decoding**: Instant recognition of Boss Furniture multi-line QR payloads and JSON order labels.
- **Hardware Torch Toggle**: Built-in flashlight button inside the camera viewport for low-light loading bays.
- **Multi-Sensory Scan Feedback**: Immediate haptic vibration + custom synthesized audio tones (success double-beep, duplicate error buzz, and countdown ticks).

### 3. 5-Second Auto-Confirm Bottom Sheet
- Every valid scan opens a slide-up confirmation sheet with a **5-second linear draining button and progress ring**.
- Displays full order context before committing:
  - **Order Number** (e.g., `#1024`) and **Copy Badge** (`GATE PASS`, `ACCOUNT COPY`, `TRANSPORT COPY`, `PRODUCTION COPY`)
  - **Exact Scan Date & Time** in **Sri Lanka Standard Time (`Asia/Colombo`, UTC+05:30)**
  - **Customer Name** and **Total Table Units / Breakdown**
  - **Assigned Vehicle / Driver** (or `Unchanged` for non-Gate Pass copies)
  - **Restock Status** (in Return mode)
- Workers can tap **Confirm** immediately, let the 5-second timer auto-submit hands-free, or tap **Cancel** to abort a mistaken scan.

### 4. Vehicle & Driver Assignment
Available in both **Dispatch** and **Return** modes via the Vehicle Picker sheet:
- **`DAC-4912 (Rathnayake)`**: Company truck mapped to driver profile in Supabase (`public.profiles`).
- **`DAE-4217 (Lasantha)`**: Company truck mapped to driver profile in Supabase (`public.profiles`).
- **`Pick Me`**: Third-party courier option with a dedicated **Plate Number** input field (e.g., `CAA-8910`).
- **Custom Vehicles (`+ Add custom vehicle`)**: Add and persist custom fleet vehicles with vehicle number and optional driver name.

### 5. Strict 4-Copy Tracking & Gate Pass Driver Assignment Rule
Boss Furniture prints 4 physical copies per order (`GATE PASS`, `ACCOUNT COPY`, `TRANSPORT COPY`, `PRODUCTION COPY`):
- **Copy Type Auto-Detection**: The copy type is automatically parsed and normalized directly from the scanned QR code (`copy_name`).
- **Strict `GATE PASS` Rule**:
  - **Only `GATE PASS`** updates `public.orders` in Supabase (`status = "assigned"`, `delivery_status = "assigned"`, and `delivery_person_id` for company drivers or `delivery_type` for Pick Me / custom vehicles).
  - The app **never overwrites `note`** or any unrelated columns on `public.orders`.
  - Scanning `ACCOUNT COPY`, `TRANSPORT COPY`, or `PRODUCTION COPY` records the scan in `public.transport` without altering driver assignment on `public.orders`.
- **Per-Station Copy Authorization**: In Settings, an admin can configure which of the 4 copies a specific phone/station is allowed to scan. Scanning an unauthorized copy triggers a 420ms horizontal shake animation, red warning card, and error tone.

### 6. Strict Local + Cloud Duplicate Prevention
- Each `(order_id, copy_name)` combination can be **dispatched once** (`transport_status = "dispatched"`) and **returned once** (`transport_status = "returned"`).
- Checked against both the local Room SQLite database and Supabase `public.transport` (`order_id + copy_name + transport_status`) before recording.
- Duplicate scans are immediately blocked with a red **Duplicate Denied** alert card showing the original scan timestamp and station.

### 7. Manual Order Number Entry Fallback
- Dedicated **Manual Entry** card on the Scanner tab for torn, smudged, or damaged QR labels.
- Supports both **Dispatch** and **Return** modes (including the **Restock to inventory** switch in Return mode).
- Manual entries are recorded with `copy_name = "GATE PASS"` and follow the same duplicate prevention and vehicle assignment rules.

### 8. Offline-First Queue & Auto-Sync (`Pending` Tab)
- Built on **Room SQLite** so factory workers never lose scans when Wi-Fi or mobile data drops in the loading bay.
- **Automatic Background Sync**: Queued scans automatically flush to Supabase as soon as network connectivity returns.
- **Swipe Actions**: Swipe left on any pending row to manually **Retry** or **Delete** it, or tap **Sync All**.
- **Last Sync Summary Card**: 4-column breakdown showing `Total`, `Synced`, `Duplicate`, and `Failed` counts.

### 9. Today's Audit Log & Record Detail Inspector (`History` Tab)
- Lists all scans recorded today (bounded by **Sri Lanka Standard Time `Asia/Colombo`** midnight-to-midnight).
- **Real-Time Search & Filtering**: Filter by `All`, `Dispatches`, or `Returns`, and search across Order #, Customer Name, District, Vehicle, or Copy Type.
- **Clear & Sync**: Re-fetches and reconciles today's records directly from Supabase `public.transport` and `public.orders`.
- **Record Detail View**: Tap any scan row to inspect its full metadata, Sri Lanka timestamp, sync state, and copy the raw QR payload to the clipboard.

### 10. Sri Lanka Standard Time (`Asia/Colombo`) Standardization
- Every scan timestamp across the Confirmation Sheet, History tab, Pending Queue, Record Detail screen, and Supabase `loaded_at` / `updated_at` ISO-8601 fields (`+05:30`) is locked to **Sri Lanka Time (`Asia/Colombo`)**, regardless of the phone's local timezone setting.

### 11. Admin-Locked Settings (`Settings` Tab)
- Protected by an Admin Password lock screen to prevent accidental changes on the factory floor.
- Pre-configured with the Boss Furniture Supabase project URL and publishable key, with support for **Test Connection**, **Station Name** configuration (e.g., `Station 01`), **Authorized Copy Switches**, and unique **Android Device ID** stamping.

---

## Supabase Database Integration

The app communicates with three tables in the `public` schema via the Supabase PostgREST API:
1. **`public.orders`** — Looks up order UUIDs by `order_form_number` and updates driver assignment (`delivery_person_id` / `delivery_type`, `status`, `delivery_status`, `updated_at`) strictly when a `GATE PASS` is dispatched or returned.
2. **`public.transport`** — Inserts a transport audit log row for every valid scan (`order_id`, `vehicle_number`, `driver_id`, `loaded_at`, `copy_name`, `transport_status`) and enforces duplicate prevention across stations.
3. **`public.profiles`** — Links company vehicle drivers (`DAC-4912 Rathnayake` and `DAE-4217 Lasantha`) to their respective Supabase profile UUIDs.

---

## Releasing the APK to GitHub

This repository includes an automated **GitHub Actions** workflow (`.github/workflows/release-apk.yml`) that compiles both **Release** and **Debug** APKs and publishes them directly to **GitHub Releases**.

### Option 1: Push to GitHub (Automatic `latest` Release)
Whenever you push to the `main` or `master` branch, GitHub Actions will automatically build the APKs and publish/update the **`latest`** release under your repository's **Releases** tab:
```bash
git add .
git commit -m "Update Boss Furniture QR Scanner"
git push origin main
```

### Option 2: Create a Versioned Release Tag (Recommended for Production)
Push a version tag (e.g., `v1.0.0`) to create a permanent versioned GitHub Release with `BossFurniture-Scanner-release.apk` and `BossFurniture-Scanner-debug.apk` attached:
```bash
git tag v1.0.0
git push origin v1.0.0
```

### Option 3: Manual 1-Click Release from GitHub UI
1. Open your repository on **GitHub**.
2. Go to the **Actions** tab -> select **Build & Release Android APK**.
3. Click **Run workflow**, enter a tag name (e.g., `v1.0.0`), and click **Run workflow**.
4. Once finished, download `BossFurniture-Scanner-release.apk` from the **Releases** page on your repo.

---

## Building the APK Locally

**Prerequisites:** JDK 17+ and Android Studio (or Android SDK Command-Line Tools).

1. Clone the repository and open it in **Android Studio**.
2. To build the debug APK from the terminal:
   ```bash
   gradle assembleDebug
   ```
   The signed debug APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`
3. You can also export the APK/AAB or push directly to GitHub from the **Google AI Studio** settings menu.
