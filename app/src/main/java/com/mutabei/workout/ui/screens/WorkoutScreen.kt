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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mutabei.workout.R
import com.mutabei.workout.data.model.ExerciseSpec
import com.mutabei.workout.data.model.HintLevel
import com.mutabei.workout.data.model.SetEntry
import com.mutabei.workout.data.model.WorkoutDefaults
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
import com.mutabei.workout.ui.components.WeeklyMuscleSetsCard
import com.mutabei.workout.ui.components.WorkoutSessionSummaryCard
import com.mutabei.workout.ui.components.WorkoutSessionTimerBar
import com.mutabei.workout.ui.components.WorkoutSplitProgressHistoryCard
import com.mutabei.workout.ui.components.WorkoutSubView
import com.mutabei.workout.ui.components.WorkoutSubViewSwitcher
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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class ExerciseDrawerSection {
    ALTERNATIVES,
    TIP,
    TOOLS
}

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
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val split = snapshot.config.effectiveSplit()
    val safeDayIdx = selectedDayIndex.coerceIn(0, (split.size - 1).coerceAtLeast(0))
    val today = todayIso()
    var plateCalcTarget by remember { mutableStateOf<Pair<String, Double>?>(null) }
    var selectedSubView by rememberSaveable { mutableStateOf(WorkoutSubView.SESSION) }

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
            // 1. Anchored Muscle Split Selector Chips
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

            // 2. Clean Sub-View Switcher: Session & Exercises vs Recovery & Log
            WorkoutSubViewSwitcher(
                selected = selectedSubView,
                onSelect = { selectedSubView = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // 3. Coordinated RTL-aware directional slide + fade transition between sub-views & splits
            AnimatedContent(
                targetState = selectedSubView to safeDayIdx,
                transitionSpec = {
                    val forward = if (targetState.first != initialState.first) {
                        targetState.first.ordinal >= initialState.first.ordinal
                    } else {
                        targetState.second >= initialState.second
                    }
                    val dir = if (forward) (if (isRtl) -1 else 1) else (if (isRtl) 1 else -1)
                    val slideIn = slideInHorizontally(
                        animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
                    ) { fullWidth -> dir * (fullWidth / 10) }
                    val slideOut = slideOutHorizontally(
                        animationSpec = tween(180)
                    ) { fullWidth -> -dir * (fullWidth / 10) }
                    (fadeIn(tween(240)) + slideIn) togetherWith (fadeOut(tween(180)) + slideOut)
                },
                label = "workout_subview_split_switch",
                modifier = Modifier.weight(1f)
            ) { (activeSubView, animatedDayIdx) ->
                val dayIdx = animatedDayIdx.coerceIn(0, (split.size - 1).coerceAtLeast(0))
                val currentDay = split[dayIdx]
                val sessionId = "${today}_$dayIdx"
                val todaySession = snapshot.sessions[sessionId]

                when (activeSubView) {
                    WorkoutSubView.SESSION -> {
                        val listState = rememberLazyListState()
                        val isScrolling by remember(listState) {
                            derivedStateOf { listState.isScrollInProgress }
                        }
                        LaunchedEffect(isScrolling, dayIdx) {
                            if (isScrolling) {
                                onFabVisibilityChange(false)
                            } else {
                                delay(180)
                                onFabVisibilityChange(true)
                            }
                        }

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

                        val restSec = snapshot.config.restSeconds
                        val restSummary = remember(restSec) {
                            val m = restSec / 60
                            val s = restSec % 60
                            if (s == 0) "${isolateLtr("$m")}د" else isolateLtr("$m:${s.toString().padStart(2, '0')}")
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Session progress strip
                            item(key = "session_progress_$dayIdx") {
                                WorkoutSessionProgressCard(
                                    dayName = currentDay.name,
                                    completedSets = completedSets,
                                    totalPlannedSets = totalPlannedSets
                                )
                            }

                            // Compact Workout Session Timer
                            item(key = "workout_session_timer") {
                                WorkoutSessionTimerBar(
                                    state = workoutTimer,
                                    onToggle = viewModel::toggleWorkoutSessionTimer,
                                    onReset = viewModel::resetWorkoutSessionTimer
                                )
                            }

                            // Live session summary & Tonnage card (auto-hides until >=1 set is logged)
                            item(key = "session_summary_$dayIdx") {
                                WorkoutSessionSummaryCard(
                                    dayName = currentDay.name,
                                    dayIndex = dayIdx,
                                    todaySession = todaySession,
                                    allSessions = snapshot.sessions
                                )
                            }

                            // Compact Warm-Up toggle (collapsed by default)
                            item(key = "general_warmup") {
                                ExpandableSectionCard(
                                    title = "التسخين العام",
                                    subtitleEn = "General Warm-Up · < 10 min",
                                    statusBadgeText = "${isolateLtr("5")} تمارين",
                                    leadingIcon = Icons.Rounded.SelfImprovement,
                                    initiallyExpanded = false,
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
                                        text = "${isolateLtr("10")} عدات لكل تمرين، حاول تتحسن في الرينج من غير ما تجهد نفسك.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Compact, scannable Exercise Cards
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
                                    restSummary = restSummary,
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

                            // Session Note (collapsed by default)
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
                                    initiallyExpanded = false,
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

                            // Finish Workout Action Card
                            item(key = "finish_workout_$dayIdx") {
                                FinishWorkoutActionCard(
                                    dayName = currentDay.name,
                                    completedSets = completedSets,
                                    totalPlannedSets = totalPlannedSets,
                                    onFinishWorkout = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (workoutTimer.isRunning) {
                                            viewModel.pauseWorkoutSessionTimer()
                                        }
                                        viewModel.dismissRestTimer()
                                        viewModel.showToast(
                                            "عاش يا بطل! أنهيت جلسة ${currentDay.name} (${isolateLtr("$completedSets / $totalPlannedSets")} مجموعة) 💪"
                                        )
                                    }
                                )
                            }
                        }
                    }

                    WorkoutSubView.RECOVERY_LOG -> {
                        LaunchedEffect(Unit) {
                            onFabVisibilityChange(false)
                        }
                        val recoveryListState = rememberLazyListState()
                        val proteinTarget = remember(snapshot.config, snapshot.bodyRecords) {
                            viewModel.computeTargets(snapshot.config, snapshot.bodyRecords)?.proteinG
                        }

                        LazyColumn(
                            state = recoveryListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item(key = "muscle_recovery_heatmap") {
                                MuscleRecoveryHeatmapCard(snapshot = snapshot)
                            }
                            item(key = "weekly_muscle_sets") {
                                WeeklyMuscleSetsCard(snapshot = snapshot)
                            }
                            item(key = "consistency_heatmap") {
                                ConsistencyHeatmapCard(snapshot = snapshot, proteinTargetG = proteinTarget)
                            }
                            item(key = "split_progress_history_$dayIdx") {
                                WorkoutSplitProgressHistoryCard(
                                    dayName = currentDay.name,
                                    dayIndex = dayIdx,
                                    exercises = currentDay.exercises,
                                    snapshot = snapshot,
                                    viewModel = viewModel
                                )
                            }
                        }
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

@Composable
private fun FinishWorkoutActionCard(
    dayName: String,
    completedSets: Int,
    totalPlannedSets: Int,
    onFinishWorkout: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val allDone = completedSets >= totalPlannedSets && totalPlannedSets > 0
    val interaction = remember { MutableInteractionSource() }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (allDone) semantic.ok.copy(alpha = 0.55f) else subtleCardBorderColor()
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (allDone) "الجلسة مكتملة!" else "إنهاء جلسة $dayName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (allDone) semantic.ok else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatCounterLtr("$completedSets", "$totalPlannedSets", "مجموعة مكتملة"),
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onFinishWorkout,
                interactionSource = interaction,
                colors = if (allDone) {
                    ButtonDefaults.buttonColors(containerColor = semantic.ok, contentColor = Color.White)
                } else {
                    ButtonDefaults.buttonColors()
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier
                    .tactilePressScale(interaction)
                    .testTag("finish_workout_session_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Flag,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("إنهاء التمرين", fontWeight = FontWeight.Bold)
            }
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
    restSummary: String,
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
    val alternatives = remember(spec) { spec.alternativesList() }

    val hasPlateau = remember(past) {
        if (past.size >= 4) {
            val e1s = past.map { it.topE1Rm() }
            val last3Max = e1s.takeLast(3).maxOrNull() ?: 0.0
            last3Max <= e1s[e1s.size - 4]
        } else false
    }

    // Secondary details stay collapsed by default so every exercise card is crisp and scannable
    var expandedDrawer by remember(chosenName) { mutableStateOf<ExerciseDrawerSection?>(null) }
    var warmupMessage by remember(chosenName) { mutableStateOf<String?>(null) }
    val saveInteraction = remember { MutableInteractionSource() }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isSaved) 1.5.dp else 1.dp,
            color = if (isSaved) semantic.ok.copy(alpha = 0.85f) else subtleCardBorderColor()
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .testTag("exercise_card_${dayIndex}_$exIndex")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Clean Header Row (LTR-isolated exercise name + target muscle/sets/rest summary + rep range badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MuscleSilhouetteCanvas(muscleKey = muscleKey)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = isolateLtr(chosenName),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
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
                        text = "$muscleLabel · ${isolateLtr("${spec.workingSets}")} مجموعات · راحة $restSummary",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                            fontSize = 17.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Plateau warning (only shown if detected)
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
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = "الوزن ثابت من ${isolateLtr("3")} جلسات. جرّب تزوّد عدة أو وزن تدريجياً.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = semantic.warn
                    )
                }
            }

            // 2. Compact Last-Session Hint + Single "نسخ" Action
            if (lastSession != null) {
                Spacer(Modifier.height(10.dp))
                val summaryLtr = isolateLtr(
                    lastSession.sets.joinToString(" • ") { s ->
                        "${s.weight.ifBlank { "–" }}×${s.reps.ifBlank { "–" }}"
                    }
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, subtleCardBorderColor()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "آخر مرة • ${isolateLtr(lastSession.date)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = summaryLtr,
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
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
                                text = if (progressionAdvice?.readyToIncrease == true) "رفع الوزن ↑" else "نسخ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 3. Unobstructed Working Set Rows (tabular كجم & عدات + 1-tap ✓ completion with emerald tint)
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
                        text = isolateLtr("${setIdx + 1}"),
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

            // 4. Compact On-Demand Secondary Controls Strip (Collapsed by Default!)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (alternatives.size > 1 || WorkoutDefaults.YOUTUBE_IDS[chosenName] != null) {
                    ExerciseDrawerTogglePill(
                        label = "البدائل",
                        icon = Icons.Rounded.SwapHoriz,
                        expanded = expandedDrawer == ExerciseDrawerSection.ALTERNATIVES,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            expandedDrawer = if (expandedDrawer == ExerciseDrawerSection.ALTERNATIVES) null else ExerciseDrawerSection.ALTERNATIVES
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_alternatives_${dayIndex}_$exIndex")
                    )
                }
                if (spec.note.isNotBlank()) {
                    ExerciseDrawerTogglePill(
                        label = "نصيحة",
                        icon = Icons.Rounded.Lightbulb,
                        iconTint = semantic.warn,
                        expanded = expandedDrawer == ExerciseDrawerSection.TIP,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            expandedDrawer = if (expandedDrawer == ExerciseDrawerSection.TIP) null else ExerciseDrawerSection.TIP
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("exercise_tip_chip_${dayIndex}_$exIndex")
                    )
                }
                ExerciseDrawerTogglePill(
                    label = "أدوات",
                    icon = Icons.Rounded.Build,
                    expanded = expandedDrawer == ExerciseDrawerSection.TOOLS,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        expandedDrawer = if (expandedDrawer == ExerciseDrawerSection.TOOLS) null else ExerciseDrawerSection.TOOLS
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_tools_${dayIndex}_$exIndex")
                )
            }

            // 5. Expanded On-Demand Drawer Content
            when (expandedDrawer) {
                ExerciseDrawerSection.ALTERNATIVES -> {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, subtleCardBorderColor()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(10.dp)
                        ) {
                            alternatives.forEach { option ->
                                val isSelected = option == chosenName
                                val ytId = WorkoutDefaults.YOUTUBE_IDS[option]
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else subtleCardBorderColor()
                                    ),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .clickable {
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onSelectAlternative(option)
                                                }
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = isolateLtr(option),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (ytId != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
                                                        else MaterialTheme.colorScheme.background
                                                    )
                                                    .clickable { onOpenYoutube(ytId) }
                                                    .padding(horizontal = 10.dp),
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
                    }
                }

                ExerciseDrawerSection.TIP -> {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, subtleCardBorderColor()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = spec.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }

                ExerciseDrawerSection.TOOLS -> {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, subtleCardBorderColor()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (history.isNotEmpty()) {
                                HorizontalDivider(color = subtleCardBorderColor())
                                val bestW = history.maxOfOrNull { it.topWeight() } ?: 0.0
                                val bestE1 = history.maxOfOrNull { it.topE1Rm() } ?: 0.0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "أعلى وزن: ${isolateLtr(String.format("%.1f", bestW))} كجم · ${isolateLtr("1RM")}: ${isolateLtr("${bestE1.roundToInt()}")} كجم (${isolateLtr("${history.size}")} جلسة)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (history.size >= 2) {
                                    DualLineProgressChart(
                                        primarySeries = history.map { it.topWeight() },
                                        secondarySeries = history.map { it.topE1Rm() },
                                        unitLabel = "كجم",
                                        legendText = "أعلى وزن (أزرق) وتقدير 1RM (متقطع)"
                                    )
                                }
                            }
                        }
                    }
                }

                null -> Unit
            }

            // 6. Primary Save / Update Exercise CTA
            Spacer(Modifier.height(10.dp))
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
                    text = if (isSaved) "حدّث التمرين ✓" else "حفظ التمرين",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ExerciseDrawerTogglePill(
    label: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color? = null
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "drawer_pill_chevron"
    )
    val bgColor by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.background,
        animationSpec = tween(200),
        label = "drawer_pill_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else subtleCardBorderColor(),
        animationSpec = tween(200),
        label = "drawer_pill_border"
    )

    Surface(
        shape = RoundedCornerShape(11.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (expanded) MaterialTheme.colorScheme.primary else (iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (expanded) FontWeight.Bold else FontWeight.Medium,
                color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(15.dp)
                    .rotate(chevronRotation)
            )
        }
    }
}
