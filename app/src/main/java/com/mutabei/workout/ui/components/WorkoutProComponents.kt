package com.mutabei.workout.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mutabei.workout.data.model.ExerciseSpec
import com.mutabei.workout.data.model.SetEntry
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.ExerciseHistoryPoint
import com.mutabei.workout.ui.viewmodel.WorkoutSessionTimerState
import kotlin.math.floor
import kotlin.math.roundToInt

data class DoubleProgressionAdvice(
    val readyToIncrease: Boolean,
    val suggestedWeightKg: Double?,
    val message: String
)

fun computeDoubleProgression(
    spec: ExerciseSpec,
    lastSession: ExerciseHistoryPoint?
): DoubleProgressionAdvice? {
    if (lastSession == null) return null
    val validSets = lastSession.sets.filter { it.weightDouble() > 0.0 && it.repsDouble() > 0.0 }
    if (validSets.isEmpty()) return null

    val topWeight = validSets.maxOf { it.weightDouble() }
    val allHitMax = validSets.size >= spec.workingSets && validSets.all { it.repsDouble() >= spec.maxReps }
    val allBelowMin = validSets.all { it.repsDouble() < spec.minReps }

    return when {
        allHitMax -> {
            val step = if (topWeight >= 20.0) 2.5 else 1.25
            val nextW = ((topWeight + step) * 100.0).roundToInt() / 100.0
            val wStr = if (nextW % 1.0 == 0.0) nextW.toInt().toString() else nextW.toString()
            DoubleProgressionAdvice(
                readyToIncrease = true,
                suggestedWeightKg = nextW,
                message = "كسرت الرينج المرة اللي فاتت — ارفع الوزن لـ $wStr كجم النهارده!"
            )
        }

        allBelowMin -> {
            val step = if (topWeight >= 20.0) 2.5 else 1.25
            val lowerW = (topWeight - step).coerceAtLeast(2.5)
            val wStr = if (lowerW % 1.0 == 0.0) lowerW.toInt().toString() else lowerW.toString()
            DoubleProgressionAdvice(
                readyToIncrease = false,
                suggestedWeightKg = lowerW,
                message = "العدات كانت تحت الرينج — ثبّت التكنيك أو خفّف لـ $wStr كجم وجيب ${spec.minReps}–${spec.maxReps} عدة."
            )
        }

        else -> {
            val topStr = if (topWeight % 1.0 == 0.0) topWeight.toInt().toString() else topWeight.toString()
            DoubleProgressionAdvice(
                readyToIncrease = false,
                suggestedWeightKg = topWeight,
                message = "هدف النهارده: ثبّت $topStr كجم وزوّد عدة على الأقل فوق آخر جلسة."
            )
        }
    }
}

enum class SetGoalKind { INCREASE, PROGRESS, HOLD_OR_FIX, FIRST_SESSION }

data class SetTargetGoal(
    val setNumber: Int,
    val kind: SetGoalKind,
    val title: String,
    val targetLine: String,
    val detailLine: String,
    val suggestedWeightKg: Double?
)

private fun fmtWeightKg(w: Double): String =
    if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()

fun computeSetTargetGoal(
    spec: ExerciseSpec,
    lastSession: ExerciseHistoryPoint?,
    setIndex: Int,
    overallAdvice: DoubleProgressionAdvice?
): SetTargetGoal {
    val setNum = setIndex + 1
    val prevSet = lastSession?.sets?.getOrNull(setIndex)
        ?.takeIf { it.weightDouble() > 0.0 && it.repsDouble() > 0.0 }
        ?: lastSession?.sets?.lastOrNull { it.weightDouble() > 0.0 && it.repsDouble() > 0.0 }

    if (prevSet == null) {
        return SetTargetGoal(
            setNumber = setNum,
            kind = SetGoalKind.FIRST_SESSION,
            title = "هدف المجموعة $setNum",
            targetLine = "${spec.minReps}–${spec.maxReps} عدات",
            detailLine = "أول مرة تسجّل المجموعة دي — اختار وزن مناسب بتكنيك مضبوط وجيب ${spec.minReps}–${spec.maxReps} عدات.",
            suggestedWeightKg = null
        )
    }

    val prevW = prevSet.weightDouble()
    val prevR = prevSet.repsInt()
    val step = if (prevW >= 20.0) 2.5 else 1.25
    val nextW = ((prevW + step) * 100.0).roundToInt() / 100.0
    val lowerW = (prevW - step).coerceAtLeast(2.5)
    val prevLabel = "${fmtWeightKg(prevW)} كجم × $prevR"

    return when {
        overallAdvice?.readyToIncrease == true -> {
            val targetW = overallAdvice.suggestedWeightKg ?: nextW
            SetTargetGoal(
                setNumber = setNum,
                kind = SetGoalKind.INCREASE,
                title = "هدف المجموعة $setNum",
                targetLine = "${fmtWeightKg(targetW)} كجم × ${spec.minReps}–${spec.maxReps} عدات",
                detailLine = "كسرت الرينج المرة اللي فاتت ($prevLabel) — ارفع الوزن لـ ${fmtWeightKg(targetW)} كجم النهارده!",
                suggestedWeightKg = targetW
            )
        }

        prevR >= spec.maxReps -> {
            SetTargetGoal(
                setNumber = setNum,
                kind = SetGoalKind.INCREASE,
                title = "هدف المجموعة $setNum",
                targetLine = "${fmtWeightKg(nextW)} كجم × ${spec.minReps}–${spec.maxReps} أو ${fmtWeightKg(prevW)} × ${spec.maxReps}",
                detailLine = "المجموعة دي وصلت لأقصى عدات ($prevLabel) — جرّب ترفع لـ ${fmtWeightKg(nextW)} كجم أو حافظ على ${spec.maxReps} عدات.",
                suggestedWeightKg = nextW
            )
        }

        prevR < spec.minReps -> {
            SetTargetGoal(
                setNumber = setNum,
                kind = SetGoalKind.HOLD_OR_FIX,
                title = "هدف المجموعة $setNum",
                targetLine = "${fmtWeightKg(prevW)} كجم × ${spec.minReps} عدات (أو ${fmtWeightKg(lowerW)} كجم)",
                detailLine = "العدات كانت تحت الرينج ($prevLabel) — ثبّت التكنيك أو خفّف لـ ${fmtWeightKg(lowerW)} كجم وجيب ${spec.minReps}–${spec.maxReps}.",
                suggestedWeightKg = lowerW
            )
        }

        else -> {
            val nextR = (prevR + 1).coerceAtMost(spec.maxReps)
            SetTargetGoal(
                setNumber = setNum,
                kind = SetGoalKind.PROGRESS,
                title = "هدف المجموعة $setNum",
                targetLine = "${fmtWeightKg(prevW)} كجم × $nextR عدات",
                detailLine = "ثبّت ${fmtWeightKg(prevW)} كجم وزوّد عدة على الأقل عن آخر مرة ($prevLabel).",
                suggestedWeightKg = prevW
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetGoalTargetIcon(
    goal: SetTargetGoal,
    currentSet: SetEntry,
    minReps: Int,
    testTag: String,
    onApplyWeight: (String) -> Unit,
    onShortTapHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }

    val achievedToday = remember(currentSet, goal, minReps) {
        val w = currentSet.weightDouble()
        val r = currentSet.repsInt()
        val targetW = goal.suggestedWeightKg
        if (w <= 0.0 || r <= 0) false
        else if (targetW != null) w >= targetW && r >= minReps
        else r >= minReps
    }

    val accent = when {
        achievedToday -> semantic.ok
        goal.kind == SetGoalKind.INCREASE -> semantic.ok
        goal.kind == SetGoalKind.HOLD_OR_FIX -> semantic.warn
        goal.kind == SetGoalKind.PROGRESS -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = accent.copy(alpha = if (achievedToday) 0.22f else 0.12f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = onShortTapHint,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        expanded = true
                    }
                )
                .semantics { contentDescription = "اضغط مطولاً لعرض ${goal.title}" }
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (goal.kind == SetGoalKind.INCREASE && !achievedToday) {
                        Icons.Rounded.ArrowUpward
                    } else {
                        Icons.Rounded.TrackChanges
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(min = 230.dp, max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrackChanges,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = accent.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = goal.targetLine,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Text(
                    text = goal.detailLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (goal.suggestedWeightKg != null) {
                    val wStr = fmtWeightKg(goal.suggestedWeightKg)
                    OutlinedButton(
                        onClick = {
                            onApplyWeight(wStr)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "طبّق $wStr كجم في المجموعة ${goal.setNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun formatWorkoutElapsed(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val h = safe / 3600L
    val m = (safe % 3600L) / 60L
    val s = safe % 60L
    return if (h > 0L) {
        "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    } else {
        "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }
}

@Composable
fun WorkoutSessionTimerBar(
    state: WorkoutSessionTimerState,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val accent = when {
        state.isRunning -> semantic.ok
        state.hasStarted -> semantic.warn
        else -> MaterialTheme.colorScheme.primary
    }
    val statusText = when {
        state.isRunning -> "وقت التمرينة · شغّال"
        state.hasStarted -> "وقت التمرينة · متوقف مؤقتًا"
        else -> "تايمر وقت التمرينة"
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (state.isRunning) {
                semantic.ok.copy(alpha = 0.09f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (state.isRunning) 1.5.dp else 1.dp,
            color = if (state.isRunning) semantic.ok.copy(alpha = 0.65f) else subtleCardBorderColor()
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("workout_session_timer_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.16f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatWorkoutElapsed(state.elapsedSeconds),
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (state.isRunning) accent else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("workout_session_timer_value")
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (state.hasStarted || state.elapsedSeconds > 0L) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("workout_timer_reset")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "تصفير تايمر التمرينة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                val toggleInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = onToggle,
                    interactionSource = toggleInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isRunning) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        contentColor = if (state.isRunning) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        }
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .tactilePressScale(toggleInteraction)
                        .testTag("workout_timer_toggle")
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = when {
                            state.isRunning -> "إيقاف"
                            state.hasStarted -> "استكمال"
                            else -> "ابدأ"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun isLivePersonalRecord(
    currentSets: List<SetEntry>,
    pastHistory: List<ExerciseHistoryPoint>
): Boolean {
    if (pastHistory.isEmpty()) return false
    val prevBest = pastHistory.maxOfOrNull { it.topE1Rm() } ?: 0.0
    if (prevBest <= 0.0) return false
    val currentBest = currentSets.maxOfOrNull { it.estimated1Rm() } ?: 0.0
    return currentBest > prevBest + 0.05
}

@Composable
fun PersonalRecordBadge(modifier: Modifier = Modifier) {
    val semantic = LocalSemanticColors.current
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = semantic.ok.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, semantic.ok),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = "رقم قياسي جديد",
                tint = semantic.ok,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "PR جديد!",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = semantic.ok
            )
        }
    }
}

@Composable
fun DoubleProgressionBanner(
    advice: DoubleProgressionAdvice,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val accent = if (advice.readyToIncrease) semantic.ok else MaterialTheme.colorScheme.primary
    Surface(
        shape = MaterialTheme.shapes.small,
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (advice.readyToIncrease) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = advice.message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }
    }
}

private data class PlateSpec(val weight: Double, val color: Color, val heightRatio: Float)

private val STANDARD_PLATES = listOf(
    PlateSpec(25.0, Color(0xFFE53935), 1.00f),
    PlateSpec(20.0, Color(0xFF1E88E5), 0.96f),
    PlateSpec(15.0, Color(0xFFFDD835), 0.86f),
    PlateSpec(10.0, Color(0xFF43A047), 0.76f),
    PlateSpec(5.0, Color(0xFFECEFF1), 0.62f),
    PlateSpec(2.5, Color(0xFF78909C), 0.50f),
    PlateSpec(1.25, Color(0xFFB0BEC5), 0.40f)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlateCalculatorDialog(
    exerciseName: String,
    initialWeightKg: Double,
    onDismiss: () -> Unit
) {
    var weightText by remember(initialWeightKg) {
        val initW = if (initialWeightKg > 0.0) initialWeightKg else 60.0
        mutableStateOf(if (initW % 1.0 == 0.0) initW.toInt().toString() else initW.toString())
    }
    var barWeight by remember { mutableDoubleStateOf(20.0) }

    val targetWeight = (weightText.replace(',', '.').toDoubleOrNull() ?: 0.0).coerceIn(0.0, 500.0)
    val loadPerSide = ((targetWeight - barWeight) / 2.0).coerceAtLeast(0.0)

    val platesPerSide = remember(loadPerSide) {
        var rem = loadPerSide + 0.001
        val out = mutableListOf<PlateSpec>()
        for (p in STANDARD_PLATES) {
            val count = floor(rem / p.weight).toInt()
            if (count > 0) {
                repeat(count.coerceAtMost(10)) { out.add(p) }
                rem -= count * p.weight
            }
        }
        out
    }
    val actualTotal = barWeight + platesPerSide.sumOf { it.weight } * 2.0
    val remainder = (targetWeight - actualTotal).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("حاسبة أوزان البار", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (exerciseName.isNotBlank()) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("الوزن الكلي المستهدف (كجم)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plate_calc_weight_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(-5.0, -2.5, 2.5, 5.0).forEach { delta ->
                        val sign = if (delta > 0) "+" else ""
                        OutlinedButton(
                            onClick = {
                                val next = (targetWeight + delta).coerceAtLeast(0.0)
                                weightText = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text("$sign${if (delta % 1.0 == 0.0) delta.toInt() else delta}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Text("وزن البار:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(20.0 to "أولمبي 20", 15.0 to "15 كجم", 10.0 to "EZ 10", 0.0 to "بدون بار (0)").forEach { (w, label) ->
                        FilterChip(
                            selected = barWeight == w,
                            onClick = { barWeight = w },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Visual Barbell Canvas
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val barColor = MaterialTheme.colorScheme.onSurfaceVariant
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(86.dp)
                        ) {
                            val cy = size.height / 2f
                            val barH = 10.dp.toPx()
                            // Main shaft
                            drawRoundRect(
                                color = barColor.copy(alpha = 0.6f),
                                topLeft = Offset(8.dp.toPx(), cy - barH / 2f),
                                size = Size(size.width - 16.dp.toPx(), barH),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            val collarOffset = size.width * 0.24f
                            val plateW = 10.dp.toPx()
                            val gap = 2.dp.toPx()
                            val maxPlateH = size.height * 0.82f
                            // Collars
                            drawRect(
                                color = barColor,
                                topLeft = Offset(size.width / 2f - collarOffset - 5.dp.toPx(), cy - 12.dp.toPx()),
                                size = Size(5.dp.toPx(), 24.dp.toPx())
                            )
                            drawRect(
                                color = barColor,
                                topLeft = Offset(size.width / 2f + collarOffset, cy - 12.dp.toPx()),
                                size = Size(5.dp.toPx(), 24.dp.toPx())
                            )
                            // Plates on both sides
                            platesPerSide.take(7).forEachIndexed { idx, p ->
                                val ph = maxPlateH * p.heightRatio
                                val offsetFromCollar = 6.dp.toPx() + idx * (plateW + gap)
                                // Right side
                                val rx = size.width / 2f + collarOffset + offsetFromCollar
                                if (rx + plateW < size.width - 4.dp.toPx()) {
                                    drawRoundRect(
                                        color = p.color,
                                        topLeft = Offset(rx, cy - ph / 2f),
                                        size = Size(plateW, ph),
                                        cornerRadius = CornerRadius(3f, 3f)
                                    )
                                }
                                // Left side
                                val lx = size.width / 2f - collarOffset - offsetFromCollar - plateW
                                if (lx > 4.dp.toPx()) {
                                    drawRoundRect(
                                        color = p.color,
                                        topLeft = Offset(lx, cy - ph / 2f),
                                        size = Size(plateW, ph),
                                        cornerRadius = CornerRadius(3f, 3f)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        if (targetWeight < barWeight) {
                            Text(
                                text = "الوزن أقل من وزن البار (${barWeight.toInt()} كجم).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (platesPerSide.isEmpty()) {
                            Text(
                                text = "بار فاضي فقط (${barWeight.toInt()} كجم)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            val grouped = platesPerSide
                                .groupBy { it.weight }
                                .entries
                                .joinToString(" + ") { (w, list) ->
                                    val ws = if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()
                                    if (list.size > 1) "${list.size}×$ws" else ws
                                }
                            Text(
                                text = "كل ناحية: $grouped كجم",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "الإجمالي الفعلي: ${if (actualTotal % 1.0 == 0.0) actualTotal.toInt() else actualTotal} كجم" +
                                    if (remainder >= 0.25) " (فرق ${String.format("%.2f", remainder)} كجم)" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("تمام") }
        }
    )
}
