package com.mutabei.workout.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutabei.workout.data.model.AiWeeklyCoachReport
import com.mutabei.workout.data.model.CoachReportRecord
import com.mutabei.workout.data.model.CoachSuggestedFood
import com.mutabei.workout.data.model.CoachSuggestedTargets
import com.mutabei.workout.data.model.PreWorkoutGamePlan
import com.mutabei.workout.data.model.SplitDay
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.AiCoachUiState
import com.mutabei.workout.ui.viewmodel.AiPreWorkoutUiState

@Composable
fun CoachActionProposalsSection(
    suggestedTargets: CoachSuggestedTargets?,
    suggestedFoods: List<CoachSuggestedFood>,
    onApplyTargets: (CoachSuggestedTargets) -> Unit,
    onAddFood: (CoachSuggestedFood) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    if (suggestedTargets == null && suggestedFoods.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (suggestedTargets != null && suggestedTargets.isValid()) {
            var applied by remember(suggestedTargets) { mutableStateOf(false) }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "اقتراح تعديل أهداف السعرات والماكروز",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MacroPillMini("سعرات", "${suggestedTargets.kcal}")
                        MacroPillMini("بروتين", "${suggestedTargets.proteinG}جم")
                        MacroPillMini("كارب", "${suggestedTargets.carbsG}جم")
                        MacroPillMini("دهون", "${suggestedTargets.fatG}جم")
                    }
                    if (suggestedTargets.reason.isNotBlank()) {
                        Text(
                            text = suggestedTargets.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            applied = true
                            onApplyTargets(suggestedTargets)
                        },
                        enabled = !applied,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("coach_apply_targets_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (applied) "تم تطبيق الأهداف في ملفك" else "طبّق الأهداف الجديدة بضغطة واحدة")
                    }
                }
            }
        }

        if (suggestedFoods.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, semantic.ok.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Restaurant,
                            contentDescription = null,
                            tint = semantic.ok,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "إضافة سريعة ليوم النهارده",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = semantic.ok
                        )
                    }
                    suggestedFoods.forEach { food ->
                        var added by remember(food) { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${food.name} (${food.grams} جم)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${food.kcal} سعر · ب ${food.proteinG}جم · ك ${food.carbsG}جم · د ${food.fatG}جم",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    added = true
                                    onAddFood(food)
                                },
                                enabled = !added,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("coach_add_food_btn")
                            ) {
                                Icon(
                                    imageVector = if (added) Icons.Rounded.CheckCircle else Icons.Rounded.Add,
                                    contentDescription = "إضافة الأكلة لليوم",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (added) "اتضافت" else "أضف",
                                    style = MaterialTheme.typography.labelMedium
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
private fun MacroPillMini(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CoachAnalysisAndPlanTab(
    splitDays: List<SplitDay>,
    initialDayIndex: Int,
    preWorkoutState: AiPreWorkoutUiState,
    weeklyState: AiCoachUiState,
    onGeneratePreWorkout: (Int) -> Unit,
    onGenerateWeeklyReport: () -> Unit,
    onApplyTargets: (CoachSuggestedTargets) -> Unit,
    onAddFood: (CoachSuggestedFood) -> Unit
) {
    val semantic = LocalSemanticColors.current
    var chosenDayIdx by remember(initialDayIndex, splitDays.size) {
        mutableIntStateOf(initialDayIndex.coerceIn(0, (splitDays.size - 1).coerceAtLeast(0)))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1) Pre-Workout Game Plan Card
        item(key = "pre_workout_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.tertiary,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "خطة ما قبل التمرين ⚡",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تحليل وقودك النهارده (كارب، ماء، صوديوم، كافيين) + أوزان الأرقام القياسية المستهدفة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (splitDays.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            splitDays.forEachIndexed { idx, day ->
                                FilterChip(
                                    selected = chosenDayIdx == idx,
                                    onClick = { chosenDayIdx = idx },
                                    label = { Text(day.name, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }
                    }

                    if (preWorkoutState.busyMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Text(
                                text = preWorkoutState.busyMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Button(
                            onClick = { onGeneratePreWorkout(chosenDayIdx) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_preworkout_plan_btn")
                        ) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (preWorkoutState.plan == null) "جهّز خطة تمرينة النهارده"
                                else "تحديث خطة ما قبل التمرين"
                            )
                        }
                    }

                    if (!preWorkoutState.errorMessage.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = preWorkoutState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    preWorkoutState.plan?.let { plan ->
                        PreWorkoutPlanBody(plan = plan)
                    }
                }
            }
        }

        // 2) Comprehensive Weekly Coach Report Card
        item(key = "weekly_coach_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            semantic.ok
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Assessment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "التقرير الأسبوعي الشامل 🎯",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "كشف ثبات الأوزان (Plateaus)، قرار الـ Deload، توازن InBody، ومراجعة خطة الأسبوع الماضي — بيتحفظ تلقائياً في الأرشيف",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (weeklyState.busyMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Text(
                                text = weeklyState.busyMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Button(
                            onClick = onGenerateWeeklyReport,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_weekly_report_btn")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (weeklyState.report == null) "ابدأ التحليل الأسبوعي الشامل"
                                else "تحديث وإنشاء تقرير أسبوعي جديد"
                            )
                        }
                    }

                    if (!weeklyState.errorMessage.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = weeklyState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    weeklyState.report?.let { report ->
                        WeeklyReportDetailsBody(
                            report = report,
                            savedBadge = weeklyState.savedToArchive,
                            onApplyTargets = onApplyTargets,
                            onAddFood = onAddFood
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreWorkoutPlanBody(plan: PreWorkoutGamePlan) {
    val semantic = LocalSemanticColors.current
    val scoreColor = when {
        plan.readinessScore >= 80 -> semantic.ok
        plan.readinessScore >= 60 -> semantic.warn
        else -> semantic.low
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = scoreColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "جاهزية يوم ${plan.dayName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = scoreColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (plan.fuelStatus.isNotBlank()) {
                        Text(
                            text = plan.fuelStatus,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "${plan.readinessScore}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor
                )
            }
        }

        if (plan.preWorkoutFuelTip.isNotBlank()) {
            CoachMiniTipRow(
                icon = Icons.Rounded.LocalFireDepartment,
                tint = semantic.warn,
                title = "وجبة قبل التمرين",
                body = plan.preWorkoutFuelTip
            )
        }
        if (plan.hydrationSodiumTip.isNotBlank()) {
            CoachMiniTipRow(
                icon = Icons.Rounded.WaterDrop,
                tint = MaterialTheme.colorScheme.primary,
                title = "الماء والصوديوم والكافيين",
                body = plan.hydrationSodiumTip
            )
        }
        if (plan.warmupFocus.isNotBlank()) {
            CoachMiniTipRow(
                icon = Icons.Rounded.FitnessCenter,
                tint = semantic.ok,
                title = "تسخين 5 دقائق",
                body = plan.warmupFocus
            )
        }

        if (plan.prTargets.isNotEmpty()) {
            Text(
                text = "أهداف الأرقام القياسية النهارده (PR Targets)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            plan.prTargets.forEach { target ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = target.exerciseName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "السابق: ${target.lastBest}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "الهدف: ${target.targetWeightReps}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = semantic.ok
                            )
                        }
                        if (target.cue.isNotBlank()) {
                            Text(
                                text = "💡 ${target.cue}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (plan.cautionNote.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "📌 ${plan.cautionNote}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
fun WeeklyReportDetailsBody(
    report: AiWeeklyCoachReport,
    savedBadge: Boolean = false,
    onApplyTargets: (CoachSuggestedTargets) -> Unit,
    onAddFood: (CoachSuggestedFood) -> Unit
) {
    val semantic = LocalSemanticColors.current
    val scoreColor = when {
        report.overallScore >= 80 -> semantic.ok
        report.overallScore >= 60 -> semantic.warn
        else -> semantic.low
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = scoreColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "تقييم الأسبوع",
                            style = MaterialTheme.typography.labelMedium,
                            color = scoreColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (savedBadge) {
                            Text(
                                text = "• محفوظ في الأرشيف",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.ok
                            )
                        }
                    }
                    Text(
                        text = report.headline,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${report.overallScore}/100",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor
                )
            }
        }

        if (report.previousPlanReview.isNotBlank()) {
            CoachMiniTipRow(
                icon = Icons.Rounded.History,
                tint = MaterialTheme.colorScheme.primary,
                title = "مراجعة خطة الأسبوع الماضي",
                body = report.previousPlanReview
            )
        }

        if (report.wins.isNotEmpty()) {
            ReportBulletBlock("نقاط القوة ✅", semantic.ok, report.wins)
        }

        if (report.attentionNeeded.isNotEmpty()) {
            ReportBulletBlock("محتاج تركيز ⚠️", semantic.warn, report.attentionNeeded)
        }

        if (report.plateaus.isNotEmpty()) {
            Surface(
                color = semantic.low.copy(alpha = 0.10f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, semantic.low.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.TrendingFlat, contentDescription = null, tint = semantic.low, modifier = Modifier.size(18.dp))
                        Text(
                            text = "كاشف ثبات الأوزان (Plateaus)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = semantic.low
                        )
                    }
                    report.plateaus.forEach { p ->
                        Text("• $p", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (report.deloadNote.isNotBlank()) {
            val deloadTint = if (report.deloadRecommended) semantic.warn else semantic.ok
            CoachMiniTipRow(
                icon = if (report.deloadRecommended) Icons.Rounded.Warning else Icons.Rounded.Hotel,
                tint = deloadTint,
                title = if (report.deloadRecommended) "مطلوب أسبوع Deload" else "حالة الاستشفاء والـ Deload",
                body = report.deloadNote
            )
        }

        if (report.asymmetryNote.isNotBlank()) {
            CoachMiniTipRow(
                icon = Icons.Rounded.Balance,
                tint = MaterialTheme.colorScheme.primary,
                title = "توازن العضلات (يمين مقابل يسار - InBody)",
                body = report.asymmetryNote
            )
        }

        if (report.nextWeekActions.isNotEmpty()) {
            ReportBulletBlock("خطة الأسبوع الجاي 🎯", MaterialTheme.colorScheme.primary, report.nextWeekActions)
        }

        CoachActionProposalsSection(
            suggestedTargets = report.suggestedTargets,
            suggestedFoods = report.suggestedFoods,
            onApplyTargets = onApplyTargets,
            onAddFood = onAddFood
        )
    }
}

@Composable
private fun ReportBulletBlock(title: String, titleColor: Color, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = titleColor
        )
        items.forEach { line ->
            Text(
                text = "• $line",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CoachMiniTipRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tint
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun CoachArchiveTab(
    reports: List<CoachReportRecord>,
    onDeleteReport: (String) -> Unit,
    onApplyTargets: (CoachSuggestedTargets) -> Unit,
    onAddFood: (CoachSuggestedFood) -> Unit,
    onJumpToGenerate: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    var expandedId by remember { mutableStateOf(reports.firstOrNull()?.id) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("حذف التقرير من الأرشيف؟") },
            text = { Text("هيتم حذف التقرير ده نهائياً من حسابك السحابي.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteId?.let(onDeleteReport)
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("إلغاء") }
            }
        )
    }

    if (reports.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "الأرشيف فاضي لسه",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "كل تقرير أسبوعي بتعمله بيتحفظ تلقائياً هنا في حسابك، والمدرب الذكي بيراجعه الأسبوع اللي بعده عشان يتابع التزامك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onJumpToGenerate,
                    modifier = Modifier.testTag("archive_jump_generate_btn")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("اعمل أول تقرير أسبوعي")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (reports.size >= 2) {
            item(key = "archive_trend_banner") {
                val latest = reports.first()
                val prev = reports[1]
                val delta = latest.report.overallScore - prev.report.overallScore
                val deltaColor = if (delta >= 0) semantic.ok else semantic.low
                val deltaLabel = if (delta >= 0) "+$delta عن التقرير السابق" else "$delta عن التقرير السابق"
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "تطور تقييمك الأسبوعي (${reports.size} تقارير)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = deltaLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = deltaColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "${latest.report.overallScore}/100",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        itemsIndexed(reports, key = { _, item -> item.id }) { index, record ->
            val isExpanded = expandedId == record.id
            val scoreColor = when {
                record.report.overallScore >= 80 -> semantic.ok
                record.report.overallScore >= 60 -> semantic.warn
                else -> semantic.low
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("archive_report_card_$index")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedId = if (isExpanded) null else record.id },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = scoreColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${record.report.overallScore}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الأسبوع ${record.weekNumber} · ${record.date}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = record.report.headline,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = if (isExpanded) 4 else 2
                            )
                        }
                        IconButton(
                            onClick = { pendingDeleteId = record.id },
                            modifier = Modifier.testTag("delete_report_btn_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "حذف التقرير",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (isExpanded) "إخفاء التفاصيل" else "عرض التفاصيل",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                            WeeklyReportDetailsBody(
                                report = record.report,
                                savedBadge = false,
                                onApplyTargets = onApplyTargets,
                                onAddFood = onAddFood
                            )
                        }
                    }
                }
            }
        }
    }
}
