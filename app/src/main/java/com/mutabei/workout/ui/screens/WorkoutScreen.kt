package com.mutabei.workout.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mutabei.workout.R
import com.mutabei.workout.data.model.ExerciseSpec
import com.mutabei.workout.data.model.HintLevel
import com.mutabei.workout.data.model.SetEntry
import com.mutabei.workout.data.model.WorkoutDefaults
import com.mutabei.workout.data.model.shiftIsoDate
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.ui.components.DualLineProgressChart
import com.mutabei.workout.ui.components.ExpandableSectionCard
import com.mutabei.workout.ui.components.MetricProgressBar
import com.mutabei.workout.ui.components.MuscleSilhouetteCanvas
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.AppDataSnapshot
import com.mutabei.workout.ui.viewmodel.ExerciseHistoryPoint
import com.mutabei.workout.ui.viewmodel.MainViewModel
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(
    snapshot: AppDataSnapshot,
    selectedDayIndex: Int,
    drafts: Map<String, List<SetEntry>>,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val split = snapshot.config.effectiveSplit()
    val safeDayIdx = selectedDayIndex.coerceIn(0, (split.size - 1).coerceAtLeast(0))
    val currentDay = split[safeDayIdx]
    val today = todayIso()
    val sessionId = "${today}_$safeDayIdx"
    val todaySession = snapshot.sessions[sessionId]

    fun openYoutube(videoId: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WorkoutDefaults.youtubeUrl(videoId))))
        } catch (_: Exception) {
            viewModel.showToast("تعذّر فتح رابط يوتيوب.")
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Split day selector chips
            item(key = "day_chips") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    split.forEachIndexed { idx, day ->
                        val selected = idx == safeDayIdx
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.selectWorkoutDay(idx) },
                            label = { Text(day.name, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                                selectedLabelColor = MaterialTheme.colorScheme.background
                            ),
                            modifier = Modifier.testTag("split_day_chip_$idx")
                        )
                    }
                }
            }

            // General warm-up accordion
            item(key = "general_warmup") {
                ExpandableSectionCard(
                    title = "التسخين العام (أقل من 10 دقايق)",
                    testTag = "general_warmup_section"
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WorkoutDefaults.GENERAL_WARMUPS.forEach { w ->
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = MaterialTheme.colorScheme.background,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.clickable { openYoutube(w.youtubeId) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = stringResource(R.string.cd_youtube),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(w.title, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "10 عدات لكل تمرين، حاول تتحسن في الرينج من غير ما تجهد نفسك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Exercise cards
            itemsIndexed(
                items = currentDay.exercises,
                key = { idx, _ -> "${safeDayIdx}_$idx" }
            ) { exIdx, spec ->
                val altKey = "$safeDayIdx|$exIdx"
                val chosenName = snapshot.config.alternatives[altKey]?.ifBlank { spec.name } ?: spec.name
                val savedEx = todaySession?.exercises?.get(exIdx.toString())
                val savedSets = if (savedEx != null && savedEx.name == chosenName) savedEx.sets else null
                val defaultSets = savedSets ?: List(spec.workingSets) { SetEntry() }
                val activeSets = drafts[altKey] ?: defaultSets
                val history = remember(snapshot.sessions, chosenName) {
                    viewModel.exerciseHistory(snapshot.sessions, chosenName)
                }

                ExerciseCard(
                    dayIndex = safeDayIdx,
                    exIndex = exIdx,
                    spec = spec,
                    chosenName = chosenName,
                    isSaved = savedSets != null,
                    sets = activeSets,
                    history = history,
                    onSelectAlternative = { alt ->
                        viewModel.selectExerciseAlternative(safeDayIdx, exIdx, alt)
                    },
                    onOpenYoutube = ::openYoutube,
                    onSetChanged = { setIdx, weight, reps ->
                        viewModel.updateDraftSet(
                            dayIndex = safeDayIdx,
                            exIndex = exIdx,
                            setIndex = setIdx,
                            setCount = spec.workingSets,
                            baseSets = activeSets,
                            weight = weight,
                            reps = reps
                        )
                    },
                    onRepsDone = { viewModel.startRestTimer() },
                    onSaveExercise = {
                        viewModel.saveExerciseLog(safeDayIdx, exIdx, chosenName, activeSets)
                    }
                )
            }

            // Session note
            item(key = "session_note_$safeDayIdx") {
                var noteText by remember(sessionId, todaySession?.note) {
                    mutableStateOf(todaySession?.note.orEmpty())
                }
                ExpandableSectionCard(
                    title = "ملاحظة الجلسة",
                    initiallyExpanded = !todaySession?.note.isNullOrBlank(),
                    testTag = "session_note_section"
                ) {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("نوم، إرهاق، ألم، أي حاجة تفتكرها") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("session_note_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.saveSessionNote(safeDayIdx, noteText)
                            viewModel.showToast("اتحفظت الملاحظة ✅")
                        },
                        modifier = Modifier.testTag("save_session_note_button")
                    ) {
                        Text("احفظ الملاحظة")
                    }
                }
            }

            // Weekly muscle sets (last 7 days)
            item(key = "weekly_muscle_sets") {
                WeeklyMuscleSetsCard(snapshot = snapshot)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(
    dayIndex: Int,
    exIndex: Int,
    spec: ExerciseSpec,
    chosenName: String,
    isSaved: Boolean,
    sets: List<SetEntry>,
    history: List<ExerciseHistoryPoint>,
    onSelectAlternative: (String) -> Unit,
    onOpenYoutube: (String) -> Unit,
    onSetChanged: (Int, String?, String?) -> Unit,
    onRepsDone: () -> Unit,
    onSaveExercise: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val focusManager = LocalFocusManager.current
    val today = todayIso()
    val past = remember(history, today) { history.filter { it.date != today } }
    val lastSession = past.lastOrNull()
    val muscleKey = WorkoutDefaults.detectMuscleGroup(chosenName)
    val muscleLabel = WorkoutDefaults.MUSCLE_GROUPS[muscleKey]?.arabicLabel ?: "عضلة"

    // Plateau warning: last 3 past sessions flat vs 4th-from-last
    val hasPlateau = remember(past) {
        if (past.size >= 4) {
            val e1s = past.map { it.topE1Rm() }
            val last3Max = e1s.takeLast(3).maxOrNull() ?: 0.0
            last3Max <= e1s[e1s.size - 4]
        } else false
    }

    var warmupMessage by remember(chosenName) { mutableStateOf<String?>(null) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (isSaved) 1.5.dp else 1.dp, if (isSaved) semantic.ok else MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exercise_card_${dayIndex}_$exIndex")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MuscleSilhouetteCanvas(muscleKey = muscleKey)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = chosenName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (isSaved) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "تم التسجيل",
                                tint = semantic.ok,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "$muscleLabel · ${spec.workingSets} مجموعات شغل · تسخين ${spec.warmupSets} · راحة 3–5 د",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${spec.minReps}–${spec.maxReps}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Alternatives + YouTube chips
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                spec.alternativesList().forEach { option ->
                    val isSelected = option == chosenName
                    val ytId = WorkoutDefaults.YOUTUBE_IDS[option]
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clickable { onSelectAlternative(option) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                            if (ytId != null) {
                                Box(
                                    modifier = Modifier
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface)
                                        .clickable { onOpenYoutube(ytId) }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "شرح $option",
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Arabic execution note
            if (spec.note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = spec.note,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Plateau warning
            if (hasPlateau) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = semantic.warn,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = "الوزن ثابت من 3 جلسات. جرّب تزوّد عدة أو وزن بشكل تدريجي، من غير ما تغيّر البرنامج قبل الـ12 أسبوع.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = semantic.warn
                    )
                }
            }

            // Last session summary
            if (lastSession != null) {
                Spacer(Modifier.height(6.dp))
                val summary = lastSession.sets.joinToString(" ، ") { s ->
                    "${s.weight.ifBlank { "–" }}كجم×${s.reps.ifBlank { "–" }}"
                }
                Text(
                    text = "آخر مرة (${lastSession.date}): $summary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Working sets rows
            Spacer(Modifier.height(8.dp))
            sets.forEachIndexed { setIdx, set ->
                val repHint = WorkoutDefaults.repHint(set.reps, spec.minReps, spec.maxReps)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${setIdx + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(22.dp),
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = set.weight,
                        onValueChange = { onSetChanged(setIdx, it, null) },
                        placeholder = { Text("كجم", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_weight_${dayIndex}_${exIndex}_$setIdx")
                    )
                    OutlinedTextField(
                        value = set.reps,
                        onValueChange = { onSetChanged(setIdx, null, it) },
                        placeholder = { Text("عدات", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (set.reps.isNotBlank()) onRepsDone()
                            }
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_reps_${dayIndex}_${exIndex}_$setIdx")
                    )
                }
                if (repHint != null) {
                    val hintColor = when (repHint.level) {
                        HintLevel.OK -> semantic.ok
                        HintLevel.HIGH -> semantic.warn
                        HintLevel.LOW -> semantic.low
                    }
                    Text(
                        text = repHint.message,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = hintColor,
                        modifier = Modifier.padding(start = 30.dp, bottom = 2.dp)
                    )
                }
            }

            // Warm-up calculator
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val baseW = sets.firstOrNull()?.weightDouble()?.takeIf { it > 0.0 }
                            ?: lastSession?.topWeight()
                            ?: 0.0
                        warmupMessage = WorkoutDefaults.warmUpAdvice(spec.warmupSets, baseW)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("warmup_calc_${dayIndex}_$exIndex")
                ) {
                    Text("حاسبة التسخين", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onRepsDone,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("start_rest_${dayIndex}_$exIndex")
                ) {
                    Text("⏱ راحة", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (!warmupMessage.isNullOrBlank()) {
                Text(
                    text = warmupMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Finish / Update Exercise button
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onSaveExercise,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("finish_exercise_${dayIndex}_$exIndex")
            ) {
                Text(if (isSaved) "حدّث التمرين" else "خلّصت التمرين")
            }

            // Progress chart & History accordion
            Spacer(Modifier.height(8.dp))
            ExpandableSectionCard(
                title = "التطور والسجل (${history.size} جلسة)",
                testTag = "exercise_history_${dayIndex}_$exIndex"
            ) {
                if (history.isEmpty()) {
                    Text(
                        text = "لسه مفيش جلسات متسجلة للتمرين ده.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val bestW = history.maxOfOrNull { it.topWeight() } ?: 0.0
                    val bestE1 = history.maxOfOrNull { it.topE1Rm() } ?: 0.0
                    Text(
                        text = "أعلى وزن: ${String.format("%.1f", bestW)} كجم · أفضل تقدير لأقصى وزن: ${bestE1.roundToInt()} كجم",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DualLineProgressChart(
                        primarySeries = history.map { it.topWeight() },
                        secondarySeries = history.map { it.topE1Rm() },
                        unitLabel = "كجم",
                        legendText = "الخط الأزرق أعلى وزن، المتقطع تقدير أقصى وزن"
                    )
                    Spacer(Modifier.height(6.dp))
                    history.reversed().take(12).forEach { pt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pt.date, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = pt.sets.joinToString(" ، ") { "${it.weight.ifBlank { "–" }}×${it.reps.ifBlank { "–" }}" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyMuscleSetsCard(snapshot: AppDataSnapshot) {
    val today = todayIso()
    val start7 = shiftIsoDate(today, -6)
    val done = mutableMapOf<String, Int>()
    val planned = mutableMapOf<String, Int>()

    snapshot.sessions.values.forEach { s ->
        if (s.date in start7..today) {
            s.exercises.values.forEach { ex ->
                val g = WorkoutDefaults.detectMuscleGroup(ex.name)
                val count = ex.sets.count { it.repsInt() > 0 }
                done[g] = (done[g] ?: 0) + count
            }
        }
    }
    snapshot.config.effectiveSplit().forEach { day ->
        day.exercises.forEach { ex ->
            val g = WorkoutDefaults.detectMuscleGroup(ex.name)
            planned[g] = (planned[g] ?: 0) + ex.workingSets
        }
    }

    ExpandableSectionCard(
        title = "المجموعات الأسبوعية لكل عضلة (آخر 7 أيام)",
        testTag = "weekly_muscle_sets_card"
    ) {
        WorkoutDefaults.MUSCLE_GROUPS.entries
            .filter { (k, _) -> (planned[k] ?: 0) > 0 || (done[k] ?: 0) > 0 }
            .forEach { (k, meta) ->
                val d = done[k] ?: 0
                val p = planned[k] ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(meta.arabicLabel, style = MaterialTheme.typography.bodyMedium)
                    Text("$d / $p مخطط", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                MetricProgressBar(current = d.toDouble(), target = max(p, 1).toDouble())
            }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "بيحسب المجموعات اللي سجّلت فيها عدات بس. \"دراع\" بتشمل البايسبس والترايسبس. التوصيات العامة للتضخيم حوالي 10–20 مجموعة أسبوعيًا لكل عضلة.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
