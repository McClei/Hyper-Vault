package com.hypervault.app.api

import java.text.NumberFormat
import java.util.Locale

data class VaultData(
    val tvl: Double = 0.0,
    val yourDeposits: Double = 0.0,
    val allTimeEarned: Double = 0.0,
    val pastMonthReturn: Double = 0.0,
    val vaultName: String = "",
    val error: String? = null,
    val lastUpdated: String = ""
) {
    fun formattedTvl(): String {
        val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        return "$" + fmt.format(kotlin.math.abs(tvl))
    }

    fun formattedDeposits(): String {
        val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
        return "$" + fmt.format(kotlin.math.abs(yourDeposits))
    }

    fun formattedEarned(): String {
        val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
        val sign = if (allTimeEarned < 0) "−" else "+"
        return "$sign$" + fmt.format(kotlin.math.abs(allTimeEarned))
    }

    fun formattedMonthReturn(): String {
        val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
        val sign = if (pastMonthReturn < 0) "−" else "+"
        return "$sign" + fmt.format(kotlin.math.abs(pastMonthReturn)) + "%"
    }

    fun isMonthPositive(): Boolean = pastMonthReturn >= 0

    fun isEarnedPositive(): Boolean = allTimeEarned >= 0

    companion object {
        fun formatShortAddr(address: String): String {
            val clean = address.trim()
            return if (clean.length > 13) {
                clean.take(6) + "…" + clean.takeLast(4)
            } else if (clean.isEmpty()) {
                "—"
            } else {
                clean
            }
        }
    }
}
