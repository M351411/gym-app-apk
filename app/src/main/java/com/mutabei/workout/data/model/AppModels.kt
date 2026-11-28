package com.mutabei.workout.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

fun todayIso(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

fun shiftIsoDate(isoDate: String, deltaDays: Long): String = try {
    LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
        .plusDays(deltaDays)
        .format(DateTimeFormatter.ISO_LOCAL_DATE)
} catch (_: Exception) {
    todayIso()
}

fun daysBetweenIso(startIso: String, endIso: String): Long = try {
    ChronoUnit.DAYS.between(
        LocalDate.parse(startIso, DateTimeFormatter.ISO_LOCAL_DATE),
        LocalDate.parse(endIso, DateTimeFormatter.ISO_LOCAL_DATE)
    )
} catch (_: Exception) {
    0L
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@Serializable
data class ExerciseSpec(
    val name: String = "",
    val warmupSets: String = "1–2",
    val workingSets: Int = 2,
    val minReps: Int = 6,
    val maxReps: Int = 10,
    val alt1: String = "",
    val alt2: String = "",
    val note: String = ""
) {
    fun alternativesList(): List<String> = listOf(name, alt1, alt2).filter { it.isNotBlank() }

    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "warmupSets" to warmupSets,
        "workingSets" to workingSets,
        "minReps" to minReps,
        "maxReps" to maxReps,
        "alt1" to alt1,
        "alt2" to alt2,
        "note" to note
    )

    companion object {
        fun fromMap(map: Map<*, *>): ExerciseSpec = ExerciseSpec(
            name = (map["name"] as? String).orEmpty(),
            warmupSets = (map["warmupSets"] as? String) ?: "1–2",
            workingSets = ((map["workingSets"] as? Number)?.toInt() ?: 2).coerceIn(1, 12),
            minReps = ((map["minReps"] as? Number)?.toInt() ?: 6).coerceIn(1, 100),
            maxReps = ((map["maxReps"] as? Number)?.toInt() ?: 10).coerceIn(1, 100),
            alt1 = (map["alt1"] as? String).orEmpty(),
            alt2 = (map["alt2"] as? String).orEmpty(),
            note = (map["note"] as? String).orEmpty()
        )
    }
}

@Keep
@Serializable
data class SplitDay(
    val name: String = "",
    val exercises: List<ExerciseSpec> = emptyList()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "exercises" to exercises.map { it.toMap() }
    )

    companion object {
        fun fromMap(map: Map<*, *>): SplitDay {
            val rawEx = map["exercises"] as? List<*> ?: emptyList<Any>()
            return SplitDay(
                name = (map["name"] as? String).orEmpty(),
                exercises = rawEx.mapNotNull { (it as? Map<*, *>)?.let(ExerciseSpec::fromMap) }
            )
        }
    }
}

@Keep
@Serializable
data class PortionOption(
    val name: String = "",
    val grams: Double = 100.0
) {
    fun toMap(): Map<String, Any> = mapOf("name" to name, "grams" to grams)

    companion object {
        fun fromMap(map: Map<*, *>): PortionOption = PortionOption(
            name = (map["name"] as? String).orEmpty(),
            grams = (map["grams"] as? Number)?.toDouble() ?: 100.0
        )
    }
}

@Keep
data class UserConfig(
    val userId: String = "",
    val startDate: String = todayIso(),
    val restSeconds: Int = 180,
    val themeMode: String = "system",
    val selectedBodyMetric: String = "w",
    val sex: String = "m",
    val age: Int = 25,
    val heightCm: Double = 175.0,
    val weightOverride: Double = 0.0,
    val bodyFatOverride: Double = 0.0,
    val activityFactor: Double = 1.55,
    val goal: String = "bulk",
    val targetBodyFat: Double = 20.0,
    val kcalOverride: Int = 0,
    val proteinOverride: Int = 0,
    val carbsOverride: Int = 0,
    val fatOverride: Int = 0,
    val alternatives: Map<String, String> = emptyMap(),
    val customPortions: Map<String, List<PortionOption>> = emptyMap(),
    val splitDays: List<SplitDay> = WorkoutDefaults.DEFAULT_SPLIT,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun effectiveSplit(): List<SplitDay> = splitDays.ifEmpty { WorkoutDefaults.DEFAULT_SPLIT }

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "startDate" to startDate.ifBlank { todayIso() },
        "restSeconds" to restSeconds.coerceIn(30, 900),
        "themeMode" to if (themeMode in listOf("system", "light", "dark")) themeMode else "system",
        "selectedBodyMetric" to selectedBodyMetric.ifBlank { "w" }.take(10),
        "sex" to if (sex == "f") "f" else "m",
        "age" to age.coerceIn(10, 120),
        "heightCm" to heightCm.coerceIn(100.0, 250.0),
        "weightOverride" to weightOverride.coerceIn(0.0, 350.0),
        "bodyFatOverride" to bodyFatOverride.coerceIn(0.0, 70.0),
        "activityFactor" to activityFactor.coerceIn(1.1, 2.5),
        "goal" to if (goal in listOf("bulk", "cut", "keep")) goal else "bulk",
        "targetBodyFat" to targetBodyFat.coerceIn(3.0, 50.0),
        "kcalOverride" to kcalOverride.coerceIn(0, 10000),
        "proteinOverride" to proteinOverride.coerceIn(0, 1000),
        "carbsOverride" to carbsOverride.coerceIn(0, 2000),
        "fatOverride" to fatOverride.coerceIn(0, 500),
        "alternatives" to alternatives,
        "customPortions" to customPortions.mapValues { (_, list) -> list.map { it.toMap() } },
        "splitDays" to splitDays.take(14).map { it.toMap() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "startDate" to startDate.ifBlank { todayIso() },
        "restSeconds" to restSeconds.coerceIn(30, 900),
        "themeMode" to if (themeMode in listOf("system", "light", "dark")) themeMode else "system",
        "selectedBodyMetric" to selectedBodyMetric.ifBlank { "w" }.take(10),
        "sex" to if (sex == "f") "f" else "m",
        "age" to age.coerceIn(10, 120),
        "heightCm" to heightCm.coerceIn(100.0, 250.0),
        "weightOverride" to weightOverride.coerceIn(0.0, 350.0),
        "bodyFatOverride" to bodyFatOverride.coerceIn(0.0, 70.0),
        "activityFactor" to activityFactor.coerceIn(1.1, 2.5),
        "goal" to if (goal in listOf("bulk", "cut", "keep")) goal else "bulk",
        "targetBodyFat" to targetBodyFat.coerceIn(3.0, 50.0),
        "kcalOverride" to kcalOverride.coerceIn(0, 10000),
        "proteinOverride" to proteinOverride.coerceIn(0, 1000),
        "carbsOverride" to carbsOverride.coerceIn(0, 2000),
        "fatOverride" to fatOverride.coerceIn(0, 500),
        "alternatives" to alternatives,
        "customPortions" to customPortions.mapValues { (_, list) -> list.map { it.toMap() } },
        "splitDays" to splitDays.take(14).map { it.toMap() },
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): UserConfig? {
            if (!doc.exists()) return null
            val data = doc.data ?: return null
            val altRaw = data["alternatives"] as? Map<*, *> ?: emptyMap<Any, Any>()
            val alternatives = altRaw.entries.mapNotNull { (k, v) ->
                val ks = k as? String
                val vs = v as? String
                if (ks != null && vs != null) ks to vs else null
            }.toMap()

            val cpRaw = data["customPortions"] as? Map<*, *> ?: emptyMap<Any, Any>()
            val customPortions = cpRaw.entries.mapNotNull { (k, v) ->
                val ks = k as? String ?: return@mapNotNull null
                val list = (v as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.let(PortionOption::fromMap) }
                    ?: return@mapNotNull null
                ks to list
            }.toMap()

            val splitRaw = data["splitDays"] as? List<*> ?: emptyList<Any>()
            val splitDays = splitRaw.mapNotNull { (it as? Map<*, *>)?.let(SplitDay::fromMap) }

            return UserConfig(
                userId = (data["userId"] as? String).orEmpty(),
                startDate = (data["startDate"] as? String) ?: todayIso(),
                restSeconds = (data["restSeconds"] as? Number)?.toInt() ?: 180,
                themeMode = (data["themeMode"] as? String) ?: "system",
                selectedBodyMetric = (data["selectedBodyMetric"] as? String) ?: "w",
                sex = (data["sex"] as? String) ?: "m",
                age = (data["age"] as? Number)?.toInt() ?: 25,
                heightCm = (data["heightCm"] as? Number)?.toDouble() ?: 175.0,
                weightOverride = (data["weightOverride"] as? Number)?.toDouble() ?: 0.0,
                bodyFatOverride = (data["bodyFatOverride"] as? Number)?.toDouble() ?: 0.0,
                activityFactor = (data["activityFactor"] as? Number)?.toDouble() ?: 1.55,
                goal = (data["goal"] as? String) ?: "bulk",
                targetBodyFat = (data["targetBodyFat"] as? Number)?.toDouble() ?: 20.0,
                kcalOverride = (data["kcalOverride"] as? Number)?.toInt() ?: 0,
                proteinOverride = (data["proteinOverride"] as? Number)?.toInt() ?: 0,
                carbsOverride = (data["carbsOverride"] as? Number)?.toInt() ?: 0,
                fatOverride = (data["fatOverride"] as? Number)?.toInt() ?: 0,
                alternatives = alternatives,
                customPortions = customPortions,
                splitDays = splitDays.ifEmpty { WorkoutDefaults.DEFAULT_SPLIT },
                createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            )
        }
    }
}

@Keep
@Serializable
data class SetEntry(
    val weight: String = "",
    val reps: String = ""
) {
    fun weightDouble(): Double = weight.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
    fun repsDouble(): Double = reps.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
    fun repsInt(): Int = repsDouble().roundToInt()
    fun estimated1Rm(): Double = WorkoutDefaults.epley1Rm(weightDouble(), repsDouble())

    fun toMap(): Map<String, Any> = mapOf("weight" to weight, "reps" to reps)

    companion object {
        fun fromMap(map: Map<*, *>): SetEntry = SetEntry(
            weight = (map["weight"] ?: map["w"])?.toString().orEmpty(),
            reps = (map["reps"] ?: map["r"])?.toString().orEmpty()
        )
    }
}

@Keep
@Serializable
data class LoggedExercise(
    val name: String = "",
    val sets: List<SetEntry> = emptyList()
) {
    fun topWeight(): Double = sets.maxOfOrNull { it.weightDouble() } ?: 0.0
    fun topE1Rm(): Double = sets.maxOfOrNull { it.estimated1Rm() } ?: 0.0

    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "sets" to sets.map { it.toMap() }
    )

    companion object {
        fun fromMap(map: Map<*, *>): LoggedExercise {
            val rawSets = map["sets"] as? List<*> ?: emptyList<Any>()
            return LoggedExercise(
                name = ((map["name"] ?: map["n"]) as? String).orEmpty(),
                sets = rawSets.mapNotNull { (it as? Map<*, *>)?.let(SetEntry::fromMap) }
            )
        }
    }
}

@Keep
data class WorkoutSession(
    val id: String = "",
    val userId: String = "",
    val date: String = todayIso(),
    val dayIndex: Int = 0,
    val note: String = "",
    val exercises: Map<String, LoggedExercise> = emptyMap(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "date" to date,
        "dayIndex" to dayIndex.coerceIn(0, 13),
        "note" to note.take(2000),
        "exercises" to exercises.mapValues { (_, v) -> v.toMap() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "date" to date,
        "dayIndex" to dayIndex.coerceIn(0, 13),
        "note" to note.take(2000),
        "exercises" to exercises.mapValues { (_, v) -> v.toMap() },
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): WorkoutSession? {
            if (!doc.exists()) return null
            val data = doc.data ?: return null
            val exRaw = data["exercises"] as? Map<*, *> ?: emptyMap<Any, Any>()
            val exMap = exRaw.entries.mapNotNull { (k, v) ->
                val ks = k?.toString() ?: return@mapNotNull null
                val vm = v as? Map<*, *> ?: return@mapNotNull null
                ks to LoggedExercise.fromMap(vm)
            }.toMap()
            return WorkoutSession(
                id = doc.id,
                userId = (data["userId"] as? String).orEmpty(),
                date = (data["date"] as? String).orEmpty(),
                dayIndex = (data["dayIndex"] as? Number)?.toInt() ?: 0,
                note = (data["note"] as? String).orEmpty(),
                exercises = exMap,
                createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            )
        }
    }
}

@Keep
@Serializable
data class LoggedFoodItem(
    val id: String = "",
    val name: String = "",
    val source: String = "e",
    val grams: Double = 100.0,
    val mealIndex: Int = 0,
    val nutrients: List<Double> = NutritionConstants.emptyNutrientVector(),
    val retinolMcgPer100g: Double? = null,
    val provitaminARaeMcgPer100g: Double? = null,
    val betaCaroteneMcgPer100g: Double? = null
) {
    fun nutrientAmount(key: String): Double {
        val idx = NutritionConstants.NUTRIENT_INDEX[key] ?: return 0.0
        val v = nutrients.getOrNull(idx) ?: -1.0
        return if (v >= 0.0) v * grams / 100.0 else 0.0
    }

    fun toMap(): Map<String, Any> = buildMap {
        put("id", id)
        put("name", name.take(200))
        put("source", source)
        put("grams", grams)
        put("mealIndex", mealIndex.coerceIn(0, 4))
        put("nutrients", normalizeNutrients(nutrients))
        retinolMcgPer100g?.takeIf { it >= 0.0 }?.let { put("retinolMcgPer100g", it) }
        provitaminARaeMcgPer100g?.takeIf { it >= 0.0 }?.let { put("provitaminARaeMcgPer100g", it) }
        betaCaroteneMcgPer100g?.takeIf { it >= 0.0 }?.let { put("betaCaroteneMcgPer100g", it) }
    }

    companion object {
        fun normalizeNutrients(list: List<Double>): List<Double> =
            if (list.size == NutritionConstants.NUTRIENT_COUNT) list else List(NutritionConstants.NUTRIENT_COUNT) { i -> list.getOrNull(i) ?: -1.0 }

        fun fromMap(map: Map<*, *>): LoggedFoodItem {
            val rawNut = map["nutrients"] as? List<*> ?: emptyList<Any>()
            val nut = List(NutritionConstants.NUTRIENT_COUNT) { i -> (rawNut.getOrNull(i) as? Number)?.toDouble() ?: -1.0 }
            return LoggedFoodItem(
                id = (map["id"] as? String).orEmpty().ifBlank { java.util.UUID.randomUUID().toString() },
                name = (map["name"] as? String).orEmpty(),
                source = (map["source"] as? String) ?: "e",
                grams = (map["grams"] as? Number)?.toDouble() ?: 100.0,
                mealIndex = ((map["mealIndex"] as? Number)?.toInt() ?: 0).coerceIn(0, 4),
                nutrients = nut,
                retinolMcgPer100g = (map["retinolMcgPer100g"] as? Number)?.toDouble(),
                provitaminARaeMcgPer100g = (map["provitaminARaeMcgPer100g"] as? Number)?.toDouble(),
                betaCaroteneMcgPer100g = (map["betaCaroteneMcgPer100g"] as? Number)?.toDouble()
            )
        }
    }
}

@Keep
data class NutritionDay(
    val id: String = "",
    val userId: String = "",
    val date: String = todayIso(),
    val waterMl: Int = 0,
    val items: List<LoggedFoodItem> = emptyList(),
    val steps: Int = 0,
    val stepTarget: Int = StepsBurnEngine.DEFAULT_STEP_TARGET,
    val extraCardioKcal: Int = 0,
    val stepBufferFactor: Double = StepsBurnEngine.DEFAULT_BUFFER_FACTOR,
    val kcalPerStepOverride: Double = 0.0,
    val stepsSource: String = "auto",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    private fun normalizedStepsSource(): String =
        if (stepsSource == "manual") "manual" else "auto"

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "date" to date,
        "waterMl" to waterMl.coerceIn(0, 20000),
        "items" to items.take(100).map { it.toMap() },
        "steps" to steps.coerceIn(0, 150000),
        "stepTarget" to stepTarget.coerceIn(1000, 50000),
        "extraCardioKcal" to extraCardioKcal.coerceIn(0, 5000),
        "stepBufferFactor" to stepBufferFactor.coerceIn(0.0, 1.0),
        "kcalPerStepOverride" to kcalPerStepOverride.coerceIn(0.0, 0.2),
        "stepsSource" to normalizedStepsSource(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "date" to date,
        "waterMl" to waterMl.coerceIn(0, 20000),
        "items" to items.take(100).map { it.toMap() },
        "steps" to steps.coerceIn(0, 150000),
        "stepTarget" to stepTarget.coerceIn(1000, 50000),
        "extraCardioKcal" to extraCardioKcal.coerceIn(0, 5000),
        "stepBufferFactor" to stepBufferFactor.coerceIn(0.0, 1.0),
        "kcalPerStepOverride" to kcalPerStepOverride.coerceIn(0.0, 0.2),
        "stepsSource" to normalizedStepsSource(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): NutritionDay? {
            if (!doc.exists()) return null
            val data = doc.data ?: return null
            val rawItems = data["items"] as? List<*> ?: emptyList<Any>()
            val rawSource = data["stepsSource"] as? String
            val safeSource = if (rawSource == "manual") "manual" else "auto"
            return NutritionDay(
                id = doc.id,
                userId = (data["userId"] as? String).orEmpty(),
                date = (data["date"] as? String) ?: doc.id,
                waterMl = (data["waterMl"] as? Number)?.toInt() ?: 0,
                items = rawItems.mapNotNull { (it as? Map<*, *>)?.let(LoggedFoodItem::fromMap) },
                steps = (data["steps"] as? Number)?.toInt()?.coerceIn(0, 150000) ?: 0,
                stepTarget = (data["stepTarget"] as? Number)?.toInt()?.coerceIn(1000, 50000)
                    ?: StepsBurnEngine.DEFAULT_STEP_TARGET,
                extraCardioKcal = (data["extraCardioKcal"] as? Number)?.toInt()?.coerceIn(0, 5000) ?: 0,
                stepBufferFactor = (data["stepBufferFactor"] as? Number)?.toDouble()?.coerceIn(0.0, 1.0)
                    ?: StepsBurnEngine.DEFAULT_BUFFER_FACTOR,
                kcalPerStepOverride = (data["kcalPerStepOverride"] as? Number)?.toDouble()?.coerceIn(0.0, 0.2)
                    ?: 0.0,
                stepsSource = safeSource,
                createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            )
        }
    }
}

@Keep
data class BodyRecord(
    val id: String = "",
    val userId: String = "",
    val date: String = todayIso(),
    val weightKg: Double? = null,
    val smmKg: Double? = null,
    val pbfPercent: Double? = null,
    val bfmKg: Double? = null,
    val ffmKg: Double? = null,
    val tbwL: Double? = null,
    val proteinKg: Double? = null,
    val mineralsKg: Double? = null,
    val bmrKcal: Double? = null,
    val vfl: Double? = null,
    val whr: Double? = null,
    val score: Double? = null,
    val targetWeightKg: Double? = null,
    val fatControlKg: Double? = null,
    val muscleControlKg: Double? = null,
    val bmi: Double? = null,
    val obesityDegree: Double? = null,
    val heightCm: Double? = null,
    val segmentalLeanKg: Map<String, Double> = emptyMap(),
    val segmentalLeanPercent: Map<String, Double> = emptyMap(),
    val segmentalFatKg: Map<String, Double> = emptyMap(),
    val ranges: Map<String, List<Double>> = emptyMap(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun metricValue(key: String): Double? = when (key) {
        "w" -> weightKg
        "f" -> pbfPercent
        "m" -> smmKg
        "bfm" -> bfmKg
        "ffm" -> ffmKg
        "bmr" -> bmrKcal
        "vfl" -> vfl
        else -> weightKg
    }

    fun rangeStatus(key: String, value: Double?): String? {
        if (value == null || value <= 0.0) return null
        val rg = ranges[key] ?: return null
        if (rg.size < 2) return null
        return when {
            value < rg[0] -> "أقل من المعدل"
            value > rg[1] -> "أعلى من المعدل"
            else -> "ضمن المعدل"
        }
    }

    fun mathWarnings(): List<String> {
        val w = mutableListOf<String>()
        val wt = weightKg ?: 0.0
        val pro = proteinKg ?: 0.0
        val min = mineralsKg ?: 0.0
        val bfm = bfmKg ?: 0.0
        val tbw = tbwL ?: 0.0
        val pbf = pbfPercent ?: 0.0
        val ffm = ffmKg ?: 0.0
        val smm = smmKg ?: 0.0

        if (wt > 0 && pro > 0 && min > 0 && bfm > 0 && tbw > 0) {
            val s = pro + min + bfm + tbw
            if (abs(s - wt) > 1.5) {
                w.add("مجموع الماء والبروتين والمعادن والدهون (${(s * 10).roundToInt() / 10.0}) مش بيطابق الوزن ($wt). راجع الأرقام.")
            }
        }
        if (wt > 0 && bfm > 0 && pbf > 0 && abs(bfm / wt * 100.0 - pbf) > 0.8) {
            val calc = (bfm / wt * 1000.0).roundToInt() / 10.0
            w.add("كتلة الدهون ÷ الوزن بتدّي $calc% ومش بتطابق نسبة الدهون $pbf%.")
        }
        if (wt > 0 && bfm > 0 && ffm > 0 && abs(wt - bfm - ffm) > 1.0) {
            w.add("الوزن − الدهون مش بيساوي الكتلة الخالية من الدهون. راجع الأرقام.")
        }
        if (smm > 0 && ffm > 0 && smm > ffm) {
            w.add("العضل الهيكلي أكبر من الكتلة الخالية من الدهون، وده مش منطقي.")
        }
        if (wt > 0 && (wt < 30 || wt > 250)) {
            w.add("الوزن غريب. راجع الرقم.")
        }
        if (pbf > 0 && (pbf < 3 || pbf > 60)) {
            w.add("نسبة الدهون غريبة. راجع الرقم.")
        }
        return w
    }

    private fun baseOptionalFields(): Map<String, Any> {
        val m = linkedMapOf<String, Any>()
        weightKg?.takeIf { it in 20.0..350.0 }?.let { m["weightKg"] = it }
        smmKg?.takeIf { it in 5.0..150.0 }?.let { m["smmKg"] = it }
        pbfPercent?.takeIf { it in 2.0..75.0 }?.let { m["pbfPercent"] = it }
        bfmKg?.takeIf { it in 1.0..250.0 }?.let { m["bfmKg"] = it }
        ffmKg?.takeIf { it in 15.0..200.0 }?.let { m["ffmKg"] = it }
        tbwL?.takeIf { it in 10.0..150.0 }?.let { m["tbwL"] = it }
        proteinKg?.takeIf { it in 2.0..50.0 }?.let { m["proteinKg"] = it }
        mineralsKg?.takeIf { it in 0.5..20.0 }?.let { m["mineralsKg"] = it }
        bmrKcal?.takeIf { it in 500.0..6000.0 }?.let { m["bmrKcal"] = it }
        vfl?.takeIf { it in 0.0..50.0 }?.let { m["vfl"] = it }
        whr?.takeIf { it in 0.4..2.0 }?.let { m["whr"] = it }
        score?.takeIf { it in 0.0..120.0 }?.let { m["score"] = it }
        targetWeightKg?.takeIf { it in 20.0..300.0 }?.let { m["targetWeightKg"] = it }
        fatControlKg?.takeIf { it in -200.0..200.0 }?.let { m["fatControlKg"] = it }
        muscleControlKg?.takeIf { it in -100.0..100.0 }?.let { m["muscleControlKg"] = it }
        bmi?.takeIf { it in 10.0..80.0 }?.let { m["bmi"] = it }
        obesityDegree?.takeIf { it in 40.0..400.0 }?.let { m["obesityDegree"] = it }
        heightCm?.takeIf { it in 100.0..250.0 }?.let { m["heightCm"] = it }
        if (segmentalLeanKg.isNotEmpty()) m["segmentalLeanKg"] = segmentalLeanKg
        if (segmentalLeanPercent.isNotEmpty()) m["segmentalLeanPercent"] = segmentalLeanPercent
        if (segmentalFatKg.isNotEmpty()) m["segmentalFatKg"] = segmentalFatKg
        if (ranges.isNotEmpty()) m["ranges"] = ranges
        return m
    }

    fun toCreateMap(uid: String): Map<String, Any> = linkedMapOf<String, Any>(
        "userId" to uid,
        "date" to date,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).apply { putAll(baseOptionalFields()) }

    fun toUpdateMap(): Map<String, Any> = linkedMapOf<String, Any>(
        "date" to date,
        "updatedAt" to FieldValue.serverTimestamp()
    ).apply { putAll(baseOptionalFields()) }

    companion object {
        private fun parseDoubleMap(raw: Any?): Map<String, Double> {
            val map = raw as? Map<*, *> ?: return emptyMap()
            return map.entries.mapNotNull { (k, v) ->
                val ks = k as? String
                val vd = (v as? Number)?.toDouble()
                if (ks != null && vd != null) ks to vd else null
            }.toMap()
        }

        private fun parseRangesMap(raw: Any?): Map<String, List<Double>> {
            val map = raw as? Map<*, *> ?: return emptyMap()
            return map.entries.mapNotNull { (k, v) ->
                val ks = k as? String ?: return@mapNotNull null
                val list = (v as? List<*>)?.mapNotNull { (it as? Number)?.toDouble() }
                if (list != null && list.size >= 2) ks to list else null
            }.toMap()
        }

        fun fromSnapshot(doc: DocumentSnapshot): BodyRecord? {
            if (!doc.exists()) return null
            val data = doc.data ?: return null
            fun d(k: String): Double? = (data[k] as? Number)?.toDouble()
            return BodyRecord(
                id = doc.id,
                userId = (data["userId"] as? String).orEmpty(),
                date = (data["date"] as? String) ?: doc.id,
                weightKg = d("weightKg"),
                smmKg = d("smmKg"),
                pbfPercent = d("pbfPercent"),
                bfmKg = d("bfmKg"),
                ffmKg = d("ffmKg"),
                tbwL = d("tbwL"),
                proteinKg = d("proteinKg"),
                mineralsKg = d("mineralsKg"),
                bmrKcal = d("bmrKcal"),
                vfl = d("vfl"),
                whr = d("whr"),
                score = d("score"),
                targetWeightKg = d("targetWeightKg"),
                fatControlKg = d("fatControlKg"),
                muscleControlKg = d("muscleControlKg"),
                bmi = d("bmi"),
                obesityDegree = d("obesityDegree"),
                heightCm = d("heightCm"),
                segmentalLeanKg = parseDoubleMap(data["segmentalLeanKg"]),
                segmentalLeanPercent = parseDoubleMap(data["segmentalLeanPercent"]),
                segmentalFatKg = parseDoubleMap(data["segmentalFatKg"]),
                ranges = parseRangesMap(data["ranges"]),
                createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            )
        }
    }
}

@Keep
@Serializable
data class RecipeIngredient(
    val name: String = "",
    val grams: Double = 100.0,
    val methodCode: String = "raw",
    val nutrients: List<Double> = NutritionConstants.emptyNutrientVector()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name.take(200),
        "grams" to grams,
        "methodCode" to methodCode
    )

    companion object {
        fun fromMap(map: Map<*, *>): RecipeIngredient = RecipeIngredient(
            name = (map["name"] as? String).orEmpty(),
            grams = (map["grams"] as? Number)?.toDouble() ?: 100.0,
            methodCode = (map["methodCode"] as? String) ?: "raw"
        )
    }
}

@Keep
data class CustomFood(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val source: String = "c",
    val barcode: String = "",
    val refusePercent: Double = 0.0,
    val favorite: Boolean = false,
    val custom: Boolean = true,
    val nutrients: List<Double> = NutritionConstants.emptyNutrientVector(),
    val portions: List<PortionOption> = emptyList(),
    val recipeServings: Int? = null,
    val recipeCookedWeight: Double? = null,
    val recipeIngredients: List<RecipeIngredient>? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toFoodItem(): FoodItem = FoodItem(
        id = id,
        name = name,
        englishName = "",
        category = "",
        source = source,
        nutrients = LoggedFoodItem.normalizeNutrients(nutrients),
        portions = portions,
        refusePercent = refusePercent,
        barcode = barcode
    )

    fun toCreateMap(uid: String): Map<String, Any> {
        val m = linkedMapOf<String, Any>(
            "userId" to uid,
            "name" to name.trim().take(200),
            "source" to if (source in listOf("c", "l", "a", "e", "u", "s", "f")) source else "c",
            "barcode" to barcode.take(64),
            "refusePercent" to refusePercent.coerceIn(0.0, 100.0),
            "favorite" to favorite,
            "custom" to custom,
            "nutrients" to LoggedFoodItem.normalizeNutrients(nutrients),
            "portions" to portions.take(30).map { it.toMap() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        recipeServings?.takeIf { it in 1..200 }?.let { m["recipeServings"] = it }
        recipeCookedWeight?.takeIf { it in 0.0..100000.0 }?.let { m["recipeCookedWeight"] = it }
        recipeIngredients?.let { m["recipeIngredients"] = it.take(50).map { ing -> ing.toMap() } }
        return m
    }

    fun toUpdateMap(): Map<String, Any> {
        val m = linkedMapOf<String, Any>(
            "name" to name.trim().take(200),
            "source" to if (source in listOf("c", "l", "a", "e", "u", "s", "f")) source else "c",
            "barcode" to barcode.take(64),
            "refusePercent" to refusePercent.coerceIn(0.0, 100.0),
            "favorite" to favorite,
            "custom" to custom,
            "nutrients" to LoggedFoodItem.normalizeNutrients(nutrients),
            "portions" to portions.take(30).map { it.toMap() },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        recipeServings?.takeIf { it in 1..200 }?.let { m["recipeServings"] = it }
        recipeCookedWeight?.takeIf { it in 0.0..100000.0 }?.let { m["recipeCookedWeight"] = it }
        recipeIngredients?.let { m["recipeIngredients"] = it.take(50).map { ing -> ing.toMap() } }
        return m
    }

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): CustomFood? {
            if (!doc.exists()) return null
            val data = doc.data ?: return null
            val rawNut = data["nutrients"] as? List<*> ?: emptyList<Any>()
            val nut = List(NutritionConstants.NUTRIENT_COUNT) { i -> (rawNut.getOrNull(i) as? Number)?.toDouble() ?: -1.0 }
            val rawPor = data["portions"] as? List<*> ?: emptyList<Any>()
            val rawIng = data["recipeIngredients"] as? List<*>
            return CustomFood(
                id = doc.id,
                userId = (data["userId"] as? String).orEmpty(),
                name = (data["name"] as? String).orEmpty(),
                source = (data["source"] as? String) ?: "c",
                barcode = (data["barcode"] as? String).orEmpty(),
                refusePercent = (data["refusePercent"] as? Number)?.toDouble() ?: 0.0,
                favorite = (data["favorite"] as? Boolean) ?: false,
                custom = (data["custom"] as? Boolean) ?: true,
                nutrients = nut,
                portions = rawPor.mapNotNull { (it as? Map<*, *>)?.let(PortionOption::fromMap) },
                recipeServings = (data["recipeServings"] as? Number)?.toInt(),
                recipeCookedWeight = (data["recipeCookedWeight"] as? Number)?.toDouble(),
                recipeIngredients = rawIng?.mapNotNull { (it as? Map<*, *>)?.let(RecipeIngredient::fromMap) },
                createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            )
        }
    }
}

data class FoodItem(
    val id: String = "",
    val name: String,
    val englishName: String = "",
    val category: String = "",
    val source: String = "e",
    val nutrients: List<Double>,
    val portions: List<PortionOption> = emptyList(),
    val refusePercent: Double = 0.0,
    val barcode: String = ""
) {
    fun kcalPer100(): Int = nutrients.getOrNull(0)?.takeIf { it >= 0 }?.roundToInt() ?: 0
}

data class NutritionTargets(
    val weightKg: Double,
    val bmr: Int,
    val tdee: Int,
    val kcal: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
    val hint: String
)

data class AiDetectedFoodRaw(
    val nameEn: String,
    val nameAr: String,
    val grams: Double,
    val fallbackPer100: Map<String, Double?>
)

data class AiFoodReviewItem(
    val id: String,
    val detectedName: String,
    val grams: Double,
    val selected: Boolean = true,
    val fallbackVector: List<Double>,
    val candidates: List<FoodItem>,
    val selectedCandidateIndex: Int, // -1 means AI estimate
    val baselineCookingMethod: CookingMethodPreset = CookingMethodEngine.inferBaselineMethod(detectedName),
    val selectedCookingMethod: CookingMethodPreset = baselineCookingMethod
) {
    fun activeNutrients(): List<Double> {
        val base = if (selectedCandidateIndex in candidates.indices) {
            candidates[selectedCandidateIndex].nutrients
        } else {
            fallbackVector
        }
        val merged = List(NutritionConstants.NUTRIENT_COUNT) { idx ->
            val primary = base.getOrNull(idx) ?: -1.0
            if (primary >= 0.0) primary else (fallbackVector.getOrNull(idx) ?: -1.0)
        }
        return CookingMethodEngine.applyMethodDelta(
            basePer100 = merged,
            baseline = baselineCookingMethod,
            selected = selectedCookingMethod
        )
    }

    fun activeName(): String {
        val baseName = if (selectedCandidateIndex in candidates.indices) candidates[selectedCandidateIndex].name else detectedName
        return if (selectedCookingMethod != baselineCookingMethod) {
            "$baseName (${selectedCookingMethod.labelAr})"
        } else baseName
    }

    fun activeSource(): String =
        if (selectedCandidateIndex in candidates.indices) candidates[selectedCandidateIndex].source else "a"
}

data class AiLabelResult(
    val name: String,
    val barcode: String,
    val servingGrams: Double?,
    val nutrientsPer100: Map<String, Double?>,
    val isSupplement: Boolean = false,
    val servingUnit: String = ""
)
