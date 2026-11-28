package com.mutabei.workout.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mutabei.workout.data.model.ExerciseSpec
import com.mutabei.workout.data.model.WorkoutDefaults
import com.mutabei.workout.data.model.shiftIsoDate
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.AppDataSnapshot
import com.mutabei.workout.ui.viewmodel.MainViewModel
import kotlin.math.max
import kotlin.math.roundToInt

enum class WorkoutSubView {
    SESSION,
    RECOVERY_LOG
}

/**
 * Clean 2-tab pill switcher inside the Workout tab separating active training
 * (`الجلسة والتمارين`) from recovery & long-term logs (`الاستشفاء والسجل`).
 */
@Composable
fun WorkoutSubViewSwitcher(
    selected: WorkoutSubView,
    onSelect: (WorkoutSubView) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("workout_subview_switcher")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Triple(WorkoutSubView.SESSION, "الجلسة والتمارين", Icons.Rounded.FitnessCenter),
                Triple(WorkoutSubView.RECOVERY_LOG, "الاستشفاء والسجل", Icons.Rounded.Insights)
            ).forEach { (tab, label, icon) ->
                val isSelected = selected == tab
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    animationSpec = tween(200),
                    label = "subview_bg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "subview_content"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "subview_icon"
                )
                val interaction = remember { MutableInteractionSource() }

                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = bgColor,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .tactilePressScale(interaction, pressedScale = 0.97f)
                        .clip(RoundedCornerShape(11.dp))
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(tab)
                            }
                        }
                        .testTag(
                            if (tab == WorkoutSubView.SESSION) "workout_subview_session"
                            else "workout_subview_recovery"
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Weekly Muscle Sets summary accordion (Last 7 Days) with strict LTR numeric ratio isolation.
 */
@Composable
fun WeeklyMuscleSetsCard(snapshot: AppDataSnapshot) {
    val semantic = LocalSemanticColors.current
    val today = todayIso()
    val start7 = shiftIsoDate(today, -6)
    val done = remember(snapshot.sessions, start7, today) {
        val map = mutableMapOf<String, Int>()
        snapshot.sessions.values.forEach { s ->
            if (s.date in start7..today) {
                s.exercises.values.forEach { ex ->
                    val g = WorkoutDefaults.detectMuscleGroup(ex.name)
                    val count = ex.sets.count { it.repsInt() > 0 }
                    map[g] = (map[g] ?: 0) + count
                }
            }
        }
        map
    }
    val planned = remember(snapshot.config.splitDays) {
        val map = mutableMapOf<String, Int>()
        snapshot.config.effectiveSplit().forEach { day ->
            day.exercises.forEach { ex ->
                val g = WorkoutDefaults.detectMuscleGroup(ex.name)
                map[g] = (map[g] ?: 0) + ex.workingSets
            }
        }
        map
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
            text = "بيحسب المجموعات اللي سجّلت فيها عدات بس. \"دراع\" بتشمل البايسبس والترايسبس. التوصيات العامة للتضخيم حوالي ${isolateLtr("10–20")} مجموعة أسبوعيًا لكل عضلة.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Consolidated Progress & Exercise History accordion shown inside the Recovery & Log sub-view
 * so the user can inspect progression charts and past logs for every exercise in the selected split.
 */
@Composable
fun WorkoutSplitProgressHistoryCard(
    dayName: String,
    dayIndex: Int,
    exercises: List<ExerciseSpec>,
    snapshot: AppDataSnapshot,
    viewModel: MainViewModel
) {
    val exerciseNames = remember(dayIndex, exercises, snapshot.config.alternatives) {
        exercises.mapIndexed { idx, spec ->
            val altKey = "$dayIndex|$idx"
            snapshot.config.alternatives[altKey]?.ifBlank { spec.name } ?: spec.name
        }
    }
    var selectedExIdx by remember(dayIndex, exerciseNames.size) { mutableIntStateOf(0) }
    val safeExIdx = selectedExIdx.coerceIn(0, (exerciseNames.size - 1).coerceAtLeast(0))
    val activeName = exerciseNames.getOrNull(safeExIdx).orEmpty()

    val history = remember(snapshot.sessions, activeName) {
        if (activeName.isBlank()) emptyList() else viewModel.exerciseHistory(snapshot.sessions, activeName)
    }
    val totalLoggedCount = remember(snapshot.sessions, exerciseNames) {
        exerciseNames.sumOf { name -> viewModel.exerciseHistory(snapshot.sessions, name).size }
    }

    ExpandableSectionCard(
        title = "التطور والسجل",
        subtitleEn = "Progress & Log · $dayName",
        statusBadgeText = "${isolateLtr("$totalLoggedCount")} سجل",
        statusBadgeColor = MaterialTheme.colorScheme.primary,
        leadingIcon = Icons.Rounded.History,
        testTag = "split_progress_history_card"
    ) {
        if (exerciseNames.isEmpty()) {
            Text(
                text = "لا توجد تمارين في هذه الجلسة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ExpandableSectionCard
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            exerciseNames.forEachIndexed { idx, name ->
                val isSelected = idx == safeExIdx
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedExIdx = idx },
                    label = {
                        Text(
                            text = isolateLtr(name),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = subtleCardBorderColor()
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (history.isEmpty()) {
            Text(
                text = "لسه مفيش جلسات متسجلة لتمرين ${isolateLtr(activeName)}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val bestW = history.maxOfOrNull { it.topWeight() } ?: 0.0
            val bestE1 = history.maxOfOrNull { it.topE1Rm() } ?: 0.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, subtleCardBorderColor()),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = "أعلى وزن",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${isolateLtr(String.format("%.1f", bestW))} كجم",
                            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, subtleCardBorderColor()),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = "أفضل تقدير ${isolateLtr("1RM")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${isolateLtr("${bestE1.roundToInt()}")} كجم",
                            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            DualLineProgressChart(
                primarySeries = history.map { it.topWeight() },
                secondarySeries = history.map { it.topE1Rm() },
                unitLabel = "كجم",
                legendText = "الخط الأزرق أعلى وزن، المتقطع تقدير أقصى وزن"
            )
            Spacer(Modifier.height(8.dp))
            history.reversed().take(10).forEach { pt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = isolateLtr(pt.date),
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = isolateLtr(
                            pt.sets.joinToString(" • ") {
                                "${it.weight.ifBlank { "–" }}×${it.reps.ifBlank { "–" }}"
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = subtleCardBorderColor())
            }
        }
    }
}
