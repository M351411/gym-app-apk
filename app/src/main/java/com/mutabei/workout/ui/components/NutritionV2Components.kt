package com.mutabei.workout.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutabei.workout.data.model.CookedWeightMode
import com.mutabei.workout.data.model.CookingMethodPreset
import com.mutabei.workout.data.model.CookingYieldEngine
import com.mutabei.workout.data.model.CookingYieldProfile
import com.mutabei.workout.data.model.MacroSwapOption
import com.mutabei.workout.data.model.RollingCalorieBudget
import com.mutabei.workout.data.model.WorkoutSyncDayType
import com.mutabei.workout.data.model.WorkoutSyncedMacros
import com.mutabei.workout.ui.theme.LocalSemanticColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val SHIMMER_NUTRITION_TIPS = listOf(
    "نصيحة سريعة: 2.5–3 جم ليوسين في الوجبة (مثل 120 جم صدور فراخ) بتفعّل بناء العضلات MPS لأقصى درجة.",
    "نصيحة سريعة: تناول فيتامين D3 مع وجبة فيها 10–15 جم دهون صحية يرفع امتصاصه لأكثر من 50%.",
    "نصيحة سريعة: افصل الشاي والقهوة عن الحديد والزنك بـ 1–2 ساعة عشان الكافيين والتانينات ميعطلوش الامتصاص.",
    "نصيحة سريعة: 100 جم أرز أبيض نيء بتبقى حوالي 265 جم بعد السلق، و100 جم فراخ نيئة بتبقى ~74 جم بعد الشوي.",
    "نصيحة سريعة: يوم واحد زيادة في السعرات مبيبوظش الدايت — المهم متوسط السعرات الأسبوعي (7-Day Budget)."
)

/**
 * Optimistic Shimmer Skeleton screen with rotating nutrition science tips shown
 * during 3–6s Gemini Vision / Text analysis to eliminate loading friction.
 */
@Composable
fun AiMealShimmerSkeleton(
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    var tipIndex by remember { mutableIntStateOf((System.currentTimeMillis() / 1000L).toInt().mod(SHIMMER_NUTRITION_TIPS.size)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800L)
            tipIndex = (tipIndex + 1) % SHIMMER_NUTRITION_TIPS.size
        }
    }

    val transition = rememberInfiniteTransition(label = "ai_shimmer")
    val shimmerShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    val baseSurface = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    val highlightSurface = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(baseSurface, highlightSurface, baseSurface),
        start = Offset(shimmerShift - 300f, 0f),
        end = Offset(shimmerShift, 180f)
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_shimmer_skeleton")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
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
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 2 optimistic skeleton cards mimicking detected food rows
            repeat(2) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmerBrush)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .width(68.dp)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(shimmerBrush)
                            )
                        }
                    }
                }
            }

            // Rotating quick nutrition tip
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = SHIMMER_NUTRITION_TIPS[tipIndex],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * USDA ARS / EuroFIR Raw <-> Cooked Auto-Conversion Toggle.
 * Lets the user pick how they weighed the food (Raw vs Cooked) and automatically
 * converts to the food entry's intrinsic database basis.
 */
@Composable
fun RawCookedToggleBar(
    profile: CookingYieldProfile,
    itemIntrinsicMode: CookedWeightMode,
    selectedWeighedMode: CookedWeightMode,
    enteredGrams: Double,
    onModeChange: (CookedWeightMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val counterpart = CookingYieldEngine.counterpartWeight(enteredGrams, selectedWeighedMode, profile.yieldFactor)
    val dbGrams = CookingYieldEngine.convertToDbGrams(enteredGrams, selectedWeighedMode, itemIntrinsicMode, profile.yieldFactor)
    val isConverting = selectedWeighedMode != itemIntrinsicMode

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            1.dp,
            if (isConverting) semantic.ok.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("raw_cooked_toggle_bar")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Scale,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "وزنته إزاي؟ (نيء ↔ مطبوخ)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "×${profile.yieldFactor} · ${profile.sourceRef}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CookedWeightMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedWeighedMode == mode,
                        onClick = { onModeChange(mode) },
                        label = { Text(mode.labelAr) },
                        modifier = Modifier.testTag("weight_mode_${mode.name.lowercase()}")
                    )
                }
            }

            if (enteredGrams > 0.0) {
                val otherLabel = if (selectedWeighedMode == CookedWeightMode.RAW) "مطبوخ" else "نيء/جاف"
                Text(
                    text = buildString {
                        append("${enteredGrams.roundToInt()} جم ${selectedWeighedMode.labelAr} ≈ ${counterpart.roundToInt()} جم $otherLabel")
                        if (isConverting) {
                            append(" · بيُحسب تلقائيًا على ${dbGrams.roundToInt()} جم (${itemIntrinsicMode.labelAr})")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isConverting) FontWeight.Bold else FontWeight.Normal,
                    color = if (isConverting) semantic.ok else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Smart Cooking-Method Follow-Up Chips shown below Gemini-detected cooked foods
 * to instantly account for hidden oils / ghee / frying fats in Egyptian cooking.
 */
@Composable
fun CookingMethodChipsRow(
    selectedMethod: CookingMethodPreset,
    baselineMethod: CookingMethodPreset,
    onSelectMethod: (CookingMethodPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cooking_method_chips_row"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = semantic.warn,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "طريقة التسوية (الدهون المخفية):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selectedMethod != baselineMethod) {
                val deltaFat = selectedMethod.addedFatPer100g - baselineMethod.addedFatPer100g
                val sign = if (deltaFat >= 0) "+" else ""
                Text(
                    text = "${sign}${deltaFat.roundToInt()} جم دهون/100جم",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (deltaFat > 0) semantic.warn else semantic.ok
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CookingMethodPreset.entries.forEach { preset ->
                FilterChip(
                    selected = selectedMethod == preset,
                    onClick = { onSelectMethod(preset) },
                    label = { Text(preset.labelAr, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("cooking_chip_${preset.name.lowercase()}")
                )
            }
        }
    }
}

/**
 * Smart Macro-Swapper ("بدّل المكون") interactive list for swapping an ingredient
 * inside a meal while preserving target protein & calories automatically.
 */
@Composable
fun MacroSwapperPanel(
    swaps: List<MacroSwapOption>,
    onPickSwap: (MacroSwapOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    if (swaps.isEmpty()) return

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("macro_swapper_panel")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${swaps.first().category.labelAr} (بنفس السعرات والماكروز تقريبًا):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            swaps.forEachIndexed { idx, opt ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickSwap(opt) }
                        .padding(vertical = 6.dp)
                        .testTag("macro_swap_option_${opt.food.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = opt.food.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${opt.householdHint} · ${opt.swapKcal} سعر · ب ${opt.swapProteinG.roundToInt()} · ك ${opt.swapCarbsG.roundToInt()} · د ${opt.swapFatG.roundToInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = semantic.ok.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, semantic.ok.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "تبديل",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = semantic.ok,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact status banners rendered inside the Nutrition Summary Card:
 * 1) Workout-Synced Nutrition (Carb/Fat cycling based on today's Gym workout or Rest Day)
 * 2) 7-Day Rolling Calorie Budget (Weekly Flexibility eliminating "Red Day" stress)
 */
@Composable
fun SmartNutritionStatusStrip(
    workoutSync: WorkoutSyncedMacros?,
    rollingBudget: RollingCalorieBudget?,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    if (workoutSync == null && rollingBudget == null) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (workoutSync != null) {
            val accent = when (workoutSync.dayType) {
                WorkoutSyncDayType.HIGH_DEMAND -> MaterialTheme.colorScheme.primary
                WorkoutSyncDayType.REST_DAY -> semantic.ok
                WorkoutSyncDayType.MODERATE_TRAINING -> MaterialTheme.colorScheme.secondary
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = accent.copy(alpha = 0.11f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.38f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("workout_synced_nutrition_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.16f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${workoutSync.dayType.badgeAr} · ${workoutSync.workoutLabelAr}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Text(
                            text = workoutSync.tipAr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (rollingBudget != null) {
            val accent = when {
                rollingBudget.todaySurplusKcal >= 50 -> semantic.warn
                rollingBudget.deltaTodayKcal <= -30 -> semantic.warn
                else -> semantic.ok
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = accent.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.38f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rolling_calorie_budget_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.16f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Balance,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Row 1: Distinct card header (never collides with target numbers)
                        Text(
                            text = rollingBudget.badgeAr,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        // Row 2: Distinct daily target pill row (no awkward parenthesis wrapping)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "هدف اليوم: ${isolateLtr("${rollingBudget.adjustedTodayTarget}")} سعر",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "الأساسي ${isolateLtr("${rollingBudget.baseDailyTarget}")}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Row 3: Concise explanation
                        Text(
                            text = rollingBudget.explanationAr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
