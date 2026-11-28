package com.mutabei.workout.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mutabei.workout.ui.components.BodyTapeMeasurementsCard
import com.mutabei.workout.ui.components.CameraOrGalleryButtons
import com.mutabei.workout.ui.components.ExpandableSectionCard
import com.mutabei.workout.ui.components.HeroInBodyDashboardCard
import com.mutabei.workout.ui.components.InteractiveTargetBodyFatCard
import com.mutabei.workout.ui.components.SmoothBezierProgressChart
import com.mutabei.workout.ui.components.StatusBadgePill
import com.mutabei.workout.ui.components.VisualSegmentalSymmetryCard
import com.mutabei.workout.ui.components.isolateLtr
import com.mutabei.workout.ui.components.subtleCardBorderColor
import com.mutabei.workout.ui.components.tactilePressScale
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.util.rememberImageCaptureController
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.ui.viewmodel.AppDataSnapshot
import com.mutabei.workout.ui.viewmodel.BodyDraftState
import com.mutabei.workout.ui.viewmodel.MainTab
import com.mutabei.workout.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private data class BodyMetricSpec(
    val key: String,
    val label: String,
    val unit: String
)

private val CHART_METRICS = listOf(
    BodyMetricSpec("w", "الوزن", "كجم"),
    BodyMetricSpec("f", "نسبة الدهون", "%"),
    BodyMetricSpec("m", "العضل", "كجم"),
    BodyMetricSpec("bfm", "كتلة الدهون", "كجم"),
    BodyMetricSpec("ffm", "الكتلة الخالية", "كجم"),
    BodyMetricSpec("bmr", "الحرق الأساسي", "سعرة"),
    BodyMetricSpec("vfl", "دهون حشوية", "")
)

private val BASIC_BODY_FIELDS = listOf(
    "w" to "الوزن (كجم)",
    "m" to "العضل ${isolateLtr("SMM")} (كجم)",
    "f" to "الدهون ${isolateLtr("PBF")} (%)",
    "bfm" to "كتلة الدهون ${isolateLtr("BFM")}",
    "tbw" to "الماء ${isolateLtr("TBW")} (لتر)",
    "bmr" to "الحرق ${isolateLtr("BMR")}",
    "vfl" to "الحشوية ${isolateLtr("VFL")}",
    "score" to "درجة ${isolateLtr("InBody")}"
)

private val ADVANCED_BODY_FIELDS = listOf(
    "ffm" to "الخالية ${isolateLtr("FFM")} (كجم)",
    "pro" to "البروتين (كجم)",
    "min" to "المعادن (كجم)",
    "whr" to "الخصر/الورك ${isolateLtr("WHR")}",
    "bmi" to "مؤشر الكتلة ${isolateLtr("BMI")}",
    "h" to "الطول (سم)",
    "tgt" to "الوزن المستهدف",
    "fc" to "تعديل الدهون (كجم)",
    "mc" to "تعديل العضل (كجم)"
)

private val SEGMENT_FIELDS = listOf(
    "la" to "ذراع يسار (كجم)",
    "ra" to "ذراع يمين (كجم)",
    "tr" to "الجذع (كجم)",
    "ll" to "رجل يسار (كجم)",
    "rl" to "رجل يمين (كجم)"
)

private val SoftDeleteRed = Color(0xFFEF4444)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BodyScreen(
    snapshot: AppDataSnapshot,
    bodyDraft: BodyDraftState,
    viewModel: MainViewModel
) {
    val semantic = LocalSemanticColors.current
    val haptics = LocalHapticFeedback.current
    val sortedRecords = remember(snapshot.bodyRecords) {
        snapshot.bodyRecords.sortedBy { it.date }
    }
    val latestRecord = sortedRecords.lastOrNull()
    val prevRecord = if (sortedRecords.size >= 2) sortedRecords[sortedRecords.size - 2] else null
    val tapeRecords by viewModel.bodyTapeRecords.collectAsStateWithLifecycle()
    val defaultStepTarget by viewModel.defaultStepTarget.collectAsStateWithLifecycle()
    val defaultStepBuffer by viewModel.defaultStepBufferFactor.collectAsStateWithLifecycle()
    val autoStepsStatus by viewModel.autoStepsStatus.collectAsStateWithLifecycle()
    val dailyContext = remember(snapshot, defaultStepTarget, defaultStepBuffer, autoStepsStatus.liveTodaySteps) {
        viewModel.computeDailyContext(
            snapshot = snapshot,
            date = todayIso(),
            defaultStepTarget = defaultStepTarget,
            defaultBufferFactor = defaultStepBuffer,
            liveAutoStepsToday = autoStepsStatus.liveTodaySteps
        )
    }
    val currentTargets = dailyContext.baseTargets

    var showInBodySheet by remember { mutableStateOf(false) }
    var deleteDialogDate by remember { mutableStateOf<String?>(null) }

    // Safe Delete Confirmation AlertDialog
    if (deleteDialogDate != null) {
        val targetDate = deleteDialogDate!!
        AlertDialog(
            onDismissRequest = { deleteDialogDate = null },
            title = {
                Text(
                    text = "تأكيد حذف القياس؟",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف قياس الجسم بتاريخ ${isolateLtr(targetDate)}؟ لا يمكن التراجع عن هذا الإجراء.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteDialogDate = null
                        viewModel.deleteBodyRecord(targetDate)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftDeleteRed),
                    modifier = Modifier.testTag("confirm_delete_body_button")
                ) {
                    Text("حذف القياس", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteDialogDate = null },
                    modifier = Modifier.testTag("cancel_delete_body_button")
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // InBody Input Form inside Material 3 ModalBottomSheet (skipPartiallyExpanded = true)
    if (showInBodySheet) {
        InBodyEntryModalSheet(
            bodyDraft = bodyDraft,
            sortedDates = sortedRecords.map { it.date },
            viewModel = viewModel,
            onDismiss = { showInBodySheet = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Prominent Top Primary CTA to open InBody / AI Scan ModalBottomSheet
            item(key = "add_inbody_cta") {
                val ctaInteraction = remember { MutableInteractionSource() }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactilePressScale(ctaInteraction, pressedScale = 0.97f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(
                            interactionSource = ctaInteraction,
                            indication = null
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showInBodySheet = true
                        }
                        .testTag("open_inbody_sheet_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "+ تسجيل قياس جديد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = isolateLtr("InBody · AI Vision Scan"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.AddAPhoto,
                            contentDescription = "مسح ورقة InBody",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 1b. Resting BMR Reference Strip (BMR = 1,600 kcal baseline or latest InBody BMR) -> linked to Steps & Burn
            item(key = "resting_bmr_reference") {
                val nf = remember { NumberFormat.getIntegerInstance(Locale.US) }
                val stepsBurn = dailyContext.stepsBurn
                val bmrAccent = Color(0xFF14B8A6)
                val bmrSubline = if (dailyContext.isBmrFromInBody) {
                    "حرق الراحة الأساسي (InBody) · TDEE ~${nf.format(stepsBurn.estimatedTdeeKcal)} kcal"
                } else {
                    "حرق الراحة المرجعي · TDEE ~${nf.format(stepsBurn.estimatedTdeeKcal)} kcal"
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, subtleCardBorderColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(MainTab.STEPS)
                        }
                        .testTag("body_bmr_reference_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bmrAccent.copy(alpha = 0.14f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = bmrAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = isolateLtr("BMR = ${nf.format(dailyContext.bmrReferenceKcal)} kcal"),
                                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = bmrSubline,
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = bmrAccent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "الخطوات والحرق",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = bmrAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "فتح الخطوات والحرق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. Hero Latest InBody Dashboard (or welcoming empty state)
            if (latestRecord != null) {
                item(key = "hero_latest_inbody") {
                    HeroInBodyDashboardCard(record = latestRecord)
                }

                // 3. Visual Segmental Lean & Symmetry Card
                item(key = "visual_segmental_symmetry") {
                    VisualSegmentalSymmetryCard(
                        latest = latestRecord,
                        previous = prevRecord
                    )
                }
            } else {
                item(key = "empty_body_hero") {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, subtleCardBorderColor()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Text(
                                text = "ابدأ بتسجيل أول قياس للجسم",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "اضغط على الزر بالأعلى لتصوير ورقة ${isolateLtr("InBody")} وسيملى الذكاء الاصطناعي كل الأرقام تلقائياً.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 4. Enhanced Progress Chart Card (`تطوّر القياسات`)
            if (sortedRecords.isNotEmpty()) {
                item(key = "body_trend_chart") {
                    val selectedMetricKey = snapshot.config.selectedBodyMetric.ifBlank { "w" }
                    val selectedSpec = CHART_METRICS.firstOrNull { it.key == selectedMetricKey } ?: CHART_METRICS.first()
                    val points = remember(sortedRecords, selectedSpec.key) {
                        sortedRecords.mapNotNull { rec ->
                            rec.metricValue(selectedSpec.key)?.let { rec.date to it }
                        }
                    }
                    val emaSeries = remember(points, selectedSpec.key) {
                        if ((selectedSpec.key == "w" || selectedSpec.key == "f") && points.size >= 2) {
                            val alpha = 0.25
                            var ema = points.first().second
                            points.mapIndexed { idx, (_, v) ->
                                ema = if (idx == 0) v else (alpha * v + (1.0 - alpha) * ema)
                                ema
                            }
                        } else null
                    }

                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, subtleCardBorderColor()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
                                            imageVector = Icons.Rounded.ShowChart,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "تطوّر القياسات",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = isolateLtr("Progress Curve & EMA Trend"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadgePill(
                                    text = "${points.size} قياسات",
                                    accentColor = MaterialTheme.colorScheme.primary
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CHART_METRICS.forEach { m ->
                                    val isSelected = m.key == selectedSpec.key
                                    val chipBg by animateColorAsState(
                                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.background,
                                        animationSpec = tween(220),
                                        label = "metric_chip_bg"
                                    )
                                    val chipLabel by animateColorAsState(
                                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface,
                                        animationSpec = tween(220),
                                        label = "metric_chip_label"
                                    )
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.updateConfig(snapshot.config.copy(selectedBodyMetric = m.key))
                                        },
                                        label = {
                                            Text(
                                                text = m.label,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = chipBg,
                                            labelColor = chipLabel,
                                            selectedContainerColor = chipBg,
                                            selectedLabelColor = chipLabel
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = subtleCardBorderColor()
                                        ),
                                        modifier = Modifier.testTag("body_metric_chip_${m.key}")
                                    )
                                }
                            }

                            SmoothBezierProgressChart(
                                points = points,
                                emaSeries = emaSeries,
                                unitLabel = selectedSpec.unit,
                                metricLabel = selectedSpec.label
                            )

                            if (emaSeries != null && points.size >= 2) {
                                val rawLatest = points.last().second
                                val trendLatest = emaSeries.last()
                                val diff = rawLatest - trendLatest
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "الاتجاه الحقيقي ${isolateLtr("EMA")}: ${isolateLtr(fmt1(trendLatest))} ${selectedSpec.unit} · فرق التذبذب: ${isolateLtr(String.format("%+.1f", diff))} ${selectedSpec.unit}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            if (points.size >= 2) {
                                val deltaFirst = points.last().second - points.first().second
                                val deltaPrev = points.last().second - points[points.size - 2].second
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "من أول قياس: ${isolateLtr(String.format("%+.1f", deltaFirst))} ${selectedSpec.unit}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "عن السابق: ${isolateLtr(String.format("%+.1f", deltaPrev))} ${selectedSpec.unit}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Interactive Target Body Fat Simulator
            if (latestRecord != null && (latestRecord.weightKg ?: 0.0) > 0.0 && (latestRecord.pbfPercent ?: 0.0) > 0.0) {
                item(key = "target_bf_calc") {
                    InteractiveTargetBodyFatCard(
                        latest = latestRecord,
                        savedTargetBf = snapshot.config.targetBodyFat,
                        currentTdee = currentTargets?.tdee,
                        onSaveTargetBf = { newTarget ->
                            viewModel.updateConfig(snapshot.config.copy(targetBodyFat = newTarget))
                            viewModel.showToast("تم حفظ نسبة الدهون المستهدفة (${fmt1(newTarget)}%) ✅")
                        },
                        onApplyPlanToCalories = { targetBf, latestW, fatToLose, weeklyLoss ->
                            viewModel.applyBodyFatPlanToCalories(targetBf, latestW, fatToLose, weeklyLoss)
                        }
                    )
                }
            }

            // 6. Optional Collapsible Body Tape Measurements Card (`قياسات المازورة`)
            item(key = "body_tape_measurements") {
                BodyTapeMeasurementsCard(
                    records = tapeRecords,
                    onSave = { date, waist, chest, arm ->
                        viewModel.saveBodyTapeRecord(date, waist, chest, arm)
                    },
                    onDelete = { date -> viewModel.deleteBodyTapeRecord(date) }
                )
            }

            // 7. Safe History List Accordion (`سجل القياسات`) with Confirmation AlertDialog
            if (sortedRecords.isNotEmpty()) {
                item(key = "body_history") {
                    val recent = sortedRecords.asReversed().take(12)
                    ExpandableSectionCard(
                        title = "سجل القياسات",
                        subtitleEn = "Scan History",
                        statusBadgeText = "${recent.size} قياسات",
                        leadingIcon = Icons.Rounded.History,
                        initiallyExpanded = false,
                        testTag = "body_history_section"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            recent.forEachIndexed { idx, rec ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = isolateLtr(rec.date),
                                            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Bold
                                        )
                                        val summaryParts = buildList {
                                            rec.weightKg?.let { add("وزن ${isolateLtr(fmt1(it))} كجم") }
                                            rec.pbfPercent?.let { add("دهون ${isolateLtr("${fmt1(it)}%")}") }
                                            rec.smmKg?.let { add("عضل ${isolateLtr(fmt1(it))} كجم") }
                                            rec.score?.let { add("درجة ${isolateLtr("${it.roundToInt()}")}") }
                                        }.joinToString(" · ")
                                        Text(
                                            text = summaryParts.ifBlank { "قياس محفوظ" },
                                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { deleteDialogDate = rec.date },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("delete_body_${rec.date}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "حذف قياس ${rec.date}",
                                            tint = SoftDeleteRed.copy(alpha = 0.8f),
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                }
                                if (idx < recent.lastIndex) {
                                    HorizontalDivider(color = subtleCardBorderColor())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InBodyEntryModalSheet(
    bodyDraft: BodyDraftState,
    sortedDates: List<String>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val draftRecord = remember(bodyDraft) { bodyDraft.toBodyRecord() }
    val warnings = remember(draftRecord) { draftRecord.mathWarnings() }
    val sameDayExists = remember(sortedDates, draftRecord.date) {
        draftRecord.date in sortedDates
    }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }

    val imageCapture = rememberImageCaptureController(
        onBitmapCaptured = { bmp -> viewModel.scanInBodySheetWithGemini(bmp) },
        onError = { msg -> viewModel.showToast(msg) }
    )

    if (showSaveConfirmDialog) {
        val dialogReasons = buildList {
            if (sameDayExists) {
                add("فيه قياس متسجل بالفعل بنفس التاريخ (${isolateLtr(draftRecord.date)}). الحفظ هيستبدل القياس القديم.")
            }
            warnings.forEach { add("• $it") }
        }.joinToString("\n")

        AlertDialog(
            onDismissRequest = { showSaveConfirmDialog = false },
            title = { Text("تأكيد حفظ القياس") },
            text = { Text(dialogReasons) },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveConfirmDialog = false
                        viewModel.saveBodyDraft(onSaved = onDismiss)
                    },
                    modifier = Modifier.testTag("body_confirm_save_button")
                ) {
                    Text("احفظ برضو")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveConfirmDialog = false },
                    modifier = Modifier.testTag("body_cancel_save_button")
                ) {
                    Text("راجع الأرقام")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sheet Header (BiDi-safe 2-line Column)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "تسجيل قياس جسم جديد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = isolateLtr("InBody Sheet · Gemini Vision OCR"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "صوّر ورقة ${isolateLtr("InBody")} بالكاميرا أو اختار صورتها من المعرض وهيملى ${isolateLtr("Gemini")} كل الخانات تلقائيًا.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CameraOrGalleryButtons(
                cameraLabel = "صوّر الورقة",
                galleryLabel = "من المعرض",
                enabled = !bodyDraft.aiBusy,
                cameraTestTag = "inbody_camera_button",
                galleryTestTag = "inbody_gallery_button",
                onCameraClick = imageCapture.launchCamera,
                onGalleryClick = imageCapture.launchGallery
            )

            if (bodyDraft.aiBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (!bodyDraft.aiStatusMessage.isNullOrBlank()) {
                val clipboard = LocalClipboardManager.current
                val isSuccess = bodyDraft.aiStatusMessage.contains("✅")
                val isError = !bodyDraft.aiBusy && !isSuccess
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = bodyDraft.aiStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (isError) {
                            TextButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(bodyDraft.aiStatusMessage))
                                    viewModel.showToast("تم نسخ تفاصيل الخطأ")
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("inbody_copy_error_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "نسخ تفاصيل الخطأ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = bodyDraft.date,
                onValueChange = viewModel::updateBodyDraftDate,
                label = { Text("تاريخ القياس ${isolateLtr("YYYY-MM-DD")}") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("body_date_input")
            )

            // Primary 8 fields in 2-column grid
            BASIC_BODY_FIELDS.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { (key, label) ->
                        OutlinedTextField(
                            value = bodyDraft.fields[key].orEmpty(),
                            onValueChange = { viewModel.updateBodyDraftField(key, it) },
                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("body_field_$key")
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // Collapsible Advanced Fields
            val advFilledCount = ADVANCED_BODY_FIELDS.count { (k, _) ->
                !bodyDraft.fields[k].isNullOrBlank()
            }
            ExpandableSectionCard(
                title = "خانات متقدمة",
                subtitleEn = "FFM · Protein · Minerals · WHR",
                statusBadgeText = if (advFilledCount > 0) isolateLtr("$advFilledCount/${ADVANCED_BODY_FIELDS.size}") else null,
                leadingIcon = Icons.Rounded.Tune,
                testTag = "body_advanced_section"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ADVANCED_BODY_FIELDS.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { (key, label) ->
                                OutlinedTextField(
                                    value = bodyDraft.fields[key].orEmpty(),
                                    onValueChange = { viewModel.updateBodyDraftField(key, it) },
                                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("body_field_$key")
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // Collapsible Segmental Lean Fields
            val segFilledCount = SEGMENT_FIELDS.count { (k, _) ->
                !bodyDraft.segmentalLeanKg[k].isNullOrBlank()
            }
            ExpandableSectionCard(
                title = "توزيع العضل على الأطراف",
                subtitleEn = "Segmental Lean Analysis",
                statusBadgeText = if (segFilledCount > 0) isolateLtr("$segFilledCount/${SEGMENT_FIELDS.size}") else null,
                leadingIcon = Icons.Rounded.AccessibilityNew,
                testTag = "body_segmental_section"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SEGMENT_FIELDS.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { (key, label) ->
                                OutlinedTextField(
                                    value = bodyDraft.segmentalLeanKg[key].orEmpty(),
                                    onValueChange = { viewModel.updateBodyDraftSegment(key, it) },
                                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("body_segment_$key")
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            if (warnings.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = semantic.warn.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, semantic.warn),
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
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = semantic.warn,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "ملاحظات رياضية على الأرقام:",
                                style = MaterialTheme.typography.labelLarge,
                                color = semantic.warn
                            )
                        }
                        warnings.forEach { w ->
                            Text(
                                text = "• $w",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            val saveInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = {
                    if (warnings.isNotEmpty() || sameDayExists) {
                        showSaveConfirmDialog = true
                    } else {
                        viewModel.saveBodyDraft(onSaved = onDismiss)
                    }
                },
                enabled = !bodyDraft.aiBusy,
                interactionSource = saveInteraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .tactilePressScale(saveInteraction, pressedScale = 0.97f)
                    .testTag("save_body_record_button")
            ) {
                Text("احفظ القياس", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun fmt1(v: Double): String = String.format("%.1f", v)
