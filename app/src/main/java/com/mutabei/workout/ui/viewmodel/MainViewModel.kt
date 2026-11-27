package com.mutabei.workout.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.mutabei.workout.data.model.AiDeficiencyFixResult
import com.mutabei.workout.data.model.AiFoodReviewItem
import com.mutabei.workout.data.model.AiLabelResult
import com.mutabei.workout.data.model.AiMacroTetrisResult
import com.mutabei.workout.data.model.AiWeeklyCoachReport
import com.mutabei.workout.data.model.BodyRecord
import com.mutabei.workout.data.model.BuiltReverseMealItem
import com.mutabei.workout.data.model.CoachAutoLoggedFood
import com.mutabei.workout.data.model.CoachChatMessage
import com.mutabei.workout.data.model.CoachReportRecord
import com.mutabei.workout.data.model.CoachSuggestedFood
import com.mutabei.workout.data.model.CoachSuggestedTargets
import com.mutabei.workout.data.model.CookingMethodEngine
import com.mutabei.workout.data.model.CookingMethodPreset
import com.mutabei.workout.data.model.CustomFood
import com.mutabei.workout.data.model.ExerciseSpec
import com.mutabei.workout.data.model.FoodIngredientAuditResult
import com.mutabei.workout.data.model.FoodItem
import com.mutabei.workout.data.model.LoggedExercise
import com.mutabei.workout.data.model.LoggedFoodItem
import com.mutabei.workout.data.model.MicronutrientEngine
import com.mutabei.workout.data.model.MicronutrientEvaluationReport
import com.mutabei.workout.data.model.MicronutrientStatus
import com.mutabei.workout.data.model.NutritionConstants
import com.mutabei.workout.data.model.NutritionDay
import com.mutabei.workout.data.model.NutritionTargets
import com.mutabei.workout.data.model.PortionOption
import com.mutabei.workout.data.model.PreWorkoutGamePlan
import com.mutabei.workout.data.model.RecipeIngredient
import com.mutabei.workout.data.model.SetEntry
import com.mutabei.workout.data.model.SplitDay
import com.mutabei.workout.data.model.SupplementCatalog
import com.mutabei.workout.data.model.UiState
import com.mutabei.workout.data.model.UserConfig
import com.mutabei.workout.data.model.WorkoutDefaults
import com.mutabei.workout.data.model.WorkoutSession
import com.mutabei.workout.data.model.daysBetweenIso
import com.mutabei.workout.data.model.shiftIsoDate
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.data.repository.FirestoreRepository
import com.mutabei.workout.data.repository.GeminiAiRepository
import com.mutabei.workout.data.repository.NutrientFoodSuggestion
import com.mutabei.workout.data.repository.OfflineFoodRepository
import com.mutabei.workout.data.repository.OpenFoodFactsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "MainViewModel"

enum class MainTab { WORKOUT, NUTRITION, BODY }

data class RestTimerState(
    val visible: Boolean = false,
    val totalSeconds: Int = 180,
    val remainingSeconds: Int = 180,
    val finished: Boolean = false
)

data class WorkoutSessionTimerState(
    val isRunning: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val hasStarted: Boolean = false
)

data class AppDataSnapshot(
    val config: UserConfig = UserConfig(),
    val sessions: Map<String, WorkoutSession> = emptyMap(),
    val nutritionDays: Map<String, NutritionDay> = emptyMap(),
    val bodyRecords: List<BodyRecord> = emptyList(),
    val customFoods: List<CustomFood> = emptyList(),
    val coachReports: List<CoachReportRecord> = emptyList()
)

data class BodyDraftState(
    val date: String = todayIso(),
    val fields: Map<String, String> = emptyMap(),
    val segmentalLeanKg: Map<String, String> = emptyMap(),
    val segmentalLeanPercent: Map<String, Double> = emptyMap(),
    val segmentalFatKg: Map<String, Double> = emptyMap(),
    val ranges: Map<String, List<Double>> = emptyMap(),
    val aiBusy: Boolean = false,
    val aiStatusMessage: String? = null
) {
    fun toBodyRecord(): BodyRecord {
        fun d(k: String): Double? = fields[k]?.replace(',', '.')?.trim()?.toDoubleOrNull()
        val sl = segmentalLeanKg.entries.mapNotNull { (k, v) ->
            v.replace(',', '.').trim().toDoubleOrNull()?.let { k to it }
        }.toMap()
        return BodyRecord(
            date = date.ifBlank { todayIso() },
            weightKg = d("w"),
            smmKg = d("m"),
            pbfPercent = d("f"),
            bfmKg = d("bfm"),
            ffmKg = d("ffm"),
            tbwL = d("tbw"),
            proteinKg = d("pro"),
            mineralsKg = d("min"),
            bmrKcal = d("bmr"),
            vfl = d("vfl"),
            whr = d("whr"),
            score = d("score"),
            targetWeightKg = d("tgt"),
            fatControlKg = d("fc"),
            muscleControlKg = d("mc"),
            bmi = d("bmi"),
            obesityDegree = d("obd"),
            heightCm = d("h"),
            segmentalLeanKg = sl,
            segmentalLeanPercent = segmentalLeanPercent,
            segmentalFatKg = segmentalFatKg,
            ranges = ranges
        )
    }
}

data class FoodSheetAiState(
    val busyMessage: String? = null,
    val errorOrInfo: String? = null,
    val detectedItems: List<AiFoodReviewItem> = emptyList(),
    val labelDraft: AiLabelResult? = null,
    val ingredientAudit: FoodIngredientAuditResult? = null,
    val barcodeInput: String = ""
)

data class ExerciseHistoryPoint(
    val date: String,
    val sets: List<SetEntry>
) {
    fun topWeight(): Double = sets.maxOfOrNull { it.weightDouble() } ?: 0.0
    fun topE1Rm(): Double = sets.maxOfOrNull { it.estimated1Rm() } ?: 0.0
}

data class AiDeficiencyUiState(
    val busyMessage: String? = null,
    val errorMessage: String? = null,
    val result: AiDeficiencyFixResult? = null
)

data class AiCoachUiState(
    val busyMessage: String? = null,
    val errorMessage: String? = null,
    val report: AiWeeklyCoachReport? = null,
    val savedToArchive: Boolean = false
)

data class AiPreWorkoutUiState(
    val busyMessage: String? = null,
    val errorMessage: String? = null,
    val plan: PreWorkoutGamePlan? = null
)

data class AiCoachChatUiState(
    val messages: List<CoachChatMessage> = emptyList(),
    val busyMessage: String? = null,
    val errorMessage: String? = null,
    val canRetry: Boolean = false
)

data class AiMacroTetrisUiState(
    val busyMessage: String? = null,
    val errorMessage: String? = null,
    val result: AiMacroTetrisResult? = null
)

class MainViewModel(
    private val appContext: Context,
    private val firestoreRepo: FirestoreRepository,
    private val offlineFoodRepo: OfflineFoodRepository,
    private val geminiRepo: GeminiAiRepository,
    private val offRepo: OpenFoodFactsRepository,
    private val currentUserId: String
) : ViewModel() {

    private val coreDataFlow = combine(
        firestoreRepo.observeUserConfig(currentUserId),
        firestoreRepo.observeWorkoutSessions(currentUserId),
        firestoreRepo.observeNutritionDays(currentUserId),
        firestoreRepo.observeBodyRecords(currentUserId),
        firestoreRepo.observeCustomFoods(currentUserId)
    ) { config, sessions, nutritionDays, bodyRecords, customFoods ->
        AppDataSnapshot(config, sessions, nutritionDays, bodyRecords, customFoods)
    }

    val dataState: StateFlow<UiState<AppDataSnapshot>> = combine(
        coreDataFlow,
        firestoreRepo.observeCoachReports(currentUserId)
    ) { base, coachReports ->
        base.copy(coachReports = coachReports)
    }
        .map<AppDataSnapshot, UiState<AppDataSnapshot>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing Firestore data", root)
            emit(UiState.Error(root.message ?: "تعذّر تحميل البيانات من السحابة."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _selectedTab = MutableStateFlow(MainTab.WORKOUT)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _selectedDayIndex = MutableStateFlow(0)
    val selectedDayIndex: StateFlow<Int> = _selectedDayIndex.asStateFlow()

    private val _selectedNutritionDate = MutableStateFlow(todayIso())
    val selectedNutritionDate: StateFlow<String> = _selectedNutritionDate.asStateFlow()

    // Client-only Nutrition V2 toggles (stored locally to keep Firestore schema untouched)
    private val nutritionV2Prefs by lazy {
        appContext.getSharedPreferences("nutrition_v2_prefs", Context.MODE_PRIVATE)
    }
    private val _rollingCalorieBudgetEnabled = MutableStateFlow(
        appContext.getSharedPreferences("nutrition_v2_prefs", Context.MODE_PRIVATE)
            .getBoolean("rolling_calorie_budget", true)
    )
    val rollingCalorieBudgetEnabled: StateFlow<Boolean> = _rollingCalorieBudgetEnabled.asStateFlow()

    private val _workoutSyncedNutritionEnabled = MutableStateFlow(
        appContext.getSharedPreferences("nutrition_v2_prefs", Context.MODE_PRIVATE)
            .getBoolean("workout_synced_nutrition", true)
    )
    val workoutSyncedNutritionEnabled: StateFlow<Boolean> = _workoutSyncedNutritionEnabled.asStateFlow()

    fun setRollingCalorieBudgetEnabled(enabled: Boolean) {
        _rollingCalorieBudgetEnabled.value = enabled
        nutritionV2Prefs.edit().putBoolean("rolling_calorie_budget", enabled).apply()
    }

    fun setWorkoutSyncedNutritionEnabled(enabled: Boolean) {
        _workoutSyncedNutritionEnabled.value = enabled
        nutritionV2Prefs.edit().putBoolean("workout_synced_nutrition", enabled).apply()
    }

    // Draft sets per "dayIdx|exIdx" while working out
    private val _workoutDrafts = MutableStateFlow<Map<String, List<SetEntry>>>(emptyMap())
    val workoutDrafts: StateFlow<Map<String, List<SetEntry>>> = _workoutDrafts.asStateFlow()

    private val _restTimer = MutableStateFlow(RestTimerState())
    val restTimer: StateFlow<RestTimerState> = _restTimer.asStateFlow()
    private var timerJob: Job? = null

    private val _workoutSessionTimer = MutableStateFlow(WorkoutSessionTimerState())
    val workoutSessionTimer: StateFlow<WorkoutSessionTimerState> = _workoutSessionTimer.asStateFlow()
    private var workoutTimerJob: Job? = null
    private var workoutTimerAccumulatedMs: Long = 0L
    private var workoutTimerStartedEpochMs: Long? = null
    private var workoutTimerManuallyControlled: Boolean = false

    private val _timerFinishedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val timerFinishedEvents: SharedFlow<Unit> = _timerFinishedEvents.asSharedFlow()

    private val _timerPreAlertEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val timerPreAlertEvents: SharedFlow<Unit> = _timerPreAlertEvents.asSharedFlow()

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toastMessages: SharedFlow<String> = _toastMessages.asSharedFlow()

    // Food Search & AI Sheet state
    private val _searchResults = MutableStateFlow<List<FoodItem>>(emptyList())
    val searchResults: StateFlow<List<FoodItem>> = _searchResults.asStateFlow()

    private val _foodSheetAi = MutableStateFlow(FoodSheetAiState())
    val foodSheetAi: StateFlow<FoodSheetAiState> = _foodSheetAi.asStateFlow()

    // Body / InBody draft form state
    private val _bodyDraft = MutableStateFlow(BodyDraftState())
    val bodyDraft: StateFlow<BodyDraftState> = _bodyDraft.asStateFlow()

    // Weekly micro suggestions cache
    private val _microSuggestions = MutableStateFlow<Map<String, List<NutrientFoodSuggestion>>>(emptyMap())
    val microSuggestions: StateFlow<Map<String, List<NutrientFoodSuggestion>>> = _microSuggestions.asStateFlow()

    // Pro AI Features: Micronutrient Deficiency Fixer & Holistic Weekly Coach
    private val _deficiencyAi = MutableStateFlow(AiDeficiencyUiState())
    val deficiencyAi: StateFlow<AiDeficiencyUiState> = _deficiencyAi.asStateFlow()

    private val _coachReport = MutableStateFlow(AiCoachUiState())
    val coachReport: StateFlow<AiCoachUiState> = _coachReport.asStateFlow()

    private val _preWorkoutPlan = MutableStateFlow(AiPreWorkoutUiState())
    val preWorkoutPlan: StateFlow<AiPreWorkoutUiState> = _preWorkoutPlan.asStateFlow()

    private val _coachChat = MutableStateFlow(AiCoachChatUiState())
    val coachChat: StateFlow<AiCoachChatUiState> = _coachChat.asStateFlow()

    private val _macroTetrisAi = MutableStateFlow(AiMacroTetrisUiState())
    val macroTetrisAi: StateFlow<AiMacroTetrisUiState> = _macroTetrisAi.asStateFlow()

    private val _battleResultsA = MutableStateFlow<List<FoodItem>>(emptyList())
    val battleResultsA: StateFlow<List<FoodItem>> = _battleResultsA.asStateFlow()

    private val _battleResultsB = MutableStateFlow<List<FoodItem>>(emptyList())
    val battleResultsB: StateFlow<List<FoodItem>> = _battleResultsB.asStateFlow()

    private val _enrichMicrosBusy = MutableStateFlow<String?>(null)
    val enrichMicrosBusy: StateFlow<String?> = _enrichMicrosBusy.asStateFlow()

    init {
        viewModelScope.launch {
            offlineFoodRepo.ensureLoaded()
            syncRequestedYesterdayMealsOnce()
        }
    }

    private suspend fun syncRequestedYesterdayMealsOnce() {
        val targetDate = "2026-11-24"
        val prefs = appContext.getSharedPreferences("mutabei_user_sync", Context.MODE_PRIVATE)
        val prefKey = "logged_requested_meals_${targetDate}_${currentUserId}_v1"
        if (prefs.getBoolean(prefKey, false)) return

        val snapshot = dataState.filterIsInstance<UiState.Success<AppDataSnapshot>>().first().data
        val existingDay = snapshot.nutritionDays[targetDate]
        if (existingDay != null && existingDay.items.any { it.id.startsWith("req_20261124_") }) {
            prefs.edit().putBoolean(prefKey, true).apply()
            return
        }

        fun makeItem(
            id: String,
            name: String,
            mealIndex: Int,
            grams: Double,
            kcalPer100: Double,
            pPer100: Double,
            fPer100: Double,
            cPer100: Double,
            fibPer100: Double = -1.0,
            naPer100: Double = -1.0,
            caPer100: Double = -1.0
        ): LoggedFoodItem {
            val nut = MutableList(NutritionConstants.NUTRIENT_COUNT) { -1.0 }
            nut[0] = kcalPer100
            nut[1] = pPer100
            nut[2] = fPer100
            nut[3] = cPer100
            if (fibPer100 >= 0.0) NutritionConstants.NUTRIENT_INDEX["fib"]?.let { nut[it] = fibPer100 }
            if (naPer100 >= 0.0) NutritionConstants.NUTRIENT_INDEX["na"]?.let { nut[it] = naPer100 }
            if (caPer100 >= 0.0) NutritionConstants.NUTRIENT_INDEX["ca"]?.let { nut[it] = caPer100 }
            return LoggedFoodItem(
                id = id,
                name = name,
                source = "e",
                grams = grams,
                mealIndex = mealIndex,
                nutrients = nut
            )
        }

        val rawItems = listOf(
            // فطار (Breakfast = 0)
            makeItem("req_20261124_01", "كيك سادة", 0, 35.0, 360.0, 6.5, 15.0, 50.0, 1.0, 320.0),
            makeItem("req_20261124_02", "مخبوزات صغيرة", 0, 30.0, 375.0, 7.5, 18.0, 46.0, 1.8, 420.0),
            makeItem("req_20261124_03", "قرص سادة متوسط", 0, 60.0, 345.0, 7.8, 12.5, 50.5, 2.4, 380.0),
            // غداء (Lunch = 1)
            makeItem("req_20261124_04", "بسلة مطبوخة", 1, 100.0, 75.0, 4.5, 2.5, 9.5, 4.2, 260.0),
            makeItem("req_20261124_05", "أرز بالشعرية", 1, 300.0, 158.0, 3.1, 2.8, 30.0, 0.9, 210.0),
            makeItem("req_20261124_06", "صدر دجاج مطبوخ", 1, 85.0, 165.0, 31.0, 3.6, 0.0, 0.0, 74.0),
            makeItem("req_20261124_07", "فول سوداني", 1, 25.0, 585.0, 26.4, 44.9, 21.7, 8.5, 18.0),
            makeItem("req_20261124_08", "بليلة (قمح مسلوق + لبن + سكر خفيف)", 1, 412.0, 87.0, 2.8, 1.8, 15.2, 1.6, 32.0, 62.0),
            // العشاء (Dinner = 2)
            makeItem("req_20261124_09", "عيش بلدي", 2, 60.0, 254.0, 8.8, 1.0, 53.8, 5.5, 390.0),
            makeItem("req_20261124_10", "جبن فيتا باندا دهن نباتي", 2, 55.0, 250.0, 8.0, 22.0, 5.0, 0.0, 950.0, 280.0)
        )

        val enriched = rawItems.map { offlineFoodRepo.enrichLoggedItem(it, snapshot.customFoods) }
        val baseDay = existingDay ?: NutritionDay(id = targetDate, userId = currentUserId, date = targetDate)
        val dayToSave = baseDay.copy(items = baseDay.items + enriched)
        val res = firestoreRepo.saveNutritionDay(currentUserId, dayToSave)
        if (res.isSuccess) {
            prefs.edit().putBoolean(prefKey, true).apply()
            showToast("تم تسجيل وجبات أمس ($targetDate) — 10 أصناف (1929 سعر) ✅")
        }
    }

    private fun currentSnapshot(): AppDataSnapshot =
        (dataState.value as? UiState.Success)?.data ?: AppDataSnapshot(config = UserConfig(userId = currentUserId))

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun selectWorkoutDay(dayIndex: Int) {
        _selectedDayIndex.value = dayIndex.coerceAtLeast(0)
    }

    fun shiftNutritionDate(deltaDays: Long) {
        _selectedNutritionDate.value = shiftIsoDate(_selectedNutritionDate.value, deltaDays)
    }

    fun selectNutritionDate(isoDate: String) {
        _selectedNutritionDate.value = isoDate
    }

    fun openOfflineMealLogger(forYesterday: Boolean) {
        val targetDate = if (forYesterday) shiftIsoDate(todayIso(), -1) else todayIso()
        _selectedNutritionDate.value = targetDate
        _selectedTab.value = MainTab.NUTRITION
    }

    fun showToast(message: String) {
        _toastMessages.tryEmit(message)
    }

    // ==================== REST TIMER ====================

    fun startRestTimer(seconds: Int? = null) {
        startWorkoutSessionTimer(autoTriggered = true)
        val dur = (seconds ?: currentSnapshot().config.restSeconds).coerceIn(30, 900)
        val endTimeMs = System.currentTimeMillis() + dur * 1000L
        _restTimer.value = RestTimerState(
            visible = true,
            totalSeconds = dur,
            remainingSeconds = dur,
            finished = false
        )
        runTimerLoop(endTimeMs, dur)
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        val st = _restTimer.value
        val newRemaining = max(0, st.remainingSeconds + deltaSeconds)
        val newTotal = if (deltaSeconds > 0) st.totalSeconds + deltaSeconds else st.totalSeconds
        val endTimeMs = System.currentTimeMillis() + newRemaining * 1000L
        _restTimer.value = st.copy(
            totalSeconds = max(1, newTotal),
            remainingSeconds = newRemaining,
            finished = newRemaining <= 0
        )
        if (newRemaining > 0) {
            runTimerLoop(endTimeMs, max(1, newTotal))
        } else {
            timerJob?.cancel()
            _timerFinishedEvents.tryEmit(Unit)
        }
    }

    fun selectRestPreset(seconds: Int) {
        val snap = currentSnapshot()
        updateConfig(snap.config.copy(restSeconds = seconds))
        startRestTimer(seconds)
    }

    fun dismissRestTimer() {
        timerJob?.cancel()
        _restTimer.update { it.copy(visible = false) }
    }

    private fun runTimerLoop(endTimeMs: Long, totalSec: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var preAlertFired = totalSec <= 5
            while (true) {
                val left = ((endTimeMs - System.currentTimeMillis() + 999) / 1000L).toInt()
                if (left > 0) {
                    if (left == 5 && !preAlertFired) {
                        preAlertFired = true
                        _timerPreAlertEvents.tryEmit(Unit)
                    }
                    _restTimer.value = RestTimerState(
                        visible = true,
                        totalSeconds = totalSec,
                        remainingSeconds = left,
                        finished = false
                    )
                    delay(250L)
                } else {
                    _restTimer.value = RestTimerState(
                        visible = true,
                        totalSeconds = totalSec,
                        remainingSeconds = 0,
                        finished = true
                    )
                    _timerFinishedEvents.tryEmit(Unit)
                    break
                }
            }
        }
    }

    // ==================== WORKOUT SESSION TIMER ====================

    fun startWorkoutSessionTimer(autoTriggered: Boolean = false) {
        val current = _workoutSessionTimer.value
        if (current.isRunning) return
        if (autoTriggered && (current.hasStarted || workoutTimerManuallyControlled)) return
        if (!autoTriggered) {
            workoutTimerManuallyControlled = true
        }
        val startedAt = System.currentTimeMillis()
        workoutTimerStartedEpochMs = startedAt
        _workoutSessionTimer.value = WorkoutSessionTimerState(
            isRunning = true,
            elapsedSeconds = workoutTimerAccumulatedMs / 1000L,
            hasStarted = true
        )
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (true) {
                val anchor = workoutTimerStartedEpochMs ?: break
                val totalMs = workoutTimerAccumulatedMs + (System.currentTimeMillis() - anchor).coerceAtLeast(0L)
                _workoutSessionTimer.value = WorkoutSessionTimerState(
                    isRunning = true,
                    elapsedSeconds = totalMs / 1000L,
                    hasStarted = true
                )
                delay(500L)
            }
        }
    }

    fun pauseWorkoutSessionTimer() {
        val anchor = workoutTimerStartedEpochMs
        if (anchor != null) {
            workoutTimerAccumulatedMs += (System.currentTimeMillis() - anchor).coerceAtLeast(0L)
            workoutTimerStartedEpochMs = null
        }
        workoutTimerManuallyControlled = true
        workoutTimerJob?.cancel()
        _workoutSessionTimer.value = WorkoutSessionTimerState(
            isRunning = false,
            elapsedSeconds = workoutTimerAccumulatedMs / 1000L,
            hasStarted = workoutTimerAccumulatedMs > 0L || _workoutSessionTimer.value.hasStarted
        )
    }

    fun toggleWorkoutSessionTimer() {
        if (_workoutSessionTimer.value.isRunning) {
            pauseWorkoutSessionTimer()
        } else {
            startWorkoutSessionTimer(autoTriggered = false)
        }
    }

    fun resetWorkoutSessionTimer() {
        workoutTimerJob?.cancel()
        workoutTimerStartedEpochMs = null
        workoutTimerAccumulatedMs = 0L
        workoutTimerManuallyControlled = true
        _workoutSessionTimer.value = WorkoutSessionTimerState(
            isRunning = false,
            elapsedSeconds = 0L,
            hasStarted = false
        )
        showToast("تم تصفير تايمر التمرينة")
    }

    // ==================== CONFIG & SETTINGS ====================

    fun updateConfig(updated: UserConfig) {
        viewModelScope.launch {
            val res = firestoreRepo.saveUserConfig(currentUserId, updated.copy(userId = currentUserId))
            if (res.isFailure) {
                showToast("تعذّر حفظ الإعدادات: ${res.exceptionOrNull()?.localizedMessage ?: ""}")
            }
        }
    }

    fun selectExerciseAlternative(dayIndex: Int, exIndex: Int, exerciseName: String) {
        val cfg = currentSnapshot().config
        val key = "$dayIndex|$exIndex"
        updateConfig(cfg.copy(alternatives = cfg.alternatives + (key to exerciseName)))
    }

    // ==================== WORKOUTS ====================

    fun updateDraftSet(dayIndex: Int, exIndex: Int, setIndex: Int, setCount: Int, baseSets: List<SetEntry>, weight: String? = null, reps: String? = null) {
        if (!weight.isNullOrBlank() || !reps.isNullOrBlank()) {
            startWorkoutSessionTimer(autoTriggered = true)
        }
        val key = "$dayIndex|$exIndex"
        _workoutDrafts.update { map ->
            val current = map[key]?.toMutableList() ?: baseSets.toMutableList()
            while (current.size < setCount) current.add(SetEntry())
            val old = current[setIndex]
            current[setIndex] = old.copy(
                weight = weight ?: old.weight,
                reps = reps ?: old.reps
            )
            map + (key to current)
        }
    }

    fun prefillWeightsFromLastSession(dayIndex: Int, exIndex: Int, setCount: Int, lastSets: List<SetEntry>, suggestedWeightKg: Double? = null) {
        val key = "$dayIndex|$exIndex"
        val suggestedStr = suggestedWeightKg?.let { w ->
            if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()
        }
        val filled = List(setCount) { idx ->
            val prevW = lastSets.getOrNull(idx)?.weight?.ifBlank { lastSets.firstOrNull()?.weight.orEmpty() }.orEmpty()
            SetEntry(weight = suggestedStr ?: prevW, reps = "")
        }
        _workoutDrafts.update { it + (key to filled) }
        showToast(if (suggestedStr != null) "تم ملء الوزن المقترح ($suggestedStr كجم) 💪" else "تم نسخ أوزان آخر جلسة ✅")
    }

    /**
     * One-tap set completion: fills missing weight/reps from previous session fallback if blank,
     * persists the exercise log immediately to Firestore, and starts the rest timer automatically.
     */
    fun quickCompleteSet(
        dayIndex: Int,
        exIndex: Int,
        setIndex: Int,
        setCount: Int,
        exerciseName: String,
        baseSets: List<SetEntry>,
        fallbackWeight: String,
        fallbackReps: String
    ) {
        startWorkoutSessionTimer(autoTriggered = true)
        val key = "$dayIndex|$exIndex"
        val updated = (_workoutDrafts.value[key] ?: baseSets).toMutableList()
        while (updated.size < setCount) updated.add(SetEntry())
        val current = updated[setIndex]
        val finalWeight = current.weight.ifBlank { fallbackWeight }
        val finalReps = current.reps.ifBlank { fallbackReps }
        updated[setIndex] = current.copy(weight = finalWeight, reps = finalReps)
        _workoutDrafts.update { it + (key to updated) }

        startRestTimer()

        val today = todayIso()
        val snap = currentSnapshot()
        val sessionId = "${today}_$dayIndex"
        val existing = snap.sessions[sessionId] ?: WorkoutSession(
            id = sessionId,
            userId = currentUserId,
            date = today,
            dayIndex = dayIndex
        )
        val updatedSession = existing.copy(
            exercises = existing.exercises + (exIndex.toString() to LoggedExercise(name = exerciseName, sets = updated))
        )
        viewModelScope.launch {
            firestoreRepo.saveWorkoutSession(currentUserId, updatedSession)
        }
    }

    fun saveExerciseLog(dayIndex: Int, exIndex: Int, exerciseName: String, sets: List<SetEntry>) {
        if (sets.none { it.weight.isNotBlank() || it.reps.isNotBlank() }) {
            showToast("اكتب الوزن أو العدات في مجموعة واحدة على الأقل.")
            return
        }
        val today = todayIso()
        val snap = currentSnapshot()
        val prevBest = exerciseHistory(snap.sessions, exerciseName)
            .filter { it.date != today }
            .maxOfOrNull { it.topE1Rm() } ?: 0.0

        val sessionId = "${today}_$dayIndex"
        val existing = snap.sessions[sessionId] ?: WorkoutSession(
            id = sessionId,
            userId = currentUserId,
            date = today,
            dayIndex = dayIndex
        )
        val newEx = LoggedExercise(name = exerciseName, sets = sets)
        val updatedSession = existing.copy(
            exercises = existing.exercises + (exIndex.toString() to newEx)
        )

        val newBest = newEx.topE1Rm()
        viewModelScope.launch {
            val res = firestoreRepo.saveWorkoutSession(currentUserId, updatedSession)
            if (res.isSuccess) {
                _workoutDrafts.update { it - "$dayIndex|$exIndex" }
                if (prevBest > 0.0 && newBest > prevBest + 0.01) {
                    showToast("رقم قياسي جديد في $exerciseName")
                } else {
                    showToast("تم حفظ التمرين ✅")
                }
            } else {
                showToast("تعذّر حفظ التمرين: ${res.exceptionOrNull()?.localizedMessage ?: ""}")
            }
        }
    }

    fun saveSessionNote(dayIndex: Int, note: String) {
        val today = todayIso()
        val snap = currentSnapshot()
        val sessionId = "${today}_$dayIndex"
        val existing = snap.sessions[sessionId] ?: WorkoutSession(
            id = sessionId,
            userId = currentUserId,
            date = today,
            dayIndex = dayIndex
        )
        viewModelScope.launch {
            firestoreRepo.saveWorkoutSession(currentUserId, existing.copy(note = note.take(2000)))
        }
    }

    fun exerciseHistory(sessions: Map<String, WorkoutSession>, exerciseName: String): List<ExerciseHistoryPoint> {
        val result = mutableListOf<ExerciseHistoryPoint>()
        sessions.values.sortedBy { it.date }.forEach { s ->
            s.exercises.values.forEach { ex ->
                if (ex.name == exerciseName) {
                    result.add(ExerciseHistoryPoint(s.date, ex.sets))
                }
            }
        }
        return result
    }

    // ==================== NUTRITION ====================

    fun computeTargets(config: UserConfig, bodyRecords: List<BodyRecord>): NutritionTargets? {
        val latest = bodyRecords.filter { (it.weightKg ?: 0.0) > 0.0 }.maxByOrNull { it.date }
        val w = if (config.weightOverride > 0.0) config.weightOverride else (latest?.weightKg ?: 0.0)
        if (w <= 0.0) return null

        val bf = if (config.bodyFatOverride > 0.0) config.bodyFatOverride else (latest?.pbfPercent ?: 0.0)
        val h = if (config.heightCm > 0.0) config.heightCm else (latest?.heightCm ?: 175.0)
        val age = config.age
        val goal = config.goal
        val ffm = latest?.ffmKg ?: if (bf > 0.0) w * (1.0 - bf / 100.0) else 0.0

        val bmr = if (bf > 0.0 && ffm > 0.0) {
            370.0 + 21.6 * ffm
        } else {
            latest?.bmrKcal ?: (10.0 * w + 6.25 * h - 5.0 * age + if (config.sex == "f") -161.0 else 5.0)
        }
        val tdee = bmr * config.activityFactor
        val goalMult = when (goal) {
            "cut" -> 0.8
            "keep" -> 1.0
            else -> 1.1
        }
        val autoKcal = ((tdee * goalMult) / 10.0).roundToInt() * 10
        val autoPr = if (ffm > 0.0) {
            (ffm * when (goal) { "cut" -> 2.8; else -> 2.5 }).roundToInt()
        } else {
            (w * when (goal) { "cut" -> 2.3; else -> 2.0 }).roundToInt()
        }
        val autoFa = (w * when (goal) { "cut" -> 0.8; "keep" -> 0.9; else -> 1.0 }).roundToInt()

        // Bug fix: calculate default carbs from effective (overridden) kcal, protein, and fat!
        val effKcal = if (config.kcalOverride > 0) config.kcalOverride else autoKcal
        val effPr = if (config.proteinOverride > 0) config.proteinOverride else autoPr
        val effFa = if (config.fatOverride > 0) config.fatOverride else autoFa
        val autoCarbs = max(0, ((effKcal - effPr * 4 - effFa * 9) / 4.0).roundToInt())
        val effCarbs = if (config.carbsOverride > 0) config.carbsOverride else autoCarbs

        val hiBf = bf > 0.0 && bf > (if (config.sex == "f") 32.0 else 25.0)
        val hint = if (hiBf && goal == "bulk") {
            "نسبة دهونك (${bf}%) أعلى من المعتاد، فالتضخيم مش الأنسب غالبًا. فكّر في ثبات أو نزول خفيف مع بروتين عالي وتمرين مقاومة. اختار الهدف من تحت."
        } else ""

        return NutritionTargets(
            weightKg = w,
            bmr = bmr.roundToInt(),
            tdee = tdee.roundToInt(),
            kcal = effKcal,
            proteinG = effPr,
            fatG = effFa,
            carbsG = effCarbs,
            hint = hint
        )
    }

    fun adjustWater(deltaMl: Int) {
        val date = _selectedNutritionDate.value
        val snap = currentSnapshot()
        val day = snap.nutritionDays[date] ?: NutritionDay(id = date, userId = currentUserId, date = date)
        val updated = day.copy(waterMl = (day.waterMl + deltaMl).coerceIn(0, 20000))
        viewModelScope.launch {
            firestoreRepo.saveNutritionDay(currentUserId, updated)
        }
    }

    fun addLoggedFood(food: FoodItem, grams: Double, mealIndex: Int) {
        if (grams <= 0.0) return
        val date = _selectedNutritionDate.value
        val snap = currentSnapshot()
        val day = snap.nutritionDays[date] ?: NutritionDay(id = date, userId = currentUserId, date = date)
        val rawEntry = LoggedFoodItem(
            id = UUID.randomUUID().toString(),
            name = food.name,
            source = food.source,
            grams = (grams * 10.0).roundToInt() / 10.0,
            mealIndex = mealIndex,
            nutrients = LoggedFoodItem.normalizeNutrients(food.nutrients)
        )
        viewModelScope.launch {
            val entry = offlineFoodRepo.enrichLoggedItem(rawEntry, snap.customFoods)
            val res = firestoreRepo.saveNutritionDay(currentUserId, day.copy(items = day.items + entry))
            if (res.isSuccess) {
                val vdMcg = entry.nutrientAmount("vD")
                if (vdMcg > NutritionConstants.VITAMIN_D_DAILY_TARGET_MCG) {
                    val carryIu = ((vdMcg - NutritionConstants.VITAMIN_D_DAILY_TARGET_MCG)
                        .coerceIn(0.0, NutritionConstants.VITAMIN_D_DAILY_TARGET_MCG) * NutritionConstants.VITAMIN_D_IU_PER_MCG).roundToInt()
                    showToast("اتضافت ${food.name} ✅ — تم تغطية 5000 IU اليوم وترحيل $carryIu IU لليوم التالي")
                } else {
                    showToast("اتضافت ${food.name} ✅")
                }
            }
        }
    }

    fun addMultipleAiFoods(items: List<AiFoodReviewItem>, mealIndex: Int) {
        val selected = items.filter { it.selected && it.grams > 0.0 }
        if (selected.isEmpty()) return
        val date = _selectedNutritionDate.value
        val snap = currentSnapshot()
        val day = snap.nutritionDays[date] ?: NutritionDay(id = date, userId = currentUserId, date = date)
        val additions = selected.map { item ->
            LoggedFoodItem(
                id = UUID.randomUUID().toString(),
                name = item.activeName(),
                source = item.activeSource(),
                grams = (item.grams * 10.0).roundToInt() / 10.0,
                mealIndex = mealIndex,
                nutrients = LoggedFoodItem.normalizeNutrients(item.activeNutrients())
            )
        }
        viewModelScope.launch {
            val res = firestoreRepo.saveNutritionDay(currentUserId, day.copy(items = day.items + additions))
            if (res.isSuccess) {
                clearFoodSheetAi()
                showToast("تمت إضافة ${additions.size} أصناف للوجبة ✅")
            }
        }
    }

    fun updateLoggedFood(itemId: String, grams: Double, mealIndex: Int) {
        if (grams <= 0.0) return
        val date = _selectedNutritionDate.value
        val snap = currentSnapshot()
        val day = snap.nutritionDays[date] ?: return
        val updatedItems = day.items.map {
            if (it.id == itemId) it.copy(grams = (grams * 10.0).roundToInt() / 10.0, mealIndex = mealIndex) else it
        }
        viewModelScope.launch {
            firestoreRepo.saveNutritionDay(currentUserId, day.copy(items = updatedItems))
        }
    }

    fun replaceLoggedFood(itemId: String, replacementFood: FoodItem, newGrams: Double, mealIndex: Int) {
        if (newGrams <= 0.0) return
        val date = _selectedNutritionDate.value
        val snap = currentSnapshot()
        val day = snap.nutritionDays[date] ?: return
        val rawReplacement = LoggedFoodItem(
            id = itemId,
            name = replacementFood.name,
            source = replacementFood.source,
            grams = (newGrams * 10.0).roundToInt() / 10.0,
            mealIndex = mealIndex,
            nutrients = LoggedFoodItem.normalizeNutrients(replacementFood.nutrients)
        )
        viewModelScope.launch {
            val enriched = offlineFoodRepo.enrichLoggedItem(rawReplacement, snap.customFoods)
            val updatedItems = day.items.map { if (it.id == itemId) enriched else it }
            val res = firestoreRepo.saveNutritionDay(currentUserId, day.copy(items = updatedItems))
            if (res.isSuccess) {
                showToast("تم التبديل إلى ${replacementFood.name} (${newGrams.roundToInt()} جم) ✅")
            }
        }
    }

    fun deleteLoggedFood(itemId: String) {
        val date = _selectedNutritionDate.value
        val snap = currentSnapshot()
        val day = snap.nutritionDays[date] ?: return
        viewModelScope.launch {
            firestoreRepo.saveNutritionDay(currentUserId, day.copy(items = day.items.filterNot { it.id == itemId }))
        }
    }

    fun copyYesterdayMeals() {
        val date = _selectedNutritionDate.value
        val yesterday = shiftIsoDate(date, -1)
        val snap = currentSnapshot()
        val yDay = snap.nutritionDays[yesterday]
        if (yDay == null || yDay.items.isEmpty()) {
            showToast("مفيش وجبات متسجلة أمس.")
            return
        }
        val cur = snap.nutritionDays[date] ?: NutritionDay(id = date, userId = currentUserId, date = date)
        val todayVdCarry = NutritionConstants.computeVitaminDBreakdown(date, snap.nutritionDays).carryInFromYesterdayMcg
        val skipHighDoseVd = todayVdCarry >= NutritionConstants.VITAMIN_D_DAILY_TARGET_MCG
        val sourceItems = if (skipHighDoseVd) {
            yDay.items.filterNot { it.nutrientAmount("vD") > NutritionConstants.VITAMIN_D_DAILY_TARGET_MCG }
        } else {
            yDay.items
        }
        val skippedVdCount = yDay.items.size - sourceItems.size
        if (sourceItems.isEmpty()) {
            showToast("أمس كان فيه جرعة فيتامين D فقط وهي مغطية النهارده تلقائياً ✅")
            return
        }
        viewModelScope.launch {
            val copied = sourceItems.map {
                offlineFoodRepo.enrichLoggedItem(it.copy(id = UUID.randomUUID().toString()), snap.customFoods)
            }
            val res = firestoreRepo.saveNutritionDay(currentUserId, cur.copy(items = cur.items + copied))
            if (res.isSuccess) {
                val msg = if (skippedVdCount > 0) {
                    "تم نسخ وجبات أمس (وتم استثناء دافاليندي لأن جرعة أمس مغطية النهارده ✅)"
                } else {
                    "تم نسخ وجبات أمس وتحديث الميكروز ✅"
                }
                showToast(msg)
            }
        }
    }

    fun enrichDayLoggedFoods(useGeminiAi: Boolean = false) {
        val date = _selectedNutritionDate.value
        val snap = currentSnapshot()
        val day = snap.nutritionDays[date]
        if (day == null || day.items.isEmpty()) {
            showToast("مفيش أكل متسجل في اليوم ده.")
            return
        }
        viewModelScope.launch {
            _enrichMicrosBusy.value = if (useGeminiAi) {
                "Gemini بيحسب كل الميكروز الناقصة (USDA + الجدول المصري)..."
            } else {
                "جاري استكمال الميكروز الناقصة من قاعدة الأغذية..."
            }
            try {
                var updatedItems = day.items
                var changedCount = 0

                if (useGeminiAi) {
                    // First enrich supplements & exact matches offline, then ask Gemini for real foods with gaps
                    val foodsToAskAi = day.items.filter { item ->
                        !SupplementCatalog.isUnitBasedLoggedItem(item) &&
                            LoggedFoodItem.normalizeNutrients(item.nutrients).any { it < 0.0 }
                    }
                    if (foodsToAskAi.isNotEmpty()) {
                        val promptText = foodsToAskAi.joinToString("\n") {
                            "- ${it.name} (${it.grams.roundToInt()} جرام)"
                        }
                        val aiRes = geminiRepo.estimateMealFromText(promptText) { stage ->
                            _enrichMicrosBusy.value = stage
                        }
                        val detected = aiRes.getOrNull().orEmpty()
                        if (detected.isNotEmpty()) {
                            updatedItems = updatedItems.map { item ->
                                if (SupplementCatalog.isUnitBasedLoggedItem(item)) {
                                    offlineFoodRepo.enrichLoggedItem(item, snap.customFoods)
                                } else {
                                    val curVec = LoggedFoodItem.normalizeNutrients(item.nutrients)
                                    if (curVec.none { it < 0.0 }) return@map item
                                    val normItem = NutritionConstants.normalizeArabic(item.name)
                                    val match = detected.firstOrNull { d ->
                                        val na = NutritionConstants.normalizeArabic(d.nameAr)
                                        val ne = d.nameEn.lowercase()
                                        (na.isNotBlank() && (normItem.contains(na) || na.contains(normItem))) ||
                                            (ne.isNotBlank() && item.name.lowercase().contains(ne))
                                    } ?: detected.getOrNull(foodsToAskAi.indexOfFirst { it.id == item.id })
                                    if (match != null) {
                                        val aiVec = NutritionConstants.vectorFromMap(match.fallbackPer100)
                                        val merged = List(NutritionConstants.NUTRIENT_COUNT) { idx ->
                                            if (curVec[idx] >= 0.0) curVec[idx] else aiVec[idx]
                                        }
                                        val enriched = item.copy(nutrients = merged)
                                        offlineFoodRepo.enrichLoggedItem(enriched, snap.customFoods)
                                    } else {
                                        offlineFoodRepo.enrichLoggedItem(item, snap.customFoods)
                                    }
                                }
                            }
                        } else {
                            updatedItems = updatedItems.map { offlineFoodRepo.enrichLoggedItem(it, snap.customFoods) }
                        }
                    } else {
                        updatedItems = updatedItems.map { offlineFoodRepo.enrichLoggedItem(it, snap.customFoods) }
                    }
                } else {
                    updatedItems = day.items.map { offlineFoodRepo.enrichLoggedItem(it, snap.customFoods) }
                }

                changedCount = updatedItems.zip(day.items).count { (a, b) -> a.nutrients != b.nutrients }
                if (changedCount > 0) {
                    val res = firestoreRepo.saveNutritionDay(currentUserId, day.copy(items = updatedItems))
                    if (res.isSuccess) {
                        showToast("تم استكمال الميكروز لـ $changedCount أصناف بنجاح ✅")
                    } else {
                        showToast("تعذّر حفظ التحديث: ${res.exceptionOrNull()?.localizedMessage ?: ""}")
                    }
                } else {
                    showToast("كل أصناف اليوم مكتملة بالفعل ✅")
                }
            } finally {
                _enrichMicrosBusy.value = null
            }
        }
    }

    fun searchFoods(query: String) {
        viewModelScope.launch {
            val customFoods = currentSnapshot().customFoods
            _searchResults.value = offlineFoodRepo.search(query, customFoods)
        }
    }

    fun searchFoodBattle(slotA: Boolean, query: String) {
        viewModelScope.launch {
            val customFoods = currentSnapshot().customFoods
            val res = offlineFoodRepo.search(query, customFoods, limit = 10)
            if (slotA) _battleResultsA.value = res else _battleResultsB.value = res
        }
    }

    fun toggleFavorite(food: FoodItem) {
        val snap = currentSnapshot()
        val existing = snap.customFoods.firstOrNull { it.name == food.name && it.source == food.source }
        viewModelScope.launch {
            if (existing != null) {
                if (existing.favorite && !existing.custom) {
                    firestoreRepo.deleteCustomFood(currentUserId, existing.id)
                } else {
                    firestoreRepo.saveCustomFood(currentUserId, existing.copy(favorite = !existing.favorite))
                }
            } else {
                val fav = CustomFood(
                    userId = currentUserId,
                    name = food.name,
                    source = food.source,
                    barcode = food.barcode,
                    refusePercent = food.refusePercent,
                    favorite = true,
                    custom = false,
                    nutrients = food.nutrients,
                    portions = food.portions
                )
                firestoreRepo.saveCustomFood(currentUserId, fav)
            }
        }
    }

    fun saveCustomPortion(food: FoodItem, portionName: String, grams: Double) {
        if (portionName.isBlank() || grams <= 0.0) return
        val cfg = currentSnapshot().config
        val key = "${food.source}|${food.name}"
        val list = cfg.customPortions[key].orEmpty() + PortionOption(portionName.trim(), grams)
        updateConfig(cfg.copy(customPortions = cfg.customPortions + (key to list)))
        showToast("اتحفظت الحصة \"$portionName\" ✅")
    }

    fun saveCustomFood(food: CustomFood, onSaved: (FoodItem) -> Unit = {}) {
        viewModelScope.launch {
            val res = firestoreRepo.saveCustomFood(currentUserId, food.copy(userId = currentUserId))
            if (res.isSuccess) {
                val savedId = res.getOrNull().orEmpty()
                showToast("اتحفظت \"${food.name}\" ✅")
                onSaved(food.copy(id = savedId).toFoodItem())
            } else {
                showToast("تعذّر حفظ الأكلة: ${res.exceptionOrNull()?.localizedMessage ?: ""}")
            }
        }
    }

    fun deleteCustomFood(foodId: String) {
        viewModelScope.launch {
            val res = firestoreRepo.deleteCustomFood(currentUserId, foodId)
            if (res.isSuccess) showToast("تم حذف الأكلة.")
        }
    }

    fun loadMicroSuggestions(lowKeys: List<String>, sex: String) {
        val rda = if (sex == "f") NutritionConstants.RDA_FEMALE else NutritionConstants.RDA_MALE
        viewModelScope.launch {
            val current = _microSuggestions.value.toMutableMap()
            var changed = false
            for (k in lowKeys) {
                if (k !in current) {
                    val target = rda[k] ?: continue
                    current[k] = offlineFoodRepo.suggestFoodsForMicro(k, target)
                    changed = true
                }
            }
            if (changed) _microSuggestions.value = current
        }
    }

    fun applyAdaptiveKcal(recommendedKcal: Int) {
        val cfg = currentSnapshot().config
        updateConfig(cfg.copy(kcalOverride = recommendedKcal.coerceIn(1000, 8000)))
        showToast("تم تطبيق الهدف الجديد: $recommendedKcal سعر ✅")
    }

    fun saveMealAsPreset(presetName: String, mealItems: List<LoggedFoodItem>) {
        if (presetName.isBlank() || mealItems.isEmpty()) return
        val totalGrams = mealItems.sumOf { it.grams }.coerceAtLeast(1.0)
        val count = NutritionConstants.NUTRIENT_COUNT
        val sum = DoubleArray(count)
        val has = BooleanArray(count)
        mealItems.forEach { item ->
            item.nutrients.forEachIndexed { idx, v ->
                if (idx < count && v >= 0.0) {
                    sum[idx] += v * item.grams / 100.0
                    has[idx] = true
                }
            }
        }
        val per100 = List(count) { idx ->
            if (has[idx]) ((sum[idx] / totalGrams) * 100.0 * 100.0).roundToInt() / 100.0 else -1.0
        }
        val preset = CustomFood(
            userId = currentUserId,
            name = presetName.trim().take(200),
            source = "c",
            favorite = true,
            custom = true,
            nutrients = per100,
            portions = listOf(PortionOption("الوجبة كاملة", (totalGrams * 10.0).roundToInt() / 10.0)),
            recipeServings = 1,
            recipeCookedWeight = (totalGrams * 10.0).roundToInt() / 10.0,
            recipeIngredients = mealItems.take(50).map { RecipeIngredient(name = it.name, grams = it.grams, methodCode = "raw") }
        )
        saveCustomFood(preset)
    }

    /**
     * Evaluates the 7-day rolling micronutrient pool using a single Firestore range query
     * (today + past 6 days) without N+1 reads, falling back to cached snapshot data if offline.
     */
    suspend fun fetchSevenDayMicronutrientEvaluation(
        referenceDate: String = _selectedNutritionDate.value
    ): MicronutrientEvaluationReport {
        val snap = currentSnapshot()
        val windowMap = firestoreRepo.fetchSevenDayNutritionWindow(currentUserId, referenceDate)
            .getOrElse { snap.nutritionDays }
        return MicronutrientEngine.evaluateMicronutrients(
            referenceDate = referenceDate,
            nutritionDays = windowMap,
            sex = snap.config.sex
        )
    }

    fun requestDeficiencyAiAdvice(lowPairs: List<Pair<String, Double>>, remainingKcal: Int?) {
        val snap = currentSnapshot()
        val refDate = _selectedNutritionDate.value
        val evalReport = MicronutrientEngine.evaluateMicronutrients(
            referenceDate = refDate,
            nutritionDays = snap.nutritionDays,
            sex = snap.config.sex
        )
        // Never flag fat-soluble vitamins as deficient if their 7-day weekly buffer is already met or near UL
        val filteredLowPairs = lowPairs.filter { (key, _) ->
            val fatEval = evalReport.fatSolubleByKey[key]
            fatEval == null || fatEval.status == MicronutrientStatus.NEEDS_TOP_UP
        }
        if (filteredLowPairs.isEmpty()) {
            showToast("مفيش عناصر ناقصة فعلياً (مخزون الفيتامينات الذائبة في الدهون مكتمل) — ممتاز!")
            return
        }
        val summary = filteredLowPairs.joinToString("\n") { (k, pct) ->
            val fatEval = evalReport.fatSolubleByKey[k]
            if (fatEval != null) {
                "- ${fatEval.labelAr} (${fatEval.labelEn}, 7-day pool): ${fatEval.weeklyPercentage.roundToInt()}% من الهدف الأسبوعي (متبقي ${fatEval.remainingWeeklyPool.roundToInt()} ${fatEval.unit}، الحالة: ${fatEval.status.wireValue})"
            } else {
                val dailyEval = evalReport.dailyByKey[k]
                if (dailyEval != null) {
                    "- ${dailyEval.labelAr}: ${pct.roundToInt()}% (متبقي اليوم ${((dailyEval.remainingToday * 10).roundToInt() / 10.0)} ${dailyEval.unit})"
                } else {
                    "- ${NutritionConstants.NUTRIENT_LABELS[k] ?: k}: ${pct.roundToInt()}% من الاحتياج اليومي"
                }
            }
        }
        val goal = snap.config.goal
        val evalJson = evalReport.toGeminiJson()
        viewModelScope.launch {
            _deficiencyAi.value = AiDeficiencyUiState(busyMessage = "Gemini بيختارلك أفضل أكلات مصرية لسد العجز...")
            val res = geminiRepo.suggestDeficiencyFoods(summary, goal, remainingKcal, evalJson) { stage ->
                _deficiencyAi.update { it.copy(busyMessage = stage) }
            }
            res.fold(
                onSuccess = { data ->
                    _deficiencyAi.value = AiDeficiencyUiState(result = data)
                },
                onFailure = { e ->
                    _deficiencyAi.value = AiDeficiencyUiState(errorMessage = e.message ?: "تعذّر الحصول على اقتراحات Gemini.")
                }
            )
        }
    }

    private fun buildFullAthleteContext(
        snap: AppDataSnapshot,
        focusDayIndex: Int = _selectedDayIndex.value
    ): String {
        val today = todayIso()
        val start7 = shiftIsoDate(today, -6)
        val weekNum = computeWeekNumber(snap.config.startDate)
        val targets = computeTargets(snap.config, snap.bodyRecords)
        val split = snap.config.effectiveSplit()
        val safeDayIdx = focusDayIndex.coerceIn(0, (split.size - 1).coerceAtLeast(0))
        val todaySplit = split.getOrNull(safeDayIdx)

        // 1) Workout summary (last 7 days) + total sets
        val sessions7 = snap.sessions.values.filter { it.date in start7..today }
        val muscleDone = mutableMapOf<String, Int>()
        val musclePlanned = mutableMapOf<String, Int>()
        var totalSets7 = 0
        sessions7.forEach { s ->
            s.exercises.values.forEach { ex ->
                val g = WorkoutDefaults.detectMuscleGroup(ex.name)
                val done = ex.sets.count { it.repsDouble() > 0.0 }
                muscleDone[g] = (muscleDone[g] ?: 0) + done
                totalSets7 += done
            }
        }
        split.forEach { day ->
            day.exercises.forEach { ex ->
                val g = WorkoutDefaults.detectMuscleGroup(ex.name)
                musclePlanned[g] = (musclePlanned[g] ?: 0) + ex.workingSets
            }
        }
        val volumeText = WorkoutDefaults.MUSCLE_GROUPS.entries
            .filter { (k, _) -> (musclePlanned[k] ?: 0) > 0 || (muscleDone[k] ?: 0) > 0 }
            .joinToString("، ") { (k, meta) -> "${meta.arabicLabel}: ${muscleDone[k] ?: 0}/${musclePlanned[k] ?: 0} مجموعات" }

        // 2) Exercise e1RM history & automatic Plateau detector across all sessions
        val allSessionsSorted = snap.sessions.values.sortedBy { it.date }
        val byExercise = linkedMapOf<String, MutableList<Pair<String, Double>>>()
        val lastBestByExercise = mutableMapOf<String, String>()
        allSessionsSorted.forEach { s ->
            s.exercises.values.forEach { ex ->
                val validSets = ex.sets.filter { it.weightDouble() > 0.0 && it.repsDouble() > 0.0 }
                if (validSets.isNotEmpty()) {
                    val bestSet = validSets.maxBy { it.estimated1Rm() }
                    val e1rm = bestSet.estimated1Rm()
                    byExercise.getOrPut(ex.name) { mutableListOf() }.add(s.date to e1rm)
                    lastBestByExercise[ex.name] = "${bestSet.weight} كجم × ${bestSet.reps} (e1RM=${e1rm.roundToInt()} كجم، ${s.date})"
                }
            }
        }
        val detectedPlateaus = byExercise.entries.mapNotNull { (exName, pts) ->
            if (pts.size < 3) return@mapNotNull null
            val last3 = pts.takeLast(3)
            val firstE = last3.first().second
            val latestE = last3.last().second
            // Flag plateau if e1RM did not improve by at least 1% across last 3 logged sessions
            if (latestE <= firstE * 1.01) {
                "$exName (آخر 3 جلسات e1RM: ${last3.joinToString(" ← ") { "${it.second.roundToInt()}" }} كجم)"
            } else null
        }.take(6)

        val todaySplitDetail = if (todaySplit != null) {
            val exLines = todaySplit.exercises.mapIndexed { idx, ex ->
                val chosenName = snap.config.alternatives["$safeDayIdx|$idx"]?.takeIf { it.isNotBlank() } ?: ex.name
                val last = lastBestByExercise[chosenName] ?: "مفيش رقم سابق مسجل"
                "- $chosenName (${ex.workingSets}×${ex.minReps}-${ex.maxReps}): آخر أفضل = $last"
            }.joinToString("\n")
            "يوم التمرين المختار (${todaySplit.name}):\n$exLines"
        } else ""

        // 3) Today's live nutrition so far (fuel, water, sodium, caffeine, supplements)
        val todayNut = snap.nutritionDays[today]
        val todayVd = NutritionConstants.computeVitaminDBreakdown(today, snap.nutritionDays)
        val todayVdNote = if (todayVd.carryInIu > 0) " (فيتامين D مُغطى اليوم بـ ${todayVd.effectiveIu}/5000 IU بترحيل من جرعة أمس)" else ""
        val todayNutText = if (todayNut != null && (todayNut.items.isNotEmpty() || todayNut.waterMl > 0)) {
            val tKcal = todayNut.items.sumOf { it.nutrientAmount("kcal") }.roundToInt()
            val tP = todayNut.items.sumOf { it.nutrientAmount("p") }.roundToInt()
            val tC = todayNut.items.sumOf { it.nutrientAmount("c") }.roundToInt()
            val tF = todayNut.items.sumOf { it.nutrientAmount("f") }.roundToInt()
            val tNa = todayNut.items.sumOf { it.nutrientAmount("na") }.roundToInt()
            val tK = todayNut.items.sumOf { it.nutrientAmount("k") }.roundToInt()
            val tCaf = todayNut.items.sumOf { it.nutrientAmount("caf") }.roundToInt()
            val remK = targets?.let { (it.kcal - tKcal) }
            val remP = targets?.let { (it.proteinG - tP) }
            val remC = targets?.let { (it.carbsG - tC) }
            val remF = targets?.let { (it.fatG - tF) }
            val itemNames = todayNut.items.takeLast(8).joinToString("، ") { "${it.name} (${it.grams.roundToInt()}جم)" }
            buildString {
                append("النهارده لحد دلوقتي: $tKcal سعر، بروتين ${tP}جم، كارب ${tC}جم، دهون ${tF}جم، ماء ${todayNut.waterMl}مل، صوديوم ${tNa}مجم، بوتاسيوم ${tK}مجم، كافيين ${tCaf}مجم.$todayVdNote")
                if (remK != null && remP != null && remC != null && remF != null) {
                    append(" المتبقي اليوم: ${remK} سعر | بروتين ${remP}جم | كارب ${remC}جم | دهون ${remF}جم.")
                }
                if (itemNames.isNotBlank()) append(" المسجل: $itemNames.")
            }
        } else {
            val remLine = targets?.let {
                " المتبقي اليوم بالكامل: ${it.kcal} سعر | بروتين ${it.proteinG}جم | كارب ${it.carbsG}جم | دهون ${it.fatG}جم."
            }.orEmpty()
            "النهارده لسه لم يتم تسجيل أكل أو ماء.$todayVdNote$remLine"
        }

        // 4) 7-day nutrition averages + Dual-Model Micronutrient Evaluation (7-day Fat-Soluble Pool + Daily Reset)
        val microEval = MicronutrientEngine.evaluateMicronutrients(
            referenceDate = today,
            nutritionDays = snap.nutritionDays,
            sex = snap.config.sex
        )
        val microEvalJson = microEval.toGeminiJson()
        val nutDays = snap.nutritionDays.values
            .filter { it.date <= today && it.items.isNotEmpty() }
            .sortedBy { it.date }
            .takeLast(7)
        val nut7Text = if (nutDays.isNotEmpty()) {
            val avgKcal = (nutDays.sumOf { d -> d.items.sumOf { it.nutrientAmount("kcal") } } / nutDays.size).roundToInt()
            val avgP = (nutDays.sumOf { d -> d.items.sumOf { it.nutrientAmount("p") } } / nutDays.size).roundToInt()
            val avgC = (nutDays.sumOf { d -> d.items.sumOf { it.nutrientAmount("c") } } / nutDays.size).roundToInt()
            val avgF = (nutDays.sumOf { d -> d.items.sumOf { it.nutrientAmount("f") } } / nutDays.size).roundToInt()
            val avgWater = (nutDays.sumOf { it.waterMl } / nutDays.size)
            val fatLows = microEval.fatSoluble
                .filter { it.status == MicronutrientStatus.NEEDS_TOP_UP }
                .map { "${it.labelAr} (${it.weeklyPercentage.roundToInt()}% أسبوعياً)" }
            val dailyLows = microEval.dailyNutrients
                .filter { !it.isLimitKey && it.hasDataToday && it.dailyPercentage < 70.0 }
                .sortedBy { it.dailyPercentage }
                .take(4)
                .map { "${it.labelAr} (${it.dailyPercentage.roundToInt()}% اليوم)" }
            val lows = (fatLows + dailyLows).take(6)
            "متوسط آخر ${nutDays.size} أيام: $avgKcal سعر (هدف ${targets?.kcal ?: "?"})، بروتين ${avgP}جم (هدف ${targets?.proteinG ?: "?"})، كارب ${avgC}جم (هدف ${targets?.carbsG ?: "?"})، دهون ${avgF}جم (هدف ${targets?.fatG ?: "?"})، ماء ${avgWater}مل. " +
                if (lows.isNotEmpty()) "أبرز نقص فعلي: ${lows.joinToString("، ")}." else "الميكروز متوازنة."
        } else {
            "لم يتم تسجيل أيام تغذية بعد."
        }

        // 5) InBody trends + Left-vs-Right Segmental Asymmetry %
        val bodySorted = snap.bodyRecords.sortedBy { it.date }
        val latest = bodySorted.lastOrNull()
        val prev = if (bodySorted.size >= 2) bodySorted[bodySorted.size - 2] else null
        val bodyText = if (latest != null) {
            buildString {
                append("آخر قياس (${latest.date}): ")
                latest.weightKg?.let { append("وزن $it كجم، ") }
                latest.smmKg?.let { append("عضل SMM $it كجم، ") }
                latest.pbfPercent?.let { append("دهون $it%، ") }
                latest.vfl?.let { append("دهون حشوية ${it.roundToInt()}، ") }
                latest.score?.let { append("درجة InBody ${it.roundToInt()}/100. ") }
                if (prev != null) {
                    val dw = if (latest.weightKg != null && prev.weightKg != null) String.format("%+.1f", latest.weightKg - prev.weightKg) else null
                    val dm = if (latest.smmKg != null && prev.smmKg != null) String.format("%+.1f", latest.smmKg - prev.smmKg) else null
                    val df = if (latest.bfmKg != null && prev.bfmKg != null) String.format("%+.1f", latest.bfmKg - prev.bfmKg) else null
                    append("التغيير عن ${prev.date}: ")
                    if (dw != null) append("وزن $dw كجم، ")
                    if (dm != null) append("عضل $dm كجم، ")
                    if (df != null) append("دهون $df كجم. ")
                }
                val ra = latest.segmentalLeanKg["ra"]
                val la = latest.segmentalLeanKg["la"]
                if (ra != null && la != null && max(ra, la) > 0.0) {
                    val diffPct = ((kotlin.math.abs(ra - la) / max(ra, la)) * 100.0)
                    append("توازن الذراعين: يمين $ra vs يسار $la كجم (فرق ${String.format("%.1f", diffPct)}%). ")
                }
                val rl = latest.segmentalLeanKg["rl"]
                val ll = latest.segmentalLeanKg["ll"]
                if (rl != null && ll != null && max(rl, ll) > 0.0) {
                    val diffPct = ((kotlin.math.abs(rl - ll) / max(rl, ll)) * 100.0)
                    append("توازن الرجلين: يمين $rl vs يسار $ll كجم (فرق ${String.format("%.1f", diffPct)}%).")
                }
            }
        } else {
            "لم يتم تسجيل قياسات InBody بعد."
        }

        // 6) Previous saved weekly coach plan for adherence review
        val lastSavedReport = snap.coachReports.firstOrNull()
        val prevPlanText = if (lastSavedReport != null) {
            "خطة الأسبوع السابق (الأسبوع ${lastSavedReport.weekNumber} - ${lastSavedReport.date} - التقييم ${lastSavedReport.report.overallScore}/100): " +
                lastSavedReport.report.nextWeekActions.joinToString(" | ")
        } else {
            "لا توجد خطة أسبوعية سابقة محفوظة."
        }

        return """
            تاريخ اليوم: $today | تاريخ أمس: ${shiftIsoDate(today, -1)}
            الهدف: ${snap.config.goal} | الأسبوع $weekNum من 12 | الجنس: ${if (snap.config.sex == "f") "أنثى" else "ذكر"} | العمر: ${snap.config.age} | الطول: ${snap.config.heightCm.roundToInt()} سم
            الأهداف اليومية الحالية: ${targets?.let { "${it.kcal} سعر | بروتين ${it.proteinG}جم | كارب ${it.carbsG}جم | دهون ${it.fatG}جم" } ?: "غير محددة"}
            حجم التمرين آخر 7 أيام (${sessions7.size} جلسات، إجمالي $totalSets7 مجموعة): $volumeText
            التمارين التي ظهر فيها ثبات (Plateau) في الـ e1RM: ${if (detectedPlateaus.isEmpty()) "لا يوجد ثبات واضح حالياً" else detectedPlateaus.joinToString("؛ ")}
            $todaySplitDetail
            تغذية اليوم: $todayNutText
            ملخص التغذية (آخر 7 أيام): $nut7Text
            micronutrient_evaluation_json: $microEvalJson
            قياسات الجسم / InBody: $bodyText
            $prevPlanText
        """.trimIndent()
    }

    fun requestWeeklyCoachReport() {
        val snap = currentSnapshot()
        val weekNum = computeWeekNumber(snap.config.startDate).coerceIn(1, 52)
        val today = todayIso()
        val contextText = buildFullAthleteContext(snap)

        viewModelScope.launch {
            _coachReport.value = AiCoachUiState(busyMessage = "المدرب الذكي بيحلل أرقام التمرين والـ Plateaus والتغذية والـ InBody...")
            val res = geminiRepo.generateWeeklyCoachReport(contextText) { stage ->
                _coachReport.update { it.copy(busyMessage = stage) }
            }
            res.fold(
                onSuccess = { report ->
                    val record = CoachReportRecord(
                        userId = currentUserId,
                        weekNumber = weekNum,
                        date = today,
                        report = report
                    )
                    val saveRes = firestoreRepo.saveCoachReport(currentUserId, record)
                    _coachReport.value = AiCoachUiState(
                        report = report,
                        savedToArchive = saveRes.isSuccess
                    )
                    if (saveRes.isSuccess) {
                        showToast("تم حفظ تقرير الأسبوع $weekNum في الأرشيف ✅")
                    }
                },
                onFailure = { e ->
                    _coachReport.value = AiCoachUiState(errorMessage = e.message ?: "تعذّر إنشاء التقرير.")
                }
            )
        }
    }

    fun deleteCoachReport(reportId: String) {
        if (reportId.isBlank()) return
        viewModelScope.launch {
            val res = firestoreRepo.deleteCoachReport(currentUserId, reportId)
            if (res.isSuccess) showToast("تم حذف التقرير من الأرشيف")
            else showToast("تعذّر حذف التقرير")
        }
    }

    fun requestPreWorkoutGamePlan(dayIndex: Int = _selectedDayIndex.value) {
        val snap = currentSnapshot()
        val split = snap.config.effectiveSplit()
        val safeIdx = dayIndex.coerceIn(0, (split.size - 1).coerceAtLeast(0))
        val dayName = split.getOrNull(safeIdx)?.name ?: "تمرين اليوم"
        val contextText = buildFullAthleteContext(snap, focusDayIndex = safeIdx)

        viewModelScope.launch {
            _preWorkoutPlan.value = AiPreWorkoutUiState(busyMessage = "جاري تجهيز خطة قبل التمرين ليوم $dayName...")
            val res = geminiRepo.generatePreWorkoutGamePlan(dayName, contextText) { stage ->
                _preWorkoutPlan.update { it.copy(busyMessage = stage) }
            }
            res.fold(
                onSuccess = { plan ->
                    _preWorkoutPlan.value = AiPreWorkoutUiState(plan = plan)
                },
                onFailure = { e ->
                    _preWorkoutPlan.value = AiPreWorkoutUiState(errorMessage = e.message ?: "تعذّر تجهيز خطة ما قبل التمرين.")
                }
            )
        }
    }

    private var lastFailedCoachRequest: Triple<List<CoachChatMessage>, String, Bitmap?>? = null

    fun sendCoachChatMessage(text: String, attachedBitmap: Bitmap? = null) {
        val cleaned = text.trim()
        if (cleaned.isBlank() && attachedBitmap == null) return
        val prevHistory = _coachChat.value.messages
        val userMsg = CoachChatMessage(
            id = UUID.randomUUID().toString(),
            fromUser = true,
            text = cleaned.ifBlank { "حلّل الصورة دي بناءً على أرقامي النهارده" },
            imageBitmap = attachedBitmap
        )
        _coachChat.update {
            it.copy(
                messages = it.messages + userMsg,
                busyMessage = "المدرب الذكي بيفكر في سؤالك...",
                errorMessage = null,
                canRetry = false
            )
        }
        executeCoachChatTurn(prevHistory = prevHistory, cleanedText = cleaned, attachedBitmap = attachedBitmap)
    }

    fun retryLastCoachChatMessage() {
        val failed = lastFailedCoachRequest ?: return
        if (_coachChat.value.busyMessage != null) return
        val (prevHistory, cleaned, bitmap) = failed
        _coachChat.update {
            it.copy(
                busyMessage = "جاري إعادة إرسال رسالتك للمدرب الذكي...",
                errorMessage = null,
                canRetry = false
            )
        }
        executeCoachChatTurn(prevHistory = prevHistory, cleanedText = cleaned, attachedBitmap = bitmap)
    }

    private fun executeCoachChatTurn(
        prevHistory: List<CoachChatMessage>,
        cleanedText: String,
        attachedBitmap: Bitmap?
    ) {
        val snap = currentSnapshot()
        val contextText = buildFullAthleteContext(snap)
        viewModelScope.launch {
            val res = geminiRepo.sendCoachChatMessage(
                athleteContext = contextText,
                history = prevHistory,
                userMessage = cleanedText,
                attachedBitmap = attachedBitmap
            ) { stage ->
                _coachChat.update { it.copy(busyMessage = stage) }
            }
            res.fold(
                onSuccess = { reply ->
                    lastFailedCoachRequest = null
                    val autoLoggedEntries = if (reply.loggedFoods.isNotEmpty()) {
                        persistCoachLoggedFoods(reply.loggedFoods)
                    } else {
                        emptyList()
                    }
                    val aiMsg = CoachChatMessage(
                        id = UUID.randomUUID().toString(),
                        fromUser = false,
                        text = reply.replyAr,
                        suggestedTargets = reply.suggestedTargets,
                        suggestedFoods = reply.suggestedFoods,
                        autoLoggedFoods = autoLoggedEntries
                    )
                    _coachChat.update {
                        it.copy(
                            messages = it.messages + aiMsg,
                            busyMessage = null,
                            errorMessage = null,
                            canRetry = false
                        )
                    }
                    if (autoLoggedEntries.isNotEmpty()) {
                        val today = todayIso()
                        val yesterday = shiftIsoDate(today, -1)
                        val totalKcal = autoLoggedEntries.sumOf { it.food.kcal }
                        val countLabel = if (autoLoggedEntries.size == 1) {
                            autoLoggedEntries.first().food.name
                        } else {
                            "${autoLoggedEntries.size} أصناف"
                        }
                        val distinctDates = autoLoggedEntries.map { it.targetDate }.distinct()
                        val dayLabel = when {
                            distinctDates.size == 1 && distinctDates.first() == today -> "ليوم النهارده"
                            distinctDates.size == 1 && distinctDates.first() == yesterday -> "ليوم أمس (${yesterday})"
                            distinctDates.size == 1 -> "ليوم ${distinctDates.first()}"
                            else -> "لسجل التغذية"
                        }
                        showToast("اتضاف $countLabel ($totalKcal سعر) $dayLabel تلقائياً ✅")
                    }
                },
                onFailure = { e ->
                    lastFailedCoachRequest = Triple(prevHistory, cleanedText, attachedBitmap)
                    _coachChat.update {
                        it.copy(
                            busyMessage = null,
                            errorMessage = e.message ?: "تعذّر الحصول على رد من المدرب الذكي.",
                            canRetry = true
                        )
                    }
                }
            )
        }
    }

    private fun buildLoggedItemFromCoachFood(
        food: CoachSuggestedFood,
        mealIndex: Int
    ): LoggedFoodItem {
        val grams = food.grams.coerceIn(5, 2500).toDouble()
        val scale = 100.0 / grams
        val nutArr = MutableList(NutritionConstants.NUTRIENT_COUNT) { -1.0 }
        nutArr[0] = (food.kcal * scale).coerceAtLeast(0.0)
        nutArr[1] = (food.proteinG * scale).coerceAtLeast(0.0)
        nutArr[2] = (food.fatG * scale).coerceAtLeast(0.0)
        nutArr[3] = (food.carbsG * scale).coerceAtLeast(0.0)
        if (food.fiberG > 0) {
            NutritionConstants.NUTRIENT_INDEX["fib"]?.let { idx ->
                nutArr[idx] = (food.fiberG * scale).coerceAtLeast(0.0)
            }
        }
        if (food.sodiumMg > 0) {
            NutritionConstants.NUTRIENT_INDEX["na"]?.let { idx ->
                nutArr[idx] = (food.sodiumMg * scale).coerceAtLeast(0.0)
            }
        }
        return LoggedFoodItem(
            id = UUID.randomUUID().toString(),
            name = food.name.trim().ifBlank { "وجبة مسجلة بالمدرب الذكي" },
            source = "a",
            grams = grams,
            mealIndex = mealIndex.coerceIn(0, 4),
            nutrients = nutArr
        )
    }

    private suspend fun persistCoachLoggedFoods(foods: List<CoachSuggestedFood>): List<CoachAutoLoggedFood> {
        val today = todayIso()
        val latestSnap = currentSnapshot()
        val groupedByDate = foods.take(12).groupBy { it.targetDate.ifBlank { today } }
        val allSaved = mutableListOf<CoachAutoLoggedFood>()
        var anyFailed = false

        for ((targetDate, dateFoods) in groupedByDate) {
            val day = latestSnap.nutritionDays[targetDate]
                ?: NutritionDay(id = targetDate, userId = currentUserId, date = targetDate)
            val pairs = dateFoods.map { f ->
                val slot = f.mealIndex.coerceIn(0, 4)
                val item = buildLoggedItemFromCoachFood(f, slot)
                item to CoachAutoLoggedFood(
                    loggedItemId = item.id,
                    food = f.copy(mealIndex = slot, targetDate = targetDate),
                    mealIndex = slot,
                    targetDate = targetDate
                )
            }
            val newItems = pairs.map { it.first }
            val res = firestoreRepo.saveNutritionDay(currentUserId, day.copy(items = day.items + newItems))
            if (res.isSuccess) {
                allSaved.addAll(pairs.map { it.second })
            } else {
                anyFailed = true
            }
        }
        if (anyFailed && allSaved.isEmpty()) {
            showToast("تعذّر حفظ الوجبة في سجل التغذية — جرّب تاني.")
        }
        return allSaved
    }

    fun undoCoachAutoLoggedFood(messageId: String, loggedItemId: String) {
        val targetDate = _coachChat.value.messages
            .firstOrNull { it.id == messageId }
            ?.autoLoggedFoods
            ?.firstOrNull { it.loggedItemId == loggedItemId }
            ?.targetDate
            ?.ifBlank { todayIso() }
            ?: todayIso()
        val snap = currentSnapshot()
        val day = snap.nutritionDays[targetDate] ?: return
        val remaining = day.items.filterNot { it.id == loggedItemId }
        viewModelScope.launch {
            val res = firestoreRepo.saveNutritionDay(currentUserId, day.copy(items = remaining))
            if (res.isSuccess) {
                _coachChat.update { st ->
                    st.copy(
                        messages = st.messages.map { m ->
                            if (m.id == messageId) {
                                m.copy(autoLoggedFoods = m.autoLoggedFoods.filterNot { it.loggedItemId == loggedItemId })
                            } else m
                        }
                    )
                }
                val dayLabel = if (targetDate == todayIso()) "يوم النهارده" else "يوم $targetDate"
                showToast("تم التراجع وحذف الصنف من $dayLabel")
            } else {
                showToast("تعذّر حذف الصنف")
            }
        }
    }

    fun moveCoachAutoLoggedFoodMeal(messageId: String, loggedItemId: String, newMealIndex: Int) {
        val safeMeal = newMealIndex.coerceIn(0, 4)
        val targetDate = _coachChat.value.messages
            .firstOrNull { it.id == messageId }
            ?.autoLoggedFoods
            ?.firstOrNull { it.loggedItemId == loggedItemId }
            ?.targetDate
            ?.ifBlank { todayIso() }
            ?: todayIso()
        val snap = currentSnapshot()
        val day = snap.nutritionDays[targetDate] ?: return
        val updatedItems = day.items.map { item ->
            if (item.id == loggedItemId) item.copy(mealIndex = safeMeal) else item
        }
        viewModelScope.launch {
            val res = firestoreRepo.saveNutritionDay(currentUserId, day.copy(items = updatedItems))
            if (res.isSuccess) {
                _coachChat.update { st ->
                    st.copy(
                        messages = st.messages.map { m ->
                            if (m.id == messageId) {
                                m.copy(
                                    autoLoggedFoods = m.autoLoggedFoods.map { entry ->
                                        if (entry.loggedItemId == loggedItemId) entry.copy(mealIndex = safeMeal) else entry
                                    }
                                )
                            } else m
                        }
                    )
                }
                val mealLabel = NutritionConstants.MEAL_NAMES.getOrElse(safeMeal) { "سناك" }
                showToast("اتنقلت لـ $mealLabel ✅")
            } else {
                showToast("تعذّر تغيير الوجبة")
            }
        }
    }

    fun clearCoachChat() {
        lastFailedCoachRequest = null
        _coachChat.value = AiCoachChatUiState()
    }

    fun applyCoachSuggestedTargets(targets: CoachSuggestedTargets) {
        if (!targets.isValid()) {
            showToast("الأهداف المقترحة غير صالحة للتطبيق.")
            return
        }
        val current = currentSnapshot().config
        val updated = current.copy(
            kcalOverride = targets.kcal,
            proteinOverride = targets.proteinG,
            carbsOverride = targets.carbsG,
            fatOverride = targets.fatG
        )
        viewModelScope.launch {
            val res = firestoreRepo.saveUserConfig(currentUserId, updated)
            if (res.isSuccess) {
                showToast("تم تطبيق الأهداف الجديدة (${targets.kcal} سعر · ${targets.proteinG}جم بروتين) ✅")
            } else {
                showToast("تعذّر حفظ الأهداف الجديدة")
            }
        }
    }

    fun addCoachSuggestedFoodToToday(food: CoachSuggestedFood, mealIndex: Int = food.mealIndex) {
        val safeMeal = mealIndex.coerceIn(0, 3)
        val today = todayIso()
        val snap = currentSnapshot()
        val day = snap.nutritionDays[today] ?: NutritionDay(id = today, userId = currentUserId, date = today)

        viewModelScope.launch {
            val entry = if (food.kcal > 0 || food.proteinG > 0 || food.carbsG > 0 || food.fatG > 0) {
                // Preserve Gemini's exact estimated calories & macros for mixed/Egyptian meals
                buildLoggedItemFromCoachFood(food, safeMeal)
            } else {
                offlineFoodRepo.ensureLoaded()
                val grams = food.grams.coerceIn(5, 2500).toDouble()
                val bestMatch = offlineFoodRepo.search(food.name, snap.customFoods, limit = 5).firstOrNull()
                if (bestMatch != null) {
                    val raw = LoggedFoodItem(
                        id = UUID.randomUUID().toString(),
                        name = bestMatch.name,
                        source = bestMatch.source,
                        grams = grams,
                        mealIndex = safeMeal,
                        nutrients = LoggedFoodItem.normalizeNutrients(bestMatch.nutrients)
                    )
                    offlineFoodRepo.enrichLoggedItem(raw, snap.customFoods)
                } else {
                    buildLoggedItemFromCoachFood(food, safeMeal)
                }
            }
            val res = firestoreRepo.saveNutritionDay(currentUserId, day.copy(items = day.items + entry))
            if (res.isSuccess) {
                val mealLabel = NutritionConstants.MEAL_NAMES.getOrElse(safeMeal) { "سناك" }
                showToast("اتضافت ${entry.name} (${entry.grams.roundToInt()}جم) في $mealLabel ✅")
            } else {
                showToast("تعذّر إضافة الأكلة ليوم النهارده")
            }
        }
    }

    fun requestMacroTetris(
        remKcal: Int,
        remProteinG: Int,
        remCarbsG: Int,
        remFatG: Int,
        selectedFridgeItems: List<String> = emptyList()
    ) {
        val snap = currentSnapshot()
        val favNames = snap.customFoods.filter { it.favorite || it.custom }.map { it.name }
        val microEvalJson = MicronutrientEngine.evaluateMicronutrients(
            referenceDate = _selectedNutritionDate.value,
            nutritionDays = snap.nutritionDays,
            sex = snap.config.sex
        ).toGeminiJson()
        viewModelScope.launch {
            _macroTetrisAi.value = AiMacroTetrisUiState(busyMessage = "Gemini بيصمم لك وجبة تقفّل باقي ماكروز النهارده من تلاجتك...")
            val res = geminiRepo.suggestMacroTetrisMeals(
                remKcal = remKcal.coerceAtLeast(0),
                remProteinG = remProteinG.coerceAtLeast(0),
                remCarbsG = remCarbsG.coerceAtLeast(0),
                remFatG = remFatG.coerceAtLeast(0),
                goal = snap.config.goal,
                favoriteNames = favNames,
                fridgeIngredients = selectedFridgeItems,
                micronutrientEvalJson = microEvalJson
            ) { stage ->
                _macroTetrisAi.update { it.copy(busyMessage = stage) }
            }
            res.fold(
                onSuccess = { data ->
                    _macroTetrisAi.value = AiMacroTetrisUiState(result = data)
                },
                onFailure = { e ->
                    _macroTetrisAi.value = AiMacroTetrisUiState(errorMessage = e.message ?: "تعذّر اقتراح الوجبة.")
                }
            )
        }
    }

    fun logReverseMealPlan(items: List<BuiltReverseMealItem>) {
        if (items.isEmpty()) return
        val targetDate = _selectedNutritionDate.value
        val snap = currentSnapshot()
        val day = snap.nutritionDays[targetDate] ?: NutritionDay(id = targetDate, userId = currentUserId, date = targetDate)
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val mealIndex = if (hour >= 17) 2 else 3 // 2 = العشاء, 3 = سناك

        viewModelScope.launch {
            offlineFoodRepo.ensureLoaded()
            val newEntries = items.map { built ->
                val raw = LoggedFoodItem(
                    id = UUID.randomUUID().toString(),
                    name = built.food.name,
                    source = built.food.source,
                    grams = built.grams,
                    mealIndex = mealIndex,
                    nutrients = LoggedFoodItem.normalizeNutrients(built.food.nutrients)
                )
                offlineFoodRepo.enrichLoggedItem(raw, snap.customFoods)
            }
            val res = firestoreRepo.saveNutritionDay(currentUserId, day.copy(items = day.items + newEntries))
            if (res.isSuccess) {
                showToast("تم تسجيل وجبة تقفيل اليوم (${newEntries.size} أصناف) ✅")
            } else {
                showToast("تعذّر تسجيل الوجبة")
            }
        }
    }

    // ==================== GEMINI AI FOOD & BARCODE FLOWS ====================

    fun clearFoodSheetAi() {
        _foodSheetAi.value = FoodSheetAiState()
    }

    fun updateBarcodeInput(value: String) {
        _foodSheetAi.update { it.copy(barcodeInput = value.filter { ch -> ch.isDigit() }) }
    }

    fun analyzeIngredientsQuality(bitmap: Bitmap) {
        viewModelScope.launch {
            _foodSheetAi.update {
                it.copy(
                    busyMessage = "مهندس جودة الأغذية بيفحص المكونات والدهون المهدرجة والسكر الخفي والمواد الحافظة...",
                    errorOrInfo = null,
                    ingredientAudit = null
                )
            }
            val result = geminiRepo.auditIngredientsLabel(bitmap) { stage ->
                _foodSheetAi.update { it.copy(busyMessage = stage) }
            }
            result.fold(
                onSuccess = { audit ->
                    _foodSheetAi.update {
                        it.copy(
                            busyMessage = null,
                            errorOrInfo = "تم فحص جودة المكونات بنجاح (Clean Score: ${audit.cleanScore}/10) ✅",
                            ingredientAudit = audit
                        )
                    }
                },
                onFailure = { e ->
                    _foodSheetAi.update {
                        it.copy(
                            busyMessage = null,
                            errorOrInfo = e.message ?: "تعذّر فحص قائمة المكونات. جرّب تصوير ظهر العبوة بإضاءة أوضح."
                        )
                    }
                }
            )
        }
    }

    fun analyzeNutritionLabel(bitmap: Bitmap) {
        viewModelScope.launch {
            _foodSheetAi.update {
                it.copy(busyMessage = "Gemini بيقرا الملصق / علبة الفيتامينات وبيملى الخانات تلقائيًا...", errorOrInfo = null, labelDraft = null)
            }
            val result = geminiRepo.parseNutritionLabel(bitmap) { stage ->
                _foodSheetAi.update { it.copy(busyMessage = stage) }
            }
            result.fold(
                onSuccess = { label ->
                    val mergedBc = label.barcode.ifBlank { _foodSheetAi.value.barcodeInput }
                    val msg = if (label.isSupplement) {
                        "تم ملء الفيتامينات والمعادن تلقائيًا بواسطة Gemini (لكل 1 ${label.servingUnit.ifBlank { "قرص" }}) ✅ راجعها قبل الحفظ."
                    } else {
                        "تم الملء التلقائي بواسطة Gemini. راجع القيم (لكل 100 جم) قبل الحفظ."
                    }
                    _foodSheetAi.update {
                        it.copy(
                            busyMessage = null,
                            errorOrInfo = msg,
                            labelDraft = label.copy(barcode = mergedBc),
                            barcodeInput = mergedBc
                        )
                    }
                },
                onFailure = { e ->
                    _foodSheetAi.update {
                        it.copy(
                            busyMessage = null,
                            errorOrInfo = e.message ?: "تعذّرت قراءة الملصق. جرّب صورة أوضح."
                        )
                    }
                }
            )
        }
    }

    fun lookupSupplementByNameWithGemini(query: String, onReady: () -> Unit = {}) {
        if (query.isBlank()) {
            _foodSheetAi.update { it.copy(errorOrInfo = "اكتب اسم المكمل أو الفيتامين الأول.") }
            return
        }
        viewModelScope.launch {
            _foodSheetAi.update {
                it.copy(busyMessage = "Gemini بيجيب تركيبة \"$query\" وبيملى الفيتامينات والمعادن تلقائيًا...", errorOrInfo = null, labelDraft = null)
            }
            val result = geminiRepo.lookupSupplementByName(query.trim()) { stage ->
                _foodSheetAi.update { it.copy(busyMessage = stage) }
            }
            result.fold(
                onSuccess = { label ->
                    _foodSheetAi.update {
                        it.copy(
                            busyMessage = null,
                            errorOrInfo = "تم الملء التلقائي لتركيبة المكمل بواسطة Gemini ✅ راجع القيم مقارنة بالعلبة واحفظه في مكملاتك.",
                            labelDraft = label.copy(isSupplement = true)
                        )
                    }
                    onReady()
                },
                onFailure = { e ->
                    _foodSheetAi.update {
                        it.copy(
                            busyMessage = null,
                            errorOrInfo = e.message ?: "تعذّر جلب تركيبة المكمل. تقدر تصوّر العلبة بالكاميرا أو تدخلها يدويًا."
                        )
                    }
                }
            )
        }
    }

    fun analyzeMealPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _foodSheetAi.update {
                it.copy(busyMessage = "Gemini بيتعرّف على الأصناف وبيقدّر الجرامات...", errorOrInfo = null, detectedItems = emptyList())
            }
            val result = geminiRepo.identifyMealPhoto(bitmap) { stage ->
                _foodSheetAi.update { it.copy(busyMessage = stage) }
            }
            handleDetectedFoodsResult(result)
        }
    }

    fun analyzeMealText(description: String) {
        if (description.isBlank()) {
            _foodSheetAi.update { it.copy(errorOrInfo = "اكتب وصف الأكلة الأول.") }
            return
        }
        viewModelScope.launch {
            _foodSheetAi.update {
                it.copy(busyMessage = "Gemini بيحلل الوصف وبيطابق الأصناف...", errorOrInfo = null, detectedItems = emptyList())
            }
            val result = geminiRepo.estimateMealFromText(description.trim()) { stage ->
                _foodSheetAi.update { it.copy(busyMessage = stage) }
            }
            handleDetectedFoodsResult(result)
        }
    }

    private suspend fun handleDetectedFoodsResult(result: Result<List<com.mutabei.workout.data.model.AiDetectedFoodRaw>>) {
        result.fold(
            onSuccess = { rawItems ->
                val customFoods = currentSnapshot().customFoods
                val reviewItems = rawItems.mapIndexed { idx, raw ->
                    val cands = offlineFoodRepo.candidatesFor(raw, customFoods)
                    val vec = NutritionConstants.vectorFromMap(raw.fallbackPer100)
                    val populatedCount = vec.count { it >= 0.0 }
                    val detectedLabel = raw.nameAr.ifBlank { raw.nameEn }
                    val inferredMethod = CookingMethodEngine.inferBaselineMethod(raw.nameAr, raw.nameEn)
                    AiFoodReviewItem(
                        id = "ai_${idx}_${System.currentTimeMillis()}",
                        detectedName = detectedLabel,
                        grams = raw.grams,
                        selected = true,
                        fallbackVector = vec,
                        candidates = cands,
                        // Prefer Gemini's full micronutrient analysis when populated; user can still switch to DB candidates
                        selectedCandidateIndex = if (populatedCount >= 10 || cands.isEmpty()) -1 else 0,
                        baselineCookingMethod = inferredMethod,
                        selectedCookingMethod = inferredMethod
                    )
                }
                _foodSheetAi.update {
                    it.copy(
                        busyMessage = null,
                        errorOrInfo = if (reviewItems.isEmpty()) "مقدرتش أحدد أكلات. جرّب صورة أو وصف أوضح." else null,
                        detectedItems = reviewItems
                    )
                }
            },
            onFailure = { e ->
                _foodSheetAi.update {
                    it.copy(busyMessage = null, errorOrInfo = e.message ?: "حصلت مشكلة مع Gemini. جرّب تاني.")
                }
            }
        )
    }

    fun updateAiDetectedItem(itemId: String, selected: Boolean? = null, grams: Double? = null, candidateIndex: Int? = null) {
        _foodSheetAi.update { state ->
            state.copy(
                detectedItems = state.detectedItems.map { item ->
                    if (item.id == itemId) {
                        item.copy(
                            selected = selected ?: item.selected,
                            grams = grams ?: item.grams,
                            selectedCandidateIndex = candidateIndex ?: item.selectedCandidateIndex
                        )
                    } else item
                }
            )
        }
    }

    fun updateAiCookingMethod(itemId: String, method: CookingMethodPreset) {
        _foodSheetAi.update { state ->
            state.copy(
                detectedItems = state.detectedItems.map { item ->
                    if (item.id == itemId) item.copy(selectedCookingMethod = method) else item
                }
            )
        }
    }

    fun scanBarcodeWithGemini(bitmap: Bitmap, onFoundCustomFood: (FoodItem) -> Unit, onSwitchToLabelMode: () -> Unit) {
        viewModelScope.launch {
            _foodSheetAi.update {
                it.copy(busyMessage = "Gemini بيقرا رقم الباركود من الصورة...", errorOrInfo = null)
            }
            val res = geminiRepo.readBarcodeFromImage(bitmap) { stage ->
                _foodSheetAi.update { it.copy(busyMessage = stage) }
            }
            res.fold(
                onSuccess = { digits ->
                    if (digits.isNullOrBlank()) {
                        _foodSheetAi.update {
                            it.copy(busyMessage = null, errorOrInfo = "مقدرتش أقرا الباركود من الصورة. قرّب الكاميرا أو اكتب الرقم يدويًا.")
                        }
                    } else {
                        _foodSheetAi.update { it.copy(barcodeInput = digits) }
                        lookupBarcode(digits, onFoundCustomFood, onSwitchToLabelMode)
                    }
                },
                onFailure = { e ->
                    _foodSheetAi.update {
                        it.copy(busyMessage = null, errorOrInfo = e.message ?: "فشل قراءة الباركود.")
                    }
                }
            )
        }
    }

    fun lookupBarcode(
        rawBarcode: String,
        onFoundCustomFood: (FoodItem) -> Unit,
        onSwitchToLabelMode: () -> Unit
    ) {
        val clean = rawBarcode.filter { it.isDigit() }
        if (clean.isBlank()) {
            _foodSheetAi.update { it.copy(errorOrInfo = "اكتب رقم الباركود الأول.") }
            return
        }
        val existing = currentSnapshot().customFoods.firstOrNull { it.barcode == clean }
        if (existing != null) {
            _foodSheetAi.update { it.copy(busyMessage = null, errorOrInfo = null) }
            onFoundCustomFood(existing.toFoodItem())
            return
        }
        viewModelScope.launch {
            _foodSheetAi.update { it.copy(busyMessage = "بدوّر على الباركود $clean في Open Food Facts...", errorOrInfo = null, barcodeInput = clean) }
            val product = offRepo.lookupBarcode(clean)
            if (product != null) {
                _foodSheetAi.update {
                    it.copy(
                        busyMessage = null,
                        errorOrInfo = "لقيته في Open Food Facts وتم الملء التلقائي! راجع القيم (لكل 100 جم) قبل الحفظ.",
                        labelDraft = product,
                        barcodeInput = clean
                    )
                }
                onSwitchToLabelMode()
            } else {
                _foodSheetAi.update {
                    it.copy(
                        busyMessage = null,
                        errorOrInfo = "الباركود $clean مش متسجل. صوّر الجدول الغذائي على العبوة بـ Gemini وهيتحفظ بالباركود ده.",
                        labelDraft = AiLabelResult("", clean, null, emptyMap()),
                        barcodeInput = clean
                    )
                }
                onSwitchToLabelMode()
            }
        }
    }

    // ==================== BODY / INBODY & GEMINI AUTO-FILL ====================

    fun updateBodyDraftDate(date: String) {
        _bodyDraft.update { it.copy(date = date) }
    }

    fun updateBodyDraftField(key: String, value: String) {
        _bodyDraft.update { it.copy(fields = it.fields + (key to value)) }
    }

    fun updateBodyDraftSegment(key: String, value: String) {
        _bodyDraft.update { it.copy(segmentalLeanKg = it.segmentalLeanKg + (key to value)) }
    }

    fun scanInBodySheetWithGemini(bitmap: Bitmap) {
        viewModelScope.launch {
            _bodyDraft.update { it.copy(aiBusy = true, aiStatusMessage = "Gemini بيقرا ورقة الـ InBody وبيملى الخانات تلقائيًا...") }
            val res = geminiRepo.parseInBodySheet(bitmap) { stage ->
                _bodyDraft.update { it.copy(aiStatusMessage = stage) }
            }
            res.fold(
                onSuccess = { rec ->
                    val f = mutableMapOf<String, String>()
                    fun put(k: String, v: Double?) {
                        if (v != null) f[k] = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
                    }
                    put("w", rec.weightKg)
                    put("m", rec.smmKg)
                    put("f", rec.pbfPercent)
                    put("bfm", rec.bfmKg)
                    put("ffm", rec.ffmKg)
                    put("tbw", rec.tbwL)
                    put("pro", rec.proteinKg)
                    put("min", rec.mineralsKg)
                    put("bmr", rec.bmrKcal)
                    put("vfl", rec.vfl)
                    put("whr", rec.whr)
                    put("score", rec.score)
                    put("tgt", rec.targetWeightKg)
                    put("fc", rec.fatControlKg)
                    put("mc", rec.muscleControlKg)
                    put("bmi", rec.bmi)
                    put("obd", rec.obesityDegree)
                    put("h", rec.heightCm)

                    val sl = rec.segmentalLeanKg.mapValues { (_, v) -> v.toString() }
                    val totalFilled = f.size + sl.size
                    val statusMsg = when {
                        totalFilled == 0 ->
                            "لم يتمكن Gemini من قراءة أرقام واضحة من الصورة. صوّر الورقة بشكل عمودي وواضح في إضاءة جيدة وجرّب تاني."
                        totalFilled < 5 ->
                            "قرأ Gemini $totalFilled خانات فقط — يبدو أن جزءًا من الورقة غير واضح أو مقطوع. راجع الأرقام أو جرّب صورة أوضح."
                        else ->
                            "تم الملء التلقائي لـ $totalFilled خانة بواسطة Gemini ✅ راجع الأرقام قبل الحفظ."
                    }
                    _bodyDraft.value = BodyDraftState(
                        date = rec.date.ifBlank { todayIso() },
                        fields = f,
                        segmentalLeanKg = sl,
                        segmentalLeanPercent = rec.segmentalLeanPercent,
                        segmentalFatKg = rec.segmentalFatKg,
                        ranges = rec.ranges,
                        aiBusy = false,
                        aiStatusMessage = statusMsg
                    )
                },
                onFailure = { e ->
                    _bodyDraft.update {
                        it.copy(
                            aiBusy = false,
                            aiStatusMessage = e.message ?: "حصلت مشكلة في قراءة ورقة الـ InBody. جرّب صورة أوضح."
                        )
                    }
                }
            )
        }
    }

    fun saveBodyDraft() {
        val record = _bodyDraft.value.toBodyRecord()
        if (record.weightKg == null && record.pbfPercent == null && record.smmKg == null) {
            showToast("اكتب الوزن على الأقل.")
            return
        }
        viewModelScope.launch {
            val res = firestoreRepo.saveBodyRecord(currentUserId, record)
            if (res.isSuccess) {
                _bodyDraft.value = BodyDraftState()
                showToast("تم حفظ القياس ✅")
            } else {
                showToast("تعذّر حفظ القياس: ${res.exceptionOrNull()?.localizedMessage ?: ""}")
            }
        }
    }

    fun deleteBodyRecord(date: String) {
        viewModelScope.launch {
            val res = firestoreRepo.deleteBodyRecord(currentUserId, date)
            if (res.isSuccess) showToast("تم حذف القياس.")
        }
    }

    // ==================== BACKUP, RESTORE & CUSTOM SPLIT ====================

    fun exportBackupJson(): String {
        val snap = currentSnapshot()
        val root = JSONObject()
        val cfg = snap.config
        root.put("start", cfg.startDate)
        root.put("rest", cfg.restSeconds)
        root.put("theme", cfg.themeMode)
        root.put("day", _selectedDayIndex.value)
        root.put("nd", _selectedNutritionDate.value)
        root.put("plan", JSONArray().apply {
            cfg.effectiveSplit().forEach { d ->
                put(JSONObject().apply {
                    put("n", d.name)
                    put("x", JSONArray().apply {
                        d.exercises.forEach { e ->
                            put(JSONArray().apply {
                                put(e.name); put(e.warmupSets); put(e.workingSets)
                                put(e.minReps); put(e.maxReps); put(e.alt1); put(e.alt2); put(e.note)
                            })
                        }
                    })
                })
            }
        })
        root.put("alt", JSONObject(cfg.alternatives))
        root.put("prof", JSONObject().apply {
            put("sex", cfg.sex); put("age", cfg.age); put("h", cfg.heightCm)
            put("w", cfg.weightOverride); put("bf", cfg.bodyFatOverride)
            put("act", cfg.activityFactor); put("goal", cfg.goal); put("tf", cfg.targetBodyFat)
            put("ok", cfg.kcalOverride); put("op", cfg.proteinOverride)
            put("oc", cfg.carbsOverride); put("of", cfg.fatOverride)
        })
        root.put("sess", JSONObject().apply {
            snap.sessions.forEach { (k, s) ->
                put(k, JSONObject().apply {
                    put("d", s.date); put("day", s.dayIndex); put("note", s.note)
                    put("ex", JSONObject().apply {
                        s.exercises.forEach { (ei, ex) ->
                            put(ei, JSONObject().apply {
                                put("n", ex.name)
                                put("sets", JSONArray().apply {
                                    ex.sets.forEach { st ->
                                        put(JSONObject().apply { put("w", st.weight); put("r", st.reps) })
                                    }
                                })
                            })
                        }
                    })
                })
            }
        })
        root.put("body", JSONArray().apply {
            snap.bodyRecords.forEach { b ->
                put(JSONObject().apply {
                    put("d", b.date)
                    b.weightKg?.let { put("w", it) }; b.smmKg?.let { put("m", it) }
                    b.pbfPercent?.let { put("f", it) }; b.bfmKg?.let { put("bfm", it) }
                    b.ffmKg?.let { put("ffm", it) }; b.tbwL?.let { put("tbw", it) }
                    b.proteinKg?.let { put("pro", it) }; b.mineralsKg?.let { put("min", it) }
                    b.bmrKcal?.let { put("bmr", it) }; b.vfl?.let { put("vfl", it) }
                    b.whr?.let { put("whr", it) }; b.score?.let { put("score", it) }
                    b.targetWeightKg?.let { put("tgt", it) }; b.fatControlKg?.let { put("fc", it) }
                    b.muscleControlKg?.let { put("mc", it) }; b.bmi?.let { put("bmi", it) }
                    b.obesityDegree?.let { put("obd", it) }; b.heightCm?.let { put("h", it) }
                    if (b.segmentalLeanKg.isNotEmpty()) put("sl_kg", JSONObject(b.segmentalLeanKg))
                    if (b.segmentalLeanPercent.isNotEmpty()) put("sl_pct", JSONObject(b.segmentalLeanPercent))
                    if (b.segmentalFatKg.isNotEmpty()) put("sf_kg", JSONObject(b.segmentalFatKg))
                })
            }
        })
        root.put("nut", JSONObject().apply {
            snap.nutritionDays.forEach { (dateKey, day) ->
                if (day.items.isNotEmpty() || day.waterMl > 0) {
                    put(dateKey, JSONObject().apply {
                        put("w", day.waterMl)
                        put("items", JSONArray().apply {
                            day.items.forEach { it ->
                                put(JSONObject().apply {
                                    put("n", it.name)
                                    put("src", it.source)
                                    put("g", it.grams)
                                    put("m", it.mealIndex)
                                    put("p", JSONArray(LoggedFoodItem.normalizeNutrients(it.nutrients)))
                                })
                            }
                        })
                    })
                }
            }
        })
        root.put("fav", JSONArray().apply {
            snap.customFoods.filter { it.favorite && !it.custom }.forEach { f ->
                put(JSONObject().apply {
                    put("n", f.name)
                    put("src", f.source)
                    put("ref", f.refusePercent)
                    put("p", JSONArray(LoggedFoodItem.normalizeNutrients(f.nutrients)))
                    put("por", JSONArray().apply {
                        f.portions.forEach { po -> put(JSONObject().apply { put("n", po.name); put("g", po.grams) }) }
                    })
                })
            }
        })
        root.put("cf", JSONArray().apply {
            snap.customFoods.filter { it.custom }.forEach { f ->
                put(JSONObject().apply {
                    put("n", f.name)
                    put("src", f.source)
                    put("bc", f.barcode)
                    put("ref", f.refusePercent)
                    put("fav", f.favorite)
                    put("p", JSONArray(LoggedFoodItem.normalizeNutrients(f.nutrients)))
                    put("por", JSONArray().apply {
                        f.portions.forEach { po -> put(JSONObject().apply { put("n", po.name); put("g", po.grams) }) }
                    })
                })
            }
        })
        return root.toString(2)
    }

    private fun optDoubleFlex(obj: JSONObject, key: String): Double? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val raw = obj.opt(key) ?: return null
        return when (raw) {
            is Number -> raw.toDouble().takeIf { !it.isNaN() }
            is String -> raw.replace(',', '.').trim().toDoubleOrNull()?.takeIf { !it.isNaN() }
            else -> null
        }
    }

    private fun optIntFlex(obj: JSONObject, key: String): Int? =
        optDoubleFlex(obj, key)?.roundToInt()

    private fun parseNutrientArray(arr: JSONArray?): List<Double> {
        if (arr == null) return NutritionConstants.emptyNutrientVector()
        return List(NutritionConstants.NUTRIENT_COUNT) { i ->
            if (i < arr.length() && !arr.isNull(i)) {
                val raw = arr.opt(i)
                when (raw) {
                    is Number -> raw.toDouble().takeIf { !it.isNaN() } ?: -1.0
                    is String -> raw.replace(',', '.').trim().toDoubleOrNull() ?: -1.0
                    else -> -1.0
                }
            } else -1.0
        }
    }

    private fun parsePortionsArray(arr: JSONArray?): List<PortionOption> {
        if (arr == null) return emptyList()
        val list = mutableListOf<PortionOption>()
        for (i in 0 until arr.length()) {
            val po = arr.optJSONObject(i) ?: continue
            val name = po.optString("n", po.optString("name", "")).trim()
            val grams = optDoubleFlex(po, "g") ?: optDoubleFlex(po, "grams") ?: continue
            if (name.isNotBlank() && grams > 0.0) {
                list.add(PortionOption(name, grams))
            }
        }
        return list
    }

    private fun parseDoubleMapJson(obj: JSONObject?): Map<String, Double> {
        if (obj == null) return emptyMap()
        val map = mutableMapOf<String, Double>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            optDoubleFlex(obj, k)?.let { map[k] = it }
        }
        return map
    }

    fun importBackupJson(jsonText: String): Boolean {
        val cleaned = jsonText.trim()
        val root = try {
            JSONObject(cleaned)
        } catch (e: Exception) {
            Log.e(TAG, "Backup JSON parse failed", e)
            showToast("النص مش بصيغة JSON صحيحة. تأكد إنك نسخت النسخة الاحتياطية كاملة.")
            return false
        }

        val snap = currentSnapshot()
        var cfg = snap.config.copy(userId = currentUserId)

        if (root.has("start")) {
            val st = root.optString("start", "").trim()
            if (st.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) cfg = cfg.copy(startDate = st)
        }
        optIntFlex(root, "rest")?.let { cfg = cfg.copy(restSeconds = it.coerceIn(30, 900)) }
        if (root.has("theme")) {
            val th = root.optString("theme", "")
            if (th in listOf("system", "light", "dark")) cfg = cfg.copy(themeMode = th)
        }
        if (root.has("plan")) {
            val parsedPlan = parseSplitJsonArray(root.optJSONArray("plan") ?: JSONArray())
            if (parsedPlan.isNotEmpty()) cfg = cfg.copy(splitDays = parsedPlan)
        }

        // Restore exercise alternatives (crucial so sessions map to the exact chosen alternative!)
        val altObj = root.optJSONObject("alt")
        val restoredAlts = mutableMapOf<String, String>()
        if (altObj != null) {
            val altKeys = altObj.keys()
            while (altKeys.hasNext()) {
                val ak = altKeys.next()
                val av = altObj.optString(ak, "").trim()
                if (ak.isNotBlank() && av.isNotBlank()) {
                    restoredAlts[ak] = av
                }
            }
            cfg = cfg.copy(alternatives = restoredAlts)
        }

        // Restore full profile & manual nutrition target overrides (supports both strings and numbers)
        val p = root.optJSONObject("prof")
        if (p != null) {
            val sex = p.optString("sex", cfg.sex).let { if (it == "f") "f" else "m" }
            val goal = p.optString("goal", cfg.goal).let { if (it in listOf("bulk", "cut", "keep")) it else cfg.goal }
            cfg = cfg.copy(
                sex = sex,
                age = optIntFlex(p, "age")?.coerceIn(10, 120) ?: cfg.age,
                heightCm = optDoubleFlex(p, "h")?.coerceIn(100.0, 250.0) ?: cfg.heightCm,
                weightOverride = optDoubleFlex(p, "w")?.coerceIn(0.0, 350.0) ?: cfg.weightOverride,
                bodyFatOverride = optDoubleFlex(p, "bf")?.coerceIn(0.0, 70.0) ?: cfg.bodyFatOverride,
                activityFactor = optDoubleFlex(p, "act")?.coerceIn(1.1, 2.5) ?: cfg.activityFactor,
                goal = goal,
                targetBodyFat = optDoubleFlex(p, "tf")?.coerceIn(3.0, 50.0) ?: cfg.targetBodyFat,
                kcalOverride = optIntFlex(p, "ok")?.coerceIn(0, 10000) ?: cfg.kcalOverride,
                proteinOverride = optIntFlex(p, "op")?.coerceIn(0, 1000) ?: cfg.proteinOverride,
                carbsOverride = optIntFlex(p, "oc")?.coerceIn(0, 2000) ?: cfg.carbsOverride,
                fatOverride = optIntFlex(p, "of")?.coerceIn(0, 500) ?: cfg.fatOverride
            )
        }

        // Restore active split day & selected nutrition date
        val splitSize = cfg.effectiveSplit().size.coerceAtLeast(1)
        optIntFlex(root, "day")?.let { _selectedDayIndex.value = it.coerceIn(0, splitSize - 1) }
        val nd = root.optString("nd", "").trim()
        if (nd.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            _selectedNutritionDate.value = nd
        }
        _workoutDrafts.value = emptyMap()

        // Parse workout sessions
        val sessionsToSave = mutableListOf<WorkoutSession>()
        val sessObj = root.optJSONObject("sess")
        if (sessObj != null) {
            val keys = sessObj.keys()
            while (keys.hasNext()) {
                val sk = keys.next()
                val so = sessObj.optJSONObject(sk) ?: continue
                val date = so.optString("d", so.optString("date", "")).trim().ifBlank {
                    sk.substringBefore("_").ifBlank { todayIso() }
                }
                val dayIdx = optIntFlex(so, "day")
                    ?: optIntFlex(so, "dayIndex")
                    ?: sk.substringAfter("_", "0").toIntOrNull()
                    ?: 0
                val exObj = so.optJSONObject("ex") ?: so.optJSONObject("exercises") ?: JSONObject()
                val exMap = mutableMapOf<String, LoggedExercise>()
                val exKeys = exObj.keys()
                while (exKeys.hasNext()) {
                    val ek = exKeys.next()
                    val eo = exObj.optJSONObject(ek) ?: continue
                    val exName = eo.optString("n", eo.optString("name", "")).trim()
                    val setsArr = eo.optJSONArray("sets") ?: JSONArray()
                    val sets = mutableListOf<SetEntry>()
                    for (si in 0 until setsArr.length()) {
                        val st = setsArr.optJSONObject(si) ?: continue
                        val wVal = if (st.isNull("w")) st.optString("weight", "") else st.opt("w")?.toString().orEmpty()
                        val rVal = if (st.isNull("r")) st.optString("reps", "") else st.opt("r")?.toString().orEmpty()
                        sets.add(SetEntry(wVal.trim(), rVal.trim()))
                    }
                    if (exName.isNotBlank() && sets.isNotEmpty()) {
                        exMap[ek] = LoggedExercise(exName, sets)
                    }
                }
                if (exMap.isNotEmpty() || so.optString("note", "").isNotBlank()) {
                    sessionsToSave.add(
                        WorkoutSession(
                            id = sk,
                            userId = currentUserId,
                            date = date,
                            dayIndex = dayIdx.coerceIn(0, 13),
                            note = so.optString("note", ""),
                            exercises = exMap
                        )
                    )
                }
            }
        }

        // Parse body records
        val bodyToSave = mutableListOf<BodyRecord>()
        val bodyArr = root.optJSONArray("body")
        if (bodyArr != null) {
            for (i in 0 until bodyArr.length()) {
                val bo = bodyArr.optJSONObject(i) ?: continue
                val date = bo.optString("d", bo.optString("date", "")).trim()
                if (date.isBlank()) continue
                bodyToSave.add(
                    BodyRecord(
                        userId = currentUserId,
                        date = date,
                        weightKg = optDoubleFlex(bo, "w"),
                        smmKg = optDoubleFlex(bo, "m") ?: optDoubleFlex(bo, "smm"),
                        pbfPercent = optDoubleFlex(bo, "f") ?: optDoubleFlex(bo, "pbf"),
                        bfmKg = optDoubleFlex(bo, "bfm"),
                        ffmKg = optDoubleFlex(bo, "ffm"),
                        tbwL = optDoubleFlex(bo, "tbw"),
                        proteinKg = optDoubleFlex(bo, "pro"),
                        mineralsKg = optDoubleFlex(bo, "min"),
                        bmrKcal = optDoubleFlex(bo, "bmr"),
                        vfl = optDoubleFlex(bo, "vfl"),
                        whr = optDoubleFlex(bo, "whr"),
                        score = optDoubleFlex(bo, "score"),
                        targetWeightKg = optDoubleFlex(bo, "tgt"),
                        fatControlKg = optDoubleFlex(bo, "fc"),
                        muscleControlKg = optDoubleFlex(bo, "mc"),
                        bmi = optDoubleFlex(bo, "bmi"),
                        obesityDegree = optDoubleFlex(bo, "obd"),
                        heightCm = optDoubleFlex(bo, "h"),
                        segmentalLeanKg = parseDoubleMapJson(bo.optJSONObject("sl_kg") ?: bo.optJSONObject("sl")),
                        segmentalLeanPercent = parseDoubleMapJson(bo.optJSONObject("sl_pct")),
                        segmentalFatKg = parseDoubleMapJson(bo.optJSONObject("sf_kg") ?: bo.optJSONObject("sf"))
                    )
                )
            }
        }

        // Parse nutrition days (skip completely empty days)
        val nutritionToSave = mutableListOf<NutritionDay>()
        val nutObj = root.optJSONObject("nut")
        if (nutObj != null) {
            val nutKeys = nutObj.keys()
            while (nutKeys.hasNext()) {
                val dateKey = nutKeys.next().trim()
                if (!dateKey.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) continue
                val dayObj = nutObj.optJSONObject(dateKey) ?: continue
                val waterMl = (optIntFlex(dayObj, "w") ?: optIntFlex(dayObj, "waterMl") ?: 0).coerceIn(0, 20000)
                val itemsArr = dayObj.optJSONArray("items") ?: JSONArray()
                val items = mutableListOf<LoggedFoodItem>()
                for (i in 0 until itemsArr.length()) {
                    val io = itemsArr.optJSONObject(i) ?: continue
                    val name = io.optString("n", io.optString("name", "")).trim()
                    if (name.isBlank()) continue
                    val src = io.optString("src", io.optString("source", "e")).ifBlank { "e" }
                    val grams = optDoubleFlex(io, "g") ?: optDoubleFlex(io, "grams") ?: 100.0
                    val mealIdx = (optIntFlex(io, "m") ?: optIntFlex(io, "mealIndex") ?: 0).coerceIn(0, 4)
                    val nutVec = parseNutrientArray(io.optJSONArray("p") ?: io.optJSONArray("nutrients"))
                    items.add(
                        LoggedFoodItem(
                            id = io.optString("id", "").ifBlank { UUID.randomUUID().toString() },
                            name = name,
                            source = src,
                            grams = grams,
                            mealIndex = mealIdx,
                            nutrients = nutVec
                        )
                    )
                }
                if (items.isNotEmpty() || waterMl > 0) {
                    nutritionToSave.add(
                        NutritionDay(
                            id = dateKey,
                            userId = currentUserId,
                            date = dateKey,
                            waterMl = waterMl,
                            items = items
                        )
                    )
                }
            }
        }

        // Parse favorites & custom foods (deduplicated against existing foods)
        val foodsToSave = mutableListOf<CustomFood>()
        val existingByKey = snap.customFoods.associateBy { "${it.name}|${it.source}|${it.custom}" }
        val seenFoodKeys = mutableSetOf<String>()

        val favArr = root.optJSONArray("fav")
        if (favArr != null) {
            for (i in 0 until favArr.length()) {
                val fo = favArr.optJSONObject(i) ?: continue
                val name = fo.optString("n", fo.optString("name", "")).trim()
                if (name.isBlank()) continue
                val src = fo.optString("src", fo.optString("source", "e")).ifBlank { "e" }
                val dedupKey = "$name|$src|false"
                if (!seenFoodKeys.add(dedupKey)) continue
                val existingId = existingByKey[dedupKey]?.id.orEmpty()
                foodsToSave.add(
                    CustomFood(
                        id = existingId,
                        userId = currentUserId,
                        name = name,
                        source = src,
                        barcode = fo.optString("bc", fo.optString("barcode", "")),
                        refusePercent = optDoubleFlex(fo, "ref") ?: 0.0,
                        favorite = true,
                        custom = false,
                        nutrients = parseNutrientArray(fo.optJSONArray("p") ?: fo.optJSONArray("nutrients")),
                        portions = parsePortionsArray(fo.optJSONArray("por") ?: fo.optJSONArray("portions"))
                    )
                )
            }
        }

        val cfArr = root.optJSONArray("cf")
        if (cfArr != null) {
            for (i in 0 until cfArr.length()) {
                val co = cfArr.optJSONObject(i) ?: continue
                val name = co.optString("n", co.optString("name", "")).trim()
                if (name.isBlank()) continue
                val src = co.optString("src", co.optString("source", "c")).ifBlank { "c" }
                val dedupKey = "$name|$src|true"
                if (!seenFoodKeys.add(dedupKey)) continue
                val existingId = existingByKey[dedupKey]?.id.orEmpty()
                foodsToSave.add(
                    CustomFood(
                        id = existingId,
                        userId = currentUserId,
                        name = name,
                        source = src,
                        barcode = co.optString("bc", co.optString("barcode", "")),
                        refusePercent = optDoubleFlex(co, "ref") ?: 0.0,
                        favorite = co.optBoolean("fav", false),
                        custom = true,
                        nutrients = parseNutrientArray(co.optJSONArray("p") ?: co.optJSONArray("nutrients")),
                        portions = parsePortionsArray(co.optJSONArray("por") ?: co.optJSONArray("portions"))
                    )
                )
            }
        }

        viewModelScope.launch {
            var failures = 0
            if (firestoreRepo.saveUserConfig(currentUserId, cfg).isFailure) failures++
            var savedSessions = 0
            for (s in sessionsToSave) {
                if (firestoreRepo.saveWorkoutSession(currentUserId, s).isSuccess) savedSessions++ else failures++
            }
            var savedBody = 0
            for (b in bodyToSave) {
                if (firestoreRepo.saveBodyRecord(currentUserId, b).isSuccess) savedBody++ else failures++
            }
            var savedNut = 0
            for (n in nutritionToSave) {
                if (firestoreRepo.saveNutritionDay(currentUserId, n).isSuccess) savedNut++ else failures++
            }
            var savedFoods = 0
            for (f in foodsToSave) {
                if (firestoreRepo.saveCustomFood(currentUserId, f).isSuccess) savedFoods++ else failures++
            }

            val parts = buildList {
                if (savedSessions > 0) add("$savedSessions جلسات تمرين")
                if (restoredAlts.isNotEmpty()) add("${restoredAlts.size} بدائل")
                if (savedNut > 0) add("$savedNut أيام تغذية")
                if (savedBody > 0) add("$savedBody قياس جسم")
                if (savedFoods > 0) add("$savedFoods أكلات ومفضلة")
            }.joinToString(" · ")

            if (failures == 0) {
                showToast("تم الاسترجاع بالكامل ✅ ${if (parts.isNotBlank()) "($parts)" else ""}")
            } else {
                showToast("تم استرجاع جزء من البيانات ($parts) وفشل حفظ $failures عنصر.")
            }
        }
        return true
    }

    fun importCustomSplitJson(jsonText: String): Boolean {
        return try {
            val arr = JSONArray(jsonText)
            val days = parseSplitJsonArray(arr)
            if (days.isEmpty()) throw IllegalArgumentException("Empty split")
            val cfg = currentSnapshot().config
            updateConfig(cfg.copy(splitDays = days, alternatives = emptyMap()))
            _selectedDayIndex.value = 0
            _workoutDrafts.value = emptyMap()
            showToast("تم تطبيق السبليت الجديد ✅")
            true
        } catch (_: Exception) {
            showToast("صيغة السبليت غلط. راجع الشكل الصحيح.")
            false
        }
    }

    fun resetToDefaultSplit() {
        val cfg = currentSnapshot().config
        updateConfig(cfg.copy(splitDays = WorkoutDefaults.DEFAULT_SPLIT, alternatives = emptyMap()))
        _selectedDayIndex.value = 0
        _workoutDrafts.value = emptyMap()
        showToast("رجّعنا جدول البالغامدي الافتراضي ✅")
    }

    private fun parseSplitJsonArray(arr: JSONArray): List<SplitDay> {
        val result = mutableListOf<SplitDay>()
        for (i in 0 until arr.length()) {
            val dayObj = arr.optJSONObject(i) ?: continue
            val dayName = dayObj.optString("n", "").ifBlank { dayObj.optString("name", "يوم ${i + 1}") }
            val exArr = dayObj.optJSONArray("x") ?: dayObj.optJSONArray("exercises") ?: continue
            val exercises = mutableListOf<ExerciseSpec>()
            for (j in 0 until exArr.length()) {
                val item = exArr.optJSONArray(j)
                if (item != null && item.length() >= 5) {
                    exercises.add(
                        ExerciseSpec(
                            name = item.optString(0, ""),
                            warmupSets = item.optString(1, "1–2"),
                            workingSets = item.optInt(2, 2).coerceIn(1, 12),
                            minReps = item.optInt(3, 6).coerceIn(1, 100),
                            maxReps = item.optInt(4, 10).coerceIn(1, 100),
                            alt1 = item.optString(5, ""),
                            alt2 = item.optString(6, ""),
                            note = item.optString(7, "")
                        )
                    )
                }
            }
            if (exercises.isNotEmpty()) result.add(SplitDay(dayName, exercises))
        }
        return result.take(14)
    }

    fun computeWeekNumber(startDate: String): Int =
        ((daysBetweenIso(startDate, todayIso()) / 7L).toInt() + 1).coerceAtLeast(1)
}
