package com.hypervault.app.data

import android.content.Context
import android.content.SharedPreferences

class WidgetPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveWidgetConfig(appWidgetId: Int, vaultAddress: String, walletAddress: String) {
        prefs.edit()
            .putString(KEY_VAULT_PREFIX + appWidgetId, vaultAddress.trim())
            .putString(KEY_WALLET_PREFIX + appWidgetId, walletAddress.trim())
            // Also store as global default
            .putString(KEY_GLOBAL_VAULT, vaultAddress.trim())
            .putString(KEY_GLOBAL_WALLET, walletAddress.trim())
            .apply()
    }

    fun getVaultAddress(appWidgetId: Int): String {
        val specific = prefs.getString(KEY_VAULT_PREFIX + appWidgetId, null)
        if (!specific.isNullOrBlank()) return specific
        val global = prefs.getString(KEY_GLOBAL_VAULT, null)
        if (!global.isNullOrBlank()) return global
        return DEFAULT_VAULT
    }

    fun getWalletAddress(appWidgetId: Int): String {
        val specific = prefs.getString(KEY_WALLET_PREFIX + appWidgetId, null)
        if (!specific.isNullOrBlank()) return specific
        val global = prefs.getString(KEY_GLOBAL_WALLET, null)
        if (!global.isNullOrBlank()) return global
        return DEFAULT_WALLET
    }

    fun saveGlobalConfig(vaultAddress: String, walletAddress: String) {
        prefs.edit()
            .putString(KEY_GLOBAL_VAULT, vaultAddress.trim())
            .putString(KEY_GLOBAL_WALLET, walletAddress.trim())
            .apply()
    }

    fun getGlobalVault(): String = prefs.getString(KEY_GLOBAL_VAULT, DEFAULT_VAULT) ?: DEFAULT_VAULT
    fun getGlobalWallet(): String = prefs.getString(KEY_GLOBAL_WALLET, DEFAULT_WALLET) ?: DEFAULT_WALLET

    companion object {
        private const val PREFS_NAME = "com.hypervault.app.widget_prefs"
        private const val KEY_VAULT_PREFIX = "vault_addr_"
        private const val KEY_WALLET_PREFIX = "wallet_addr_"
        private const val KEY_GLOBAL_VAULT = "global_vault_addr"
        private const val KEY_GLOBAL_WALLET = "global_wallet_addr"

        const val DEFAULT_VAULT = "0xdfc24b077bc1425ad1dea75bcb6f8158e10df303"
        const val DEFAULT_WALLET = "0x28974a706da39a3f2536c4b2a370b4ec74f63e69"
    }
}
