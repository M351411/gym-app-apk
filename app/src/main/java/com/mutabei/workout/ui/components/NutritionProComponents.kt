package com.mutabei.workout.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutabei.workout.data.model.AiDeficiencyFixResult
import com.mutabei.workout.data.model.BodyRecord
import com.mutabei.workout.data.model.LoggedFoodItem
import com.mutabei.workout.data.model.NutritionConstants
import com.mutabei.workout.data.model.NutritionDay
import com.mutabei.workout.data.model.NutritionTargets
import com.mutabei.workout.data.model.daysBetweenIso
import com.mutabei.workout.data.model.shiftIsoDate
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.ui.theme.LocalSemanticColors
import kotlin.math.abs
import kotlin.math.roundToInt

data class MealLeucineStatus(
    val leucineGrams: Double,
    val hasEstimatedPart: Boolean,
    val mpsTriggered: Boolean
)

fun computeMealLeucineStatus(mealItems: List<LoggedFoodItem>): MealLeucineStatus {
    val leuIdx = NutritionConstants.NUTRIENT_INDEX["leu"] ?: -1
    var totalLeu = 0.0
    var estimated = false
    mealItems.forEach { item ->
        val per100Leu = if (leuIdx >= 0) (item.nutrients.getOrNull(leuIdx) ?: -1.0) else -1.0
        if (per100Leu >= 0.0) {
            totalLeu += per100Leu * item.grams / 100.0
        } else {
            val protein = item.nutrientAmount("p")
            if (protein > 0.0) {
                // Typical mixed-protein leucine content (~8.5% of protein) when legacy food lacks explicit leucine
                totalLeu += protein * 0.085
                estimated = true
            }
        }
    }
    return MealLeucineStatus(
        leucineGrams = totalLeu,
        hasEstimatedPart = estimated,
        mpsTriggered = totalLeu >= 2.5
    )
}

@Composable
fun MealLeucineMpsChip(
    status: MealLeucineStatus,
    modifier: Modifier = Modifier
) {
    if (status.leucineGrams < 0.2) return
    val semantic = LocalSemanticColors.current
    val prefix = if (status.hasEstimatedPart) "~" else ""
    val formatted = String.format("%.1f", status.leucineGrams)

    val bgColor = if (status.mpsTriggered) semantic.ok.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val contentColor = if (status.mpsTriggered) semantic.ok else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(99.dp),
        color = bgColor,
        border = if (status.mpsTriggered) BorderStroke(1.dp, semantic.ok.copy(alpha = 0.55f)) else null,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            if (status.mpsTriggered) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = "تحفيز البناء العضلي مفعّل",
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = if (status.mpsTriggered) {
                    "MPS مفعّل · ${prefix}${formatted} جم ليوسين"
                } else {
                    "ليوسين ${prefix}${formatted}/2.5 جم"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (status.mpsTriggered) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

data class AdaptiveTdeeEstimate(
    val actualTdee: Int,
    val avgIntake: Int,
    val loggedDaysCount: Int,
    val spanDays: Int,
    val weeklyWeightChangeKg: Double,
    val recommendedKcal: Int,
    val advice: String,
    val isPlateauDetected: Boolean = false,
    val isBodyRecompDetected: Boolean = false,
    val plateauNoteAr: String = "",
    val inBodyTrendSummaryAr: String = ""
)

fun computeAdaptiveTdee(
    nutritionDays: Map<String, NutritionDay>,
    bodyRecords: List<BodyRecord>,
    goal: String
): AdaptiveTdeeEstimate? {
    val today = todayIso()
    val cutoff = shiftIsoDate(today, -35)
    val weights = bodyRecords
        .filter { (it.weightKg ?: 0.0) > 0.0 && it.date in cutoff..today }
        .sortedBy { it.date }
    if (weights.size < 2) return null

    val first = weights.first()
    val last = weights.last()
    val spanDays = daysBetweenIso(first.date, last.date).toInt()
    if (spanDays < 7) return null

    val validNutrition = nutritionDays.values.filter { d ->
        d.date in first.date..last.date && d.items.sumOf { it.nutrientAmount("kcal") } >= 800.0
    }
    if (validNutrition.size < 4) return null

    val avgIntake = validNutrition.sumOf { d -> d.items.sumOf { it.nutrientAmount("kcal") } } / validNutrition.size
    val deltaKg = last.weightKg!! - first.weightKg!!
    val dailyEnergyDelta = (deltaKg * 7700.0) / spanDays.toDouble()
    val rawTdee = (avgIntake - dailyEnergyDelta).coerceIn(1100.0, 5500.0)
    val actualTdee = ((rawTdee / 10.0).roundToInt() * 10)
    val weeklyChange = deltaKg / (spanDays / 7.0)

    // InBody muscle & body fat trend analysis (distinguishes Body Recomp from true Plateau)
    val inBodyRecords = bodyRecords
        .filter { (it.smmKg != null || it.pbfPercent != null) && it.date in cutoff..today }
        .sortedBy { it.date }
    val deltaSmm = if (inBodyRecords.size >= 2 && inBodyRecords.first().smmKg != null && inBodyRecords.last().smmKg != null) {
        inBodyRecords.last().smmKg!! - inBodyRecords.first().smmKg!!
    } else null
    val deltaPbf = if (inBodyRecords.size >= 2 && inBodyRecords.first().pbfPercent != null && inBodyRecords.last().pbfPercent != null) {
        inBodyRecords.last().pbfPercent!! - inBodyRecords.first().pbfPercent!!
    } else null

    val inBodyTrendSummary = buildString {
        if (deltaSmm != null || deltaPbf != null) {
            append("تريند InBody: ")
            deltaSmm?.let { append("عضل ${String.format("%+.1f", it)} كجم") }
            if (deltaSmm != null && deltaPbf != null) append(" · ")
            deltaPbf?.let { append("دهون ${String.format("%+.1f", it)}%") }
        }
    }

    val isRecomp = abs(weeklyChange) <= 0.16 &&
        ((deltaSmm != null && deltaSmm >= 0.2) && (deltaPbf != null && deltaPbf <= -0.4))

    val isTruePlateau = !isRecomp &&
        spanDays >= 14 &&
        validNutrition.size >= 8 &&
        ((goal == "cut" && weeklyChange > -0.12) || (goal == "bulk" && weeklyChange < 0.10))

    val baseGoalDelta = when (goal) {
        "cut" -> -400
        "keep" -> 0
        else -> 250
    }
    val plateauAdjustment = when {
        isTruePlateau && goal == "cut" -> -150
        isTruePlateau && goal == "bulk" -> 150
        else -> 0
    }
    val recommended = (((actualTdee + baseGoalDelta + plateauAdjustment).coerceIn(1200, 6000)) / 10.0).roundToInt() * 10

    val plateauNote = when {
        isRecomp -> "ممتاز! الميزان شبه ثابت لكن قياسات InBody بتأكد إعادة تشكيل جسم (Recomp: زيادة عضل مع نزول دهون) — لا تغيّر سعراتك بعنف."
        isTruePlateau && goal == "cut" -> "تم رصد ثبات وزن (Plateau) لأكتر من أسبوعين رغم التزامك بالتسجيل. نقترح خفض 150 سعر يوميًا أو عمل يوم Refeed عالي الكارب ثم التطبيق."
        isTruePlateau && goal == "bulk" -> "تم رصد ثبات في التضخيم (Plateau) لأكتر من أسبوعين. نقترح رفع السعرات +150 سعر/يوم (أغلبها كارب حول التمرين)."
        else -> ""
    }

    val advice = when (goal) {
        "cut" -> "بناءً على حرقك الفعلي، $recommended سعر هينزلك ~0.4 كجم دهون أسبوعيًا مع حماية العضل."
        "keep" -> "بناءً على حرقك الفعلي، $recommended سعر هيثبّت وزنك بدقة."
        else -> "بناءً على حرقك الفعلي، $recommended سعر هيدّيك تضخيم نظيف (~0.25 كجم/أسبوع) بأقل دهون."
    }

    return AdaptiveTdeeEstimate(
        actualTdee = actualTdee,
        avgIntake = avgIntake.roundToInt(),
        loggedDaysCount = validNutrition.size,
        spanDays = spanDays,
        weeklyWeightChangeKg = weeklyChange,
        recommendedKcal = recommended,
        advice = advice,
        isPlateauDetected = isTruePlateau,
        isBodyRecompDetected = isRecomp,
        plateauNoteAr = plateauNote,
        inBodyTrendSummaryAr = inBodyTrendSummary
    )
}

@Composable
fun AdaptiveTdeeSection(
    estimate: AdaptiveTdeeEstimate?,
    theoreticalTargets: NutritionTargets?,
    onApplyRecommendedKcal: (Int) -> Unit
) {
    val semantic = LocalSemanticColors.current
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(
            1.dp,
            when {
                estimate?.isPlateauDetected == true -> semantic.warn.copy(alpha = 0.65f)
                estimate?.isBodyRecompDetected == true -> semantic.ok.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("adaptive_tdee_section")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "الحرق الفعلي + كاشف الثبات وInBody (Dynamic TDEE)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (estimate == null) {
                Text(
                    text = "سجّل على الأقل 4 أيام تغذية + قياسين وزن أو InBody بينهم أسبوع عشان التطبيق يحسب حرق جسمك الحقيقي ويكشف أي ثبات (Plateau) تلقائيًا.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("حرقك الفعلي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${estimate.actualTdee} سعر",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = semantic.ok
                        )
                    }
                    if (theoreticalTargets != null) {
                        val diff = estimate.actualTdee - theoreticalTargets.tdee
                        val sign = if (diff >= 0) "+" else ""
                        Column(horizontalAlignment = Alignment.End) {
                            Text("الحرق النظري", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${theoreticalTargets.tdee} ($sign$diff)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = "محسوب من متوسط ${estimate.avgIntake} سعر (${estimate.loggedDaysCount} يوم) وتغيّر وزن ${String.format("%+.2f", estimate.weeklyWeightChangeKg)} كجم/أسبوع خلال ${estimate.spanDays} يوم.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (estimate.inBodyTrendSummaryAr.isNotBlank()) {
                    Text(
                        text = estimate.inBodyTrendSummaryAr,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (estimate.plateauNoteAr.isNotBlank()) {
                    val noteColor = if (estimate.isBodyRecompDetected) semantic.ok else semantic.warn
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = noteColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, noteColor.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plateau_detector_banner")
                    ) {
                        Text(
                            text = estimate.plateauNoteAr,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = noteColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
                Text(
                    text = estimate.advice,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (theoreticalTargets == null || abs(theoreticalTargets.kcal - estimate.recommendedKcal) >= 30 || estimate.isPlateauDetected) {
                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = { onApplyRecommendedKcal(estimate.recommendedKcal) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_adaptive_kcal_button")
                    ) {
                        Text(
                            if (estimate.isPlateauDetected) "كسر الثبات: تطبيق ${estimate.recommendedKcal} سعر"
                            else "تطبيق ${estimate.recommendedKcal} سعر كهدف يومي"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaveMealPresetDialog(
    defaultMealName: String,
    itemCount: Int,
    totalKcal: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember(defaultMealName) { mutableStateOf("وجبة $defaultMealName") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حفظ كوجبة جاهزة", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "هتتحفظ كل الأصناف ($itemCount صنف · $totalKcal سعر) في «أكلاتي» وتقدر تنزلها بعدين بضغطة واحدة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الوجبة المحفوظة") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_preset_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_save_preset_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun AiDeficiencyFixerBlock(
    busyMessage: String?,
    errorMessage: String?,
    result: AiDeficiencyFixResult?,
    onRequestFix: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onRequestFix,
            enabled = busyMessage == null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_deficiency_fixer_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text("اقترح أكلات مصرية لسد النقص (Gemini)")
        }

        if (busyMessage != null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = busyMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (result != null) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = result.summary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    result.suggestions.forEachIndexed { idx, s ->
                        if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• ${s.foodNameAr} (${s.servingGrams} جم)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "~${s.approxKcal} سعر",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.ok,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (s.coversNutrients.isNotEmpty()) {
                            Text(
                                text = "يسد: ${s.coversNutrients.joinToString(" · ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (s.note.isNotBlank()) {
                            Text(
                                text = s.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
