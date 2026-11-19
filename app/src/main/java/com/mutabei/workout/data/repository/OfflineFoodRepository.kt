package com.mutabei.workout.data.repository

import android.content.Context
import com.mutabei.workout.data.model.AiDetectedFoodRaw
import com.mutabei.workout.data.model.CustomFood
import com.mutabei.workout.data.model.FoodItem
import com.mutabei.workout.data.model.NutritionConstants
import com.mutabei.workout.data.model.PortionOption
import com.mutabei.workout.data.model.SupplementCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class NutrientFoodSuggestion(
    val name: String,
    val rdaPercentPer100g: Double
)

private data class IndexedOfflineFood(
    val item: FoodItem,
    val enLower: String,
    val arNorm: String
)

class OfflineFoodRepository(private val appContext: Context) {
    private val loadMutex = Mutex()
    @Volatile
    private var indexedFoods: List<IndexedOfflineFood>? = null

    suspend fun ensureLoaded() {
        if (indexedFoods != null) return
        loadMutex.withLock {
            if (indexedFoods != null) return
            indexedFoods = withContext(Dispatchers.IO) {
                val jsonStr = appContext.assets.open("foods.json").bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val arr = root.getJSONArray("f")
                val list = ArrayList<IndexedOfflineFood>(arr.length())
                for (i in 0 until arr.length()) {
                    val f = arr.getJSONArray(i)
                    val enName = f.optString(0, "")
                    val category = f.optString(1, "")
                    val src = f.optString(2, "u")
                    val pArr = f.getJSONArray(3)
                    val nutrients = List(NutritionConstants.NUTRIENT_COUNT) { idx ->
                        if (idx < pArr.length()) pArr.optDouble(idx, -1.0) else -1.0
                    }
                    val porArr = f.optJSONArray(4)
                    val portions = if (porArr != null) {
                        List(porArr.length()) { pi ->
                            val entry = porArr.getJSONArray(pi)
                            PortionOption(
                                name = entry.optString(0, ""),
                                grams = entry.optDouble(1, 100.0)
                            )
                        }
                    } else emptyList()
                    val arName = f.optString(5, "")
                    val refuse = f.optDouble(6, 0.0)
                    val displayName = arName.ifBlank { enName }
                    val item = FoodItem(
                        id = "db_$i",
                        name = displayName,
                        englishName = if (arName.isNotBlank()) enName else "",
                        category = category,
                        source = src,
                        nutrients = nutrients,
                        portions = portions,
                        refusePercent = refuse
                    )
                    list.add(
                        IndexedOfflineFood(
                            item = item,
                            enLower = enName.lowercase(),
                            arNorm = if (arName.isNotBlank()) NutritionConstants.normalizeArabic(arName) else ""
                        )
                    )
                }
                list
            }
        }
    }

    suspend fun search(query: String, customFoods: List<CustomFood>, limit: Int = 40): List<FoodItem> {
        ensureLoaded()
        return withContext(Dispatchers.Default) {
            val q = NutritionConstants.normalizeArabic(query.trim())
            if (q.length < 2) return@withContext emptyList()

            fun stem(t: String): String = t.removePrefix("ال").removeSuffix("ه")

            data class TokenGroup(val strong: List<String>, val all: List<String>)
            val groups = q.split(Regex("\\s+")).filter { it.isNotBlank() }.map { t ->
                val syn = NutritionConstants.ARABIC_SYNONYMS[t]?.split("|")?.filter { it.isNotBlank() }.orEmpty()
                val strong = (listOf(t, stem(t)) + syn).distinct()
                val weak = if (t.length >= 5) listOf(t.take(3)) else emptyList()
                TokenGroup(strong, strong + weak)
            }

            fun matchesStrong(text: String): Boolean =
                groups.all { g -> g.strong.any { text.contains(it) } }

            fun matchesAll(text: String): Boolean =
                groups.all { g -> g.all.any { text.contains(it) } }

            fun startsWithAny(text: String): Boolean =
                groups.all { g -> g.all.any { text.startsWith(it) } }

            val scored = mutableListOf<Pair<Double, FoodItem>>()

            customFoods.filter { it.custom }.forEach { cf ->
                val norm = NutritionConstants.normalizeArabic(cf.name)
                if (matchesAll(norm)) {
                    scored.add(0.0 to cf.toFoodItem())
                }
            }

            SupplementCatalog.ALL.forEach { supp ->
                val arNorm = NutritionConstants.normalizeArabic(supp.name)
                val enLower = supp.englishName.lowercase()
                if (matchesAll(arNorm) || matchesAll(enLower)) {
                    val strong = matchesStrong(arNorm) || matchesStrong(enLower)
                    scored.add((if (strong) 0.1 else 0.4) to supp)
                }
            }

            val foods = indexedFoods.orEmpty()
            for (entry in foods) {
                val enMatch = matchesAll(entry.enLower)
                val arMatch = entry.arNorm.isNotEmpty() && matchesAll(entry.arNorm)
                if (enMatch || arMatch) {
                    val f = entry.item
                    val strongHit = matchesStrong(entry.enLower) || (entry.arNorm.isNotEmpty() && matchesStrong(entry.arNorm))
                    val startHit = if (entry.arNorm.isNotEmpty()) startsWithAny(entry.arNorm) else startsWithAny(entry.enLower)
                    val srcBase = when (f.source) {
                        "e" -> 0.5
                        "f" -> 0.65
                        "u" -> 1.0
                        else -> 1.3
                    }
                    val score = srcBase +
                        (if (strongHit) 0.0 else 2.0) +
                        (if (startHit) 0.0 else 0.5) +
                        (f.name.length / 200.0)
                    scored.add(score to f)
                }
            }
            scored.sortedBy { it.first }.take(limit).map { it.second }
        }
    }

    suspend fun candidatesFor(aiFood: AiDetectedFoodRaw, customFoods: List<CustomFood>): List<FoodItem> {
        ensureLoaded()
        val out = mutableListOf<FoodItem>()
        fun addList(list: List<FoodItem>) {
            for (c in list) {
                if (out.size >= 16) break
                if (out.none { it.name == c.name }) out.add(c)
            }
        }
        val q = aiFood.nameEn
        val tk = q.split(Regex("[ ,]+")).filter { it.isNotBlank() }
        val ar = if (aiFood.nameAr.isNotBlank()) search(aiFood.nameAr, customFoods) else emptyList()
        val all = if (q.isNotBlank()) search(q, customFoods) else emptyList()
        val two = if (tk.size > 1) search(tk.take(2).joinToString(" "), customFoods) else emptyList()
        val one = if (tk.isNotEmpty()) search(tk[0], customFoods) else emptyList()

        addList(ar.filter { it.source == "e" })
        addList(all.take(6))
        addList(two.take(5))
        addList(ar.take(3))
        addList(one.filter { it.source == "e" }.take(4))
        addList(one.take(3))

        val words = q.lowercase().split(Regex("[ ,]+")).filter { it.length > 2 }
        fun overlap(c: FoodItem): Int {
            val target = (c.englishName.ifBlank { c.name }).lowercase()
            return words.count { target.contains(it) }
        }

        return out.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<FoodItem>> { overlap(it.value) }
                    .thenBy {
                        when (it.value.source) {
                            "e" -> 0
                            "f" -> 1
                            else -> 2
                        }
                    }
                    .thenBy { it.index }
            )
            .take(5)
            .map { it.value }
    }

    suspend fun suggestFoodsForMicro(nutrientKey: String, rda: Double): List<NutrientFoodSuggestion> {
        ensureLoaded()
        return withContext(Dispatchers.Default) {
            val idx = NutritionConstants.NUTRIENT_INDEX[nutrientKey] ?: return@withContext emptyList()
            val foods = indexedFoods.orEmpty()
            val excludedAr = setOf("توابل", "مشروبات", "متنوع", "حلويات")
            val excludedEn = Regex("Spices|Fats and Oils|Beverages|Sweets|Soups|Baby", RegexOption.IGNORE_CASE)
            val candidates = mutableListOf<Triple<String, Double, Double>>()

            for (entry in foods) {
                val f = entry.item
                val v = f.nutrients.getOrNull(idx) ?: -1.0
                val kcal = f.nutrients.getOrNull(0) ?: -1.0
                if (v <= 0.0 || kcal <= 0.0 || kcal > 420.0) continue
                if (f.category in excludedAr || excludedEn.containsMatchIn(f.category)) continue
                val pct = (v / rda) * 100.0
                val weight = when (f.source) {
                    "e" -> 1.3
                    "f" -> 1.15
                    else -> 1.0
                }
                candidates.add(Triple(f.name, pct, pct * weight))
            }
            candidates.sortedByDescending { it.third }
                .take(3)
                .map { NutrientFoodSuggestion(it.first, it.second) }
        }
    }

    /**
     * Backfills missing (< 0.0) micronutrients on an already-logged food or supplement
     * using SupplementCatalog, exact/fuzzy match in the 40-nutrient offline foods.json,
     * and a conservative USDA/NIH macro-based fallback for custom/unmatched foods.
     * Existing measured (>= 0.0) values are always preserved.
     */
    suspend fun enrichLoggedItem(
        item: com.mutabei.workout.data.model.LoggedFoodItem,
        customFoods: List<CustomFood>
    ): com.mutabei.workout.data.model.LoggedFoodItem {
        ensureLoaded()
        return withContext(Dispatchers.Default) {
            val count = NutritionConstants.NUTRIENT_COUNT
            val current = com.mutabei.workout.data.model.LoggedFoodItem.normalizeNutrients(item.nutrients).toMutableList()
            val normName = NutritionConstants.normalizeArabic(item.name.trim())

            // 1. Check SupplementCatalog first (handles older logged supplements missing e.g. EPA/DHA)
            val matchedSupp = SupplementCatalog.ALL.firstOrNull { supp ->
                val sn = NutritionConstants.normalizeArabic(supp.name)
                sn == normName || normName.contains(sn) || sn.contains(normName) ||
                    (supp.englishName.isNotBlank() && item.name.contains(supp.englishName.split(" ").take(2).joinToString(" "), ignoreCase = true))
            }
            if (matchedSupp != null) {
                var changed = false
                for (i in 0 until count) {
                    val catVal = matchedSupp.nutrients.getOrNull(i) ?: -1.0
                    if (current[i] < 0.0 && catVal >= 0.0) {
                        current[i] = catVal
                        changed = true
                    }
                }
                return@withContext if (changed) item.copy(nutrients = current) else item
            }

            // Skip generic unit-based supplements (vitamin pills shouldn't get food estimates)
            if (SupplementCatalog.isUnitBasedLoggedItem(item)) {
                return@withContext item
            }

            if (current.none { it < 0.0 }) return@withContext item

            // 2. Try exact match in foods.json, then search match
            val foods = indexedFoods.orEmpty()
            val exactMatch = foods.firstOrNull { entry ->
                entry.item.name.equals(item.name, ignoreCase = true) ||
                    (entry.arNorm.isNotEmpty() && entry.arNorm == normName) ||
                    entry.enLower == item.name.trim().lowercase()
            }?.item ?: search(item.name, customFoods, limit = 5).firstOrNull { cand ->
                cand.nutrients.count { it >= 0.0 } >= 35
            }

            var changed = false
            if (exactMatch != null) {
                val pIdx = NutritionConstants.NUTRIENT_INDEX["p"] ?: 1
                val fIdx = NutritionConstants.NUTRIENT_INDEX["f"] ?: 2
                val curP = current[pIdx].coerceAtLeast(0.0)
                val curF = current[fIdx].coerceAtLeast(0.0)
                val matchP = (exactMatch.nutrients.getOrNull(pIdx) ?: 0.0).coerceAtLeast(0.0)
                val matchF = (exactMatch.nutrients.getOrNull(fIdx) ?: 0.0).coerceAtLeast(0.0)
                val pRatio = if (curP > 0.5 && matchP > 0.5) (curP / matchP).coerceIn(0.5, 1.8) else 1.0
                val fRatio = if (curF > 0.5 && matchF > 0.5) (curF / matchF).coerceIn(0.4, 2.0) else 1.0
                val fatIndices = setOf(6, 7, 8, 32, 35)
                val leuIdx = NutritionConstants.NUTRIENT_INDEX["leu"] ?: 33

                for (i in 0 until count) {
                    val mv = exactMatch.nutrients.getOrNull(i) ?: -1.0
                    if (current[i] < 0.0 && mv >= 0.0) {
                        val scaled = when {
                            i == leuIdx -> if (curP <= 0.05) 0.0 else mv * pRatio
                            i in fatIndices -> if (curF <= 0.05) 0.0 else mv * fRatio
                            else -> mv
                        }
                        current[i] = kotlin.math.round(scaled * 1000.0) / 1000.0
                        changed = true
                    }
                }
            }

            // 3. Fill any still-missing (< 0.0) slots using conservative USDA macro/name rules
            if (current.any { it < 0.0 }) {
                val filled = fillRemainingNutrientsConservatively(item.name, current)
                if (filled) changed = true
            }

            if (changed) item.copy(nutrients = current) else item
        }
    }

    private fun fillRemainingNutrientsConservatively(name: String, vec: MutableList<Double>): Boolean {
        var changed = false
        val lower = NutritionConstants.normalizeArabic(name)
        val p = vec[1].coerceAtLeast(0.0)
        val fat = vec[2].coerceAtLeast(0.0)
        val c = vec[3].coerceAtLeast(0.0)

        fun setIfMissing(key: String, value: Double) {
            val idx = NutritionConstants.NUTRIENT_INDEX[key] ?: return
            if (vec[idx] < 0.0) {
                vec[idx] = kotlin.math.round(value.coerceAtLeast(0.0) * 1000.0) / 1000.0
                changed = true
            }
        }

        val isFish = listOf("سمك", "سلمون", "تونه", "ماكريل", "سردين", "جمبري", "salmon", "tuna", "fish", "shrimp", "sardine", "mackerel").any { lower.contains(it) }
        val isFattyFish = listOf("سلمون", "ماكريل", "سردين", "رنجة", "salmon", "mackerel", "sardine").any { lower.contains(it) }
        val isEgg = (lower.contains("بيض") || Regex("\\begg(s)?\\b").containsMatchIn(lower)) && !lower.contains("باذنجان") && !lower.contains("eggplant")
        val isDairy = listOf("لبن", "حليب", "جبن", "زبادي", "قشطه", "زبده", "واي", "milk", "cheese", "yogurt", "whey", "butter").any { lower.contains(it) }
        val isMeat = listOf("فراخ", "دجاج", "لحم", "كبده", "كفته", "رومي", "بط", "chicken", "beef", "meat", "turkey", "lamb", "liver", "steak").any { lower.contains(it) }
        val isAnimal = isFish || isEgg || isDairy || isMeat
        val isSweet = listOf("سكر", "عسل", "مربي", "كيك", "بسكويت", "بسبوسه", "كنافه", "حلاوه", "شيكولاته", "شوكولاته", "كولا", "بيبسي", "sugar", "honey", "cake", "cookie", "candy", "chocolate", "soda", "cola").any { lower.contains(it) }
        val isCoffee = listOf("قهوه", "نسكافيه", "اسبريسو", "coffee", "espresso", "latte").any { lower.contains(it) }
        val isTea = (lower.contains("شاي") || Regex("\\btea\\b").containsMatchIn(lower))
        val isLeafy = listOf("سبانخ", "ملوخيه", "جرجير", "بقدونس", "خس", "بروكلي", "كرنب", "spinach", "kale", "lettuce", "broccoli").any { lower.contains(it) }

        val leuFrac = when {
            isDairy -> 0.098
            isEgg -> 0.086
            isMeat || isFish -> 0.080
            else -> 0.070
        }
        setIfMissing("leu", if (p <= 0.05) 0.0 else p * leuFrac)
        setIfMissing("sat", if (fat <= 0.05) 0.0 else fat * (if (isDairy) 0.62 else if (isMeat) 0.35 else if (isFish) 0.22 else 0.16))
        setIfMissing("mono", if (fat <= 0.05) 0.0 else fat * 0.45)
        setIfMissing("poly", if (fat <= 0.05) 0.0 else fat * 0.22)
        setIfMissing("chol", when {
            !isAnimal -> 0.0
            isEgg -> 372.0
            isMeat -> 78.0
            isFish -> 58.0
            else -> (fat * 2.5).coerceAtMost(215.0)
        })
        setIfMissing("o3", when {
            isFattyFish -> (fat * 0.16).coerceAtLeast(0.8)
            isFish -> 0.22
            isEgg -> 0.08
            else -> fat * 0.015
        })
        val o3Val = vec[NutritionConstants.NUTRIENT_INDEX["o3"] ?: 32].coerceAtLeast(0.0)
        setIfMissing("epa", when {
            !isAnimal -> 0.0
            isFattyFish -> (o3Val * 0.78).coerceAtLeast(0.6)
            isFish -> (o3Val * 0.72).coerceAtLeast(0.15)
            isEgg -> 0.04
            isMeat -> (o3Val * 0.2).coerceAtMost(0.05)
            else -> 0.0
        })
        val sugVal = vec[NutritionConstants.NUTRIENT_INDEX["sug"] ?: 5].coerceAtLeast(0.0)
        setIfMissing("asug", if (!isSweet) 0.0 else if (sugVal > 0.0) sugVal * 0.8 else c * 0.5)
        setIfMissing("vK", if (isLeafy) 160.0 else if (isAnimal) 1.5 else 5.0)
        setIfMissing("mn", if (isAnimal) 0.03 else 0.45)
        setIfMissing("se", if (isFish) 38.0 else if (isEgg) 30.0 else if (isMeat) 23.0 else if (isDairy) 10.0 else 4.0)
        setIfMissing("io", if (isFish) 50.0 else if (isEgg) 48.0 else if (isDairy) 32.0 else if (isMeat) 6.5 else 3.0)
        setIfMissing("cr", if (isLeafy) 6.0 else if (isMeat || isFish) 3.0 else 2.0)
        setIfMissing("caf", when {
            isCoffee -> 40.0
            isTea -> 20.0
            lower.contains("كولا") || lower.contains("cola") -> 10.0
            lower.contains("شوكولاته") || lower.contains("شيكولاته") || lower.contains("chocolate") -> 25.0
            else -> 0.0
        })
        setIfMissing("wat", (100.0 - p - fat - c - 1.5).coerceIn(0.0, 99.5))

        return changed
    }
}
