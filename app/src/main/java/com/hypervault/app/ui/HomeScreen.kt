package com.hypervault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypervault.app.api.HyperliquidApi
import com.hypervault.app.api.VaultData
import com.hypervault.app.data.WidgetPreferences
import com.hypervault.app.ui.theme.BgDark
import com.hypervault.app.ui.theme.CardDark
import com.hypervault.app.ui.theme.GreenPos
import com.hypervault.app.ui.theme.LineBorder
import com.hypervault.app.ui.theme.LiveDot
import com.hypervault.app.ui.theme.TextMuted
import com.hypervault.app.ui.theme.TextPrimary
import com.hypervault.app.widget.VaultWidgetProvider
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val prefs = remember { WidgetPreferences(context) }

    var vaultAddress by remember { mutableStateOf(prefs.getGlobalVault().ifEmpty { "0xdfc24b077bc1425ad1dea75bcb6f8158e10df303" }) }
    var walletAddress by remember { mutableStateOf(prefs.getGlobalWallet().ifEmpty { "0x28974a706da39a3f2536c4b2a370b4ec74f63e69" }) }

    var vaultData by remember { mutableStateOf(VaultData()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun fetchSignal() {
        if (vaultAddress.isBlank() || walletAddress.isBlank()) {
            statusMessage = "Please fill in both Vault and Wallet addresses."
            return
        }
        isLoading = true
        statusMessage = "Connecting to Hyperliquid API..."
        scope.launch {
            vaultData = HyperliquidApi.fetchVaultDetails(vaultAddress, walletAddress)
            isLoading = false
            statusMessage = if (vaultData.error != null) {
                "Error: ${vaultData.error}"
            } else {
                "Signal connected! TVL: ${vaultData.formattedTvl()} | Return: ${vaultData.formattedMonthReturn()}"
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchSignal()
    }

    Scaffold(
        containerColor = BgDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(LiveDot)
                    )
                    Text(
                        text = "HL VAULT MONITOR",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardDark)
                        .border(1.dp, LineBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "5x3 WIDGET",
                        color = GreenPos,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Configure your vault monitor parameters to populate the 5x3 terminal widget.",
                color = TextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Form inputs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
                    .border(1.dp, LineBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "VAULT ADDRESS",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
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

                Text(
                    text = "WALLET ADDRESS",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
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

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            fetchSignal()
                            prefs.saveGlobalConfig(vaultAddress, walletAddress)
                            VaultWidgetProvider.updateAllWidgets(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPos),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = BgDark,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "TEST SIGNAL & SAVE",
                                color = BgDark,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = statusMessage,
                    color = if (vaultData.error != null) TextMuted else GreenPos,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Widget Preview Title
            Text(
                text = "LIVE WIDGET PREVIEW (5x3 TERMINAL)",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            WidgetPreviewCard(
                vaultData = vaultData,
                vaultAddress = vaultAddress,
                walletAddress = walletAddress
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Widget Setup Instructions Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
                    .border(1.dp, LineBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "HOW TO ADD 5x3 WIDGET TO HOME SCREEN",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "1. Long press an empty area on your Android Home Screen.\n" +
                            "2. Select 'Widgets' from the menu.\n" +
                            "3. Search for 'HL Vault Monitor'.\n" +
                            "4. Drag the 5x3 widget onto your Home Screen.\n" +
                            "5. Confirm your Vault and Wallet addresses.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
