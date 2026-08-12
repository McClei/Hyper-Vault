package com.hypervault.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.hypervault.app.MainActivity
import com.hypervault.app.R
import com.hypervault.app.api.HyperliquidApi
import com.hypervault.app.api.VaultData
import com.hypervault.app.data.WidgetPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VaultWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val prefs = WidgetPreferences(context)
                val vaultAddr = prefs.getVaultAddress(appWidgetId)
                val walletAddr = prefs.getWalletAddress(appWidgetId)

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                if (vaultAddr.isBlank() || walletAddr.isBlank()) {
                    views.setTextViewText(R.id.txt_vault_name, "Config Required")
                    views.setTextViewText(R.id.val_tvl, "$0")
                    views.setTextViewText(R.id.val_month, "+0.00%")
                    views.setTextViewText(R.id.val_deposits, "$0.00")
                    views.setTextViewText(R.id.val_earned, "+$0.00")
                    views.setTextViewText(R.id.txt_wallet_short, "WALLET —")
                    views.setTextViewText(R.id.txt_vault_short, "VAULT —")
                    views.setTextViewText(R.id.txt_footer, "CONFIGURE IN APP TO START ⟳")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return
                }

                // Immediately set basic info synchronously so widget displays on screen instantly
                views.setTextViewText(R.id.txt_wallet_short, "WALLET " + VaultData.formatShortAddr(walletAddr))
                views.setTextViewText(R.id.txt_vault_short, "VAULT " + VaultData.formatShortAddr(vaultAddr))
                appWidgetManager.updateAppWidget(appWidgetId, views)

                // Async API fetch to populate live data
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val data = HyperliquidApi.fetchVaultDetails(vaultAddr, walletAddr)
                        val title = if (data.vaultName.isNotBlank()) data.vaultName else VaultData.formatShortAddr(vaultAddr)

                        views.setTextViewText(R.id.txt_vault_name, title)
                        views.setTextViewText(R.id.val_tvl, data.formattedTvl())
                        views.setTextViewText(R.id.val_month, data.formattedMonthReturn())
                        views.setTextColor(
                            R.id.val_month,
                            if (data.isMonthPositive()) Color.parseColor("#3FE081") else Color.parseColor("#FF5C5C")
                        )

                        views.setTextViewText(R.id.val_deposits, data.formattedDeposits())
                        views.setTextViewText(R.id.val_earned, data.formattedEarned())
                        views.setTextColor(
                            R.id.val_earned,
                            if (data.isEarnedPositive()) Color.parseColor("#3FE081") else Color.parseColor("#FF5C5C")
                        )

                        val timeStr = if (data.lastUpdated.isNotEmpty()) data.lastUpdated else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        views.setTextViewText(R.id.txt_footer, "UPDATED $timeStr · AUTO-REFRESH HOURLY ⟳")

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, VaultWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}

