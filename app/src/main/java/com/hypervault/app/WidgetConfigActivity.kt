package com.hypervault.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypervault.app.api.HyperliquidApi
import com.hypervault.app.api.VaultData
import com.hypervault.app.data.WidgetPreferences
import com.hypervault.app.ui.WidgetPreviewCard
import com.hypervault.app.ui.theme.BgDark
import com.hypervault.app.ui.theme.GreenPos
import com.hypervault.app.ui.theme.HLVaultMonitorTheme
import com.hypervault.app.ui.theme.LineBorder
import com.hypervault.app.ui.theme.TextMuted
import com.hypervault.app.ui.theme.TextPrimary
import com.hypervault.app.widget.VaultWidgetProvider
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        intent?.extras?.let {
            appWidgetId = it.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            HLVaultMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    WidgetConfigScreen(
                        appWidgetId = appWidgetId,
                        onSaveConfig = { vault, wallet ->
                            val prefs = WidgetPreferences(this)
                            prefs.saveWidgetConfig(appWidgetId, vault, wallet)

                            val appWidgetManager = AppWidgetManager.getInstance(this)
                            com.hypervault.app.widget.VaultWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

                            val resultValue = Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            setResult(Activity.RESULT_OK, resultValue)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetConfigScreen(
    appWidgetId: Int,
    onSaveConfig: (vault: String, wallet: String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { WidgetPreferences(context) }

    var vaultAddress by remember { mutableStateOf(prefs.getVaultAddress(appWidgetId)) }
    var walletAddress by remember { mutableStateOf(prefs.getWalletAddress(appWidgetId)) }

    // Pre-fill defaults if empty for convenient testing
    if (vaultAddress.isBlank()) {
        vaultAddress = "0xdfc24b077bc1425ad1dea75bcb6f8158e10df303" // Standard Hyperliquid vault address
    }
    if (walletAddress.isBlank()) {
        walletAddress = "0x28974a706da39a3f2536c4b2a370b4ec74f63e69" // Example wallet address
    }

    var vaultData by remember { mutableStateOf(VaultData()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refreshData() {
        if (vaultAddress.isNotBlank() && walletAddress.isNotBlank()) {
            isLoading = true
            scope.launch {
                vaultData = HyperliquidApi.fetchVaultDetails(vaultAddress, walletAddress)
                isLoading = false
            }
        }
    }

    LaunchedEffect(vaultAddress, walletAddress) {
        refreshData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "CONFIGURE WIDGET #$appWidgetId",
            color = GreenPos,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Enter Vault and Wallet addresses to display in your Home Screen widget.",
            color = TextMuted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Input 1: Vault Address
        Text(
            text = "VAULT ADDRESS",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = vaultAddress,
            onValueChange = { vaultAddress = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("0x…", color = TextMuted) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPos,
                unfocusedBorderColor = LineBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Input 2: Wallet Address
        Text(
            text = "WALLET ADDRESS",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = walletAddress,
            onValueChange = { walletAddress = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("0x…", color = TextMuted) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPos,
                unfocusedBorderColor = LineBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Live Preview Title
        Text(
            text = "LIVE WIDGET PREVIEW",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        WidgetPreviewCard(
            vaultData = vaultData,
            vaultAddress = vaultAddress,
            walletAddress = walletAddress
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    onSaveConfig(vaultAddress, walletAddress)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPos),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SAVE & ADD WIDGET",
                    color = BgDark,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
        }
    }
}
