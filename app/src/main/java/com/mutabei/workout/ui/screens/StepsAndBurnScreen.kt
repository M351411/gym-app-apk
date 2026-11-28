package com.mutabei.workout.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mutabei.workout.R
import com.mutabei.workout.data.model.NutritionTargets
import com.mutabei.workout.data.model.StepBufferPreset
import com.mutabei.workout.data.model.StepsBurnEngine
import com.mutabei.workout.data.model.StepsBurnSummary
import com.mutabei.workout.data.model.shiftIsoDate
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.ui.components.isolateLtr
import com.mutabei.workout.ui.components.subtleCardBorderColor
import com.mutabei.workout.ui.components.tactilePressScale
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.AppDataSnapshot
import com.mutabei.workout.ui.viewmodel.MainTab
import com.mutabei.workout.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val StepsAccent = Color(0xFF14B8A6)

private fun fmtCommaInt(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.US).format(value)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepsAndBurnScreen(
    snapshot: AppDataSnapshot,
    selectedDate: String,
    viewModel: MainViewModel
) {
    val semantic = LocalSemanticColors.current
    val haptics = LocalHapticFeedback.current
    val today = todayIso()
    val rollingBudgetEnabled by viewModel.rollingCalorieBudgetEnabled.collectAsStateWithLifecycle()
    val workoutSyncedEnabled by viewModel.workoutSyncedNutritionEnabled.collectAsStateWithLifecycle()
    val defaultStepTarget by viewModel.defaultStepTarget.collectAsStateWithLifecycle()
    val defaultStepBuffer by viewModel.defaultStepBufferFactor.collectAsStateWithLifecycle()
    val dailyContext = remember(
        snapshot,
        selectedDate,
        rollingBudgetEnabled,
        workoutSyncedEnabled,
        defaultStepTarget,
        defaultStepBuffer
    ) {
        viewModel.computeDailyContext(
            snapshot = snapshot,
            date = selectedDate,
            rollingBudgetEnabled = rollingBudgetEnabled,
            workoutSyncedEnabled = workoutSyncedEnabled,
            defaultStepTarget = defaultStepTarget,
            defaultBufferFactor = defaultStepBuffer
        )
    }
    val summary = dailyContext.stepsBurn
    val adjustedTargets = dailyContext.adjustedTargets
    val consumedKcal = dailyContext.consumedKcal

    var showManualStepsDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var showExtraCardioDialog by remember { mutableStateOf(false) }
    var tuningExpanded by rememberSaveable { mutableStateOf(false) }

    if (showManualStepsDialog) {
        NumericInputDialog(
            title = "تعديل خطوات اليوم",
            label = "عدد الخطوات (Steps)",
            initialValue = if (summary.steps > 0) summary.steps.toString() else "",
            helperText = "اكتب الرقم من الساعة أو عدّاد الموبايل مباشرة.",
            onDismiss = { showManualStepsDialog = false },
            onConfirm = { raw ->
                raw.toIntOrNull()?.let { viewModel.setSteps(it) }
                showManualStepsDialog = false
            }
        )
    }

    if (showTargetDialog) {
        NumericInputDialog(
            title = "هدف الخطوات اليومي",
            label = "الهدف (مثلاً 10000)",
            initialValue = summary.stepTarget.toString(),
            helperText = "المدى المسموح من 1,000 إلى 50,000 خطوة.",
            onDismiss = { showTargetDialog = false },
            onConfirm = { raw ->
                raw.toIntOrNull()?.let { viewModel.setStepTarget(it) }
                showTargetDialog = false
            }
        )
    }

    if (showExtraCardioDialog) {
        NumericInputDialog(
            title = "كارديو / نشاط إضافي",
            label = "السعرات المحروقة (kcal)",
            initialValue = if (summary.extraCardioKcal > 0) summary.extraCardioKcal.toString() else "",
            helperText = "مثلاً عجلة أو سباحة مش محسوبة في الخطوات.",
            onDismiss = { showExtraCardioDialog = false },
            onConfirm = { raw ->
                val v = raw.toIntOrNull() ?: 0
                viewModel.setExtraCardioKcal(v)
                showExtraCardioDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Clean Date Switcher Strip
            item(key = "steps_date_strip") {
                val dateLabel = when (selectedDate) {
                    today -> "النهارده · ${isolateLtr(selectedDate)}"
                    shiftIsoDate(today, -1) -> "أمس · ${isolateLtr(selectedDate)}"
                    else -> isolateLtr(selectedDate)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { viewModel.shiftNutritionDate(-1) },
                        modifier = Modifier.testTag("steps_prev_day")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.cd_prev_day)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .clickable { viewModel.selectNutritionDate(today) }
                    ) {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.shiftNutritionDate(1) },
                        enabled = selectedDate < today,
                        modifier = Modifier.testTag("steps_next_day")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.cd_next_day),
                            tint = if (selectedDate < today) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            }
                        )
                    }
                }
            }

            // 2. Hero Steps Tracker Card
            item(key = "steps_hero_card") {
                StepsHeroTrackerCard(
                    summary = summary,
                    onAdjustSteps = { delta ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.adjustSteps(delta)
                    },
                    onOpenManualInput = { showManualStepsDialog = true },
                    onOpenTargetInput = { showTargetDialog = true }
                )
            }

            // 3. Calorie Translation Card (Base Target -> Active Burn -> 0%/50%/100% Buffer -> Adjusted Target)
            item(key = "calorie_translation_card") {
                CalorieTranslationCard(
                    summary = summary,
                    adjustedTargets = adjustedTargets,
                    consumedKcal = consumedKcal,
                    onSelectPreset = { preset ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setStepBufferFactor(preset.factor)
                    },
                    onOpenNutrition = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.selectTab(MainTab.NUTRITION)
                    }
                )
            }

            // 4. Optional Fine-Tuning Drawer (Extra Cardio + kcal/step rate — collapsed by default)
            item(key = "steps_tuning_drawer") {
                val chevron by animateFloatAsState(
                    targetValue = if (tuningExpanded) 180f else 0f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                    label = "steps_tuning_chevron"
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, subtleCardBorderColor()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tuningExpanded = !tuningExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("steps_tuning_toggle"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = StepsAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "كارديو إضافي ومعامل الحرق",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                val rateStr = isolateLtr(String.format(Locale.US, "%.3f kcal/step", summary.effectiveKcalPerStep))
                                val extraStr = if (summary.extraCardioKcal > 0) " · +${isolateLtr("${summary.extraCardioKcal}")} كارديو" else ""
                                Text(
                                    text = "$rateStr$extraStr",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(chevron)
                            )
                        }

                        AnimatedVisibility(
                            visible = tuningExpanded,
                            enter = expandVertically(tween(200)) + fadeIn(tween(180)),
                            exit = shrinkVertically(tween(160)) + fadeOut(tween(140))
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HorizontalDivider(color = subtleCardBorderColor())

                                // Extra Cardio / Activity kcal controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "كارديو إضافي (${isolateLtr("Active Burn")})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${isolateLtr("${summary.extraCardioKcal}")} سعر",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.Bold,
                                        color = StepsAccent
                                    )
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(100, 200, 300).forEach { addKcal ->
                                        QuickStepPill(
                                            label = isolateLtr("+$addKcal kcal"),
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.setExtraCardioKcal(summary.extraCardioKcal + addKcal)
                                            }
                                        )
                                    }
                                    QuickStepPill(
                                        label = "إدخال يدوي",
                                        onClick = { showExtraCardioDialog = true }
                                    )
                                    if (summary.extraCardioKcal > 0) {
                                        QuickStepPill(
                                            label = "تصفير",
                                            accent = semantic.low,
                                            onClick = { viewModel.setExtraCardioKcal(0) }
                                        )
                                    }
                                }

                                HorizontalDivider(color = subtleCardBorderColor())

                                // kcalPerStep rate selector
                                val autoRate = StepsBurnEngine.weightAwareKcalPerStep(summary.weightKg)
                                Text(
                                    text = "معامل حرق الخطوة (${isolateLtr("kcal/step")})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isAuto = !summary.isCustomKcalPerStep
                                    QuickStepPill(
                                        label = "تلقائي (${isolateLtr(String.format(Locale.US, "%.3f", autoRate))})",
                                        selected = isAuto,
                                        onClick = { viewModel.setKcalPerStepOverride(0.0) }
                                    )
                                    listOf(0.035, 0.04, 0.045, 0.05).forEach { rate ->
                                        val active = summary.isCustomKcalPerStep &&
                                            kotlin.math.abs(summary.effectiveKcalPerStep - rate) < 0.001
                                        QuickStepPill(
                                            label = isolateLtr(String.format(Locale.US, "%.3f", rate)),
                                            selected = active,
                                            onClick = { viewModel.setKcalPerStepOverride(rate) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Compact 7-Day Steps Consistency Strip
            item(key = "steps_7day_strip") {
                SevenDayStepsCard(
                    snapshot = snapshot,
                    endDate = selectedDate,
                    stepTarget = summary.stepTarget
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepsHeroTrackerCard(
    summary: StepsBurnSummary,
    onAdjustSteps: (Int) -> Unit,
    onOpenManualInput: () -> Unit,
    onOpenTargetInput: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val hitTarget = summary.steps >= summary.stepTarget && summary.stepTarget > 0
    val ringAccent = if (hitTarget) semantic.ok else StepsAccent
    val animatedSweep by animateFloatAsState(
        targetValue = summary.progressFraction,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "steps_ring_sweep"
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
    val pct = (summary.progressFraction * 100f).roundToInt()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (hitTarget) semantic.ok.copy(alpha = 0.45f) else subtleCardBorderColor()),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("steps_hero_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row: Title + Target edit pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                        contentDescription = null,
                        tint = ringAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "خطوات اليوم · ${isolateLtr("Steps")}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable(onClick = onOpenTargetInput)
                        .testTag("edit_step_target_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "الهدف ${isolateLtr(fmtCommaInt(summary.stepTarget))}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "تعديل هدف الخطوات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Animated circular progress ring
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onOpenManualInput)
                    .testTag("steps_ring_tap_edit"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 11.dp.toPx()
                    val inset = strokePx / 2f
                    val arcSize = Size(size.width - strokePx, size.height - strokePx)
                    val topLeft = Offset(inset, inset)
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    if (animatedSweep > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    ringAccent.copy(alpha = 0.78f),
                                    ringAccent,
                                    ringAccent
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = isolateLtr(fmtCommaInt(summary.steps)),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 30.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("steps_count_value")
                    )
                    Text(
                        text = if (hitTarget) {
                            "تم الهدف ✓"
                        } else {
                            "${isolateLtr("$pct%")} · ~${isolateLtr("${summary.estimatedDistanceKm}")} كم"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = if (hitTarget) semantic.ok else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick tactile step adjustments + manual entry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(-500 to "-500", 500 to "+500", 1000 to "+1K", 2500 to "+2.5K").forEach { (delta, label) ->
                    val interaction = remember { MutableInteractionSource() }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (delta > 0) {
                            StepsAccent.copy(alpha = 0.13f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .tactilePressScale(interaction, pressedScale = 0.94f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) { onAdjustSteps(delta) }
                            .testTag("steps_adjust_$delta")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                text = isolateLtr(label),
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = if (delta > 0) StepsAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = onOpenManualInput,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.testTag("steps_manual_entry_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "إدخال رقم الخطوات يدويًا",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CalorieTranslationCard(
    summary: StepsBurnSummary,
    adjustedTargets: NutritionTargets?,
    consumedKcal: Int,
    onSelectPreset: (StepBufferPreset) -> Unit,
    onOpenNutrition: () -> Unit
) {
    val semantic = LocalSemanticColors.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, subtleCardBorderColor()),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calorie_translation_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = semantic.warn,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "ترجمة الحرق للسعرات",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${isolateLtr("Active Burn")} → ${isolateLtr("Calorie Target")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Clean 3-column Energy Reference Strip: BMR (rest) vs Active Burn vs Estimated TDEE
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bmr_tdee_reference_strip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EnergyMiniStat(
                        title = isolateLtr("BMR"),
                        subtitle = "وقت الراحة",
                        value = isolateLtr(fmtCommaInt(summary.bmrReferenceKcal)),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(subtleCardBorderColor())
                    )
                    EnergyMiniStat(
                        title = isolateLtr("Active Burn"),
                        subtitle = "حرق الحركة",
                        value = "+${isolateLtr(fmtCommaInt(summary.totalActiveBurnKcal))}",
                        valueColor = StepsAccent,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(subtleCardBorderColor())
                    )
                    EnergyMiniStat(
                        title = isolateLtr("TDEE"),
                        subtitle = "حرق اليوم",
                        value = "~${isolateLtr(fmtCommaInt(summary.estimatedTdeeKcal))}",
                        valueColor = semantic.warn,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3-Option Buffer Factor Switcher: 0% / 50% (Default) / 100%
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("buffer_factor_switcher")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StepBufferPreset.entries.forEach { preset ->
                        val selected = summary.activePreset == preset
                        val bg by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            animationSpec = tween(180),
                            label = "buffer_pill_bg"
                        )
                        val titleColor by animateColorAsState(
                            targetValue = if (selected) StepsAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(180),
                            label = "buffer_pill_fg"
                        )
                        val interaction = remember { MutableInteractionSource() }

                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = bg,
                            shadowElevation = if (selected) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .tactilePressScale(interaction, pressedScale = 0.96f)
                                .clip(RoundedCornerShape(11.dp))
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null
                                ) { onSelectPreset(preset) }
                                .testTag("buffer_preset_${preset.percentLabel.removeSuffix("%")}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = isolateLtr(preset.percentLabel),
                                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = titleColor
                                )
                                Text(
                                    text = preset.titleAr,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Concise explanation note for active buffer preset
            Text(
                text = summary.activePreset.noteAr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = subtleCardBorderColor())

            // Clean numeric breakdown
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TranslationRow(
                    label = "حرق الراحة الكاملة (${isolateLtr("BMR")})",
                    value = "${isolateLtr(fmtCommaInt(summary.bmrReferenceKcal))} سعر",
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TranslationRow(
                    label = "الهدف الأساسي للتنشيف (${isolateLtr("Base Target")})",
                    value = "${isolateLtr(fmtCommaInt(summary.baseCalorieTarget))} سعر",
                    valueColor = MaterialTheme.colorScheme.onSurface
                )
                val burnSubtitle = if (summary.extraCardioKcal > 0) {
                    "حرق الخطوات (${isolateLtr("${summary.stepBurnKcal}")}) + كارديو (${isolateLtr("${summary.extraCardioKcal}")})"
                } else {
                    "حرق الخطوات التقديري (${isolateLtr("Step Burn")})"
                }
                TranslationRow(
                    label = burnSubtitle,
                    value = "~${isolateLtr(fmtCommaInt(summary.totalActiveBurnKcal))} سعر",
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TranslationRow(
                    label = "السعرات المضافة (${isolateLtr(summary.activePreset.percentLabel)})",
                    value = "+${isolateLtr(fmtCommaInt(summary.addedCaloriesTotal))} سعر",
                    valueColor = if (summary.addedCaloriesTotal > 0) StepsAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    boldValue = true
                )
                if (summary.protectedDeficitBufferKcal > 0) {
                    Text(
                        text = "أمان إضافي محجوز لعجز التنشيف: ${isolateLtr("${summary.protectedDeficitBufferKcal}")} سعر",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = semantic.ok.copy(alpha = 0.9f)
                    )
                }
            }

            HorizontalDivider(color = subtleCardBorderColor())

            // Prominent Adjusted Calorie Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "الهدف المعدّل اليوم",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = isolateLtr("Adjusted Calorie Target"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${isolateLtr(fmtCommaInt(summary.adjustedCalorieTarget))} سعر",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = StepsAccent,
                    modifier = Modifier.testTag("adjusted_calorie_target_value")
                )
            }

            val remaining = summary.adjustedCalorieTarget - consumedKcal
            val nutritionStatusText = when {
                consumedKcal > 0 && remaining >= 0 ->
                    "أكلت ${isolateLtr(fmtCommaInt(consumedKcal))} · باقيلك ${isolateLtr(fmtCommaInt(remaining))} سعر اليوم"
                consumedKcal > 0 ->
                    "أكلت ${isolateLtr(fmtCommaInt(consumedKcal))} · زيادة ${isolateLtr(fmtCommaInt(-remaining))} سعر عن المعدّل"
                adjustedTargets != null ->
                    "مربوط بالتغذية · البروتين محمي (${isolateLtr("${adjustedTargets.proteinG}g")})"
                else ->
                    "مربوط تلقائياً بتاب التغذية اليومي"
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenNutrition)
                    .testTag("steps_open_nutrition_link")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = nutritionStatusText,
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = if (consumedKcal > 0 && remaining < 0) semantic.warn else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "فتح التغذية",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StepsAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun EnergyMiniStat(
    title: String,
    subtitle: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.ExtraBold,
            color = valueColor,
            maxLines = 1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            maxLines = 1
        )
    }
}

@Composable
private fun TranslationRow(
    label: String,
    value: String,
    valueColor: Color,
    boldValue: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = if (boldValue) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun SevenDayStepsCard(
    snapshot: AppDataSnapshot,
    endDate: String,
    stepTarget: Int
) {
    val semantic = LocalSemanticColors.current
    val days = remember(snapshot.nutritionDays, endDate) {
        (-6..0).map { offset ->
            val iso = shiftIsoDate(endDate, offset.toLong())
            val st = snapshot.nutritionDays[iso]?.steps ?: 0
            iso.takeLast(2) to st
        }
    }
    val loggedDays = days.filter { it.second > 0 }
    val avgSteps = if (loggedDays.isNotEmpty()) loggedDays.map { it.second }.average().roundToInt() else 0
    val maxSteps = (days.maxOfOrNull { it.second } ?: 0).coerceAtLeast(stepTarget).coerceAtLeast(1)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, subtleCardBorderColor()),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر ${isolateLtr("7")} أيام",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (avgSteps > 0) "المتوسط ${isolateLtr(fmtCommaInt(avgSteps))} Steps" else "ابدأ تسجيل خطواتك",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { (dayNum, steps) ->
                    val frac = (steps.toFloat() / maxSteps.toFloat()).coerceIn(0.06f, 1f)
                    val hit = steps >= stepTarget && stepTarget > 0
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height((48.dp * frac).coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    when {
                                        steps <= 0 -> MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                                        hit -> semantic.ok
                                        else -> StepsAccent.copy(alpha = 0.85f)
                                    }
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = isolateLtr(dayNum),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStepPill(
    label: String,
    selected: Boolean = false,
    accent: Color = StepsAccent,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = if (selected) BorderStroke(1.dp, accent.copy(alpha = 0.55f)) else null,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun NumericInputDialog(
    title: String,
    label: String,
    initialValue: String,
    helperText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { next -> text = next.filter { it.isDigit() } },
                    label = { Text(label) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
