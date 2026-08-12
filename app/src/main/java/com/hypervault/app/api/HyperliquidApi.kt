package com.hypervault.app.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

object HyperliquidApi {
    private const val API_URL = "https://api.hyperliquid.xyz/info"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun fetchVaultDetails(vaultAddress: String, userAddress: String): VaultData = withContext(Dispatchers.IO) {
        val cleanVault = vaultAddress.trim().trim('\'', '"')
        val cleanUser = userAddress.trim().trim('\'', '"')

        if (cleanVault.isEmpty() || cleanUser.isEmpty()) {
            return@withContext VaultData(error = "Faltan parámetros (vault o wallet)")
        }

        try {
            val payload = JsonObject().apply {
                addProperty("type", "vaultDetails")
                addProperty("vaultAddress", cleanVault)
                if (cleanUser.isNotEmpty()) {
                    addProperty("user", cleanUser)
                }
            }

            val request = Request.Builder()
                .url(API_URL)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful || bodyString.isEmpty()) {
                return@withContext VaultData(error = "API HTTP Error ${response.code}")
            }

            val jsonElement = JsonParser.parseString(bodyString)
            if (!jsonElement.isJsonObject) {
                return@withContext VaultData(error = "Respuesta no es objeto JSON")
            }

            val data = jsonElement.asJsonObject

            var tvlStr = getJsonString(data, "tvl") ?: getJsonString(data, "tvlTotal") ?: "0.0"
            var vaultName = ""

            for (key in listOf("name", "vaultName", "vault_name")) {
                val v = getJsonString(data, key)
                if (!v.isNullOrBlank()) {
                    vaultName = v.trim()
                    break
                }
            }

            var yourDeposits = 0.0
            var allTimeEarned = 0.0

            // 1. Try dedicated userVaultEquities endpoint for user position across vaults
            if (cleanUser.isNotEmpty()) {
                try {
                    val userPayload = JsonObject().apply {
                        addProperty("type", "userVaultEquities")
                        addProperty("user", cleanUser)
                    }
                    val userReq = Request.Builder()
                        .url(API_URL)
                        .post(userPayload.toString().toRequestBody(jsonMediaType))
                        .build()

                    val userResp = client.newCall(userReq).execute()
                    val userBody = userResp.body?.string() ?: ""
                    if (userResp.isSuccessful && userBody.isNotEmpty()) {
                        val userElem = JsonParser.parseString(userBody)
                        if (userElem.isJsonArray) {
                            for (itemElem in userElem.asJsonArray) {
                                if (itemElem.isJsonObject) {
                                    val itemObj = itemElem.asJsonObject
                                    val itemVault = extractAddress(itemObj, "vaultAddress", "vault", "address") ?: ""
                                    if (itemVault.equals(cleanVault, ignoreCase = true)) {
                                        val (eq, pnl) = extractPositionData(itemObj)
                                        if (eq != null) yourDeposits = eq
                                        if (pnl != null) allTimeEarned = pnl
                                        break
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 2. Fallback to followers list in vaultDetails
            if (yourDeposits == 0.0 && data.has("followers") && data.get("followers").isJsonArray) {
                val followers = data.getAsJsonArray("followers")
                for (followerElem in followers) {
                    if (followerElem.isJsonObject) {
                        val followerObj = followerElem.asJsonObject
                        val followerUser = extractAddress(followerObj, "user", "follower", "address") ?: ""
                        if (followerUser.equals(cleanUser, ignoreCase = true)) {
                            val (eq, pnl) = extractPositionData(followerObj)
                            if (eq != null) yourDeposits = eq
                            if (pnl != null) allTimeEarned = pnl
                            break
                        }
                    }
                }
            }

            // 3. Fallback to relationship or root level user position in vaultDetails
            if (yourDeposits == 0.0 || allTimeEarned == 0.0) {
                val (eq, pnl) = extractPositionData(data)
                if (yourDeposits == 0.0 && eq != null) yourDeposits = eq
                if (allTimeEarned == 0.0 && pnl != null) allTimeEarned = pnl
            }

            // 4. Fallback to leader info in vaultDetails if user is vault leader
            val leaderAddr = extractAddress(data, "leader") ?: ""
            if (leaderAddr.isNotEmpty() && leaderAddr.equals(cleanUser, ignoreCase = true)) {
                if (yourDeposits == 0.0) {
                    val leaderEq = extractDouble(data, "leaderEquity")
                    if (leaderEq != null) {
                        yourDeposits = leaderEq
                    } else {
                        val leaderFrac = extractDouble(data, "leaderFraction")
                        val tvlValTemp = parseDouble(tvlStr)
                        if (leaderFrac != null && tvlValTemp > 0.0) {
                            yourDeposits = tvlValTemp * leaderFrac
                        }
                    }
                }
            }

            var monthDataObj: JsonObject? = null
            var allTimeDataObj: JsonObject? = null

            if (data.has("portfolio") && data.get("portfolio").isJsonArray) {
                val portfolioArr = data.getAsJsonArray("portfolio")
                for (itemElem in portfolioArr) {
                    if (itemElem.isJsonArray) {
                        val itemArr = itemElem.asJsonArray
                        if (itemArr.size() >= 2) {
                            val period = itemArr.get(0).asString
                            if (period == "month" && itemArr.get(1).isJsonObject) {
                                monthDataObj = itemArr.get(1).asJsonObject
                            } else if (period == "allTime" && itemArr.get(1).isJsonObject) {
                                allTimeDataObj = itemArr.get(1).asJsonObject
                            }
                        }
                    }
                }
            }

            var tvlVal = parseDouble(tvlStr)
            if (tvlVal == 0.0 && allTimeDataObj != null && allTimeDataObj.has("accountValueHistory")) {
                val accHist = allTimeDataObj.getAsJsonArray("accountValueHistory")
                if (accHist != null && accHist.size() > 0) {
                    val lastElem = accHist.get(accHist.size() - 1)
                    tvlVal = extractValueFromHistoryElem(lastElem)
                }
            }

            var pastMonthReturn = 0.0
            if (monthDataObj != null) {
                val accHist = if (monthDataObj.has("accountValueHistory") && monthDataObj.get("accountValueHistory").isJsonArray) {
                    monthDataObj.getAsJsonArray("accountValueHistory")
                } else null

                val pnlHist = if (monthDataObj.has("pnlHistory") && monthDataObj.get("pnlHistory").isJsonArray) {
                    monthDataObj.getAsJsonArray("pnlHistory")
                } else null

                if (accHist != null && pnlHist != null) {
                    val n = min(accHist.size(), pnlHist.size())
                    if (n >= 2) {
                        try {
                            var factor = 1.0
                            for (i in 1 until n) {
                                val avPrev = extractValueFromHistoryElem(accHist.get(i - 1))
                                val pnlCurr = extractValueFromHistoryElem(pnlHist.get(i))
                                val pnlPrev = extractValueFromHistoryElem(pnlHist.get(i - 1))
                                val deltaPnl = pnlCurr - pnlPrev
                                if (avPrev > 0.0) {
                                    val r = deltaPnl / avPrev
                                    factor *= (1.0 + r)
                                }
                            }
                            pastMonthReturn = (factor - 1.0) * 100.0
                        } catch (_: Exception) {
                            pastMonthReturn = 0.0
                        }
                    }
                }
            }

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentTime = timeFormat.format(Date())

            return@withContext VaultData(
                tvl = tvlVal,
                yourDeposits = yourDeposits,
                allTimeEarned = allTimeEarned,
                pastMonthReturn = pastMonthReturn,
                vaultName = vaultName,
                lastUpdated = currentTime
            )
        } catch (e: Exception) {
            return@withContext VaultData(error = "Fallo al procesar la API: ${e.message}")
        }
    }

    private fun extractAddress(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            if (obj.has(key) && !obj.get(key).isJsonNull) {
                val elem = obj.get(key)
                if (elem.isJsonPrimitive) {
                    val s = elem.asString
                    if (s.isNotBlank()) return s
                } else if (elem.isJsonObject) {
                    val nested = elem.asJsonObject
                    val addr = getJsonString(nested, "address") ?: getJsonString(nested, "user") ?: getJsonString(nested, "vaultAddress") ?: getJsonString(nested, "vault")
                    if (!addr.isNullOrBlank()) return addr
                }
            }
        }
        return null
    }

    private fun extractPositionData(obj: JsonObject): Pair<Double?, Double?> {
        val eqKeys = arrayOf("equity", "vaultEquity", "userEquity", "value", "deposits", "userDeposits", "leaderEquity")
        val pnlKeys = arrayOf("allTimePnl", "pnl", "allTimeEarned", "totalPnl", "cumPnl", "earned", "netPnl", "profit", "leaderPnl", "leaderAllTimePnl")

        var eq = extractDouble(obj, *eqKeys)
        var pnl = extractDouble(obj, *pnlKeys)

        for (nestedKey in listOf("relationship", "followerState", "userState", "state", "follower", "userVaultEquity", "user")) {
            if (obj.has(nestedKey) && obj.get(nestedKey).isJsonObject) {
                val nestedObj = obj.getAsJsonObject(nestedKey)
                if (eq == null) eq = extractDouble(nestedObj, *eqKeys)
                if (pnl == null) pnl = extractDouble(nestedObj, *pnlKeys)
            }
        }

        return Pair(eq, pnl)
    }

    private fun extractDouble(obj: JsonObject, vararg keys: String): Double? {
        for (key in keys) {
            if (obj.has(key) && !obj.get(key).isJsonNull) {
                val elem = obj.get(key)
                if (elem.isJsonPrimitive) {
                    val prim = elem.asJsonPrimitive
                    if (prim.isNumber) {
                        return prim.asDouble
                    } else if (prim.isString) {
                        val d = prim.asString.toDoubleOrNull()
                        if (d != null) return d
                    }
                }
            }
        }
        return null
    }

    private fun getJsonString(obj: JsonObject, key: String): String? {
        if (!obj.has(key) || obj.get(key).isJsonNull) return null
        return obj.get(key).asString
    }

    private fun parseDouble(str: String?): Double {
        if (str.isNullOrBlank()) return 0.0
        return str.toDoubleOrNull() ?: 0.0
    }

    private fun extractValueFromHistoryElem(elem: JsonElement): Double {
        return try {
            if (elem.isJsonArray) {
                val arr = elem.asJsonArray
                if (arr.size() >= 2) arr.get(1).asDouble else 0.0
            } else {
                elem.asDouble
            }
        } catch (_: Exception) {
            0.0
        }
    }
}
