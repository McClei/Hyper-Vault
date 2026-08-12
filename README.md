# Vault Monitor (Hyperliquid Vault Tracker)

A clean, high-contrast, terminal-styled Android Home Screen Widget for monitoring **Hyperliquid** vaults and personal positions in real time.

---

### 📲 **[📥 Download Latest Release APK](https://github.com/McClei/Hyper-Vault/actions/runs/31578904429/artifacts/9134355779)**

> **Note**: Click the link above to download the pre-compiled, signed Android Release APK directly from GitHub Actions.

---

## 📌 Overview

**Vault Monitor** allows Hyperliquid traders and liquidity providers to track vault performance and personal position metrics directly from their Android Home Screen without needing to open a browser or log in. 

It fetches on-chain vault state and user relationship data directly from Hyperliquid's official L1 REST API endpoints.

---

## ✨ Features

- **Home Screen Widget**: Compact, high-density terminal layout presenting key metrics at a glance.
- **Real-Time Vault Metrics**:
  - **Vault Name & Addresses**: Formatted short-addresses for both Vault and Wallet.
  - **TVL (Total Value Locked)**: Total capital held in the vault.
  - **30-Day Return (%)**: Monthly ROI color-coded green for profit and red for loss.
  - **User Deposits**: Total funds deposited into the vault by your wallet.
  - **User Earned / PnL**: Net accumulated earnings/profit from the vault position.
- **Live In-App Preview**: Configure Vault and Wallet addresses inside the app with an instant visual simulation before placing the widget.
- **No API Keys Required**: Uses Hyperliquid's public, read-only L1 info API.
- **Automated CI/CD**: Automatic GitHub Actions workflow that builds and signs production Release APKs.

---

## 🛠️ How It Works

1. **Address Configuration**: Enter a **Vault Address** (0x...) and your **Wallet Address** (0x...) inside the app or widget configuration screen.
2. **Hyperliquid REST API Queries**:
   - Fetches vault state details via Hyperliquid's `POST /info` API (`vaultDetails`).
   - Retrieves user account relationship metrics, net deposits, equity, and all-time PnL.
3. **Background Sync**: The widget updates on startup, upon manual configuration, and automatically refreshes in the background hourly.

---

## 📱 How to Add the Widget to Your Home Screen

1. Install the APK on your Android device.
2. Open the **Vault Monitor** app to enter your **Vault Address** and **Wallet Address** (or save them for default widget initialization).
3. Long-press an empty space on your Android Home Screen.
4. Tap **Widgets** and search for **Vault Monitor**.
5. Drag and drop the **Vault Monitor** widget onto your Home Screen.
6. The widget will instantly fetch live data from Hyperliquid and stay updated automatically.

---

## ⚡ Tech Stack

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose (In-App) & RemoteViews (AppWidget)
- **Networking**: Kotlin Coroutines, Flow & `java.net.HttpURLConnection` / GSON
- **Architecture**: MVVM, Clean Architecture
- **CI/CD**: GitHub Actions (Automatic Release APK compilation & `apksigner` signing)

---

## 📜 License

This project is open-source and available under the [MIT License](LICENSE).
