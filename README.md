# 📊 InvestTrack — Full Investment & Loan Tracker Android App

A production-grade Android application to track all your investments and loans for your entire family.

---

## ✨ Features

### 👨‍👩‍👧 Family Management
- Add unlimited family members (Self, Spouse, Children, Parents, etc.)
- Capture PAN, Aadhaar, DOB, Phone, Email
- **Nominee management** — Add multiple nominees per member with % allocation, minor guardian support

### 📂 Security Master
Dynamic security creation with **type-specific fields**:

| Type | Fields |
|------|--------|
| **Mutual Fund** | Scheme Code, Name, AMC, ISIN, Scheme Type (ELSS, Large Cap, etc.), Asset Class |
| **Shares** | Symbol, Name, Asset Class |
| **Bonds / GOI** | Code, Name, Face Value, Coupon Rate, Coupon Frequency, First Coupon Date, Maturity |
| **NPS** | PRAN, Fund Manager, Tier |
| **PF** | UAN, Account Number |
| **FD** | Interest Rate, Tenure, Maturity Date |
| **Insurance** | Policy No., Insurer, Sum Assured, Policy Term, Premium Term, Frequency |
| **Property** | Address, Property Type |
| **Gold / Others** | Code, Name |

### 💰 Transactions
- **Single security, multiple dates** — Buying Axis ELSS on 1/1/2020 and again on 1/1/2021 creates one holding with cumulative units
- Type-aware transaction forms:
  - Units-based: Shares, MF, Bonds (BUY, SELL, SIP, SWP, DIVIDEND, COUPON, etc.)
  - Amount-based: NPS, PF, FD, Insurance (INVEST, REDEEM, PREMIUM, MATURITY)
- Captures brokerage, STT, stamp duty, folio number, notes
- Coupon entries with auto-generated dates from Bond master data

### 📈 Price / NAV Management
- Update price/NAV for any security on any date
- History view with delete option
- Accessible from Holdings screen with quick shortcut

### 🏦 Dashboard
- **Total Portfolio Value**, Invested Cost, Gain/Loss
- **XIRR**, Absolute Return, CAGR
- Asset class breakdown with visual progress bars
- Security type category cards (clickable)
- Recent transactions list
- Loan outstanding + Monthly EMI summary

### 📋 Holdings Screen
- All holdings with: Units, Avg Cost, Current Price, Market Value, Gain/Loss, Abs Return, XIRR, CAGR
- Filter by security type or asset class
- Click to drill into holding detail with transaction history

### 🏠 Loan Management
- Home Loan, Car Loan, Personal Loan, Education Loan, Gold Loan, Business Loan
- Auto-calculate EMI using standard formula: `P × r × (1+r)ⁿ / ((1+r)ⁿ - 1)`
- **Editable EMI** — adjust to match actual bank EMI
- Full amortisation schedule — principal, interest, balance per installment
- Mark individual EMIs as paid
- Track: outstanding principal, paid installments, total interest paid, remaining EMIs
- Visual repayment progress bar

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository |
| DI | Hilt (Dagger) |
| Database | Room (SQLite) |
| Async | Coroutines + Flow |
| Navigation | Compose Navigation |
| Min SDK | 26 (Android 8.0) |

---

## 🚀 Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Gradle 8.2+

### Steps
1. **Open Project**
   ```
   File → Open → Select InvestTrack folder
   ```

2. **Sync Gradle**
   - Android Studio will auto-sync. If not: `File → Sync Project with Gradle Files`

3. **Run**
   - Select device/emulator (API 26+)
   - Click ▶️ Run

### First Time Use
1. Go to **Family Members** (via nav drawer or settings) → Add yourself as "SELF"
2. Go to **Security Master** → Create your securities (e.g., Axis ELSS - Mutual Fund)
3. Go to **Transactions** → Add your purchases
4. Go to **Price / NAV** → Enter current NAV/price
5. Dashboard will now show your portfolio summary

---

## ✅ Build APK on GitHub (no Android Studio needed)

If you don’t have Android SDK/Android Studio locally, you can build the APK using GitHub Actions.

### How to download the APK
1. Push to `main` or `develop` (or open a Pull Request to `main`)
2. In GitHub → **Actions** → open the latest run named **Android CI/CD**
3. Scroll to **Artifacts** → download `investtrack-debug-<run_number>`
4. Inside the downloaded zip, the APK is in `app/build/outputs/apk/debug/`

### Notes
- GitHub Actions installs **JDK 17** + **Android SDK (API 34)** automatically.
- This produces a **debug APK**.

---

## 🔐 Build a Signed Release APK on GitHub

This creates a **signed Release APK** (better for sharing/installing than debug).

### 1) Create a keystore (one-time)
On your Windows PC, install any Java (JDK 17 recommended). Then run:

```bash
keytool -genkeypair -v -keystore investtrack-release.jks -alias investtrack -keyalg RSA -keysize 2048 -validity 10000
```

It will ask for:
- keystore password
- key password
- your name/organization (can be anything)

Keep this `.jks` file safe. **If you lose it, you can’t update the app with the same signature.**

### 2) Add GitHub Secrets (one-time)
GitHub repo → **Settings → Secrets and variables → Actions → New repository secret**

Create these secrets:
- **`ANDROID_KEYSTORE_BASE64`**: base64 of the `investtrack-release.jks` file
- **`ANDROID_KEYSTORE_PASSWORD`**
- **`ANDROID_KEY_ALIAS`** (example: `investtrack`)
- **`ANDROID_KEY_PASSWORD`**

To get base64 on Windows PowerShell:

```powershell
$bytes = [System.IO.File]::ReadAllBytes("investtrack-release.jks")
[Convert]::ToBase64String($bytes) | Set-Clipboard
```

Paste (from clipboard) into the `ANDROID_KEYSTORE_BASE64` secret.

### 3) Build the signed APK
GitHub → **Actions** → workflow **Signed Release APK** → **Run workflow**

Download artifact `investtrack-release-<run_number>` → APK will be in:
`app/build/outputs/apk/release/`

---

## 🗃️ Database Schema

```
family_members ──< nominees
family_members ──< transactions >── security_master
family_members ──< sip_plans >── security_master
security_master ──< price_history
family_members ──< loans ──< loan_payments
```

---

## 📐 Financial Calculations

### XIRR
Newton-Raphson iterative method on dated cashflows:
- Outflows: Buy/SIP/Invest transactions (negative)  
- Inflows: Sell/Dividend/Coupon + Current Market Value (positive)

### EMI
```
EMI = P × r × (1+r)^n / ((1+r)^n - 1)
r = annual_rate / 1200
```

### CAGR
```
CAGR = (MV/Cost)^(1/years) - 1
```

### Amortisation
Month-by-month schedule computing:
- Interest = Outstanding Balance × monthly_rate
- Principal = EMI - Interest
- New Balance = Previous Balance - Principal

---

## 📁 Project Structure

```
com.investtrack/
├── data/
│   ├── database/
│   │   ├── entities/     # All Room entities + enums
│   │   ├── dao/          # Data Access Objects  
│   │   ├── AppDatabase   # Room database config
│   │   └── Converters    # Type converters
│   └── repository/       # Business logic + portfolio calculations
├── di/                   # Hilt dependency injection module
├── navigation/           # NavHost + Screen routes
├── ui/
│   ├── dashboard/        # Portfolio dashboard
│   ├── family/           # Family + nominee management
│   ├── security/         # Security master CRUD
│   ├── transaction/      # Transaction list + add form
│   ├── price/            # Price/NAV update
│   ├── loan/             # Loan management + amortisation
│   ├── holdings/         # Portfolio holdings + detail
│   ├── common/           # Reusable components
│   └── theme/            # Material3 color scheme
├── utils/
│   ├── FinancialUtils    # XIRR, EMI, CAGR, formatters
│   └── DateUtils         # Date helpers
├── InvestTrackApp.kt     # Hilt app
└── MainActivity.kt       # Bottom nav + entry point
```

---

## 🔮 Roadmap / Enhancements
- [ ] SIP plan auto-reminder notifications
- [ ] CSV / Excel export of portfolio
- [ ] Mutual Fund NAV auto-fetch via AMFI API
- [ ] NSE/BSE price auto-fetch
- [ ] Tax P&L report (STCG/LTCG)
- [ ] Goal tracking (Retirement, Education, etc.)
- [ ] Backup & restore to Google Drive
- [ ] Biometric lock screen

---

*Built with ❤️ using Jetpack Compose + Room + Hilt*
