package com.mutabei.workout.data.repository

import android.util.Log
import com.mutabei.workout.data.model.AiLabelResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val TAG = "OpenFoodFactsRepo"

class OpenFoodFactsRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun lookupBarcode(barcodeDigits: String): AiLabelResult? = withContext(Dispatchers.IO) {
        val clean = barcodeDigits.filter { it.isDigit() }
        if (clean.isBlank()) return@withContext null
        val url = "https://world.openfoodfacts.org/api/v2/product/$clean.json?fields=product_name,product_name_ar,brands,nutriments,serving_quantity"
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MutabeiWorkoutAndroid/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val root = JSONObject(body)
                if (root.optInt("status", 0) != 1) return@withContext null
                val p = root.optJSONObject("product") ?: return@withContext null
                val m = p.optJSONObject("nutriments") ?: return@withContext null

                val kcal = when {
                    m.has("energy-kcal_100g") && !m.isNull("energy-kcal_100g") -> m.optDouble("energy-kcal_100g", Double.NaN)
                    m.has("energy_100g") && !m.isNull("energy_100g") -> m.optDouble("energy_100g", Double.NaN) / 4.184
                    else -> Double.NaN
                }
                if (kcal.isNaN()) return@withContext null

                fun optD(k: String): Double? =
                    if (m.has(k) && !m.isNull(k)) m.optDouble(k, Double.NaN).takeIf { !it.isNaN() } else null

                fun optMg(k: String): Double? = optD(k)?.let { (it * 1000.0 * 10.0).roundToInt() / 10.0 }

                val nutrients = mapOf(
                    "kcal" to kcal.roundToInt().toDouble(),
                    "p" to optD("proteins_100g"),
                    "f" to optD("fat_100g"),
                    "c" to optD("carbohydrates_100g"),
                    "fib" to optD("fiber_100g"),
                    "sug" to optD("sugars_100g"),
                    "sat" to optD("saturated-fat_100g"),
                    "chol" to optMg("cholesterol_100g"),
                    "na" to optMg("sodium_100g"),
                    "k" to optMg("potassium_100g"),
                    "ca" to optMg("calcium_100g"),
                    "fe" to optMg("iron_100g")
                )

                val arName = p.optString("product_name_ar", "")
                val enName = p.optString("product_name", "")
                val brands = p.optString("brands", "")
                val displayName = buildString {
                    append(arName.ifBlank { enName })
                    if (brands.isNotBlank()) {
                        if (isNotEmpty()) append(" - ")
                        append(brands)
                    }
                }.trim()

                val servingQty = p.optString("serving_quantity", "").toDoubleOrNull()
                    ?: p.optDouble("serving_quantity", Double.NaN).takeIf { !it.isNaN() }

                AiLabelResult(
                    name = displayName,
                    barcode = clean,
                    servingGrams = servingQty,
                    nutrientsPer100 = nutrients
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenFoodFacts lookup failed for $clean", e)
            null
        }
    }
}
