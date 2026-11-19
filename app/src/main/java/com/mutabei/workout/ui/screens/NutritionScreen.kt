package com.mutabei.workout.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mutabei.workout.R
import com.mutabei.workout.data.model.LoggedFoodItem
import com.mutabei.workout.data.model.NutritionConstants
import com.mutabei.workout.data.model.NutritionDay
import com.mutabei.workout.data.model.NutritionTargets
import com.mutabei.workout.data.model.SupplementCatalog
import com.mutabei.workout.data.model.daysBetweenIso
import com.mutabei.workout.data.model.shiftIsoDate
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.data.repository.NutrientFoodSuggestion
import com.mutabei.workout.ui.components.ExpandableSectionCard
import com.mutabei.workout.ui.components.MetricProgressBar
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.viewmodel.AppDataSnapshot
import com.mutabei.workout.ui.viewmodel.FoodSheetAiState
import com.mutabei.workout.ui.viewmodel.MainViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun NutritionScreen(
    snapshot: AppDataSnapshot,
    selectedDate: String,
    searchResults: List<com.mutabei.workout.data.model.FoodItem>,
    aiState: FoodSheetAiState,
    microSuggestions: Map<String, List<NutrientFoodSuggestion>>,
    viewModel: MainViewModel
) {
    val semantic = LocalSemanticColors.current
    val today = todayIso()
    val day = snapshot.nutritionDays[selectedDate] ?: NutritionDay(date = selectedDate)
    val targets = remember(snapshot.config, snapshot.bodyRecords) {
        viewModel.computeTargets(snapshot.config, snapshot.bodyRecords)
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
        val idx = NutritionConstants.NUTRIENT_INDEX[key] ?: return 0.0
        return totals.first[idx]
    }
    fun hasDataFor(key: String): Boolean {
        val idx = NutritionConstants.NUTRIENT_INDEX[key] ?: return false
        return totals.second[idx]
    }

    var addSheetMealIndex by remember { mutableStateOf<Int?>(null) }
    var editingItem by remember { mutableStateOf<LoggedFoodItem?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary + Date switcher + Water card
            item(key = "nutrition_summary") {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                text = if (selectedDate == today) "النهارده" else selectedDate,
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

                        val kcal = totalOf("kcal")
                        if (targets != null) {
                            val rem = targets.kcal - kcal
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${abs(rem).roundToInt()}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = if (rem < 0) semantic.low else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "سعر ${if (rem >= 0) "فاضلين" else "زيادة"} من ${targets.kcal}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            MetricProgressBar(
                                current = kcal,
                                target = targets.kcal.toDouble(),
                                barColor = if (rem < 0) semantic.low else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(10.dp))
                            listOf(
                                Triple("p", "بروتين", targets.proteinG),
                                Triple("c", "كارب", targets.carbsG),
                                Triple("f", "دهون", targets.fatG)
                            ).forEach { (k, label, goalG) ->
                                val cur = totalOf(k)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                    Text("${cur.roundToInt()} / $goalG جم", style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.height(3.dp))
                                MetricProgressBar(current = cur, target = goalG.coerceAtLeast(1).toDouble())
                            }
                        } else {
                            Text(
                                text = "${kcal.roundToInt()} سعر",
                                style = MaterialTheme.typography.displaySmall
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
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Water tracker
                        val waterTarget = targets?.let { ((it.weightKg * 35.0) / 100.0).roundToInt() * 100 } ?: 2500
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ماء", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("${day.waterMl} / $waterTarget مل", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(4.dp))
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
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.adjustWater(250) },
                                modifier = Modifier.testTag("water_add_250")
                            ) { Text("+250") }
                            OutlinedButton(
                                onClick = { viewModel.adjustWater(500) },
                                modifier = Modifier.testTag("water_add_500")
                            ) { Text("+500") }
                            OutlinedButton(
                                onClick = { viewModel.adjustWater(-250) },
                                modifier = Modifier.testTag("water_sub_250")
                            ) { Text("−250") }
                        }
                    }
                }
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
                        else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                    "${mealKcal.roundToInt()} سعر"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSupplementSection && mealItems.isEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "سجّل الفيتامينات والمكملات (مثل فيتا ستريس، أوميجا 3، باور ماكس) عشان تتحسب تلقائيًا في الميكروز.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        mealItems.forEach { item ->
                            val isUnitSupp = SupplementCatalog.isUnitBasedLoggedItem(item)
                            val microSummary = if (isUnitSupp) {
                                SupplementCatalog.activeNutrientsSummary(item.nutrients, item.grams, maxItems = 3)
                            } else ""
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
                        Button(
                            onClick = { addSheetMealIndex = mealIdx },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("add_food_meal_$mealIdx")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Text(if (isSupplementSection) "أضف مكمل / فيتامين" else "أضف")
                        }
                    }
                }
            }

            // Copy yesterday button
            item(key = "copy_yesterday") {
                OutlinedButton(
                    onClick = { viewModel.copyYesterdayMeals() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("copy_yesterday_button")
                ) {
                    Text("انسخ وجبات أمس")
                }
            }

            // Daily Micronutrients (grouped cleanly: macros/amino, 12 vitamins, 12 minerals, other)
            item(key = "daily_micros") {
                val rda = if (snapshot.config.sex == "f") NutritionConstants.RDA_FEMALE else NutritionConstants.RDA_MALE
                val sections = listOf(
                    "تفاصيل الماكروز والأحماض والدهون" to NutritionConstants.MICRO_GROUP_MACROS,
                    "الفيتامينات (12)" to NutritionConstants.MICRO_GROUP_VITAMINS,
                    "المعادن (12)" to NutritionConstants.MICRO_GROUP_MINERALS,
                    "أخرى" to NutritionConstants.MICRO_GROUP_OTHER
                )
                ExpandableSectionCard(
                    title = "الميكروز النهارده (تفصيل كامل للبالغين)",
                    testTag = "daily_micros_card"
                ) {
                    sections.forEachIndexed { sIdx, (sectionTitle, keys) ->
                        if (sIdx > 0) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        }
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
                            val valueText = if (k == "vD") {
                                val iuCur = (v * 40.0).roundToInt()
                                val iuTarget = (targetVal * 40.0).roundToInt()
                                "$iuCur / $iuTarget IU ($formattedVal مكجم)"
                            } else {
                                "$formattedVal / ${if (isLim) "حد " else ""}$targetVal $unit"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = (NutritionConstants.NUTRIENT_LABELS[k] ?: k) + if (has) "" else " (مفيش بيانات)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = valueText,
                                    style = MaterialTheme.typography.bodySmall
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
                        text = "الأصناف اللي بتتحلل بـ Gemini بتجيب كل الميكروز كاملة (USDA + الجدول المصري). الأصناف القديمة اللي مفيهاش قيمة لعنصر معين بتتحسب صفر فيه.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Goals & Profile editor
            item(key = "goals_profile") {
                NutritionGoalsCard(snapshot = snapshot, targets = targets, viewModel = viewModel)
            }

            // Weekly Review
            item(key = "weekly_review") {
                WeeklyReviewCard(snapshot = snapshot, targets = targets)
            }

            // Weekly Micros + Suggestions
            item(key = "weekly_micros") {
                WeeklyMicrosCard(
                    snapshot = snapshot,
                    suggestions = microSuggestions,
                    onNeedSuggestions = { lowKeys -> viewModel.loadMicroSuggestions(lowKeys, snapshot.config.sex) }
                )
            }
        }
    }

    // Add Food Bottom Sheet
    if (addSheetMealIndex != null) {
        AddFoodSheet(
            initialMealIndex = addSheetMealIndex!!,
            snapshot = snapshot,
            searchResults = searchResults,
            aiState = aiState,
            viewModel = viewModel,
            onDismiss = {
                addSheetMealIndex = null
                viewModel.clearFoodSheetAi()
            }
        )
    }

    // Edit Logged Food Dialog (with live calorie & macro preview!)
    if (editingItem != null) {
        EditLoggedFoodDialog(
            item = editingItem!!,
            onDismiss = { editingItem = null },
            onSave = { grams, mealIdx ->
                viewModel.updateLoggedFood(editingItem!!.id, grams, mealIdx)
                editingItem = null
            },
            onDelete = {
                viewModel.deleteLoggedFood(editingItem!!.id)
                editingItem = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditLoggedFoodDialog(
    item: LoggedFoodItem,
    onDismiss: () -> Unit,
    onSave: (Double, Int) -> Unit,
    onDelete: () -> Unit
) {
    val isUnitSupp = remember(item) { SupplementCatalog.isUnitBasedLoggedItem(item) }
    var amountText by remember(item) {
        val initial = if (isUnitSupp) item.grams / 100.0 else item.grams
        mutableStateOf(if (initial % 1.0 == 0.0) initial.toInt().toString() else initial.toString())
    }
    var mealIdx by remember(item) { mutableIntStateOf(item.mealIndex) }
    val rawVal = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val gVal = if (isUnitSupp) rawVal * 100.0 else rawVal
    val previewItem = item.copy(grams = gVal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(if (isUnitSupp) "عدد الأقراص / الكبسولات" else "الوزن بالجرام") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("edit_food_grams_input")
                )
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
    targets: NutritionTargets?
) {
    val semantic = LocalSemanticColors.current
    val loggedDays = snapshot.nutritionDays.values
        .filter { it.items.isNotEmpty() }
        .sortedBy { it.date }
        .takeLast(7)

    ExpandableSectionCard(
        title = "مراجعة أسبوعية",
        testTag = "weekly_review_card"
    ) {
        if (loggedDays.size < 3) {
            Text(
                text = "سجّل أكلك 3 أيام على الأقل عشان تظهر مراجعة.",
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
    onNeedSuggestions: (List<String>) -> Unit
) {
    val semantic = LocalSemanticColors.current
    val today = todayIso()
    val rda = if (snapshot.config.sex == "f") NutritionConstants.RDA_FEMALE else NutritionConstants.RDA_MALE
    val days = snapshot.nutritionDays.values
        .filter { it.date <= today && it.items.isNotEmpty() }
        .sortedBy { it.date }
        .takeLast(7)

    val rows = remember(days, rda) {
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
            val idx = NutritionConstants.NUTRIENT_INDEX[k] ?: return@mapNotNull null
            if (!has[idx]) null else k to ((sum[idx] / days.size) / target * 100.0)
        }
    }

    val lowThree = remember(rows) {
        rows.filter { (k, pct) -> k !in NutritionConstants.LIMIT_KEYS && pct < 70.0 }
            .sortedBy { it.second }
            .take(3)
    }

    LaunchedEffect(lowThree) {
        if (lowThree.isNotEmpty()) {
            onNeedSuggestions(lowThree.map { it.first })
        }
    }

    ExpandableSectionCard(
        title = "الميكروز: متوسط الأسبوع واقتراحات",
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
        } else {
            Text(
                text = "مفيش عنصر واضح تحت 70%.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
