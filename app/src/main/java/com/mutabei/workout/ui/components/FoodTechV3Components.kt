package com.mutabei.workout.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mutabei.workout.data.model.AiMacroTetrisResult
import com.mutabei.workout.data.model.BuiltReverseMealItem
import com.mutabei.workout.data.model.FoodIngredientAuditResult
import com.mutabei.workout.data.model.FridgeIngredient
import com.mutabei.workout.data.model.GroceryCategory
import com.mutabei.workout.data.model.GroceryListItem
import com.mutabei.workout.data.model.IngredientFlagSeverity
import com.mutabei.workout.data.model.ReverseMealBuilderEngine
import com.mutabei.workout.data.model.ReverseMealPlan
import com.mutabei.workout.data.model.WeeklyGroceryListEngine
import com.mutabei.workout.ui.theme.LocalSemanticColors
import kotlin.math.roundToInt

/**
 * V3 Feature 1 UI: Food Additive & Label Scanner Result Card
 * Displays Clean Score (/10), NOVA processing level, 4-point Food-Tech checklist
 * (hydrogenated fats, hidden sugars, high sodium, preservatives), itemized flags, and cleaner alternative.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IngredientAuditResultCard(
    result: FoodIngredientAuditResult,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val scoreColor = when {
        result.cleanScore >= 8 -> semantic.ok
        result.cleanScore >= 5 -> semantic.warn
        else -> semantic.low
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.5.dp, scoreColor.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ingredient_audit_result_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Product Name + Clean Score / 10 + NOVA Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Science,
                            contentDescription = null,
                            tint = scoreColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = result.productNameAr.ifBlank { "تحليل جودة المنتج" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "${result.cleanGradeAr} · تصنيف التصنيع NOVA ${result.novaGroup}/4",
                        style = MaterialTheme.typography.labelSmall,
                        color = scoreColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Clean Score Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scoreColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.5.dp, scoreColor),
                    modifier = Modifier.testTag("clean_score_badge")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${result.cleanScore}/10",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = scoreColor
                        )
                        Text(
                            text = "Clean Score",
                            style = MaterialTheme.typography.labelSmall,
                            color = scoreColor
                        )
                    }
                }
            }

            if (result.summaryAr.isNotBlank()) {
                Text(
                    text = result.summaryAr,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 4-Point Food Processing Engineering Checklist
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AuditCheckPill("دهون مهدرجة", detected = result.hasHydrogenatedFats)
                AuditCheckPill("سكر خفي", detected = result.hasHiddenSugars)
                AuditCheckPill("صوديوم عالي", detected = result.hasHighSodium)
                AuditCheckPill("مواد حافظة وإضافات", detected = result.hasPreservativesOrAdditives)
            }

            // Detailed ingredient flags
            if (result.flags.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                Text(
                    text = "تقرير مهندس جودة الأغذية:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                result.flags.forEach { flag ->
                    val accent = when (flag.severity) {
                        IngredientFlagSeverity.HIGH_RISK -> semantic.low
                        IngredientFlagSeverity.MODERATE -> semantic.warn
                        IngredientFlagSeverity.CLEAN -> semantic.ok
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = accent.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (flag.severity == IngredientFlagSeverity.CLEAN) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${flag.categoryAr}: ${flag.ingredientName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                                Text(
                                    text = flag.explanationAr,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (result.detectedIngredientsAr.isNotEmpty()) {
                Text(
                    text = "المكونات المقروءة: ${result.detectedIngredientsAr.joinToString("، ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (result.cleanerAlternativeAr.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = semantic.ok.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, semantic.ok.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = semantic.ok,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "بديل أنضف من السوبر ماركت:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = semantic.ok
                            )
                            Text(
                                text = result.cleanerAlternativeAr,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditCheckPill(
    label: String,
    detected: Boolean
) {
    val semantic = LocalSemanticColors.current
    val accent = if (detected) semantic.low else semantic.ok
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (detected) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (detected) "$label: موجود" else "$label: خالي",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

/**
 * V3 Feature 2 UI: Smart Reverse Meal Builder ("فاضلي X سعر و Y بروتين — آكل إيه من اللي عندي؟")
 * Combines 30-day user meal habits + selectable fridge ingredients + instant offline 100% ring-closer
 * + 1-tap meal logging + Gemini AI custom recipe generation from the selected fridge items.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReverseMealBuilderCard(
    remKcal: Int,
    remProteinG: Int,
    remCarbsG: Int,
    remFatG: Int,
    fridgePool: List<FridgeIngredient>,
    groceryItems: List<GroceryListItem> = emptyList(),
    busyMessage: String?,
    errorMessage: String?,
    aiResult: AiMacroTetrisResult?,
    onGenerateAi: (List<String>) -> Unit,
    onLogInstantMeal: (List<BuiltReverseMealItem>) -> Unit
) {
    val semantic = LocalSemanticColors.current
    val selectedIds = remember(fridgePool) {
        mutableStateListOf<String>().apply {
            val defaults = fridgePool.filter { it.isUserHabit }.ifEmpty { fridgePool.take(7) }
            addAll(defaults.map { it.id })
            fridgePool.filterNot { it.id in this }.take(4).forEach { add(it.id) }
        }
    }
    var extraFridgeText by remember { mutableStateOf("") }
    var instantPlans by remember(remKcal, remProteinG, remCarbsG, remFatG) {
        mutableStateOf<List<ReverseMealPlan>>(emptyList())
    }
    var showGroceryDialog by remember { mutableStateOf(false) }

    val selectedPool = fridgePool.filter { it.id in selectedIds }

    if (showGroceryDialog) {
        WeeklyGroceryListDialog(
            items = groceryItems,
            onDismiss = { showGroceryDialog = false }
        )
    }

    ExpandableSectionCard(
        title = "حاسبة باقي اليوم الذكية",
        subtitleEn = "Reverse Meal Builder",
        statusBadgeText = if (remKcal >= 60 || remProteinG >= 8) "باقي $remKcal سعر" else "مكتمل ✓",
        statusBadgeColor = if (remKcal >= 60 || remProteinG >= 8) MaterialTheme.colorScheme.primary else semantic.ok,
        leadingIcon = Icons.Rounded.Kitchen,
        testTag = "macro_tetris_card"
    ) {
        Text(
            text = "فاضلك النهارده: $remKcal سعر · $remProteinG جم بروتين · $remCarbsG جم كارب · $remFatG جم دهون",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        // Selectable habits + fridge staples
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Kitchen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "أكلك المعتاد ومكونات تلاجتك (اختار المتوفر عندك):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            fridgePool.take(14).forEach { ing ->
                val isSelected = ing.id in selectedIds
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) selectedIds.remove(ing.id) else selectedIds.add(ing.id)
                    },
                    label = {
                        Text(
                            text = if (ing.isUserHabit) "★ ${ing.name}" else ing.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.testTag("fridge_chip_${ing.id}")
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = extraFridgeText,
            onValueChange = { extraFridgeText = it },
            placeholder = { Text("مكونات إضافية في تلاجتك؟ (مثلاً: تونة، خيار، جبنة رومي...)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reverse_builder_fridge_input")
        )
        Spacer(Modifier.height(8.dp))

        // Dynamic Hero CTA quoting exact remaining macros (Instant Offline Solver)
        Button(
            onClick = {
                instantPlans = ReverseMealBuilderEngine.buildInstantPlans(
                    remKcal = remKcal,
                    remProteinG = remProteinG,
                    remCarbsG = remCarbsG,
                    remFatG = remFatG,
                    selectedPool = selectedPool.ifEmpty { fridgePool }
                )
            },
            enabled = remKcal >= 60 || remProteinG >= 8,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reverse_meal_instant_button")
        ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("فاضلي $remKcal سعر و$remProteinG جم بروتين — آكل إيه من اللي عندي؟")
        }

        Spacer(Modifier.height(6.dp))

        // Gemini AI Reverse Meal Builder using exact fridge + habit items
        OutlinedButton(
            onClick = {
                val chosenNames = buildList {
                    selectedPool.forEach { add(it.name) }
                    extraFridgeText
                        .split("،", ",", "+", "و")
                        .map { it.trim() }
                        .filter { it.length >= 2 }
                        .forEach { add(it) }
                }.distinct()
                onGenerateAi(chosenNames)
            },
            enabled = busyMessage == null && (remKcal >= 60 || remProteinG >= 8),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("macro_tetris_generate_button")
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("صمّم وصفات مبتكرة من تلاجتي بـ Gemini")
        }

        if (groceryItems.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { showGroceryDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_grocery_list_button")
            ) {
                Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("قائمة المشتريات الأسبوعية (${groceryItems.size} أصناف)")
            }
        }

        if (remKcal < 60 && remProteinG < 8) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "أنت تقريبًا قفّلت دائرة السعرات والبروتين 100% النهارده — عاش!",
                style = MaterialTheme.typography.bodySmall,
                color = semantic.ok,
                fontWeight = FontWeight.Bold
            )
        }

        // Instant Offline Circle-Closing Meal Plans
        if (instantPlans.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "وجبات جاهزة بالجرامات تقفّل دائرتك 100% فورًا:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = semantic.ok
            )
            instantPlans.forEach { plan ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, semantic.ok.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = plan.titleAr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${plan.totalKcal} سعر (${plan.kcalMatchPct}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = semantic.ok,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "بروتين ${plan.totalProteinG} جم (${plan.proteinMatchPct}%) · كارب ${plan.totalCarbsG} جم · دهون ${plan.totalFatG} جم · ليوسين ${plan.totalLeucineG} جم",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        plan.items.forEach { item ->
                            Text(
                                text = "• ${item.food.name}: ${item.grams.roundToInt()} جم (${item.kcal} سعر · ب ${item.proteinG.roundToInt()} جم)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = plan.tipAr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { onLogInstantMeal(plan.items) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("log_reverse_meal_button")
                        ) {
                            Icon(Icons.Rounded.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("سجّل الوجبة دي لليوم بضغطة واحدة")
                        }
                    }
                }
            }
        }

        // Optimistic Shimmer Skeleton during Gemini Reverse Builder call
        if (busyMessage != null) {
            Spacer(Modifier.height(8.dp))
            AiMealShimmerSkeleton(statusMessage = busyMessage)
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        // Gemini AI Recipe Results
        if (aiResult != null) {
            Spacer(Modifier.height(10.dp))
            Text(aiResult.summary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            aiResult.options.forEach { opt ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opt.titleAr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${opt.kcal} سعر",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "بروتين ${opt.proteinG} جم · كارب ${opt.carbsG} جم · دهون ${opt.fatG} جم · ليوسين ${String.format("%.1f", opt.leucineG)} جم",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.ok,
                            fontWeight = FontWeight.SemiBold
                        )
                        opt.ingredientsAr.forEach { ing ->
                            Text("• $ing", style = MaterialTheme.typography.bodySmall)
                        }
                        if (opt.tip.isNotBlank()) {
                            Text(
                                text = opt.tip,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Categorized Weekly Grocery Checklist Dialog with one-tap WhatsApp / Share Intent.
 */
@Composable
fun WeeklyGroceryListDialog(
    items: List<GroceryListItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val semantic = LocalSemanticColors.current
    val checkedIds = remember(items) { mutableStateListOf<String>() }
    val grouped = remember(items) { items.groupBy { it.category } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "قائمة المشتريات الأسبوعية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "مبنية من وجباتك المتكررة في آخر أسبوعين",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GroceryCategory.entries.forEach { cat ->
                    val catItems = grouped[cat].orEmpty()
                    if (catItems.isNotEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, subtleCardBorderColor()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${cat.emoji} ${cat.labelAr}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                catItems.forEach { item ->
                                    val isChecked = item.id in checkedIds
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isChecked) checkedIds.remove(item.id) else checkedIds.add(item.id)
                                            }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = {
                                                    if (isChecked) checkedIds.remove(item.id) else checkedIds.add(item.id)
                                                }
                                            )
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = item.displayQtyAr,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else semantic.ok
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shareText = WeeklyGroceryListEngine.formatShareText(items, checkedIds.toSet())
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    try {
                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة قائمة المشتريات"))
                    } catch (_: Exception) {
                        // ignore if no share target
                    }
                },
                modifier = Modifier.testTag("share_grocery_list_button")
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("مشاركة القائمة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

