package com.mutabei.workout.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mutabei.workout.R
import com.mutabei.workout.data.model.WorkoutDefaults
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.RestTimerState

/**
 * Wraps an English or numeric token in Unicode Left-to-Right Isolate (LRI ... PDI)
 * anchored with Left-to-Right Marks (LRM) so mixed Arabic RTL strings NEVER flip
 * parentheses, colons, or swap numeric ratios (e.g. `0 / 12`, `25 / 67`) on any Android version.
 */
fun isolateLtr(text: String): String = "\u2066\u200E$text\u200E\u2069"

/**
 * Unified counter formatting: `[Current] / [Target] [Unit]` with strict LTR isolation for the numbers.
 * Example: `formatCounterLtr("25", "67", "مجموعة")` -> `\u2066\u200E25 / 67\u200E\u2069 مجموعة`.
 */
fun formatCounterLtr(current: String, target: String, unit: String = ""): String {
    val ratio = isolateLtr("$current / $target")
    return if (unit.isBlank()) ratio else "$ratio $unit"
}

/**
 * Subtle elevated border color suited for flagship Dark Mode (`Color.White.copy(alpha = 0.08f)`)
 * while staying crisp in Light Mode.
 */
@Composable
fun subtleCardBorderColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.35f
    return if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
}

/**
 * Tactile micro-interaction: smoothly scales down to 0.96f on press with spring physics.
 */
fun Modifier.tactilePressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "tactile_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Compact pill-shaped status badge used on collapsed accordions and cards.
 */
@Composable
fun StatusBadgePill(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

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

        drawCircle(
            color = baseColor,
            radius = 4f * sx,
            center = Offset(20f * sx, 5f * sy)
        )
        drawRoundRect(
            color = baseColor,
            topLeft = Offset(13f * sx, 11f * sy),
            size = Size(14f * sx, 19f * sy),
            cornerRadius = CornerRadius(4f * sx, 4f * sy)
        )
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

/**
 * Animated Circular Calorie Ring with smooth gradient sweep and tabular center counter.
 */
@Composable
fun AnimatedCalorieRing(
    consumedKcal: Double,
    targetKcal: Int,
    ringColor: Color,
    isOver: Boolean,
    size: Dp = 116.dp,
    modifier: Modifier = Modifier
) {
    val rawFraction = if (targetKcal > 0) (consumedKcal / targetKcal.toDouble()).coerceIn(0.0, 1.0).toFloat() else 0f
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val animatedSweep by animateFloatAsState(
        targetValue = if (started) rawFraction else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "calorie_ring_sweep"
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val remAbs = kotlin.math.abs(targetKcal - consumedKcal).toInt()

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 10.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedSweep > 0.005f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            ringColor.copy(alpha = 0.72f),
                            ringColor,
                            ringColor.copy(alpha = 0.92f)
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = isolateLtr("$remAbs"),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = FontWeight.Black,
                color = if (isOver) ringColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isOver) "زيادة" else "متبقي",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Smoothly animated segmented glow progress bar that always fills Start -> End (Right -> Left in RTL).
 */
@Composable
fun MetricProgressBar(
    current: Double,
    target: Double,
    barColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val targetFraction = if (target > 0.0) (current / target).coerceIn(0.0, 1.0).toFloat() else 0f
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mounted = true }
    val animatedFraction by animateFloatAsState(
        targetValue = if (mounted) targetFraction else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "metric_progress_fill"
    )
    val fillBrush = remember(barColor) {
        Brush.horizontalGradient(
            colors = listOf(barColor.copy(alpha = 0.78f), barColor)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        contentAlignment = Alignment.CenterStart
    ) {
        if (animatedFraction > 0.002f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(height)
                    .clip(RoundedCornerShape(99.dp))
                    .background(fillBrush)
            )
        }
    }
}

private val TRAILING_EN_PAREN_REGEX = Regex("""^(.*?)\s*\(([A-Za-z0-9\s\-&/+.,·]+)\)\s*$""")

/**
 * Flagship BiDi-safe Accordion Card:
 * - Splits mixed Arabic + English parentheses into a clean 2-line Column (Arabic Primary + LTR-isolated English Subtitle)
 * - Supports a compact pill-shaped status badge visible when collapsed
 * - Uses physics-based `animateContentSize` + smooth 180° chevron rotation
 */
@Composable
fun ExpandableSectionCard(
    title: String,
    subtitleEn: String? = null,
    statusBadgeText: String? = null,
    statusBadgeColor: Color? = null,
    leadingIcon: ImageVector? = null,
    initiallyExpanded: Boolean = false,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "accordion_chevron"
    )

    // Auto-extract trailing English parenthetical if caller didn't pass subtitleEn explicitly
    val (displayTitle, displaySubtitle) = remember(title, subtitleEn) {
        if (!subtitleEn.isNullOrBlank()) {
            title to subtitleEn
        } else {
            val m = TRAILING_EN_PAREN_REGEX.matchEntire(title.trim())
            if (m != null && m.groupValues[1].isNotBlank()) {
                m.groupValues[1].trim() to m.groupValues[2].trim()
            } else {
                title to null
            }
        }
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, subtleCardBorderColor()),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.35f
        val subtitleColor = if (isDark) Color(0xFF9CA3AF) else MaterialTheme.colorScheme.onSurfaceVariant

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (leadingIcon != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = (statusBadgeColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.14f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                tint = statusBadgeColor ?: MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!displaySubtitle.isNullOrBlank()) {
                        Text(
                            text = isolateLtr(displaySubtitle),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            ),
                            color = subtitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!statusBadgeText.isNullOrBlank()) {
                    StatusBadgePill(
                        text = statusBadgeText,
                        accentColor = statusBadgeColor ?: MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "إخفاء" else "عرض",
                    tint = subtitleColor,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(chevronRotation)
                )
            }
            if (expanded) {
                Column(modifier = Modifier.padding(top = 12.dp), content = content)
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
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
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
                overflow = TextOverflow.Ellipsis
            )
        }
        OutlinedButton(
            onClick = onGalleryClick,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
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
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RestTimerFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        interactionSource = interactionSource,
        modifier = modifier
            .tactilePressScale(interactionSource)
            .testTag("rest_timer_fab")
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
    val isPreAlert = !state.finished && state.remainingSeconds in 1..5
    val ringColor = when {
        state.finished -> semantic.ok
        isPreAlert -> semantic.warn
        else -> MaterialTheme.colorScheme.primary
    }
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
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
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(34.dp)
                    .testTag("timer_skip")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .then(
                            if (state.finished) {
                                Modifier
                                    .background(semantic.ok.copy(alpha = 0.14f))
                                    .clickable(onClick = onDismiss)
                                    .semantics { contentDescription = "يلا - إغلاق مؤقت الراحة" }
                                    .testTag("timer_go_dismiss")
                            } else {
                                Modifier
                            }
                        ),
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
                    if (state.finished) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "يلا",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = semantic.ok
                            )
                            Text(
                                text = "اضغط",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.ok.copy(alpha = 0.85f)
                            )
                        }
                    } else {
                        val m = state.remainingSeconds / 60
                        val s = state.remainingSeconds % 60
                        Text(
                            text = isolateLtr("$m:${s.toString().padStart(2, '0')}"),
                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPreAlert) semantic.warn else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = when {
                            state.finished -> "الراحة خلصت. اضغط «يلا» للمجموعة الجاية!"
                            isPreAlert -> "جهّز نفسك للمجموعة الجاية..."
                            else -> "استريح"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = when {
                            state.finished -> semantic.ok
                            isPreAlert -> semantic.warn
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(end = 28.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onAdjust(-30) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("timer_minus_30")
                        ) { Text(isolateLtr("-30")) }
                        OutlinedButton(
                            onClick = { onAdjust(30) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("timer_plus_30")
                        ) { Text(isolateLtr("+30")) }
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
}

