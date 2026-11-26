package com.mutabei.workout.data.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// ============================================================================
// 1. USDA ARS & EuroFIR Cooking Yield Factors (Raw <-> Cooked Auto-Conversion)
//    Formula: Cooked Weight = Raw Weight * Yield Factor
// ============================================================================

enum class CookedWeightMode(val labelAr: String) {
    RAW("نيء / جاف"),
    COOKED("مطبوخ")
}

data class CookingYieldProfile(
    val id: String,
    val categoryAr: String,
    val yieldFactor: Double,
    val sourceRef: String,
    val keywords: List<String>
)

object CookingYieldEngine {

    private val PROFILES: List<CookingYieldProfile> = listOf(
        CookingYieldProfile(
            id = "white_rice",
            categoryAr = "أرز أبيض / بسمتي",
            yieldFactor = 2.65,
            sourceRef = "USDA ARS (2.5–2.8×)",
            keywords = listOf("رز", "أرز", "ارز", "بسمتي", "rice", "basmati")
        ),
        CookingYieldProfile(
            id = "brown_rice",
            categoryAr = "أرز بني",
            yieldFactor = 2.40,
            sourceRef = "USDA ARS",
            keywords = listOf("رز بني", "أرز بني", "brown rice")
        ),
        CookingYieldProfile(
            id = "pasta",
            categoryAr = "مكرونة / باستا",
            yieldFactor = 2.30,
            sourceRef = "USDA ARS / EuroFIR",
            keywords = listOf("مكرون", "معكرون", "سباجيتي", "باستا", "شعرية", "لسان عصفور", "pasta", "macaroni", "spaghetti", "noodle")
        ),
        CookingYieldProfile(
            id = "oats",
            categoryAr = "شوفان",
            yieldFactor = 2.50,
            sourceRef = "USDA ARS",
            keywords = listOf("شوفان", "oats", "oatmeal")
        ),
        CookingYieldProfile(
            id = "legumes",
            categoryAr = "عدس / بقوليات / فاصوليا / حمص",
            yieldFactor = 2.40,
            sourceRef = "USDA ARS",
            keywords = listOf("عدس", "فاصوليا", "لوبيا", "حمص", "فول", "ترمس", "بليلة", "قمح", "lentil", "chickpea", "bean", "fava")
        ),
        CookingYieldProfile(
            id = "grains",
            categoryAr = "فريك / برغل / كينوا",
            yieldFactor = 2.60,
            sourceRef = "EuroFIR / USDA",
            keywords = listOf("فريك", "برغل", "كينوا", "كسكسي", "quinoa", "bulgur", "freekeh", "couscous")
        ),
        CookingYieldProfile(
            id = "chicken_breast",
            categoryAr = "صدور دجاج / فراخ",
            yieldFactor = 0.74,
            sourceRef = "USDA ARS (0.72–0.75×)",
            keywords = listOf("صدر", "صدور", "فراخ", "دجاج", "بانيه", "شيش", "رومي", "chicken", "turkey", "poultry")
        ),
        CookingYieldProfile(
            id = "beef_meat",
            categoryAr = "لحم بقري / كفتة / مفروم",
            yieldFactor = 0.72,
            sourceRef = "USDA ARS",
            keywords = listOf("لحم", "لحمة", "بقري", "كندوز", "بتلو", "ضاني", "ستيك", "كفتة", "مفروم", "برجر", "beef", "steak", "veal", "lamb", "meat")
        ),
        CookingYieldProfile(
            id = "liver",
            categoryAr = "كبدة",
            yieldFactor = 0.74,
            sourceRef = "USDA ARS",
            keywords = listOf("كبدة", "كبده", "كبد", "liver")
        ),
        CookingYieldProfile(
            id = "fish_seafood",
            categoryAr = "سمك / جمبري / سي فود",
            yieldFactor = 0.78,
            sourceRef = "USDA ARS",
            keywords = listOf("سمك", "بلطي", "بوري", "سلمون", "سالمون", "فيليه", "ماكريل", "جمبري", "كاليماري", "fish", "salmon", "tilapia", "shrimp")
        ),
        CookingYieldProfile(
            id = "potatoes",
            categoryAr = "بطاطس / بطاطا",
            yieldFactor = 0.88,
            sourceRef = "EuroFIR / USDA",
            keywords = listOf("بطاطس", "بطاطا", "potato", "sweet potato")
        ),
        CookingYieldProfile(
            id = "mixed_veg",
            categoryAr = "خضار مطبوخ",
            yieldFactor = 0.85,
            sourceRef = "EuroFIR",
            keywords = listOf("بسلة", "كوسة", "بامية", "ملوخية", "سبانخ", "بروكلي", "قرنبيط", "فاصوليا خضراء", "broccoli", "spinach", "zucchini", "peas")
        )
    )

    private val COOKED_MARKERS = listOf(
        "مطبوخ", "مسلوق", "مشوي", "مقلي", "محمر", "في الفرن", "صينية", "طاجن", "شاورما", "كفتة", "بانيه", "مدمس", "بليلة",
        "cooked", "boiled", "grilled", "roasted", "baked", "fried", "steamed", "braised", "stewed"
    )

    private val RAW_MARKERS = listOf(
        "نيء", "نيئ", "ني ", "جاف", "غير مطبوخ", "قبل الطبخ", "خام",
        "raw", "dry", "uncooked", "unprepared"
    )

    fun detectProfile(nameAr: String, nameEn: String = ""): CookingYieldProfile? {
        val hay = "$nameAr $nameEn".lowercase()
        // Exclude canned/ready-to-eat items where raw/cooked toggle makes no sense
        if (listOf("تونة", "tuna", "معلب", "canned", "زبادي", "جبن", "لبن", "حليب", "عيش", "خبز", "توست", "واي", "whey", "كبسول", "قرص").any { hay.contains(it) }) {
            return null
        }
        // Check brown rice before white rice
        return PROFILES.firstOrNull { profile ->
            profile.keywords.any { kw -> hay.contains(kw) }
        }
    }

    /**
     * Infers whether a database entry's nutrient vector is defined per 100g RAW or per 100g COOKED.
     */
    fun detectItemIntrinsicState(nameAr: String, nameEn: String = "", kcalPer100: Double = -1.0): CookedWeightMode {
        val hay = "$nameAr $nameEn".lowercase()
        if (RAW_MARKERS.any { hay.contains(it) }) return CookedWeightMode.RAW
        if (COOKED_MARKERS.any { hay.contains(it) }) return CookedWeightMode.COOKED
        // Calorie density heuristic for grains/pasta/legumes (raw > 300 kcal/100g, cooked ~110-165 kcal/100g)
        val profile = detectProfile(nameAr, nameEn)
        if (profile != null && profile.yieldFactor > 1.5 && kcalPer100 > 0.0) {
            return if (kcalPer100 >= 250.0) CookedWeightMode.RAW else CookedWeightMode.COOKED
        }
        // For meats/chicken: raw chicken breast ~110-125 kcal, cooked ~155-185 kcal
        if (profile?.id == "chicken_breast" && kcalPer100 > 0.0) {
            return if (kcalPer100 <= 135.0) CookedWeightMode.RAW else CookedWeightMode.COOKED
        }
        return CookedWeightMode.COOKED
    }

    /**
     * Converts the weight entered by the user (in [userWeighedMode]) into the equivalent grams
     * matching the food item's intrinsic database basis ([itemIntrinsicMode]).
     * Core equation: Cooked Weight = Raw Weight * Yield Factor.
     */
    fun convertToDbGrams(
        enteredGrams: Double,
        userWeighedMode: CookedWeightMode,
        itemIntrinsicMode: CookedWeightMode,
        yieldFactor: Double
    ): Double {
        if (enteredGrams <= 0.0 || yieldFactor <= 0.0) return enteredGrams
        return when {
            userWeighedMode == itemIntrinsicMode -> enteredGrams
            userWeighedMode == CookedWeightMode.COOKED && itemIntrinsicMode == CookedWeightMode.RAW ->
                // User weighed COOKED, DB item is RAW -> Raw = Cooked / YieldFactor
                enteredGrams / yieldFactor
            else ->
                // User weighed RAW, DB item is COOKED -> Cooked = Raw * YieldFactor
                enteredGrams * yieldFactor
        }
    }

    /**
     * Displays the counterpart weight (Raw -> Cooked or Cooked -> Raw) for instant user clarity.
     */
    fun counterpartWeight(enteredGrams: Double, enteredMode: CookedWeightMode, yieldFactor: Double): Double {
        if (enteredGrams <= 0.0 || yieldFactor <= 0.0) return 0.0
        return if (enteredMode == CookedWeightMode.RAW) {
            enteredGrams * yieldFactor
        } else {
            enteredGrams / yieldFactor
        }
    }
}

// ============================================================================
// 2. Smart Cooking-Method Follow-Up Chips (Solving Hidden Egyptian Fats)
// ============================================================================

enum class CookingMethodPreset(
    val labelAr: String,
    val shortHintAr: String,
    val addedFatPer100g: Double,
    val addedSatFatPer100g: Double,
    val addedCarbsPer100g: Double
) {
    BOILED_GRILLED(
        labelAr = "مسلوق / مشوي",
        shortHintAr = "بدون دهون مضافة",
        addedFatPer100g = 0.0,
        addedSatFatPer100g = 0.0,
        addedCarbsPer100g = 0.0
    ),
    AIR_FRYER_LIGHT(
        labelAr = "زيت خفيف / Air Fryer",
        shortHintAr = "+2.5 جم دهون/100جم",
        addedFatPer100g = 2.5,
        addedSatFatPer100g = 0.4,
        addedCarbsPer100g = 0.0
    ),
    OIL_OR_GHEE(
        labelAr = "معلقة زيت / سمنة",
        shortHintAr = "+6.5 جم دهون/100جم",
        addedFatPer100g = 6.5,
        addedSatFatPer100g = 3.2,
        addedCarbsPer100g = 0.0
    ),
    DEEP_FRIED(
        labelAr = "مقلي",
        shortHintAr = "+12 جم دهون/100جم",
        addedFatPer100g = 12.0,
        addedSatFatPer100g = 3.0,
        addedCarbsPer100g = 4.0
    )
}

object CookingMethodEngine {

    private val NON_COOKED_EXCLUDES = listOf(
        "ماء", "مياه", "سلطة", "خيار", "طماطم", "خس", "تفاح", "موز", "برتقال", "فراولة", "عنب",
        "زبادي", "جبنة قريش", "لبن", "حليب", "واي", "whey", "قهوة", "شاي", "مكمل", "فيتامين",
        "water", "salad", "apple", "banana", "orange", "yogurt", "milk", "coffee", "tea"
    )

    fun shouldShowCookingChips(nameAr: String, nameEn: String = ""): Boolean {
        val hay = "$nameAr $nameEn".lowercase()
        if (NON_COOKED_EXCLUDES.any { hay.contains(it) }) return false
        return true
    }

    fun inferBaselineMethod(nameAr: String, nameEn: String = ""): CookingMethodPreset {
        val hay = "$nameAr $nameEn".lowercase()
        return when {
            listOf("مقلي", "بانيه", "بطاطس محمرة", "طعمية", "فلافل", "fried", "crispy", "pane", "falafel").any { hay.contains(it) } ->
                CookingMethodPreset.DEEP_FRIED
            listOf("سمن", "زبدة", "بالزيت", "بشاميل", "محشي", "شعرية", "صينية", "طاجن", "ghee", "butter", "sauteed").any { hay.contains(it) } ->
                CookingMethodPreset.OIL_OR_GHEE
            listOf("air fryer", "اير فراير", "قلاية هوائية", "زيت خفيف").any { hay.contains(it) } ->
                CookingMethodPreset.AIR_FRYER_LIGHT
            else -> CookingMethodPreset.BOILED_GRILLED
        }
    }

    /**
     * Adjusts fat, saturated fat, carbs, and calories per 100g when user switches cooking-method chip.
     */
    fun applyMethodDelta(
        basePer100: List<Double>,
        baseline: CookingMethodPreset,
        selected: CookingMethodPreset
    ): List<Double> {
        if (baseline == selected) return basePer100
        val out = LoggedFoodItem.normalizeNutrients(basePer100).toMutableList()
        val deltaFat = selected.addedFatPer100g - baseline.addedFatPer100g
        val deltaSat = selected.addedSatFatPer100g - baseline.addedSatFatPer100g
        val deltaCarb = selected.addedCarbsPer100g - baseline.addedCarbsPer100g
        val deltaKcal = deltaFat * 9.0 + deltaCarb * 4.0

        fun adjust(key: String, delta: Double) {
            val idx = NutritionConstants.NUTRIENT_INDEX[key] ?: return
            val cur = out[idx]
            if (cur >= 0.0) {
                out[idx] = max(0.0, cur + delta)
            } else if (delta > 0.0) {
                out[idx] = delta
            }
        }
        adjust("f", deltaFat)
        adjust("sat", deltaSat)
        adjust("c", deltaCarb)
        adjust("kcal", deltaKcal)
        return out
    }
}

// ============================================================================
// 3. Smart Macro-Swapper ("بدّل المكون")
// ============================================================================

enum class SwapMacroCategory(val labelAr: String) {
    PROTEIN("بدائل البروتين"),
    CARB("بدائل الكارب"),
    FAT("بدائل الدهون الصحية")
}

data class MacroSwapOption(
    val food: FoodItem,
    val category: SwapMacroCategory,
    val suggestedGrams: Double,
    val targetKcal: Int,
    val swapKcal: Int,
    val swapProteinG: Double,
    val swapCarbsG: Double,
    val swapFatG: Double,
    val householdHint: String
)

object MacroSwapperEngine {

    private fun vec(
        kcal: Double,
        p: Double,
        f: Double,
        c: Double,
        fib: Double = 0.0,
        leu: Double = p * 0.085,
        sat: Double = f * 0.28,
        k: Double = -1.0,
        fe: Double = -1.0,
        ca: Double = -1.0,
        mg: Double = -1.0,
        zn: Double = -1.0,
        o3: Double = -1.0
    ): List<Double> = NutritionConstants.vectorFromMap(
        mapOf(
            "kcal" to kcal, "p" to p, "f" to f, "c" to c, "fib" to fib,
            "leu" to leu, "sat" to sat, "k" to k, "fe" to fe, "ca" to ca,
            "mg" to mg, "zn" to zn, "o3" to o3
        )
    )

    private data class SwapCatalogEntry(
        val category: SwapMacroCategory,
        val householdUnitLabel: String?,
        val householdUnitGrams: Double?,
        val food: FoodItem
    )

    private val CATALOG: List<SwapCatalogEntry> = listOf(
        // --- PROTEIN ---
        SwapCatalogEntry(
            SwapMacroCategory.PROTEIN, null, null,
            FoodItem(
                id = "swap_chicken_breast", name = "صدور فراخ مشوية", englishName = "Grilled Chicken Breast",
                source = "u", nutrients = vec(165.0, 31.0, 3.6, 0.0, leu = 2.6, k = 256.0, mg = 29.0, zn = 1.0, fe = 1.0)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.PROTEIN, "علبة مصفاة (~130 جم)", 130.0,
            FoodItem(
                id = "swap_tuna_drained", name = "تونة مصفاة (قطعة واحدة)", englishName = "Canned Tuna in Water Drained",
                source = "u", nutrients = vec(116.0, 25.5, 0.8, 0.0, leu = 2.1, k = 237.0, mg = 27.0, fe = 1.5, o3 = 0.27)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.PROTEIN, null, null,
            FoodItem(
                id = "swap_cottage_cheese", name = "جبنة قريش مصرية", englishName = "Egyptian Qareesh Cottage Cheese",
                source = "e", nutrients = vec(98.0, 14.0, 3.2, 3.4, leu = 1.35, ca = 110.0, k = 104.0, zn = 0.6)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.PROTEIN, "بيضة (~55 جم)", 55.0,
            FoodItem(
                id = "swap_boiled_eggs", name = "بيض مسلوق", englishName = "Hard Boiled Egg",
                source = "u", nutrients = vec(155.0, 12.6, 10.6, 1.1, leu = 1.08, fe = 1.2, ca = 50.0, zn = 1.1)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.PROTEIN, null, null,
            FoodItem(
                id = "swap_lean_beef", name = "لحم بقري أحمر مشوي", englishName = "Lean Grilled Beef",
                source = "u", nutrients = vec(195.0, 29.0, 8.0, 0.0, leu = 2.4, fe = 2.6, zn = 5.5, k = 340.0)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.PROTEIN, null, null,
            FoodItem(
                id = "swap_tilapia", name = "سمك بلطي مشوي", englishName = "Grilled Tilapia Fish",
                source = "e", nutrients = vec(128.0, 26.2, 2.7, 0.0, leu = 2.1, k = 380.0, mg = 34.0, o3 = 0.24)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.PROTEIN, "سكوب (30 جم)", 30.0,
            FoodItem(
                id = "swap_whey", name = "واي بروتين (Whey)", englishName = "Whey Protein Powder",
                source = "s", nutrients = vec(385.0, 78.0, 4.5, 8.0, leu = 8.5, ca = 400.0)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.PROTEIN, null, null,
            FoodItem(
                id = "swap_greek_yogurt", name = "زبادي يوناني لايت", englishName = "Greek Yogurt Low Fat",
                source = "u", nutrients = vec(73.0, 10.0, 1.5, 4.0, leu = 1.0, ca = 115.0, k = 141.0)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.PROTEIN, null, null,
            FoodItem(
                id = "swap_liver", name = "كبدة بقري مشوية", englishName = "Grilled Beef Liver",
                source = "e", nutrients = vec(175.0, 26.5, 4.7, 5.1, leu = 2.3, fe = 6.5, zn = 5.3)
            )
        ),
        // --- CARB ---
        SwapCatalogEntry(
            SwapMacroCategory.CARB, null, null,
            FoodItem(
                id = "swap_cooked_rice", name = "رز أبيض مطبوخ", englishName = "Cooked White Rice",
                source = "e", nutrients = vec(130.0, 2.7, 0.3, 28.2, fib = 0.4, k = 35.0, mg = 12.0)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.CARB, null, null,
            FoodItem(
                id = "swap_cooked_pasta", name = "مكرونة مسلوقة", englishName = "Cooked Pasta",
                source = "u", nutrients = vec(157.0, 5.8, 0.9, 30.9, fib = 1.8, k = 44.0, mg = 18.0)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.CARB, null, null,
            FoodItem(
                id = "swap_boiled_potato", name = "بطاطس مسلوقة / مشوية", englishName = "Boiled Potato",
                source = "e", nutrients = vec(87.0, 1.9, 0.1, 20.1, fib = 1.8, k = 379.0, mg = 22.0)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.CARB, null, null,
            FoodItem(
                id = "swap_sweet_potato", name = "بطاطا حلوة مشوية", englishName = "Baked Sweet Potato",
                source = "e", nutrients = vec(90.0, 2.0, 0.2, 20.7, fib = 3.3, k = 475.0, mg = 27.0)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.CARB, "رغيف (~90 جم)", 90.0,
            FoodItem(
                id = "swap_baladi_bread", name = "عيش بلدي مصري", englishName = "Egyptian Baladi Bread",
                source = "e", nutrients = vec(265.0, 8.8, 1.5, 53.0, fib = 5.5, fe = 2.8, mg = 58.0)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.CARB, null, null,
            FoodItem(
                id = "swap_dry_oats", name = "شوفان جاف", englishName = "Dry Rolled Oats",
                source = "u", nutrients = vec(379.0, 13.2, 6.5, 67.7, fib = 10.1, mg = 138.0, fe = 4.3, k = 362.0)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.CARB, null, null,
            FoodItem(
                id = "swap_ful", name = "فول مدمس ساده", englishName = "Cooked Fava Beans",
                source = "e", nutrients = vec(110.0, 7.6, 0.4, 19.6, fib = 5.4, fe = 1.5, mg = 43.0, k = 268.0)
            )
        ),
        // --- FAT ---
        SwapCatalogEntry(
            SwapMacroCategory.FAT, "معلقة كبيرة (14 جم)", 14.0,
            FoodItem(
                id = "swap_olive_oil", name = "زيت زيتون بكر", englishName = "Extra Virgin Olive Oil",
                source = "u", nutrients = vec(884.0, 0.0, 100.0, 0.0, sat = 13.8, o3 = 0.76)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.FAT, null, null,
            FoodItem(
                id = "swap_peanuts", name = "فول سوداني محمص", englishName = "Roasted Peanuts",
                source = "e", nutrients = vec(585.0, 23.7, 49.7, 21.5, fib = 8.0, mg = 176.0, k = 658.0)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.FAT, "معلقة (16 جم)", 16.0,
            FoodItem(
                id = "swap_peanut_butter", name = "زبدة فول سوداني طبيعية", englishName = "Natural Peanut Butter",
                source = "u", nutrients = vec(598.0, 22.2, 51.4, 22.3, fib = 5.0, mg = 168.0, k = 558.0)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.FAT, null, null,
            FoodItem(
                id = "swap_almonds", name = "لوز نيء", englishName = "Raw Almonds",
                source = "u", nutrients = vec(579.0, 21.2, 49.9, 21.6, fib = 12.5, mg = 270.0, ca = 269.0)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.FAT, "معلقة (15 جم)", 15.0,
            FoodItem(
                id = "swap_tahini", name = "طحينة سمسم", englishName = "Sesame Tahini",
                source = "e", nutrients = vec(595.0, 17.0, 53.8, 21.2, fib = 9.3, ca = 426.0, mg = 95.0, fe = 8.9)
            )
        ),
        SwapCatalogEntry(
            SwapMacroCategory.FAT, null, null,
            FoodItem(
                id = "swap_avocado", name = "أفوكادو", englishName = "Avocado",
                source = "u", nutrients = vec(160.0, 2.0, 14.7, 8.5, fib = 6.7, k = 485.0, mg = 29.0)
            )
        )
    )

    fun detectPrimaryCategory(item: LoggedFoodItem): SwapMacroCategory {
        val pCal = item.nutrientAmount("p") * 4.0
        val cCal = item.nutrientAmount("c") * 4.0
        val fCal = item.nutrientAmount("f") * 9.0
        return when {
            pCal >= cCal && pCal >= fCal -> SwapMacroCategory.PROTEIN
            cCal >= pCal && cCal >= fCal -> SwapMacroCategory.CARB
            else -> SwapMacroCategory.FAT
        }
    }

    fun generateSwaps(item: LoggedFoodItem): List<MacroSwapOption> {
        val category = detectPrimaryCategory(item)
        val targetKcal = item.nutrientAmount("kcal").coerceAtLeast(10.0)
        val targetP = item.nutrientAmount("p").coerceAtLeast(0.0)
        val targetC = item.nutrientAmount("c").coerceAtLeast(0.0)
        val targetF = item.nutrientAmount("f").coerceAtLeast(0.0)
        val normName = item.name.trim().lowercase()

        return CATALOG
            .filter { it.category == category }
            .filterNot { normName.contains(it.food.name.take(8).lowercase()) }
            .mapNotNull { entry ->
                val p100 = entry.food.nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["p"] ?: 1)?.coerceAtLeast(0.0) ?: 0.0
                val c100 = entry.food.nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["c"] ?: 3)?.coerceAtLeast(0.0) ?: 0.0
                val f100 = entry.food.nutrients.getOrNull(NutritionConstants.NUTRIENT_INDEX["f"] ?: 2)?.coerceAtLeast(0.0) ?: 0.0
                val kcal100 = entry.food.nutrients.getOrNull(0)?.coerceAtLeast(1.0) ?: 1.0

                val rawGrams = when (category) {
                    SwapMacroCategory.PROTEIN -> if (targetP >= 3.0 && p100 > 1.0) (targetP / p100) * 100.0 else (targetKcal / kcal100) * 100.0
                    SwapMacroCategory.CARB -> if (targetC >= 5.0 && c100 > 1.0) (targetC / c100) * 100.0 else (targetKcal / kcal100) * 100.0
                    SwapMacroCategory.FAT -> if (targetF >= 2.0 && f100 > 1.0) (targetF / f100) * 100.0 else (targetKcal / kcal100) * 100.0
                }
                val roundedGrams = ((rawGrams / 5.0).roundToInt() * 5.0).coerceIn(10.0, 800.0)
                val factor = roundedGrams / 100.0
                val hint = if (entry.householdUnitLabel != null && entry.householdUnitGrams != null && entry.householdUnitGrams > 0.0) {
                    val units = (roundedGrams / entry.householdUnitGrams * 10.0).roundToInt() / 10.0
                    "≈ $units × ${entry.householdUnitLabel}"
                } else {
                    "${roundedGrams.roundToInt()} جم"
                }

                MacroSwapOption(
                    food = entry.food,
                    category = category,
                    suggestedGrams = roundedGrams,
                    targetKcal = targetKcal.roundToInt(),
                    swapKcal = (kcal100 * factor).roundToInt(),
                    swapProteinG = ((p100 * factor) * 10.0).roundToInt() / 10.0,
                    swapCarbsG = ((c100 * factor) * 10.0).roundToInt() / 10.0,
                    swapFatG = ((f100 * factor) * 10.0).roundToInt() / 10.0,
                    householdHint = hint
                )
            }
            .sortedBy { abs(it.swapKcal - it.targetKcal) }
            .take(6)
    }
}

// ============================================================================
// 4. 7-Day Rolling Calorie Budget (Weekly Flexibility — No "Red Day" Guilt)
// ============================================================================

data class RollingCalorieBudget(
    val baseDailyTarget: Int,
    val adjustedTodayTarget: Int,
    val deltaTodayKcal: Int,
    val todayConsumedKcal: Int,
    val weeklyTargetKcal: Int,
    val weeklyConsumedKcal: Int,
    val pastLoggedDaysCount: Int,
    val pastBalanceKcal: Int, // positive = over budget in past 6 days, negative = banked calories
    val todaySurplusKcal: Int, // > 0 if today exceeded adjustedTodayTarget
    val perDaySpreadNext5Days: Int, // gentle daily trim over next 5 days if today had a surplus
    val badgeAr: String,
    val explanationAr: String,
    val isSupportiveMode: Boolean
)

object RollingCalorieBudgetEngine {

    private const val MIN_SAFE_DAILY_KCAL = 1200
    private const val MAX_DAILY_REDISTRIBUTION = 280
    private const val SPREAD_DAYS = 5

    fun evaluate(
        referenceDate: String,
        baseDailyTarget: Int,
        nutritionDays: Map<String, NutritionDay>
    ): RollingCalorieBudget {
        val safeBase = baseDailyTarget.coerceAtLeast(MIN_SAFE_DAILY_KCAL)
        val todayKcal = (nutritionDays[referenceDate]?.items?.sumOf { it.nutrientAmount("kcal") } ?: 0.0).roundToInt()

        var pastLoggedCount = 0
        var pastConsumedSum = 0
        var pastExpectedSum = 0
        for (offset in 1..6) {
            val dIso = shiftIsoDate(referenceDate, -offset.toLong())
            val kcal = (nutritionDays[dIso]?.items?.sumOf { it.nutrientAmount("kcal") } ?: 0.0).roundToInt()
            // Only count days where the user meaningfully logged meals (>= 400 kcal)
            if (kcal >= 400) {
                pastLoggedCount++
                pastConsumedSum += kcal
                pastExpectedSum += safeBase
            }
        }

        // pastBalance > 0 means user ate OVER target in past 6 days; < 0 means user banked a small buffer
        val pastBalance = pastConsumedSum - pastExpectedSum
        val rawDailyAdjustment = if (pastLoggedCount > 0) {
            // Smoothly spread past surplus/deficit over 5 days
            -(pastBalance.toDouble() / SPREAD_DAYS.toDouble()).roundToInt()
        } else 0

        val clampedDelta = rawDailyAdjustment.coerceIn(-MAX_DAILY_REDISTRIBUTION, MAX_DAILY_REDISTRIBUTION)
        val adjustedToday = max(MIN_SAFE_DAILY_KCAL, safeBase + clampedDelta)
        val effectiveDelta = adjustedToday - safeBase

        val todaySurplus = max(0, todayKcal - adjustedToday)
        val spreadNext5 = if (todaySurplus > 0) (todaySurplus.toDouble() / SPREAD_DAYS.toDouble()).roundToInt().coerceAtLeast(10) else 0

        val weeklyTarget = safeBase * (pastLoggedCount + 1)
        val weeklyConsumed = pastConsumedSum + todayKcal

        val (badge, explanation, supportive) = when {
            todaySurplus >= 50 -> Triple(
                "مرونة أسبوعية · بدون يوم أحمر",
                "زدت +$todaySurplus سعر النهارده — مفيش أي مشكلة! التطبيق هيقسّمهم بذكاء (-$spreadNext5 سعر/يوم على الـ 5 أيام الجايين) عشان متوسطك الأسبوعي يفضل مظبوط.",
                true
            )
            effectiveDelta <= -30 -> Triple(
                "توازن تلقائي ($effectiveDelta سعر اليوم)",
                "موازنة هادئة لفائض +$pastBalance سعر من الأيام الأخيرة (${pastLoggedCount} أيام) بحيث يظل متوسطك الأسبوعي على الهدف ($safeBase سعر/يوم).",
                true
            )
            effectiveDelta >= 30 -> Triple(
                "رصيد مُدّخر (+$effectiveDelta سعر اليوم)",
                "عندك رصيد متوفّر (${abs(pastBalance)} سعر) من التزامك في آخر $pastLoggedCount أيام — تقدر تستفيد منه النهارده براحتك.",
                false
            )
            else -> Triple(
                "متوسط 7 أيام متوازن",
                "ميزانيتك الأسبوعية ماشية بدقة على هدفك ($safeBase سعر/يوم). لو لخبطت في أي يوم التطبيق بيوزع الفرق تلقائياً بدون ضغط.",
                false
            )
        }

        return RollingCalorieBudget(
            baseDailyTarget = safeBase,
            adjustedTodayTarget = adjustedToday,
            deltaTodayKcal = effectiveDelta,
            todayConsumedKcal = todayKcal,
            weeklyTargetKcal = weeklyTarget,
            weeklyConsumedKcal = weeklyConsumed,
            pastLoggedDaysCount = pastLoggedCount,
            pastBalanceKcal = pastBalance,
            todaySurplusKcal = todaySurplus,
            perDaySpreadNext5Days = spreadNext5,
            badgeAr = badge,
            explanationAr = explanation,
            isSupportiveMode = supportive
        )
    }
}

// ============================================================================
// 5. Workout-Synced Nutrition (Gym <-> Macro Cycling)
// ============================================================================

enum class WorkoutSyncDayType(val badgeAr: String) {
    HIGH_DEMAND("يوم تمرين عالي · كارب مرفوع"),
    MODERATE_TRAINING("يوم تمرين · ماكروز متوازنة"),
    REST_DAY("يوم راحة · كارب أقل ودهون صحية")
}

data class WorkoutSyncedMacros(
    val dayType: WorkoutSyncDayType,
    val workoutLabelAr: String,
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val carbsDeltaG: Int,
    val fatDeltaG: Int,
    val tipAr: String
)

object WorkoutSyncedNutritionEngine {

    private val HIGH_DEMAND_KEYWORDS = listOf(
        "رجل", "أرجل", "ارجل", "سفلي", "أفخاذ", "سكوات", "هيت", "فول بودي", "جسم كامل",
        "leg", "legs", "lower", "quad", "glute", "hiit", "full body", "deadlift"
    )

    private val REST_KEYWORDS = listOf("راحة", "راحه", "استشفاء", "rest", "off", "recovery")

    fun evaluate(
        referenceDate: String,
        baseTargets: NutritionTargets,
        effectiveKcal: Int,
        config: UserConfig,
        sessions: List<WorkoutSession>
    ): WorkoutSyncedMacros {
        val split = config.effectiveSplit()
        val loggedSession = sessions.firstOrNull { it.date == referenceDate }

        val (dayType, label) = if (loggedSession != null) {
            val splitName = split.getOrNull(loggedSession.dayIndex)?.name.orEmpty()
            val totalSets = loggedSession.exercises.values.sumOf { ex ->
                ex.sets.count { it.repsDouble() > 0.0 || it.weightDouble() > 0.0 }
            }
            val exNames = loggedSession.exercises.values.joinToString(" ") { it.name }
            val combined = "$splitName $exNames".lowercase()
            when {
                HIGH_DEMAND_KEYWORDS.any { combined.contains(it) } || totalSets >= 16 ->
                    WorkoutSyncDayType.HIGH_DEMAND to (splitName.ifBlank { "تمرينة عالية الشدة" })
                else ->
                    WorkoutSyncDayType.MODERATE_TRAINING to (splitName.ifBlank { "يوم تمرين" })
            }
        } else {
            // Check scheduled split day for today/future; if past day with no logged workout -> Rest Day
            val today = todayIso()
            if (referenceDate < today) {
                WorkoutSyncDayType.REST_DAY to "يوم راحة (بدون تمرين مسجّل)"
            } else {
                val daysSinceStart = daysBetweenIso(config.startDate, referenceDate).toInt()
                val modIdx = if (split.isNotEmpty()) ((daysSinceStart % split.size) + split.size) % split.size else -1
                val scheduledName = split.getOrNull(modIdx)?.name.orEmpty()
                val low = scheduledName.lowercase()
                when {
                    scheduledName.isBlank() || REST_KEYWORDS.any { low.contains(it) } ->
                        WorkoutSyncDayType.REST_DAY to (scheduledName.ifBlank { "يوم راحة" })
                    HIGH_DEMAND_KEYWORDS.any { low.contains(it) } ->
                        WorkoutSyncDayType.HIGH_DEMAND to scheduledName
                    else ->
                        WorkoutSyncDayType.MODERATE_TRAINING to scheduledName
                }
            }
        }

        // Recalculate base carbs/fat for the effectiveKcal while keeping protein fixed
        val proteinG = baseTargets.proteinG
        val minFatG = max(38, (baseTargets.weightKg * 0.55).roundToInt())
        val baseFatG = baseTargets.fatG.coerceAtLeast(minFatG)
        val baseCarbsG = max(40, ((effectiveKcal - proteinG * 4 - baseFatG * 9) / 4.0).roundToInt())

        return when (dayType) {
            WorkoutSyncDayType.HIGH_DEMAND -> {
                // Shift +18% carbs from fats while preserving total calories & protein
                val desiredExtraCarbs = max(20, (baseCarbsG * 0.18).roundToInt())
                val maxRemovableFat = max(0, baseFatG - minFatG)
                val maxExtraCarbsFromFat = (maxRemovableFat * 9.0 / 4.0).roundToInt()
                val actualExtraCarbs = min(desiredExtraCarbs, maxExtraCarbsFromFat)
                val actualFatDrop = (actualExtraCarbs * 4.0 / 9.0).roundToInt()
                WorkoutSyncedMacros(
                    dayType = dayType,
                    workoutLabelAr = label,
                    kcal = effectiveKcal,
                    proteinG = proteinG,
                    carbsG = baseCarbsG + actualExtraCarbs,
                    fatG = max(minFatG, baseFatG - actualFatDrop),
                    carbsDeltaG = actualExtraCarbs,
                    fatDeltaG = -actualFatDrop,
                    tipAr = "رفعنا الكارب (+${actualExtraCarbs} جم) حول التمرين لتعبئة الجليكوجين ورفع الأداء مع ثبات السعرات والبروتين."
                )
            }
            WorkoutSyncDayType.REST_DAY -> {
                // Lower carbs ~20% and shift calories into healthy fats while preserving kcal & protein
                val carbDrop = min(max(20, (baseCarbsG * 0.20).roundToInt()), max(0, baseCarbsG - 50))
                val fatBoost = (carbDrop * 4.0 / 9.0).roundToInt()
                WorkoutSyncedMacros(
                    dayType = dayType,
                    workoutLabelAr = label,
                    kcal = effectiveKcal,
                    proteinG = proteinG,
                    carbsG = baseCarbsG - carbDrop,
                    fatG = baseFatG + fatBoost,
                    carbsDeltaG = -carbDrop,
                    fatDeltaG = fatBoost,
                    tipAr = "يوم راحة: خفّضنا الكارب (-${carbDrop} جم) ورفعنا الدهون الصحية (+${fatBoost} جم) لدعم الهرمونات والشبع بنفس السعرات."
                )
            }
            WorkoutSyncDayType.MODERATE_TRAINING -> {
                WorkoutSyncedMacros(
                    dayType = dayType,
                    workoutLabelAr = label,
                    kcal = effectiveKcal,
                    proteinG = proteinG,
                    carbsG = baseCarbsG,
                    fatG = baseFatG,
                    carbsDeltaG = 0,
                    fatDeltaG = 0,
                    tipAr = "توزيع متوازن ليوم التمرين ($label) — ركّز جزء من الكارب قبل وبعد التمرين بساعة."
                )
            }
        }
    }
}
