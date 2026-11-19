package com.mutabei.workout.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutabei.workout.R
import com.mutabei.workout.data.model.WorkoutDefaults
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.RestTimerState

@Composable
fun MuscleSilhouetteCanvas(
    muscleKey: String,
    modifier: Modifier = Modifier
) {
    val meta = WorkoutDefaults.MUSCLE_GROUPS[muscleKey] ?: WorkoutDefaults.MUSCLE_GROUPS["back"]!!
    val baseColor = MaterialTheme.colorScheme.outline
    val highlightColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .size(width = 36.dp, height = 50.dp)
            .semantics { contentDescription = meta.arabicLabel }
    ) {
        val sx = size.width / 40f
        val sy = size.height / 56f

        // Head
        drawCircle(
            color = baseColor,
            radius = 4f * sx,
            center = Offset(20f * sx, 5f * sy)
        )
        // Torso
        drawRoundRect(
            color = baseColor,
            topLeft = Offset(13f * sx, 11f * sy),
            size = Size(14f * sx, 19f * sy),
            cornerRadius = CornerRadius(4f * sx, 4f * sy)
        )
        // Left & Right Arms
        drawRoundRect(
            color = baseColor,
            topLeft = Offset(6f * sx, 12f * sy),
            size = Size(5f * sx, 18f * sy),
            cornerRadius = CornerRadius(2.5f * sx, 2.5f * sy)
        )
        drawRoundRect(
            color = baseColor,
            topLeft = Offset(29f * sx, 12f * sy),
            size = Size(5f * sx, 18f * sy),
            cornerRadius = CornerRadius(2.5f * sx, 2.5f * sy)
        )
        // Left & Right Legs
        drawRoundRect(
            color = baseColor,
            topLeft = Offset(13f * sx, 31f * sy),
            size = Size(6f * sx, 22f * sy),
            cornerRadius = CornerRadius(3f * sx, 3f * sy)
        )
        drawRoundRect(
            color = baseColor,
            topLeft = Offset(21f * sx, 31f * sy),
            size = Size(6f * sx, 22f * sy),
            cornerRadius = CornerRadius(3f * sx, 3f * sy)
        )
        // Highlight ellipses
        meta.ellipses.forEach { e ->
            drawOval(
                color = highlightColor,
                topLeft = Offset((e.cx - e.rx) * sx, (e.cy - e.ry) * sy),
                size = Size(e.rx * 2f * sx, e.ry * 2f * sy)
            )
        }
    }
}

@Composable
fun DualLineProgressChart(
    primarySeries: List<Double>,
    secondarySeries: List<Double>? = null,
    unitLabel: String = "كجم",
    legendText: String? = null,
    modifier: Modifier = Modifier
) {
    if (primarySeries.size < 2) {
        Text(
            text = "محتاج جلستين على الأقل عشان يظهر الرسم.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 6.dp)
        )
        return
    }

    val all = if (secondarySeries != null) primarySeries + secondarySeries else primarySeries
    val minVal = all.minOrNull() ?: 0.0
    val maxVal = all.maxOrNull() ?: 1.0
    val range = (maxVal - minVal).takeIf { it > 0.0001 } ?: 1.0

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .padding(vertical = 6.dp)
        ) {
            val pad = 12f
            val w = size.width - pad * 2f
            val h = size.height - pad * 2f

            // Subtle baseline & midline
            drawLine(gridColor, Offset(pad, pad), Offset(size.width - pad, pad), strokeWidth = 1f)
            drawLine(gridColor, Offset(pad, pad + h / 2f), Offset(size.width - pad, pad + h / 2f), strokeWidth = 1f)
            drawLine(gridColor, Offset(pad, size.height - pad), Offset(size.width - pad, size.height - pad), strokeWidth = 1f)

            fun buildPath(values: List<Double>): Path {
                val path = Path()
                values.forEachIndexed { idx, v ->
                    val x = pad + (idx.toFloat() / (values.size - 1).coerceAtLeast(1)) * w
                    val y = size.height - pad - ((v - minVal) / range).toFloat() * h
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                return path
            }

            if (secondarySeries != null && secondarySeries.size >= 2) {
                drawPath(
                    path = buildPath(secondarySeries),
                    color = secondaryColor,
                    style = Stroke(
                        width = 4f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f)
                    )
                )
            }

            drawPath(
                path = buildPath(primarySeries),
                color = primaryColor,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            // Draw data dots for primary series
            primarySeries.forEachIndexed { idx, v ->
                val x = pad + (idx.toFloat() / (primarySeries.size - 1).coerceAtLeast(1)) * w
                val y = size.height - pad - ((v - minVal) / range).toFloat() * h
                drawCircle(color = primaryColor, radius = 6f, center = Offset(x, y))
            }
        }
        val minText = String.format("%.1f", minVal)
        val maxText = String.format("%.1f", maxVal)
        Text(
            text = "من $minText إلى $maxText $unitLabel" + (legendText?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MetricProgressBar(
    current: Double,
    target: Double,
    barColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val fraction = if (target > 0.0) (current / target).coerceIn(0.0, 1.0).toFloat() else 0f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(barColor)
        )
    }
}

@Composable
fun ExpandableSectionCard(
    title: String,
    initiallyExpanded: Boolean = false,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "إخفاء" else "عرض",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp), content = content)
            }
        }
    }
}

@Composable
fun CameraOrGalleryButtons(
    cameraLabel: String,
    galleryLabel: String = "من المعرض",
    enabled: Boolean = true,
    cameraTestTag: String,
    galleryTestTag: String,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onCameraClick,
            enabled = enabled,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            modifier = Modifier
                .weight(1f)
                .testTag(cameraTestTag)
        ) {
            Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = stringResource(R.string.cd_camera),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = cameraLabel,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        OutlinedButton(
            onClick = onGalleryClick,
            enabled = enabled,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            modifier = Modifier
                .weight(1f)
                .testTag(galleryTestTag)
        ) {
            Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = stringResource(R.string.cd_gallery),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = galleryLabel,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RestTimerFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.testTag("rest_timer_fab")
    ) {
        Icon(
            imageVector = Icons.Rounded.Timer,
            contentDescription = stringResource(R.string.cd_rest_timer)
        )
    }
}

@Composable
fun RestTimerOverlay(
    state: RestTimerState,
    defaultPresetSeconds: Int,
    onAdjust: (Int) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.visible) return
    val semantic = LocalSemanticColors.current
    val ringColor = if (state.finished) semantic.ok else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outline
    val progress = if (state.totalSeconds > 0) {
        (state.remainingSeconds.toFloat() / state.totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, ringColor),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("rest_timer_overlay")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(86.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = stroke
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = if (state.finished) 360f else 360f * progress,
                        useCenter = false,
                        style = stroke
                    )
                }
                val timeLabel = if (state.finished) {
                    "يلا"
                } else {
                    val m = state.remainingSeconds / 60
                    val s = state.remainingSeconds % 60
                    "$m:${s.toString().padStart(2, '0')}"
                }
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (state.finished) semantic.ok else MaterialTheme.colorScheme.onSurface
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (state.finished) "الراحة خلصت. ابدأ المجموعة!" else "استريح",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (state.finished) semantic.ok else MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { onAdjust(-30) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("timer_minus_30")
                    ) { Text("-30") }
                    OutlinedButton(
                        onClick = { onAdjust(30) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("timer_plus_30")
                    ) { Text("+30") }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("timer_skip")
                    ) { Text("تخطي") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(120, 180, 240, 300).forEach { sec ->
                        val active = sec == defaultPresetSeconds
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .clickable { onSelectPreset(sec) }
                                .testTag("timer_preset_${sec / 60}")
                        ) {
                            Text(
                                text = "${sec / 60} د",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
