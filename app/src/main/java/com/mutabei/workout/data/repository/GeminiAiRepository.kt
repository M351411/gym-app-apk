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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAiRepo"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val TIMEOUT_MS = 60_000L

class GeminiAiRepository {

    private fun segmentalSchema(): Schema = Schema.obj(
        properties = mapOf(
            "left_arm" to Schema.double(nullable = true),
            "right_arm" to Schema.double(nullable = true),
            "trunk" to Schema.double(nullable = true),
            "left_leg" to Schema.double(nullable = true),
            "right_leg" to Schema.double(nullable = true)
        ),
        optionalProperties = listOf("left_arm", "right_arm", "trunk", "left_leg", "right_leg")
    )

    private fun rangePairSchema(): Schema = Schema.array(
        items = Schema.double(),
        description = "Two-element array [low, high] for normal range",
        nullable = true
    )

    private val inBodyModel by lazy {
        val rangeKeys = listOf("w", "bfm", "ffm", "tbw", "pro", "min", "bmr", "whr", "vfl")
        val inBodySchema = Schema.obj(
            properties = mapOf(
                "test_date" to Schema.string(description = "YYYY-MM-DD test date printed on sheet", nullable = true),
                "height_cm" to Schema.double(nullable = true),
                "weight_kg" to Schema.double(nullable = true),
                "total_body_water_l" to Schema.double(nullable = true),
                "protein_kg" to Schema.double(nullable = true),
                "minerals_kg" to Schema.double(nullable = true),
                "body_fat_mass_kg" to Schema.double(nullable = true),
                "fat_free_mass_kg" to Schema.double(nullable = true),
                "skeletal_muscle_mass_kg" to Schema.double(nullable = true),
                "body_fat_percent" to Schema.double(nullable = true),
                "bmi" to Schema.double(nullable = true),
                "bmr_kcal" to Schema.double(nullable = true),
                "waist_hip_ratio" to Schema.double(nullable = true),
                "visceral_fat_level" to Schema.double(nullable = true),
                "obesity_degree_percent" to Schema.double(nullable = true),
                "inbody_score" to Schema.double(nullable = true),
                "target_weight_kg" to Schema.double(nullable = true),
                "fat_control_kg" to Schema.double(nullable = true),
                "muscle_control_kg" to Schema.double(nullable = true),
                "segmental_lean_kg" to segmentalSchema(),
                "segmental_lean_percent" to segmentalSchema(),
                "segmental_fat_kg" to segmentalSchema(),
                "ranges" to Schema.obj(
                    properties = rangeKeys.associateWith { rangePairSchema() },
                    optionalProperties = rangeKeys
                )
            ),
            optionalProperties = listOf(
                "test_date", "height_cm", "weight_kg", "total_body_water_l", "protein_kg",
                "minerals_kg", "body_fat_mass_kg", "fat_free_mass_kg", "skeletal_muscle_mass_kg",
                "body_fat_percent", "bmi", "bmr_kcal", "waist_hip_ratio", "visceral_fat_level",
                "obesity_degree_percent", "inbody_score", "target_weight_kg", "fat_control_kg",
                "muscle_control_kg", "segmental_lean_kg", "segmental_lean_percent",
                "segmental_fat_kg", "ranges"
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = inBodySchema
            }
        )
    }

    private val nutritionLabelModel by lazy {
        val per100Schema = Schema.obj(
            properties = NutritionConstants.NUTRIENT_KEYS.associateWith { Schema.double(nullable = true) },
            optionalProperties = NutritionConstants.NUTRIENT_KEYS
        )
        val labelSchema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string(description = "Product or supplement name in Arabic or original language", nullable = true),
                "barcode" to Schema.string(description = "Barcode digits if visible", nullable = true),
                "serving_g" to Schema.double(description = "Serving size in grams if food/powder; use 100 for per-tablet/capsule supplements", nullable = true),
                "is_supplement" to Schema.boolean(description = "True if this product is a vitamin, mineral, multivitamin, omega-3, or tablet/capsule supplement", nullable = true),
                "serving_unit" to Schema.string(description = "Arabic unit label for one serving if supplement: e.g. قرص, كبسولة, سكوب, كيس", nullable = true),
                "per100" to per100Schema
            ),
            optionalProperties = listOf("name", "barcode", "serving_g", "is_supplement", "serving_unit", "per100")
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = labelSchema
            }
        )
    }

    private val mealFoodsModel by lazy {
        val fallbackSchema = Schema.obj(
            properties = mapOf(
                "kcal" to Schema.double(nullable = true),
                "p" to Schema.double(nullable = true),
                "f" to Schema.double(nullable = true),
                "c" to Schema.double(nullable = true),
                "fib" to Schema.double(nullable = true)
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
                responseMimeType = "application/json"
                responseSchema = foodsSchema
            }
        )
    }

    private val barcodeModel by lazy {
        val barcodeSchema = Schema.obj(
            properties = mapOf(
                "digits" to Schema.string(description = "Exact numeric barcode digits under the bars", nullable = true)
            ),
            optionalProperties = listOf("digits")
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = barcodeSchema
            }
        )
    }

    suspend fun parseInBodySheet(bitmap: Bitmap): Result<BodyRecord> = runAiCall {
        val prompt = """
            Read this InBody body composition result sheet carefully.
            Extract all printed numeric measurements accurately (use null if not printed; never guess).
            Ranges should contain [low, high] normal ranges printed in brackets next to each value.
            Left and Right labels on the body diagram refer to the person's left and right sides as printed.
            Ignore all personal identifiers (name, ID number, phone numbers, addresses); do not output them.
            Format test_date as YYYY-MM-DD if visible.
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

        val segMap = mapOf(
            "la" to "left_arm", "ra" to "right_arm", "tr" to "trunk",
            "ll" to "left_leg", "rl" to "right_leg"
        )
        fun readSeg(objKey: String): Map<String, Double> {
            val obj = json.optJSONObject(objKey) ?: return emptyMap()
            return segMap.entries.mapNotNull { (shortKey, longKey) ->
                if (obj.has(longKey) && !obj.isNull(longKey)) {
                    obj.optDouble(longKey, Double.NaN).takeIf { !it.isNaN() }?.let { shortKey to it }
                } else null
            }.toMap()
        }

        val rangesObj = json.optJSONObject("ranges")
        val ranges = mutableMapOf<String, List<Double>>()
        if (rangesObj != null) {
            val keys = rangesObj.keys()
            while (keys.hasNext()) {
                val rk = keys.next()
                val arr = rangesObj.optJSONArray(rk)
                if (arr != null && arr.length() >= 2) {
                    val lo = arr.optDouble(0, Double.NaN)
                    val hi = arr.optDouble(1, Double.NaN)
                    if (!lo.isNaN() && !hi.isNaN()) ranges[rk] = listOf(lo, hi)
                }
            }
        }

        val rawDate = json.optString("test_date", "")
            .replace('.', '-')
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
            weightKg = optD("weight_kg"),
            smmKg = optD("skeletal_muscle_mass_kg"),
            pbfPercent = optD("body_fat_percent"),
            bfmKg = optD("body_fat_mass_kg"),
            ffmKg = optD("fat_free_mass_kg"),
            tbwL = optD("total_body_water_l"),
            proteinKg = optD("protein_kg"),
            mineralsKg = optD("minerals_kg"),
            bmrKcal = optD("bmr_kcal"),
            vfl = optD("visceral_fat_level"),
            whr = optD("waist_hip_ratio"),
            score = optD("inbody_score"),
            targetWeightKg = optD("target_weight_kg"),
            fatControlKg = optD("fat_control_kg"),
            muscleControlKg = optD("muscle_control_kg"),
            bmi = optD("bmi"),
            obesityDegree = optD("obesity_degree_percent"),
            heightCm = optD("height_cm"),
            segmentalLeanKg = readSeg("segmental_lean_kg"),
            segmentalLeanPercent = readSeg("segmental_lean_percent"),
            segmentalFatKg = readSeg("segmental_fat_kg"),
            ranges = ranges
        )
    }

    suspend fun parseNutritionLabel(bitmap: Bitmap): Result<AiLabelResult> = runAiCall {
        val prompt = """
            Read this Nutrition Facts or Supplement Facts label accurately.
            - If this is a FOOD or POWDER with weights in grams: all per100 values MUST be per 100 g (or 100 ml). If the label only lists per-serving values, convert them to per 100 g using serving_g, and set is_supplement=false.
            - If this is a DIETARY SUPPLEMENT (multivitamin, vitamin, mineral, omega-3, tablet, capsule, softgel): set is_supplement=true, serving_g=100, serving_unit to the Arabic unit ("قرص" for tablet, "كبسولة" for capsule/softgel, "سكوب" for scoop), and put the exact nutrient amounts FOR 1 TABLET/CAPSULE (or 1 serving if listed as 1 unit) directly into per100 (and kcal=0 if calories are not printed).
            - Convert IU units accurately when present: Vitamin D (1000 IU = 25 mcg), Vitamin A (1 IU = 0.3 mcg RAE), Vitamin E (1 IU = 0.67 mg d-alpha or 1.0 mg synthetic if unspecified).
            - For Omega-3 / Fish Oil: sum EPA + DHA (or total Omega-3 fatty acids) and output in GRAMS in key "o3" (e.g., 700 mg -> 0.7).
            Units required: kcal; GRAMS for p, f, c, fib, sug, sat, mono, poly, o3;
            MILLIGRAMS (mg) for chol, na, k, ca, fe, mg, ph, zn, cu, mn, vC, b1, b2, b3, b6, b5, vE;
            MICROGRAMS (mcg) for se, vA, fol, b12, vD, vK.
            Use null for any nutrient not printed on the label. Never guess unprinted values.
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
            Return its standard official active composition PER 1 TABLET / CAPSULE (or per 1 scoop if powder) directly inside "per100".
            Set is_supplement=true, serving_g=100 (for tablets/capsules) or actual scoop grams (for powders like whey), and serving_unit in Arabic ("قرص", "كبسولة", or "سكوب").
            Set kcal=0 if negligible/not applicable.
            Units required: kcal; GRAMS for p, f, c, fib, sug, sat, mono, poly, o3 (total Omega-3 in grams, e.g. 700 mg = 0.7);
            MILLIGRAMS (mg) for chol, na, k, ca, fe, mg, ph, zn, cu, mn, vC, b1, b2, b3, b6, b5, vE;
            MICROGRAMS (mcg) for se, vA, fol, b12, vD, vK.
            Convert IU to mcg/mg (Vitamin D: 1000 IU = 25 mcg; Vitamin A: 1 IU = 0.3 mcg; Vitamin E: 1 IU ≈ 0.67-1 mg).
            Only include nutrients genuinely present in this product; leave absent nutrients as null.
        """.trimIndent()

        val response = withTimeout(TIMEOUT_MS) {
            nutritionLabelModel.generateContent(prompt)
        }
        parseLabelJson(response.text ?: throw IllegalStateException("لم يرجع Gemini بيانات للمكمل."))
    }

    private fun parseLabelJson(rawText: String): AiLabelResult {
        val json = JSONObject(rawText)
        val per100Obj = json.optJSONObject("per100") ?: JSONObject()
        val nutMap = NutritionConstants.NUTRIENT_KEYS.associateWith { k ->
            if (per100Obj.has(k) && !per100Obj.isNull(k)) {
                per100Obj.optDouble(k, Double.NaN).takeIf { !it.isNaN() }
            } else null
        }
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
        val prompt = "Read the exact numeric barcode digits printed under the barcode bars in this image. Do not guess if unreadable; return null."
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

    private suspend inline fun <T> runAiCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Gemini AI call timed out after ${TIMEOUT_MS}ms", e)
        Result.failure(Exception("انتهى وقت الاتصال بـ Gemini بعد 60 ثانية. جرّب مرة تانية."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Gemini AI call failed", e)
        Result.failure(Exception(e.localizedMessage ?: "حصلت مشكلة أثناء التحليل بـ Gemini. جرّب صورة أوضح."))
    }
}
