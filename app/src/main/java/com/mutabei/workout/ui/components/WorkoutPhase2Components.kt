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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutabei.workout.data.model.WorkoutDefaults
import com.mutabei.workout.data.model.WorkoutSession
import com.mutabei.workout.data.model.daysBetweenIso
import com.mutabei.workout.data.model.shiftIsoDate
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.AppDataSnapshot
import kotlin.math.roundToInt

/**
 * Live Workout Summary & Tonnage card for the currently active split day.
 */
@Composable
fun WorkoutSessionSummaryCard(
    dayName: String,
    dayIndex: Int,
    todaySession: WorkoutSession?,
    allSessions: Map<String, WorkoutSession>
) {
    if (todaySession == null || todaySession.exercises.isEmpty()) return
    val semantic = LocalSemanticColors.current
    val today = todayIso()

    val stats = remember(todaySession, allSessions, dayIndex) {
        fun sessionTonnage(s: WorkoutSession): Double =
            s.exercises.values.sumOf { ex ->
                ex.sets.sumOf { st -> st.weightDouble() * st.repsDouble() }
            }

        val todayTonnage = sessionTonnage(todaySession)
        val todaySets = todaySession.exercises.values.sumOf { ex -> ex.sets.count { it.repsDouble() > 0.0 } }
        val prevSameDay = allSessions.values
            .filter { it.dayIndex == dayIndex && it.date < today && it.exercises.isNotEmpty() }
            .maxByOrNull { it.date }
        val prevTonnage = prevSameDay?.let(::sessionTonnage) ?: 0.0
        val deltaPct = if (prevTonnage > 0.0) ((todayTonnage - prevTonnage) / prevTonnage) * 100.0 else null

        // Count how many exercises in today's session beat all-time prior e1RM
        var prCount = 0
        todaySession.exercises.values.forEach { ex ->
            val curE1 = ex.topE1Rm()
            if (curE1 > 0.0) {
                val prevBest = allSessions.values
                    .filter { it.date != today }
                    .flatMap { it.exercises.values }
                    .filter { it.name == ex.name }
                    .maxOfOrNull { it.topE1Rm() } ?: 0.0
                if (prevBest > 0.0 && curE1 > prevBest + 0.05) prCount++
            }
        }
        Triple(todayTonnage, todaySets, Pair(deltaPct, prCount))
    }

    val (tonnage, setsDone, pair) = stats
    val (deltaPct, prCount) = pair
    if (setsDone == 0) return

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workout_session_summary_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ملخص جلسة $dayName النهارده",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (prCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = semantic.ok.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, semantic.ok)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = semantic.ok, modifier = Modifier.size(13.dp))
                            Text(isolateLtr("$prCount PR"), style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.ExtraBold, color = semantic.ok)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryStatChip(
                    label = "الحجم الكلي",
                    value = if (tonnage >= 1000.0) "${isolateLtr(String.format("%.2f", tonnage / 1000.0))} طن" else "${isolateLtr("${tonnage.roundToInt()}")} كجم",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatChip(
                    label = "مجموعات مكتملة",
                    value = "${isolateLtr("$setsDone")} مجموعة",
                    modifier = Modifier.weight(1f)
                )
                if (deltaPct != null) {
                    val accent = if (deltaPct >= 0.0) semantic.ok else semantic.warn
                    SummaryStatChip(
                        label = "عن آخر جلسة",
                        value = isolateLtr("${String.format("%+.1f", deltaPct)}%"),
                        valueColor = accent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatChip(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

/**
 * Muscle Recovery Heatmap + Automated Deload Detector Card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuscleRecoveryHeatmapCard(snapshot: AppDataSnapshot) {
    val semantic = LocalSemanticColors.current
    val today = todayIso()

    val recoveryData = remember(snapshot.sessions, snapshot.config.splitDays, today) {
        val lastTrainedDate = mutableMapOf<String, String>()
        val lastSetsCount = mutableMapOf<String, Int>()
        snapshot.sessions.values
            .filter { it.date <= today }
            .sortedBy { it.date }
            .forEach { session ->
                val dayCounts = mutableMapOf<String, Int>()
                session.exercises.values.forEach { ex ->
                    val setsDone = ex.sets.count { it.repsDouble() > 0.0 }
                    if (setsDone > 0) {
                        val g = WorkoutDefaults.detectMuscleGroup(ex.name)
                        dayCounts[g] = (dayCounts[g] ?: 0) + setsDone
                    }
                }
                dayCounts.forEach { (g, c) ->
                    lastTrainedDate[g] = session.date
                    lastSetsCount[g] = c
                }
            }

        // Detect Deload signal: >= 4 consecutive active training weeks OR >= 2 exercises on 3-session plateau
        var activeConsecutiveWeeks = 0
        for (w in 0 until 6) {
            val wEnd = shiftIsoDate(today, (-w * 7).toLong())
            val wStart = shiftIsoDate(wEnd, -6)
            val count = snapshot.sessions.values.count { it.date in wStart..wEnd && it.exercises.isNotEmpty() }
            if (count >= 2) activeConsecutiveWeeks++ else break
        }
        val exHistories = mutableMapOf<String, MutableList<Pair<String, Double>>>()
        snapshot.sessions.values.sortedBy { it.date }.forEach { s ->
            s.exercises.values.forEach { ex ->
                val e1 = ex.topE1Rm()
                if (e1 > 0.0) exHistories.getOrPut(ex.name) { mutableListOf() }.add(s.date to e1)
            }
        }
        val plateauCount = exHistories.values.count { list ->
            list.size >= 4 && (list.takeLast(3).maxOfOrNull { it.second } ?: 0.0) <= list[list.size - 4].second
        }
        val needsDeload = activeConsecutiveWeeks >= 5 || (activeConsecutiveWeeks >= 4 && plateauCount >= 2)

        val activeGroups = snapshot.config.effectiveSplit()
            .flatMap { it.exercises }
            .map { WorkoutDefaults.detectMuscleGroup(it.name) }
            .toSet()
            .ifEmpty { WorkoutDefaults.MUSCLE_GROUPS.keys }

        val items = WorkoutDefaults.MUSCLE_GROUPS.entries
            .filter { (k, _) -> k in activeGroups || k in lastTrainedDate }
            .map { (k, meta) ->
                val d = lastTrainedDate[k]
                val daysAgo = d?.let { daysBetweenIso(it, today).toInt().coerceAtLeast(0) }
                Triple(meta.arabicLabel, daysAgo, lastSetsCount[k] ?: 0)
            }
        Triple(items, needsDeload, activeConsecutiveWeeks to plateauCount)
    }

    val (muscles, needsDeload, deloadMeta) = recoveryData
    val readyPct = remember(muscles) {
        if (muscles.isEmpty()) 100
        else {
            val totalScore = muscles.sumOf { (_, daysAgo, _) ->
                when {
                    daysAgo == null || daysAgo >= 2 -> 100
                    daysAgo == 1 -> 55
                    else -> 15
                }
            }
            (totalScore.toDouble() / muscles.size).roundToInt().coerceIn(0, 100)
        }
    }
    val (recoveryPillText, recoveryPillColor) = when {
        needsDeload -> "إجهاد · ${isolateLtr("Deload")}" to semantic.warn
        readyPct >= 80 -> "جاهز ${isolateLtr("$readyPct%")}" to semantic.ok
        readyPct >= 50 -> "إجهاد متوسط" to semantic.warn
        else -> "استشفاء ${isolateLtr("$readyPct%")}" to semantic.low
    }

    ExpandableSectionCard(
        title = "خريطة استشفاء العضلات",
        subtitleEn = "Muscle Recovery & Deload",
        statusBadgeText = recoveryPillText,
        statusBadgeColor = recoveryPillColor,
        leadingIcon = Icons.Rounded.SelfImprovement,
        testTag = "muscle_recovery_heatmap_card"
    ) {
        if (needsDeload) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = semantic.warn.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, semantic.warn),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryAlert,
                        contentDescription = null,
                        tint = semantic.warn,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "تنبيه استشفاء عصبي · ${isolateLtr("Deload")}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = semantic.warn
                        )
                        Text(
                            text = "بقالك ${isolateLtr("${deloadMeta.first}")} أسابيع تمرين متواصلة" +
                                (if (deloadMeta.second > 0) " مع ثبات في ${isolateLtr("${deloadMeta.second}")} تمارين" else "") +
                                ". فكّر تعمل أسبوع ${isolateLtr("Deload")} بنص المجموعات وبأوزان ${isolateLtr("60–70%")} عشان ترجع أقوى.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            muscles.forEach { (label, daysAgo, sets) ->
                val (statusText, accent) = when {
                    daysAgo == null -> "جاهز" to semantic.ok
                    daysAgo == 0 -> "اتمرنت النهارده ($sets مجم)" to semantic.low
                    daysAgo == 1 -> "استشفاء (~24س)" to semantic.warn
                    daysAgo == 2 -> "جاهز (~48س)" to semantic.ok
                    else -> "جاهز (${daysAgo} أيام)" to semantic.ok
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = accent.copy(alpha = 0.13f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(statusText, style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SelfImprovement,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "أحمر = أقل من 24 ساعة (استشفاء) · أصفر = 24–48 ساعة · أخضر = جاهز للتمرين.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 12-Week Consistency Heatmap (GitHub-style grid) + Active Streak Counter.
 */
@Composable
fun ConsistencyHeatmapCard(
    snapshot: AppDataSnapshot,
    proteinTargetG: Int?
) {
    val semantic = LocalSemanticColors.current
    val today = todayIso()

    val gridData = remember(snapshot.sessions, snapshot.nutritionDays, proteinTargetG, today) {
        val workoutDates = snapshot.sessions.values
            .filter { it.exercises.isNotEmpty() }
            .map { it.date }
            .toSet()

        // Build 84 days (12 weeks x 7 days) ending today
        // Status: 0 = none, 1 = nutrition only, 2 = workout only, 3 = workout + protein/nutrition
        val cells = (83 downTo 0).map { offset ->
            val d = shiftIsoDate(today, -offset.toLong())
            val hasWorkout = d in workoutDates
            val nDay = snapshot.nutritionDays[d]
            val hasNut = nDay != null && nDay.items.isNotEmpty()
            val hitProtein = if (hasNut && proteinTargetG != null && proteinTargetG > 0) {
                nDay!!.items.sumOf { it.nutrientAmount("p") } >= proteinTargetG * 0.85
            } else hasNut
            when {
                hasWorkout && hitProtein -> 3
                hasWorkout -> 2
                hasNut -> 1
                else -> 0
            }
        }

        // Current active streak (either workout or nutrition logged consecutively backwards from today or yesterday)
        var streak = 0
        var cursor = today
        if (cells.last() == 0) cursor = shiftIsoDate(today, -1)
        for (i in 0 until 84) {
            val d = shiftIsoDate(cursor, -i.toLong())
            val active = (d in workoutDates) || (snapshot.nutritionDays[d]?.items?.isNotEmpty() == true)
            if (active) streak++ else break
        }

        val totalWorkouts84 = cells.count { it == 2 || it == 3 }
        val totalNut84 = cells.count { it == 1 || it == 3 }
        Triple(cells, streak, totalWorkouts84 to totalNut84)
    }

    val (cells, streak, totals) = gridData
    val emptyCol = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val nutCol = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val workoutCol = semantic.warn
    val superCol = semantic.ok

    ExpandableSectionCard(
        title = "تقويم الالتزام",
        subtitleEn = "12-Week Consistency",
        statusBadgeText = "🔥 $streak يوم",
        statusBadgeColor = if (streak >= 3) semantic.ok else semantic.warn,
        leadingIcon = Icons.Rounded.LocalFireDepartment,
        testTag = "consistency_heatmap_card"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = semantic.warn, modifier = Modifier.size(18.dp))
                Text("سلسلة الالتزام: $streak يوم", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("${totals.first} تمرين · ${totals.second} يوم أكل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))

        // 12 columns (weeks) x 7 rows (days)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
        ) {
            val cols = 12
            val rows = 7
            val gap = 4.dp.toPx()
            val cellW = (size.width - gap * (cols - 1)) / cols
            val cellH = (size.height - gap * (rows - 1)) / rows
            val cellSize = minOf(cellW, cellH)
            val totalGridW = cols * cellSize + (cols - 1) * gap
            val startX = (size.width - totalGridW) / 2f

            cells.forEachIndexed { idx, level ->
                val col = idx / 7
                val row = idx % 7
                val x = startX + col * (cellSize + gap)
                val y = row * (cellSize + gap)
                val color = when (level) {
                    3 -> superCol
                    2 -> workoutCol
                    1 -> nutCol
                    else -> emptyCol
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(cellSize, cellSize),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "أخضر = تمرين + تغذية · برتقالي = تمرين · أزرق = تغذية فقط",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
