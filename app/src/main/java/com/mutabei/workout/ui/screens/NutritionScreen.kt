package com.mutabei.workout.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mutabei.workout.R
import com.mutabei.workout.data.model.CookingYieldEngine
import com.mutabei.workout.data.model.FoodItem
import com.mutabei.workout.data.model.LoggedFoodItem
import com.mutabei.workout.data.model.MacroSwapperEngine
import com.mutabei.workout.data.model.MicronutrientEngine
import com.mutabei.workout.data.model.MicronutrientStatus
import com.mutabei.workout.data.model.NutritionConstants
import com.mutabei.workout.data.model.NutritionDay
import com.mutabei.workout.data.model.NutritionTargets
import com.mutabei.workout.data.model.ReverseMealBuilderEngine
import com.mutabei.workout.data.model.RollingCalorieBudgetEngine
import com.mutabei.workout.data.model.StepBufferPreset
import com.mutabei.workout.data.model.SupplementCatalog
import com.mutabei.workout.data.model.WeeklyGroceryListEngine
import com.mutabei.workout.data.model.WorkoutSyncedNutritionEngine
import com.mutabei.workout.data.model.daysBetweenIso
import com.mutabei.workout.data.model.shiftIsoDate
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.data.repository.NutrientFoodSuggestion
import com.mutabei.workout.ui.components.AdaptiveTdeeSection
import com.mutabei.workout.ui.components.AiDeficiencyFixerBlock
import com.mutabei.workout.ui.components.AnimatedCalorieRing
import com.mutabei.workout.ui.components.DailyNutrientRow
import com.mutabei.workout.ui.components.ExpandableSectionCard
import com.mutabei.workout.ui.components.FatSolubleDualIndicatorCard
import com.mutabei.workout.ui.components.FoodBattleDialog
import com.mutabei.workout.ui.components.MacroSwapperPanel
import com.mutabei.workout.ui.components.MealLeucineMpsChip
import com.mutabei.workout.ui.components.MetricProgressBar
import com.mutabei.workout.ui.components.RawCookedToggleBar
import com.mutabei.workout.ui.components.ReverseMealBuilderCard
import com.mutabei.workout.ui.components.SaveMealPresetDialog
import com.mutabei.workout.ui.components.SmartNutritionStatusStrip
import com.mutabei.workout.ui.components.StatusBadgePill
import com.mutabei.workout.ui.components.SupplementSynergyCard
import com.mutabei.workout.ui.components.computeAdaptiveTdee
import com.mutabei.workout.ui.components.computeMealLeucineStatus
import com.mutabei.workout.ui.components.formatCounterLtr
import com.mutabei.workout.ui.components.isolateLtr
import com.mutabei.workout.ui.components.subtleCardBorderColor
import com.mutabei.workout.ui.components.tactilePressScale
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.AiDeficiencyUiState
import com.mutabei.workout.ui.viewmodel.AiMacroTetrisUiState
import com.mutabei.workout.ui.viewmodel.AppDataSnapshot
import com.mutabei.workout.ui.viewmodel.FoodSheetAiState
import com.mutabei.workout.ui.viewmodel.MainTab
import com.mutabei.workout.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun NutritionScreen(
    snapshot: AppDataSnapshot,
    selectedDate: String,
    searchResults: List<com.mutabei.workout.data.model.FoodItem>,
    aiState: FoodSheetAiState,
    microSuggestions: Map<String, List<NutrientFoodSuggestion>>,
    deficiencyAi: AiDeficiencyUiState,
    macroTetrisAi: AiMacroTetrisUiState,
    battleResultsA: List<com.mutabei.workout.data.model.FoodItem>,
    battleResultsB: List<com.mutabei.workout.data.model.FoodItem>,
    viewModel: MainViewModel
) {
    val semantic = LocalSemanticColors.current
    val today = todayIso()
    val day = snapshot.nutritionDays[selectedDate] ?: NutritionDay(date = selectedDate)
    val rollingBudgetEnabled by viewModel.rollingCalorieBudgetEnabled.collectAsStateWithLifecycle()
    val workoutSyncedEnabled by viewModel.workoutSyncedNutritionEnabled.collectAsStateWithLifecycle()
    val defaultStepTarget by viewModel.defaultStepTarget.collectAsStateWithLifecycle()
    val defaultStepBuffer by viewModel.defaultStepBufferFactor.collectAsStateWithLifecycle()

    val dailyContext = remember(
        selectedDate,
        snapshot,
        rollingBudgetEnabled,
        workoutSyncedEnabled,
        defaultStepTarget,
        defaultStepBuffer
    ) {
        viewModel.computeDailyContext(
            snapshot = snapshot,
            date = selectedDate,
            rollingBudgetEnabled = rollingBudgetEnabled,
            workoutSyncedEnabled = workoutSyncedEnabled,
            defaultStepTarget = defaultStepTarget,
            defaultBufferFactor = defaultStepBuffer
        )
    }
    val targets = dailyContext.baseTargets
    val rollingBudget = dailyContext.rollingBudget
    val workoutSync = dailyContext.workoutSync
    val stepsBurn = dailyContext.stepsBurn
    val effectiveTargets = dailyContext.adjustedTargets

    val vitDBreakdown = remember(selectedDate, snapshot.nutritionDays) {
        NutritionConstants.computeVitaminDBreakdown(selectedDate, snapshot.nutritionDays)
    }

    val microReport = remember(selectedDate, snapshot.nutritionDays, snapshot.config.sex) {
        MicronutrientEngine.evaluateMicronutrients(
            referenceDate = selectedDate,
            nutritionDays = snapshot.nutritionDays,
            sex = snapshot.config.sex
        )
    }

    val fridgePool = remember(snapshot.nutritionDays, snapshot.customFoods) {
        ReverseMealBuilderEngine.extractHabitAndFridgePool(
            nutritionDays = snapshot.nutritionDays,
            customFoods = snapshot.customFoods
        )
    }

    val groceryItems = remember(snapshot.nutritionDays, snapshot.customFoods) {
        WeeklyGroceryListEngine.generate(
            nutritionDays = snapshot.nutritionDays,
            customFoods = snapshot.customFoods
        )
    }

    // Daily totals across all tracked nutrient keys
    val totals = remember(day.items) {
        val count = NutritionConstants.NUTRIENT_COUNT
        val sum = DoubleArray(count)
        val has = BooleanArray(count)
        day.items.forEach { it ->
            it.nutrients.forEachIndexed { idx, v ->
                if (idx < count && v >= 0.0) {
                    sum[idx] += v * it.grams / 100.0
                    has[idx] = true
                }
            }
        }
        sum to has
    }
    fun totalOf(key: String): Double {
        if (key == "vD") return vitDBreakdown.effectiveMcg
        val idx = NutritionConstants.NUTRIENT_INDEX[key] ?: return 0.0
        return totals.first[idx]
    }
    fun hasDataFor(key: String): Boolean {
        if (key == "vD" && vitDBreakdown.carryInFromYesterdayMcg > 0.0) return true
        val idx = NutritionConstants.NUTRIENT_INDEX[key] ?: return false
        return totals.second[idx]
    }

    fun smartDefaultMealIndex(): Int {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 11 -> 0
            hour < 17 -> 1
            hour < 22 -> 2
            else -> 3
        }
    }

    val haptics = LocalHapticFeedback.current
    var addSheetMealIndex by remember { mutableStateOf<Int?>(null) }
    var addSheetInitialMode by remember { mutableStateOf<FoodSheetMode?>(null) }
    var editingItem by remember { mutableStateOf<LoggedFoodItem?>(null) }
    var savingPresetMeal by remember { mutableStateOf<Pair<String, List<LoggedFoodItem>>?>(null) }
    var showFoodBattle by remember { mutableStateOf(false) }
    val enrichMicrosBusy by viewModel.enrichMicrosBusy.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary + Date switcher + Animated Calorie Ring + Water card
            item(key = "nutrition_summary") {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, subtleCardBorderColor()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val yesterday = shiftIsoDate(today, -1)
                        val dateTitle = when (selectedDate) {
                            today -> "النهارده"
                            yesterday -> "أمس · $selectedDate"
                            else -> selectedDate
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { viewModel.shiftNutritionDate(-1) },
                                modifier = Modifier.testTag("nutrition_prev_day")
                            ) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.cd_prev_day))
                            }
                            Text(
                                text = dateTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            IconButton(
                                onClick = { viewModel.shiftNutritionDate(1) },
                                modifier = Modifier.testTag("nutrition_next_day")
                            ) {
                                Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.cd_next_day))
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            FilterChip(
                                selected = selectedDate == today,
                                onClick = { viewModel.selectNutritionDate(today) },
                                label = { Text("النهارده") },
                                modifier = Modifier.testTag("nutrition_jump_today")
                            )
                            FilterChip(
                                selected = selectedDate == yesterday,
                                onClick = { viewModel.selectNutritionDate(yesterday) },
                                label = { Text("أمس") },
                                modifier = Modifier.testTag("nutrition_jump_yesterday")
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        // Fluid AnimatedContent transition when switching between Today / Yesterday / other dates
                        AnimatedContent(
                            targetState = selectedDate,
                            transitionSpec = {
                                val newer = targetState >= initialState
                                val slideIn = slideInHorizontally(
                                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                                ) { w -> if (newer) -w / 6 else w / 6 }
                                val slideOut = slideOutHorizontally(tween(180)) { w -> if (newer) w / 7 else -w / 7 }
                                (fadeIn(tween(220)) + slideIn) togetherWith (fadeOut(tween(160)) + slideOut)
                            },
                            label = "nutrition_date_summary"
                        ) { _ ->
                            Column {
                                val kcal = totalOf("kcal")
                                if (effectiveTargets != null) {
                                    val rem = effectiveTargets.kcal - kcal
                                    val overColor = if (rollingBudgetEnabled) semantic.warn else semantic.low
                                    val ringColor = if (rem < 0) overColor else MaterialTheme.colorScheme.primary
                                    val pct = if (effectiveTargets.kcal > 0) {
                                        ((kcal / effectiveTargets.kcal) * 100.0).roundToInt().coerceAtLeast(0)
                                    } else 0

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        AnimatedCalorieRing(
                                            consumedKcal = kcal,
                                            targetKcal = effectiveTargets.kcal,
                                            ringColor = ringColor,
                                            isOver = rem < 0,
                                            size = 114.dp
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "السعرات اليومية",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = formatCounterLtr("${kcal.roundToInt()}", "${effectiveTargets.kcal}", "سعر"),
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontSize = 22.sp,
                                                    fontFeatureSettings = "tnum"
                                                ),
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            StatusBadgePill(
                                                text = if (rem < 0) {
                                                    "زيادة ${isolateLtr("${abs(rem).roundToInt()}")} سعر · ${isolateLtr("$pct%")}"
                                                } else {
                                                    "تم استهلاك ${isolateLtr("$pct%")}"
                                                },
                                                accentColor = if (rem < 0) overColor else semantic.ok
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))
                                    listOf(
                                        Triple("p", "بروتين", effectiveTargets.proteinG),
                                        Triple("c", "كارب", effectiveTargets.carbsG),
                                        Triple("f", "دهون", effectiveTargets.fatG)
                                    ).forEach { (k, label, goalG) ->
                                        val cur = totalOf(k)
                                        val macroColor = when (k) {
                                            "p" -> semantic.ok
                                            "c" -> MaterialTheme.colorScheme.primary
                                            else -> semantic.warn
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = formatCounterLtr("${cur.roundToInt()}", "$goalG", "جم"),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        MetricProgressBar(
                                            current = cur,
                                            target = goalG.coerceAtLeast(1).toDouble(),
                                            barColor = macroColor
                                        )
                                    }
                                    if (workoutSync != null || rollingBudget != null) {
                                        Spacer(Modifier.height(12.dp))
                                        SmartNutritionStatusStrip(
                                            workoutSync = workoutSync,
                                            rollingBudget = rollingBudget
                                        )
                                    }

                                    // Connected Steps & Burn -> Calorie Budget Strip (protects protein target)
                                    Spacer(Modifier.height(10.dp))
                                    val nf = remember { NumberFormat.getIntegerInstance(Locale.US) }
                                    val stepsAccent = Color(0xFF14B8A6)
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = stepsAccent.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, stepsAccent.copy(alpha = 0.24f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.selectTab(MainTab.STEPS)
                                            }
                                            .testTag("nutrition_steps_burn_link")
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                                                        contentDescription = null,
                                                        tint = stepsAccent,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    val stepsHeadline = if (stepsBurn.steps > 0 || stepsBurn.extraCardioKcal > 0) {
                                                        "${isolateLtr(nf.format(stepsBurn.steps))} خطوة · ${isolateLtr("+${stepsBurn.addedCaloriesTotal}")} سعر (${isolateLtr(stepsBurn.activePreset.percentLabel)})"
                                                    } else {
                                                        "${isolateLtr("BMR ${nf.format(stepsBurn.bmrReferenceKcal)}")} · سجّل خطواتك لتحديث السعرات"
                                                    }
                                                    Text(
                                                        text = stepsHeadline,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = "الخطوات والحرق",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = stepsAccent
                                                )
                                            }
                                            if (stepsBurn.totalActiveBurnKcal > 0) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "حماية البروتين (${isolateLtr("${effectiveTargets.proteinG}g")}) · حرق نشط ${isolateLtr("${stepsBurn.totalActiveBurnKcal}")} سعر",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        StepBufferPreset.entries.forEach { preset ->
                                                            val selected = stepsBurn.activePreset == preset
                                                            Surface(
                                                                shape = RoundedCornerShape(99.dp),
                                                                color = if (selected) stepsAccent else MaterialTheme.colorScheme.surface,
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    if (selected) stepsAccent else subtleCardBorderColor()
                                                                ),
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(99.dp))
                                                                    .clickable {
                                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                        viewModel.setStepBufferFactor(preset.factor)
                                                                    }
                                                                    .testTag("nutrition_buffer_${preset.name.lowercase()}")
                                                            ) {
                                                                Text(
                                                                    text = isolateLtr(preset.percentLabel),
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "${isolateLtr("${kcal.roundToInt()}")} سعر",
                                        style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "اكتب وزنك في \"الأهداف والبيانات\" تحت عشان يحسبلك الأهداف، أو سجّل قياس في تاب الجسم.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = semantic.warn,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    Text(
                                        text = "بروتين ${totalOf("p").roundToInt()} · كارب ${totalOf("c").roundToInt()} · دهون ${totalOf("f").roundToInt()}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Water tracker
                                val waterTarget = targets?.let { ((it.weightKg * 35.0) / 100.0).roundToInt() * 100 } ?: 2500
                                Spacer(Modifier.height(14.dp))
                                HorizontalDivider(color = subtleCardBorderColor())
                                Spacer(Modifier.height(12.dp))
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
                                            imageVector = Icons.Rounded.WaterDrop,
                                            contentDescription = null,
                                            tint = semantic.ok,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text("الماء", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = formatCounterLtr("${day.waterMl}", "$waterTarget", "مل"),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                MetricProgressBar(
                                    current = day.waterMl.toDouble(),
                                    target = waterTarget.toDouble(),
                                    barColor = semantic.ok
                                )
                                val foodWaterMl = totalOf("wat").roundToInt()
                                if (foodWaterMl > 0) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "+ $foodWaterMl مل رطوبة من الأكل (الإجمالي ${day.waterMl + foodWaterMl} مل)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.adjustWater(250)
                                        },
                                        modifier = Modifier.weight(1f).testTag("water_add_250")
                                    ) { Text("+250", maxLines = 1) }
                                    OutlinedButton(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.adjustWater(500)
                                        },
                                        modifier = Modifier.weight(1f).testTag("water_add_500")
                                    ) { Text("+500", maxLines = 1) }
                                    OutlinedButton(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.adjustWater(-250)
                                        },
                                        modifier = Modifier.weight(1f).testTag("water_sub_250")
                                    ) { Text("−250", maxLines = 1) }
                                }
                            }
                        }
                    }
                }
            }

            // Quick-Log Action Bar (AI Meal Photo | Barcode Scanner | Quick Search Add)
            item(key = "quick_log_bar") {
                QuickLogActionBar(
                    onPhotoClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        addSheetInitialMode = FoodSheetMode.MEAL_PHOTO
                        addSheetMealIndex = smartDefaultMealIndex()
                    },
                    onBarcodeClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        addSheetInitialMode = FoodSheetMode.BARCODE
                        addSheetMealIndex = smartDefaultMealIndex()
                    },
                    onQuickAddClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        addSheetInitialMode = FoodSheetMode.SEARCH
                        addSheetMealIndex = smartDefaultMealIndex()
                    }
                )
            }

            // 4 Meals + Supplements & Vitamins cards
            items(NutritionConstants.MEAL_NAMES.size, key = { "meal_$it" }) { mealIdx ->
                val mealName = NutritionConstants.MEAL_NAMES[mealIdx]
                val isSupplementSection = mealIdx == NutritionConstants.SUPPLEMENT_MEAL_INDEX
                val mealItems = day.items.filter { it.mealIndex == mealIdx }
                val mealKcal = mealItems.sumOf { it.nutrientAmount("kcal") }

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (isSupplementSection) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        else subtleCardBorderColor()
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mealName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = if (isSupplementSection && mealKcal < 1.0) {
                                    "${mealItems.size} صنف"
                                } else {
                                    "${mealKcal.roundToInt()} سعر · ${mealItems.sumOf { it.nutrientAmount("p") }.roundToInt()} جم بروتين"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isSupplementSection && mealItems.isNotEmpty()) {
                            val leuStatus = remember(mealItems) { computeMealLeucineStatus(mealItems) }
                            Spacer(Modifier.height(4.dp))
                            MealLeucineMpsChip(status = leuStatus)
                        }
                        if (isSupplementSection && vitDBreakdown.carryInIu > 0) {
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = semantic.ok.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, semantic.ok.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("vitamin_d_carryover_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WbSunny,
                                        contentDescription = null,
                                        tint = semantic.ok,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "فيتامين D مُغطى النهارده تلقائياً · ${isolateLtr("+${vitDBreakdown.carryInIu} IU")}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Bold,
                                            color = semantic.ok
                                        )
                                        val sourceLabel = (vitDBreakdown.yesterdaySourceName ?: "دافاليندي")
                                            .substringBefore("(")
                                            .trim()
                                            .ifBlank { "دافاليندي" }
                                        Text(
                                            text = "مُرحّل من جرعة أمس · $sourceLabel — نظام يوم أه ويوم لا",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        if (isSupplementSection && mealItems.isEmpty() && vitDBreakdown.carryInIu == 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "سجّل الفيتامينات والمكملات (مثل دافاليندي، فيتا ستريس، أوميجا 3، باور ماكس) عشان تتحسب تلقائيًا في الميكروز.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        mealItems.forEach { item ->
                            val isUnitSupp = SupplementCatalog.isUnitBasedLoggedItem(item)
                            val microSummary = if (isUnitSupp) {
                                SupplementCatalog.activeNutrientsSummary(item.nutrients, item.grams, maxItems = 3)
                            } else ""
                            val triggersVdCarryover = vitDBreakdown.carryOutIu > 0 &&
                                item.nutrientAmount("vD") > NutritionConstants.VITAMIN_D_DAILY_TARGET_MCG
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editingItem = item }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (isUnitSupp) {
                                            SupplementCatalog.formatServingCount(item.grams) +
                                                if (microSummary.isNotBlank()) " · $microSummary" else ""
                                        } else {
                                            "${item.grams} جم · ${NutritionConstants.SOURCE_LABELS[item.source] ?: ""}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isUnitSupp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (triggersVdCarryover) {
                                        Text(
                                            text = "فيتامين D: ${isolateLtr("5000 IU")} اليوم + ${isolateLtr("${vitDBreakdown.carryOutIu} IU")} مُرحّل لغداً تلقائياً",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.SemiBold,
                                            color = semantic.ok
                                        )
                                    }
                                }
                                if (!isUnitSupp || item.nutrientAmount("kcal") >= 1.0) {
                                    Text(
                                        text = "${item.nutrientAmount("kcal").roundToInt()} سعر · ${item.nutrientAmount("p").roundToInt()} بروتين",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val addInteraction = remember { MutableInteractionSource() }
                            Button(
                                onClick = {
                                    addSheetInitialMode = null
                                    addSheetMealIndex = mealIdx
                                },
                                interactionSource = addInteraction,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .tactilePressScale(addInteraction)
                                    .testTag("add_food_meal_$mealIdx")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Text(if (isSupplementSection) "أضف مكمل / فيتامين" else "أضف")
                            }
                            if (!isSupplementSection && mealItems.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { savingPresetMeal = mealName to mealItems },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("save_meal_preset_$mealIdx")
                                ) {
                                    Text("حفظ كوجبة جاهزة", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions Row: Copy yesterday + Food Battle
            item(key = "copy_yesterday") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.copyYesterdayMeals() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_yesterday_button")
                    ) {
                        Text("انسخ وجبات أمس")
                    }
                    OutlinedButton(
                        onClick = { showFoodBattle = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("food_battle_button")
                    ) {
                        Text("⚔️ مقارنة أكلتين")
                    }
                }
            }

            // V3 Smart Reverse Meal Builder ("فاضلي X سعر و Y بروتين — آكل إيه من اللي عندي؟")
            if (effectiveTargets != null) {
                item(key = "macro_tetris") {
                    val remKcal = (effectiveTargets.kcal - totalOf("kcal")).roundToInt().coerceAtLeast(0)
                    val remP = (effectiveTargets.proteinG - totalOf("p")).roundToInt().coerceAtLeast(0)
                    val remC = (effectiveTargets.carbsG - totalOf("c")).roundToInt().coerceAtLeast(0)
                    val remF = (effectiveTargets.fatG - totalOf("f")).roundToInt().coerceAtLeast(0)
                    ReverseMealBuilderCard(
                        remKcal = remKcal,
                        remProteinG = remP,
                        remCarbsG = remC,
                        remFatG = remF,
                        fridgePool = fridgePool,
                        groceryItems = groceryItems,
                        busyMessage = macroTetrisAi.busyMessage,
                        errorMessage = macroTetrisAi.errorMessage,
                        aiResult = macroTetrisAi.result,
                        onGenerateAi = { fridgeNames ->
                            viewModel.requestMacroTetris(remKcal, remP, remC, remF, fridgeNames)
                        },
                        onLogInstantMeal = { builtItems ->
                            viewModel.logReverseMealPlan(builtItems)
                        }
                    )
                }
            }

            // Supplement Timing & Nutrient Interaction Alerts
            item(key = "supplement_synergy") {
                SupplementSynergyCard(
                    dayItems = day.items,
                    totalOf = ::totalOf,
                    microReport = microReport
                )
            }

            // Daily Micronutrients (Dual-model: 7-day rolling Fat-Soluble pool + Daily-reset Water-Soluble & Minerals)
            item(key = "daily_micros") {
                val rda = if (snapshot.config.sex == "f") NutritionConstants.RDA_FEMALE else NutritionConstants.RDA_MALE
                val missingMicroCount = remember(day.items) {
                    val allTracked = NutritionConstants.MICRO_GROUP_MACROS +
                        NutritionConstants.MICRO_GROUP_VITAMINS +
                        NutritionConstants.MICRO_GROUP_MINERALS +
                        NutritionConstants.MICRO_GROUP_OTHER
                    allTracked.count { !hasDataFor(it) }
                }
                val topLowMicroLabels = remember(microReport) {
                    val shortMap = mapOf(
                        "vA" to "Vit A", "vD" to "Vit D", "vE" to "Vit E", "vK" to "Vit K",
                        "vC" to "Vit C", "b1" to "B1", "b2" to "B2", "b3" to "B3", "b5" to "B5",
                        "b6" to "B6", "fol" to "Folate", "b12" to "B12",
                        "ca" to "Ca", "fe" to "Fe", "mg" to "Mg", "pM" to "P",
                        "k" to "K", "zn" to "Zn", "cu" to "Cu", "mn" to "Mn", "se" to "Se"
                    )
                    val candidates = mutableListOf<Pair<String, Double>>()
                    microReport.fatSoluble.forEach { fs ->
                        if (fs.status == MicronutrientStatus.NEEDS_TOP_UP && fs.weeklyPercentage < 70.0) {
                            candidates.add((shortMap[fs.key] ?: fs.key) to fs.weeklyPercentage)
                        }
                    }
                    microReport.dailyByKey.values.forEach { d ->
                        if (!d.isLimitKey && d.dailyPercentage < 70.0 && d.key != "na") {
                            candidates.add((shortMap[d.key] ?: d.key) to d.dailyPercentage)
                        }
                    }
                    candidates.sortedBy { it.second }.take(2).map { it.first }
                }
                ExpandableSectionCard(
                    title = "الميكروز النهارده",
                    subtitleEn = "Daily & 7-Day Rolling Pool",
                    statusBadgeText = if (topLowMicroLabels.isNotEmpty()) {
                        "ناقص: ${isolateLtr(topLowMicroLabels.joinToString(", "))}"
                    } else "ممتاز ✓",
                    statusBadgeColor = if (topLowMicroLabels.isNotEmpty()) semantic.warn else semantic.ok,
                    leadingIcon = Icons.Rounded.Science,
                    testTag = "daily_micros_card"
                ) {
                    if (day.items.isNotEmpty() && (missingMicroCount > 0 || enrichMicrosBusy != null)) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "فيه $missingMicroCount عناصر لسه مفيهاش بيانات في أصناف اليوم (أصناف متسجلة قبل تحديث القاعدة أو أكلات مخصصة). استكملها بضغطة واحدة:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if (enrichMicrosBusy != null) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Text(
                                        text = enrichMicrosBusy!!,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.enrichDayLoggedFoods(useGeminiAi = false) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            modifier = Modifier.weight(1f).testTag("enrich_micros_db_button")
                                        ) {
                                            Text("⚡ استكمال من القاعدة", style = MaterialTheme.typography.labelMedium)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.enrichDayLoggedFoods(useGeminiAi = true) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            modifier = Modifier.weight(1f).testTag("enrich_micros_ai_button")
                                        ) {
                                            Text("✨ تحليل بـ Gemini", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 1. Fat-Soluble Vitamins (A, D3, E, K) -> 7-Day Rolling Reserve Pool Dual-Indicator
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "الفيتامينات الذائبة في الدهون (مخزون 7 أيام متجدد)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تُخزّن في الجسم ولا تُصفّر منتصف الليل — يُحسب مخزون آخر 7 أيام (اليوم + 6 أيام سابقة).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                    )
                    microReport.fatSoluble.forEach { fsEval ->
                        FatSolubleDualIndicatorCard(eval = fsEval)
                    }

                    // 2. Water-Soluble Vitamins (B-Complex, C) -> Daily Reset
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "الفيتامينات الذائبة في الماء (تتجدد يومياً)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    MicronutrientEngine.WATER_SOLUBLE_KEYS.forEach { k ->
                        val eval = microReport.dailyByKey[k] ?: return@forEach
                        DailyNutrientRow(eval = eval)
                    }

                    // 3. Minerals (12) -> Daily Reset + Food vs Supplement UL Distinction
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "المعادن (12 — تتجدد يومياً)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    MicronutrientEngine.MINERAL_KEYS.forEach { k ->
                        val eval = microReport.dailyByKey[k] ?: return@forEach
                        DailyNutrientRow(eval = eval)
                    }

                    // 4. Macro/Amino/Fatty Acid Breakdown & Other Tracked Nutrients
                    val extraSections = listOf(
                        "تفاصيل الماكروز والأحماض والدهون" to NutritionConstants.MICRO_GROUP_MACROS,
                        "أخرى" to NutritionConstants.MICRO_GROUP_OTHER
                    )
                    extraSections.forEach { (sectionTitle, keys) ->
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = sectionTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        keys.forEach { k ->
                            val targetVal = rda[k] ?: return@forEach
                            val v = totalOf(k)
                            val has = hasDataFor(k)
                            val isLim = k in NutritionConstants.LIMIT_KEYS
                            val formattedVal = (v * 10).roundToInt() / 10.0
                            val unit = NutritionConstants.NUTRIENT_UNITS[k] ?: ""
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = (NutritionConstants.NUTRIENT_LABELS[k] ?: k) + if (has) "" else " (مفيش بيانات)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = if (isLim) {
                                        "حد ${formatCounterLtr("$formattedVal", "$targetVal", unit)}"
                                    } else {
                                        formatCounterLtr("$formattedVal", "$targetVal", unit)
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum")
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            MetricProgressBar(
                                current = v,
                                target = targetVal,
                                barColor = if (isLim && v > targetVal) semantic.low else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    val foodMoisture = totalOf("wat")
                    if (hasDataFor("wat")) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = NutritionConstants.NUTRIENT_LABELS["wat"] ?: "ماء الأكل",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${foodMoisture.roundToInt()} مل",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "كل الأصناف في قاعدة الأغذية (12,903 صنف — الجدول المصري + USDA + Foundation) والتحليل بـ Gemini بقت بتحسب كل الـ 40 عنصر كاملين تلقائيًا.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Goals & Profile editor
            item(key = "goals_profile") {
                NutritionGoalsCard(snapshot = snapshot, targets = targets, viewModel = viewModel)
            }

            // Weekly Review + Adaptive TDEE
            item(key = "weekly_review") {
                WeeklyReviewCard(
                    snapshot = snapshot,
                    targets = targets,
                    onApplyAdaptiveKcal = viewModel::applyAdaptiveKcal
                )
            }

            // Weekly Micros + Offline & Gemini AI Suggestions
            item(key = "weekly_micros") {
                val remKcal = targets?.let { (it.kcal - totalOf("kcal")).roundToInt().coerceAtLeast(0) }
                WeeklyMicrosCard(
                    snapshot = snapshot,
                    suggestions = microSuggestions,
                    deficiencyAi = deficiencyAi,
                    onNeedSuggestions = { lowKeys -> viewModel.loadMicroSuggestions(lowKeys, snapshot.config.sex) },
                    onRequestAiFix = { lowPairs -> viewModel.requestDeficiencyAiAdvice(lowPairs, remKcal) }
                )
            }
        }
    }

    if (savingPresetMeal != null) {
        val (mName, mItems) = savingPresetMeal!!
        SaveMealPresetDialog(
            defaultMealName = mName,
            itemCount = mItems.size,
            totalKcal = mItems.sumOf { it.nutrientAmount("kcal") }.roundToInt(),
            onDismiss = { savingPresetMeal = null },
            onSave = { presetName ->
                viewModel.saveMealAsPreset(presetName, mItems)
                savingPresetMeal = null
            }
        )
    }

    if (showFoodBattle) {
        FoodBattleDialog(
            searchResultsA = battleResultsA,
            searchResultsB = battleResultsB,
            onSearchA = { viewModel.searchFoodBattle(true, it) },
            onSearchB = { viewModel.searchFoodBattle(false, it) },
            onDismiss = { showFoodBattle = false }
        )
    }

    // Add Food Bottom Sheet
    if (addSheetMealIndex != null) {
        AddFoodSheet(
            initialMealIndex = addSheetMealIndex!!,
            initialMode = addSheetInitialMode,
            snapshot = snapshot,
            searchResults = searchResults,
            aiState = aiState,
            viewModel = viewModel,
            onDismiss = {
                addSheetMealIndex = null
                addSheetInitialMode = null
                viewModel.clearFoodSheetAi()
            }
        )
    }

    // Edit Logged Food Dialog (with live calorie & macro preview, Raw/Cooked toggle, and Smart Macro-Swapper!)
    if (editingItem != null) {
        EditLoggedFoodDialog(
            item = editingItem!!,
            onDismiss = { editingItem = null },
            onSave = { grams, mealIdx ->
                viewModel.updateLoggedFood(editingItem!!.id, grams, mealIdx)
                editingItem = null
            },
            onSwapFood = { replacementFood, newGrams, mealIdx ->
                viewModel.replaceLoggedFood(editingItem!!.id, replacementFood, newGrams, mealIdx)
                editingItem = null
            },
            onDelete = {
                viewModel.deleteLoggedFood(editingItem!!.id)
                editingItem = null
            }
        )
    }
}

@Composable
private fun QuickLogActionBar(
    onPhotoClick: () -> Unit,
    onBarcodeClick: () -> Unit,
    onQuickAddClick: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("nutrition_quick_log_bar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickLogActionCard(
            title = "تصوير وجبة",
            subtitle = "AI Vision",
            icon = Icons.Rounded.CameraAlt,
            accentColor = MaterialTheme.colorScheme.primary,
            testTag = "quick_log_ai_photo",
            onClick = onPhotoClick,
            modifier = Modifier.weight(1f)
        )
        QuickLogActionCard(
            title = "باركود",
            subtitle = "Scan",
            icon = Icons.Rounded.QrCodeScanner,
            accentColor = semantic.ok,
            testTag = "quick_log_barcode",
            onClick = onBarcodeClick,
            modifier = Modifier.weight(1f)
        )
        QuickLogActionCard(
            title = "إضافة سريعة",
            subtitle = "Search",
            icon = Icons.Rounded.AddCircleOutline,
            accentColor = semantic.warn,
            testTag = "quick_log_search",
            onClick = onQuickAddClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickLogActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, subtleCardBorderColor()),
        modifier = modifier
            .heightIn(min = 78.dp)
            .tactilePressScale(interaction)
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = isolateLtr(subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditLoggedFoodDialog(
    item: LoggedFoodItem,
    onDismiss: () -> Unit,
    onSave: (Double, Int) -> Unit,
    onSwapFood: (FoodItem, Double, Int) -> Unit,
    onDelete: () -> Unit
) {
    val isUnitSupp = remember(item) { SupplementCatalog.isUnitBasedLoggedItem(item) }
    val yieldProfile = remember(item, isUnitSupp) {
        if (isUnitSupp) null else CookingYieldEngine.detectProfile(item.name)
    }
    val itemIntrinsicMode = remember(item, yieldProfile) {
        CookingYieldEngine.detectItemIntrinsicState(item.name, kcalPer100 = item.nutrients.getOrNull(0) ?: -1.0)
    }
    var weighedMode by remember(item, itemIntrinsicMode) { mutableStateOf(itemIntrinsicMode) }
    var showSwapper by remember(item) { mutableStateOf(false) }

    var amountText by remember(item) {
        val initial = if (isUnitSupp) item.grams / 100.0 else item.grams
        mutableStateOf(if (initial % 1.0 == 0.0) initial.toInt().toString() else initial.toString())
    }
    var mealIdx by remember(item) { mutableIntStateOf(item.mealIndex) }
    val rawVal = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val enteredGrams = if (isUnitSupp) rawVal * 100.0 else rawVal
    val gVal = if (yieldProfile != null && !isUnitSupp) {
        CookingYieldEngine.convertToDbGrams(enteredGrams, weighedMode, itemIntrinsicMode, yieldProfile.yieldFactor)
    } else enteredGrams
    val previewItem = item.copy(grams = gVal)
    val swapOptions = remember(previewItem, showSwapper, isUnitSupp) {
        if (showSwapper && !isUnitSupp) MacroSwapperEngine.generateSwaps(previewItem) else emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(if (isUnitSupp) "عدد الأقراص / الكبسولات" else "الوزن بالجرام") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("edit_food_grams_input")
                )
                if (yieldProfile != null && !isUnitSupp) {
                    RawCookedToggleBar(
                        profile = yieldProfile,
                        itemIntrinsicMode = itemIntrinsicMode,
                        selectedWeighedMode = weighedMode,
                        enteredGrams = enteredGrams,
                        onModeChange = { weighedMode = it }
                    )
                }
                val microLine = if (isUnitSupp) {
                    SupplementCatalog.activeNutrientsSummary(previewItem.nutrients, gVal, maxItems = 4)
                } else ""
                Text(
                    text = if (microLine.isNotBlank()) {
                        microLine
                    } else {
                        "${previewItem.nutrientAmount("kcal").roundToInt()} سعر · بروتين ${previewItem.nutrientAmount("p").roundToInt()} · كارب ${previewItem.nutrientAmount("c").roundToInt()} · دهون ${previewItem.nutrientAmount("f").roundToInt()}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (!isUnitSupp) {
                    OutlinedButton(
                        onClick = { showSwapper = !showSwapper },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_macro_swapper_button")
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (showSwapper) "إخفاء بدائل المكون" else "بدّل المكون بنفس الماكروز (Smart Swap)")
                    }
                    if (showSwapper) {
                        MacroSwapperPanel(
                            swaps = swapOptions,
                            onPickSwap = { opt ->
                                onSwapFood(opt.food, opt.suggestedGrams, mealIdx)
                            }
                        )
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NutritionConstants.MEAL_NAMES.forEachIndexed { idx, mn ->
                        FilterChip(
                            selected = idx == mealIdx,
                            onClick = { mealIdx = idx },
                            label = { Text(mn) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (gVal > 0.0) onSave(gVal, mealIdx) },
                modifier = Modifier.testTag("save_edit_food_button")
            ) { Text("احفظ") }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_logged_food_button")
                ) {
                    Text("احذف", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("إغلاق") }
            }
        }
    )
}

@Composable
private fun NutritionGoalsCard(
    snapshot: AppDataSnapshot,
    targets: NutritionTargets?,
    viewModel: MainViewModel
) {
    val cfg = snapshot.config
    val semantic = LocalSemanticColors.current
    var ageText by remember(cfg.age) { mutableStateOf(cfg.age.toString()) }
    var heightText by remember(cfg.heightCm) { mutableStateOf(cfg.heightCm.toString()) }
    var weightText by remember(cfg.weightOverride) { mutableStateOf(if (cfg.weightOverride > 0) cfg.weightOverride.toString() else "") }
    var bfText by remember(cfg.bodyFatOverride) { mutableStateOf(if (cfg.bodyFatOverride > 0) cfg.bodyFatOverride.toString() else "") }
    var kcalText by remember(cfg.kcalOverride) { mutableStateOf(if (cfg.kcalOverride > 0) cfg.kcalOverride.toString() else "") }
    var proText by remember(cfg.proteinOverride) { mutableStateOf(if (cfg.proteinOverride > 0) cfg.proteinOverride.toString() else "") }
    var carbText by remember(cfg.carbsOverride) { mutableStateOf(if (cfg.carbsOverride > 0) cfg.carbsOverride.toString() else "") }
    var fatText by remember(cfg.fatOverride) { mutableStateOf(if (cfg.fatOverride > 0) cfg.fatOverride.toString() else "") }

    ExpandableSectionCard(
        title = "الأهداف والبيانات",
        subtitleEn = "Targets & Smart Engines",
        statusBadgeText = targets?.let { "${it.kcal} سعر" },
        leadingIcon = Icons.Rounded.Tune,
        testTag = "nutrition_goals_card"
    ) {
        // Sex selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = cfg.sex != "f",
                onClick = { viewModel.updateConfig(cfg.copy(sex = "m")) },
                label = { Text("ذكر") }
            )
            FilterChip(
                selected = cfg.sex == "f",
                onClick = { viewModel.updateConfig(cfg.copy(sex = "f")) },
                label = { Text("أنثى") }
            )
        }
        Spacer(Modifier.height(6.dp))
        // Goal selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("bulk" to "تضخيم", "cut" to "تنشيف", "keep" to "ثبات").forEach { (code, label) ->
                FilterChip(
                    selected = cfg.goal == code,
                    onClick = { viewModel.updateConfig(cfg.copy(goal = code)) },
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        // Activity selector
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1.375 to "3 تمرينات", 1.55 to "4–5 تمرينات", 1.725 to "6–7 تمرينات").forEach { (act, label) ->
                FilterChip(
                    selected = abs(cfg.activityFactor - act) < 0.01,
                    onClick = { viewModel.updateConfig(cfg.copy(activityFactor = act)) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ageText, onValueChange = { ageText = it },
                label = { Text("السن") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = heightText, onValueChange = { heightText = it },
                label = { Text("الطول سم") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = weightText, onValueChange = { weightText = it },
                label = { Text("الوزن كجم") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = bfText, onValueChange = { bfText = it },
                label = { Text("دهون %") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = kcalText, onValueChange = { kcalText = it },
                label = { Text("سعرات يدوي") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = proText, onValueChange = { proText = it },
                label = { Text("بروتين جم") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = carbText, onValueChange = { carbText = it },
                label = { Text("كارب جم") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = fatText, onValueChange = { fatText = it },
                label = { Text("دهون جم") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                viewModel.updateConfig(
                    cfg.copy(
                        age = ageText.toIntOrNull()?.coerceIn(10, 120) ?: cfg.age,
                        heightCm = heightText.toDoubleOrNull()?.coerceIn(100.0, 250.0) ?: cfg.heightCm,
                        weightOverride = weightText.toDoubleOrNull()?.coerceIn(0.0, 350.0) ?: 0.0,
                        bodyFatOverride = bfText.toDoubleOrNull()?.coerceIn(0.0, 70.0) ?: 0.0,
                        kcalOverride = kcalText.toIntOrNull()?.coerceIn(0, 10000) ?: 0,
                        proteinOverride = proText.toIntOrNull()?.coerceIn(0, 1000) ?: 0,
                        carbsOverride = carbText.toIntOrNull()?.coerceIn(0, 2000) ?: 0,
                        fatOverride = fatText.toIntOrNull()?.coerceIn(0, 500) ?: 0
                    )
                )
                viewModel.showToast("تم تحديث الأهداف ✅")
            },
            modifier = Modifier.fillMaxWidth().testTag("save_nutrition_goals_button")
        ) {
            Text("احفظ الأهداف")
        }
        val rollingEnabled by viewModel.rollingCalorieBudgetEnabled.collectAsStateWithLifecycle()
        val workoutSynced by viewModel.workoutSyncedNutritionEnabled.collectAsStateWithLifecycle()
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text("مرونة الـ 7 أيام للسعرات (Rolling Budget)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("يوزّع أي زيادة في يوم على باقي الأسبوع بذكاء بدون يوم أحمر.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = rollingEnabled,
                onCheckedChange = { viewModel.setRollingCalorieBudgetEnabled(it) },
                modifier = Modifier.testTag("toggle_rolling_budget_switch")
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text("ربط الماكروز بالتمرين (Workout-Synced)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("يرفع الكارب أيام الرجل/التمرين العالي ويخفضه أيام الراحة مع رفع الدهون الصحية.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = workoutSynced,
                onCheckedChange = { viewModel.setWorkoutSyncedNutritionEnabled(it) },
                modifier = Modifier.testTag("toggle_workout_synced_switch")
            )
        }
        if (targets != null) {
            if (targets.hint.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(targets.hint, style = MaterialTheme.typography.bodySmall, color = semantic.warn, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "الحساب: معدل الحرق الأساسي ${targets.bmr} سعر، والحرق اليومي ~${targets.tdee}. تقدير بمعادلات Katch-McArdle (لو دخلت الدهون %) وإلا Mifflin-St Jeor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyReviewCard(
    snapshot: AppDataSnapshot,
    targets: NutritionTargets?,
    onApplyAdaptiveKcal: (Int) -> Unit
) {
    val semantic = LocalSemanticColors.current
    val loggedDays = snapshot.nutritionDays.values
        .filter { it.items.isNotEmpty() }
        .sortedBy { it.date }
        .takeLast(7)
    val adaptiveTdee = remember(snapshot.nutritionDays, snapshot.bodyRecords, snapshot.config.goal) {
        computeAdaptiveTdee(snapshot.nutritionDays, snapshot.bodyRecords, snapshot.config.goal)
    }

    ExpandableSectionCard(
        title = "حرقك الفعلي والمراجعة الأسبوعية",
        subtitleEn = "Adaptive TDEE",
        statusBadgeText = adaptiveTdee?.let { "${it.actualTdee} سعر" } ?: "قيد الحساب",
        statusBadgeColor = if (adaptiveTdee != null) semantic.ok else MaterialTheme.colorScheme.primary,
        leadingIcon = Icons.Rounded.LocalFireDepartment,
        testTag = "weekly_review_card"
    ) {
        AdaptiveTdeeSection(
            estimate = adaptiveTdee,
            theoreticalTargets = targets,
            onApplyRecommendedKcal = onApplyAdaptiveKcal
        )
        Spacer(Modifier.height(10.dp))
        if (loggedDays.size < 3) {
            Text(
                text = "سجّل أكلك 3 أيام على الأقل عشان تظهر المراجعة الأسبوعية.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ExpandableSectionCard
        }
        val avgKcal = (loggedDays.sumOf { d -> d.items.sumOf { it.nutrientAmount("kcal") } } / loggedDays.size).roundToInt()
        Text(
            text = "متوسط سعراتك آخر ${loggedDays.size} أيام مسجلة: $avgKcal سعر" + (targets?.let { " (الهدف ${it.kcal})" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        val cutoff = shiftIsoDate(todayIso(), -28)
        val recentWeights = snapshot.bodyRecords
            .filter { (it.weightKg ?: 0.0) > 0.0 && it.date >= cutoff }
            .sortedBy { it.date }

        Spacer(Modifier.height(6.dp))
        if (recentWeights.size >= 2) {
            val first = recentWeights.first()
            val last = recentWeights.last()
            val days = daysBetweenIso(first.date, last.date)
            if (days >= 7) {
                val rate = (last.weightKg!! - first.weightKg!!) / (days / 7.0)
                val pct = rate / last.weightKg * 100.0
                val goal = snapshot.config.goal
                val (lo, hi) = when (goal) {
                    "cut" -> -1.0 to -0.5
                    "keep" -> -0.25 to 0.25
                    else -> 0.25 to 0.5
                }
                Text(
                    text = "تغير الوزن: ${String.format("%.2f", rate)} كجم/أسبوع (${String.format("%.2f", pct)}% من وزنك).",
                    style = MaterialTheme.typography.bodyMedium
                )
                val advice = when {
                    pct < lo -> "أبطأ من المستهدف. زوّد حوالي 150 سعر باليوم وراقب أسبوعين."
                    pct > hi -> "أسرع من المستهدف. قلّل حوالي 150 سعر باليوم وراقب أسبوعين."
                    else -> "ماشي في المعدل المستهدف. كمّل زي ما انت."
                }
                Text(
                    text = advice,
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.warn,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "محتاج قياسين للوزن بينهم أسبوع على الأقل.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "سجّل وزنك في تاب الجسم مرتين على الأقل عشان أقارن.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyMicrosCard(
    snapshot: AppDataSnapshot,
    suggestions: Map<String, List<NutrientFoodSuggestion>>,
    deficiencyAi: AiDeficiencyUiState,
    onNeedSuggestions: (List<String>) -> Unit,
    onRequestAiFix: (List<Pair<String, Double>>) -> Unit
) {
    val semantic = LocalSemanticColors.current
    val today = todayIso()
    val rda = if (snapshot.config.sex == "f") NutritionConstants.RDA_FEMALE else NutritionConstants.RDA_MALE
    val days = snapshot.nutritionDays.values
        .filter { it.date <= today && it.items.isNotEmpty() }
        .sortedBy { it.date }
        .takeLast(7)

    val microReportToday = remember(today, snapshot.nutritionDays, snapshot.config.sex) {
        MicronutrientEngine.evaluateMicronutrients(
            referenceDate = today,
            nutritionDays = snapshot.nutritionDays,
            sex = snapshot.config.sex
        )
    }

    val rows = remember(days, rda, microReportToday) {
        if (days.size < 3) return@remember emptyList<Pair<String, Double>>()
        val count = NutritionConstants.NUTRIENT_COUNT
        val sum = DoubleArray(count)
        val has = BooleanArray(count)
        days.forEach { d ->
            d.items.forEach { item ->
                item.nutrients.forEachIndexed { idx, v ->
                    if (idx < count && v >= 0.0) {
                        sum[idx] += v * item.grams / 100.0
                        has[idx] = true
                    }
                }
            }
        }
        rda.entries.mapNotNull { (k, target) ->
            // Fat-soluble vitamins (A, D3, E, K) use the exact 7-Day Rolling Pool percentage
            val fs = microReportToday.fatSolubleByKey[k]
            if (fs != null) {
                return@mapNotNull if (!fs.hasAnyDataInWindow) null else k to fs.weeklyPercentage
            }
            val idx = NutritionConstants.NUTRIENT_INDEX[k] ?: return@mapNotNull null
            if (!has[idx]) null else k to ((sum[idx] / days.size) / target * 100.0)
        }
    }

    val lowThree = remember(rows, microReportToday) {
        rows.filter { (k, pct) ->
            if (k in NutritionConstants.LIMIT_KEYS) return@filter false
            val fs = microReportToday.fatSolubleByKey[k]
            if (fs != null) {
                fs.status == MicronutrientStatus.NEEDS_TOP_UP && fs.weeklyPercentage < 70.0
            } else {
                pct < 70.0
            }
        }
            .sortedBy { it.second }
            .take(3)
    }

    LaunchedEffect(lowThree) {
        if (lowThree.isNotEmpty()) {
            onNeedSuggestions(lowThree.map { it.first })
        }
    }

    ExpandableSectionCard(
        title = "الميكروز: متوسط الأسبوع",
        subtitleEn = "7-Day Average & AI Fixer",
        statusBadgeText = when {
            days.size < 3 -> "قيد التجميع"
            lowThree.isNotEmpty() -> "${lowThree.size} عناصر ناقصة"
            else -> "متوازن ✓"
        },
        statusBadgeColor = if (lowThree.isNotEmpty()) semantic.warn else semantic.ok,
        leadingIcon = Icons.Rounded.Insights,
        testTag = "weekly_micros_card"
    ) {
        if (days.size < 3) {
            Text(
                text = "سجّل أكلك 3 أيام على الأقل عشان يظهر المتوسط.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ExpandableSectionCard
        }
        Text(
            text = "متوسط ${days.size} أيام مسجّلة كنسبة من الاحتياج التقريبي:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        rows.forEach { (k, pct) ->
            val isLim = k in NutritionConstants.LIMIT_KEYS
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(NutritionConstants.NUTRIENT_LABELS[k] ?: k, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "${pct.roundToInt()}%" + if (isLim && pct > 100) " (أعلى من الحد)" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(3.dp))
            MetricProgressBar(
                current = pct,
                target = 100.0,
                barColor = if (isLim && pct > 100) semantic.low else MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(10.dp))
        if (lowThree.isNotEmpty()) {
            Text(
                text = "أقل من 70% من احتياجك: " + lowThree.joinToString("، ") { NutritionConstants.NUTRIENT_LABELS[it.first] ?: it.first },
                style = MaterialTheme.typography.bodySmall,
                color = semantic.warn,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            lowThree.forEach { (k, _) ->
                val list = suggestions[k].orEmpty()
                if (list.isNotEmpty()) {
                    val text = list.joinToString("، ") { s ->
                        val p = if (s.rdaPercentPer100g > 300) ">300" else s.rdaPercentPer100g.roundToInt().toString()
                        "${s.name} ($p%)"
                    }
                    Text(
                        text = "${NutritionConstants.NUTRIENT_LABELS[k]}: $text",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            AiDeficiencyFixerBlock(
                busyMessage = deficiencyAi.busyMessage,
                errorMessage = deficiencyAi.errorMessage,
                result = deficiencyAi.result,
                onRequestFix = {
                    val allLow = rows
                        .filter { (k, pct) ->
                            if (k in NutritionConstants.LIMIT_KEYS) return@filter false
                            val fs = microReportToday.fatSolubleByKey[k]
                            if (fs != null) {
                                fs.status == MicronutrientStatus.NEEDS_TOP_UP && fs.weeklyPercentage < 75.0
                            } else {
                                pct < 75.0
                            }
                        }
                        .sortedBy { it.second }
                        .take(6)
                    onRequestAiFix(allLow)
                }
            )
        } else {
            Text(
                text = "مفيش عنصر واضح تحت 70%.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
