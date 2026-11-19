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
private const val PRIMARY_TIMEOUT_MS = 35_000L
private const val FALLBACK_TIMEOUT_MS = 25_000L

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

    private val inBodySchema: Schema by lazy {
        val rangeItemSchema = Schema.obj(
            properties = mapOf(
                "k" to Schema.string(description = "Metric key: w, bfm, ffm, tbw, pro, min, bmr, whr, or vfl"),
                "lo" to Schema.double(description = "Lower bound of printed normal range"),
                "hi" to Schema.double(description = "Upper bound of printed normal range")
            )
        )
        Schema.obj(
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
    }

    private val foodsSchema: Schema by lazy {
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
        Schema.obj(
            properties = mapOf(
                "items" to Schema.array(items = itemSchema, description = "Distinct foods identified")
            )
        )
    }

    private val barcodeSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "digits" to Schema.string(description = "Exact numeric barcode digits under the bars")
            ),
            optionalProperties = listOf("digits")
        )
    }

    private fun buildModel(
        modelName: String,
        schema: Schema,
        temp: Float,
        lowThinking: Boolean
    ): GenerativeModel = Firebase.ai.generativeModel(
        modelName = modelName,
        generationConfig = generationConfig {
            temperature = temp
            responseMimeType = "application/json"
            responseSchema = schema
            if (lowThinking) {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            }
        }
    )

    private val inBodyPrimary by lazy { buildModel(PRIMARY_MODEL_NAME, inBodySchema, 0.1f, lowThinking = true) }
    private val inBodyFallback by lazy { buildModel(FALLBACK_MODEL_NAME, inBodySchema, 0.1f, lowThinking = false) }

    private val labelPrimary by lazy { buildModel(PRIMARY_MODEL_NAME, labelSchema, 0.1f, lowThinking = true) }
    private val labelFallback by lazy { buildModel(FALLBACK_MODEL_NAME, labelSchema, 0.1f, lowThinking = false) }

    private val foodsPrimary by lazy { buildModel(PRIMARY_MODEL_NAME, foodsSchema, 0.2f, lowThinking = true) }
    private val foodsFallback by lazy { buildModel(FALLBACK_MODEL_NAME, foodsSchema, 0.2f, lowThinking = false) }

    private val barcodePrimary by lazy { buildModel(PRIMARY_MODEL_NAME, barcodeSchema, 0.0f, lowThinking = true) }
    private val barcodeFallback by lazy { buildModel(FALLBACK_MODEL_NAME, barcodeSchema, 0.0f, lowThinking = false) }

    suspend fun parseInBodySheet(
        bitmap: Bitmap,
        onProgress: (String) -> Unit = {}
    ): Result<BodyRecord> = runAiCall {
        val prompt = """
            Read this InBody body composition result sheet carefully.
            Extract all printed numeric measurements accurately and OMIT any field not printed (never guess).
            Segmental maps: sl_kg = Segmental Lean (kg), sl_pct = Segmental Lean (%), sf_kg = Segmental Fat (kg), using keys la (left arm), ra (right arm), tr (trunk), ll (left leg), rl (right leg).
            For ranges, include printed [low, high] normal range brackets with k in [w, bfm, ffm, tbw, pro, min, bmr, whr, vfl].
            Ignore personal identifiers (name, ID, phone, address).
            Format date as YYYY-MM-DD if printed.
        """.trimIndent()

        val rawText = executeMultimodalJson(
            bitmap = bitmap,
            prompt = prompt,
            taskLabel = "ورقة الـ InBody",
            primaryModel = inBodyPrimary,
            fallbackModel = inBodyFallback,
            onProgress = onProgress
        )
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

    suspend fun parseNutritionLabel(
        bitmap: Bitmap,
        onProgress: (String) -> Unit = {}
    ): Result<AiLabelResult> = runAiCall {
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

        val rawText = executeMultimodalJson(
            bitmap = bitmap,
            prompt = prompt,
            taskLabel = "الملصق الغذائي",
            primaryModel = labelPrimary,
            fallbackModel = labelFallback,
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
        val prompt = "Read the exact numeric barcode digits printed under the barcode bars in this image. Do not guess if unreadable; omit digits."
        val rawText = executeMultimodalJson(
            bitmap = bitmap,
            prompt = prompt,
            taskLabel = "رقم الباركود",
            primaryModel = barcodePrimary,
            fallbackModel = barcodeFallback,
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
        onProgress: (String) -> Unit
    ): String {
        onProgress("1/3 تجهيز الصورة...")
        val jpegBytes = withContext(Dispatchers.IO) {
            compressBitmapToJpeg(bitmap)
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

        // Phase 1: Explicitly acquire App Check token on Main dispatcher (non-blocking await)
        // so we can distinguish reCAPTCHA/App Check hangs from Gemini inference latency.
        onProgress("$stepPrefix التحقق الأمني (App Check)...")
        val appCheckMs = ensureAppCheckToken()

        // Phase 2: Stream from primary model (gemini-3.8-flash with LOW thinking)
        val sizeSuffix = if (imageKb != null) " (${imageKb} KB)" else ""
        onProgress("$finalPrefix جاري تحليل $taskLabel بـ Gemini$sizeSuffix...")

        val primaryAttempt = streamJsonAttempt(
            model = primaryModel,
            modelName = PRIMARY_MODEL_NAME,
            requestContent = requestContent,
            timeoutMs = PRIMARY_TIMEOUT_MS,
            onFirstChunk = { onProgress("$finalPrefix جاري استلام النتيجة من Gemini...") }
        )
        if (primaryAttempt.text != null) {
            return primaryAttempt.text
        }

        // If the primary error is clearly an App Check / 403 / quota rejection, don't waste time retrying.
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

        // Phase 3: Automatic fast fallback to gemini-3.5-flash-lite
        onProgress("$finalPrefix جاري المحاولة بنموذج Gemini السريع (flash-lite)...")
        val fallbackAttempt = streamJsonAttempt(
            model = fallbackModel,
            modelName = FALLBACK_MODEL_NAME,
            requestContent = requestContent,
            timeoutMs = FALLBACK_TIMEOUT_MS,
            onFirstChunk = { onProgress("$finalPrefix جاري استلام النتيجة من Gemini السريع...") }
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

    private fun compressBitmapToJpeg(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        var bytes = out.toByteArray()
        if (bytes.size > 450 * 1024) {
            out.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 65, out)
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
