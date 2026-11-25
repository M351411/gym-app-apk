package com.mutabei.workout.data.model

import kotlin.math.roundToInt

data class CookingRetentionMethod(
    val code: String,
    val labelAr: String,
    val factorsPercent: List<Int>? // 12 factors aligned with RETENTION_KEYS
)

data class VitaminDDayBreakdown(
    val rawMcg: Double,
    val carryInFromYesterdayMcg: Double,
    val carryOutToTomorrowMcg: Double,
    val effectiveMcg: Double,
    val yesterdaySourceName: String? = null
) {
    val rawIu: Int get() = (rawMcg * NutritionConstants.VITAMIN_D_IU_PER_MCG).roundToInt()
    val carryInIu: Int get() = (carryInFromYesterdayMcg * NutritionConstants.VITAMIN_D_IU_PER_MCG).roundToInt()
    val carryOutIu: Int get() = (carryOutToTomorrowMcg * NutritionConstants.VITAMIN_D_IU_PER_MCG).roundToInt()
    val effectiveIu: Int get() = (effectiveMcg * NutritionConstants.VITAMIN_D_IU_PER_MCG).roundToInt()
}

object NutritionConstants {
    const val VITAMIN_D_DAILY_TARGET_MCG: Double = 125.0 // 5,000 IU daily target (1 mcg = 40 IU)
    const val VITAMIN_D_IU_PER_MCG: Double = 40.0
    val NUTRIENT_KEYS: List<String> = listOf(
        "kcal", "p", "f", "c", "fib", "sug", "sat", "mono", "poly", "chol",
        "na", "k", "ca", "fe", "mg", "ph", "zn", "cu", "mn", "se",
        "vA", "vC", "b1", "b2", "b3", "b6", "fol", "b12", "vD", "vE", "vK", "b5", "o3",
        "leu", "asug", "epa", "io", "cr", "caf", "wat"
    )

    val NUTRIENT_COUNT: Int = NUTRIENT_KEYS.size

    val NUTRIENT_INDEX: Map<String, Int> = NUTRIENT_KEYS.withIndex().associate { (i, k) -> k to i }

    val NUTRIENT_LABELS: Map<String, String> = mapOf(
        "kcal" to "سعرات",
        "p" to "بروتين",
        "f" to "دهون",
        "c" to "كارب",
        "fib" to "ألياف",
        "sug" to "سكر",
        "sat" to "دهون مشبعة",
        "mono" to "دهون أحادية",
        "poly" to "دهون عديدة",
        "chol" to "كوليسترول",
        "na" to "صوديوم",
        "k" to "بوتاسيوم",
        "ca" to "كالسيوم",
        "fe" to "حديد",
        "mg" to "ماغنسيوم",
        "ph" to "فوسفور",
        "zn" to "زنك",
        "cu" to "نحاس",
        "mn" to "منجنيز",
        "se" to "سيلينيوم",
        "vA" to "فيتامين A",
        "vC" to "فيتامين C",
        "b1" to "B1 ثيامين",
        "b2" to "B2 ريبوفلافين",
        "b3" to "B3 نياسين",
        "b6" to "B6 بيريدوكسين",
        "fol" to "B9 فولات",
        "b12" to "B12 كوبالامين",
        "vD" to "فيتامين D",
        "vE" to "فيتامين E",
        "vK" to "فيتامين K",
        "b5" to "B5 بانتوثينيك",
        "o3" to "أوميجا 3 (إجمالي)",
        "leu" to "ليوسين (Leucine)",
        "asug" to "سكر مضاف",
        "epa" to "أوميجا 3 (EPA+DHA)",
        "io" to "يود",
        "cr" to "كروميوم",
        "caf" to "كافيين",
        "wat" to "ماء/رطوبة الأكل"
    )

    val NUTRIENT_UNITS: Map<String, String> = mapOf(
        "chol" to "مجم", "na" to "مجم", "k" to "مجم", "ca" to "مجم",
        "fe" to "مجم", "mg" to "مجم", "ph" to "مجم", "zn" to "مجم",
        "cu" to "مجم", "mn" to "مجم", "se" to "مكجم", "io" to "مكجم", "cr" to "مكجم",
        "vA" to "مكجم", "vC" to "مجم", "b1" to "مجم", "b2" to "مجم", "b3" to "مجم",
        "b6" to "مجم", "fol" to "مكجم", "b12" to "مكجم", "vD" to "مكجم",
        "vE" to "مجم", "vK" to "مكجم", "b5" to "مجم",
        "fib" to "جم", "sat" to "جم", "asug" to "جم", "leu" to "جم",
        "o3" to "جم", "epa" to "جم", "caf" to "مجم", "wat" to "مل"
    )

    val RDA_MALE: LinkedHashMap<String, Double> = linkedMapOf(
        "leu" to 3.0, "fib" to 30.0, "asug" to 36.0, "sat" to 22.0,
        "o3" to 1.6, "epa" to 0.5, "chol" to 300.0,
        "vA" to 900.0, "vC" to 90.0, "vD" to VITAMIN_D_DAILY_TARGET_MCG, "vE" to 15.0, "vK" to 120.0,
        "b1" to 1.2, "b2" to 1.3, "b3" to 16.0, "b5" to 5.0, "b6" to 1.3,
        "fol" to 400.0, "b12" to 2.4,
        "na" to 2300.0, "k" to 3400.0, "ca" to 1000.0, "fe" to 8.0,
        "mg" to 400.0, "ph" to 700.0, "zn" to 11.0, "cu" to 0.9,
        "mn" to 2.3, "se" to 55.0, "io" to 150.0, "cr" to 35.0,
        "caf" to 400.0
    )

    val RDA_FEMALE: LinkedHashMap<String, Double> = LinkedHashMap(RDA_MALE).apply {
        put("fib", 25.0)
        put("asug", 25.0)
        put("sat", 18.0)
        put("fe", 18.0)
        put("mg", 310.0)
        put("zn", 8.0)
        put("cr", 25.0)
        put("vA", 700.0)
        put("vC", 75.0)
        put("b1", 1.1)
        put("b2", 1.1)
        put("b3", 14.0)
        put("vK", 90.0)
        put("o3", 1.1)
    }

    val LIMIT_KEYS: Set<String> = setOf("na", "chol", "asug", "sat", "caf")

    val MICRO_GROUP_MACROS: List<String> = listOf("leu", "fib", "asug", "sat", "o3", "epa", "chol")
    val MICRO_GROUP_VITAMINS: List<String> = listOf("vA", "vC", "vD", "vE", "vK", "b1", "b2", "b3", "b5", "b6", "fol", "b12")
    val MICRO_GROUP_MINERALS: List<String> = listOf("na", "k", "ca", "fe", "mg", "ph", "zn", "cu", "mn", "se", "io", "cr")
    val MICRO_GROUP_OTHER: List<String> = listOf("caf")

    val SOURCE_LABELS: Map<String, String> = mapOf(
        "e" to "جدول الأغذية المصري",
        "f" to "USDA Foundation",
        "u" to "USDA",
        "s" to "USDA أكلات",
        "c" to "أكلتي",
        "a" to "تقدير AI",
        "l" to "ملصق"
    )

    const val SUPPLEMENT_MEAL_INDEX = 4

    val MEAL_NAMES: List<String> = listOf("فطار", "غدا", "عشا", "سناك", "مكملات وفيتامينات")

    val ARABIC_SYNONYMS: Map<String, String> = mapOf(
        "فول" to "fava|broadbean|broad bean|falafel",
        "طعميه" to "falafel",
        "فلافل" to "falafel",
        "حمص" to "hummus|chickpea",
        "عدس" to "lentil",
        "ارز" to "rice",
        "رز" to "rice",
        "عيش" to "bread|pita",
        "خبز" to "bread|pita",
        "لحمه" to "beef",
        "لحم" to "beef|meat",
        "كبده" to "liver",
        "فراخ" to "chicken|دجاج",
        "دجاج" to "chicken|فراخ",
        "بط" to "duck",
        "رومي" to "turkey",
        "سلمون" to "salmon",
        "سمك" to "fish|salmon|tuna|tilapia|cod|halibut",
        "تونه" to "tuna",
        "جمبري" to "shrimp",
        "بيض" to "egg,|eggs,|egg |omelet",
        "لبن" to "milk|yogurt",
        "حليب" to "milk",
        "زبادي" to "yogurt",
        "جبنه" to "cheese",
        "جبن" to "cheese",
        "قشطه" to "cream",
        "زبده" to "butter",
        "سمن" to "ghee|butter",
        "زيت" to "oil",
        "بطاطس" to "potato",
        "بطاطا" to "sweet potato",
        "طماطم" to "tomato",
        "خيار" to "cucumber",
        "جزر" to "carrot",
        "بصل" to "onion",
        "ثوم" to "garlic",
        "خس" to "lettuce",
        "ملوخيه" to "jute|molokhia",
        "سبانخ" to "spinach",
        "كرنب" to "cabbage",
        "فلفل" to "pepper",
        "باذنجان" to "eggplant",
        "كوسه" to "squash|zucchini",
        "بامي" to "okra",
        "بسله" to "peas",
        "فاصوليا" to "beans",
        "موز" to "banana",
        "تفاح" to "apple",
        "برتقال" to "orange",
        "مانجو" to "mango",
        "عنب" to "grape",
        "بطيخ" to "watermelon",
        "تمر" to "dates",
        "زبيب" to "raisin",
        "فراوله" to "strawberr",
        "جوافه" to "guava",
        "رمان" to "pomegranate",
        "افوكادو" to "avocado",
        "بروكلي" to "broccoli",
        "قرنبيط" to "cauliflower",
        "مشروم" to "mushroom",
        "فطر" to "mushroom",
        "توت" to "berr",
        "اناناس" to "pineapple",
        "كيوي" to "kiwi",
        "خوخ" to "peach",
        "كمثري" to "pear",
        "مشمش" to "apricot",
        "برقوق" to "plum",
        "لوز" to "almond",
        "كاجو" to "cashew",
        "بندق" to "hazelnut",
        "فسدق" to "pistachio",
        "جوز" to "walnut",
        "سوداني" to "peanut",
        "عسل" to "honey",
        "سكر" to "sugar",
        "شوفان" to "oat",
        "كينوا" to "quinoa",
        "ذره" to "corn",
        "مكرونه" to "pasta|macaroni",
        "شاي" to "tea",
        "قهوه" to "coffee",
        "طحينه" to "tahini",
        "تين" to "fig",
        "كشري" to "rice|lentil|pasta",
        "شاورما" to "gyro|shawarma",
        "كفته" to "meatball|beef",
        "بروتين" to "protein",
        "بسبوسه" to "cake",
        "كنافه" to "pastry",
        "لبنه" to "cheese",
        "فطير" to "pie|pastry",
        "كباب" to "kabob|beef",
        "شوربه" to "soup",
        "سلطه" to "salad",
        "مشوي" to "grilled|broil",
        "مسلوق" to "boiled",
        "مقلي" to "fried",
        "نيء" to "raw",
        "مطبوخ" to "cooked",
        "مالتي" to "multivitamin|فيتا ستريس|مان ماكس",
        "فايتمن" to "vitamin|فيتامين|multivitamin",
        "فايتمنس" to "vitamin|فيتامين|multivitamin",
        "فيتامين" to "vitamin|multivitamin",
        "فيتا" to "vita|فيتا ستريس",
        "ستريس" to "stress|فيتا ستريس",
        "اوميجا" to "omega|اوميجا",
        "ليمتلس" to "limitless|ليمتلس",
        "باور" to "power|باور ماكس",
        "ماكس" to "max|باور ماكس|مان ماكس",
        "ماغنسيوم" to "magnesium|ماغنسيوم",
        "زنك" to "zinc|زنك",
        "كرياتين" to "creatine|كرياتين",
        "واي" to "whey|واي بروتين",
        "دافاليندي" to "davalindi|دافاليندي|فيتامين d",
        "دافاليندا" to "davalindi|دافاليندي|فيتامين d",
        "كوليكالسيفيرول" to "cholecalciferol|davalindi|دافاليندي|vitamin d",
        "دال" to "vitamin d|فيتامين d|دافاليندي|davalindi",
        "مكمل" to "supplement|multivitamin|omega|zinc|creatine|whey|davalindi"
    )

    val RETENTION_KEYS: List<String> = listOf(
        "ca", "fe", "mg", "ph", "k", "na", "zn", "cu", "vC", "b1", "b2", "b3"
    )

    val RETENTION_METHODS: List<CookingRetentionMethod> = listOf(
        CookingRetentionMethod("raw", "نيّء أو جاهز (بدون فقد)", null),
        CookingRetentionMethod("vb", "خضار مسلوق ومصفّى", listOf(95, 95, 95, 90, 90, 95, 95, 95, 75, 80, 90, 85)),
        CookingRetentionMethod("vs", "خضار في شوربة أو طاجن (السائل يُؤكل)", listOf(100, 100, 100, 100, 100, 100, 100, 100, 85, 90, 95, 95)),
        CookingRetentionMethod("vf", "خضار محمّر أو مشوي", listOf(100, 100, 100, 100, 100, 100, 100, 100, 85, 85, 95, 90)),
        CookingRetentionMethod("mg", "لحمة مشوية أو محمّرة", listOf(100, 95, 85, 90, 85, 85, 100, 100, 80, 70, 90, 80)),
        CookingRetentionMethod("mb", "لحمة مسلوقة ومصفّاة", listOf(80, 100, 65, 65, 55, 55, 100, 95, 80, 45, 85, 55)),
        CookingRetentionMethod("ms", "لحمة في صلصة أو يخنة (السائل يُؤكل)", listOf(100, 100, 100, 100, 100, 100, 100, 100, 85, 60, 100, 90)),
        CookingRetentionMethod("cg", "فراخ مشوية أو محمّرة", listOf(95, 90, 75, 80, 80, 80, 100, 95, 80, 70, 90, 80)),
        CookingRetentionMethod("cb", "فراخ مسلوقة ومصفّاة", listOf(80, 90, 65, 70, 60, 70, 100, 90, 80, 55, 95, 60)),
        CookingRetentionMethod("fg", "سمك محمّر أو مشوي", listOf(100, 100, 100, 100, 100, 100, 100, 100, 80, 85, 95, 100)),
        CookingRetentionMethod("lb", "بقوليات مسلوقة ومصفّاة", listOf(85, 80, 75, 85, 70, 90, 85, 65, 65, 60, 70, 65)),
        CookingRetentionMethod("ls", "بقوليات في الطبخة (السائل يُؤكل)", listOf(90, 85, 80, 90, 75, 95, 90, 70, 70, 65, 75, 70)),
        CookingRetentionMethod("rc", "أرز (بيشرب الماية)", listOf(100, 95, 100, 95, 95, 100, 100, 95, 80, 80, 90, 100)),
        CookingRetentionMethod("pa", "مكرونة مسلوقة ومصفّاة", listOf(95, 75, 85, 85, 30, 50, 100, 90, 70, 65, 75, 65)),
        CookingRetentionMethod("pb", "بطاطس مسلوقة", listOf(95, 95, 95, 95, 90, 95, 95, 95, 75, 80, 95, 95)),
        CookingRetentionMethod("pf", "بطاطس محمّرة أو مشوية", listOf(100, 100, 100, 100, 100, 100, 100, 100, 80, 80, 95, 95)),
        CookingRetentionMethod("eg", "بيض مقلي أو مسلوق", listOf(100, 100, 100, 100, 100, 100, 100, 100, 80, 85, 95, 95)),
        CookingRetentionMethod("bk", "عجين أو خبز في الفرن", listOf(100, 100, 100, 100, 100, 100, 100, 100, 80, 80, 90, 90))
    )

    val RETENTION_BY_CODE: Map<String, CookingRetentionMethod> =
        RETENTION_METHODS.associateBy { it.code }

    fun normalizeArabic(input: String): String = input
        .lowercase()
        .replace(Regex("[\u064B-\u0652\u0640]"), "")
        .replace(Regex("[أإآ]"), "ا")
        .replace("ى", "ي")
        .replace("ة", "ه")
        .replace("ڤ", "ف")

    /**
     * Vitamin D-only every-other-day carryover exception:
     * When a user takes a 10,000 IU dose (e.g. Davalindi = 250 mcg) on Day D,
     * Day D retains its 5,000 IU (125 mcg) daily target and automatically carries over
     * the surplus (up to 5,000 IU / 125 mcg) to Day D+1.
     */
    fun computeVitaminDBreakdown(
        date: String,
        nutritionDays: Map<String, NutritionDay>
    ): VitaminDDayBreakdown {
        val todayDay = nutritionDays[date]
        val yesterdayDate = shiftIsoDate(date, -1)
        val yesterdayDay = nutritionDays[yesterdayDate]

        val rawToday = todayDay?.items?.sumOf { it.nutrientAmount("vD") } ?: 0.0
        val rawYesterday = yesterdayDay?.items?.sumOf { it.nutrientAmount("vD") } ?: 0.0

        val carryIn = (rawYesterday - VITAMIN_D_DAILY_TARGET_MCG)
            .coerceIn(0.0, VITAMIN_D_DAILY_TARGET_MCG)
        val carryOut = (rawToday - VITAMIN_D_DAILY_TARGET_MCG)
            .coerceIn(0.0, VITAMIN_D_DAILY_TARGET_MCG)
        val effective = (rawToday - carryOut + carryIn).coerceAtLeast(0.0)

        val yesterdaySource = if (carryIn > 0.0) {
            yesterdayDay?.items
                ?.filter { it.nutrientAmount("vD") > 0.0 }
                ?.maxByOrNull { it.nutrientAmount("vD") }
                ?.name
        } else null

        return VitaminDDayBreakdown(
            rawMcg = rawToday,
            carryInFromYesterdayMcg = carryIn,
            carryOutToTomorrowMcg = carryOut,
            effectiveMcg = effective,
            yesterdaySourceName = yesterdaySource
        )
    }

    fun emptyNutrientVector(): List<Double> = List(NUTRIENT_COUNT) { -1.0 }

    fun vectorFromMap(map: Map<String, Double?>): List<Double> =
        NUTRIENT_KEYS.map { key -> map[key]?.takeIf { !it.isNaN() } ?: -1.0 }
}
