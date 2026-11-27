package com.mutabei.workout.data.model

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

enum class NutrientTrackingType(val wireValue: String, val rollingDays: Int) {
    FAT_SOLUBLE("fat_soluble", 7),
    WATER_SOLUBLE("water_soluble", 1),
    MINERAL("mineral", 1)
}

enum class MicronutrientStatus(
    val wireValue: String,
    val uiBadgeEn: String,
    val uiBadgeAr: String
) {
    TOXICITY_WARNING(
        wireValue = "TOXICITY_WARNING",
        uiBadgeEn = "Toxicity Warning (UL Reached)",
        uiBadgeAr = "تحذير: وصلت للحد الآمن الأقصى"
    ),
    APPROACHING_UL(
        wireValue = "APPROACHING_UL",
        uiBadgeEn = "Near Safe Limit",
        uiBadgeAr = "قريب من الحد الآمن"
    ),
    WEEKLY_BUFFER_ACTIVE(
        wireValue = "WEEKLY_BUFFER_ACTIVE",
        uiBadgeEn = "Weekly Goal Met (Buffer Active)",
        uiBadgeAr = "الهدف الأسبوعي مكتمل (مخزون نشط)"
    ),
    NEEDS_TOP_UP(
        wireValue = "NEEDS_TOP_UP",
        uiBadgeEn = "Needs Intake",
        uiBadgeAr = "يحتاج تعويض"
    ),
    ON_TRACK(
        wireValue = "ON_TRACK",
        uiBadgeEn = "On Track",
        uiBadgeAr = "على المسار الصحيح"
    )
}

data class FatSolubleEvaluation(
    val key: String,
    val labelAr: String,
    val labelEn: String,
    val unit: String,
    val todayIntake: Double,
    val todayFoodSource: Double,
    val todaySupplementSource: Double,
    val todayCarryInSource: Double = 0.0,
    val todayCarryInLabel: String? = null,
    val dailyTargetBase: Double,
    val sevenDayTotal: Double,
    val sevenDayAvg: Double,
    val weeklyTarget: Double,
    val remainingWeeklyPool: Double,
    val weeklyPercentage: Double,
    val dailyUl: Double?,
    val weeklyUl: Double?,
    val hasAnyDataInWindow: Boolean,
    val status: MicronutrientStatus
) {
    val type: NutrientTrackingType get() = NutrientTrackingType.FAT_SOLUBLE
    val uiBadge: String get() = status.uiBadgeAr
    val uiBadgeEn: String get() = status.uiBadgeEn
}

data class DailyNutrientEvaluation(
    val key: String,
    val labelAr: String,
    val labelEn: String,
    val type: NutrientTrackingType,
    val unit: String,
    val todayIntake: Double,
    val foodSource: Double,
    val supplementSource: Double,
    val dailyTarget: Double,
    val remainingToday: Double,
    val dailyPercentage: Double,
    val dailyUl: Double?,
    val ulAppliesToSupplementsOnly: Boolean,
    val isLimitKey: Boolean,
    val hasDataToday: Boolean,
    val status: MicronutrientStatus
) {
    val rollingDays: Int get() = 1
    val uiBadge: String get() = status.uiBadgeAr
    val uiBadgeEn: String get() = status.uiBadgeEn
}

data class MicronutrientEvaluationReport(
    val referenceDate: String,
    val windowStartDate: String,
    val fatSoluble: List<FatSolubleEvaluation>,
    val dailyNutrients: List<DailyNutrientEvaluation>
) {
    val fatSolubleByKey: Map<String, FatSolubleEvaluation> = fatSoluble.associateBy { it.key }
    val dailyByKey: Map<String, DailyNutrientEvaluation> = dailyNutrients.associateBy { it.key }

    fun toGeminiJson(): String {
        val root = JSONObject()
        root.put("reference_date", referenceDate)
        root.put("window_start_date", windowStartDate)
        root.put("rolling_window_days", 7)

        val fatArr = JSONArray()
        fatSoluble.forEach { f ->
            val o = JSONObject()
            o.put("key", f.key)
            o.put("name", f.labelEn)
            o.put("type", f.type.wireValue)
            o.put("rolling_days", f.type.rollingDays)
            o.put("unit", f.unit)
            o.put("today_intake", round1(f.todayIntake))
            o.put("today_food_source", round1(f.todayFoodSource))
            o.put("today_supplement_source", round1(f.todaySupplementSource))
            if (f.todayCarryInSource > 0.0) {
                o.put("today_carry_in_from_yesterday", round1(f.todayCarryInSource))
            }
            o.put("daily_target_base", round1(f.dailyTargetBase))
            o.put("seven_day_total", round1(f.sevenDayTotal))
            o.put("seven_day_avg", f.sevenDayAvg.roundToInt())
            o.put("weekly_target", round1(f.weeklyTarget))
            o.put("remaining_weekly_pool", round1(f.remainingWeeklyPool))
            o.put("weekly_percentage", f.weeklyPercentage.roundToInt())
            if (f.dailyUl != null) o.put("daily_ul", round1(f.dailyUl)) else o.put("daily_ul", JSONObject.NULL)
            if (f.weeklyUl != null) o.put("weekly_ul", round1(f.weeklyUl)) else o.put("weekly_ul", JSONObject.NULL)
            o.put("status", f.status.wireValue)
            o.put("ui_badge", f.uiBadgeEn)
            fatArr.put(o)
        }
        root.put("fat_soluble_vitamins", fatArr)

        val dailyArr = JSONArray()
        dailyNutrients.forEach { d ->
            val o = JSONObject()
            o.put("key", d.key)
            o.put("name", d.labelEn)
            o.put("type", d.type.wireValue)
            o.put("rolling_days", d.rollingDays)
            o.put("unit", d.unit)
            o.put("today_intake", round1(d.todayIntake))
            o.put("food_source", round1(d.foodSource))
            o.put("supplement_source", round1(d.supplementSource))
            o.put("daily_target", round1(d.dailyTarget))
            o.put("remaining_today", round1(d.remainingToday))
            o.put("daily_percentage", d.dailyPercentage.roundToInt())
            if (d.dailyUl != null) {
                o.put("daily_ul", round1(d.dailyUl))
                o.put("ul_applies_to_supplements_only", d.ulAppliesToSupplementsOnly)
            }
            o.put("status", d.status.wireValue)
            dailyArr.put(o)
        }
        root.put("daily_nutrients", dailyArr)
        return root.toString()
    }

    private fun round1(v: Double): Double = (v * 10.0).roundToInt() / 10.0
}

/**
 * Dual-model Micronutrient Evaluation Engine:
 * - Fat-soluble vitamins (A, D3, E, K): 7-day rolling reserve pool (today + past 6 days)
 *   with custom Vitamin D3 5,000 IU/day baseline (35,000 IU/week target, 70,000 IU/week UL).
 * - Water-soluble vitamins (B-Complex, C) & Minerals: 24-hour daily reset with
 *   food_source vs supplement_source distinction (e.g., Magnesium 350 mg/day UL applies
 *   strictly to supplemental Magnesium, never whole foods).
 *
 * Preserves all existing foods.json (Cronometer / USDA / Egyptian Food Composition Table)
 * keys, indices, and internal storage units without altering any schema.
 */
object MicronutrientEngine {

    val FAT_SOLUBLE_KEYS: List<String> = listOf("vA", "vD", "vE", "vK")
    val FAT_SOLUBLE_KEY_SET: Set<String> = FAT_SOLUBLE_KEYS.toSet()

    val WATER_SOLUBLE_KEYS: List<String> = listOf("vC", "b1", "b2", "b3", "b5", "b6", "fol", "b12")
    val MINERAL_KEYS: List<String> = listOf("na", "k", "ca", "fe", "mg", "ph", "zn", "cu", "mn", "se", "io", "cr")

    private data class FatSolubleSpec(
        val key: String,
        val labelEn: String,
        val unit: String,
        val internalToDisplayScale: Double,
        val defaultDailyTargetDisplay: Double,
        val dailyUlDisplay: Double?
    )

    private val FAT_SOLUBLE_SPECS: List<FatSolubleSpec> = listOf(
        FatSolubleSpec(
            key = "vA",
            labelEn = "Vitamin A (RAE)",
            unit = "mcg",
            internalToDisplayScale = 1.0,
            defaultDailyTargetDisplay = 900.0,
            dailyUlDisplay = 3000.0
        ),
        FatSolubleSpec(
            key = "vD",
            labelEn = "Vitamin D3",
            unit = "IU",
            // Internal foods.json & LoggedFoodItem store vD in mcg (1 mcg = 40 IU)
            internalToDisplayScale = NutritionConstants.VITAMIN_D_IU_PER_MCG,
            defaultDailyTargetDisplay = 5000.0,
            dailyUlDisplay = 10000.0
        ),
        FatSolubleSpec(
            key = "vE",
            labelEn = "Vitamin E",
            unit = "mg",
            internalToDisplayScale = 1.0,
            defaultDailyTargetDisplay = 15.0,
            dailyUlDisplay = 1000.0
        ),
        FatSolubleSpec(
            key = "vK",
            labelEn = "Vitamin K",
            unit = "mcg",
            internalToDisplayScale = 1.0,
            defaultDailyTargetDisplay = 120.0,
            dailyUlDisplay = null
        )
    )

    private data class DailyNutrientSpec(
        val key: String,
        val labelEn: String,
        val type: NutrientTrackingType,
        val unit: String,
        val dailyUl: Double? = null,
        val ulAppliesToSupplementsOnly: Boolean = false
    )

    private val DAILY_SPECS: List<DailyNutrientSpec> = listOf(
        // Water-soluble vitamins
        DailyNutrientSpec("vC", "Vitamin C", NutrientTrackingType.WATER_SOLUBLE, "mg", dailyUl = 2000.0),
        DailyNutrientSpec("b1", "Vitamin B1 (Thiamine)", NutrientTrackingType.WATER_SOLUBLE, "mg"),
        DailyNutrientSpec("b2", "Vitamin B2 (Riboflavin)", NutrientTrackingType.WATER_SOLUBLE, "mg"),
        DailyNutrientSpec("b3", "Vitamin B3 (Niacin)", NutrientTrackingType.WATER_SOLUBLE, "mg", dailyUl = 35.0, ulAppliesToSupplementsOnly = true),
        DailyNutrientSpec("b5", "Vitamin B5 (Pantothenic Acid)", NutrientTrackingType.WATER_SOLUBLE, "mg"),
        DailyNutrientSpec("b6", "Vitamin B6", NutrientTrackingType.WATER_SOLUBLE, "mg", dailyUl = 100.0),
        DailyNutrientSpec("fol", "Vitamin B9 (Folate)", NutrientTrackingType.WATER_SOLUBLE, "mcg", dailyUl = 1000.0, ulAppliesToSupplementsOnly = true),
        DailyNutrientSpec("b12", "Vitamin B12", NutrientTrackingType.WATER_SOLUBLE, "mcg"),
        // Minerals
        DailyNutrientSpec("na", "Sodium", NutrientTrackingType.MINERAL, "mg", dailyUl = 2300.0),
        DailyNutrientSpec("k", "Potassium", NutrientTrackingType.MINERAL, "mg"),
        DailyNutrientSpec("ca", "Calcium", NutrientTrackingType.MINERAL, "mg", dailyUl = 2500.0),
        DailyNutrientSpec("fe", "Iron", NutrientTrackingType.MINERAL, "mg", dailyUl = 45.0),
        // Magnesium UL (350 mg/day) applies STRICTLY to supplemental intake, never whole foods!
        DailyNutrientSpec("mg", "Magnesium", NutrientTrackingType.MINERAL, "mg", dailyUl = 350.0, ulAppliesToSupplementsOnly = true),
        DailyNutrientSpec("ph", "Phosphorus", NutrientTrackingType.MINERAL, "mg", dailyUl = 4000.0),
        DailyNutrientSpec("zn", "Zinc", NutrientTrackingType.MINERAL, "mg", dailyUl = 40.0),
        DailyNutrientSpec("cu", "Copper", NutrientTrackingType.MINERAL, "mg", dailyUl = 10.0),
        DailyNutrientSpec("mn", "Manganese", NutrientTrackingType.MINERAL, "mg", dailyUl = 11.0),
        DailyNutrientSpec("se", "Selenium", NutrientTrackingType.MINERAL, "mcg", dailyUl = 400.0),
        DailyNutrientSpec("io", "Iodine", NutrientTrackingType.MINERAL, "mcg", dailyUl = 1100.0),
        DailyNutrientSpec("cr", "Chromium", NutrientTrackingType.MINERAL, "mcg")
    )

    fun isSupplementItem(item: LoggedFoodItem): Boolean =
        item.mealIndex == NutritionConstants.SUPPLEMENT_MEAL_INDEX ||
            SupplementCatalog.isUnitBasedLoggedItem(item)

    fun evaluateMicronutrients(
        referenceDate: String,
        nutritionDays: Map<String, NutritionDay>,
        sex: String = "m"
    ): MicronutrientEvaluationReport {
        val rda = if (sex == "f") NutritionConstants.RDA_FEMALE else NutritionConstants.RDA_MALE
        val windowDates = (0..6).map { offset -> shiftIsoDate(referenceDate, -offset.toLong()) }
        val windowStartDate = windowDates.last()
        val todayDay = nutritionDays[referenceDate]

        val fatSolubleResults = FAT_SOLUBLE_SPECS.map { spec ->
            val idx = NutritionConstants.NUTRIENT_INDEX[spec.key] ?: -1
            val scale = spec.internalToDisplayScale

            var todayFood = 0.0
            var todaySupp = 0.0
            var sevenDayTotal = 0.0
            var hasAnyData = false

            windowDates.forEach { dIso ->
                val day = nutritionDays[dIso]
                if (day != null && day.items.isNotEmpty()) {
                    var daySum = 0.0
                    day.items.forEach { item ->
                        val rawPer100 = if (idx >= 0) item.nutrients.getOrNull(idx) ?: -1.0 else -1.0
                        if (rawPer100 >= 0.0) {
                            hasAnyData = true
                            val amountDisplay = (rawPer100 * item.grams / 100.0) * scale
                            daySum += amountDisplay
                            if (dIso == referenceDate) {
                                if (isSupplementItem(item)) {
                                    todaySupp += amountDisplay
                                } else {
                                    todayFood += amountDisplay
                                }
                            }
                        }
                    }
                    sevenDayTotal += daySum
                }
            }

            // Apply Vitamin D3 every-other-day carryover (e.g. yesterday's 10,000 IU Davalindi carries +5,000 IU into today).
            // Note: sevenDayTotal already sums yesterday's full logged dose across the 7-day window, so carry-in is applied
            // ONLY to todayIntake (never added again to sevenDayTotal) to prevent double-counting.
            val vdBreakdown = if (spec.key == "vD") {
                NutritionConstants.computeVitaminDBreakdown(referenceDate, nutritionDays)
            } else null
            val todayCarryIn = (vdBreakdown?.carryInFromYesterdayMcg ?: 0.0) * scale
            if (todayCarryIn > 0.0) {
                hasAnyData = true
            }

            val todayIntake = todayFood + todaySupp + todayCarryIn
            // Preserve Vitamin D3 custom 5,000 IU baseline regardless of sex;
            // respect RDA for A/E/K (which equals 900 mcg / 15 mg / 120 mcg for male).
            val dailyTargetBase = if (spec.key == "vD") {
                spec.defaultDailyTargetDisplay
            } else {
                (rda[spec.key] ?: spec.defaultDailyTargetDisplay) * scale
            }
            val weeklyTarget = dailyTargetBase * 7.0
            val remainingWeeklyPool = max(0.0, weeklyTarget - sevenDayTotal)
            val weeklyPercentage = if (weeklyTarget > 0.0) (sevenDayTotal / weeklyTarget) * 100.0 else 0.0
            val sevenDayAvg = (sevenDayTotal / 7.0).roundToInt().toDouble()
            val weeklyUl = spec.dailyUlDisplay?.times(7.0)

            val status = when {
                weeklyUl != null && sevenDayTotal >= weeklyUl -> MicronutrientStatus.TOXICITY_WARNING
                weeklyUl != null && sevenDayTotal >= (weeklyUl * 0.85) -> MicronutrientStatus.APPROACHING_UL
                sevenDayTotal >= weeklyTarget -> MicronutrientStatus.WEEKLY_BUFFER_ACTIVE
                todayIntake < dailyTargetBase && remainingWeeklyPool > 0.0 -> MicronutrientStatus.NEEDS_TOP_UP
                else -> MicronutrientStatus.ON_TRACK
            }

            FatSolubleEvaluation(
                key = spec.key,
                labelAr = NutritionConstants.NUTRIENT_LABELS[spec.key] ?: spec.labelEn,
                labelEn = spec.labelEn,
                unit = spec.unit,
                todayIntake = todayIntake,
                todayFoodSource = todayFood,
                todaySupplementSource = todaySupp,
                todayCarryInSource = todayCarryIn,
                todayCarryInLabel = vdBreakdown?.yesterdaySourceName,
                dailyTargetBase = dailyTargetBase,
                sevenDayTotal = sevenDayTotal,
                sevenDayAvg = sevenDayAvg,
                weeklyTarget = weeklyTarget,
                remainingWeeklyPool = remainingWeeklyPool,
                weeklyPercentage = weeklyPercentage,
                dailyUl = spec.dailyUlDisplay,
                weeklyUl = weeklyUl,
                hasAnyDataInWindow = hasAnyData,
                status = status
            )
        }

        val dailyResults = DAILY_SPECS.map { spec ->
            val idx = NutritionConstants.NUTRIENT_INDEX[spec.key] ?: -1
            var foodSum = 0.0
            var suppSum = 0.0
            var hasToday = false

            todayDay?.items?.forEach { item ->
                val rawPer100 = if (idx >= 0) item.nutrients.getOrNull(idx) ?: -1.0 else -1.0
                if (rawPer100 >= 0.0) {
                    hasToday = true
                    val amount = rawPer100 * item.grams / 100.0
                    if (isSupplementItem(item)) {
                        suppSum += amount
                    } else {
                        foodSum += amount
                    }
                }
            }

            val todayIntake = foodSum + suppSum
            val dailyTarget = rda[spec.key] ?: 100.0
            val remainingToday = max(0.0, dailyTarget - todayIntake)
            val dailyPct = if (dailyTarget > 0.0) (todayIntake / dailyTarget) * 100.0 else 0.0
            val isLim = spec.key in NutritionConstants.LIMIT_KEYS

            val ulCheckedValue = if (spec.ulAppliesToSupplementsOnly) suppSum else todayIntake
            val status = when {
                spec.dailyUl != null && ulCheckedValue >= spec.dailyUl -> MicronutrientStatus.TOXICITY_WARNING
                spec.dailyUl != null && ulCheckedValue >= (spec.dailyUl * 0.85) -> MicronutrientStatus.APPROACHING_UL
                isLim -> MicronutrientStatus.ON_TRACK
                todayIntake >= dailyTarget -> MicronutrientStatus.ON_TRACK
                else -> MicronutrientStatus.NEEDS_TOP_UP
            }

            DailyNutrientEvaluation(
                key = spec.key,
                labelAr = NutritionConstants.NUTRIENT_LABELS[spec.key] ?: spec.labelEn,
                labelEn = spec.labelEn,
                type = spec.type,
                unit = spec.unit,
                todayIntake = todayIntake,
                foodSource = foodSum,
                supplementSource = suppSum,
                dailyTarget = dailyTarget,
                remainingToday = remainingToday,
                dailyPercentage = dailyPct,
                dailyUl = spec.dailyUl,
                ulAppliesToSupplementsOnly = spec.ulAppliesToSupplementsOnly,
                isLimitKey = isLim,
                hasDataToday = hasToday,
                status = status
            )
        }

        return MicronutrientEvaluationReport(
            referenceDate = referenceDate,
            windowStartDate = windowStartDate,
            fatSoluble = fatSolubleResults,
            dailyNutrients = dailyResults
        )
    }
}
