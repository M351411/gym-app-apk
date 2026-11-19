package com.mutabei.workout.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.mutabei.workout.data.model.AiFoodReviewItem
import com.mutabei.workout.data.model.AiLabelResult
import com.mutabei.workout.data.model.BodyRecord
import com.mutabei.workout.data.model.CustomFood
import com.mutabei.workout.data.model.ExerciseSpec
import com.mutabei.workout.data.model.FoodItem
import com.mutabei.workout.data.model.LoggedExercise
import com.mutabei.workout.data.model.LoggedFoodItem
import com.mutabei.workout.data.model.NutritionConstants
import com.mutabei.workout.data.model.NutritionDay
import com.mutabei.workout.data.model.NutritionTargets
import com.mutabei.workout.data.model.PortionOption
import com.mutabei.workout.data.model.RecipeIngredient
import com.mutabei.workout.data.model.SetEntry
import com.mutabei.workout.data.model.SplitDay
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

data class AppDataSnapshot(
    val config: UserConfig = UserConfig(),
    val sessions: Map<String, WorkoutSession> = emptyMap(),
    val nutritionDays: Map<String, NutritionDay> = emptyMap(),
    val bodyRecords: List<BodyRecord> = emptyList(),
    val customFoods: List<CustomFood> = emptyList()
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
    val barcodeInput: String = ""
)

data class ExerciseHistoryPoint(
    val date: String,
    val sets: List<SetEntry>
) {
    fun topWeight(): Double = sets.maxOfOrNull { it.weightDouble() } ?: 0.0
    fun topE1Rm(): Double = sets.maxOfOrNull { it.estimated1Rm() } ?: 0.0
}

class MainViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val offlineFoodRepo: OfflineFoodRepository,
    private val geminiRepo: GeminiAiRepository,
    private val offRepo: OpenFoodFactsRepository,
    private val currentUserId: String
) : ViewModel() {

    val dataState: StateFlow<UiState<AppDataSnapshot>> = combine(
        firestoreRepo.observeUserConfig(currentUserId),
        firestoreRepo.observeWorkoutSessions(currentUserId),
        firestoreRepo.observeNutritionDays(currentUserId),
        firestoreRepo.observeBodyRecords(currentUserId),
        firestoreRepo.observeCustomFoods(currentUserId)
    ) { config, sessions, nutritionDays, bodyRecords, customFoods ->
        AppDataSnapshot(config, sessions, nutritionDays, bodyRecords, customFoods)
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

    // Draft sets per "dayIdx|exIdx" while working out
    private val _workoutDrafts = MutableStateFlow<Map<String, List<SetEntry>>>(emptyMap())
    val workoutDrafts: StateFlow<Map<String, List<SetEntry>>> = _workoutDrafts.asStateFlow()

    private val _restTimer = MutableStateFlow(RestTimerState())
    val restTimer: StateFlow<RestTimerState> = _restTimer.asStateFlow()
    private var timerJob: Job? = null

    private val _timerFinishedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val timerFinishedEvents: SharedFlow<Unit> = _timerFinishedEvents.asSharedFlow()

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

    init {
        viewModelScope.launch {
            offlineFoodRepo.ensureLoaded()
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

    fun showToast(message: String) {
        _toastMessages.tryEmit(message)
    }

    // ==================== REST TIMER ====================

    fun startRestTimer(seconds: Int? = null) {
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
            while (true) {
                val left = ((endTimeMs - System.currentTimeMillis() + 999) / 1000L).toInt()
                if (left > 0) {
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
        val entry = LoggedFoodItem(
            id = UUID.randomUUID().toString(),
            name = food.name,
            source = food.source,
            grams = (grams * 10.0).roundToInt() / 10.0,
            mealIndex = mealIndex,
            nutrients = LoggedFoodItem.normalizeNutrients(food.nutrients)
        )
        viewModelScope.launch {
            val res = firestoreRepo.saveNutritionDay(currentUserId, day.copy(items = day.items + entry))
            if (res.isSuccess) showToast("اتضافت ${food.name} ✅")
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
            if (it.id == itemId) it.copy(grams = grams, mealIndex = mealIndex) else it
        }
        viewModelScope.launch {
            firestoreRepo.saveNutritionDay(currentUserId, day.copy(items = updatedItems))
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
        val copied = yDay.items.map { it.copy(id = UUID.randomUUID().toString()) }
        viewModelScope.launch {
            val res = firestoreRepo.saveNutritionDay(currentUserId, cur.copy(items = cur.items + copied))
            if (res.isSuccess) showToast("تم نسخ وجبات أمس ✅")
        }
    }

    fun searchFoods(query: String) {
        viewModelScope.launch {
            val customFoods = currentSnapshot().customFoods
            _searchResults.value = offlineFoodRepo.search(query, customFoods)
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

    // ==================== GEMINI AI FOOD & BARCODE FLOWS ====================

    fun clearFoodSheetAi() {
        _foodSheetAi.value = FoodSheetAiState()
    }

    fun updateBarcodeInput(value: String) {
        _foodSheetAi.update { it.copy(barcodeInput = value.filter { ch -> ch.isDigit() }) }
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
                    AiFoodReviewItem(
                        id = "ai_${idx}_${System.currentTimeMillis()}",
                        detectedName = raw.nameAr.ifBlank { raw.nameEn },
                        grams = raw.grams,
                        selected = true,
                        fallbackVector = vec,
                        candidates = cands,
                        // Prefer Gemini's full micronutrient analysis when populated; user can still switch to DB candidates
                        selectedCandidateIndex = if (populatedCount >= 10 || cands.isEmpty()) -1 else 0
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
