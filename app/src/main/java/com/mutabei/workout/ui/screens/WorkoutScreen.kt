package com.mutabei.workout.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mutabei.workout.R
import com.mutabei.workout.data.model.ExerciseSpec
import com.mutabei.workout.data.model.HintLevel
import com.mutabei.workout.data.model.SetEntry
import com.mutabei.workout.data.model.WorkoutDefaults
import com.mutabei.workout.data.model.shiftIsoDate
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.ui.components.ConsistencyHeatmapCard
import com.mutabei.workout.ui.components.DualLineProgressChart
import com.mutabei.workout.ui.components.ExpandableSectionCard
import com.mutabei.workout.ui.components.MetricProgressBar
import com.mutabei.workout.ui.components.MuscleRecoveryHeatmapCard
import com.mutabei.workout.ui.components.MuscleSilhouetteCanvas
import com.mutabei.workout.ui.components.PersonalRecordBadge
import com.mutabei.workout.ui.components.PlateCalculatorDialog
import com.mutabei.workout.ui.components.SetGoalTargetIcon
import com.mutabei.workout.ui.components.StatusBadgePill
import com.mutabei.workout.ui.components.WorkoutSessionSummaryCard
import com.mutabei.workout.ui.components.WorkoutSessionTimerBar
import com.mutabei.workout.ui.components.computeDoubleProgression
import com.mutabei.workout.ui.components.computeSetTargetGoal
import com.mutabei.workout.ui.components.formatCounterLtr
import com.mutabei.workout.ui.components.isLivePersonalRecord
import com.mutabei.workout.ui.components.isolateLtr
import com.mutabei.workout.ui.components.subtleCardBorderColor
import com.mutabei.workout.ui.components.tactilePressScale
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.AppDataSnapshot
import com.mutabei.workout.ui.viewmodel.ExerciseHistoryPoint
import com.mutabei.workout.ui.viewmodel.MainViewModel
import com.mutabei.workout.ui.viewmodel.WorkoutSessionTimerState
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(
    snapshot: AppDataSnapshot,
    selectedDayIndex: Int,
    drafts: Map<String, List<SetEntry>>,
    workoutTimer: WorkoutSessionTimerState,
    viewModel: MainViewModel,
    onFabVisibilityChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val split = snapshot.config.effectiveSplit()
    val safeDayIdx = selectedDayIndex.coerceIn(0, (split.size - 1).coerceAtLeast(0))
    val today = todayIso()
    var plateCalcTarget by remember { mutableStateOf<Pair<String, Double>?>(null) }

    fun openYoutube(videoId: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WorkoutDefaults.youtubeUrl(videoId))))
        } catch (_: Exception) {
            viewModel.showToast("تعذّر فتح رابط يوتيوب.")
        }
    }

    if (plateCalcTarget != null) {
        PlateCalculatorDialog(
            exerciseName = plateCalcTarget!!.first,
            initialWeightKg = plateCalcTarget!!.second,
            onDismiss = { plateCalcTarget = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
        ) {
            // Anchored Split Day Selector Chips (stays crisp while content slides underneath)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("day_chips_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                split.forEachIndexed { idx, day ->
                    val selected = idx == safeDayIdx
                    FilterChip(
                        selected = selected,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectWorkoutDay(idx)
                        },
                        label = {
                            Text(
                                text = day.name,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = subtleCardBorderColor()
                        ),
                        modifier = Modifier.testTag("split_day_chip_$idx")
                    )
                }
            }

            // Smooth horizontal slide + fade transition when switching muscle splits
            AnimatedContent(
                targetState = safeDayIdx,
                transitionSpec = {
                    val forward = targetState >= initialState
                    val slideIn = slideInHorizontally(
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                    ) { fullWidth -> if (forward) fullWidth / 5 else -fullWidth / 5 }
                    val slideOut = slideOutHorizontally(
                        animationSpec = tween(220)
                    ) { fullWidth -> if (forward) -fullWidth / 6 else fullWidth / 6 }
                    (fadeIn(tween(240)) + slideIn) togetherWith (fadeOut(tween(180)) + slideOut)
                },
                label = "workout_split_switch",
                modifier = Modifier.weight(1f)
            ) { animatedDayIdx ->
                val dayIdx = animatedDayIdx.coerceIn(0, (split.size - 1).coerceAtLeast(0))
                val currentDay = split[dayIdx]
                val sessionId = "${today}_$dayIdx"
                val todaySession = snapshot.sessions[sessionId]
                val listState = rememberLazyListState()

                // Scroll-aware FAB visibility: hide while scrolling down, reappear when scrolling up or at top
                LaunchedEffect(listState, dayIdx) {
                    onFabVisibilityChange(true)
                    var prevIndex = 0
                    var prevOffset = 0
                    snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                        .collect { (idx, offset) ->
                            val atTop = idx == 0 && offset < 24
                            val scrollingDown = idx > prevIndex || (idx == prevIndex && offset > prevOffset + 18)
                            val scrollingUp = idx < prevIndex || (idx == prevIndex && offset < prevOffset - 18)
                            when {
                                atTop -> onFabVisibilityChange(true)
                                scrollingDown -> onFabVisibilityChange(false)
                                scrollingUp -> onFabVisibilityChange(true)
                            }
                            prevIndex = idx
                            prevOffset = offset
                        }
                }

                // Live workout session progress computation
                val totalPlannedSets = remember(currentDay) {
                    currentDay.exercises.sumOf { it.workingSets }.coerceAtLeast(1)
                }
                val completedSets = remember(currentDay, todaySession, drafts, dayIdx, snapshot.config.alternatives) {
                    currentDay.exercises.indices.sumOf { exIdx ->
                        val spec = currentDay.exercises[exIdx]
                        val altKey = "$dayIdx|$exIdx"
                        val chosenName = snapshot.config.alternatives[altKey]?.ifBlank { spec.name } ?: spec.name
                        val savedEx = todaySession?.exercises?.get(exIdx.toString())
                        val savedSets = if (savedEx != null && savedEx.name == chosenName) savedEx.sets else null
                        val activeSets = drafts[altKey] ?: savedSets ?: emptyList()
                        activeSets.count { it.repsInt() > 0 }.coerceAtMost(spec.workingSets)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Workout Session Progress Bar Card
                    item(key = "session_progress_$dayIdx") {
                        WorkoutSessionProgressCard(
                            dayName = currentDay.name,
                            completedSets = completedSets,
                            totalPlannedSets = totalPlannedSets
                        )
                    }

                    // Total Workout Session Timer
                    item(key = "workout_session_timer") {
                        WorkoutSessionTimerBar(
                            state = workoutTimer,
                            onToggle = viewModel::toggleWorkoutSessionTimer,
                            onReset = viewModel::resetWorkoutSessionTimer
                        )
                    }

                    // Live session summary & Tonnage card
                    item(key = "session_summary_$dayIdx") {
                        WorkoutSessionSummaryCard(
                            dayName = currentDay.name,
                            dayIndex = dayIdx,
                            todaySession = todaySession,
                            allSessions = snapshot.sessions
                        )
                    }

                    // General warm-up accordion
                    item(key = "general_warmup") {
                        ExpandableSectionCard(
                            title = "التسخين العام",
                            subtitleEn = "General Warm-Up · < 10 min",
                            statusBadgeText = "5 تمارين",
                            leadingIcon = Icons.Rounded.SelfImprovement,
                            testTag = "general_warmup_section"
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WorkoutDefaults.GENERAL_WARMUPS.forEach { w ->
                                    Surface(
                                        shape = RoundedCornerShape(99.dp),
                                        color = MaterialTheme.colorScheme.background,
                                        border = BorderStroke(1.dp, subtleCardBorderColor()),
                                        modifier = Modifier.clickable { openYoutube(w.youtubeId) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = stringResource(R.string.cd_youtube),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(w.title, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "10 عدات لكل تمرين، حاول تتحسن في الرينج من غير ما تجهد نفسك.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Exercise cards
                    itemsIndexed(
                        items = currentDay.exercises,
                        key = { idx, _ -> "${dayIdx}_$idx" }
                    ) { exIdx, spec ->
                        val altKey = "$dayIdx|$exIdx"
                        val chosenName = snapshot.config.alternatives[altKey]?.ifBlank { spec.name } ?: spec.name
                        val savedEx = todaySession?.exercises?.get(exIdx.toString())
                        val savedSets = if (savedEx != null && savedEx.name == chosenName) savedEx.sets else null
                        val defaultSets = savedSets ?: List(spec.workingSets) { SetEntry() }
                        val activeSets = drafts[altKey] ?: defaultSets
                        val history = remember(snapshot.sessions, chosenName) {
                            viewModel.exerciseHistory(snapshot.sessions, chosenName)
                        }

                        ExerciseCard(
                            dayIndex = dayIdx,
                            exIndex = exIdx,
                            spec = spec,
                            chosenName = chosenName,
                            isSaved = savedSets != null,
                            sets = activeSets,
                            history = history,
                            onSelectAlternative = { alt ->
                                viewModel.selectExerciseAlternative(dayIdx, exIdx, alt)
                            },
                            onOpenYoutube = ::openYoutube,
                            onSetChanged = { setIdx, weight, reps ->
                                viewModel.updateDraftSet(
                                    dayIndex = dayIdx,
                                    exIndex = exIdx,
                                    setIndex = setIdx,
                                    setCount = spec.workingSets,
                                    baseSets = activeSets,
                                    weight = weight,
                                    reps = reps
                                )
                            },
                            onQuickCompleteSet = { setIdx, fallbackWeight, fallbackReps ->
                                viewModel.quickCompleteSet(
                                    dayIndex = dayIdx,
                                    exIndex = exIdx,
                                    setIndex = setIdx,
                                    setCount = spec.workingSets,
                                    exerciseName = chosenName,
                                    baseSets = activeSets,
                                    fallbackWeight = fallbackWeight,
                                    fallbackReps = fallbackReps
                                )
                            },
                            onRepsDone = { viewModel.startRestTimer() },
                            onPrefillWeights = { lastSets, suggestedW ->
                                viewModel.prefillWeightsFromLastSession(
                                    dayIndex = dayIdx,
                                    exIndex = exIdx,
                                    setCount = spec.workingSets,
                                    lastSets = lastSets,
                                    suggestedWeightKg = suggestedW
                                )
                            },
                            onOpenPlateCalc = { w -> plateCalcTarget = chosenName to w },
                            onShowToast = viewModel::showToast,
                            onSaveExercise = {
                                viewModel.saveExerciseLog(dayIdx, exIdx, chosenName, activeSets)
                            }
                        )
                    }

                    // Session note
                    item(key = "session_note_$dayIdx") {
                        var noteText by remember(sessionId, todaySession?.note) {
                            mutableStateOf(todaySession?.note.orEmpty())
                        }
                        ExpandableSectionCard(
                            title = "ملاحظة الجلسة",
                            subtitleEn = "Session Notes",
                            statusBadgeText = if (!todaySession?.note.isNullOrBlank()) "محفوظة ✓" else null,
                            statusBadgeColor = LocalSemanticColors.current.ok,
                            leadingIcon = Icons.Rounded.NoteAlt,
                            initiallyExpanded = !todaySession?.note.isNullOrBlank(),
                            testTag = "session_note_section"
                        ) {
                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                placeholder = { Text("نوم، إرهاق، ألم، أي حاجة تفتكرها") },
                                minLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("session_note_input")
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveSessionNote(dayIdx, noteText)
                                    viewModel.showToast("اتحفظت الملاحظة ✅")
                                },
                                modifier = Modifier.testTag("save_session_note_button")
                            ) {
                                Text("احفظ الملاحظة")
                            }
                        }
                    }

                    // Muscle Recovery Heatmap + Deload Detector
                    item(key = "muscle_recovery_heatmap") {
                        MuscleRecoveryHeatmapCard(snapshot = snapshot)
                    }

                    // 12-Week Consistency Heatmap + Streak
                    item(key = "consistency_heatmap") {
                        val proteinTarget = remember(snapshot.config, snapshot.bodyRecords) {
                            viewModel.computeTargets(snapshot.config, snapshot.bodyRecords)?.proteinG
                        }
                        ConsistencyHeatmapCard(snapshot = snapshot, proteinTargetG = proteinTarget)
                    }

                    // Weekly muscle sets (last 7 days)
                    item(key = "weekly_muscle_sets") {
                        WeeklyMuscleSetsCard(snapshot = snapshot)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSessionProgressCard(
    dayName: String,
    completedSets: Int,
    totalPlannedSets: Int
) {
    val semantic = LocalSemanticColors.current
    val pct = ((completedSets.toDouble() / totalPlannedSets.coerceAtLeast(1)) * 100.0)
        .roundToInt()
        .coerceIn(0, 100)
    val accent = if (pct >= 100) semantic.ok else MaterialTheme.colorScheme.primary

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (pct >= 100) semantic.ok.copy(alpha = 0.55f) else subtleCardBorderColor()),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workout_session_progress_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "تقدم جلسة $dayName",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatCounterLtr("$completedSets", "$totalPlannedSets", "مجموعة مكتملة"),
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadgePill(
                    text = isolateLtr("$pct%"),
                    accentColor = accent
                )
            }
            MetricProgressBar(
                current = completedSets.toDouble(),
                target = totalPlannedSets.toDouble(),
                barColor = accent,
                height = 9.dp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(
    dayIndex: Int,
    exIndex: Int,
    spec: ExerciseSpec,
    chosenName: String,
    isSaved: Boolean,
    sets: List<SetEntry>,
    history: List<ExerciseHistoryPoint>,
    onSelectAlternative: (String) -> Unit,
    onOpenYoutube: (String) -> Unit,
    onSetChanged: (Int, String?, String?) -> Unit,
    onQuickCompleteSet: (Int, String, String) -> Unit,
    onRepsDone: () -> Unit,
    onPrefillWeights: (List<SetEntry>, Double?) -> Unit,
    onOpenPlateCalc: (Double) -> Unit,
    onShowToast: (String) -> Unit,
    onSaveExercise: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current
    val today = todayIso()
    val past = remember(history, today) { history.filter { it.date != today } }
    val lastSession = past.lastOrNull()
    val isLivePr = remember(sets, past) { isLivePersonalRecord(sets, past) }
    val progressionAdvice = remember(spec, lastSession) { computeDoubleProgression(spec, lastSession) }
    val muscleKey = WorkoutDefaults.detectMuscleGroup(chosenName)
    val muscleLabel = WorkoutDefaults.MUSCLE_GROUPS[muscleKey]?.arabicLabel ?: "عضلة"

    // Plateau warning: last 3 past sessions flat vs 4th-from-last
    val hasPlateau = remember(past) {
        if (past.size >= 4) {
            val e1s = past.map { it.topE1Rm() }
            val last3Max = e1s.takeLast(3).maxOrNull() ?: 0.0
            last3Max <= e1s[e1s.size - 4]
        } else false
    }

    var warmupMessage by remember(chosenName) { mutableStateOf<String?>(null) }
    var tipExpanded by remember(chosenName) { mutableStateOf(false) }
    val tipChevronRotation by animateFloatAsState(
        targetValue = if (tipExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "tip_chevron"
    )
    val saveInteraction = remember { MutableInteractionSource() }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (isSaved) 1.5.dp else 1.dp, if (isSaved) semantic.ok.copy(alpha = 0.85f) else subtleCardBorderColor()),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exercise_card_${dayIndex}_$exIndex")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MuscleSilhouetteCanvas(muscleKey = muscleKey)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = chosenName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (isSaved) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "تم التسجيل",
                                tint = semantic.ok,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (isLivePr) {
                            PersonalRecordBadge()
                        }
                    }
                    Text(
                        text = "$muscleLabel · ${spec.workingSets} مجموعات شغل · تسخين ${spec.warmupSets}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = isolateLtr("${spec.minReps}–${spec.maxReps}"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Alternatives + YouTube chips
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                spec.alternativesList().forEach { option ->
                    val isSelected = option == chosenName
                    val ytId = WorkoutDefaults.YOUTUBE_IDS[option]
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else subtleCardBorderColor())
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clickable { onSelectAlternative(option) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                            if (ytId != null) {
                                Box(
                                    modifier = Modifier
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface)
                                        .clickable { onOpenYoutube(ytId) }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "شرح $option",
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Compact Collapsible Exercise Tip Chip
            if (spec.note.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, subtleCardBorderColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        .testTag("exercise_tip_chip_${dayIndex}_$exIndex")
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tipExpanded = !tipExpanded }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = semantic.warn,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "نصيحة الأداء",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Rounded.ExpandMore,
                                contentDescription = if (tipExpanded) "إخفاء النصيحة" else "عرض النصيحة",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(tipChevronRotation)
                            )
                        }
                        if (tipExpanded) {
                            HorizontalDivider(color = subtleCardBorderColor())
                            Text(
                                text = spec.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Plateau warning
            if (hasPlateau) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = semantic.warn,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = "الوزن ثابت من 3 جلسات. جرّب تزوّد عدة أو وزن بشكل تدريجي، من غير ما تغيّر البرنامج قبل الـ12 أسبوع.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = semantic.warn
                    )
                }
            }

            // Last session summary + 1-tap weight fill
            if (lastSession != null) {
                Spacer(Modifier.height(10.dp))
                val summary = lastSession.sets.joinToString(" ، ") { s ->
                    isolateLtr("${s.weight.ifBlank { "–" }}×${s.reps.ifBlank { "–" }}")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "آخر مرة (${lastSession.date}): $summary",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val nextW = if (progressionAdvice?.readyToIncrease == true) progressionAdvice.suggestedWeightKg else null
                                onPrefillWeights(lastSession.sets, nextW)
                            }
                            .testTag("prefill_weights_${dayIndex}_$exIndex")
                    ) {
                        Text(
                            text = if (progressionAdvice?.readyToIncrease == true) "طبّق الوزن ↑" else "انسخ الأوزان",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Working sets rows (with last-session placeholders, 1-tap checkmark logging, emerald animation & goal target icon)
            Spacer(Modifier.height(10.dp))
            sets.forEachIndexed { setIdx, set ->
                val repHint = WorkoutDefaults.repHint(set.reps, spec.minReps, spec.maxReps)
                val setGoal = remember(spec, lastSession, setIdx, progressionAdvice) {
                    computeSetTargetGoal(spec, lastSession, setIdx, progressionAdvice)
                }
                val prevSet = lastSession?.sets?.getOrNull(setIdx)
                val suggestedWText = setGoal?.suggestedWeightKg?.let { w ->
                    if (w % 1.0 == 0.0) w.toInt().toString() else String.format("%.1f", w)
                }
                val weightPlaceholder = prevSet?.weight?.takeIf { it.isNotBlank() } ?: suggestedWText ?: "كجم"
                val repsPlaceholder = prevSet?.reps?.takeIf { it.isNotBlank() } ?: "${spec.minReps}"
                val isCompleted = set.repsInt() > 0

                val rowBgColor by animateColorAsState(
                    targetValue = if (isCompleted) Color(0xFF22C55E).copy(alpha = 0.15f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 300),
                    label = "set_row_bg"
                )
                val checkPopScale by animateFloatAsState(
                    targetValue = if (isCompleted) 1.06f else 1f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
                    label = "set_check_pop"
                )
                val checkInteraction = remember { MutableInteractionSource() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(rowBgColor)
                        .padding(horizontal = 4.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${setIdx + 1}",
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) semantic.ok else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(18.dp),
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = set.weight,
                        onValueChange = { onSetChanged(setIdx, it, null) },
                        placeholder = {
                            Text(
                                text = weightPlaceholder,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_weight_${dayIndex}_${exIndex}_$setIdx")
                    )
                    OutlinedTextField(
                        value = set.reps,
                        onValueChange = { onSetChanged(setIdx, null, it) },
                        placeholder = {
                            Text(
                                text = repsPlaceholder,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (set.reps.isNotBlank()) onRepsDone()
                            }
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_reps_${dayIndex}_${exIndex}_$setIdx")
                    )
                    SetGoalTargetIcon(
                        goal = setGoal,
                        currentSet = set,
                        minReps = spec.minReps,
                        testTag = "set_goal_icon_${dayIndex}_${exIndex}_$setIdx",
                        onApplyWeight = { wStr -> onSetChanged(setIdx, wStr, null) },
                        onShortTapHint = {
                            onShowToast("اضغط مطوّلاً على أيقونة الهدف لعرض هدف المجموعة ${setIdx + 1}")
                        }
                    )
                    // 1-Tap Quick-Complete Set Button (✓)
                    Surface(
                        shape = CircleShape,
                        color = if (isCompleted) Color(0xFF22C55E) else MaterialTheme.colorScheme.background,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isCompleted) Color(0xFF22C55E) else subtleCardBorderColor()
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .tactilePressScale(checkInteraction, pressedScale = 0.92f)
                            .graphicsLayer {
                                scaleX = checkPopScale
                                scaleY = checkPopScale
                            }
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = checkInteraction,
                                indication = null
                            ) {
                                focusManager.clearFocus()
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val fallbackW = if (weightPlaceholder != "كجم") weightPlaceholder else ""
                                onQuickCompleteSet(setIdx, fallbackW, repsPlaceholder)
                            }
                            .testTag("quick_complete_set_${dayIndex}_${exIndex}_$setIdx")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "إكمال المجموعة ${setIdx + 1}",
                                tint = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                if (repHint != null) {
                    val hintColor = when (repHint.level) {
                        HintLevel.OK -> semantic.ok
                        HintLevel.HIGH -> semantic.warn
                        HintLevel.LOW -> semantic.low
                    }
                    Text(
                        text = repHint.message,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = hintColor,
                        modifier = Modifier.padding(start = 26.dp, bottom = 2.dp)
                    )
                }
            }

            // Equalized Action Buttons Row (حاسبة التسخين | راحة | الطارات)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val baseW = sets.firstOrNull()?.weightDouble()?.takeIf { it > 0.0 }
                            ?: lastSession?.topWeight()
                            ?: 0.0
                        warmupMessage = WorkoutDefaults.warmUpAdvice(spec.warmupSets, baseW)
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("warmup_calc_${dayIndex}_$exIndex")
                ) {
                    Text(
                        text = "حاسبة التسخين",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onRepsDone,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("start_rest_${dayIndex}_$exIndex")
                ) {
                    Text(
                        text = "⏱ راحة",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = {
                        val baseW = sets.firstOrNull()?.weightDouble()?.takeIf { it > 0.0 }
                            ?: lastSession?.topWeight()
                            ?: 60.0
                        onOpenPlateCalc(baseW)
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("plate_calc_${dayIndex}_$exIndex")
                ) {
                    Text(
                        text = "⚖️ الطارات",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!warmupMessage.isNullOrBlank()) {
                Text(
                    text = warmupMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // Finish / Update Exercise button with tactile press scale
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSaveExercise()
                },
                interactionSource = saveInteraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .tactilePressScale(saveInteraction)
                    .testTag("finish_exercise_${dayIndex}_$exIndex")
            ) {
                Text(
                    text = if (isSaved) "حدّث التمرين" else "خلّصت التمرين",
                    fontWeight = FontWeight.Bold
                )
            }

            // Progress chart & History accordion
            Spacer(Modifier.height(10.dp))
            ExpandableSectionCard(
                title = "التطور والسجل",
                subtitleEn = "Progress & History",
                statusBadgeText = "${history.size} جلسة",
                leadingIcon = Icons.Rounded.History,
                testTag = "exercise_history_${dayIndex}_$exIndex"
            ) {
                if (history.isEmpty()) {
                    Text(
                        text = "لسه مفيش جلسات متسجلة للتمرين ده.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val bestW = history.maxOfOrNull { it.topWeight() } ?: 0.0
                    val bestE1 = history.maxOfOrNull { it.topE1Rm() } ?: 0.0
                    Text(
                        text = "أعلى وزن: ${String.format("%.1f", bestW)} كجم · أفضل تقدير لأقصى وزن: ${bestE1.roundToInt()} كجم",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DualLineProgressChart(
                        primarySeries = history.map { it.topWeight() },
                        secondarySeries = history.map { it.topE1Rm() },
                        unitLabel = "كجم",
                        legendText = "الخط الأزرق أعلى وزن، المتقطع تقدير أقصى وزن"
                    )
                    Spacer(Modifier.height(6.dp))
                    history.reversed().take(12).forEach { pt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pt.date, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"))
                            Text(
                                text = pt.sets.joinToString(" ، ") {
                                    isolateLtr("${it.weight.ifBlank { "–" }}×${it.reps.ifBlank { "–" }}")
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(color = subtleCardBorderColor())
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyMuscleSetsCard(snapshot: AppDataSnapshot) {
    val semantic = LocalSemanticColors.current
    val today = todayIso()
    val start7 = shiftIsoDate(today, -6)
    val done = mutableMapOf<String, Int>()
    val planned = mutableMapOf<String, Int>()

    snapshot.sessions.values.forEach { s ->
        if (s.date in start7..today) {
            s.exercises.values.forEach { ex ->
                val g = WorkoutDefaults.detectMuscleGroup(ex.name)
                val count = ex.sets.count { it.repsInt() > 0 }
                done[g] = (done[g] ?: 0) + count
            }
        }
    }
    snapshot.config.effectiveSplit().forEach { day ->
        day.exercises.forEach { ex ->
            val g = WorkoutDefaults.detectMuscleGroup(ex.name)
            planned[g] = (planned[g] ?: 0) + ex.workingSets
        }
    }

    val totalDone = done.values.sum()
    val totalPlanned = planned.values.sum().coerceAtLeast(1)

    ExpandableSectionCard(
        title = "المجموعات الأسبوعية لكل عضلة",
        subtitleEn = "Weekly Volume · 7 Days",
        statusBadgeText = formatCounterLtr("$totalDone", "$totalPlanned", "مجموعة"),
        statusBadgeColor = if (totalDone >= totalPlanned) semantic.ok else MaterialTheme.colorScheme.primary,
        leadingIcon = Icons.Rounded.FitnessCenter,
        testTag = "weekly_muscle_sets_card"
    ) {
        WorkoutDefaults.MUSCLE_GROUPS.entries
            .filter { (k, _) -> (planned[k] ?: 0) > 0 || (done[k] ?: 0) > 0 }
            .forEach { (k, meta) ->
                val d = done[k] ?: 0
                val p = planned[k] ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(meta.arabicLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        text = formatCounterLtr("$d", "$p", "مخطط"),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")
                    )
                }
                Spacer(Modifier.height(4.dp))
                MetricProgressBar(current = d.toDouble(), target = max(p, 1).toDouble())
            }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "بيحسب المجموعات اللي سجّلت فيها عدات بس. \"دراع\" بتشمل البايسبس والترايسبس. التوصيات العامة للتضخيم حوالي 10–20 مجموعة أسبوعيًا لكل عضلة.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
