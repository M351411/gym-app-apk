package com.mutabei.workout.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.mutabei.workout.data.model.AiDetectedFoodRaw
import com.mutabei.workout.data.model.AiLabelResult
import com.mutabei.workout.data.model.BodyRecord
import com.mutabei.workout.data.model.NutritionConstants
import com.mutabei.workout.data.model.todayIso
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAiRepo"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val TIMEOUT_MS = 60_000L

class GeminiAiRepository {

    private fun segmentalSchema(): Schema = Schema.obj(
        properties = mapOf(
            "la" to Schema.double(description = "Left arm"),
            "ra" to Schema.double(description = "Right arm"),
            "tr" to Schema.double(description = "Trunk"),
            "ll" to Schema.double(description = "Left leg"),
            "rl" to Schema.double(description = "Right leg")
        ),
        optionalProperties = listOf("la", "ra", "tr", "ll", "rl")
    )

    private val inBodyModel by lazy {
        val rangeItemSchema = Schema.obj(
            properties = mapOf(
                "k" to Schema.string(description = "Metric key: w, bfm, ffm, tbw, pro, min, bmr, whr, or vfl"),
                "lo" to Schema.double(description = "Lower bound of printed normal range"),
                "hi" to Schema.double(description = "Upper bound of printed normal range")
            )
        )
        val inBodySchema = Schema.obj(
            properties = mapOf(
                "date" to Schema.string(description = "YYYY-MM-DD test date printed on sheet"),
                "h" to Schema.double(description = "Height in cm"),
                "w" to Schema.double(description = "Weight in kg"),
                "tbw" to Schema.double(description = "Total body water in L"),
                "pro" to Schema.double(description = "Protein in kg"),
                "min" to Schema.double(description = "Minerals in kg"),
                "bfm" to Schema.double(description = "Body fat mass in kg"),
                "ffm" to Schema.double(description = "Fat free mass in kg"),
                "smm" to Schema.double(description = "Skeletal muscle mass in kg"),
                "pbf" to Schema.double(description = "Percent body fat %"),
                "bmi" to Schema.double(description = "BMI"),
                "bmr" to Schema.double(description = "Basal metabolic rate in kcal"),
                "whr" to Schema.double(description = "Waist-hip ratio"),
                "vfl" to Schema.double(description = "Visceral fat level"),
                "obd" to Schema.double(description = "Obesity degree %"),
                "score" to Schema.double(description = "InBody score out of 100"),
                "tgt" to Schema.double(description = "Target weight in kg"),
                "fc" to Schema.double(description = "Fat control in kg"),
                "mc" to Schema.double(description = "Muscle control in kg"),
                "sl_kg" to segmentalSchema(),
                "sl_pct" to segmentalSchema(),
                "sf_kg" to segmentalSchema(),
                "ranges" to Schema.array(
                    items = rangeItemSchema,
                    description = "Normal ranges printed in brackets next to values"
                )
            ),
            optionalProperties = listOf(
                "date", "h", "w", "tbw", "pro", "min", "bfm", "ffm", "smm", "pbf",
                "bmi", "bmr", "whr", "vfl", "obd", "score", "tgt", "fc", "mc",
                "sl_kg", "sl_pct", "sf_kg", "ranges"
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.1f
                responseMimeType = "application/json"
                responseSchema = inBodySchema
            }
        )
    }

    private val nutritionLabelModel by lazy {
        val nutrientEntrySchema = Schema.obj(
            properties = mapOf(
                "k" to Schema.string(description = "Nutrient key from: kcal, p, f, c, fib, sug, sat, mono, poly, o3, chol, na, k, ca, fe, mg, ph, zn, cu, mn, se, vA, vC, vD, vE, vK, b1, b2, b3, b5, b6, fol, b12"),
                "v" to Schema.double(description = "Numeric value in required unit")
            )
        )
        val labelSchema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string(description = "Product or supplement name"),
                "barcode" to Schema.string(description = "Barcode digits if visible"),
                "serving_g" to Schema.double(description = "Serving size in grams; use 100 for per-tablet/capsule supplements"),
                "is_supplement" to Schema.boolean(description = "True if vitamin, mineral, multivitamin, omega-3, or tablet/capsule supplement"),
                "serving_unit" to Schema.string(description = "Arabic unit for one serving if supplement: قرص, كبسولة, سكوب, كيس"),
                "nutrients" to Schema.array(
                    items = nutrientEntrySchema,
                    description = "List of printed nutrients only"
                )
            ),
            optionalProperties = listOf("name", "barcode", "serving_g", "is_supplement", "serving_unit", "nutrients")
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.1f
                responseMimeType = "application/json"
                responseSchema = labelSchema
            }
        )
    }

    private val mealFoodsModel by lazy {
        val fallbackSchema = Schema.obj(
            properties = mapOf(
                "kcal" to Schema.double(),
                "p" to Schema.double(),
                "f" to Schema.double(),
                "c" to Schema.double(),
                "fib" to Schema.double()
            ),
            optionalProperties = listOf("kcal", "p", "f", "c", "fib")
        )
        val itemSchema = Schema.obj(
            properties = mapOf(
                "name_en" to Schema.string(description = "Short generic English food name like 'rice, white, cooked' or 'chicken breast, grilled'"),
                "name_ar" to Schema.string(description = "Natural Arabic food name"),
                "grams" to Schema.double(description = "Realistic estimated weight eaten in grams"),
                "per100" to fallbackSchema
            ),
            optionalProperties = listOf("per100")
        )
        val foodsSchema = Schema.obj(
            properties = mapOf(
                "items" to Schema.array(items = itemSchema, description = "Distinct foods identified")
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.2f
                responseMimeType = "application/json"
                responseSchema = foodsSchema
            }
        )
    }

    private val barcodeModel by lazy {
        val barcodeSchema = Schema.obj(
            properties = mapOf(
                "digits" to Schema.string(description = "Exact numeric barcode digits under the bars")
            ),
            optionalProperties = listOf("digits")
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.0f
                responseMimeType = "application/json"
                responseSchema = barcodeSchema
            }
        )
    }

    suspend fun parseInBodySheet(bitmap: Bitmap): Result<BodyRecord> = runAiCall {
        val prompt = """
            Read this InBody body composition result sheet carefully.
            Extract all printed numeric measurements accurately and OMIT any field not printed (never guess).
            Segmental maps: sl_kg = Segmental Lean (kg), sl_pct = Segmental Lean (%), sf_kg = Segmental Fat (kg), using keys la (left arm), ra (right arm), tr (trunk), ll (left leg), rl (right leg).
            For ranges, include printed [low, high] normal range brackets with k in [w, bfm, ffm, tbw, pro, min, bmr, whr, vfl].
            Ignore personal identifiers (name, ID, phone, address).
            Format date as YYYY-MM-DD if printed.
        """.trimIndent()

        val response = withTimeout(TIMEOUT_MS) {
            inBodyModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
        }
        val rawText = response.text ?: throw IllegalStateException("لم يرجع Gemini أي بيانات من الصورة.")
        val json = JSONObject(rawText)

        fun optD(k: String): Double? =
            if (json.has(k) && !json.isNull(k)) json.optDouble(k, Double.NaN).takeIf { !it.isNaN() } else null

        val segKeys = listOf("la", "ra", "tr", "ll", "rl")
        fun readSeg(objKey: String): Map<String, Double> {
            val obj = json.optJSONObject(objKey) ?: return emptyMap()
            return segKeys.mapNotNull { k ->
                if (obj.has(k) && !obj.isNull(k)) {
                    obj.optDouble(k, Double.NaN).takeIf { !it.isNaN() }?.let { k to it }
                } else null
            }.toMap()
        }

        val ranges = mutableMapOf<String, List<Double>>()
        val rangesArr = json.optJSONArray("ranges")
        if (rangesArr != null) {
            for (i in 0 until rangesArr.length()) {
                val item = rangesArr.optJSONObject(i) ?: continue
                val rk = item.optString("k", "").trim()
                val lo = item.optDouble("lo", Double.NaN)
                val hi = item.optDouble("hi", Double.NaN)
                if (rk.isNotBlank() && !lo.isNaN() && !hi.isNaN()) {
                    ranges[rk] = listOf(lo, hi)
                }
            }
        }

        val rawDate = json.optString("date", "")
            .replace('.', '-')
            .replace('/', '-')
            .replace(Regex("\\s.*$"), "")
            .removeSuffix("-")
        val parsedDate = if (Regex("^\\d{4}-\\d{1,2}-\\d{1,2}$").matches(rawDate)) {
            val parts = rawDate.split("-")
            "${parts[0]}-${parts[1].padStart(2, '0')}-${parts[2].padStart(2, '0')}"
        } else {
            todayIso()
        }

        BodyRecord(
            date = parsedDate,
            weightKg = optD("w"),
            smmKg = optD("smm"),
            pbfPercent = optD("pbf"),
            bfmKg = optD("bfm"),
            ffmKg = optD("ffm"),
            tbwL = optD("tbw"),
            proteinKg = optD("pro"),
            mineralsKg = optD("min"),
            bmrKcal = optD("bmr"),
            vfl = optD("vfl"),
            whr = optD("whr"),
            score = optD("score"),
            targetWeightKg = optD("tgt"),
            fatControlKg = optD("fc"),
            muscleControlKg = optD("mc"),
            bmi = optD("bmi"),
            obesityDegree = optD("obd"),
            heightCm = optD("h"),
            segmentalLeanKg = readSeg("sl_kg"),
            segmentalLeanPercent = readSeg("sl_pct"),
            segmentalFatKg = readSeg("sf_kg"),
            ranges = ranges
        )
    }

    suspend fun parseNutritionLabel(bitmap: Bitmap): Result<AiLabelResult> = runAiCall {
        val prompt = """
            Read this Nutrition Facts or Supplement Facts label accurately.
            - If FOOD or POWDER with weights in grams: put values per 100g (or 100ml) in nutrients array; convert per-serving values to per 100g using serving_g; set is_supplement=false.
            - If DIETARY SUPPLEMENT (multivitamin, vitamin, mineral, omega-3, tablet, capsule, softgel): set is_supplement=true, serving_g=100, serving_unit in Arabic ("قرص", "كبسولة", or "سكوب"), and put exact amounts FOR 1 TABLET/CAPSULE directly in nutrients.
            - Convert IU: Vitamin D (1000 IU = 25 mcg), Vitamin A (1 IU = 0.3 mcg), Vitamin E (1 IU = 0.67-1 mg).
            - For Omega-3 / Fish Oil: total Omega-3 (EPA+DHA) in GRAMS under key "o3" (e.g., 700 mg -> 0.7).
            Required units: kcal; GRAMS for p, f, c, fib, sug, sat, mono, poly, o3;
            MG for chol, na, k, ca, fe, mg, ph, zn, cu, mn, vC, b1, b2, b3, b6, b5, vE;
            MCG for se, vA, fol, b12, vD, vK.
            Only include nutrients printed on the label; never guess unprinted values.
        """.trimIndent()

        val response = withTimeout(TIMEOUT_MS) {
            nutritionLabelModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
        }
        parseLabelJson(response.text ?: throw IllegalStateException("لم يرجع Gemini بيانات من الملصق."))
    }

    suspend fun lookupSupplementByName(supplementQuery: String): Result<AiLabelResult> = runAiCall {
        val prompt = """
            The user wants to log this dietary supplement / multivitamin / pharmacy product (Egyptian and international pharmacy brands are common): "$supplementQuery".
            Return its standard official active composition PER 1 TABLET / CAPSULE (or per 1 scoop if powder) inside "nutrients" array as {k, v} entries.
            Set is_supplement=true, serving_g=100 (for tablets/capsules) or actual scoop grams (for powders), and serving_unit in Arabic ("قرص", "كبسولة", or "سكوب").
            Required units: kcal; GRAMS for p, f, c, fib, sug, sat, mono, poly, o3 (total Omega-3 in grams, e.g. 700 mg = 0.7);
            MG for chol, na, k, ca, fe, mg, ph, zn, cu, mn, vC, b1, b2, b3, b6, b5, vE;
            MCG for se, vA, fol, b12, vD, vK.
            Convert IU to mcg/mg (Vitamin D: 1000 IU = 25 mcg; Vitamin A: 1 IU = 0.3 mcg; Vitamin E: 1 IU = 0.67-1 mg).
            Only include nutrients genuinely present in this product.
        """.trimIndent()

        val response = withTimeout(TIMEOUT_MS) {
            nutritionLabelModel.generateContent(prompt)
        }
        parseLabelJson(response.text ?: throw IllegalStateException("لم يرجع Gemini بيانات للمكمل."))
    }

    private fun parseLabelJson(rawText: String): AiLabelResult {
        val json = JSONObject(rawText)
        val rawMap = mutableMapOf<String, Double>()
        val nutArr = json.optJSONArray("nutrients")
        if (nutArr != null) {
            for (i in 0 until nutArr.length()) {
                val entry = nutArr.optJSONObject(i) ?: continue
                val k = entry.optString("k", "").trim()
                val v = entry.optDouble("v", Double.NaN)
                if (k.isNotBlank() && !v.isNaN()) {
                    rawMap[k] = v
                }
            }
        }
        val nutMap = NutritionConstants.NUTRIENT_KEYS.associateWith { k -> rawMap[k] }
        val isSupp = json.optBoolean("is_supplement", false)
        val unit = if (json.isNull("serving_unit")) "" else json.optString("serving_unit", "").trim()
        val sg = if (json.has("serving_g") && !json.isNull("serving_g")) {
            json.optDouble("serving_g", Double.NaN).takeIf { !it.isNaN() && it > 0 }
        } else null

        return AiLabelResult(
            name = if (json.isNull("name")) "" else json.optString("name", ""),
            barcode = if (json.isNull("barcode")) "" else json.optString("barcode", "").filter { it.isDigit() },
            servingGrams = sg ?: if (isSupp) 100.0 else null,
            nutrientsPer100 = nutMap,
            isSupplement = isSupp,
            servingUnit = unit.ifBlank { if (isSupp) "قرص" else "" }
        )
    }

    suspend fun identifyMealPhoto(bitmap: Bitmap): Result<List<AiDetectedFoodRaw>> = runAiCall {
        val prompt = """
            Identify all foods visible in this meal photo. Egyptian and Middle Eastern dishes are common.
            List each distinct food item separately (e.g., rice, grilled chicken, salad, baladi bread, sauces).
            Provide realistic estimated grams eaten and a rough per100 fallback (kcal, p, f, c, fib).
            Be realistic, not optimistic.
        """.trimIndent()

        val response = withTimeout(TIMEOUT_MS) {
            mealFoodsModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
        }
        parseDetectedFoods(response.text)
    }

    suspend fun estimateMealFromText(description: String): Result<List<AiDetectedFoodRaw>> = runAiCall {
        val prompt = """
            Identify the foods described here: "$description".
            Egyptian and Middle Eastern dishes are common.
            List each distinct food separately with realistic estimated grams eaten and rough per100 fallback (kcal, p, f, c, fib).
            Be realistic, not optimistic.
        """.trimIndent()

        val response = withTimeout(TIMEOUT_MS) {
            mealFoodsModel.generateContent(prompt)
        }
        parseDetectedFoods(response.text)
    }

    suspend fun readBarcodeFromImage(bitmap: Bitmap): Result<String?> = runAiCall {
        val prompt = "Read the exact numeric barcode digits printed under the barcode bars in this image. Do not guess if unreadable; omit digits."
        val response = withTimeout(TIMEOUT_MS) {
            barcodeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
        }
        val rawText = response.text ?: return@runAiCall null
        val json = JSONObject(rawText)
        val digits = if (json.isNull("digits")) "" else json.optString("digits", "").filter { it.isDigit() }
        digits.ifBlank { null }
    }

    private fun parseDetectedFoods(rawText: String?): List<AiDetectedFoodRaw> {
        val text = rawText ?: throw IllegalStateException("لم يرجع Gemini أصنافاً.")
        val json = JSONObject(text)
        val arr = json.optJSONArray("items") ?: return emptyList()
        val result = mutableListOf<AiDetectedFoodRaw>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val per100Obj = o.optJSONObject("per100") ?: JSONObject()
            fun p(k: String): Double? =
                if (per100Obj.has(k) && !per100Obj.isNull(k)) per100Obj.optDouble(k, Double.NaN).takeIf { !it.isNaN() } else null
            result.add(
                AiDetectedFoodRaw(
                    nameEn = o.optString("name_en", ""),
                    nameAr = o.optString("name_ar", ""),
                    grams = o.optDouble("grams", 100.0).takeIf { !it.isNaN() && it > 0 } ?: 100.0,
                    fallbackPer100 = mapOf(
                        "kcal" to p("kcal"),
                        "p" to p("p"),
                        "f" to p("f"),
                        "c" to p("c"),
                        "fib" to p("fib")
                    )
                )
            )
        }
        return result
    }

    private fun formatAiError(error: Throwable): String {
        val chain = generateSequence(error) { it.cause }
            .map { t ->
                val cls = t.javaClass.simpleName.ifBlank { "Error" }
                val msg = t.message?.trim().orEmpty()
                if (msg.isNotBlank()) "$cls: $msg" else cls
            }
            .distinct()
            .toList()
        val combined = chain.joinToString(" ← ")
        val lower = combined.lowercase()
        val friendlyPrefix = when {
            lower.contains("403") || lower.contains("appcheck") || lower.contains("app check") || lower.contains("recaptcha") || lower.contains("attestation") ->
                "تعذّر التحقق الأمني (App Check / reCAPTCHA) أثناء الاتصال بـ Gemini."

            lower.contains("429") || lower.contains("quota") || lower.contains("resource_exhausted") ->
                "تم تجاوز حد الطلبات المؤقت لـ Gemini. انتظر قليلًا ثم جرّب مرة أخرى."

            lower.contains("unknownhost") || lower.contains("connect") || lower.contains("network") || lower.contains("socket") ->
                "مشكلة في اتصال الإنترنت أثناء إرسال الصورة لـ Gemini."

            else ->
                "تعذّر التحليل بواسطة Gemini."
        }
        return "$friendlyPrefix\n($combined)"
    }

    private suspend inline fun <T> runAiCall(crossinline block: suspend () -> T): Result<T> = try {
        withContext(Dispatchers.IO) {
            Result.success(block())
        }
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Gemini AI call timed out after ${TIMEOUT_MS}ms", e)
        Result.failure(Exception("انتهى وقت الاتصال بـ Gemini بعد 60 ثانية. تأكد من استقرار الإنترنت وجرّب صورة أوضح."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Gemini AI call failed", e)
        Result.failure(Exception(formatAiError(e)))
    }
}
