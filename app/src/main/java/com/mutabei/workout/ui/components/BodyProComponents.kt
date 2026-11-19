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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutabei.workout.data.model.AiWeeklyCoachReport
import com.mutabei.workout.data.model.BodyRecord
import com.mutabei.workout.ui.theme.LocalSemanticColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun BodySymmetryAndQualityCard(
    latest: BodyRecord,
    previous: BodyRecord?
) {
    val semantic = LocalSemanticColors.current
    val ra = latest.segmentalLeanKg["ra"]
    val la = latest.segmentalLeanKg["la"]
    val rl = latest.segmentalLeanKg["rl"]
    val ll = latest.segmentalLeanKg["ll"]
    val hasSymmetry = (ra != null && la != null && ra > 0 && la > 0) || (rl != null && ll != null && rl > 0 && ll > 0)

    val prevSmm = previous?.smmKg
    val curSmm = latest.smmKg
    val prevBfm = previous?.bfmKg
    val curBfm = latest.bfmKg
    val hasQuality = previous != null && prevSmm != null && curSmm != null && prevBfm != null && curBfm != null

    if (!hasSymmetry && !hasQuality) return

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("body_symmetry_quality_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hasSymmetry) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Balance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "تناسق العضلات (يمين vs يسار)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (ra != null && la != null && ra > 0 && la > 0) {
                    SymmetryPairRow(label = "الذراعين", rightKg = ra, leftKg = la)
                }
                if (rl != null && ll != null && rl > 0 && ll > 0) {
                    SymmetryPairRow(label = "الرجلين", rightKg = rl, leftKg = ll)
                }
            }

            if (hasSymmetry && hasQuality) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }

            if (hasQuality && previous != null && curSmm != null && prevSmm != null && curBfm != null && prevBfm != null) {
                val dSmm = curSmm - prevSmm
                val dBfm = curBfm - prevBfm
                val totalGain = dSmm + dBfm

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = semantic.ok,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "جودة التطور (من ${previous.date} إلى ${latest.date})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeltaMetricPill(
                        label = "تغيّر العضل SMM",
                        value = "${String.format("%+.1f", dSmm)} كجم",
                        positiveIsGood = true,
                        rawDelta = dSmm,
                        modifier = Modifier.weight(1f)
                    )
                    DeltaMetricPill(
                        label = "تغيّر الدهون BFM",
                        value = "${String.format("%+.1f", dBfm)} كجم",
                        positiveIsGood = false,
                        rawDelta = dBfm,
                        modifier = Modifier.weight(1f)
                    )
                }

                val verdict = when {
                    dSmm > 0.1 && dBfm <= 0.0 ->
                        "Recomp ممتاز! زدت عضل صافي (${String.format("+%.1f", dSmm)} كجم) ونزلت دهون (${String.format("%.1f", dBfm)} كجم) في نفس الوقت."
                    dSmm > 0.1 && dBfm > 0.0 && totalGain > 0.1 -> {
                        val leanPct = ((dSmm / totalGain) * 100.0).roundToInt().coerceIn(0, 100)
                        if (leanPct >= 55) {
                            "جودة التضخيم ممتازة: $leanPct% من الزيادة عضل صافي."
                        } else {
                            "نسبة العضل $leanPct% من الزيادة والباقي دهون — فكّر تقلل فائض السعرات 150 سعر."
                        }
                    }
                    dBfm < -0.1 && dSmm >= -0.2 ->
                        "تنشيف نظيف جدًا! نزلت دهون وحافظت على الكتلة العضلية."
                    dBfm < -0.1 && dSmm < -0.2 ->
                        "خسرت دهون ومعاها شوية عضل (${String.format("%.1f", dSmm)} كجم) — تأكد من البروتين العالي وشدة التمرين."
                    else ->
                        "قياساتك مستقرة مقارنة بالقياس السابق."
                }
                Text(
                    text = verdict,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SymmetryPairRow(
    label: String,
    rightKg: Double,
    leftKg: Double
) {
    val semantic = LocalSemanticColors.current
    val maxVal = max(rightKg, leftKg).coerceAtLeast(0.1)
    val diffKg = abs(rightKg - leftKg)
    val diffPct = (diffKg / maxVal) * 100.0
    val isImbalanced = diffPct > 5.0
    val strongerSide = if (rightKg >= leftKg) "اليمين" else "اليسار"
    val accent = if (isImbalanced) semantic.warn else semantic.ok

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = accent.copy(alpha = 0.14f)
            ) {
                Text(
                    text = if (isImbalanced) {
                        "فرق ${String.format("%.1f", diffPct)}% ($strongerSide أقوى)"
                    } else {
                        "متوازن (${String.format("%.1f", diffPct)}%)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("يمين: ${String.format("%.2f", rightKg)} كجم", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("يسار: ${String.format("%.2f", leftKg)} كجم", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isImbalanced) {
            Text(
                text = "ابدأ التمارين الفردية (Unilateral) بالجانب الأضعف وحدد العدات على أساسه.",
                style = MaterialTheme.typography.labelSmall,
                color = semantic.warn
            )
        }
    }
}

@Composable
private fun DeltaMetricPill(
    label: String,
    value: String,
    positiveIsGood: Boolean,
    rawDelta: Double,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val isGood = if (positiveIsGood) rawDelta >= 0.0 else rawDelta <= 0.0
    val color = if (abs(rawDelta) < 0.05) MaterialTheme.colorScheme.onSurfaceVariant else if (isGood) semantic.ok else semantic.warn
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun AiWeeklyCoachDialog(
    busyMessage: String?,
    errorMessage: String?,
    report: AiWeeklyCoachReport?,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("المدرب الذكي الشامل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (report == null && busyMessage == null && errorMessage == null) {
                    Text(
                        text = "هقرأ بياناتك الحقيقية من التمرين (حجم كل عضلة وتطور الأوزان) + التغذية (السعرات، البروتين، الليوسين، ونقص الميكروز) + آخر قياس InBody وأطلعلك تقرير أسبوعي عملي بالأرقام.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (report != null) {
                    val scoreColor = when {
                        report.overallScore >= 80 -> semantic.ok
                        report.overallScore >= 60 -> semantic.warn
                        else -> semantic.low
                    }
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = scoreColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, scoreColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "${report.overallScore}/100",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreColor
                            )
                            Text(
                                text = report.headline,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (report.wins.isNotEmpty()) {
                        CoachBulletSection(
                            title = "نقاط القوة هذا الأسبوع",
                            icon = Icons.Rounded.CheckCircle,
                            tint = semantic.ok,
                            items = report.wins
                        )
                    }
                    if (report.attentionNeeded.isNotEmpty()) {
                        CoachBulletSection(
                            title = "نقاط تحتاج انتباه",
                            icon = Icons.Rounded.Warning,
                            tint = semantic.warn,
                            items = report.attentionNeeded
                        )
                    }
                    if (report.nextWeekActions.isNotEmpty()) {
                        CoachBulletSection(
                            title = "خطة الأسبوع القادم",
                            icon = Icons.Rounded.Flag,
                            tint = MaterialTheme.colorScheme.primary,
                            items = report.nextWeekActions
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))
                Button(
                    onClick = onGenerate,
                    enabled = busyMessage == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_ai_coach_report_button")
                ) {
                    Text(if (report == null) "ابدأ التحليل الشامل بـ Gemini" else "تحديث التحليل")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
private fun CoachBulletSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    items: List<String>
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = tint)
            }
            items.forEach { bullet ->
                Text("• $bullet", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
