package com.mutabei.workout.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mutabei.workout.data.model.SetEntry
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.ui.components.formatCounterLtr
import com.mutabei.workout.ui.components.formatWorkoutElapsed
import com.mutabei.workout.ui.components.isolateLtr
import com.mutabei.workout.ui.components.subtleCardBorderColor
import com.mutabei.workout.ui.components.tactilePressScale
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.AppDataSnapshot
import com.mutabei.workout.ui.viewmodel.MainTab
import com.mutabei.workout.ui.viewmodel.MainViewModel
import com.mutabei.workout.ui.viewmodel.WorkoutSessionTimerState
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Clean, spacious Section Hub entry view.
 * Presents the 3 core sections (`التمرين`, `التغذية`, `الجسم`) with calm hierarchy
 * and a single concise live status line per section—zero nested card walls.
 */
@Composable
fun SectionHubScreen(
    snapshot: AppDataSnapshot,
    selectedDayIndex: Int,
    workoutDrafts: Map<String, List<SetEntry>>,
    workoutTimer: WorkoutSessionTimerState,
    viewModel: MainViewModel,
    onSelectSection: (MainTab) -> Unit
) {
    val semantic = LocalSemanticColors.current
    val haptics = LocalHapticFeedback.current
    val today = todayIso()

    val rollingBudgetEnabled by viewModel.rollingCalorieBudgetEnabled.collectAsStateWithLifecycle()
    val workoutSyncedEnabled by viewModel.workoutSyncedNutritionEnabled.collectAsStateWithLifecycle()
    val defaultStepTarget by viewModel.defaultStepTarget.collectAsStateWithLifecycle()
    val defaultStepBuffer by viewModel.defaultStepBufferFactor.collectAsStateWithLifecycle()
    val autoStepsStatus by viewModel.autoStepsStatus.collectAsStateWithLifecycle()

    // Single unified live daily context shared across Hub, Workout, Nutrition, Steps & Burn, and Body
    val dailyContext = remember(
        snapshot,
        today,
        selectedDayIndex,
        workoutDrafts,
        workoutTimer,
        rollingBudgetEnabled,
        workoutSyncedEnabled,
        defaultStepTarget,
        defaultStepBuffer,
        autoStepsStatus.liveTodaySteps
    ) {
        viewModel.computeDailyContext(
            snapshot = snapshot,
            date = today,
            rollingBudgetEnabled = rollingBudgetEnabled,
            workoutSyncedEnabled = workoutSyncedEnabled,
            selectedDayIndex = selectedDayIndex,
            workoutDrafts = workoutDrafts,
            workoutTimer = workoutTimer,
            defaultStepTarget = defaultStepTarget,
            defaultBufferFactor = defaultStepBuffer,
            liveAutoStepsToday = autoStepsStatus.liveTodaySteps
        )
    }
    val nf = remember { NumberFormat.getIntegerInstance(Locale.US) }

    // 1. Concise live Workout summary
    val workoutSummary = remember(dailyContext.workout) {
        val w = dailyContext.workout
        if (w.splitDayName == null) {
            Triple("ابدأ جلستك النهارده", null, 0f)
        } else {
            val statusLine = if (w.isTimerRunning) {
                "جلسة ${w.splitDayName} · شغّال ${isolateLtr(formatWorkoutElapsed(w.elapsedSeconds))}"
            } else {
                "جلسة ${w.splitDayName} · ${formatCounterLtr("${w.doneSets}", "${w.totalSets}", isolateLtr("Sets"))}"
            }
            val badge = when {
                w.isTimerRunning -> "نشط الآن"
                w.doneSets >= w.totalSets && w.totalSets > 0 -> "مكتملة ✓"
                w.doneSets > 0 -> isolateLtr("${(w.progressFraction * 100f).roundToInt()}%")
                else -> null
            }
            Triple(statusLine, badge, w.progressFraction)
        }
    }

    // 2. Concise live Nutrition summary (uses step-burn-adjusted calorie target & protected protein)
    val nutritionSummary = remember(dailyContext.adjustedTargets, dailyContext.consumedKcal, dailyContext.consumedProteinG) {
        val targets = dailyContext.adjustedTargets
        val kcal = dailyContext.consumedKcal
        val protein = dailyContext.consumedProteinG
        if (targets != null && targets.kcal > 0) {
            val progress = (kcal.toFloat() / targets.kcal.toFloat()).coerceIn(0f, 1f)
            val remaining = (targets.kcal - kcal).coerceAtLeast(0)
            val statusLine = if (kcal > 0) {
                "${formatCounterLtr("$kcal", "${targets.kcal}", "سعر")} · ${isolateLtr("${protein}g")} بروتين"
            } else {
                "هدفك اليوم ${isolateLtr("${targets.kcal}")} سعر · ${isolateLtr("${targets.proteinG}g")} بروتين"
            }
            val badge = if (kcal > 0) "باقي ${isolateLtr("$remaining")}" else null
            Triple(statusLine, badge, progress)
        } else {
            val statusLine = if (kcal > 0) {
                "${isolateLtr("$kcal")} سعر · ${isolateLtr("${protein}g")} بروتين"
            } else {
                "سجّل وجباتك وتابع الـ ${isolateLtr("Macros")}"
            }
            Triple(statusLine, null, 0f)
        }
    }

    // 3. Concise live Steps & Burn summary (with BMR 1,600 kcal reference & 50% safe buffer)
    val stepsSummary = remember(dailyContext.stepsBurn) {
        val eval = dailyContext.stepsBurn
        val statusLine = if (eval.steps > 0) {
            "${formatCounterLtr(nf.format(eval.steps), nf.format(eval.stepTarget), isolateLtr("Steps"))} · ${isolateLtr("+${eval.addedCaloriesTotal}")} سعر"
        } else {
            "${isolateLtr("BMR ${nf.format(eval.bmrReferenceKcal)}")} · سجّل خطواتك اليوم"
        }
        val badge = when {
            eval.steps >= eval.stepTarget -> "تم الهدف ✓"
            eval.steps > 0 -> isolateLtr("${(eval.progressFraction * 100f).roundToInt()}%")
            else -> isolateLtr(eval.activePreset.percentLabel)
        }
        Triple(statusLine, badge, eval.progressFraction)
    }

    // 4. Concise live Body summary (reflects live weight/fat/muscle & BMR reference)
    val bodySummary = remember(dailyContext.latestBodyRecord, dailyContext.bmrReferenceKcal) {
        val latest = dailyContext.latestBodyRecord
        if (latest != null) {
            val parts = buildList {
                latest.weightKg?.takeIf { it > 0.0 }?.let { w ->
                    add("${isolateLtr(String.format("%.1f", w))} كجم")
                }
                val pbf = latest.pbfPercent?.takeIf { it > 0.0 }
                val smm = latest.smmKg?.takeIf { it > 0.0 }
                if (pbf != null) {
                    add("${isolateLtr(String.format("%.1f%%", pbf))} دهون")
                } else if (smm != null) {
                    add("${isolateLtr(String.format("%.1f", smm))} كجم عضل")
                }
            }
            val line = if (parts.isNotEmpty()) {
                parts.joinToString(" · ")
            } else {
                "${isolateLtr("BMR ${nf.format(dailyContext.bmrReferenceKcal)}")} · سجّل قياس ${isolateLtr("InBody")}"
            }
            val badge = isolateLtr(latest.date.takeLast(5))
            Triple(line, badge, 0f)
        } else {
            Triple("${isolateLtr("BMR ${nf.format(dailyContext.bmrReferenceKcal)}")} · سجّل وزنك أو ${isolateLtr("InBody")}", null, 0f)
        }
    }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "hub_header") {
                Column(
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "لوحة اليوم · ${isolateLtr("Hub")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "ابدأ منين النهارده؟",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            item(key = "hub_card_workout") {
                HubSectionCard(
                    titleAr = "التمرين",
                    subtitleEn = "Workout",
                    statusLine = workoutSummary.first,
                    badgeText = workoutSummary.second,
                    progress = workoutSummary.third,
                    icon = Icons.Rounded.FitnessCenter,
                    accent = MaterialTheme.colorScheme.primary,
                    entered = entered,
                    delayMs = 0,
                    testTag = "nav_workout",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectSection(MainTab.WORKOUT)
                    }
                )
            }

            item(key = "hub_card_nutrition") {
                HubSectionCard(
                    titleAr = "التغذية",
                    subtitleEn = "Nutrition",
                    statusLine = nutritionSummary.first,
                    badgeText = nutritionSummary.second,
                    progress = nutritionSummary.third,
                    icon = Icons.Rounded.Restaurant,
                    accent = semantic.ok,
                    entered = entered,
                    delayMs = 35,
                    testTag = "nav_nutrition",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectSection(MainTab.NUTRITION)
                    }
                )
            }

            item(key = "hub_card_steps") {
                HubSectionCard(
                    titleAr = "الخطوات والحرق",
                    subtitleEn = "Steps & Burn",
                    statusLine = stepsSummary.first,
                    badgeText = stepsSummary.second,
                    progress = stepsSummary.third,
                    icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
                    accent = Color(0xFF14B8A6),
                    entered = entered,
                    delayMs = 70,
                    testTag = "nav_steps",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectSection(MainTab.STEPS)
                    }
                )
            }

            item(key = "hub_card_body") {
                HubSectionCard(
                    titleAr = "الجسم",
                    subtitleEn = "Body & InBody",
                    statusLine = bodySummary.first,
                    badgeText = bodySummary.second,
                    progress = bodySummary.third,
                    icon = Icons.Rounded.MonitorWeight,
                    accent = semantic.warn,
                    entered = entered,
                    delayMs = 105,
                    testTag = "nav_body",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectSection(MainTab.BODY)
                    }
                )
            }
        }
    }
}

@Composable
private fun HubSectionCard(
    titleAr: String,
    subtitleEn: String,
    statusLine: String,
    badgeText: String?,
    progress: Float,
    icon: ImageVector,
    accent: Color,
    entered: Boolean,
    delayMs: Int,
    testTag: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 240, delayMillis = delayMs, easing = FastOutSlowInEasing),
        label = "hub_card_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (entered) 0f else 18f,
        animationSpec = tween(durationMillis = 260, delayMillis = delayMs, easing = FastOutSlowInEasing),
        label = "hub_card_offset"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, subtleCardBorderColor()),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
            .tactilePressScale(interaction, pressedScale = 0.98f)
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = accent.copy(alpha = 0.14f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = titleAr,
                                tint = accent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = titleAr,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = isolateLtr(subtitleEn),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = statusLine,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!badgeText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = accent.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                maxLines = 1
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(3.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(accent)
                        )
                    }
                }
            }
        }
    }
}
