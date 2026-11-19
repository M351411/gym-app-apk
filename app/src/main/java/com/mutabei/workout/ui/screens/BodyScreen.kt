package com.mutabei.workout.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mutabei.workout.data.model.BodyRecord
import com.mutabei.workout.ui.components.CameraOrGalleryButtons
import com.mutabei.workout.ui.components.DualLineProgressChart
import com.mutabei.workout.ui.components.ExpandableSectionCard
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.util.rememberImageCaptureController
import com.mutabei.workout.ui.viewmodel.AppDataSnapshot
import com.mutabei.workout.ui.viewmodel.BodyDraftState
import com.mutabei.workout.ui.viewmodel.MainViewModel
import kotlin.math.abs
import kotlin.math.ceil
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
    "m" to "العضل الهيكلي SMM (كجم)",
    "f" to "نسبة الدهون PBF (%)",
    "bfm" to "كتلة الدهون BFM (كجم)",
    "tbw" to "الماء TBW (لتر)",
    "bmr" to "الحرق الأساسي BMR",
    "vfl" to "الدهون الحشوية VFL",
    "score" to "درجة InBody (/100)"
)

private val ADVANCED_BODY_FIELDS = listOf(
    "ffm" to "الكتلة الخالية FFM (كجم)",
    "pro" to "البروتين (كجم)",
    "min" to "المعادن (كجم)",
    "whr" to "نسبة الخصر للورك WHR",
    "bmi" to "مؤشر الكتلة BMI",
    "h" to "الطول (سم)",
    "tgt" to "الوزن المستهدف (كجم)",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BodyScreen(
    snapshot: AppDataSnapshot,
    bodyDraft: BodyDraftState,
    viewModel: MainViewModel
) {
    val semantic = LocalSemanticColors.current
    val sortedRecords = remember(snapshot.bodyRecords) {
        snapshot.bodyRecords.sortedBy { it.date }
    }
    val latestRecord = sortedRecords.lastOrNull()
    val draftRecord = remember(bodyDraft) { bodyDraft.toBodyRecord() }
    val warnings = remember(draftRecord) { draftRecord.mathWarnings() }
    val sameDayExists = remember(sortedRecords, draftRecord.date) {
        sortedRecords.any { it.date == draftRecord.date }
    }

    var showSaveConfirmDialog by remember { mutableStateOf(false) }
    var pendingDeleteDate by remember { mutableStateOf<String?>(null) }

    val imageCapture = rememberImageCaptureController(
        onBitmapCaptured = { bmp -> viewModel.scanInBodySheetWithGemini(bmp) },
        onError = { msg -> viewModel.showToast(msg) }
    )

    if (showSaveConfirmDialog) {
        val dialogReasons = buildList {
            if (sameDayExists) {
                add("فيه قياس متسجل بالفعل بنفس التاريخ (${draftRecord.date}). الحفظ هيستبدل القياس القديم.")
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
                        viewModel.saveBodyDraft()
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Gemini InBody Scanner + Manual Entry Form
            item(key = "inbody_form") {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "تسجيل قياس جسم / InBody",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = "صوّر ورقة الـ InBody بالكاميرا أو اختار صورتها من المعرض وهيملى Gemini كل الخانات تلقائيًا.",
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
                            label = { Text("تاريخ القياس (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("body_date_input")
                        )

                        // Basic 8 fields in 2-column grid
                        BASIC_BODY_FIELDS.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { (key, label) ->
                                    OutlinedTextField(
                                        value = bodyDraft.fields[key].orEmpty(),
                                        onValueChange = { viewModel.updateBodyDraftField(key, it) },
                                        label = { Text(label, maxLines = 1) },
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

                        // Expandable advanced fields
                        val advFilledCount = ADVANCED_BODY_FIELDS.count { (k, _) ->
                            !bodyDraft.fields[k].isNullOrBlank()
                        }
                        ExpandableSectionCard(
                            title = if (advFilledCount > 0) {
                                "خانات متقدمة ($advFilledCount/${ADVANCED_BODY_FIELDS.size} ممتلئة)"
                            } else {
                                "خانات متقدمة (FFM، بروتين، معادن، WHR)"
                            },
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
                                                label = { Text(label, maxLines = 1) },
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

                        // Expandable segmental lean fields
                        val segFilledCount = SEGMENT_FIELDS.count { (k, _) ->
                            !bodyDraft.segmentalLeanKg[k].isNullOrBlank()
                        }
                        ExpandableSectionCard(
                            title = if (segFilledCount > 0) {
                                "توزيع العضل على الأطراف ($segFilledCount/${SEGMENT_FIELDS.size} ممتلئة)"
                            } else {
                                "توزيع العضل على الأطراف (Segmental Lean)"
                            },
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
                                                label = { Text(label, maxLines = 1) },
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

                        Button(
                            onClick = {
                                if (warnings.isNotEmpty() || sameDayExists) {
                                    showSaveConfirmDialog = true
                                } else {
                                    viewModel.saveBodyDraft()
                                }
                            },
                            enabled = !bodyDraft.aiBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_body_record_button")
                        ) {
                            Text("احفظ القياس")
                        }
                    }
                }
            }

            // 2. Latest Measurement Card
            if (latestRecord != null) {
                item(key = "latest_body_card") {
                    LatestBodyRecordCard(record = latestRecord)
                }
            }

            // 3. 7-Metric Trend Chart
            if (sortedRecords.isNotEmpty()) {
                item(key = "body_trend_chart") {
                    val selectedMetricKey = snapshot.config.selectedBodyMetric.ifBlank { "w" }
                    val selectedSpec = CHART_METRICS.firstOrNull { it.key == selectedMetricKey } ?: CHART_METRICS.first()
                    val points = sortedRecords.mapNotNull { rec ->
                        rec.metricValue(selectedSpec.key)?.let { rec.date to it }
                    }

                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("تطوّر القياسات", style = MaterialTheme.typography.titleMedium)

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CHART_METRICS.forEach { m ->
                                    FilterChip(
                                        selected = m.key == selectedSpec.key,
                                        onClick = {
                                            viewModel.updateConfig(
                                                snapshot.config.copy(selectedBodyMetric = m.key)
                                            )
                                        },
                                        label = { Text(m.label) },
                                        modifier = Modifier.testTag("body_metric_chip_${m.key}")
                                    )
                                }
                            }

                            DualLineProgressChart(
                                primarySeries = points.map { it.second },
                                unitLabel = selectedSpec.unit,
                                legendText = "${points.size} قياسات"
                            )

                            if (points.size >= 2) {
                                val firstVal = points.first().second
                                val prevVal = points[points.size - 2].second
                                val lastVal = points.last().second
                                val deltaFirst = lastVal - firstVal
                                val deltaPrev = lastVal - prevVal
                                val signFirst = if (deltaFirst >= 0) "+" else ""
                                val signPrev = if (deltaPrev >= 0) "+" else ""

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "من أول قياس: $signFirst${String.format("%.1f", deltaFirst)} ${selectedSpec.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "عن القياس السابق: $signPrev${String.format("%.1f", deltaPrev)} ${selectedSpec.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Target Body Fat Calculator
            if (latestRecord != null && (latestRecord.weightKg ?: 0.0) > 0.0 && (latestRecord.pbfPercent ?: 0.0) > 0.0) {
                item(key = "target_bf_calc") {
                    TargetBodyFatCard(
                        latest = latestRecord,
                        savedTargetBf = snapshot.config.targetBodyFat,
                        onSaveTargetBf = { newTarget ->
                            viewModel.updateConfig(snapshot.config.copy(targetBodyFat = newTarget))
                        }
                    )
                }
            }

            // 5. History List (Last 12 records with double-tap delete)
            if (sortedRecords.isNotEmpty()) {
                item(key = "body_history") {
                    ExpandableSectionCard(
                        title = "سجل القياسات (آخر ${sortedRecords.takeLast(12).size})",
                        initiallyExpanded = true,
                        testTag = "body_history_section"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sortedRecords.asReversed().take(12).forEach { rec ->
                                val isConfirming = pendingDeleteDate == rec.date
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rec.date,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        val summaryParts = buildList {
                                            rec.weightKg?.let { add("وزن ${fmt1(it)} كجم") }
                                            rec.pbfPercent?.let { add("دهون ${fmt1(it)}%") }
                                            rec.smmKg?.let { add("عضل ${fmt1(it)} كجم") }
                                            rec.score?.let { add("درجة ${it.roundToInt()}") }
                                        }.joinToString(" · ")
                                        Text(
                                            text = summaryParts.ifBlank { "قياس محفوظ" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            if (isConfirming) {
                                                pendingDeleteDate = null
                                                viewModel.deleteBodyRecord(rec.date)
                                            } else {
                                                pendingDeleteDate = rec.date
                                            }
                                        },
                                        modifier = Modifier.testTag("delete_body_${rec.date}")
                                    ) {
                                        Text(
                                            text = if (isConfirming) "تأكيد؟" else "حذف",
                                            color = if (isConfirming) semantic.low else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isConfirming) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LatestBodyRecordCard(record: BodyRecord) {
    val semantic = LocalSemanticColors.current
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("آخر قياس", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (record.score != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "درجة InBody: ${record.score.roundToInt()}/100",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            val rows = listOfNotNull(
                record.weightKg?.let { Triple("w", "الوزن", "${fmt1(it)} كجم" to it) },
                record.smmKg?.let { Triple("m", "العضل الهيكلي SMM", "${fmt1(it)} كجم" to it) },
                record.pbfPercent?.let { Triple("f", "نسبة الدهون PBF", "${fmt1(it)}%" to it) },
                record.bfmKg?.let { Triple("bfm", "كتلة الدهون BFM", "${fmt1(it)} كجم" to it) },
                record.ffmKg?.let { Triple("ffm", "الكتلة الخالية FFM", "${fmt1(it)} كجم" to it) },
                record.tbwL?.let { Triple("tbw", "الماء TBW", "${fmt1(it)} لتر" to it) },
                record.proteinKg?.let { Triple("pro", "البروتين", "${fmt1(it)} كجم" to it) },
                record.mineralsKg?.let { Triple("min", "المعادن", "${fmt1(it)} كجم" to it) },
                record.bmrKcal?.let { Triple("bmr", "الحرق الأساسي BMR", "${it.roundToInt()} سعرة" to it) },
                record.vfl?.let { Triple("vfl", "الدهون الحشوية VFL", "${it.roundToInt()}" to it) },
                record.whr?.let { Triple("whr", "نسبة الخصر للورك WHR", String.format("%.2f", it) to it) },
                record.bmi?.let { Triple("bmi", "مؤشر الكتلة BMI", fmt1(it) to it) }
            )

            rows.forEach { (key, label, valPair) ->
                val status = record.rangeStatus(key, valPair.second)
                val statusColor = when (status) {
                    "ضمن المعدل" -> semantic.ok
                    "أعلى من المعدل" -> semantic.low
                    "أقل من المعدل" -> semantic.warn
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (status != null) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = statusColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = valPair.first,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }

            if (record.segmentalLeanKg.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "توزيع العضل (Segmental Lean):",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                SEGMENT_FIELDS.forEach { (key, label) ->
                    val kg = record.segmentalLeanKg[key]
                    val pct = record.segmentalLeanPercent[key]
                    if (kg != null) {
                        val pctText = pct?.let { " (${fmt1(it)}%)" } ?: ""
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label.replace(" (كجم)", ""), style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "${fmt1(kg)} كجم$pctText",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetBodyFatCard(
    latest: BodyRecord,
    savedTargetBf: Double,
    onSaveTargetBf: (Double) -> Unit
) {
    var inputText by remember(savedTargetBf) {
        mutableStateOf(if (savedTargetBf % 1.0 == 0.0) savedTargetBf.toInt().toString() else savedTargetBf.toString())
    }
    val w = latest.weightKg ?: return
    val pbf = latest.pbfPercent ?: return
    val ffm = latest.ffmKg ?: (w * (1.0 - pbf / 100.0))
    val targetBf = (inputText.replace(',', '.').toDoubleOrNull() ?: savedTargetBf).coerceIn(5.0, 45.0)
    val targetWeight = ffm / (1.0 - targetBf / 100.0)
    val fatToLose = w - targetWeight
    val weeklyLossKg = (w * 0.005).coerceAtLeast(0.25)
    val weeksNeeded = if (fatToLose > 0.1) ceil(fatToLose / weeklyLossKg).toInt() else 0

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("حاسبة نسبة الدهون المستهدفة", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("نسبة الدهون المستهدفة (%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("target_bf_input")
                )
                OutlinedButton(
                    onClick = {
                        val parsed = inputText.replace(',', '.').toDoubleOrNull()
                        if (parsed != null && parsed in 5.0..45.0) {
                            onSaveTargetBf(parsed)
                        }
                    },
                    modifier = Modifier.testTag("save_target_bf_button")
                ) {
                    Text("حفظ")
                }
            }

            if (fatToLose > 0.1) {
                Text(
                    text = "بافتراض الحفاظ على الكتلة العضلية (${fmt1(ffm)} كجم):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• الوزن المستهدف: ${fmt1(targetWeight)} كجم\n" +
                        "• دهون محتاج تنزلها: ${fmt1(fatToLose)} كجم\n" +
                        "• المدة التقريبية (بمعدل 0.5% أسبوعيًا): حوالي $weeksNeeded أسبوع",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "نسبة دهونك الحالية (${fmt1(pbf)}%) وصلت أو أقل من المستهدف (${fmt1(targetBf)}%) بفرق ${fmt1(abs(fatToLose))} كجم.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalSemanticColors.current.ok
                )
            }
        }
    }
}

private fun fmt1(v: Double): String = String.format("%.1f", v)
