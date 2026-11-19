package com.mutabei.workout.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import com.mutabei.workout.appCheckInitSummary
import com.mutabei.workout.data.model.AiDetectedFoodRaw
import com.mutabei.workout.data.model.AiLabelResult
import com.mutabei.workout.data.model.BodyRecord
import com.mutabei.workout.data.model.NutritionConstants
import com.mutabei.workout.data.model.todayIso
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.ByteArrayOutputStream

private const val TAG = "GeminiAiRepo"
private const val PRIMARY_MODEL_NAME = "gemini-3.8-flash"
private const val FALLBACK_MODEL_NAME = "gemini-3.5-flash-lite"

private const val APP_CHECK_TIMEOUT_MS = 20_000L
private const val PRIMARY_TIMEOUT_MS = 45_000L
private const val FALLBACK_TIMEOUT_MS = 30_000L

private class AppCheckStageException(
    val timedOut: Boolean,
    val elapsedMs: Long,
    cause: Throwable? = null
) : Exception(
    if (timedOut) "App Check timed out after ${elapsedMs}ms" else (cause?.message ?: "App Check failed"),
    cause
)

private class GeminiStreamStageException(
    val primaryModel: String,
    val primarySummary: String,
    val fallbackModel: String?,
    val fallbackSummary: String?,
    val appCheckMs: Long,
    val imageKb: Int?,
    val anyTimedOut: Boolean,
    cause: Throwable?
) : Exception(primarySummary, cause)

class GeminiAiRepository {

    private fun segmentalSchema(description: String): Schema = Schema.obj(
        description = description,
        properties = mapOf(
            "left_arm" to Schema.double(description = "Left Arm value", nullable = true),
            "right_arm" to Schema.double(description = "Right Arm value", nullable = true),
            "trunk" to Schema.double(description = "Trunk value", nullable = true),
            "left_leg" to Schema.double(description = "Left Leg value", nullable = true),
            "right_leg" to Schema.double(description = "Right Leg value", nullable = true)
        ),
        nullable = true
    )

    private val inBodySchema: Schema by lazy {
        val rangeItemSchema = Schema.obj(
            properties = mapOf(
                "metric_key" to Schema.string(description = "One of: w, bfm, ffm, tbw, pro, min, bmr, whr, vfl"),
                "low" to Schema.double(description = "Lower bound of printed normal range"),
                "high" to Schema.double(description = "Upper bound of printed normal range")
            )
        )
        // Keep every core field REQUIRED with nullable=true so Gemini is forced
        // to inspect and report every single measurement instead of stopping early.
        Schema.obj(
            properties = mapOf(
                "test_date" to Schema.string(description = "Test date printed on sheet formatted as YYYY-MM-DD", nullable = true),
                "height_cm" to Schema.double(description = "Height in cm", nullable = true),
                "weight_kg" to Schema.double(description = "Total body weight in kg (from Body Composition table or Weight bar in Muscle-Fat Analysis)", nullable = true),
                "skeletal_muscle_mass_kg" to Schema.double(description = "Skeletal Muscle Mass (SMM) in kg from Muscle-Fat Analysis bar", nullable = true),
                "body_fat_mass_kg" to Schema.double(description = "Body Fat Mass (BFM) in kg", nullable = true),
                "percent_body_fat" to Schema.double(description = "Percent Body Fat (PBF %) from Obesity Analysis bar", nullable = true),
                "fat_free_mass_kg" to Schema.double(description = "Fat Free Mass (FFM) or Lean Body Mass in kg", nullable = true),
                "total_body_water_l" to Schema.double(description = "Total Body Water (TBW) in L or kg", nullable = true),
                "protein_kg" to Schema.double(description = "Protein mass in kg", nullable = true),
                "minerals_kg" to Schema.double(description = "Minerals mass in kg", nullable = true),
                "bmi" to Schema.double(description = "Body Mass Index (BMI) in kg/m2", nullable = true),
                "bmr_kcal" to Schema.double(description = "Basal Metabolic Rate (BMR) in kcal", nullable = true),
                "waist_hip_ratio" to Schema.double(description = "Waist-Hip Ratio (WHR), e.g. 0.95", nullable = true),
                "visceral_fat_level" to Schema.double(description = "Visceral Fat Level (VFL), e.g. 10", nullable = true),
                "obesity_degree_percent" to Schema.double(description = "Obesity Degree in %", nullable = true),
                "inbody_score" to Schema.double(description = "InBody Score out of 100 (e.g. 74 from 74/100)", nullable = true),
                "target_weight_kg" to Schema.double(description = "Target Weight in kg from Weight Control section", nullable = true),
                "fat_control_kg" to Schema.double(description = "Fat Control in kg (keep negative sign if printed, e.g. -8.5)", nullable = true),
                "muscle_control_kg" to Schema.double(description = "Muscle Control in kg (keep sign if printed, e.g. 4.2)", nullable = true),
                "segmental_lean_kg" to segmentalSchema("Segmental Lean Analysis upper bar values in kg"),
                "segmental_lean_percent" to segmentalSchema("Segmental Lean Analysis lower bar values in %"),
                "segmental_fat_kg" to segmentalSchema("Segmental Fat Analysis values in kg"),
                "normal_ranges" to Schema.array(
                    items = rangeItemSchema,
                    description = "Normal ranges printed in parentheses or brackets next to values, e.g. (58.0~78.4)",
                    nullable = true
                )
            ),
            optionalProperties = listOf("segmental_fat_kg", "normal_ranges")
        )
    }

    private val labelSchema: Schema by lazy {
        val nutrientEntrySchema = Schema.obj(
            properties = mapOf(
                "k" to Schema.string(description = "Nutrient key from: kcal, p, f, c, fib, sug, sat, mono, poly, o3, chol, na, k, ca, fe, mg, ph, zn, cu, mn, se, vA, vC, vD, vE, vK, b1, b2, b3, b5, b6, fol, b12"),
                "v" to Schema.double(description = "Numeric value in required unit")
            )
        )
        Schema.obj(
            properties = mapOf(
                "name" to Schema.string(description = "Product or supplement name", nullable = true),
                "barcode" to Schema.string(description = "Barcode digits if visible", nullable = true),
                "serving_g" to Schema.double(description = "Serving size in grams; use 100 for per-tablet/capsule supplements", nullable = true),
                "is_supplement" to Schema.boolean(description = "True if vitamin, mineral, multivitamin, omega-3, or tablet/capsule supplement"),
                "serving_unit" to Schema.string(description = "Arabic unit for one serving if supplement: قرص, كبسولة, سكوب, كيس", nullable = true),
                "nutrients" to Schema.array(
                    items = nutrientEntrySchema,
                    description = "All nutrients printed on the label"
                )
            )
        )
    }

    private val foodsSchema: Schema by lazy {
        val fallbackSchema = Schema.obj(
            properties = mapOf(
                "kcal" to Schema.double(nullable = true),
                "p" to Schema.double(nullable = true),
                "f" to Schema.double(nullable = true),
                "c" to Schema.double(nullable = true),
                "fib" to Schema.double(nullable = true)
            )
        )
        val itemSchema = Schema.obj(
            properties = mapOf(
                "name_en" to Schema.string(description = "Short generic English food name like 'rice, white, cooked' or 'chicken breast, grilled'"),
                "name_ar" to Schema.string(description = "Natural Arabic food name"),
                "grams" to Schema.double(description = "Realistic estimated weight eaten in grams"),
                "per100" to fallbackSchema
            )
        )
        Schema.obj(
            properties = mapOf(
                "items" to Schema.array(items = itemSchema, description = "Distinct foods identified")
            )
        )
    }

    private val barcodeSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "digits" to Schema.string(description = "Exact numeric barcode digits under the bars", nullable = true)
            )
        )
    }

    private fun buildModel(
        modelName: String,
        schema: Schema,
        temp: Float,
        thinking: ThinkingLevel?
    ): GenerativeModel = Firebase.ai.generativeModel(
        modelName = modelName,
        generationConfig = generationConfig {
            temperature = temp
            responseMimeType = "application/json"
            responseSchema = schema
            if (thinking != null) {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = thinking
                }
            }
        }
    )

    // InBody sheets are dense A4 tables + bar graphs: MEDIUM thinking gives much higher OCR completeness.
    private val inBodyPrimary by lazy { buildModel(PRIMARY_MODEL_NAME, inBodySchema, 0.0f, ThinkingLevel.MEDIUM) }
    private val inBodyFallback by lazy { buildModel(FALLBACK_MODEL_NAME, inBodySchema, 0.0f, thinking = null) }

    private val labelPrimary by lazy { buildModel(PRIMARY_MODEL_NAME, labelSchema, 0.1f, ThinkingLevel.LOW) }
    private val labelFallback by lazy { buildModel(FALLBACK_MODEL_NAME, labelSchema, 0.1f, thinking = null) }

    private val foodsPrimary by lazy { buildModel(PRIMARY_MODEL_NAME, foodsSchema, 0.2f, ThinkingLevel.LOW) }
    private val foodsFallback by lazy { buildModel(FALLBACK_MODEL_NAME, foodsSchema, 0.2f, thinking = null) }

    private val barcodePrimary by lazy { buildModel(PRIMARY_MODEL_NAME, barcodeSchema, 0.0f, ThinkingLevel.LOW) }
    private val barcodeFallback by lazy { buildModel(FALLBACK_MODEL_NAME, barcodeSchema, 0.0f, thinking = null) }

    suspend fun parseInBodySheet(
        bitmap: Bitmap,
        onProgress: (String) -> Unit = {}
    ): Result<BodyRecord> = runAiCall {
        val prompt = """
            You are an expert OCR extractor for body composition result sheets (InBody 120/270/370/570/770, Tanita, Accuniq, Charder, BioSpace, English or Arabic).
            Carefully scan EVERY section of the entire page from top to bottom and left to right, and extract EVERY printed numeric value into the JSON schema:

            1. Header:
               - test_date (YYYY-MM-DD), height_cm. Ignore personal name/ID/phone.
            2. Body Composition Analysis table (top-left):
               - total_body_water_l (Total Body Water / TBW / الماء)
               - protein_kg (Protein / البروتين)
               - minerals_kg (Minerals / المعادن)
               - body_fat_mass_kg (Body Fat Mass / BFM / كتلة الدهون)
               - fat_free_mass_kg (Fat Free Mass / FFM / Lean Body Mass)
               - weight_kg (Weight / الوزن)
               - normal_ranges: read the normal range printed in parentheses/brackets next to each value, e.g. (58.0~78.4) or [10.0-20.0], with metric_key in [w, bfm, ffm, tbw, pro, min, bmr, whr, vfl].
            3. Muscle-Fat Analysis horizontal bar graph (middle-left):
               - Read the BOLD measured value printed at the right end of each dark horizontal bar (NOT the reference percentage header numbers 55, 70, 85, 100, 115!):
                 * Weight -> weight_kg
                 * SMM / Skeletal Muscle Mass -> skeletal_muscle_mass_kg
                 * Body Fat Mass -> body_fat_mass_kg
            4. Obesity Analysis horizontal bar graph:
               - Read the measured value at the end of each dark bar:
                 * BMI -> bmi
                 * PBF / Percent Body Fat -> percent_body_fat
            5. Segmental Lean Analysis (bottom-left torso diagram with 5 sections: Right Arm, Left Arm, Trunk, Right Leg, Left Leg):
               - Upper number (kg) -> segmental_lean_kg (right_arm, left_arm, trunk, right_leg, left_leg)
               - Lower number (%) -> segmental_lean_percent (right_arm, left_arm, trunk, right_leg, left_leg)
               - If Segmental Fat Analysis is printed, put kg values in segmental_fat_kg.
            6. Right column (InBody Score, Weight Control, Research Parameters, Visceral Fat, Obesity Evaluation):
               - inbody_score: the score before "/100" (e.g. "76 / 100 Points" -> 76)
               - target_weight_kg, fat_control_kg (keep minus sign e.g. -8.4), muscle_control_kg (keep sign e.g. 3.5)
               - bmr_kcal (Basal Metabolic Rate in kcal, e.g. 1750)
               - waist_hip_ratio (WHR, e.g. 0.95)
               - visceral_fat_level (VFL / Visceral Fat Level, e.g. 10)
               - obesity_degree_percent (Obesity Degree %, e.g. 112)

            Important rules:
            - Extract EVERY number that is visible anywhere on the sheet. Do NOT skip fields that are printed in bar charts or the right column!
            - Set a field to null ONLY if that measurement genuinely does not exist on the sheet.
            - If the image is slightly tilted or rotated, still read all sections accurately.
        """.trimIndent()

        val rawText = executeMultimodalJson(
            bitmap = bitmap,
            prompt = prompt,
            taskLabel = "ورقة الـ InBody",
            primaryModel = inBodyPrimary,
            fallbackModel = inBodyFallback,
            highDetailJpeg = true,
            onProgress = onProgress
        )
        val json = JSONObject(rawText)

        fun optD(vararg keys: String): Double? {
            for (k in keys) {
                if (json.has(k) && !json.isNull(k)) {
                    val v = json.optDouble(k, Double.NaN)
                    if (!v.isNaN()) return v
                }
            }
            return null
        }

        val segMap = listOf(
            "la" to listOf("left_arm", "la"),
            "ra" to listOf("right_arm", "ra"),
            "tr" to listOf("trunk", "tr"),
            "ll" to listOf("left_leg", "ll"),
            "rl" to listOf("right_leg", "rl")
        )
        fun readSeg(vararg objKeys: String): Map<String, Double> {
            val obj = objKeys.firstNotNullOfOrNull { json.optJSONObject(it) } ?: return emptyMap()
            return segMap.mapNotNull { (shortKey, aliases) ->
                val v = aliases.firstNotNullOfOrNull { a ->
                    if (obj.has(a) && !obj.isNull(a)) obj.optDouble(a, Double.NaN).takeIf { !it.isNaN() } else null
                }
                v?.let { shortKey to it }
            }.toMap()
        }

        val ranges = mutableMapOf<String, List<Double>>()
        val rangesArr = json.optJSONArray("normal_ranges") ?: json.optJSONArray("ranges")
        if (rangesArr != null) {
            for (i in 0 until rangesArr.length()) {
                val item = rangesArr.optJSONObject(i) ?: continue
                val rk = item.optString("metric_key", item.optString("k", "")).trim()
                val lo = item.optDouble("low", item.optDouble("lo", Double.NaN))
                val hi = item.optDouble("high", item.optDouble("hi", Double.NaN))
                if (rk.isNotBlank() && !lo.isNaN() && !hi.isNaN()) {
                    ranges[rk] = listOf(lo, hi)
                }
            }
        }

        val rawDate = (if (json.isNull("test_date")) "" else json.optString("test_date", ""))
            .ifBlank { if (json.isNull("date")) "" else json.optString("date", "") }
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
            weightKg = optD("weight_kg", "w"),
            smmKg = optD("skeletal_muscle_mass_kg", "smm"),
            pbfPercent = optD("percent_body_fat", "pbf"),
            bfmKg = optD("body_fat_mass_kg", "bfm"),
            ffmKg = optD("fat_free_mass_kg", "ffm"),
            tbwL = optD("total_body_water_l", "tbw"),
            proteinKg = optD("protein_kg", "pro"),
            mineralsKg = optD("minerals_kg", "min"),
            bmrKcal = optD("bmr_kcal", "bmr"),
            vfl = optD("visceral_fat_level", "vfl"),
            whr = optD("waist_hip_ratio", "whr"),
            score = optD("inbody_score", "score"),
            targetWeightKg = optD("target_weight_kg", "tgt"),
            fatControlKg = optD("fat_control_kg", "fc"),
            muscleControlKg = optD("muscle_control_kg", "mc"),
            bmi = optD("bmi"),
            obesityDegree = optD("obesity_degree_percent", "obd"),
            heightCm = optD("height_cm", "h"),
            segmentalLeanKg = readSeg("segmental_lean_kg", "sl_kg"),
            segmentalLeanPercent = readSeg("segmental_lean_percent", "sl_pct"),
            segmentalFatKg = readSeg("segmental_fat_kg", "sf_kg"),
            ranges = ranges
        )
    }

    suspend fun parseNutritionLabel(
        bitmap: Bitmap,
        onProgress: (String) -> Unit = {}
    ): Result<AiLabelResult> = runAiCall {
        val prompt = """
            Read EVERY line of this Nutrition Facts or Supplement Facts label accurately.
            - If FOOD or POWDER with weights in grams: put values per 100g (or 100ml) in nutrients array; convert per-serving values to per 100g using serving_g; set is_supplement=false.
            - If DIETARY SUPPLEMENT (multivitamin, vitamin, mineral, omega-3, tablet, capsule, softgel): set is_supplement=true, serving_g=100, serving_unit in Arabic ("قرص", "كبسولة", or "سكوب"), and put exact amounts FOR 1 TABLET/CAPSULE directly in nutrients.
            - Convert IU: Vitamin D (1000 IU = 25 mcg), Vitamin A (1 IU = 0.3 mcg), Vitamin E (1 IU = 0.67-1 mg).
            - For Omega-3 / Fish Oil: total Omega-3 (EPA+DHA) in GRAMS under key "o3" (e.g., 700 mg -> 0.7).
            Required units: kcal; GRAMS for p, f, c, fib, sug, sat, mono, poly, o3;
            MG for chol, na, k, ca, fe, mg, ph, zn, cu, mn, vC, b1, b2, b3, b6, b5, vE;
            MCG for se, vA, fol, b12, vD, vK.
            Extract ALL nutrients printed on the label; never guess unprinted values.
        """.trimIndent()

        val rawText = executeMultimodalJson(
            bitmap = bitmap,
            prompt = prompt,
            taskLabel = "الملصق الغذائي",
            primaryModel = labelPrimary,
            fallbackModel = labelFallback,
            highDetailJpeg = true,
            onProgress = onProgress
        )
        parseLabelJson(rawText)
    }

    suspend fun lookupSupplementByName(
        supplementQuery: String,
        onProgress: (String) -> Unit = {}
    ): Result<AiLabelResult> = runAiCall {
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

        val rawText = executeTextJson(
            prompt = prompt,
            taskLabel = "تركيبة المكمل",
            primaryModel = labelPrimary,
            fallbackModel = labelFallback,
            onProgress = onProgress
        )
        parseLabelJson(rawText)
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

    suspend fun identifyMealPhoto(
        bitmap: Bitmap,
        onProgress: (String) -> Unit = {}
    ): Result<List<AiDetectedFoodRaw>> = runAiCall {
        val prompt = """
            Identify all foods visible in this meal photo. Egyptian and Middle Eastern dishes are common.
            List each distinct food item separately (e.g., rice, grilled chicken, salad, baladi bread, sauces).
            Provide realistic estimated grams eaten and a rough per100 fallback (kcal, p, f, c, fib).
            Be realistic, not optimistic.
        """.trimIndent()

        val rawText = executeMultimodalJson(
            bitmap = bitmap,
            prompt = prompt,
            taskLabel = "صورة الوجبة",
            primaryModel = foodsPrimary,
            fallbackModel = foodsFallback,
            highDetailJpeg = false,
            onProgress = onProgress
        )
        parseDetectedFoods(rawText)
    }

    suspend fun estimateMealFromText(
        description: String,
        onProgress: (String) -> Unit = {}
    ): Result<List<AiDetectedFoodRaw>> = runAiCall {
        val prompt = """
            Identify the foods described here: "$description".
            Egyptian and Middle Eastern dishes are common.
            List each distinct food separately with realistic estimated grams eaten and rough per100 fallback (kcal, p, f, c, fib).
            Be realistic, not optimistic.
        """.trimIndent()

        val rawText = executeTextJson(
            prompt = prompt,
            taskLabel = "وصف الوجبة",
            primaryModel = foodsPrimary,
            fallbackModel = foodsFallback,
            onProgress = onProgress
        )
        parseDetectedFoods(rawText)
    }

    suspend fun readBarcodeFromImage(
        bitmap: Bitmap,
        onProgress: (String) -> Unit = {}
    ): Result<String?> = runAiCall {
        val prompt = "Read the exact numeric barcode digits printed under the barcode bars in this image. Do not guess if unreadable; return null."
        val rawText = executeMultimodalJson(
            bitmap = bitmap,
            prompt = prompt,
            taskLabel = "رقم الباركود",
            primaryModel = barcodePrimary,
            fallbackModel = barcodeFallback,
            highDetailJpeg = true,
            onProgress = onProgress
        )
        val json = JSONObject(rawText)
        val digits = if (json.isNull("digits")) "" else json.optString("digits", "").filter { it.isDigit() }
        digits.ifBlank { null }
    }

    private suspend fun executeMultimodalJson(
        bitmap: Bitmap,
        prompt: String,
        taskLabel: String,
        primaryModel: GenerativeModel,
        fallbackModel: GenerativeModel,
        highDetailJpeg: Boolean,
        onProgress: (String) -> Unit
    ): String {
        onProgress("1/3 تجهيز الصورة بدقة عالية...")
        val jpegBytes = withContext(Dispatchers.IO) {
            compressBitmapToJpeg(bitmap, highDetail = highDetailJpeg)
        }
        val imageKb = (jpegBytes.size / 1024).coerceAtLeast(1)
        val requestContent = content {
            inlineData(jpegBytes, "image/jpeg")
            text(prompt)
        }
        return executeWithAppCheckAndFallback(
            requestContent = requestContent,
            taskLabel = taskLabel,
            imageKb = imageKb,
            primaryModel = primaryModel,
            fallbackModel = fallbackModel,
            onProgress = onProgress
        )
    }

    private suspend fun executeTextJson(
        prompt: String,
        taskLabel: String,
        primaryModel: GenerativeModel,
        fallbackModel: GenerativeModel,
        onProgress: (String) -> Unit
    ): String {
        val requestContent = content { text(prompt) }
        return executeWithAppCheckAndFallback(
            requestContent = requestContent,
            taskLabel = taskLabel,
            imageKb = null,
            primaryModel = primaryModel,
            fallbackModel = fallbackModel,
            onProgress = onProgress
        )
    }

    private suspend fun executeWithAppCheckAndFallback(
        requestContent: Content,
        taskLabel: String,
        imageKb: Int?,
        primaryModel: GenerativeModel,
        fallbackModel: GenerativeModel,
        onProgress: (String) -> Unit
    ): String {
        val stepPrefix = if (imageKb != null) "2/3" else "1/2"
        val finalPrefix = if (imageKb != null) "3/3" else "2/2"

        onProgress("$stepPrefix التحقق الأمني (App Check)...")
        val appCheckMs = ensureAppCheckToken()

        val sizeSuffix = if (imageKb != null) " (${imageKb} KB)" else ""
        onProgress("$finalPrefix جاري قراءة $taskLabel بـ Gemini$sizeSuffix...")

        val primaryAttempt = streamJsonAttempt(
            model = primaryModel,
            modelName = PRIMARY_MODEL_NAME,
            requestContent = requestContent,
            timeoutMs = PRIMARY_TIMEOUT_MS,
            onFirstChunk = { onProgress("$finalPrefix جاري استلام البيانات من Gemini...") }
        )
        if (primaryAttempt.text != null) {
            return primaryAttempt.text
        }

        val primaryErrLower = primaryAttempt.errorChain.lowercase()
        val isAuthOrQuotaError = primaryErrLower.contains("403") ||
            primaryErrLower.contains("appcheck") ||
            primaryErrLower.contains("app check") ||
            primaryErrLower.contains("recaptcha") ||
            primaryErrLower.contains("429") ||
            primaryErrLower.contains("quota")

        if (isAuthOrQuotaError) {
            throw GeminiStreamStageException(
                primaryModel = PRIMARY_MODEL_NAME,
                primarySummary = primaryAttempt.summary,
                fallbackModel = null,
                fallbackSummary = null,
                appCheckMs = appCheckMs,
                imageKb = imageKb,
                anyTimedOut = primaryAttempt.timedOut,
                cause = primaryAttempt.error
            )
        }

        onProgress("$finalPrefix جاري المحاولة بنموذج Gemini السريع (flash-lite)...")
        val fallbackAttempt = streamJsonAttempt(
            model = fallbackModel,
            modelName = FALLBACK_MODEL_NAME,
            requestContent = requestContent,
            timeoutMs = FALLBACK_TIMEOUT_MS,
            onFirstChunk = { onProgress("$finalPrefix جاري استلام البيانات من Gemini السريع...") }
        )
        if (fallbackAttempt.text != null) {
            return fallbackAttempt.text
        }

        throw GeminiStreamStageException(
            primaryModel = PRIMARY_MODEL_NAME,
            primarySummary = primaryAttempt.summary,
            fallbackModel = FALLBACK_MODEL_NAME,
            fallbackSummary = fallbackAttempt.summary,
            appCheckMs = appCheckMs,
            imageKb = imageKb,
            anyTimedOut = primaryAttempt.timedOut || fallbackAttempt.timedOut,
            cause = fallbackAttempt.error ?: primaryAttempt.error
        )
    }

    private suspend fun ensureAppCheckToken(): Long {
        val start = System.currentTimeMillis()
        return try {
            withContext(Dispatchers.Main) {
                withTimeout(APP_CHECK_TIMEOUT_MS) {
                    FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
                }
            }
            val elapsed = System.currentTimeMillis() - start
            Log.d(TAG, "App Check token ready in ${elapsed}ms ($appCheckInitSummary)")
            elapsed
        } catch (e: TimeoutCancellationException) {
            val elapsed = System.currentTimeMillis() - start
            Log.e(TAG, "App Check token timed out after ${elapsed}ms ($appCheckInitSummary)", e)
            throw AppCheckStageException(timedOut = true, elapsedMs = elapsed, cause = e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            Log.e(TAG, "App Check token failed after ${elapsed}ms ($appCheckInitSummary)", e)
            throw AppCheckStageException(timedOut = false, elapsedMs = elapsed, cause = e)
        }
    }

    private data class StreamAttemptResult(
        val text: String?,
        val timedOut: Boolean,
        val error: Throwable?,
        val errorChain: String,
        val summary: String
    )

    private suspend fun streamJsonAttempt(
        model: GenerativeModel,
        modelName: String,
        requestContent: Content,
        timeoutMs: Long,
        onFirstChunk: () -> Unit
    ): StreamAttemptResult {
        val start = System.currentTimeMillis()
        var chunks = 0
        var firstChunkMs = -1L
        val buffer = StringBuilder()

        return try {
            withContext(Dispatchers.IO) {
                withTimeout(timeoutMs) {
                    model.generateContentStream(requestContent).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            if (chunks == 0) {
                                firstChunkMs = System.currentTimeMillis() - start
                                withContext(Dispatchers.Main) { onFirstChunk() }
                            }
                            chunks++
                            buffer.append(piece)
                        }
                    }
                }
            }
            val finalText = buffer.toString().trim()
            if (finalText.isBlank()) {
                val elapsed = System.currentTimeMillis() - start
                val err = IllegalStateException("Empty response from $modelName")
                StreamAttemptResult(
                    text = null,
                    timedOut = false,
                    error = err,
                    errorChain = err.message.orEmpty(),
                    summary = "$modelName: empty after ${elapsed}ms (chunks=$chunks)"
                )
            } else {
                Log.d(TAG, "$modelName succeeded in ${System.currentTimeMillis() - start}ms (ttft=${firstChunkMs}ms, chunks=$chunks, chars=${finalText.length})")
                StreamAttemptResult(
                    text = finalText,
                    timedOut = false,
                    error = null,
                    errorChain = "",
                    summary = "ok"
                )
            }
        } catch (e: TimeoutCancellationException) {
            val elapsed = System.currentTimeMillis() - start
            Log.w(TAG, "$modelName timed out after ${elapsed}ms (chunks=$chunks, chars=${buffer.length})")
            StreamAttemptResult(
                text = null,
                timedOut = true,
                error = e,
                errorChain = "TimeoutCancellationException(${timeoutMs / 1000}s)",
                summary = "$modelName: timeout ${elapsed / 1000}s (chunks=$chunks, chars=${buffer.length})"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            val chain = unwrapCauseChain(e)
            Log.w(TAG, "$modelName failed after ${elapsed}ms: $chain", e)
            StreamAttemptResult(
                text = null,
                timedOut = false,
                error = e,
                errorChain = chain,
                summary = "$modelName: ${elapsed / 1000}s, chunks=$chunks, $chain"
            )
        }
    }

    private fun compressBitmapToJpeg(bitmap: Bitmap, highDetail: Boolean): ByteArray {
        val out = ByteArrayOutputStream()
        val initialQuality = if (highDetail) 90 else 80
        val maxBytes = if (highDetail) 1_500 * 1024 else 550 * 1024
        bitmap.compress(Bitmap.CompressFormat.JPEG, initialQuality, out)
        var bytes = out.toByteArray()
        if (bytes.size > maxBytes) {
            out.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, if (highDetail) 80 else 68, out)
            bytes = out.toByteArray()
        }
        return bytes
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

    private fun unwrapCauseChain(error: Throwable): String =
        generateSequence(error) { it.cause }
            .map { t ->
                val cls = t.javaClass.simpleName.ifBlank { "Error" }
                val msg = t.message?.trim().orEmpty()
                if (msg.isNotBlank()) "$cls: $msg" else cls
            }
            .distinct()
            .joinToString(" ← ")

    private fun formatAiError(error: Throwable): String {
        if (error is AppCheckStageException) {
            val causeText = error.cause?.let { unwrapCauseChain(it) }.orEmpty()
            val sec = (error.elapsedMs / 1000L).coerceAtLeast(1L)
            return if (error.timedOut) {
                "انتهى وقت التحقق الأمني (App Check / reCAPTCHA) بعد $sec ثانية قبل الاتصال بـ Gemini.\n" +
                    "[Stage=AppCheck-Timeout | init=$appCheckInitSummary | ${error.elapsedMs}ms]"
            } else {
                "تعذّر التحقق الأمني (App Check / reCAPTCHA) قبل الاتصال بـ Gemini.\n" +
                    "[Stage=AppCheck-Error | init=$appCheckInitSummary | ${error.elapsedMs}ms] ($causeText)"
            }
        }

        if (error is GeminiStreamStageException) {
            val details = buildString {
                append("[Stage=Gemini | appCheck=${error.appCheckMs}ms")
                if (error.imageKb != null) append(" | img=${error.imageKb}KB")
                append(" | init=$appCheckInitSummary]\n")
                append("1) ${error.primarySummary}")
                if (!error.fallbackSummary.isNullOrBlank()) {
                    append("\n2) ${error.fallbackSummary}")
                }
            }
            val lower = details.lowercase()
            val prefix = when {
                lower.contains("403") || lower.contains("appcheck") || lower.contains("app check") || lower.contains("recaptcha") || lower.contains("attestation") ->
                    "رفض الخادم التحقق الأمني (App Check / reCAPTCHA) عند الاتصال بـ Gemini."

                lower.contains("429") || lower.contains("quota") || lower.contains("resource_exhausted") ->
                    "تم تجاوز حد الطلبات المؤقت لـ Gemini. انتظر قليلًا ثم جرّب مرة أخرى."

                error.anyTimedOut ->
                    "انتهى وقت انتظار رد Gemini (بعد تجربة النموذج الأساسي والسريع). تأكد من الإنترنت وجرّب مرة تانية."

                lower.contains("unknownhost") || lower.contains("connect") || lower.contains("network") || lower.contains("socket") ->
                    "مشكلة في اتصال الإنترنت أثناء الإرسال إلى Gemini."

                else ->
                    "تعذّر التحليل بواسطة Gemini."
            }
            return "$prefix\n$details"
        }

        val combined = unwrapCauseChain(error)
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
        return "$friendlyPrefix\n[init=$appCheckInitSummary] ($combined)"
    }

    private suspend inline fun <T> runAiCall(crossinline block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Gemini AI call failed", e)
        Result.failure(Exception(formatAiError(e)))
    }
}
