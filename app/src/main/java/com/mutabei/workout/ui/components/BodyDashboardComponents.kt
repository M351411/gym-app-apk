package com.mutabei.workout.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mutabei.workout.data.model.BodyRecord
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.BodyTapeRecord
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private val EmeraldAccent = Color(0xFF22C55E)
private val PastelBlueAccent = Color(0xFF93B4FF)
private val AmberAccent = Color(0xFFF59E0B)
private val SoftRedAccent = Color(0xFFEF4444)

private fun fmt1(v: Double): String = String.format("%.1f", v)

/**
 * 1. Hero Latest InBody Dashboard Card:
 * - Animated 240° Radial Arc Gauge for InBody Score (or Body Composition Index fallback)
 * - 3 Highlight Stat Tiles (Weight, SMM, PBF) with 1dp subtle borders
 * - Detailed semantic range rows with BiDi-isolated English codes and tinted status pills
 */
@Composable
fun HeroInBodyDashboardCard(
    record: BodyRecord,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val scoreVal = record.score?.roundToInt()?.coerceIn(0, 100)
    val gaugeScore = scoreVal ?: when {
        record.pbfPercent != null && record.pbfPercent <= 18.0 -> 82
        record.pbfPercent != null && record.pbfPercent <= 24.0 -> 72
        else -> 65
    }
    val gaugeColor = when {
        gaugeScore >= 80 -> EmeraldAccent
        gaugeScore >= 70 -> PastelBlueAccent
        gaugeScore >= 60 -> AmberAccent
        else -> SoftRedAccent
    }
    val statusLabel = when {
        scoreVal != null && scoreVal >= 85 -> "ممتاز • لياقة عالية"
        scoreVal != null && scoreVal >= 75 -> "مستوى جيد جداً"
        scoreVal != null && scoreVal >= 65 -> if ((record.pbfPercent ?: 0.0) > 20.0) "مستوى جيد • يحتاج تنشيف" else "مستوى جيد • قابل للتطوير"
        scoreVal != null -> "يحتاج خطة التزام"
        (record.pbfPercent ?: 0.0) > 22.0 -> "تركيز على خفض الدهون"
        else -> "قياس حديث محفوظ"
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, subtleCardBorderColor()),
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_inbody_dashboard_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = gaugeColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.MonitorHeart,
                            contentDescription = null,
                            tint = gaugeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "ملخص آخر قياس للجسم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = isolateLtr("InBody Snapshot · ${record.date}"),
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Hero Row: Animated 240° Gauge + 3 Highlight Stat Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InBodyRadialArcGauge(
                    score = scoreVal,
                    fallbackFraction = gaugeScore / 100f,
                    arcColor = gaugeColor,
                    statusLabel = statusLabel,
                    size = 148.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroStatTile(
                        titleAr = "الوزن",
                        codeEn = "Weight",
                        valueText = record.weightKg?.let { "${isolateLtr(fmt1(it))} كجم" } ?: "—",
                        accentColor = PastelBlueAccent
                    )
                    HeroStatTile(
                        titleAr = "العضل الهيكلي",
                        codeEn = "SMM",
                        valueText = record.smmKg?.let { "${isolateLtr(fmt1(it))} كجم" } ?: "—",
                        accentColor = EmeraldAccent
                    )
                    HeroStatTile(
                        titleAr = "نسبة الدهون",
                        codeEn = "PBF",
                        valueText = record.pbfPercent?.let { isolateLtr("${fmt1(it)}%") } ?: "—",
                        accentColor = if ((record.pbfPercent ?: 0.0) > 22.0) AmberAccent else EmeraldAccent
                    )
                }
            }

            // Detailed Semantic Range Metrics
            data class MetricDetailRow(
                val rangeKey: String,
                val titleAr: String,
                val codeEn: String,
                val valueText: String,
                val rawValue: Double
            )

            val detailRows = listOfNotNull(
                record.bfmKg?.let { MetricDetailRow("bfm", "كتلة الدهون", "BFM", "${isolateLtr(fmt1(it))} كجم", it) },
                record.ffmKg?.let { MetricDetailRow("ffm", "الكتلة الخالية", "FFM", "${isolateLtr(fmt1(it))} كجم", it) },
                record.tbwL?.let { MetricDetailRow("tbw", "الماء الكلي", "TBW", "${isolateLtr(fmt1(it))} لتر", it) },
                record.proteinKg?.let { MetricDetailRow("pro", "البروتين", "Protein", "${isolateLtr(fmt1(it))} كجم", it) },
                record.mineralsKg?.let { MetricDetailRow("min", "المعادن", "Minerals", "${isolateLtr(fmt1(it))} كجم", it) },
                record.bmrKcal?.let { MetricDetailRow("bmr", "الحرق الأساسي", "BMR", "${isolateLtr("${it.roundToInt()}")} سعرة", it) },
                record.vfl?.let { MetricDetailRow("vfl", "الدهون الحشوية", "VFL", isolateLtr("${it.roundToInt()}"), it) },
                record.whr?.let { MetricDetailRow("whr", "نسبة الخصر للورك", "WHR", isolateLtr(String.format("%.2f", it)), it) },
                record.bmi?.let { MetricDetailRow("bmi", "مؤشر الكتلة", "BMI", isolateLtr(fmt1(it)), it) }
            )

            if (detailRows.isNotEmpty()) {
                HorizontalDivider(color = subtleCardBorderColor())
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    detailRows.forEachIndexed { idx, item ->
                        val status = record.rangeStatus(item.rangeKey, item.rawValue)
                        val statusColor = when (status) {
                            "ضمن المعدل" -> EmeraldAccent
                            "أعلى من المعدل" -> if (item.rangeKey in setOf("m", "ffm", "tbw", "pro", "min")) EmeraldAccent else SoftRedAccent
                            "أقل من المعدل" -> AmberAccent
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = item.titleAr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = isolateLtr(item.codeEn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (status != null) {
                                    StatusBadgePill(text = status, accentColor = statusColor)
                                }
                                Text(
                                    text = item.valueText,
                                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (idx < detailRows.lastIndex) {
                            HorizontalDivider(color = subtleCardBorderColor())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InBodyRadialArcGauge(
    score: Int?,
    fallbackFraction: Float,
    arcColor: Color,
    statusLabel: String,
    size: Dp = 148.dp
) {
    val targetFraction = (score?.let { it / 100f } ?: fallbackFraction).coerceIn(0f, 1f)
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(score) { mounted = true }
    val animatedFraction by animateFloatAsState(
        targetValue = if (mounted) targetFraction else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "inbody_gauge_sweep"
    )
    val trackColor = subtleCardBorderColor()

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 11.dp.toPx()
            val inset = strokePx / 2f + 2.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2f, this.size.height - inset * 2f)
            val topLeft = Offset(inset, inset)

            // 240-degree background track
            drawArc(
                color = trackColor,
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            // Soft outer glow
            if (animatedFraction > 0.01f) {
                drawArc(
                    color = arcColor.copy(alpha = 0.16f),
                    startAngle = 150f,
                    sweepAngle = 240f * animatedFraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx + 6.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            arcColor.copy(alpha = 0.72f),
                            arcColor,
                            arcColor.copy(alpha = 0.92f)
                        )
                    ),
                    startAngle = 150f,
                    sweepAngle = 240f * animatedFraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(top = 12.dp, start = 14.dp, end = 14.dp)
        ) {
            Text(
                text = isolateLtr("InBody"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = isolateLtr(score?.toString() ?: "—"),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 30.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (score != null) {
                    Text(
                        text = isolateLtr("/100"),
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = arcColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, arcColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = arcColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroStatTile(
    titleAr: String,
    codeEn: String,
    valueText: String,
    accentColor: Color
) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Column {
                    Text(
                        text = titleAr,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = isolateLtr(codeEn),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 2. Visual Segmental Lean & Symmetry Card:
 * - Stylized Human Body Silhouette Canvas tinted dynamically per zone (Emerald <3% diff, Amber >=3%)
 * - Paired Left vs. Right animated comparison bars for Arms & Legs + full-width Trunk bar
 * - Preserves Recomposition Quality delta section vs. previous scan
 */
@Composable
fun VisualSegmentalSymmetryCard(
    latest: BodyRecord,
    previous: BodyRecord?,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val ra = latest.segmentalLeanKg["ra"]
    val la = latest.segmentalLeanKg["la"]
    val tr = latest.segmentalLeanKg["tr"]
    val rl = latest.segmentalLeanKg["rl"]
    val ll = latest.segmentalLeanKg["ll"]

    val raPct = latest.segmentalLeanPercent["ra"]
    val laPct = latest.segmentalLeanPercent["la"]
    val trPct = latest.segmentalLeanPercent["tr"]
    val rlPct = latest.segmentalLeanPercent["rl"]
    val llPct = latest.segmentalLeanPercent["ll"]

    val hasArms = ra != null && la != null && ra > 0 && la > 0
    val hasLegs = rl != null && ll != null && rl > 0 && ll > 0
    val hasTrunk = tr != null && tr > 0
    val hasAnySegmental = hasArms || hasLegs || hasTrunk

    val prevSmm = previous?.smmKg
    val curSmm = latest.smmKg
    val prevBfm = previous?.bfmKg
    val curBfm = latest.bfmKg
    val hasQuality = previous != null && prevSmm != null && curSmm != null && prevBfm != null && curBfm != null

    if (!hasAnySegmental && !hasQuality) return

    val armDiffPct = if (hasArms) abs(ra!! - la!!) / max(ra, la).coerceAtLeast(0.1) * 100.0 else null
    val legDiffPct = if (hasLegs) abs(rl!! - ll!!) / max(rl, ll).coerceAtLeast(0.1) * 100.0 else null
    val armsBalanced = (armDiffPct ?: 0.0) < 3.0
    val legsBalanced = (legDiffPct ?: 0.0) < 3.0
    val overallBalanced = (!hasArms || armsBalanced) && (!hasLegs || legsBalanced)

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, subtleCardBorderColor()),
        modifier = modifier
            .fillMaxWidth()
            .testTag("body_symmetry_quality_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (hasAnySegmental) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = (if (overallBalanced) EmeraldAccent else AmberAccent).copy(alpha = 0.14f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AccessibilityNew,
                                contentDescription = null,
                                tint = if (overallBalanced) EmeraldAccent else AmberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "توزيع العضل وتناسق الأطراف",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = isolateLtr("Segmental Lean & Symmetry"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (hasArms || hasLegs) {
                        StatusBadgePill(
                            text = if (overallBalanced) "تناسق ممتاز ✓" else "تفاوت بسيط",
                            accentColor = if (overallBalanced) EmeraldAccent else AmberAccent
                        )
                    }
                }

                // Stylized Silhouette Canvas centered cleanly with zone callouts
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, subtleCardBorderColor()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SegmentalBodySilhouetteCanvas(
                            armsColor = if (!hasArms) PastelBlueAccent.copy(alpha = 0.4f) else if (armsBalanced) EmeraldAccent else AmberAccent,
                            trunkColor = if (!hasTrunk) PastelBlueAccent.copy(alpha = 0.4f) else if ((trPct ?: 100.0) >= 95.0) EmeraldAccent else PastelBlueAccent,
                            legsColor = if (!hasLegs) PastelBlueAccent.copy(alpha = 0.4f) else if (legsBalanced) EmeraldAccent else AmberAccent,
                            modifier = Modifier.size(width = 112.dp, height = 148.dp)
                        )
                    }
                }

                // Paired Left vs. Right Comparison Bars
                if (hasArms) {
                    SegmentalPairComparisonRow(
                        titleAr = "الذراعين",
                        codeEn = "Arms",
                        rightKg = ra!!,
                        leftKg = la!!,
                        rightPct = raPct,
                        leftPct = laPct
                    )
                }

                if (hasTrunk) {
                    TrunkLeanBarRow(
                        trunkKg = tr!!,
                        trunkPct = trPct
                    )
                }

                if (hasLegs) {
                    SegmentalPairComparisonRow(
                        titleAr = "الرجلين",
                        codeEn = "Legs",
                        rightKg = rl!!,
                        leftKg = ll!!,
                        rightPct = rlPct,
                        leftPct = llPct
                    )
                }
            }

            if (hasAnySegmental && hasQuality) {
                HorizontalDivider(color = subtleCardBorderColor())
            }

            if (hasQuality && previous != null && curSmm != null && prevSmm != null && curBfm != null && prevBfm != null) {
                val dSmm = curSmm - prevSmm
                val dBfm = curBfm - prevBfm
                val totalGain = dSmm + dBfm

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = semantic.ok,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "جودة التطور مقارنة بالقياس السابق",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = isolateLtr("${previous.date} → ${latest.date}"),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RecompDeltaTile(
                        labelAr = "تغيّر العضل",
                        codeEn = "SMM",
                        valueText = "${isolateLtr(String.format("%+.1f", dSmm))} كجم",
                        positiveIsGood = true,
                        rawDelta = dSmm,
                        modifier = Modifier.weight(1f)
                    )
                    RecompDeltaTile(
                        labelAr = "تغيّر الدهون",
                        codeEn = "BFM",
                        valueText = "${isolateLtr(String.format("%+.1f", dBfm))} كجم",
                        positiveIsGood = false,
                        rawDelta = dBfm,
                        modifier = Modifier.weight(1f)
                    )
                }

                val verdict = when {
                    dSmm > 0.1 && dBfm <= 0.0 ->
                        "Recomp ممتاز! زدت عضل صافي (${isolateLtr(String.format("+%.1f", dSmm))} كجم) ونزلت دهون (${isolateLtr(String.format("%.1f", dBfm))} كجم) في نفس الوقت."
                    dSmm > 0.1 && dBfm > 0.0 && totalGain > 0.1 -> {
                        val leanPct = ((dSmm / totalGain) * 100.0).roundToInt().coerceIn(0, 100)
                        if (leanPct >= 55) "جودة التضخيم ممتازة: ${isolateLtr("$leanPct%")} من الزيادة عضل صافي."
                        else "نسبة العضل ${isolateLtr("$leanPct%")} من الزيادة — فكّر تقلل فائض السعرات 150 سعر."
                    }
                    dBfm < -0.1 && dSmm >= -0.2 ->
                        "تنشيف نظيف جدًا! نزلت دهون وحافظت على الكتلة العضلية."
                    dBfm < -0.1 && dSmm < -0.2 ->
                        "خسرت دهون ومعاها شوية عضل (${isolateLtr(String.format("%.1f", dSmm))} كجم) — تأكد من البروتين العالي وشدة التمرين."
                    else -> "قياساتك مستقرة مقارنة بالقياس السابق."
                }
                Text(
                    text = verdict,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SegmentalBodySilhouetteCanvas(
    armsColor: Color,
    trunkColor: Color,
    legsColor: Color,
    modifier: Modifier = Modifier
) {
    val headColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Head
        drawCircle(
            color = headColor,
            radius = w * 0.105f,
            center = Offset(cx, h * 0.10f)
        )
        // Neck
        drawRoundRect(
            color = headColor,
            topLeft = Offset(cx - w * 0.045f, h * 0.18f),
            size = Size(w * 0.09f, h * 0.055f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Trunk glow + fill
        val trunkLeft = cx - w * 0.17f
        val trunkTop = h * 0.24f
        val trunkW = w * 0.34f
        val trunkH = h * 0.31f
        drawRoundRect(
            color = trunkColor.copy(alpha = 0.18f),
            topLeft = Offset(trunkLeft - 4.dp.toPx(), trunkTop - 3.dp.toPx()),
            size = Size(trunkW + 8.dp.toPx(), trunkH + 6.dp.toPx()),
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
        )
        drawRoundRect(
            color = trunkColor.copy(alpha = 0.85f),
            topLeft = Offset(trunkLeft, trunkTop),
            size = Size(trunkW, trunkH),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        )

        // Left & Right Arms
        val armW = w * 0.125f
        val armH = h * 0.31f
        val armTop = h * 0.255f
        val leftArmX = trunkLeft - armW - w * 0.055f
        val rightArmX = trunkLeft + trunkW + w * 0.055f
        listOf(leftArmX, rightArmX).forEach { ax ->
            drawRoundRect(
                color = armsColor.copy(alpha = 0.18f),
                topLeft = Offset(ax - 3.dp.toPx(), armTop - 2.dp.toPx()),
                size = Size(armW + 6.dp.toPx(), armH + 4.dp.toPx()),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )
            drawRoundRect(
                color = armsColor.copy(alpha = 0.88f),
                topLeft = Offset(ax, armTop),
                size = Size(armW, armH),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
        }

        // Left & Right Legs
        val legW = w * 0.14f
        val legH = h * 0.38f
        val legTop = h * 0.58f
        val leftLegX = cx - legW - w * 0.03f
        val rightLegX = cx + w * 0.03f
        listOf(leftLegX, rightLegX).forEach { lx ->
            drawRoundRect(
                color = legsColor.copy(alpha = 0.18f),
                topLeft = Offset(lx - 3.dp.toPx(), legTop - 2.dp.toPx()),
                size = Size(legW + 6.dp.toPx(), legH + 4.dp.toPx()),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )
            drawRoundRect(
                color = legsColor.copy(alpha = 0.88f),
                topLeft = Offset(lx, legTop),
                size = Size(legW, legH),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
        }
    }
}

@Composable
private fun SegmentalPairComparisonRow(
    titleAr: String,
    codeEn: String,
    rightKg: Double,
    leftKg: Double,
    rightPct: Double?,
    leftPct: Double?
) {
    val maxVal = max(rightKg, leftKg).coerceAtLeast(0.1)
    val diffPct = abs(rightKg - leftKg) / maxVal * 100.0
    val isBalanced = diffPct < 3.0
    val accent = if (isBalanced) EmeraldAccent else AmberAccent
    val strongerSide = if (rightKg >= leftKg) "اليمين" else "اليسار"

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(titleAr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = isolateLtr(codeEn),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadgePill(
                text = if (isBalanced) "متوازن ${isolateLtr("${fmt1(diffPct)}%")}"
                else "فرق ${isolateLtr("${fmt1(diffPct)}%")} · $strongerSide",
                accentColor = accent
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("يمين", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = buildString {
                            append("${isolateLtr(fmt1(rightKg))} كجم")
                            rightPct?.let { append(" · ${isolateLtr("${fmt1(it)}%")}") }
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold
                    )
                }
                MetricProgressBar(current = rightKg, target = maxVal * 1.08, barColor = accent, height = 7.dp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("يسار", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = buildString {
                            append("${isolateLtr(fmt1(leftKg))} كجم")
                            leftPct?.let { append(" · ${isolateLtr("${fmt1(it)}%")}") }
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold
                    )
                }
                MetricProgressBar(current = leftKg, target = maxVal * 1.08, barColor = accent, height = 7.dp)
            }
        }
        if (!isBalanced) {
            Text(
                text = "ابدأ التمارين الفردية بالجانب الأضعف وحدد العدات على أساسه.",
                style = MaterialTheme.typography.labelSmall,
                color = AmberAccent
            )
        }
    }
}

@Composable
private fun TrunkLeanBarRow(
    trunkKg: Double,
    trunkPct: Double?
) {
    val pctValue = trunkPct ?: 100.0
    val barColor = if (pctValue >= 95.0) EmeraldAccent else PastelBlueAccent
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("الجذع", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = isolateLtr("Trunk"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = buildString {
                    append("${isolateLtr(fmt1(trunkKg))} كجم")
                    trunkPct?.let { append(" · ${isolateLtr("${fmt1(it)}%")}") }
                },
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        MetricProgressBar(
            current = pctValue.coerceIn(0.0, 125.0),
            target = 120.0,
            barColor = barColor,
            height = 7.dp
        )
    }
}

@Composable
private fun RecompDeltaTile(
    labelAr: String,
    codeEn: String,
    valueText: String,
    positiveIsGood: Boolean,
    rawDelta: Double,
    modifier: Modifier = Modifier
) {
    val isGood = if (positiveIsGood) rawDelta >= 0.0 else rawDelta <= 0.0
    val color = if (abs(rawDelta) < 0.05) MaterialTheme.colorScheme.onSurfaceVariant else if (isGood) EmeraldAccent else AmberAccent
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(labelAr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(isolateLtr(codeEn), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

/**
 * 3. Smooth Bezier Curve Progress Chart (`Canvas`) with vertical gradient fill,
 * animated path reveal, dashed EMA trend overlay, interactive touch/drag point inspection,
 * and illustrated empty state when < 2 points exist.
 */
@Composable
fun SmoothBezierProgressChart(
    points: List<Pair<String, Double>>,
    emaSeries: List<Double>?,
    unitLabel: String,
    metricLabel: String,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val trendColor = EmeraldAccent
    val gridColor = subtleCardBorderColor()

    if (points.size < 2) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, subtleCardBorderColor()),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = primaryColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.ShowChart,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    text = if (points.isEmpty()) "لا توجد بيانات لـ $metricLabel بعد"
                    else "تم تسجيل قياس واحد (${isolateLtr(fmt1(points.first().second))} $unitLabel)",
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "سجّل قياسك القادم ليبدأ رسم منحنى التطور الذكي وتحليل الاتجاه الحقيقي.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var selectedIdx by remember(points) { mutableStateOf<Int?>(null) }
    var animStarted by remember(points) { mutableStateOf(false) }
    LaunchedEffect(points) { animStarted = true }
    val revealProgress by animateFloatAsState(
        targetValue = if (animStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "bezier_reveal"
    )

    val allVals = remember(points, emaSeries) {
        points.map { it.second } + emaSeries.orEmpty()
    }
    val rawMin = allVals.minOrNull() ?: 0.0
    val rawMax = allVals.maxOrNull() ?: 1.0
    val padSpan = ((rawMax - rawMin) * 0.15).coerceAtLeast(0.5)
    val minVal = rawMin - padSpan
    val maxVal = rawMax + padSpan
    val range = (maxVal - minVal).coerceAtLeast(0.1)

    val inspectedIdx = (selectedIdx ?: points.lastIndex).coerceIn(0, points.lastIndex)
    val inspectedPt = points[inspectedIdx]
    val inspectedEma = emaSeries?.getOrNull(inspectedIdx)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Interactive inspected-point readout strip
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, subtleCardBorderColor()),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (selectedIdx == null) "أحدث نقطة (${isolateLtr(inspectedPt.first)})"
                        else "نقطة محددة (${isolateLtr(inspectedPt.first)})",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${isolateLtr(fmt1(inspectedPt.second))} $unitLabel" +
                            (inspectedEma?.let { " · اتجاه ${isolateLtr(fmt1(it))}" } ?: ""),
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor
                    )
                }
                Text(
                    text = "اسحب للمعاينة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Smooth Bezier Canvas with touch & drag inspection
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                .pointerInput(points) {
                    fun pickAtX(xPx: Float) {
                        val pad = 16.dp.toPx()
                        val usableW = (size.width - pad * 2f).coerceAtLeast(1f)
                        val frac = ((xPx - pad) / usableW).coerceIn(0f, 1f)
                        val idx = (frac * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                        if (selectedIdx != idx) {
                            selectedIdx = idx
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                    detectTapGestures(onTap = { offset -> pickAtX(offset.x) })
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures { change, _ ->
                        val pad = 16.dp.toPx()
                        val usableW = (size.width - pad * 2f).coerceAtLeast(1f)
                        val frac = ((change.position.x - pad) / usableW).coerceIn(0f, 1f)
                        val idx = (frac * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                        if (selectedIdx != idx) {
                            selectedIdx = idx
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }
        ) {
            val padX = 16.dp.toPx()
            val padY = 18.dp.toPx()
            val usableW = size.width - padX * 2f
            val usableH = size.height - padY * 2f

            // Subtle horizontal gridlines
            for (i in 0..2) {
                val gy = padY + (usableH * i / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(padX, gy),
                    end = Offset(size.width - padX, gy),
                    strokeWidth = 1.dp.toPx()
                )
            }

            fun coordsOf(values: List<Double>): List<Offset> = values.mapIndexed { idx, v ->
                val x = padX + (idx.toFloat() / (values.size - 1).coerceAtLeast(1)) * usableW
                val y = padY + usableH - (((v - minVal) / range).toFloat() * usableH)
                Offset(x, y)
            }

            fun buildSmoothBezier(coords: List<Offset>): Path {
                val p = Path()
                if (coords.isEmpty()) return p
                p.moveTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.lastIndex) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val midX = (p0.x + p1.x) / 2f
                    p.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
                return p
            }

            val mainCoords = coordsOf(points.map { it.second })
            val bezierPath = buildSmoothBezier(mainCoords)
            val fillPath = Path().apply {
                addPath(bezierPath)
                lineTo(mainCoords.last().x, size.height - padY)
                lineTo(mainCoords.first().x, size.height - padY)
                close()
            }

            clipRect(right = padX + usableW * revealProgress + padX) {
                // Vertical gradient fill beneath curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.28f),
                            primaryColor.copy(alpha = 0.02f)
                        ),
                        startY = padY,
                        endY = size.height - padY
                    )
                )
                // Primary smooth Bezier curve
                drawPath(
                    path = bezierPath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                // Dashed EMA trend line
                if (emaSeries != null && emaSeries.size >= 2) {
                    val emaPath = buildSmoothBezier(coordsOf(emaSeries))
                    drawPath(
                        path = emaPath,
                        color = trendColor.copy(alpha = 0.85f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    )
                }
                // Data nodes
                mainCoords.forEachIndexed { idx, pt ->
                    val isSel = idx == inspectedIdx
                    if (isSel) {
                        drawLine(
                            color = primaryColor.copy(alpha = 0.35f),
                            start = Offset(pt.x, padY),
                            end = Offset(pt.x, size.height - padY),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawCircle(color = primaryColor.copy(alpha = 0.24f), radius = 10.dp.toPx(), center = pt)
                        drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
                        drawCircle(color = primaryColor, radius = 3.2.dp.toPx(), center = pt)
                    } else {
                        drawCircle(color = primaryColor, radius = 3.dp.toPx(), center = pt)
                    }
                }
            }
        }
    }
}

/**
 * 4. Interactive Target Body Fat Calculator:
 * - Material 3 Slider (8%..30%, 0.5% steps) + tactile `-` / `+` stepper buttons
 * - Real-time animated metrics with tabular numbers
 * - Primary CTA `تطبيق الخطة على سعراتي` syncing deficit directly with Nutrition tab
 */
@Composable
fun InteractiveTargetBodyFatCard(
    latest: BodyRecord,
    savedTargetBf: Double,
    currentTdee: Int?,
    onSaveTargetBf: (Double) -> Unit,
    onApplyPlanToCalories: (targetBf: Double, latestWeightKg: Double, fatToLoseKg: Double, weeklyLossKg: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val w = latest.weightKg ?: return
    val pbf = latest.pbfPercent ?: return
    val ffm = latest.ffmKg ?: (w * (1.0 - pbf / 100.0))
    val haptics = LocalHapticFeedback.current

    var sliderBf by remember(savedTargetBf) {
        mutableFloatStateOf(((savedTargetBf.coerceIn(8.0, 30.0) * 2.0).roundToInt() / 2.0).toFloat())
    }
    val targetBf = sliderBf.toDouble()
    val rawTargetWeight = ffm / (1.0 - targetBf / 100.0)
    val rawFatToLose = w - rawTargetWeight
    val weeklyLossKg = (w * 0.005).coerceAtLeast(0.25)
    val weeksNeeded = if (rawFatToLose > 0.1) ceil(rawFatToLose / weeklyLossKg).toInt() else 0
    val estDailyDeficit = if (rawFatToLose > 0.1) ((weeklyLossKg * 7700.0) / 7.0).roundToInt().coerceIn(250, 750) else 0
    val previewTargetKcal = currentTdee?.let { (it - estDailyDeficit).coerceAtLeast(1300) }

    val animTargetWeight by animateFloatAsState(
        targetValue = rawTargetWeight.toFloat(),
        animationSpec = tween(220),
        label = "sim_target_weight"
    )
    val animFatToLose by animateFloatAsState(
        targetValue = rawFatToLose.coerceAtLeast(0.0).toFloat(),
        animationSpec = tween(220),
        label = "sim_fat_lose"
    )

    val ctaInteraction = remember { MutableInteractionSource() }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, subtleCardBorderColor()),
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_target_bf_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.TrackChanges,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "حاسبة نسبة الدهون المستهدفة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = isolateLtr("Interactive Body Fat Simulator"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = isolateLtr("${fmt1(targetBf)}%"),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 26.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Slider flanked by haptic - / + steppers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, subtleCardBorderColor()),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            val next = (sliderBf - 0.5f).coerceIn(8f, 30f)
                            if (next != sliderBf) {
                                sliderBf = next
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                        .testTag("target_bf_minus")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Remove, contentDescription = "تقليل النسبة", modifier = Modifier.size(20.dp))
                    }
                }

                Slider(
                    value = sliderBf,
                    onValueChange = { raw ->
                        val snapped = ((raw * 2f).roundToInt() / 2f).coerceIn(8f, 30f)
                        if (snapped != sliderBf) {
                            sliderBf = snapped
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    valueRange = 8f..30f,
                    steps = 43,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = subtleCardBorderColor()
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("target_bf_slider")
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, subtleCardBorderColor()),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            val next = (sliderBf + 0.5f).coerceIn(8f, 30f)
                            if (next != sliderBf) {
                                sliderBf = next
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                        .testTag("target_bf_plus")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, contentDescription = "زيادة النسبة", modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Real-time animated output tiles
            if (rawFatToLose > 0.1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SimulatorResultTile(
                        label = "الوزن المستهدف",
                        value = "${isolateLtr(fmt1(animTargetWeight.toDouble()))} كجم",
                        accentColor = PastelBlueAccent,
                        modifier = Modifier.weight(1f)
                    )
                    SimulatorResultTile(
                        label = "دهون للنزول",
                        value = "${isolateLtr(fmt1(animFatToLose.toDouble()))} كجم",
                        accentColor = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, subtleCardBorderColor()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "المدة التقريبية: $weeksNeeded أسبوع",
                                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "بمعدل ${isolateLtr("0.5%")} أسبوعياً مع الحفاظ على ${isolateLtr(fmt1(ffm))} كجم كتلة خالية",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (previewTargetKcal != null) {
                            StatusBadgePill(
                                text = "${isolateLtr("$previewTargetKcal")} سعر",
                                accentColor = EmeraldAccent
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ممتاز! نسبة دهونك الحالية (${isolateLtr("${fmt1(pbf)}%")}) وصلت أو أقل من المستهدف (${isolateLtr("${fmt1(targetBf)}%")}) بفرق ${isolateLtr(fmt1(abs(rawFatToLose)))} كجم.",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldAccent,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Primary CTA syncing directly with Nutrition tab daily calorie target
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onApplyPlanToCalories(targetBf, w, rawFatToLose, weeklyLossKg)
                },
                interactionSource = ctaInteraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .tactilePressScale(ctaInteraction, pressedScale = 0.97f)
                    .testTag("apply_bf_plan_to_calories_button")
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("تطبيق الخطة على سعراتي", fontWeight = FontWeight.Bold)
            }

            if (abs(targetBf - savedTargetBf) >= 0.1) {
                TextButton(
                    onClick = { onSaveTargetBf(targetBf) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("save_target_bf_button")
                ) {
                    Text("حفظ النسبة المستهدفة فقط بدون تعديل السعرات")
                }
            }
        }
    }
}

@Composable
private fun SimulatorResultTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, subtleCardBorderColor()),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 19.sp,
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = FontWeight.Black,
                color = accentColor
            )
        }
    }
}

/**
 * 5. Optional Collapsible Body Tape Measurements Card (`قياسات المازورة`):
 * Tracks Waist, Chest, and Arm circumferences in cm alongside InBody scans.
 */
@Composable
fun BodyTapeMeasurementsCard(
    records: List<BodyTapeRecord>,
    onSave: (date: String, waistCm: Double?, chestCm: Double?, armCm: Double?) -> Unit,
    onDelete: (date: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val latest = records.lastOrNull()
    val prev = if (records.size >= 2) records[records.size - 2] else null
    val haptics = LocalHapticFeedback.current

    var waistText by remember(latest?.date) { mutableStateOf(latest?.waistCm?.let(::fmt1).orEmpty()) }
    var chestText by remember(latest?.date) { mutableStateOf(latest?.chestCm?.let(::fmt1).orEmpty()) }
    var armText by remember(latest?.date) { mutableStateOf(latest?.armCm?.let(::fmt1).orEmpty()) }

    val badge = latest?.waistCm?.let { "الوسط ${isolateLtr(fmt1(it))} سم" } ?: "اختياري"

    ExpandableSectionCard(
        title = "قياسات المازورة",
        subtitleEn = "Waist · Chest · Arm (cm)",
        statusBadgeText = badge,
        statusBadgeColor = if (latest != null) EmeraldAccent else MaterialTheme.colorScheme.primary,
        leadingIcon = Icons.Rounded.Straighten,
        initiallyExpanded = false,
        testTag = "body_tape_measurements_card"
    ) {
        // Latest summary tiles with delta vs previous
        if (latest != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TapeStatChip(
                    label = "الوسط",
                    current = latest.waistCm,
                    previous = prev?.waistCm,
                    lowerIsBetter = true,
                    modifier = Modifier.weight(1f)
                )
                TapeStatChip(
                    label = "الصدر",
                    current = latest.chestCm,
                    previous = prev?.chestCm,
                    lowerIsBetter = false,
                    modifier = Modifier.weight(1f)
                )
                TapeStatChip(
                    label = "الذراع",
                    current = latest.armCm,
                    previous = prev?.armCm,
                    lowerIsBetter = false,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = waistText,
                onValueChange = { waistText = it },
                label = { Text("الوسط (سم)", maxLines = 1) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).testTag("tape_waist_input")
            )
            OutlinedTextField(
                value = chestText,
                onValueChange = { chestText = it },
                label = { Text("الصدر (سم)", maxLines = 1) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).testTag("tape_chest_input")
            )
            OutlinedTextField(
                value = armText,
                onValueChange = { armText = it },
                label = { Text("الذراع (سم)", maxLines = 1) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).testTag("tape_arm_input")
            )
        }
        Spacer(Modifier.height(10.dp))
        val saveInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSave(
                    todayIso(),
                    waistText.replace(',', '.').trim().toDoubleOrNull(),
                    chestText.replace(',', '.').trim().toDoubleOrNull(),
                    armText.replace(',', '.').trim().toDoubleOrNull()
                )
            },
            interactionSource = saveInteraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .tactilePressScale(saveInteraction)
                .testTag("save_tape_measurements_button")
        ) {
            Text("حفظ قياس المازورة لليوم", fontWeight = FontWeight.Bold)
        }

        if (records.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = subtleCardBorderColor())
            Spacer(Modifier.height(6.dp))
            records.asReversed().take(5).forEach { rec ->
                val parts = buildList {
                    rec.waistCm?.let { add("وسط ${isolateLtr(fmt1(it))}") }
                    rec.chestCm?.let { add("صدر ${isolateLtr(fmt1(it))}") }
                    rec.armCm?.let { add("ذراع ${isolateLtr(fmt1(it))}") }
                }.joinToString(" · ")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = isolateLtr(rec.date),
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$parts سم",
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onDelete(rec.date) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "حذف قياس المازورة",
                            tint = SoftRedAccent.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TapeStatChip(
    label: String,
    current: Double?,
    previous: Double?,
    lowerIsBetter: Boolean,
    modifier: Modifier = Modifier
) {
    val delta = if (current != null && previous != null) current - previous else null
    val deltaColor = when {
        delta == null || abs(delta) < 0.05 -> MaterialTheme.colorScheme.onSurfaceVariant
        lowerIsBetter && delta < 0 -> EmeraldAccent
        !lowerIsBetter && delta > 0 -> EmeraldAccent
        else -> AmberAccent
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, subtleCardBorderColor()),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = current?.let { "${isolateLtr(fmt1(it))}" } ?: "—",
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.ExtraBold
            )
            if (delta != null && abs(delta) >= 0.05) {
                Text(
                    text = isolateLtr(String.format("%+.1f", delta)),
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = deltaColor
                )
            }
        }
    }
}
