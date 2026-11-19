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
}
