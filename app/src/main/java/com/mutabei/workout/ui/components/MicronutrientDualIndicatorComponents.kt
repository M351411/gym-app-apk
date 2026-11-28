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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutabei.workout.data.model.DailyNutrientEvaluation
import com.mutabei.workout.data.model.FatSolubleEvaluation
import com.mutabei.workout.data.model.MicronutrientStatus
import com.mutabei.workout.ui.theme.LocalSemanticColors
import kotlin.math.roundToInt

/**
 * Dual-Indicator card for Fat-Soluble Vitamins (A, D3, E, K):
 * Row 1: Today's Intake vs Daily Target Base
 * Row 2: 7-Day Rolling Reserve Pool (Today + Past 6 Days) vs Weekly Target, Average & Remaining Pool
 */
@Composable
fun FatSolubleDualIndicatorCard(
    eval: FatSolubleEvaluation,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val isCoveredByCarryIn = eval.todayCarryInSource > 0.0 && eval.todayIntake >= eval.dailyTargetBase
    val badgeColor: Color = when (eval.status) {
        MicronutrientStatus.TOXICITY_WARNING -> semantic.low
        MicronutrientStatus.APPROACHING_UL -> semantic.warn
        MicronutrientStatus.WEEKLY_BUFFER_ACTIVE -> semantic.ok
        MicronutrientStatus.NEEDS_TOP_UP -> semantic.warn
        MicronutrientStatus.ON_TRACK -> if (isCoveredByCarryIn || eval.todayIntake >= eval.dailyTargetBase) semantic.ok else MaterialTheme.colorScheme.primary
    }

    val badgeLabel: String = when {
        eval.status == MicronutrientStatus.ON_TRACK && isCoveredByCarryIn -> "مُغطى اليوم · مُرحّل"
        eval.status == MicronutrientStatus.ON_TRACK && eval.todayIntake >= eval.dailyTargetBase -> "مُغطى اليوم"
        else -> eval.uiBadge
    }

    val reserveBarColor: Color = when (eval.status) {
        MicronutrientStatus.TOXICITY_WARNING -> semantic.low
        MicronutrientStatus.APPROACHING_UL -> semantic.warn
        MicronutrientStatus.WEEKLY_BUFFER_ACTIVE -> semantic.ok
        MicronutrientStatus.NEEDS_TOP_UP -> MaterialTheme.colorScheme.primary
        MicronutrientStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                eval.status == MicronutrientStatus.TOXICITY_WARNING -> semantic.low.copy(alpha = 0.65f)
                eval.status == MicronutrientStatus.APPROACHING_UL -> semantic.warn.copy(alpha = 0.55f)
                eval.status == MicronutrientStatus.WEEKLY_BUFFER_ACTIVE || isCoveredByCarryIn -> semantic.ok.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("fat_soluble_card_${eval.key}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Vitamin name + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = eval.labelAr + if (!eval.hasAnyDataInWindow) " (مفيش بيانات)" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("fat_soluble_badge_${eval.key}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val badgeIcon = when {
                            eval.status == MicronutrientStatus.TOXICITY_WARNING ||
                                eval.status == MicronutrientStatus.APPROACHING_UL -> Icons.Rounded.Warning
                            eval.status == MicronutrientStatus.WEEKLY_BUFFER_ACTIVE ||
                                (eval.status == MicronutrientStatus.ON_TRACK && eval.todayIntake >= eval.dailyTargetBase) -> Icons.Rounded.CheckCircle
                            else -> Icons.Rounded.Shield
                        }
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            // Indicator 1: Today's Intake vs Daily Base (includes automatic Vitamin D3 carryover from yesterday)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (eval.key == "vA") "اليوم (${isolateLtr("RAE")})" else "اليوم",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = isolateLtr("${fmtMicro(eval.todayIntake, eval.unit)} / ${fmtMicro(eval.dailyTargetBase, eval.unit)} ${eval.unit}"),
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = if (eval.todayIntake >= eval.dailyTargetBase) semantic.ok else MaterialTheme.colorScheme.onSurface
                )
            }
            MetricProgressBar(
                current = eval.todayIntake,
                target = eval.dailyTargetBase.coerceAtLeast(1.0),
                barColor = if (eval.todayIntake >= eval.dailyTargetBase) semantic.ok else MaterialTheme.colorScheme.secondary
            )
            if (eval.todayCarryInSource > 0.0) {
                val breakdownParts = buildList {
                    if (eval.todayFoodSource > 0.0) add("أكل ${isolateLtr(fmtMicro(eval.todayFoodSource, eval.unit))}")
                    if (eval.todaySupplementSource > 0.0) add("مكملات ${isolateLtr(fmtMicro(eval.todaySupplementSource, eval.unit))}")
                    add("مُرحّل من أمس ${isolateLtr("+${fmtMicro(eval.todayCarryInSource, eval.unit)} ${eval.unit}")}")
                }.joinToString(" · ")
                Text(
                    text = "اليوم: $breakdownParts",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = semantic.ok
                )
            } else if (eval.key == "vA" && eval.todayIntake > 0.0) {
                Text(
                    text = "اليوم: ${isolateLtr("Retinol")} ${isolateLtr("${fmtMicro(eval.todayRetinolMcg, "mcg")} mcg")} · ${isolateLtr("Beta-carotene (RAE)")} ${isolateLtr("${fmtMicro(eval.todayProvitaminARaeMcg, "mcg")} mcg")}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (eval.todaySupplementSource > 0.0 && eval.todayFoodSource > 0.0) {
                Text(
                    text = "اليوم: أكل ${isolateLtr(fmtMicro(eval.todayFoodSource, eval.unit))} · مكملات ${isolateLtr("${fmtMicro(eval.todaySupplementSource, eval.unit)} ${eval.unit}")}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(2.dp))

            // Indicator 2: 7-Day Rolling Reserve Pool
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مخزون ${isolateLtr("7")} أيام (${isolateLtr("${eval.weeklyPercentage.roundToInt()}%")})",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = reserveBarColor
                )
                Text(
                    text = isolateLtr("${fmtMicro(eval.sevenDayTotal, eval.unit)} / ${fmtMicro(eval.weeklyTarget, eval.unit)} ${eval.unit}"),
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold
                )
            }
            MetricProgressBar(
                current = eval.sevenDayTotal,
                target = eval.weeklyTarget.coerceAtLeast(1.0),
                barColor = reserveBarColor
            )

            // Weekly average + Remaining weekly pool
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "متوسط ${isolateLtr("7-day rolling")}: ${isolateLtr("${fmtMicro(eval.sevenDayAvg, eval.unit)} ${eval.unit}")}/يوم",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (eval.remainingWeeklyPool > 0.0) {
                        "متبقي: ${isolateLtr("${fmtMicro(eval.remainingWeeklyPool, eval.unit)} ${eval.unit}")}"
                    } else {
                        "المخزون الأسبوعي مُغطى"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = if (eval.remainingWeeklyPool <= 0.0) semantic.ok else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Vitamin A 7-day Retinol vs Plant Beta-Carotene breakdown line
            if (eval.key == "vA" && eval.sevenDayTotal > 0.0) {
                Text(
                    text = "خلال ${isolateLtr("7")} أيام: ${isolateLtr("Retinol")} ${isolateLtr("${fmtMicro(eval.sevenDayRetinolMcg, "mcg")} mcg")} (حد ${isolateLtr("UL")} اليومي ${isolateLtr("3000 mcg")}) · ${isolateLtr("Beta-carotene")} ${isolateLtr("${fmtMicro(eval.sevenDayProvitaminARaeMcg, "mcg")} mcg RAE")}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Calm non-alarmist Egyptian Arabic note for high plant Provitamin A + fat Bioavailability tip
            if (eval.key == "vA" && eval.highCarotenoidSafeNoteActive) {
                val infoAccent = MaterialTheme.colorScheme.primary
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = infoAccent.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, infoAccent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vitamin_a_carotenoid_safe_note")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "معظم فيتامين A عندك من مصادر نباتية (${isolateLtr("Beta-carotene / RAE")}) — وده آمن تماماً ومفيهوش خطورة سمّية لأن حد الـ ${isolateLtr("UL")} (${isolateLtr("3000 mcg")}/يوم) يخص الـ ${isolateLtr("Retinol")} الحيواني والمكملات بس.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (!eval.todayCarotenoidMealHasAdequateFat) {
                                "💡 نصيحة ${isolateLtr("Bioavailability")}: ضيف دهون صحية مع الجزر أو الخضار في نفس الوجبة (زي زيت زيتون، بيض، أو مكسرات) عشان جسمك يمتص الـ ${isolateLtr("Beta-carotene")} بكفاءة أعلى."
                            } else {
                                "💡 ملحوظة ${isolateLtr("Bioavailability")}: امتصاص الـ ${isolateLtr("Beta-carotene")} بيكون أفضل دايماً لما يتاكل مع مصدر دهون صحية في الوجبة."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Safety guardrail for APPROACHING_UL / TOXICITY_WARNING (strictly Preformed Retinol for Vitamin A)
            if (eval.status == MicronutrientStatus.TOXICITY_WARNING ||
                eval.status == MicronutrientStatus.APPROACHING_UL
            ) {
                val ulVal = eval.weeklyUl
                val isTox = eval.status == MicronutrientStatus.TOXICITY_WARNING
                val guardrailColor = if (isTox) semantic.low else semantic.warn
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = guardrailColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, guardrailColor.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fat_soluble_ul_warning_${eval.key}")
                ) {
                    val warningText = if (eval.key == "vA") {
                        buildString {
                            if (isTox) {
                                append("تنبيه ${isolateLtr("UL")} للـ ${isolateLtr("Retinol")}: استهلاكك من فيتامين A الحيواني/المكملات (${isolateLtr("Retinol")}) وصل أو عدّى الحد الآمن")
                            } else {
                                append("قربت من الحد الآمن (${isolateLtr("UL")}) للـ ${isolateLtr("Retinol")} الحيواني/المكملات")
                            }
                            append(" (${isolateLtr("${fmtMicro(eval.todayRetinolMcg, "mcg")} / 3000 mcg")} اليوم · ${isolateLtr("${fmtMicro(eval.sevenDayRetinolMcg, "mcg")} / 21000 mcg")} خلال ${isolateLtr("7")} أيام). ")
                            append("الأفضل توقف الكبدة أو مكملات الـ ${isolateLtr("Retinol")} لمدة ${isolateLtr("24–48")} ساعة — أما المصادر النباتية (${isolateLtr("Beta-carotene")}) فملهاش نفس خطورة السمّية.")
                        }
                    } else {
                        buildString {
                            if (isTox) {
                                append("وصلت للحد الأسبوعي الآمن (${isolateLtr("UL")})")
                            } else {
                                append("اقتربت من الحد الأسبوعي الآمن (${isolateLtr("UL")})")
                            }
                            if (ulVal != null) {
                                append(" (${isolateLtr("${fmtMicro(ulVal, eval.unit)} ${eval.unit}")}/أسبوع)")
                            }
                            append(". يُنصح بإيقاف المكملات أو الجرعات العالية لمدة ${isolateLtr("24–48")} ساعة.")
                        }
                    }
                    Text(
                        text = warningText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = guardrailColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Daily-reset row for Water-Soluble Vitamins (B-Complex, C) and Minerals.
 * Enforces food_source vs supplement_source distinction so supplement-only ULs
 * (like Magnesium 350 mg/day) never trigger false toxicity warnings from whole foods.
 */
@Composable
fun DailyNutrientRow(
    eval: DailyNutrientEvaluation,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val unit = eval.unit
    val targetLabel = (if (eval.isLimitKey) "حد " else "") + fmtMicro(eval.dailyTarget, unit)
    val valueText = formatCounterLtr(fmtMicro(eval.todayIntake, unit), targetLabel, unit)

    val barColor = when {
        eval.status == MicronutrientStatus.TOXICITY_WARNING -> semantic.low
        eval.status == MicronutrientStatus.APPROACHING_UL -> semantic.warn
        eval.isLimitKey && eval.todayIntake > eval.dailyTarget -> semantic.low
        !eval.isLimitKey && eval.todayIntake >= eval.dailyTarget -> semantic.ok
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = eval.labelAr + if (eval.hasDataToday) "" else " (مفيش بيانات)",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (eval.status == MicronutrientStatus.TOXICITY_WARNING) FontWeight.Bold else FontWeight.Normal,
                color = if (eval.status == MicronutrientStatus.TOXICITY_WARNING) semantic.low else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(3.dp))
        MetricProgressBar(
            current = eval.todayIntake,
            target = eval.dailyTarget.coerceAtLeast(1.0),
            barColor = barColor
        )

        // Show food_source vs supplement_source distinction for supplement-UL nutrients (e.g., Magnesium 350 mg/day)
        if (eval.ulAppliesToSupplementsOnly && eval.dailyUl != null &&
            (eval.supplementSource > 0.0 || eval.todayIntake >= eval.dailyUl)
        ) {
            val suppNoteColor = when (eval.status) {
                MicronutrientStatus.TOXICITY_WARNING -> semantic.low
                MicronutrientStatus.APPROACHING_UL -> semantic.warn
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val suppNote = buildString {
                append("أكل: ${fmtMicro(eval.foodSource, unit)} $unit · مكملات: ${fmtMicro(eval.supplementSource, unit)} / حد المكملات ${fmtMicro(eval.dailyUl, unit)} $unit")
                if (eval.status == MicronutrientStatus.TOXICITY_WARNING) {
                    append(" — تجاوز حد المكملات اليومي")
                } else if (eval.todayIntake >= eval.dailyUl && eval.supplementSource < eval.dailyUl * 0.85) {
                    append(" (مصادر الأكل الطبيعي آمنة)")
                }
            }
            Text(
                text = suppNote,
                style = MaterialTheme.typography.labelSmall,
                color = suppNoteColor,
                fontWeight = if (eval.status == MicronutrientStatus.TOXICITY_WARNING || eval.status == MicronutrientStatus.APPROACHING_UL) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
                modifier = Modifier.padding(top = 2.dp)
            )
        } else if (!eval.isLimitKey && eval.hasDataToday && eval.remainingToday > 0.0 && eval.dailyPercentage < 100.0) {
            Text(
                text = "المتبقي اليوم: ${fmtMicro(eval.remainingToday, unit)} $unit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

private fun fmtMicro(value: Double, unit: String): String {
    if (unit == "IU" || value >= 100.0 || value % 1.0 == 0.0) {
        return value.roundToInt().toString()
    }
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.roundToInt().toString() else rounded.toString()
}
