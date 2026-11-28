package com.mutabei.workout.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import com.mutabei.workout.data.model.SetEntry
import com.mutabei.workout.data.model.SplitDay
import com.mutabei.workout.data.model.WorkoutDefaults
import com.mutabei.workout.data.model.WorkoutSession
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.ui.components.ConsistencyHeatmapCard
import com.mutabei.workout.ui.components.DualLineProgressChart
import com.mutabei.workout.ui.components.ExpandableSectionCard
import com.mutabei.workout.ui.components.MetricProgressBar
import com.mutabei.workout.ui.components.MuscleRecoveryHeatmapCard
import com.mutabei.workout.ui.components.MuscleSilhouetteCanvas
import com.mutabei.workout.ui.components.PersonalRecordBadge
import com.mutabei.workout.ui.components.PlateCalculatorDialog
import com.mutabei.workout.ui.components.SetOverloadCueChip
import com.mutabei.workout.ui.components.WeeklyMuscleSetsCard
import com.mutabei.workout.ui.components.WorkoutSplitProgressHistoryCard
import com.mutabei.workout.ui.components.WorkoutSubView
import com.mutabei.workout.ui.components.WorkoutSubViewSwitcher
import com.mutabei.workout.ui.components.computeDoubleProgression
import com.mutabei.workout.ui.components.computeSetTargetGoal
import com.mutabei.workout.ui.components.formatCounterLtr
import com.mutabei.workout.ui.components.formatWorkoutElapsed
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

private enum class ExerciseInlinePanel {
    ALTERNATIVES,
    TIP,
    WARMUP,
    HISTORY
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
            // 1. Balanced, Smoothly Scrollable Split Selector Pills (no awkward hard-clipped edges)
            SplitDayPillStrip(
                split = split,
                selectedIndex = safeDayIdx,
                onSelectDay = { idx ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.selectWorkoutDay(idx)
                }
            )

            // 2. Calm Borderless Sub-View Switcher (`الجلسة والتمارين` vs `الاستشفاء والسجل`)
            WorkoutSubViewSwitcher(
                selected = selectedSubView,
                onSelect = { selectedSubView = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // 3. Coordinated RTL-aware directional slide + fade transition
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
                    ) { fullWidth -> dir * (fullWidth / 12) }
                    val slideOut = slideOutHorizontally(
                        animationSpec = tween(170)
                    ) { fullWidth -> -dir * (fullWidth / 12) }
                    (fadeIn(tween(220)) + slideIn) togetherWith (fadeOut(tween(170)) + slideOut)
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
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Unified Session Header (Progress + Elapsed Timer + Inline Live Tonnage/PRs)
                            item(key = "session_unified_header_$dayIdx") {
                                UnifiedSessionHeaderCard(
                                    dayName = currentDay.name,
                                    dayIndex = dayIdx,
                                    completedSets = completedSets,
                                    totalPlannedSets = totalPlannedSets,
                                    workoutTimer = workoutTimer,
                                    todaySession = todaySession,
                                    allSessions = snapshot.sessions,
                                    onToggleTimer = viewModel::toggleWorkoutSessionTimer,
                                    onResetTimer = viewModel::resetWorkoutSessionTimer
                                )
                            }

                            // Slim Collapsible General Warm-Up Strip
                            item(key = "general_warmup") {
                                CompactWarmupStrip(onOpenYoutube = ::openYoutube)
                            }

                            // Streamlined Exercise Cards
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
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
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

/**
 * Balanced, calm split day pill bar with auto-scroll to active item and soft edge fade
 * so pills at the trailing edge never appear awkwardly half-clipped.
 */
@Composable
private fun SplitDayPillStrip(
    split: List<SplitDay>,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    val rowState = rememberLazyListState()
    val bgColor = MaterialTheme.colorScheme.background
    val canScrollStart by remember { derivedStateOf { rowState.canScrollBackward } }
    val canScrollEnd by remember { derivedStateOf { rowState.canScrollForward } }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in split.indices) {
            rowState.animateScrollToItem(selectedIndex)
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("day_chips_row")
        ) {
            itemsIndexed(items = split, key = { idx, d -> "${idx}_${d.name}" }) { idx, day ->
                val selected = idx == selectedIndex
                val pillBg by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    animationSpec = tween(180),
                    label = "split_pill_bg"
                )
                val pillText by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(180),
                    label = "split_pill_text"
                )
                val interaction = remember { MutableInteractionSource() }

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = pillBg,
                    modifier = Modifier
                        .tactilePressScale(interaction, pressedScale = 0.96f)
                        .clip(RoundedCornerShape(99.dp))
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) { onSelectDay(idx) }
                        .testTag("split_day_chip_$idx")
                ) {
                    Text(
                        text = day.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = pillText,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Subtle edge fades so overflow feels intentional instead of hard-clipped
        if (canScrollStart) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(20.dp)
                    .height(44.dp)
                    .background(Brush.horizontalGradient(listOf(bgColor, Color.Transparent)))
            )
        }
        if (canScrollEnd) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(20.dp)
                    .height(44.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, bgColor)))
            )
        }
    }
}

/**
 * Unified, low-clutter Session Header combining split progress, compact elapsed timer,
 * and inline tonnage/PR telemetry into a single calm surface (replaces 3 stacked cards).
 */
@Composable
private fun UnifiedSessionHeaderCard(
    dayName: String,
    dayIndex: Int,
    completedSets: Int,
    totalPlannedSets: Int,
    workoutTimer: WorkoutSessionTimerState,
    todaySession: WorkoutSession?,
    allSessions: Map<String, WorkoutSession>,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val today = todayIso()
    val pct = ((completedSets.toDouble() / totalPlannedSets.coerceAtLeast(1)) * 100.0)
        .roundToInt()
        .coerceIn(0, 100)
    val progressAccent = if (pct >= 100) semantic.ok else MaterialTheme.colorScheme.primary
    val timerAccent = when {
        workoutTimer.isRunning -> semantic.ok
        workoutTimer.hasStarted -> semantic.warn
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val liveStats = remember(todaySession, allSessions, dayIndex) {
        if (todaySession == null || todaySession.exercises.isEmpty()) {
            null
        } else {
            val tonnage = todaySession.exercises.values.sumOf { ex ->
                ex.sets.sumOf { st -> st.weightDouble() * st.repsDouble() }
            }
            val setsDone = todaySession.exercises.values.sumOf { ex -> ex.sets.count { it.repsDouble() > 0.0 } }
            if (setsDone == 0) {
                null
            } else {
                var prCount = 0
                todaySession.exercises.values.forEach { ex ->
                    val curE1 = ex.topE1Rm()
                    if (curE1 > 0.0) {
                        val prevBest = allSessions.values
                            .filter { it.date != today }
                            .flatMap { it.exercises.values }
                            .filter { it.name == ex.name }
                            .maxOfOrNull { it.topE1Rm() } ?: 0.0
                        if (prevBest > 0.0 && curE1 > prevBest + 0.05) prCount++
                    }
                }
                Triple(tonnage, setsDone, prCount)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (pct >= 100) semantic.ok.copy(alpha = 0.45f) else subtleCardBorderColor()
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workout_session_progress_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "جلسة $dayName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatCounterLtr("$completedSets", "$totalPlannedSets", isolateLtr("Sets")),
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Inline compact workout elapsed timer control
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.testTag("workout_session_timer_bar")
                ) {
                    if (workoutTimer.hasStarted || workoutTimer.elapsedSeconds > 0L) {
                        IconButton(
                            onClick = onResetTimer,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("workout_timer_reset")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "تصفير تايمر التمرينة",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    val timerInteraction = remember { MutableInteractionSource() }
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = if (workoutTimer.isRunning) {
                            semantic.ok.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        },
                        modifier = Modifier
                            .tactilePressScale(timerInteraction, pressedScale = 0.95f)
                            .clip(RoundedCornerShape(99.dp))
                            .clickable(
                                interactionSource = timerInteraction,
                                indication = null,
                                onClick = onToggleTimer
                            )
                            .testTag("workout_timer_toggle")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = if (workoutTimer.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = if (workoutTimer.isRunning) semantic.ok else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (workoutTimer.hasStarted || workoutTimer.elapsedSeconds > 0L) {
                                    isolateLtr(formatWorkoutElapsed(workoutTimer.elapsedSeconds))
                                } else {
                                    "ابدأ الوقت"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = if (workoutTimer.isRunning) timerAccent else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("workout_session_timer_value")
                            )
                        }
                    }
                }
            }

            MetricProgressBar(
                current = completedSets.toDouble(),
                target = totalPlannedSets.toDouble(),
                barColor = progressAccent,
                height = 5.dp
            )

            // Quiet inline telemetry strip (only appears once at least 1 set is logged)
            if (liveStats != null) {
                val (tonnage, _, prCount) = liveStats
                val tonnageLabel = if (tonnage >= 1000.0) {
                    "${isolateLtr(String.format("%.2f", tonnage / 1000.0))} طن"
                } else {
                    "${isolateLtr("${tonnage.roundToInt()}")} كجم"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workout_session_summary_card"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "الحجم الكلي: $tonnageLabel · ${isolateLtr("$pct%")}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (prCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = semantic.ok,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = isolateLtr("$prCount PR"),
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.ExtraBold,
                                color = semantic.ok
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ultra-slim collapsible General Warm-Up strip so exercises appear near the top of the screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactWarmupStrip(onOpenYoutube: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "warmup_chevron"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("general_warmup_section")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SelfImprovement,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "التسخين العام",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "· ${isolateLtr("5")} تمارين",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(200)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
            ) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WorkoutDefaults.GENERAL_WARMUPS.forEach { w ->
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .clickable { onOpenYoutube(w.youtubeId) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = stringResource(R.string.cd_youtube),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(w.title, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                    Text(
                        text = "${isolateLtr("10")} عدات لكل تمرين بهدوء من غير إجهاد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (allDone) semantic.ok.copy(alpha = 0.5f) else subtleCardBorderColor()
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    text = formatCounterLtr("$completedSets", "$totalPlannedSets", isolateLtr("Sets")),
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

/**
 * Streamlined, low-friction gym ExerciseCard:
 * - Calm single-surface container (zero nested card-within-card borders)
 * - Clean header with rep-range chip + single `⋯` overflow menu for secondary tools
 * - Unobtrusive single-line previous session reference (`آخر مرة`)
 * - Crisp tabular set rows with compact numeric inputs, `SetOverloadCueChip` (`↑ +وزن`, `= ثبّت`, `↓ -وزن`), and 1-tap `✓`
 */
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
    val chosenYoutubeId = WorkoutDefaults.YOUTUBE_IDS[chosenName]

    val hasPlateau = remember(past) {
        if (past.size >= 4) {
            val e1s = past.map { it.topE1Rm() }
            val last3Max = e1s.takeLast(3).maxOrNull() ?: 0.0
            last3Max <= e1s[e1s.size - 4]
        } else false
    }

    var overflowExpanded by remember { mutableStateOf(false) }
    var activePanel by remember(chosenName) { mutableStateOf<ExerciseInlinePanel?>(null) }
    var warmupMessage by remember(chosenName) { mutableStateOf<String?>(null) }
    val saveInteraction = remember { MutableInteractionSource() }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSaved) semantic.ok.copy(alpha = 0.45f) else subtleCardBorderColor()
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .testTag("exercise_card_${dayIndex}_$exIndex")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // 1. Crisp Header Row: Silhouette + Name + Rep Range Pill + Single `⋯` Overflow Menu
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
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isSaved) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "تم التسجيل",
                                tint = semantic.ok,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (isLivePr) {
                            PersonalRecordBadge()
                        }
                    }
                    Text(
                        text = "$muscleLabel · ${isolateLtr("${spec.workingSets} Sets")} · ${isolateLtr("Rest")} $restSummary",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Calm tonal rep-range badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = isolateLtr("${spec.minReps}–${spec.maxReps}"),
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                // Single compact overflow menu grouping البدائل / نصيحة / أدوات / سجل
                Box {
                    IconButton(
                        onClick = { overflowExpanded = true },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("exercise_more_menu_${dayIndex}_$exIndex")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "خيارات التمرين",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = overflowExpanded,
                        onDismissRequest = { overflowExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        if (alternatives.size > 1) {
                            DropdownMenuItem(
                                text = { Text("البدائل وتبديل التمرين") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    overflowExpanded = false
                                    activePanel = if (activePanel == ExerciseInlinePanel.ALTERNATIVES) null else ExerciseInlinePanel.ALTERNATIVES
                                },
                                modifier = Modifier.testTag("toggle_alternatives_${dayIndex}_$exIndex")
                            )
                        }
                        if (chosenYoutubeId != null) {
                            DropdownMenuItem(
                                text = { Text("شرح الفيديو (${isolateLtr("YouTube")})") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    overflowExpanded = false
                                    onOpenYoutube(chosenYoutubeId)
                                }
                            )
                        }
                        if (spec.note.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("نصيحة التكنيك") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Lightbulb,
                                        contentDescription = null,
                                        tint = semantic.warn,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    overflowExpanded = false
                                    activePanel = if (activePanel == ExerciseInlinePanel.TIP) null else ExerciseInlinePanel.TIP
                                },
                                modifier = Modifier.testTag("exercise_tip_chip_${dayIndex}_$exIndex")
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("حاسبة التسخين") },
                            leadingIcon = {
                                Icon(Icons.Rounded.SelfImprovement, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                overflowExpanded = false
                                val baseW = sets.firstOrNull()?.weightDouble()?.takeIf { it > 0.0 }
                                    ?: lastSession?.topWeight()
                                    ?: 0.0
                                warmupMessage = WorkoutDefaults.warmUpAdvice(spec.warmupSets, baseW)
                                activePanel = ExerciseInlinePanel.WARMUP
                            },
                            modifier = Modifier.testTag("warmup_calc_${dayIndex}_$exIndex")
                        )
                        DropdownMenuItem(
                            text = { Text("حاسبة الطارات (${isolateLtr("Plates")})") },
                            leadingIcon = {
                                Icon(Icons.Rounded.FitnessCenter, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                overflowExpanded = false
                                val baseW = sets.firstOrNull()?.weightDouble()?.takeIf { it > 0.0 }
                                    ?: lastSession?.topWeight()
                                    ?: 60.0
                                onOpenPlateCalc(baseW)
                            },
                            modifier = Modifier.testTag("plate_calc_${dayIndex}_$exIndex")
                        )
                        DropdownMenuItem(
                            text = { Text("بدء مؤقت الراحة (${isolateLtr("Rest")})") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                overflowExpanded = false
                                onRepsDone()
                            },
                            modifier = Modifier.testTag("start_rest_${dayIndex}_$exIndex")
                        )
                        if (history.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("سجل التمرين وتقدير ${isolateLtr("1RM")}") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    overflowExpanded = false
                                    activePanel = if (activePanel == ExerciseInlinePanel.HISTORY) null else ExerciseInlinePanel.HISTORY
                                }
                            )
                        }
                    }
                }
            }

            // Quiet plateau cue (only if detected)
            if (hasPlateau) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = semantic.warn,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "الوزن ثابت من ${isolateLtr("3")} جلسات — جرّب تزوّد عدة أو وزن تدريجياً.",
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.warn
                    )
                }
            }

            // 2. On-demand inline drawer (only visible when picked from `⋯` menu)
            AnimatedVisibility(
                visible = activePanel != null,
                enter = expandVertically(tween(200)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(160)) + fadeOut(tween(130))
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when (activePanel) {
                                        ExerciseInlinePanel.ALTERNATIVES -> "البدائل"
                                        ExerciseInlinePanel.TIP -> "نصيحة التكنيك"
                                        ExerciseInlinePanel.WARMUP -> "حاسبة التسخين"
                                        ExerciseInlinePanel.HISTORY -> "تطور التمرين"
                                        null -> ""
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { activePanel = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "إغلاق",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            when (activePanel) {
                                ExerciseInlinePanel.ALTERNATIVES -> {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        alternatives.forEach { option ->
                                            val isSelected = option == chosenName
                                            val ytId = WorkoutDefaults.YOUTUBE_IDS[option]
                                            Surface(
                                                shape = RoundedCornerShape(99.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                modifier = Modifier
                                                    .height(34.dp)
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
                                                            style = MaterialTheme.typography.labelSmall,
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
                                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                                )
                                                                .clickable { onOpenYoutube(ytId) }
                                                                .padding(horizontal = 9.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.PlayArrow,
                                                                contentDescription = "شرح $option",
                                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                ExerciseInlinePanel.TIP -> {
                                    Text(
                                        text = spec.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                ExerciseInlinePanel.WARMUP -> {
                                    Text(
                                        text = warmupMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                ExerciseInlinePanel.HISTORY -> {
                                    val bestW = history.maxOfOrNull { it.topWeight() } ?: 0.0
                                    val bestE1 = history.maxOfOrNull { it.topE1Rm() } ?: 0.0
                                    Text(
                                        text = "أعلى وزن: ${isolateLtr(String.format("%.1f", bestW))} كجم · ${isolateLtr("1RM")}: ${isolateLtr("${bestE1.roundToInt()}")} كجم (${isolateLtr("${history.size}")} جلسة)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (history.size >= 2) {
                                        DualLineProgressChart(
                                            primarySeries = history.map { it.topWeight() },
                                            secondarySeries = history.map { it.topE1Rm() },
                                            unitLabel = "كجم",
                                            legendText = "أعلى وزن (أزرق) وتقدير 1RM (متقطع)"
                                        )
                                    }
                                }

                                null -> Unit
                            }
                        }
                    }
                }
            }

            // 3. Unobtrusive Single-Line Previous Session Strip (`آخر مرة` — no nested bordered box!)
            if (lastSession != null) {
                Spacer(Modifier.height(10.dp))
                val summaryLtr = isolateLtr(
                    lastSession.sets.joinToString(" • ") { s ->
                        "${s.weight.ifBlank { "–" }}×${s.reps.ifBlank { "–" }}"
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "آخر مرة: $summaryLtr",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = if (progressionAdvice?.readyToIncrease == true) {
                            semantic.ok.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        },
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
                            text = if (progressionAdvice?.readyToIncrease == true) "↑ +وزن" else "نسخ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (progressionAdvice?.readyToIncrease == true) semantic.ok else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 4. Clean Column Header + Crisp Tabular Set Rows
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(18.dp)
                )
                Text(
                    text = "كجم",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = isolateLtr("Reps"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "الحالة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(62.dp)
                )
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(44.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sets.forEachIndexed { setIdx, set ->
                    val repHint = WorkoutDefaults.repHint(set.reps, spec.minReps, spec.maxReps)
                    val setGoal = remember(spec, lastSession, setIdx, progressionAdvice) {
                        computeSetTargetGoal(spec, lastSession, setIdx, progressionAdvice)
                    }
                    val prevSet = lastSession?.sets?.getOrNull(setIdx)
                    val suggestedWText = setGoal.suggestedWeightKg?.let { w ->
                        if (w % 1.0 == 0.0) w.toInt().toString() else String.format("%.1f", w)
                    }
                    val weightPlaceholder = prevSet?.weight?.takeIf { it.isNotBlank() } ?: suggestedWText ?: "0"
                    val repsPlaceholder = prevSet?.reps?.takeIf { it.isNotBlank() } ?: "${spec.minReps}"
                    val isCompleted = set.repsInt() > 0

                    val rowBgColor by animateColorAsState(
                        targetValue = if (isCompleted) semantic.ok.copy(alpha = 0.10f) else Color.Transparent,
                        animationSpec = tween(durationMillis = 220),
                        label = "set_row_bg"
                    )
                    val checkPopScale by animateFloatAsState(
                        targetValue = if (isCompleted) 1.04f else 1f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                        label = "set_check_pop"
                    )
                    val checkInteraction = remember { MutableInteractionSource() }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(rowBgColor)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = isolateLtr("${setIdx + 1}"),
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) semantic.ok else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(18.dp),
                            textAlign = TextAlign.Center
                        )

                        CompactSetNumberInput(
                            value = set.weight,
                            placeholder = weightPlaceholder,
                            imeAction = ImeAction.Next,
                            onValueChange = { onSetChanged(setIdx, it, null) },
                            onDone = {},
                            modifier = Modifier
                                .weight(1f)
                                .testTag("set_weight_${dayIndex}_${exIndex}_$setIdx")
                        )

                        CompactSetNumberInput(
                            value = set.reps,
                            placeholder = repsPlaceholder,
                            imeAction = ImeAction.Done,
                            onValueChange = { onSetChanged(setIdx, null, it) },
                            onDone = {
                                focusManager.clearFocus()
                                if (set.reps.isNotBlank()) onRepsDone()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("set_reps_${dayIndex}_${exIndex}_$setIdx")
                        )

                        SetOverloadCueChip(
                            goal = setGoal,
                            repHint = repHint,
                            testTag = "set_goal_icon_${dayIndex}_${exIndex}_$setIdx",
                            onApplyWeight = { wStr -> onSetChanged(setIdx, wStr, null) }
                        )

                        // Crisp 1-tap completion toggle (48dp touch area via outer Box)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .tactilePressScale(checkInteraction, pressedScale = 0.9f)
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
                                    val fallbackW = if (weightPlaceholder != "0") weightPlaceholder else ""
                                    onQuickCompleteSet(setIdx, fallbackW, repsPlaceholder)
                                }
                                .testTag("quick_complete_set_${dayIndex}_${exIndex}_$setIdx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isCompleted) semantic.ok else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "إكمال المجموعة ${setIdx + 1}",
                                        tint = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Calm, low-noise tonal Save / Update button
            Spacer(Modifier.height(10.dp))
            FilledTonalButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSaveExercise()
                },
                interactionSource = saveInteraction,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isSaved) {
                        semantic.ok.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    },
                    contentColor = if (isSaved) semantic.ok else MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .tactilePressScale(saveInteraction, pressedScale = 0.98f)
                    .testTag("finish_exercise_${dayIndex}_$exIndex")
            ) {
                Text(
                    text = if (isSaved) "محفوظ ✓ · تحديث" else "حفظ التمرين",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Sleek, low-height numeric input pill for gym set logging.
 * Replaces bulky 56dp OutlinedTextField boxes with a crisp 40dp centered tabular pill.
 */
@Composable
private fun CompactSetNumberInput(
    value: String,
    placeholder: String,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.Transparent,
        animationSpec = tween(150),
        label = "set_input_border"
    )

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFeatureSettings = "tnum",
                                textAlign = TextAlign.Center
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f),
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
        )
    }
}
