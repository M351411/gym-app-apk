package com.mutabei.workout.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mutabei.workout.data.model.ExerciseSpec
import com.mutabei.workout.data.model.SetEntry
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.ExerciseHistoryPoint
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
