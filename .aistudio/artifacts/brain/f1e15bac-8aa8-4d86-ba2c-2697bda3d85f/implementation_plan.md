# Factory Dispatch & Return System (Supabase Integration)

Integrate the factory operations system with a Supabase cloud database backend, introducing dual operating modes—**Dispatch Mode** and **Return Mode**—toggled via a color-coded top segmented button, with support for QR scanning and manual order number returns.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following product decisions were confirmed during clarification:
> - **Backend Database**: Supabase REST API (with PostgREST) to serve as the single source of truth for the factory system orders and dispatch/return records.
> - **Return Workflow**: Scan QR or manually type an Order Number to mark the item as returned and restocked in Supabase and local cache.
> - **Mode Switcher**: Segmented toggle button placed prominently at the top with distinct accent colors for Dispatch Mode (Industrial Indigo) and Return Mode (Amber/Emerald Restock).

- **Confirmed Decision 1**: Mode switching will immediately adapt the scanner viewfinder accent, action buttons, status labels, and history views.
- **Confirmed Decision 2**: Return mode includes both a Camera QR scanner and a dedicated "Enter Order Number" text input field for manual return entry when labels are damaged.
- **Open Recommendation**: Supabase credentials (`SUPABASE_URL` and `SUPABASE_KEY`) will default to `.env` / `BuildConfig` with fallback editable fields in Settings for on-the-fly factory floor adjustments.

---

### 1. Overview & Core Concept

- **What It Does**: Allows factory operators to seamlessly switch between dispatching finished goods and processing returned items. Operators can scan QR tags or enter order numbers to immediately update the central Supabase database (`status = "dispatched"` or `status = "returned" / "restocked"`).
- **Target Audience / Persona**: Factory dispatchers, warehouse logistics coordinators, and inventory restock handlers operating in fast-paced production environments.
- **Key Value**: Eliminates manual spreadsheets and discrepancies between warehouse dispatch and customer returns by syncing directly with the Supabase factory system.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Mode Switching**: Operator taps the top segmented button to toggle between **Dispatch Mode** (Indigo/Blue) and **Return Mode** (Warm Amber/Emerald). The UI headers, viewfinder corner guides, and action badges adapt instantly.
2. **Dispatch Scanning**: In Dispatch Mode, the operator scans production QR codes. The app verifies authorized copies, detects duplicates, updates the local log, and patches the order in Supabase to dispatched status.
3. **Return Scanning & Restocking**: In Return Mode, the operator scans the returned unit's QR code. The app retrieves the order, marks it as `returned`, updates restock status to `restocked`, and logs the return timestamp and operator identity.
4. **Manual Order Return**: If a QR label is torn or unreadable, the operator taps the "Manual Order Return" input, enters the Order Number (e.g., `ORD-2024-001`), and taps "Process Return".
5. **Mode-Specific Filtered History**: History screen shows segmented tabs or filters to inspect Dispatch Logs vs. Return & Restock Logs.

#### Visual Identity & Theme
- **Color Palette & Accents**:
  - *Dispatch Mode Accent*: Deep Electric Indigo (`#4F46E5`), Slate Surface (`#0F172A`), Cyan Highlights (`#06B6D4`).
  - *Return Mode Accent*: High-Visibility Amber/Gold (`#D97706` / `#F59E0B`) with Emerald Restock confirmation (`#10B981`).
  - *Base Theme*: Material 3 dark/light elevation surfaces with high contrast for industrial lighting.
- **Top Segmented Switcher**:
  - Pill-shaped container with smooth animated sliding indicator (`spring(dampingRatio = 0.8f)`).
  - High-contrast icons: `Icons.Default.LocalShipping` (Dispatch) and `Icons.Default.AssignmentReturn` (Return).
- **Scanner Viewfinder**: Dynamic reticle corner strokes adapting to active mode color.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Supabase Direct REST API vs. Webhook Proxy**
  - *Chosen Approach*: Direct Retrofit calls to Supabase PostgREST endpoints (`/rest/v1/orders`) with bearer apikey headers, alongside local Room caching.
  - *Why*: Provides sub-second real-time order updates, atomic patch operations, and offline queue resilience.
  - *Alternatives Considered*: Google Sheets webhooks (maintained as secondary backup sync for historical audits).

- **Decision 2: Dual Entry in Return Mode (QR + Manual Input)**
  - *Chosen Approach*: Dedicated persistent barcode viewfinder paired with an expandable or bottom-anchored "Manual Order #" quick-entry card.
  - *Why*: Damaged packaging and returned goods frequently have smudged or scratched QR codes. Manual order number typing prevents warehouse bottlenecks.

- **Decision 3: Unified ScannedRecord Schema with `operationMode`**
  - *Chosen Approach*: Add `operationMode` ("DISPATCH" vs "RETURN") and `returnStatus` to Room database and Supabase payload.
  - *Why*: Allows one cohesive database and history list while providing clean filtering and badge distinction.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Top Segmented Mode Switcher                     │
│               [  📦 Dispatch Mode  ]   [  🔄 Return Mode  ]            │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                  ┌─────────────────┴─────────────────┐
                  ▼                                   ▼
      ┌───────────────────────┐           ┌───────────────────────┐
      │     Dispatch Flow     │           │   Return & Restock    │
      │ • Camera QR Scanner   │           │ • Camera QR Scanner   │
      │ • Copy authorization  │           │ • Manual Order Entry  │
      │ • Duplicate prevention│           │ • Restock Flagging    │
      └───────────┬───────────┘           └───────────┬───────────┘
                  │                                   │
                  └─────────────────┬─────────────────┘
                                    ▼
                      ┌───────────────────────────┐
                      │    DispatchViewModel      │
                      │  • currentMode (StateFlow)│
                      │  • submitReturnOrder()    │
                      │  • syncWithSupabase()     │
                      └─────────────┬─────────────┘
                                    ▼
                      ┌───────────────────────────┐
                      │    DispatchRepository     │
                      └───────┬───────────┬───────┘
                              │           │
              ┌───────────────┘           └───────────────┐
              ▼                                           ▼
  ┌───────────────────────┐                   ┌───────────────────────┐
  │  Local Room Database  │                   │    Supabase PostgREST │
  │  • ScannedRecordDao   │                   │  • PATCH /orders      │
  │  • mode & restock cols│                   │  • GET /orders?orderNo│
  └───────────────────────┘                   └───────────────────────┘
```

#### Data Entities & API Endpoints
1. **Supabase Orders Table Patch**:
   - `order_form_number`: eq.<orderNo>
   - Dispatch Body: `{ "status": "dispatched", "delivery_status": "in_transit", "dispatched_at": "<ISO>", "dispatched_by": "<scannerName>" }`
   - Return Body: `{ "status": "returned", "delivery_status": "returned_to_factory", "restocked": true, "returned_at": "<ISO>", "returned_by": "<scannerName>" }`
2. **Local Room `ScannedRecord` Extensions**:
   - `mode`: `"DISPATCH"` | `"RETURN"`
   - `returnStatus`: `"RESTOCKED"` | `"PENDING_INSPECTION"`
3. **UI State Mapping**:
   - `currentMode`: `OperationMode.DISPATCH` vs `OperationMode.RETURN`
   - `manualOrderInput`: `String` with clear and submit triggers
