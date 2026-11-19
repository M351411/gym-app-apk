package com.mutabei.workout.data.model

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Curated catalog of popular Egyptian pharmacy & gym supplements.
 *
 * For unit-based supplements (tablets / capsules), [nutrients] stores the exact
 * amount per 1 tablet/capsule at `grams = 100.0` (since LoggedFoodItem computes
 * `value * grams / 100.0`), paired with clear unit portions (e.g. "1 قرص" = 100.0,
 * "2 كبسولة" = 200.0). Powder supplements (Whey, Creatine) use standard per-100g
 * values with scoop gram portions.
 */
object SupplementCatalog {

    const val SUPPLEMENT_CATEGORY = "مكملات"

    private fun unitSupplement(
        id: String,
        nameAr: String,
        nameEn: String,
        unitLabel: String,
        defaultUnits: Int = 1,
        values: Map<String, Double>
    ): FoodItem {
        val baseMap = HashMap<String, Double?>()
        baseMap["kcal"] = values["kcal"] ?: 0.0
        values.forEach { (k, v) -> baseMap[k] = v }

        val portions = buildList {
            if (defaultUnits == 2) {
                add(PortionOption("2 $unitLabel (حصة كاملة)", 200.0))
                add(PortionOption("1 $unitLabel", 100.0))
                add(PortionOption("3 $unitLabel", 300.0))
            } else {
                add(PortionOption("1 $unitLabel", 100.0))
                add(PortionOption("2 $unitLabel", 200.0))
                add(PortionOption("نصف $unitLabel", 50.0))
            }
        }

        return FoodItem(
            id = id,
            name = nameAr,
            englishName = nameEn,
            category = SUPPLEMENT_CATEGORY,
            source = "c",
            nutrients = NutritionConstants.vectorFromMap(baseMap),
            portions = portions
        )
    }

    val ALL: List<FoodItem> = listOf(
        // 1. Vita-Stress (MUP Egypt) — per 1 tablet
        unitSupplement(
            id = "supp_vita_stress",
            nameAr = "فيتا ستريس (Vita-Stress) — مالتي فيتامين",
            nameEn = "Vita Stress Multivitamin Zinc B-Complex Vitamin C E MUP",
            unitLabel = "قرص",
            values = mapOf(
                "kcal" to 0.0,
                "vC" to 600.0,
                "b3" to 100.0,
                "vE" to 45.0,
                "b5" to 25.0,
                "b1" to 20.0,
                "b2" to 10.0,
                "b6" to 10.0,
                "zn" to 23.9,
                "cu" to 3.0,
                "b12" to 25.0,
                "fol" to 400.0
            )
        ),

        // 2. Limitless Omega 3 (Eva Pharma Egypt) — per 1 softgel (serving = 2 softgels: 2000mg fish oil, 1400mg Omega-3, 10mcg D3)
        unitSupplement(
            id = "supp_limitless_omega3",
            nameAr = "ليمتلس أوميجا 3 (Limitless Omega 3)",
            nameEn = "Limitless Omega 3 Fish Oil EPA DHA Vitamin D3 Eva Pharma",
            unitLabel = "كبسولة",
            defaultUnits = 2,
            values = mapOf(
                "kcal" to 9.0,
                "f" to 1.0,
                "poly" to 0.7,
                "o3" to 0.7,  // 700 mg per capsule -> 1.4 g (1400 mg) per 2 capsules
                "epa" to 0.7, // Combined EPA+DHA per capsule
                "vD" to 5.0   // 5 mcg (200 IU) per capsule -> 10 mcg (400 IU) per 2 capsules
            )
        ),

        // 3. Limitless Power Max (Eva Pharma Egypt) — per 1 tablet (Zinc 25mg + Tribulus 250mg + Maca 300mg + Ginseng 250mg)
        unitSupplement(
            id = "supp_limitless_power_max",
            nameAr = "ليمتلس باور ماكس (Power Max — زنك + ماكا + تريبولوس + جينسنج)",
            nameEn = "Limitless Power Max Zinc Tribulus Maca Panax Ginseng Eva Pharma",
            unitLabel = "قرص",
            values = mapOf(
                "kcal" to 0.0,
                "zn" to 25.0
            )
        ),

        // 4. Limitless Man Max (Eva Pharma Egypt) — per 1 tablet (27 nutrients)
        unitSupplement(
            id = "supp_limitless_man_max",
            nameAr = "ليمتلس مان ماكس (Limitless Man Max) — مالتي فيتامين للرجال",
            nameEn = "Limitless Man Max Multivitamin Minerals Men Eva Pharma",
            unitLabel = "قرص",
            values = mapOf(
                "kcal" to 0.0,
                "vA" to 1050.0,
                "vC" to 90.0,
                "vD" to 25.0,
                "vE" to 20.3,
                "vK" to 60.0,
                "b1" to 1.2,
                "b2" to 1.3,
                "b3" to 16.0,
                "b5" to 15.0,
                "b6" to 2.0,
                "fol" to 200.0,
                "b12" to 6.0,
                "ca" to 210.0,
                "mg" to 100.0,
                "k" to 80.0,
                "ph" to 20.0,
                "zn" to 11.0,
                "fe" to 8.0,
                "se" to 100.0,
                "cu" to 0.9,
                "mn" to 2.3,
                "io" to 150.0,
                "cr" to 35.0
            )
        ),

        // 5. Limitless Magnesium 150mg (Eva Pharma Egypt) — per 1 tablet
        unitSupplement(
            id = "supp_limitless_magnesium",
            nameAr = "ليمتلس ماغنسيوم جلايسينات 150 مجم (Limitless Magnesium)",
            nameEn = "Limitless Magnesium Glycinate Bisglycinate 150mg Eva Pharma",
            unitLabel = "قرص",
            values = mapOf(
                "kcal" to 0.0,
                "mg" to 150.0
            )
        ),

        // 6. Vitamin D3 1000 IU (25 mcg)
        unitSupplement(
            id = "supp_vit_d3_1000",
            nameAr = "فيتامين D3 — 1000 وحدة (Vitamin D3 1000 IU)",
            nameEn = "Vitamin D3 1000 IU Cholecalciferol Limitless Ossofortin",
            unitLabel = "قرص",
            values = mapOf(
                "kcal" to 0.0,
                "vD" to 25.0
            )
        ),

        // 7. Vitamin D3 5000 IU (125 mcg)
        unitSupplement(
            id = "supp_vit_d3_5000",
            nameAr = "فيتامين D3 — 5000 وحدة (Vitamin D3 5000 IU)",
            nameEn = "Vitamin D3 5000 IU Cholecalciferol",
            unitLabel = "كبسولة",
            values = mapOf(
                "kcal" to 0.0,
                "vD" to 125.0
            )
        ),

        // 8. Zinc 25mg (Octozinc / Limitless Zinc)
        unitSupplement(
            id = "supp_zinc_25",
            nameAr = "زنك 25 مجم (أوكتوزنك / ليمتلس زنك)",
            nameEn = "Zinc 25mg Octozinc Limitless Zinc",
            unitLabel = "كبسولة",
            values = mapOf(
                "kcal" to 0.0,
                "zn" to 25.0
            )
        ),

        // 9. Creatine Monohydrate (powder — per 100g)
        FoodItem(
            id = "supp_creatine",
            name = "كرياتين مونوهيدرات (Creatine Monohydrate)",
            englishName = "Creatine Monohydrate Powder Supplement",
            category = SUPPLEMENT_CATEGORY,
            source = "c",
            nutrients = NutritionConstants.vectorFromMap(mapOf("kcal" to 0.0, "p" to 0.0, "c" to 0.0, "f" to 0.0)),
            portions = listOf(
                PortionOption("سكوب (5 جم)", 5.0),
                PortionOption("سكوبين (10 جم)", 10.0)
            )
        ),

        // 10. Whey Protein (powder — per 100g)
        FoodItem(
            id = "supp_whey_protein",
            name = "واي بروتين (Whey Protein)",
            englishName = "Whey Protein Isolate Concentrate Powder Scoop",
            category = SUPPLEMENT_CATEGORY,
            source = "c",
            nutrients = NutritionConstants.vectorFromMap(
                mapOf(
                    "kcal" to 390.0,
                    "p" to 80.0,
                    "leu" to 8.5,
                    "c" to 7.0,
                    "f" to 4.0,
                    "ca" to 400.0,
                    "na" to 250.0,
                    "k" to 450.0
                )
            ),
            portions = listOf(
                PortionOption("1 سكوب (30 جم)", 30.0),
                PortionOption("2 سكوب (60 جم)", 60.0)
            )
        )
    )

    private val UNIT_KEYWORDS = listOf("قرص", "كبسول", "حبه", "حبة", "حصة", "مضغ", "فوار", "كيس")

    fun isUnitBasedFood(food: FoodItem): Boolean {
        val firstPor = food.portions.firstOrNull() ?: return false
        val isHundredUnit = abs(firstPor.grams - 100.0) < 0.01 || abs(firstPor.grams - 200.0) < 0.01
        return isHundredUnit && UNIT_KEYWORDS.any { firstPor.name.contains(it) }
    }

    fun isUnitBasedLoggedItem(item: LoggedFoodItem): Boolean {
        val kcal = item.nutrientAmount("kcal")
        val hasMicro = NutritionConstants.RDA_MALE.keys.any { k ->
            k != "fib" && k != "na" && k != "chol" && item.nutrientAmount(k) > 0.0
        }
        return item.mealIndex == NutritionConstants.SUPPLEMENT_MEAL_INDEX &&
            kcal <= 35.0 &&
            (hasMicro || item.grams >= 50.0)
    }

    fun formatServingCount(grams: Double): String {
        val units = grams / 100.0
        return if (abs(units - units.roundToInt()) < 0.05) {
            val n = units.roundToInt()
            when (n) {
                1 -> "1 حصة (قرص/كبسولة)"
                2 -> "2 حصة"
                else -> "$n حصص"
            }
        } else {
            "${(units * 10.0).roundToInt() / 10.0} حصة"
        }
    }

    fun activeNutrientsSummary(nutrients: List<Double>, grams: Double = 100.0, maxItems: Int = 4): String {
        val priorityKeys = listOf(
            "o3", "epa", "vC", "vD", "zn", "mg", "vE", "b12", "fol", "b1", "b2", "b3", "b5", "b6",
            "vA", "vK", "fe", "ca", "cu", "se", "mn", "io", "cr", "leu", "caf", "k", "ph"
        )
        val parts = mutableListOf<String>()
        for (k in priorityKeys) {
            if (parts.size >= maxItems) break
            val idx = NutritionConstants.NUTRIENT_INDEX[k] ?: continue
            val raw = nutrients.getOrNull(idx) ?: -1.0
            if (raw > 0.0) {
                val amount = raw * grams / 100.0
                val formatted = if (amount >= 10.0) {
                    amount.roundToInt().toString()
                } else {
                    val r = (amount * 10.0).roundToInt() / 10.0
                    if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
                }
                val label = NutritionConstants.NUTRIENT_LABELS[k] ?: k
                val unit = NutritionConstants.NUTRIENT_UNITS[k] ?: ""
                parts.add("$label $formatted $unit".trim())
            }
        }
        return parts.joinToString(" · ")
    }
}
