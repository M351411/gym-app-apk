package com.mutabei.workout.data.model

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Quick calorie-translation buffer presets for active burn from steps & cardio.
 * Default is SAFE_HALF (`0.5` = 50% safe buffer) to guard against pedometer
 * overestimation and protect the daily cutting deficit.
 */
enum class StepBufferPreset(
    val factor: Double,
    val percentLabel: String,
    val titleAr: String,
    val noteAr: String
) {
    ZERO(
        factor = 0.0,
        percentLabel = "0%",
        titleAr = "حجز العجز",
        noteAr = "كل حرق الخطوات بيفضل بونص إضافي للتنشيف بدون زيادة سعرات الأكل النهارده."
    ),
    SAFE_HALF(
        factor = 0.5,
        percentLabel = "50%",
        titleAr = "أمان (افتراضي)",
        noteAr = "نضيف نصف الحرق فقط لحماية عجز السعرات من مبالغة عدّادات الخطوات."
    ),
    FULL(
        factor = 1.0,
        percentLabel = "100%",
        titleAr = "تعويض كامل",
        noteAr = "تعويض كامل لحرق الخطوات — مناسب لو خطواتك عالية جداً ومحتاج طاقة للتمرين."
    );

    companion object {
        fun fromFactor(factor: Double): StepBufferPreset =
            entries.minByOrNull { abs(it.factor - factor) } ?: SAFE_HALF
    }
}

data class StepsBurnSummary(
    val steps: Int,
    val stepTarget: Int,
    val progressFraction: Float,
    val estimatedDistanceKm: Double,
    val weightKg: Double?,
    val bmrReferenceKcal: Int,
    val estimatedTdeeKcal: Int,
    val effectiveKcalPerStep: Double,
    val isCustomKcalPerStep: Boolean,
    val stepBurnKcal: Int,
    val extraCardioKcal: Int,
    val totalActiveBurnKcal: Int,
    val bufferFactor: Double,
    val activePreset: StepBufferPreset,
    val baseCalorieTarget: Int,
    val addedCaloriesFromSteps: Int,
    val addedCaloriesTotal: Int,
    val adjustedCalorieTarget: Int,
    val protectedDeficitBufferKcal: Int,
    val stepsSource: String = "auto"
)

object StepsBurnEngine {
    const val DEFAULT_STEP_TARGET = 10_000
    const val DEFAULT_REFERENCE_BMR_KCAL = 1_600
    const val DEFAULT_BASE_CUTTING_KCAL = 2_000
    const val DEFAULT_KCAL_PER_STEP = 0.04
    const val DEFAULT_BUFFER_FACTOR = 0.5
    private const val SEDENTARY_LIVING_FACTOR = 1.2

    /**
     * Weight-aware kcal/step estimate anchored around ~0.04 kcal/step at 75 kg.
     * Falls back cleanly to 0.04 kcal/step if weight is not available.
     */
    fun weightAwareKcalPerStep(weightKg: Double?): Double {
        if (weightKg == null || weightKg <= 0.0) return DEFAULT_KCAL_PER_STEP
        val raw = (weightKg / 75.0) * DEFAULT_KCAL_PER_STEP
        val clamped = raw.coerceIn(0.025, 0.070)
        return (clamped * 1000.0).roundToInt() / 1000.0
    }

    fun estimateDistanceKm(steps: Int): Double =
        ((steps.coerceAtLeast(0) * 0.00075) * 10.0).roundToInt() / 10.0

    fun evaluate(
        day: NutritionDay?,
        baseTargetKcal: Int?,
        weightKg: Double?,
        bmrKcal: Int? = null,
        defaultBufferFactor: Double = DEFAULT_BUFFER_FACTOR,
        defaultStepTarget: Int = DEFAULT_STEP_TARGET,
        liveAutoStepsOverride: Int? = null
    ): StepsBurnSummary {
        val source = if (day?.stepsSource == "manual") "manual" else "auto"
        val rawSteps = if (source == "auto" && liveAutoStepsOverride != null) {
            maxOf(day?.steps ?: 0, liveAutoStepsOverride)
        } else {
            day?.steps ?: 0
        }
        val steps = rawSteps.coerceIn(0, 150_000)
        val target = (day?.stepTarget ?: defaultStepTarget).coerceIn(1_000, 50_000)
        val extraCardio = (day?.extraCardioKcal ?: 0).coerceIn(0, 5_000)
        val buffer = (day?.stepBufferFactor ?: defaultBufferFactor).coerceIn(0.0, 1.0)
        val customRate = day?.kcalPerStepOverride ?: 0.0
        val isCustomRate = customRate > 0.0
        val effectiveRate = if (isCustomRate) {
            customRate.coerceIn(0.015, 0.2)
        } else {
            weightAwareKcalPerStep(weightKg)
        }

        val referenceBmr = bmrKcal?.takeIf { it > 0 } ?: DEFAULT_REFERENCE_BMR_KCAL
        val stepBurn = (steps * effectiveRate).roundToInt()
        val totalActiveBurn = stepBurn + extraCardio
        // Distinguish resting BMR (1,600 kcal) from daily non-step baseline (~1.2x BMR) + active step/cardio burn
        val estimatedTdee = (referenceBmr * SEDENTARY_LIVING_FACTOR).roundToInt() + totalActiveBurn
        val addedFromSteps = (stepBurn * buffer).roundToInt()
        val addedTotal = (totalActiveBurn * buffer).roundToInt()
        val baseKcal = (baseTargetKcal?.takeIf { it > 0 } ?: DEFAULT_BASE_CUTTING_KCAL)
        val adjustedTarget = baseKcal + addedTotal
        val protectedBuffer = (totalActiveBurn - addedTotal).coerceAtLeast(0)

        return StepsBurnSummary(
            steps = steps,
            stepTarget = target,
            progressFraction = (steps.toFloat() / target.toFloat()).coerceIn(0f, 1f),
            estimatedDistanceKm = estimateDistanceKm(steps),
            weightKg = weightKg?.takeIf { it > 0.0 },
            bmrReferenceKcal = referenceBmr,
            estimatedTdeeKcal = estimatedTdee,
            effectiveKcalPerStep = effectiveRate,
            isCustomKcalPerStep = isCustomRate,
            stepBurnKcal = stepBurn,
            extraCardioKcal = extraCardio,
            totalActiveBurnKcal = totalActiveBurn,
            bufferFactor = buffer,
            activePreset = StepBufferPreset.fromFactor(buffer),
            baseCalorieTarget = baseKcal,
            addedCaloriesFromSteps = addedFromSteps,
            addedCaloriesTotal = addedTotal,
            adjustedCalorieTarget = adjustedTarget,
            protectedDeficitBufferKcal = protectedBuffer,
            stepsSource = source
        )
    }
}

/**
 * Live workout summary for the active split day shared across Hub & Nutrition.
 */
data class UnifiedWorkoutSnapshot(
    val splitDayName: String?,
    val doneSets: Int,
    val totalSets: Int,
    val progressFraction: Float,
    val isTimerRunning: Boolean,
    val elapsedSeconds: Long
)

/**
 * Single source of truth connecting Body (weight & BMR = 1,600 kcal reference),
 * Steps & Active Burn (default 50% safe buffer), Nutrition targets (protein-protected),
 * and live Workout session progress for a given date.
 */
data class UnifiedDailyContext(
    val date: String,
    val latestBodyRecord: BodyRecord?,
    val effectiveWeightKg: Double?,
    val bmrReferenceKcal: Int,
    val isBmrFromInBody: Boolean,
    val baseTargets: NutritionTargets?,
    val rollingBudget: RollingCalorieBudget?,
    val workoutSync: WorkoutSyncedMacros?,
    val stepsBurn: StepsBurnSummary,
    val adjustedTargets: NutritionTargets?,
    val consumedKcal: Int,
    val consumedProteinG: Int,
    val workout: UnifiedWorkoutSnapshot
)

object ConnectedAppEngine {

    /**
     * Resolves the effective resting BMR reference (`1,600 kcal` baseline unless a logged
     * InBody reading provides a newer measured BMR) and returns `(bmrKcal, isFromInBody)`.
     */
    fun resolveBmrReference(bodyRecords: List<BodyRecord>): Pair<Int, Boolean> {
        val measured = bodyRecords
            .filter { (it.bmrKcal ?: 0.0) > 0.0 }
            .maxByOrNull { it.date }
            ?.bmrKcal
            ?.roundToInt()
        return if (measured != null && measured > 0) {
            measured to true
        } else {
            StepsBurnEngine.DEFAULT_REFERENCE_BMR_KCAL to false
        }
    }

    /**
     * Resolves the current body weight from config override or latest BodyRecord.
     */
    fun resolveEffectiveWeight(config: UserConfig, bodyRecords: List<BodyRecord>): Double? {
        if (config.weightOverride > 0.0) return config.weightOverride
        return bodyRecords
            .filter { (it.weightKg ?: 0.0) > 0.0 }
            .maxByOrNull { it.date }
            ?.weightKg
    }

    /**
     * Applies buffered step/cardio burn calories onto daily nutrition targets while
     * strictly protecting the daily protein target (`proteinG` is never reduced) and
     * channeling added activity calories cleanly into carbs (~75%) and fats (~25%).
     */
    fun applyStepsBurnToTargets(
        baseTargets: NutritionTargets?,
        rollingBudget: RollingCalorieBudget?,
        workoutSync: WorkoutSyncedMacros?,
        stepsBurn: StepsBurnSummary
    ): NutritionTargets? {
        if (baseTargets == null) return null
        val preStepKcal = rollingBudget?.adjustedTodayTarget ?: baseTargets.kcal
        val addedKcal = stepsBurn.addedCaloriesTotal.coerceAtLeast(0)
        val finalKcal = preStepKcal + addedKcal

        val baseCarbs = workoutSync?.carbsG ?: baseTargets.carbsG
        val baseFat = workoutSync?.fatG ?: baseTargets.fatG

        val extraCarbsG = if (addedKcal > 0) ((addedKcal * 0.75) / 4.0).roundToInt() else 0
        val extraFatG = if (addedKcal > 0) ((addedKcal * 0.25) / 9.0).roundToInt() else 0

        return baseTargets.copy(
            bmr = stepsBurn.bmrReferenceKcal,
            tdee = stepsBurn.estimatedTdeeKcal,
            kcal = finalKcal,
            proteinG = baseTargets.proteinG, // Protected protein floor
            carbsG = (baseCarbs + extraCarbsG).coerceAtLeast(0),
            fatG = (baseFat + extraFatG).coerceAtLeast(0)
        )
    }

    /**
     * Evaluates the live workout session snapshot for the selected split day.
     */
    fun evaluateWorkoutSnapshot(
        date: String,
        config: UserConfig,
        sessions: Map<String, WorkoutSession>,
        selectedDayIndex: Int,
        workoutDrafts: Map<String, List<SetEntry>>,
        isTimerRunning: Boolean,
        elapsedSeconds: Long
    ): UnifiedWorkoutSnapshot {
        val split = config.effectiveSplit()
        val safeIdx = selectedDayIndex.coerceIn(0, (split.size - 1).coerceAtLeast(0))
        val day = split.getOrNull(safeIdx)
            ?: return UnifiedWorkoutSnapshot(
                splitDayName = null,
                doneSets = 0,
                totalSets = 0,
                progressFraction = 0f,
                isTimerRunning = isTimerRunning,
                elapsedSeconds = elapsedSeconds
            )

        val session = sessions["${date}_$safeIdx"]
        val totalSets = day.exercises.sumOf { it.workingSets }.coerceAtLeast(1)
        val doneSets = day.exercises.indices.sumOf { exIdx ->
            val spec = day.exercises[exIdx]
            val altKey = "$safeIdx|$exIdx"
            val chosenName = config.alternatives[altKey]?.ifBlank { spec.name } ?: spec.name
            val savedEx = session?.exercises?.get(exIdx.toString())
            val savedSets = if (savedEx != null && savedEx.name == chosenName) savedEx.sets else null
            val activeSets = workoutDrafts[altKey] ?: savedSets ?: emptyList()
            activeSets.count { it.repsInt() > 0 }.coerceAtMost(spec.workingSets)
        }
        return UnifiedWorkoutSnapshot(
            splitDayName = day.name,
            doneSets = doneSets,
            totalSets = totalSets,
            progressFraction = (doneSets.toFloat() / totalSets.toFloat()).coerceIn(0f, 1f),
            isTimerRunning = isTimerRunning,
            elapsedSeconds = elapsedSeconds
        )
    }

    /**
     * Builds the unified cross-section context for a given ISO date (`YYYY-MM-DD`).
     */
    fun buildDailyContext(
        date: String,
        config: UserConfig,
        sessions: Map<String, WorkoutSession>,
        nutritionDays: Map<String, NutritionDay>,
        bodyRecords: List<BodyRecord>,
        baseTargets: NutritionTargets?,
        rollingBudgetEnabled: Boolean,
        workoutSyncedEnabled: Boolean,
        defaultStepTarget: Int,
        defaultBufferFactor: Double,
        selectedDayIndex: Int,
        workoutDrafts: Map<String, List<SetEntry>>,
        isTimerRunning: Boolean,
        elapsedSeconds: Long,
        liveAutoStepsToday: Int? = null
    ): UnifiedDailyContext {
        val latestBody = bodyRecords.maxByOrNull { it.date }
        val effectiveWeight = resolveEffectiveWeight(config, bodyRecords)
        val (bmrRef, isBmrFromInBody) = resolveBmrReference(bodyRecords)
        val day = nutritionDays[date]

        val rollingBudget = if (baseTargets != null && rollingBudgetEnabled) {
            RollingCalorieBudgetEngine.evaluate(date, baseTargets.kcal, nutritionDays)
        } else null

        val preStepKcal = rollingBudget?.adjustedTodayTarget ?: baseTargets?.kcal ?: StepsBurnEngine.DEFAULT_BASE_CUTTING_KCAL
        val liveAutoOverride = if (date == todayIso()) liveAutoStepsToday else null

        val stepsBurn = StepsBurnEngine.evaluate(
            day = day,
            baseTargetKcal = preStepKcal,
            weightKg = effectiveWeight,
            bmrKcal = bmrRef,
            defaultBufferFactor = defaultBufferFactor,
            defaultStepTarget = defaultStepTarget,
            liveAutoStepsOverride = liveAutoOverride
        )

        val workoutSync = if (baseTargets != null && workoutSyncedEnabled) {
            WorkoutSyncedNutritionEngine.evaluate(
                referenceDate = date,
                baseTargets = baseTargets,
                effectiveKcal = preStepKcal,
                config = config,
                sessions = sessions.values.toList()
            )
        } else null

        val adjustedTargets = applyStepsBurnToTargets(
            baseTargets = baseTargets,
            rollingBudget = rollingBudget,
            workoutSync = workoutSync,
            stepsBurn = stepsBurn
        )

        val items = day?.items.orEmpty()
        val consumedKcal = items.sumOf { it.nutrientAmount("kcal") }.roundToInt()
        val consumedProteinG = items.sumOf { it.nutrientAmount("p") }.roundToInt()

        val workout = evaluateWorkoutSnapshot(
            date = date,
            config = config,
            sessions = sessions,
            selectedDayIndex = selectedDayIndex,
            workoutDrafts = workoutDrafts,
            isTimerRunning = isTimerRunning,
            elapsedSeconds = elapsedSeconds
        )

        return UnifiedDailyContext(
            date = date,
            latestBodyRecord = latestBody,
            effectiveWeightKg = effectiveWeight,
            bmrReferenceKcal = bmrRef,
            isBmrFromInBody = isBmrFromInBody,
            baseTargets = baseTargets,
            rollingBudget = rollingBudget,
            workoutSync = workoutSync,
            stepsBurn = stepsBurn,
            adjustedTargets = adjustedTargets,
            consumedKcal = consumedKcal,
            consumedProteinG = consumedProteinG,
            workout = workout
        )
    }
}

