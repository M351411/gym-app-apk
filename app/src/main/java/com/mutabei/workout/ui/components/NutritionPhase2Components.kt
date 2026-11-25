package com.mutabei.workout.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutabei.workout.data.model.AiMacroTetrisResult
import com.mutabei.workout.data.model.FoodItem
import com.mutabei.workout.data.model.LoggedFoodItem
import com.mutabei.workout.data.model.NutritionConstants
import com.mutabei.workout.ui.theme.LocalSemanticColors
import kotlin.math.roundToInt

/**
 * Gemini Macro Tetris Card: designs Egyptian meal options that exactly fill remaining daily macros.
 */
@Composable
fun MacroTetrisCard(
    remKcal: Int,
    remProteinG: Int,
    remCarbsG: Int,
    remFatG: Int,
    busyMessage: String?,
    errorMessage: String?,
    result: AiMacroTetrisResult?,
    onGenerate: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    ExpandableSectionCard(
        title = "مهندس تقفيل باقي اليوم (Macro Tetris)",
        testTag = "macro_tetris_card"
    ) {
        Text(
            text = "باقي لك النهارده: $remKcal سعر · $remProteinG جم بروتين · $remCarbsG جم كارب · $remFatG جم دهون",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onGenerate,
            enabled = busyMessage == null && (remKcal >= 80 || remProteinG >= 10),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("macro_tetris_generate_button")
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("اقترح وجبة تقفّل باقي ماكروزك (Gemini)")
        }

        if (remKcal < 80 && remProteinG < 10) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "أنت تقريبًا قفّلت سعراتك وبروتينك النهارده — عاش!",
                style = MaterialTheme.typography.bodySmall,
                color = semantic.ok
            )
        }

        if (busyMessage != null) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(busyMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        if (result != null) {
            Spacer(Modifier.height(10.dp))
            Text(result.summary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            result.options.forEach { opt ->
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
 * Science-based Supplement Timing & Nutrient Interaction Card for today's logged foods/supplements.
 */
@Composable
fun SupplementSynergyCard(
    dayItems: List<LoggedFoodItem>,
    totalOf: (String) -> Double
) {
    val semantic = LocalSemanticColors.current
    val alerts = remember(dayItems) {
        val out = mutableListOf<Triple<Boolean, String, String>>() // isWarning, title, body
        val suppItems = dayItems.filter { it.mealIndex == NutritionConstants.SUPPLEMENT_MEAL_INDEX }
        val ca = totalOf("ca")
        val fe = totalOf("fe")
        val zn = totalOf("zn")
        val mg = totalOf("mg")
        val rawVd = dayItems.sumOf { it.nutrientAmount("vD") }
        val vC = totalOf("vC")
        val o3 = totalOf("o3") + totalOf("epa")
        val caf = totalOf("caf")

        val hasSuppFeOrZn = suppItems.any { it.nutrientAmount("fe") >= 5.0 || it.nutrientAmount("zn") >= 5.0 }
        val hasHighCa = ca >= 500.0
        if (hasSuppFeOrZn && hasHighCa) {
            out.add(
                Triple(
                    true,
                    "تعارض امتصاص: كالسيوم مع حديد/زنك",
                    "الكالسيوم العالي (${ca.roundToInt()} مجم) بيقلل امتصاص الحديد والزنك — افصل مكمل الزنك أو الحديد بساعتين عن منتجات الألبان."
                )
            )
        }
        if (suppItems.any { it.nutrientAmount("vD") >= 5.0 || it.nutrientAmount("o3") >= 0.3 || it.nutrientAmount("epa") >= 0.2 } || rawVd >= 15.0 || o3 >= 1.0) {
            out.add(
                Triple(
                    false,
                    "أقصى امتصاص لفيتامين D وأوميجا-3",
                    "خد فيتامين D والأوميجا-3 وسط أكبر وجبة فيها دهون صحية (10–15 جم دهون على الأقل) لرفع الامتصاص لأكتر من 50%."
                )
            )
        }
        if (caf >= 300.0) {
            out.add(
                Triple(
                    true,
                    "كافيين مرتفع (${caf.roundToInt()} مجم)",
                    "تجنّب أي كافيين جديد قبل النوم بـ 8–10 ساعات علشان متضربش جودة النوم العميق والاستشفاء العضلي."
                )
            )
        } else if (caf > 30.0) {
            out.add(
                Triple(
                    false,
                    "توقيت الكافيين (${caf.roundToInt()} مجم)",
                    "يفضل آخر كوب قهوة أو شاي يكون قبل ميعاد نومك بـ 8 ساعات على الأقل."
                )
            )
        }
        if (suppItems.any { it.nutrientAmount("mg") >= 50.0 } || mg >= 350.0) {
            out.add(
                Triple(
                    false,
                    "توقيت الماغنسيوم",
                    "الماغنسيوم (خاصة جلايسينات) أفضل وقت ليه قبل النوم بـ 30–60 دقيقة لدعم الاسترخاء العصبي وجودة النوم."
                )
            )
        }
        if (fe >= 8.0 && vC >= 50.0) {
            out.add(
                Triple(
                    false,
                    "تآزر ممتاز: فيتامين C + حديد",
                    "فيتامين C الموجود في يومك (${vC.roundToInt()} مجم) بيزوّد امتصاص الحديد النباتي بشكل ملحوظ."
                )
            )
        }
        out
    }

    if (alerts.isEmpty()) return

    ExpandableSectionCard(
        title = "توقيت المكملات وتداخلات العناصر (${alerts.size})",
        testTag = "supplement_synergy_card"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            alerts.forEach { (isWarn, title, body) ->
                val accent = if (isWarn) semantic.warn else semantic.ok
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = accent.copy(alpha = 0.11f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (isWarn) Icons.Rounded.Warning else Icons.Rounded.Medication,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accent)
                            Text(body, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Head-to-Head Food Battle Dialog across the 12,903-item offline food database.
 */
@Composable
fun FoodBattleDialog(
    searchResultsA: List<FoodItem>,
    searchResultsB: List<FoodItem>,
    onSearchA: (String) -> Unit,
    onSearchB: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    var queryA by remember { mutableStateOf("") }
    var queryB by remember { mutableStateOf("") }
    var foodA by remember { mutableStateOf<FoodItem?>(null) }
    var foodB by remember { mutableStateOf<FoodItem?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("مقارنة أكلتين (Food Battle)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FoodBattlePicker(
                    slotLabel = "الأكلة الأولى (A)",
                    query = queryA,
                    selected = foodA,
                    results = searchResultsA,
                    onQueryChange = {
                        queryA = it
                        foodA = null
                        onSearchA(it)
                    },
                    onPick = {
                        foodA = it
                        queryA = it.name
                    },
                    tagPrefix = "battle_a"
                )

                FoodBattlePicker(
                    slotLabel = "الأكلة التانية (B)",
                    query = queryB,
                    selected = foodB,
                    results = searchResultsB,
                    onQueryChange = {
                        queryB = it
                        foodB = null
                        onSearchB(it)
                    },
                    onPick = {
                        foodB = it
                        queryB = it.name
                    },
                    tagPrefix = "battle_b"
                )

                if (foodA != null && foodB != null) {
                    val a = foodA!!
                    val b = foodB!!
                    fun v(f: FoodItem, key: String): Double {
                        val idx = NutritionConstants.NUTRIENT_INDEX[key] ?: return -1.0
                        return f.nutrients.getOrNull(idx) ?: -1.0
                    }
                    val pDensA = if (v(a, "kcal") > 0) (v(a, "p").coerceAtLeast(0.0) / v(a, "kcal")) * 100.0 else 0.0
                    val pDensB = if (v(b, "kcal") > 0) (v(b, "p").coerceAtLeast(0.0) / v(b, "kcal")) * 100.0 else 0.0

                    Card(
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "المقارنة لكل 100 جم:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            BattleRow("كثافة البروتين / 100 سعر", pDensA, pDensB, "جم", higherIsBetter = true)
                            BattleRow("السعرات", v(a, "kcal"), v(b, "kcal"), "سعر", higherIsBetter = false)
                            BattleRow("البروتين", v(a, "p"), v(b, "p"), "جم", higherIsBetter = true)
                            BattleRow("الليوسين", v(a, "leu"), v(b, "leu"), "جم", higherIsBetter = true)
                            BattleRow("الكارب", v(a, "c"), v(b, "c"), "جم", higherIsBetter = null)
                            BattleRow("الألياف", v(a, "fib"), v(b, "fib"), "جم", higherIsBetter = true)
                            BattleRow("الدهون", v(a, "f"), v(b, "f"), "جم", higherIsBetter = false)
                            BattleRow("أوميجا-3", v(a, "o3"), v(b, "o3"), "جم", higherIsBetter = true)
                            BattleRow("البوتاسيوم", v(a, "k"), v(b, "k"), "مجم", higherIsBetter = true)
                            BattleRow("الحديد", v(a, "fe"), v(b, "fe"), "مجم", higherIsBetter = true)
                            BattleRow("الماغنسيوم", v(a, "mg"), v(b, "mg"), "مجم", higherIsBetter = true)

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            val winnerName = if (pDensA >= pDensB) a.name else b.name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Verified, contentDescription = null, tint = semantic.ok, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "الأعلى كثافة بروتين: $winnerName (${String.format("%.1f", maxOf(pDensA, pDensB))} جم/100 سعر)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = semantic.ok
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
private fun FoodBattlePicker(
    slotLabel: String,
    query: String,
    selected: FoodItem?,
    results: List<FoodItem>,
    onQueryChange: (String) -> Unit,
    onPick: (FoodItem) -> Unit,
    tagPrefix: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(slotLabel) },
            placeholder = { Text("ابحث: سلمون، تونة، فراخ، شوفان...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${tagPrefix}_input")
        )
        if (selected == null && query.trim().length >= 2) {
            results.take(4).forEach { item ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(item) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${item.kcalPer100()} سعر", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleRow(
    label: String,
    valA: Double,
    valB: Double,
    unit: String,
    higherIsBetter: Boolean?
) {
    if (valA < 0 && valB < 0) return
    val semantic = LocalSemanticColors.current
    fun fmt(v: Double): String = if (v < 0) "—" else if (v >= 10) "${v.roundToInt()}" else String.format("%.1f", v)

    val aWins = higherIsBetter != null && valA >= 0 && valB >= 0 && valA != valB &&
        if (higherIsBetter) valA > valB else valA < valB
    val bWins = higherIsBetter != null && valA >= 0 && valB >= 0 && valA != valB &&
        if (higherIsBetter) valB > valA else valB < valA

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${fmt(valA)} $unit",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (aWins) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (aWins) semantic.ok else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Text(
            text = "${fmt(valB)} $unit",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bWins) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (bWins) semantic.ok else MaterialTheme.colorScheme.onSurface
        )
    }
}
