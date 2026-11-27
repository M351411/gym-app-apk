package com.mutabei.workout.data.model

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

// ============================================================================
// V3 Feature 1: Food Additive & Label Scanner (Food Processing Quality Audit)
// ============================================================================

enum class IngredientFlagSeverity(val labelAr: String) {
    HIGH_RISK("خطر / يُفضّل تجنّبه"),
    MODERATE("انتبه / باعتدال"),
    CLEAN("مكوّن نظيف")
}

data class IngredientAuditFlag(
    val categoryAr: String,
    val ingredientName: String,
    val severity: IngredientFlagSeverity,
    val explanationAr: String
)

data class FoodIngredientAuditResult(
    val productNameAr: String,
    val cleanScore: Int, // 0..10
    val cleanGradeAr: String,
    val novaGroup: Int, // 1..4 (NOVA food processing classification)
    val hasHydrogenatedFats: Boolean,
    val hasHiddenSugars: Boolean,
    val hasHighSodium: Boolean,
    val hasPreservativesOrAdditives: Boolean,
    val summaryAr: String,
    val flags: List<IngredientAuditFlag>,
    val detectedIngredientsAr: List<String>,
    val cleanerAlternativeAr: String
)

// ============================================================================
// V3 Feature 2: Smart Reverse Meal Builder ("فاضلي X سعر و Y بروتين — آكل إيه؟")
// ============================================================================

enum class ReverseFoodRole {
    PROTEIN_ANCHOR,
    CARB_FILLER,
    FAT_BALANCER,
    MIXED
}

data class FridgeIngredient(
    val id: String,
    val name: String,
    val food: FoodItem,
    val role: ReverseFoodRole,
    val isUserHabit: Boolean,
    val habitCount: Int = 0
)

data class BuiltReverseMealItem(
    val food: FoodItem,
    val grams: Double,
    val kcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)

data class ReverseMealPlan(
    val titleAr: String,
    val items: List<BuiltReverseMealItem>,
    val totalKcal: Int,
    val totalProteinG: Int,
    val totalCarbsG: Int,
    val totalFatG: Int,
    val totalLeucineG: Double,
    val kcalMatchPct: Int,
    val proteinMatchPct: Int,
    val tipAr: String
)

object ReverseMealBuilderEngine {

    private fun vec(
        kcal: Double,
        p: Double,
        f: Double,
        c: Double,
        fib: Double = 0.0,
        leu: Double = p * 0.085,
        k: Double = -1.0,
        ca: Double = -1.0,
        fe: Double = -1.0,
        mg: Double = -1.0,
        o3: Double = -1.0
    ): List<Double> = NutritionConstants.vectorFromMap(
        mapOf(
            "kcal" to kcal, "p" to p, "f" to f, "c" to c, "fib" to fib,
            "leu" to leu, "k" to k, "ca" to ca, "fe" to fe, "mg" to mg, "o3" to o3
        )
    )

    private val DEFAULT_FRIDGE_STAPLES: List<FoodItem> = listOf(
        FoodItem(
            id = "rev_qareesh", name = "جبنة قريش مصرية", englishName = "Cottage Cheese",
            source = "e", nutrients = vec(98.0, 14.0, 3.2, 3.4, leu = 1.35, ca = 110.0, k = 104.0)
        ),
        FoodItem(
            id = "rev_eggs", name = "بيض مسلوق", englishName = "Boiled Eggs",
            source = "u", nutrients = vec(155.0, 12.6, 10.6, 1.1, leu = 1.08, fe = 1.2, ca = 50.0)
        ),
        FoodItem(
            id = "rev_tuna", name = "تونة مصفاة", englishName = "Drained Tuna",
            source = "u", nutrients = vec(116.0, 25.5, 0.8, 0.0, leu = 2.1, k = 237.0, o3 = 0.27)
        ),
        FoodItem(
            id = "rev_chicken", name = "صدور فراخ مشوية", englishName = "Grilled Chicken Breast",
            source = "u", nutrients = vec(165.0, 31.0, 3.6, 0.0, leu = 2.6, k = 256.0, mg = 29.0)
        ),
        FoodItem(
            id = "rev_yogurt", name = "زبادي يوناني لايت", englishName = "Greek Yogurt",
            source = "u", nutrients = vec(73.0, 10.0, 1.5, 4.0, leu = 1.0, ca = 115.0)
        ),
        FoodItem(
            id = "rev_whey", name = "واي بروتين (Whey)", englishName = "Whey Protein",
            source = "s", nutrients = vec(385.0, 78.0, 4.5, 8.0, leu = 8.5, ca = 400.0)
        ),
        FoodItem(
            id = "rev_baladi", name = "عيش بلدي مصري", englishName = "Baladi Bread",
            source = "e", nutrients = vec(265.0, 8.8, 1.5, 53.0, fib = 5.5, fe = 2.8, mg = 58.0)
        ),
        FoodItem(
            id = "rev_oats", name = "شوفان جاف", englishName = "Rolled Oats",
            source = "u", nutrients = vec(379.0, 13.2, 6.5, 67.7, fib = 10.1, mg = 138.0, fe = 4.3)
        ),
        FoodItem(
            id = "rev_rice", name = "رز أبيض مطبوخ", englishName = "Cooked White Rice",
            source = "e", nutrients = vec(130.0, 2.7, 0.3, 28.2, fib = 0.4, k = 35.0)
        ),
        FoodItem(
            id = "rev_potato", name = "بطاطس / بطاطا مشوية", englishName = "Baked Potato",
            source = "e", nutrients = vec(90.0, 2.0, 0.2, 20.5, fib = 2.5, k = 420.0)
        ),
        FoodItem(
            id = "rev_banana", name = "موز", englishName = "Banana",
            source = "u", nutrients = vec(89.0, 1.1, 0.3, 22.8, fib = 2.6, k = 358.0, mg = 27.0)
        ),
        FoodItem(
            id = "rev_peanuts", name = "فول سوداني / زبدة سوداني", englishName = "Peanuts",
            source = "e", nutrients = vec(585.0, 23.7, 49.7, 21.5, fib = 8.0, mg = 176.0)
        ),
        FoodItem(
            id = "rev_olive_oil", name = "زيت زيتون بكر", englishName = "Olive Oil",
            source = "u", nutrients = vec(884.0, 0.0, 100.0, 0.0, o3 = 0.76)
        )
    )

    private fun classifyRole(nutrients: List<Double>): ReverseFoodRole {
        val kcal = (nutrients.getOrNull(0) ?: 0.0).coerceAtLeast(1.0)
        val p = (nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["p"] ?: 1) ?: 0.0).coerceAtLeast(0.0)
        val f = (nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["f"] ?: 2) ?: 0.0).coerceAtLeast(0.0)
        val c = (nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["c"] ?: 3) ?: 0.0).coerceAtLeast(0.0)
        val pShare = (p * 4.0) / kcal
        val cShare = (c * 4.0) / kcal
        val fShare = (f * 9.0) / kcal
        return when {
            pShare >= 0.34 || p >= 12.0 && pShare >= 0.28 -> ReverseFoodRole.PROTEIN_ANCHOR
            cShare >= 0.50 -> ReverseFoodRole.CARB_FILLER
            fShare >= 0.55 -> ReverseFoodRole.FAT_BALANCER
            else -> ReverseFoodRole.MIXED
        }
    }

    /**
     * Scans the user's last 30 days of meal logs + favorite/custom foods to extract
     * what the user actually eats regularly ("حاجاتك المعتادة"), then merges everyday fridge staples.
     */
    fun extractHabitAndFridgePool(
        nutritionDays: Map<String, NutritionDay>,
        customFoods: List<CustomFood>
    ): List<FridgeIngredient> {
        val today = todayIso()
        val cutoff = shiftIsoDate(today, -30)

        data class HabitAcc(val food: FoodItem, var count: Int)
        val habitMap = linkedMapOf<String, HabitAcc>()

        nutritionDays.values
            .filter { it.date in cutoff..today }
            .forEach { day ->
                day.items.forEach { item ->
                    if (item.mealIndex == NutritionConstants.SUPPLEMENT_MEAL_INDEX ||
                        SupplementCatalog.isUnitBasedLoggedItem(item)
                    ) return@forEach
                    val kcal100 = item.nutrients.getOrNull(0) ?: 0.0
                    if (kcal100 < 25.0) return@forEach
                    val key = item.name.trim().lowercase()
                    val existing = habitMap[key]
                    if (existing != null) {
                        existing.count++
                    } else {
                        habitMap[key] = HabitAcc(
                            food = FoodItem(
                                id = "habit_${key.hashCode()}",
                                name = item.name.trim(),
                                source = item.source,
                                nutrients = LoggedFoodItem.normalizeNutrients(item.nutrients)
                            ),
                            count = 1
                        )
                    }
                }
            }

        customFoods.filter { it.favorite && !SupplementCatalog.isUnitBasedFood(it.toFoodItem()) }.forEach { cf ->
            val fi = cf.toFoodItem()
            val key = fi.name.trim().lowercase()
            if (key !in habitMap && (fi.nutrients.getOrNull(0) ?: 0.0) >= 25.0) {
                habitMap[key] = HabitAcc(fi, 2)
            }
        }

        val out = mutableListOf<FridgeIngredient>()
        val seenNames = mutableSetOf<String>()

        habitMap.values
            .sortedByDescending { it.count }
            .take(8)
            .forEach { acc ->
                val norm = acc.food.name.lowercase()
                seenNames.add(norm)
                out.add(
                    FridgeIngredient(
                        id = acc.food.id,
                        name = acc.food.name,
                        food = acc.food,
                        role = classifyRole(acc.food.nutrients),
                        isUserHabit = true,
                        habitCount = acc.count
                    )
                )
            }

        DEFAULT_FRIDGE_STAPLES.forEach { staple ->
            val shortKey = staple.name.take(7).lowercase()
            if (seenNames.none { it.contains(shortKey) }) {
                seenNames.add(staple.name.lowercase())
                out.add(
                    FridgeIngredient(
                        id = staple.id,
                        name = staple.name,
                        food = staple,
                        role = classifyRole(staple.nutrients),
                        isUserHabit = false
                    )
                )
            }
        }
        return out
    }

    /**
     * Offline instant solver: computes 1 to 2 exact gram-weighted meals from the user's
     * selected habit/fridge foods that close the remaining calorie & protein ring ~95-100%.
     */
    fun buildInstantPlans(
        remKcal: Int,
        remProteinG: Int,
        remCarbsG: Int,
        remFatG: Int,
        selectedPool: List<FridgeIngredient>
    ): List<ReverseMealPlan> {
        if (remKcal < 60 && remProteinG < 8) return emptyList()
        if (selectedPool.isEmpty()) return emptyList()

        val proteins = selectedPool
            .filter { it.role == ReverseFoodRole.PROTEIN_ANCHOR }
            .ifEmpty { selectedPool.sortedByDescending { it.food.nutrients.getOrNull(1) ?: 0.0 }.take(2) }
        val carbs = selectedPool
            .filter { it.role == ReverseFoodRole.CARB_FILLER || it.role == ReverseFoodRole.MIXED }
            .ifEmpty { DEFAULT_FRIDGE_STAPLES.filter { classifyRole(it.nutrients) == ReverseFoodRole.CARB_FILLER }.map { FridgeIngredient(it.id, it.name, it, ReverseFoodRole.CARB_FILLER, false) } }
        val fats = selectedPool
            .filter { it.role == ReverseFoodRole.FAT_BALANCER }

        fun makeItem(food: FoodItem, grams: Double): BuiltReverseMealItem {
            val g = ((grams / 5.0).roundToInt() * 5.0).coerceAtLeast(5.0)
            val factor = g / 100.0
            val kcal = ((food.nutrients.getOrNull(0) ?: 0.0).coerceAtLeast(0.0) * factor).roundToInt()
            val p = ((food.nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["p"] ?: 1) ?: 0.0).coerceAtLeast(0.0) * factor)
            val f = ((food.nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["f"] ?: 2) ?: 0.0).coerceAtLeast(0.0) * factor)
            val c = ((food.nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["c"] ?: 3) ?: 0.0).coerceAtLeast(0.0) * factor)
            return BuiltReverseMealItem(food, g, kcal, p, c, f)
        }

        fun solveWith(anchor: FridgeIngredient, carbOpt: FridgeIngredient?, fatOpt: FridgeIngredient?, optionTitle: String): ReverseMealPlan? {
            val chosen = mutableListOf<BuiltReverseMealItem>()
            val anchorP100 = (anchor.food.nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["p"] ?: 1) ?: 0.0).coerceAtLeast(1.0)
            val anchorKcal100 = (anchor.food.nutrients.getOrNull(0) ?: 100.0).coerceAtLeast(1.0)

            // Reserve ~15% of protein for secondary carb foods if carb calories are substantial
            val targetAnchorProtein = if (remCarbsG >= 25 && carbOpt != null) remProteinG * 0.85 else remProteinG.toDouble()
            val maxGramsByKcal = (remKcal.coerceAtLeast(100) * 0.82 / anchorKcal100) * 100.0
            val desiredGramsByProtein = if (remProteinG >= 8) (targetAnchorProtein / anchorP100) * 100.0 else (remKcal * 0.5 / anchorKcal100) * 100.0
            val maxCap = if (anchor.name.contains("واي", ignoreCase = true) || anchor.name.contains("whey", ignoreCase = true)) 65.0 else 320.0
            val anchorGrams = min(min(desiredGramsByProtein, maxGramsByKcal), maxCap).coerceAtLeast(25.0)
            val anchorItem = makeItem(anchor.food, anchorGrams)
            chosen.add(anchorItem)

            var leftKcal = (remKcal - anchorItem.kcal).coerceAtLeast(0)
            var leftCarbs = (remCarbsG - anchorItem.carbsG).coerceAtLeast(0.0)
            var leftFat = (remFatG - anchorItem.fatG).coerceAtLeast(0.0)

            // Add carb filler if remaining calories/carbs warrant it
            if (carbOpt != null && carbOpt.id != anchor.id && leftKcal >= 55 && (leftCarbs >= 8.0 || leftKcal >= 120)) {
                val c100 = (carbOpt.food.nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["c"] ?: 3) ?: 20.0).coerceAtLeast(5.0)
                val k100 = (carbOpt.food.nutrients.getOrNull(0) ?: 120.0).coerceAtLeast(20.0)
                val gramsByCarbs = if (leftCarbs >= 8.0) (leftCarbs / c100) * 100.0 else (leftKcal * 0.7 / k100) * 100.0
                val gramsByKcal = (leftKcal * 0.92 / k100) * 100.0
                val carbGrams = min(gramsByCarbs, gramsByKcal).coerceIn(20.0, 350.0)
                if (carbGrams >= 20.0) {
                    val cItem = makeItem(carbOpt.food, carbGrams)
                    chosen.add(cItem)
                    leftKcal = (leftKcal - cItem.kcal).coerceAtLeast(0)
                    leftFat = (leftFat - cItem.fatG).coerceAtLeast(0.0)
                }
            }

            // Add fat balancer if calories & fat remain
            if (fatOpt != null && chosen.none { it.food.id == fatOpt.id } && leftKcal >= 55 && leftFat >= 4.0) {
                val f100 = (fatOpt.food.nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["f"] ?: 2) ?: 50.0).coerceAtLeast(10.0)
                val k100 = (fatOpt.food.nutrients.getOrNull(0) ?: 500.0).coerceAtLeast(100.0)
                val gramsByFat = (leftFat / f100) * 100.0
                val gramsByKcal = (leftKcal / k100) * 100.0
                val fatGrams = min(gramsByFat, gramsByKcal).coerceIn(5.0, 60.0)
                if (fatGrams >= 5.0) {
                    chosen.add(makeItem(fatOpt.food, fatGrams))
                }
            }

            val totKcal = chosen.sumOf { it.kcal }
            val totP = chosen.sumOf { it.proteinG }.roundToInt()
            val totC = chosen.sumOf { it.carbsG }.roundToInt()
            val totF = chosen.sumOf { it.fatG }.roundToInt()
            val totLeu = chosen.sumOf { item ->
                val leuIdx = NutritionConstants.NUTRIENT_INDEX["leu"] ?: -1
                val raw = if (leuIdx >= 0) item.food.nutrients.getOrNull(leuIdx) ?: -1.0 else -1.0
                if (raw >= 0.0) raw * item.grams / 100.0 else item.proteinG * 0.085
            }

            val kcalMatch = if (remKcal > 0) (100 - (abs(totKcal - remKcal) * 100.0 / remKcal)).roundToInt().coerceIn(0, 100) else 100
            val proMatch = if (remProteinG > 0) (100 - (abs(totP - remProteinG) * 100.0 / remProteinG)).roundToInt().coerceIn(0, 100) else 100
            val habitTag = if (chosen.any { c -> selectedPool.any { it.food.id == c.food.id && it.isUserHabit } }) {
                "مبنية من أكلك المعتاد في آخر 30 يوم"
            } else {
                "مبنية من مكونات تلاجتك المختارة"
            }

            return ReverseMealPlan(
                titleAr = optionTitle,
                items = chosen,
                totalKcal = totKcal,
                totalProteinG = totP,
                totalCarbsG = totC,
                totalFatG = totF,
                totalLeucineG = (totLeu * 10.0).roundToInt() / 10.0,
                kcalMatchPct = kcalMatch,
                proteinMatchPct = proMatch,
                tipAr = "$habitTag · تقفيل السعرات $kcalMatch% والبروتين $proMatch%."
            )
        }

        val plans = mutableListOf<ReverseMealPlan>()
        val firstAnchor = proteins.firstOrNull() ?: return emptyList()
        val firstCarb = carbs.firstOrNull { it.id != firstAnchor.id }
        val firstFat = fats.firstOrNull { it.id != firstAnchor.id && it.id != firstCarb?.id }
        solveWith(firstAnchor, firstCarb, firstFat, "اختيار 1: ${firstAnchor.name}" + (firstCarb?.let { " + ${it.name}" } ?: ""))?.let {
            plans.add(it)
        }

        val secondAnchor = proteins.getOrNull(1)
        val secondCarb = carbs.getOrNull(1)?.takeIf { it.id != secondAnchor?.id } ?: firstCarb
        val secondFat = fats.getOrNull(1) ?: firstFat
        if (secondAnchor != null) {
            solveWith(secondAnchor, secondCarb, secondFat, "اختيار 2: ${secondAnchor.name}" + (secondCarb?.let { " + ${it.name}" } ?: ""))?.let {
                plans.add(it)
            }
        }
        return plans
    }
}

// ============================================================================
// Smart Weekly Grocery List Generator ("قائمة المشتريات الأسبوعية")
// ============================================================================

enum class GroceryCategory(val labelAr: String, val emoji: String) {
    PROTEIN("بروتين", "🥩"),
    DAIRY("ألبان وبيض", "🥛"),
    CARBS("كارب وحبوب", "🍚"),
    HEALTHY_FATS("دهون صحية ومكسرات", "🥑")
}

data class GroceryListItem(
    val id: String,
    val name: String,
    val category: GroceryCategory,
    val weeklyGrams: Int,
    val displayQtyAr: String,
    val isFromUserHistory: Boolean
)

object WeeklyGroceryListEngine {

    private fun classifyGroceryCategory(name: String, nutrients: List<Double>): GroceryCategory {
        val lower = name.lowercase()
        val isDairyOrEggs = listOf("جبن", "قريش", "زبادي", "لبن", "حليب", "بيض", "egg", "yogurt", "cheese", "milk")
            .any { lower.contains(it) }
        if (isDairyOrEggs) return GroceryCategory.DAIRY

        val kcal = (nutrients.getOrNull(0) ?: 100.0).coerceAtLeast(1.0)
        val p = (nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["p"] ?: 1) ?: 0.0).coerceAtLeast(0.0)
        val f = (nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["f"] ?: 2) ?: 0.0).coerceAtLeast(0.0)
        val c = (nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["c"] ?: 3) ?: 0.0).coerceAtLeast(0.0)
        val pShare = (p * 4.0) / kcal
        val fShare = (f * 9.0) / kcal
        val cShare = (c * 4.0) / kcal

        return when {
            pShare >= 0.30 || p >= 14.0 -> GroceryCategory.PROTEIN
            fShare >= 0.48 -> GroceryCategory.HEALTHY_FATS
            cShare >= 0.42 -> GroceryCategory.CARBS
            else -> GroceryCategory.CARBS
        }
    }

    private fun formatWeeklyQuantity(grams: Int): String {
        val rounded = ((grams / 50.0).roundToInt() * 50).coerceAtLeast(100)
        return if (rounded >= 1000) {
            val kg = rounded / 1000.0
            if (kg % 1.0 == 0.0) "${kg.toInt()} كجم" else "${String.format("%.1f", kg)} كجم"
        } else {
            "$rounded جم"
        }
    }

    /**
     * Aggregates recurring ingredients from the user's last 14 days of nutrition logs
     * (projected to a 7-day shopping list) + favorite foods, with balanced Egyptian defaults
     * if history is still sparse.
     */
    fun generate(
        nutritionDays: Map<String, NutritionDay>,
        customFoods: List<CustomFood>
    ): List<GroceryListItem> {
        val today = todayIso()
        val cutoff = shiftIsoDate(today, -14)

        data class Acc(val name: String, val nutrients: List<Double>, var totalGrams: Double, var occurrences: Int)
        val map = linkedMapOf<String, Acc>()
        val activeDays = nutritionDays.values.filter { it.date in cutoff..today && it.items.isNotEmpty() }

        activeDays.forEach { day ->
            day.items.forEach { item ->
                if (item.mealIndex == NutritionConstants.SUPPLEMENT_MEAL_INDEX ||
                    SupplementCatalog.isUnitBasedLoggedItem(item)
                ) return@forEach
                if ((item.nutrients.getOrNull(0) ?: 0.0) < 25.0) return@forEach
                val key = item.name.trim().lowercase()
                val existing = map[key]
                if (existing != null) {
                    existing.totalGrams += item.grams
                    existing.occurrences++
                } else {
                    map[key] = Acc(
                        name = item.name.trim(),
                        nutrients = LoggedFoodItem.normalizeNutrients(item.nutrients),
                        totalGrams = item.grams,
                        occurrences = 1
                    )
                }
            }
        }

        val scaleTo7Days = if (activeDays.isNotEmpty()) (7.0 / activeDays.size.coerceIn(1, 14)).coerceIn(0.5, 2.5) else 1.0
        val out = mutableListOf<GroceryListItem>()
        val seen = mutableSetOf<String>()

        map.values
            .sortedByDescending { it.occurrences * 1000.0 + it.totalGrams }
            .take(14)
            .forEach { acc ->
                seen.add(acc.name.lowercase())
                val weeklyG = (acc.totalGrams * scaleTo7Days).roundToInt().coerceIn(150, 5000)
                out.add(
                    GroceryListItem(
                        id = "groc_${acc.name.hashCode()}",
                        name = acc.name,
                        category = classifyGroceryCategory(acc.name, acc.nutrients),
                        weeklyGrams = weeklyG,
                        displayQtyAr = formatWeeklyQuantity(weeklyG),
                        isFromUserHistory = true
                    )
                )
            }

        // Fill with favorite custom foods if under 8 items
        if (out.size < 8) {
            customFoods.filter { it.favorite && !SupplementCatalog.isUnitBasedFood(it.toFoodItem()) }.forEach { cf ->
                val fi = cf.toFoodItem()
                if (fi.name.lowercase() !in seen && out.size < 12) {
                    seen.add(fi.name.lowercase())
                    out.add(
                        GroceryListItem(
                            id = "groc_fav_${fi.id}",
                            name = fi.name,
                            category = classifyGroceryCategory(fi.name, fi.nutrients),
                            weeklyGrams = 500,
                            displayQtyAr = "500 جم",
                            isFromUserHistory = true
                        )
                    )
                }
            }
        }

        // Ensure every category has helpful Egyptian staples if history is sparse
        val fallbackStaples = listOf(
            Triple("صدور فراخ", GroceryCategory.PROTEIN, 1500),
            Triple("تونة مصفاة", GroceryCategory.PROTEIN, 600),
            Triple("جبنة قريش", GroceryCategory.DAIRY, 1000),
            Triple("بيض (طبق)", GroceryCategory.DAIRY, 1000),
            Triple("زبادي يوناني لايت", GroceryCategory.DAIRY, 700),
            Triple("أرز أبيض / بسمتي", GroceryCategory.CARBS, 1000),
            Triple("شوفان كامل", GroceryCategory.CARBS, 500),
            Triple("عيش بلدي", GroceryCategory.CARBS, 700),
            Triple("زيت زيتون بكر", GroceryCategory.HEALTHY_FATS, 250),
            Triple("فول سوداني / زبدة سوداني", GroceryCategory.HEALTHY_FATS, 300)
        )
        if (out.size < 6) {
            fallbackStaples.forEach { (name, cat, grams) ->
                val key = name.take(6).lowercase()
                if (seen.none { it.contains(key) }) {
                    seen.add(name.lowercase())
                    out.add(
                        GroceryListItem(
                            id = "groc_def_${name.hashCode()}",
                            name = name,
                            category = cat,
                            weeklyGrams = grams,
                            displayQtyAr = formatWeeklyQuantity(grams),
                            isFromUserHistory = false
                        )
                    )
                }
            }
        }

        return out.sortedBy { it.category.ordinal }
    }

    fun formatShareText(items: List<GroceryListItem>, checkedIds: Set<String>): String {
        val remaining = items.filterNot { it.id in checkedIds }
        val listToShare = remaining.ifEmpty { items }
        val grouped = listToShare.groupBy { it.category }
        return buildString {
            appendLine("🛒 قائمة مشتريات الأسبوع (متابعي):")
            appendLine()
            GroceryCategory.entries.forEach { cat ->
                val catItems = grouped[cat].orEmpty()
                if (catItems.isNotEmpty()) {
                    appendLine("${cat.emoji} *${cat.labelAr}*:")
                    catItems.forEach { item ->
                        appendLine("⬜ ${item.name} — ${item.displayQtyAr}")
                    }
                    appendLine()
                }
            }
        }.trim()
    }
}

