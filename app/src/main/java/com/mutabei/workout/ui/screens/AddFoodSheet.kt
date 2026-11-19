package com.mutabei.workout.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mutabei.workout.R
import com.mutabei.workout.data.model.AiFoodReviewItem
import com.mutabei.workout.data.model.AiLabelResult
import com.mutabei.workout.data.model.CustomFood
import com.mutabei.workout.data.model.FoodItem
import com.mutabei.workout.data.model.NutritionConstants
import com.mutabei.workout.data.model.PortionOption
import com.mutabei.workout.data.model.RecipeIngredient
import com.mutabei.workout.data.model.SupplementCatalog
import com.mutabei.workout.ui.components.CameraOrGalleryButtons
import com.mutabei.workout.ui.components.ExpandableSectionCard
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.util.rememberImageCaptureController
import com.mutabei.workout.ui.viewmodel.AppDataSnapshot
import com.mutabei.workout.ui.viewmodel.FoodSheetAiState
import com.mutabei.workout.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

private enum class FoodSheetMode(val label: String) {
    SEARCH("بحث"),
    SUPPLEMENTS("مكملات وفيتامينات"),
    FAVORITES("مفضلة"),
    LABEL("ملصق / علبة"),
    MEAL_PHOTO("صورة أكل"),
    TEXT_AI("وصف"),
    BARCODE("باركود"),
    RECIPE("وصفة"),
    MY_FOODS("أكلاتي"),
    CUSTOM("إضافة خاصة")
}

private val SUPPLEMENT_MICRO_KEYS = listOf(
    "o3", "vC", "vD", "vE", "vA", "vK",
    "b1", "b2", "b3", "b5", "b6", "fol", "b12",
    "zn", "mg", "fe", "ca", "cu", "se", "mn", "k", "ph"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodSheet(
    initialMealIndex: Int,
    snapshot: AppDataSnapshot,
    searchResults: List<FoodItem>,
    aiState: FoodSheetAiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember {
        mutableStateOf(
            if (initialMealIndex == NutritionConstants.SUPPLEMENT_MEAL_INDEX) FoodSheetMode.SUPPLEMENTS
            else FoodSheetMode.SEARCH
        )
    }
    var mealIndex by remember { mutableIntStateOf(initialMealIndex) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var supplementAiQuery by remember { mutableStateOf("") }
    var mealDescription by remember { mutableStateOf("") }

    // Unified camera + gallery capture routed cleanly to Gemini AI according to active mode
    val imageCapture = rememberImageCaptureController(
        onBitmapCaptured = { bitmap ->
            when (mode) {
                FoodSheetMode.LABEL -> viewModel.analyzeNutritionLabel(bitmap)
                FoodSheetMode.SUPPLEMENTS -> {
                    mode = FoodSheetMode.LABEL
                    viewModel.analyzeNutritionLabel(bitmap)
                }
                FoodSheetMode.MEAL_PHOTO -> viewModel.analyzeMealPhoto(bitmap)
                FoodSheetMode.BARCODE -> viewModel.scanBarcodeWithGemini(
                    bitmap = bitmap,
                    onFoundCustomFood = { selectedFood = it },
                    onSwitchToLabelMode = { mode = FoodSheetMode.LABEL }
                )
                else -> {}
            }
        },
        onError = { msg -> viewModel.showToast(msg) }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أضف لـ ${NutritionConstants.MEAL_NAMES[mealIndex]}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_food_sheet")) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close))
                }
            }

            if (selectedFood != null) {
                FoodSelectionDetailView(
                    food = selectedFood!!,
                    mealIndex = mealIndex,
                    snapshot = snapshot,
                    onChangeMeal = { mealIndex = it },
                    onAdd = { grams ->
                        viewModel.addLoggedFood(selectedFood!!, grams, mealIndex)
                        onDismiss()
                    },
                    onToggleFav = { viewModel.toggleFavorite(selectedFood!!) },
                    onSavePortion = { name, grams -> viewModel.saveCustomPortion(selectedFood!!, name, grams) },
                    onBack = { selectedFood = null }
                )
            } else {
                // Mode tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FoodSheetMode.entries.forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = {
                                mode = m
                                if (m != FoodSheetMode.LABEL && m != FoodSheetMode.BARCODE) {
                                    viewModel.clearFoodSheetAi()
                                }
                            },
                            label = { Text(m.label) },
                            modifier = Modifier.testTag("food_mode_${m.name.lowercase()}")
                        )
                    }
                }

                // AI Loading indicator
                if (!aiState.busyMessage.isNullOrBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(
                                text = aiState.busyMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // AI Status / Error message
                if (!aiState.errorOrInfo.isNullOrBlank()) {
                    val clipboard = LocalClipboardManager.current
                    val isSuccess = aiState.errorOrInfo.contains("✅") || aiState.errorOrInfo.contains("Open Food Facts")
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = aiState.errorOrInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!isSuccess) {
                                TextButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(aiState.errorOrInfo))
                                        viewModel.showToast("تم نسخ تفاصيل الخطأ")
                                    },
                                    modifier = Modifier.testTag("food_ai_copy_error_button")
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

                when (mode) {
                    FoodSheetMode.SEARCH -> {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.searchFoods(it)
                            },
                            placeholder = { Text("ابحث: فول، رز، فيتا ستريس، أوميجا 3، باور ماكس...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("food_search_input")
                        )
                        if (searchResults.isEmpty() && searchQuery.trim().length >= 2) {
                            Text(
                                text = "مفيش نتيجة. جرّب تاب \"مكملات وفيتامينات\" أو صوّر العبوة بالـ Gemini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            searchResults.forEach { item ->
                                FoodListRow(item = item, onClick = { selectedFood = item })
                            }
                        }
                    }

                    FoodSheetMode.SUPPLEMENTS -> {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "إضافة أي مكمل أو مالتي فيتامين بـ Gemini",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "صوّر جدول المكونات على العلبة أو اكتب اسمه وهيملى Gemini كل الفيتامينات والمعادن والأوميجا 3 تلقائيًا.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                CameraOrGalleryButtons(
                                    cameraLabel = "صوّر علبة المكمل",
                                    galleryLabel = "صورة العلبة",
                                    enabled = aiState.busyMessage == null,
                                    cameraTestTag = "supplement_camera_button",
                                    galleryTestTag = "supplement_gallery_button",
                                    onCameraClick = imageCapture.launchCamera,
                                    onGalleryClick = imageCapture.launchGallery
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = supplementAiQuery,
                                        onValueChange = { supplementAiQuery = it },
                                        placeholder = { Text("مثال: سنتروم، كيروفيت، أوكتاترون...") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("supplement_ai_name_input")
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.lookupSupplementByNameWithGemini(supplementAiQuery) {
                                                mode = FoodSheetMode.LABEL
                                            }
                                        },
                                        enabled = aiState.busyMessage == null && supplementAiQuery.isNotBlank(),
                                        modifier = Modifier.testTag("supplement_ai_lookup_button")
                                    ) {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("ملء تلقائي")
                                    }
                                }
                            }
                        }

                        Text(
                            text = "مكملات وفيتامينات جاهزة (اضغط للإضافة السريعة):",
                            style = MaterialTheme.typography.titleSmall
                        )

                        SupplementCatalog.ALL.forEach { supp ->
                            SupplementCatalogRow(
                                item = supp,
                                onClick = {
                                    mealIndex = NutritionConstants.SUPPLEMENT_MEAL_INDEX
                                    selectedFood = supp
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = { mode = FoodSheetMode.CUSTOM },
                            modifier = Modifier.fillMaxWidth().testTag("add_custom_supplement_manual_button")
                        ) {
                            Text("أضف مكمل أو فيتامين جديد يدويًا")
                        }
                    }

                    FoodSheetMode.FAVORITES -> {
                        val favs = snapshot.customFoods.filter { it.favorite }
                        if (favs.isEmpty()) {
                            Text(
                                text = "لسه مفيش مفضلة. اضغط ★ على أي أكلة بعد ما تختارها.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            favs.forEach { cf ->
                                val item = cf.toFoodItem()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedFood = item }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${item.kcalPer100()} سعر/100جم", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { viewModel.toggleFavorite(item) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.cd_delete))
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            }
                        }
                    }

                    FoodSheetMode.LABEL -> {
                        Text(
                            text = "صوّر الجدول الغذائي على العبوة (الخلفية). Gemini بيقراه وبيملى كل القيم تلقائيًا وانت بتراجع قبل الحفظ.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CameraOrGalleryButtons(
                            cameraLabel = "صوّر الملصق بـ Gemini",
                            enabled = aiState.busyMessage == null,
                            cameraTestTag = "label_camera_button",
                            galleryTestTag = "label_gallery_button",
                            onCameraClick = imageCapture.launchCamera,
                            onGalleryClick = imageCapture.launchGallery
                        )
                        LabelAutoFillForm(
                            initial = aiState.labelDraft,
                            defaultBarcode = aiState.barcodeInput,
                            onSave = { customFood ->
                                viewModel.saveCustomFood(customFood) { savedItem ->
                                    viewModel.clearFoodSheetAi()
                                    selectedFood = savedItem
                                }
                            }
                        )
                    }

                    FoodSheetMode.MEAL_PHOTO -> {
                        Text(
                            text = "صوّر الوجبة بالكاميرا أو اختار صورة. Gemini بيتعرّف على الأصناف والجرامات ويطابقها مع قاعدة الأغذية تلقائيًا.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CameraOrGalleryButtons(
                            cameraLabel = "صوّر الوجبة بـ Gemini",
                            enabled = aiState.busyMessage == null,
                            cameraTestTag = "meal_camera_button",
                            galleryTestTag = "meal_gallery_button",
                            onCameraClick = imageCapture.launchCamera,
                            onGalleryClick = imageCapture.launchGallery
                        )
                        if (aiState.detectedItems.isNotEmpty()) {
                            AiDetectedItemsReview(
                                items = aiState.detectedItems,
                                onUpdate = { id, sel, g, candIdx ->
                                    viewModel.updateAiDetectedItem(id, sel, g, candIdx)
                                },
                                onConfirmAdd = {
                                    viewModel.addMultipleAiFoods(aiState.detectedItems, mealIndex)
                                    onDismiss()
                                }
                            )
                        }
                    }

                    FoodSheetMode.TEXT_AI -> {
                        OutlinedTextField(
                            value = mealDescription,
                            onValueChange = { mealDescription = it },
                            placeholder = { Text("مثال: طبقين كشري كبير مع دقة وشطة، أو سندوتش شاورما فراخ") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("meal_text_ai_input")
                        )
                        Button(
                            onClick = { viewModel.analyzeMealText(mealDescription) },
                            enabled = aiState.busyMessage == null,
                            modifier = Modifier.fillMaxWidth().testTag("meal_text_ai_submit")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("قدّر الأصناف بالـ Gemini")
                        }
                        if (aiState.detectedItems.isNotEmpty()) {
                            AiDetectedItemsReview(
                                items = aiState.detectedItems,
                                onUpdate = { id, sel, g, candIdx ->
                                    viewModel.updateAiDetectedItem(id, sel, g, candIdx)
                                },
                                onConfirmAdd = {
                                    viewModel.addMultipleAiFoods(aiState.detectedItems, mealIndex)
                                    onDismiss()
                                }
                            )
                        }
                    }

                    FoodSheetMode.BARCODE -> {
                        Text(
                            text = "صوّر الباركود بالكاميرا (Gemini بيقرا الرقم ويملى بيانات المنتج من Open Food Facts تلقائيًا) أو اكتبه يدويًا.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CameraOrGalleryButtons(
                            cameraLabel = "صوّر الباركود بـ Gemini",
                            enabled = aiState.busyMessage == null,
                            cameraTestTag = "barcode_camera_button",
                            galleryTestTag = "barcode_gallery_button",
                            onCameraClick = imageCapture.launchCamera,
                            onGalleryClick = imageCapture.launchGallery
                        )
                        OutlinedTextField(
                            value = aiState.barcodeInput,
                            onValueChange = { viewModel.updateBarcodeInput(it) },
                            label = { Text("رقم الباركود") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("barcode_manual_input")
                        )
                        Button(
                            onClick = {
                                viewModel.lookupBarcode(
                                    rawBarcode = aiState.barcodeInput,
                                    onFoundCustomFood = { selectedFood = it },
                                    onSwitchToLabelMode = { mode = FoodSheetMode.LABEL }
                                )
                            },
                            enabled = aiState.busyMessage == null,
                            modifier = Modifier.fillMaxWidth().testTag("barcode_lookup_button")
                        ) {
                            Text("دوّر على الباركود واملأ تلقائيًا")
                        }
                    }

                    FoodSheetMode.RECIPE -> {
                        RecipeBuilderSection(
                            searchResults = searchResults,
                            onSearch = { viewModel.searchFoods(it) },
                            onSaveRecipe = { recipeFood ->
                                viewModel.saveCustomFood(recipeFood) { savedItem ->
                                    selectedFood = savedItem
                                }
                            }
                        )
                    }

                    FoodSheetMode.MY_FOODS -> {
                        MyFoodsSection(
                            customFoods = snapshot.customFoods.filter { it.custom },
                            onSelectToLog = { selectedFood = it.toFoodItem() },
                            onUpdateFood = { viewModel.saveCustomFood(it) },
                            onDeleteFood = { viewModel.deleteCustomFood(it) }
                        )
                    }

                    FoodSheetMode.CUSTOM -> {
                        CustomFoodForm(
                            onSave = { cf ->
                                viewModel.saveCustomFood(cf) { savedItem ->
                                    selectedFood = savedItem
                                }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SupplementCatalogRow(item: FoodItem, onClick: () -> Unit) {
    val isUnit = SupplementCatalog.isUnitBasedFood(item)
    val defaultGrams = item.portions.firstOrNull()?.grams ?: 100.0
    val microSummary = SupplementCatalog.activeNutrientsSummary(item.nutrients, defaultGrams, maxItems = 4)
    val defaultPortionLabel = item.portions.firstOrNull()?.name ?: "100 جم"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (microSummary.isNotBlank()) microSummary else "${item.kcalPer100()} سعر/100جم",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "الحصة الافتراضية: $defaultPortionLabel" + if (!isUnit) " · ${item.kcalPer100()} سعر/100جم" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
}

@Composable
private fun FoodListRow(item: FoodItem, onClick: () -> Unit) {
    val isUnit = SupplementCatalog.isUnitBasedFood(item)
    val microSummary = if (isUnit) {
        SupplementCatalog.activeNutrientsSummary(item.nutrients, item.portions.firstOrNull()?.grams ?: 100.0, maxItems = 3)
    } else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            val sub = buildString {
                if (microSummary.isNotBlank()) {
                    append(microSummary)
                } else {
                    if (item.englishName.isNotBlank()) append("${item.englishName} · ")
                    append(NutritionConstants.SOURCE_LABELS[item.source] ?: "")
                }
            }
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = if (isUnit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (isUnit) item.portions.firstOrNull()?.name ?: "1 حصة" else "${item.kcalPer100()} سعر/100جم",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodSelectionDetailView(
    food: FoodItem,
    mealIndex: Int,
    snapshot: AppDataSnapshot,
    onChangeMeal: (Int) -> Unit,
    onAdd: (Double) -> Unit,
    onToggleFav: () -> Unit,
    onSavePortion: (String, Double) -> Unit,
    onBack: () -> Unit
) {
    val isUnitSupp = remember(food) { SupplementCatalog.isUnitBasedFood(food) }
    val defaultG = food.portions.firstOrNull()?.grams ?: 100.0
    var inputText by remember(food) {
        val initialVal = if (isUnitSupp) defaultG / 100.0 else defaultG
        mutableStateOf(if (initialVal % 1.0 == 0.0) initialVal.toInt().toString() else initialVal.toString())
    }
    var deductRefuse by remember(food) { mutableStateOf(false) }
    var portionName by remember { mutableStateOf("") }

    val customKey = "${food.source}|${food.name}"
    val allPortions = remember(food, snapshot.config.customPortions, isUnitSupp) {
        if (isUnitSupp) {
            food.portions + snapshot.config.customPortions[customKey].orEmpty()
        } else {
            listOf(PortionOption("100 جم", 100.0)) + food.portions + snapshot.config.customPortions[customKey].orEmpty()
        }
    }
    val isFav = snapshot.customFoods.any { it.favorite && it.name == food.name && it.source == food.source }
    val parsedInput = inputText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val rawGrams = if (isUnitSupp) parsedInput * 100.0 else parsedInput
    val effectiveGrams = rawGrams * if (deductRefuse && food.refusePercent > 0.0) (1.0 - food.refusePercent / 100.0) else 1.0

    fun calc(key: String): String {
        val idx = NutritionConstants.NUTRIENT_INDEX[key] ?: return "–"
        val v = food.nutrients.getOrNull(idx) ?: -1.0
        return if (v >= 0.0) ((v * effectiveGrams / 10.0).roundToInt() / 10.0).toString() else "–"
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(food.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(
            text = if (isUnitSupp) "مكمل غذائي / فيتامين" else (NutritionConstants.SOURCE_LABELS[food.source] ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            allPortions.forEach { por ->
                FilterChip(
                    selected = kotlin.math.abs(rawGrams - por.grams) < 0.1,
                    onClick = {
                        val v = if (isUnitSupp) por.grams / 100.0 else por.grams
                        inputText = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
                    },
                    label = {
                        Text(if (isUnitSupp) por.name else "${por.name} (${por.grams}جم)")
                    }
                )
            }
        }
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text(if (isUnitSupp) "عدد الأقراص / الكبسولات" else "الوزن بالجرام") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().testTag("selected_food_grams_input")
        )
        if (food.refusePercent > 0.0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = deductRefuse, onCheckedChange = { deductRefuse = it })
                Text(
                    text = "الوزن بالقشر أو العظم (فاقد ${food.refusePercent}%، بيتخصم تلقائيًا)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (isUnitSupp) {
            val activeMicros = SupplementCatalog.activeNutrientsSummary(food.nutrients, effectiveGrams, maxItems = 12)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "الفيتامينات والمعادن اللي هتتضاف لليوم:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = activeMicros.ifBlank { "${calc("kcal")} سعر · بروتين ${calc("p")} · دهون ${calc("f")}" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Text(
                text = "${calc("kcal")} سعر · بروتين ${calc("p")} · كارب ${calc("c")} · دهون ${calc("f")}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (!isUnitSupp) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = portionName,
                    onValueChange = { portionName = it },
                    placeholder = { Text("اسم حصة (مثلاً: رغيفي)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        if (portionName.isNotBlank() && rawGrams > 0) {
                            onSavePortion(portionName, rawGrams)
                            portionName = ""
                        }
                    }
                ) { Text("احفظ حصة") }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            NutritionConstants.MEAL_NAMES.forEachIndexed { idx, mn ->
                FilterChip(selected = idx == mealIndex, onClick = { onChangeMeal(idx) }, label = { Text(mn) })
            }
        }
        Button(
            onClick = { if (effectiveGrams > 0.0) onAdd(effectiveGrams) },
            modifier = Modifier.fillMaxWidth().testTag("confirm_add_food_button")
        ) { Text(if (mealIndex == NutritionConstants.SUPPLEMENT_MEAL_INDEX) "أضف للمكملات اليومية" else "أضف للوجبة") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onToggleFav, modifier = Modifier.weight(1f)) {
                Icon(if (isFav) Icons.Rounded.Star else Icons.Rounded.StarBorder, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (isFav) "شيل من المفضلة" else "أضف للمفضلة")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("رجوع") }
        }
    }
}

@Composable
private fun AiDetectedItemsReview(
    items: List<AiFoodReviewItem>,
    onUpdate: (String, Boolean?, Double?, Int?) -> Unit,
    onConfirmAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Gemini اتعرّف على الأصناف دي. اختار أقرب صنف من الجدول لكل واحدة وعدّل الجرامات:",
            style = MaterialTheme.typography.bodySmall,
            color = LocalSemanticColors.current.warn,
            fontWeight = FontWeight.SemiBold
        )
        items.forEach { item ->
            var menuOpen by remember(item.id) { mutableStateOf(false) }
            var gText by remember(item.id, item.grams) { mutableStateOf(item.grams.roundToInt().toString()) }
            val activeVec = item.activeNutrients()
            val kcal = ((activeVec.getOrNull(0)?.takeIf { it >= 0 } ?: 0.0) * item.grams / 100.0).roundToInt()
            val pro = ((activeVec.getOrNull(1)?.takeIf { it >= 0 } ?: 0.0) * item.grams / 100.0).roundToInt()

            Surface(
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = item.selected,
                            onCheckedChange = { onUpdate(item.id, it, null, null) }
                        )
                        Text(item.detectedName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text("$kcal سعر · $pro جم بروتين", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            val chosenLabel = if (item.selectedCandidateIndex in item.candidates.indices) {
                                val c = item.candidates[item.selectedCandidateIndex]
                                "${c.name} (${NutritionConstants.SOURCE_LABELS[c.source] ?: ""})"
                            } else "تقدير Gemini AI"
                            Text(chosenLabel, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            item.candidates.forEachIndexed { idx, c ->
                                DropdownMenuItem(
                                    text = { Text("${c.name} (${NutritionConstants.SOURCE_LABELS[c.source] ?: ""})") },
                                    onClick = {
                                        onUpdate(item.id, null, null, idx)
                                        menuOpen = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("تقدير Gemini AI") },
                                onClick = {
                                    onUpdate(item.id, null, null, -1)
                                    menuOpen = false
                                }
                            )
                        }
                        OutlinedTextField(
                            value = gText,
                            onValueChange = {
                                gText = it
                                it.replace(',', '.').toDoubleOrNull()?.let { d -> onUpdate(item.id, null, d, null) }
                            },
                            label = { Text("جم") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(92.dp)
                        )
                    }
                }
            }
        }
        Button(
            onClick = onConfirmAdd,
            modifier = Modifier.fillMaxWidth().testTag("confirm_add_ai_foods_button")
        ) {
            Text("أضف الأصناف المحددة للوجبة")
        }
    }
}

@Composable
private fun LabelAutoFillForm(
    initial: AiLabelResult?,
    defaultBarcode: String,
    onSave: (CustomFood) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var barcode by remember(initial, defaultBarcode) { mutableStateOf(initial?.barcode?.ifBlank { defaultBarcode } ?: defaultBarcode) }
    var isSupplement by remember(initial) { mutableStateOf(initial?.isSupplement ?: false) }
    var unitLabel by remember(initial) { mutableStateOf(initial?.servingUnit?.ifBlank { "قرص" } ?: "قرص") }
    var servingG by remember(initial) { mutableStateOf(initial?.servingGrams?.toString().orEmpty()) }

    val mainKeys = listOf("kcal", "p", "f", "c", "fib", "sug", "sat", "na")
    val allEditableKeys = mainKeys + SUPPLEMENT_MICRO_KEYS
    val values = remember(initial) {
        androidx.compose.runtime.mutableStateMapOf<String, String>().apply {
            allEditableKeys.forEach { k ->
                val v = initial?.nutrientsPer100?.get(k)
                put(k, v?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty())
            }
        }
    }
    var err by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("اسم المنتج أو المكمل") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("label_name_input")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = !isSupplement,
                onClick = { isSupplement = false },
                label = { Text("منتج غذائي (لكل 100 جم)") }
            )
            FilterChip(
                selected = isSupplement,
                onClick = { isSupplement = true },
                label = { Text("مكمل / فيتامين (بالقرص)") }
            )
        }
        if (isSupplement) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("قرص", "كبسولة", "سكوب", "كيس").forEach { u ->
                    FilterChip(
                        selected = unitLabel == u,
                        onClick = { unitLabel = u },
                        label = { Text(u) }
                    )
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = barcode, onValueChange = { barcode = it.filter(Char::isDigit) },
                    label = { Text("الباركود") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = servingG, onValueChange = { servingG = it },
                    label = { Text("وزن الحصة (جم)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            text = if (isSupplement) "القيم لكل 1 $unitLabel (السعرات اختيارية):" else "القيم لكل 100 جم:",
            style = MaterialTheme.typography.labelMedium
        )
        mainKeys.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { k ->
                    OutlinedTextField(
                        value = values[k].orEmpty(),
                        onValueChange = { values[k] = it },
                        label = { Text(NutritionConstants.NUTRIENT_LABELS[k] ?: k) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        ExpandableSectionCard(
            title = "الفيتامينات والمعادن والأوميجا 3",
            initiallyExpanded = isSupplement || SUPPLEMENT_MICRO_KEYS.any { !values[it].isNullOrBlank() },
            testTag = "label_micros_section"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SUPPLEMENT_MICRO_KEYS.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { k ->
                            val unit = NutritionConstants.NUTRIENT_UNITS[k] ?: ""
                            OutlinedTextField(
                                value = values[k].orEmpty(),
                                onValueChange = { values[k] = it },
                                label = { Text("${NutritionConstants.NUTRIENT_LABELS[k] ?: k} ($unit)", maxLines = 1) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        if (!err.isNullOrBlank()) {
            Text(err!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = {
                val kcal = values["kcal"]?.replace(',', '.')?.toDoubleOrNull()
                val hasAnyMicro = SUPPLEMENT_MICRO_KEYS.any { values[it]?.replace(',', '.')?.toDoubleOrNull() != null }
                if (name.isBlank() || (kcal == null && !isSupplement && !hasAnyMicro)) {
                    err = "اكتب اسم المنتج والسعرات (أو أي فيتامين/معدن للمكملات)."
                    return@Button
                }
                err = null
                val merged = (initial?.nutrientsPer100 ?: emptyMap()).toMutableMap()
                allEditableKeys.forEach { k ->
                    merged[k] = values[k]?.replace(',', '.')?.toDoubleOrNull()
                }
                if (merged["kcal"] == null) merged["kcal"] = 0.0

                val portions = if (isSupplement) {
                    listOf(
                        PortionOption("1 $unitLabel", 100.0),
                        PortionOption("2 $unitLabel", 200.0)
                    )
                } else {
                    val sg = servingG.replace(',', '.').toDoubleOrNull()
                    if (sg != null && sg > 0.0) listOf(PortionOption("حصة", sg)) else emptyList()
                }
                onSave(
                    CustomFood(
                        name = name.trim(),
                        source = "l",
                        barcode = barcode.filter(Char::isDigit),
                        favorite = false,
                        custom = true,
                        nutrients = NutritionConstants.vectorFromMap(merged),
                        portions = portions
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().testTag("save_label_food_button")
        ) { Text(if (isSupplement) "احفظ المكمل واستخدمه" else "احفظ المنتج كأكلة") }
    }
}

@Composable
private fun RecipeBuilderSection(
    searchResults: List<FoodItem>,
    onSearch: (String) -> Unit,
    onSaveRecipe: (CustomFood) -> Unit
) {
    var recipeName by remember { mutableStateOf("") }
    var servingsText by remember { mutableStateOf("1") }
    var cookedWeightText by remember { mutableStateOf("") }
    var ingQuery by remember { mutableStateOf("") }
    val ingredients = remember { mutableStateListOf<RecipeIngredient>() }
    var err by remember { mutableStateOf<String?>(null) }

    fun computeTotals(): Triple<DoubleArray, BooleanArray, Double> {
        val sum = DoubleArray(33)
        val has = BooleanArray(33)
        var rawW = 0.0
        ingredients.forEach { ing ->
            rawW += ing.grams
            val ret = NutritionConstants.RETENTION_BY_CODE[ing.methodCode]?.factorsPercent
            ing.nutrients.forEachIndexed { i, v ->
                if (i < 33 && v >= 0.0) {
                    var factor = 1.0
                    if (ret != null) {
                        val key = NutritionConstants.NUTRIENT_KEYS[i]
                        val rIdx = NutritionConstants.RETENTION_KEYS.indexOf(key)
                        if (rIdx >= 0) factor = ret[rIdx] / 100.0
                    }
                    sum[i] += v * ing.grams / 100.0 * factor
                    has[i] = true
                }
            }
        }
        return Triple(sum, has, rawW)
    }

    val (totArr, hasArr, rawWeight) = computeTotals()
    val servings = servingsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val finalWeight = cookedWeightText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: rawWeight

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = recipeName, onValueChange = { recipeName = it },
            label = { Text("اسم الوصفة (مثلاً: كشري البيت)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = servingsText, onValueChange = { servingsText = it },
                label = { Text("عدد الأطباق") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = cookedWeightText, onValueChange = { cookedWeightText = it },
                label = { Text("وزن الطبخة النهائي (اختياري)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        ingredients.forEachIndexed { idx, ing ->
            var methodMenu by remember { mutableStateOf(false) }
            var gText by remember(idx, ing.grams) { mutableStateOf(ing.grams.roundToInt().toString()) }
            Surface(
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ing.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { ingredients.removeAt(idx) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.cd_delete))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = gText,
                            onValueChange = {
                                gText = it
                                it.replace(',', '.').toDoubleOrNull()?.let { d ->
                                    ingredients[idx] = ing.copy(grams = d)
                                }
                            },
                            label = { Text("جم") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(88.dp)
                        )
                        OutlinedButton(onClick = { methodMenu = true }, modifier = Modifier.weight(1f)) {
                            Text(
                                text = NutritionConstants.RETENTION_BY_CODE[ing.methodCode]?.labelAr ?: "نيّء",
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        DropdownMenu(expanded = methodMenu, onDismissRequest = { methodMenu = false }) {
                            NutritionConstants.RETENTION_METHODS.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.labelAr) },
                                    onClick = {
                                        ingredients[idx] = ing.copy(methodCode = m.code)
                                        methodMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = ingQuery,
            onValueChange = {
                ingQuery = it
                onSearch(it)
            },
            label = { Text("ابحث عن مكوّن وأضفه") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (ingQuery.trim().length >= 2) {
            searchResults.take(6).forEach { cand ->
                FoodListRow(item = cand) {
                    ingredients.add(RecipeIngredient(cand.name, 100.0, "raw", cand.nutrients))
                    ingQuery = ""
                }
            }
        }

        if (ingredients.isNotEmpty() && finalWeight > 0) {
            Text(
                text = "للطبق الواحد (حوالي ${(finalWeight / servings).roundToInt()} جم): ${(totArr[0] / servings).roundToInt()} سعر · بروتين ${(totArr[1] / servings).roundToInt()} · كارب ${(totArr[3] / servings).roundToInt()} · دهون ${(totArr[2] / servings).roundToInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        if (!err.isNullOrBlank()) {
            Text(err!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = {
                if (recipeName.isBlank() || ingredients.isEmpty()) {
                    err = "اكتب اسم الوصفة وأضف مكوّن واحد على الأقل."
                    return@Button
                }
                err = null
                val per100 = List(33) { i ->
                    if (hasArr[i] && finalWeight > 0) ((totArr[i] / finalWeight * 100000.0).roundToInt() / 1000.0) else -1.0
                }
                onSaveRecipe(
                    CustomFood(
                        name = recipeName.trim(),
                        source = "c",
                        custom = true,
                        nutrients = per100,
                        portions = listOf(PortionOption("طبق (1 من $servings)", (finalWeight / servings).roundToInt().toDouble())),
                        recipeServings = servings,
                        recipeCookedWeight = finalWeight,
                        recipeIngredients = ingredients.toList()
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().testTag("save_recipe_button")
        ) { Text("احفظ الوصفة") }
        Text(
            text = "فقد الفيتامينات والمعادن بالطبخ بيتحسب تلقائيًا من جدول USDA لنسب الاحتفاظ (Release 6).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MyFoodsSection(
    customFoods: List<CustomFood>,
    onSelectToLog: (CustomFood) -> Unit,
    onUpdateFood: (CustomFood) -> Unit,
    onDeleteFood: (String) -> Unit
) {
    var editing by remember { mutableStateOf<CustomFood?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (editing != null) {
        val f = editing!!
        var name by remember(f.id) { mutableStateOf(f.name) }
        val keys = listOf("kcal", "p", "c", "f", "fib", "na")
        val vals = remember(f.id) {
            androidx.compose.runtime.mutableStateMapOf<String, String>().apply {
                keys.forEach { k ->
                    val idx = NutritionConstants.NUTRIENT_INDEX[k] ?: -1
                    val v = f.nutrients.getOrNull(idx) ?: -1.0
                    put(k, if (v >= 0) v.toString() else "")
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth())
            keys.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { k ->
                        OutlinedTextField(
                            value = vals[k].orEmpty(),
                            onValueChange = { vals[k] = it },
                            label = { Text(NutritionConstants.NUTRIENT_LABELS[k] ?: k) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Button(
                onClick = {
                    val vec = f.nutrients.toMutableList()
                    keys.forEach { k ->
                        val idx = NutritionConstants.NUTRIENT_INDEX[k] ?: return@forEach
                        vec[idx] = vals[k]?.replace(',', '.')?.toDoubleOrNull() ?: -1.0
                    }
                    onUpdateFood(f.copy(name = name.trim().ifBlank { f.name }, nutrients = vec))
                    editing = null
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("احفظ التعديلات") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (!confirmDelete) {
                            confirmDelete = true
                        } else {
                            onDeleteFood(f.id)
                            editing = null
                            confirmDelete = false
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (confirmDelete) "تأكيد الحذف؟" else "حذف الأكلة",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedButton(onClick = { editing = null; confirmDelete = false }, modifier = Modifier.weight(1f)) {
                    Text("رجوع")
                }
            }
        }
    } else if (customFoods.isEmpty()) {
        Text(
            text = "لسه مفيش أكلات خاصة. الوصفات والملصقات والأكلات اللي بتحفظها بتظهر هنا.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        customFoods.forEach { cf ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectToLog(cf) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(cf.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    val meta = buildString {
                        append(NutritionConstants.SOURCE_LABELS[cf.source] ?: "")
                        if (cf.recipeServings != null) append(" · وصفة")
                        if (cf.barcode.isNotBlank()) append(" · باركود ${cf.barcode}")
                    }
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { editing = cf; confirmDelete = false }) { Text("تعديل") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun CustomFoodForm(onSave: (CustomFood) -> Unit) {
    var isSupplement by remember { mutableStateOf(false) }
    var unitLabel by remember { mutableStateOf("قرص") }
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }
    var f by remember { mutableStateOf("") }
    var fib by remember { mutableStateOf("") }
    var na by remember { mutableStateOf("") }
    var bc by remember { mutableStateOf("") }
    val microVals = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    var err by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = !isSupplement,
                onClick = { isSupplement = false },
                label = { Text("أكلة خاصة (لكل 100 جم)") }
            )
            FilterChip(
                selected = isSupplement,
                onClick = { isSupplement = true },
                label = { Text("مكمل / فيتامين (بالقرص)") }
            )
        }
        if (isSupplement) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("قرص", "كبسولة", "سكوب", "كيس").forEach { u ->
                    FilterChip(
                        selected = unitLabel == u,
                        onClick = { unitLabel = u },
                        label = { Text(u) }
                    )
                }
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(if (isSupplement) "اسم المكمل أو الفيتامين" else "الاسم") },
            modifier = Modifier.fillMaxWidth().testTag("custom_food_name_input")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = kcal,
                onValueChange = { kcal = it },
                label = { Text(if (isSupplement) "سعرات/1 $unitLabel (اختياري)" else "سعرات/100جم") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = p,
                onValueChange = { p = it },
                label = { Text("بروتين (جم)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = c, onValueChange = { c = it }, label = { Text("كارب (جم)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            OutlinedTextField(value = f, onValueChange = { f = it }, label = { Text("دهون (جم)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
        }
        if (!isSupplement) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = fib, onValueChange = { fib = it }, label = { Text("ألياف") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = na, onValueChange = { na = it }, label = { Text("صوديوم مجم") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
        }

        ExpandableSectionCard(
            title = if (isSupplement) "الفيتامينات والمعادن والأوميجا 3 (لكل 1 $unitLabel)" else "فيتامينات ومعادن (اختياري)",
            initiallyExpanded = isSupplement,
            testTag = "custom_food_micros_section"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SUPPLEMENT_MICRO_KEYS.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { k ->
                            val unit = NutritionConstants.NUTRIENT_UNITS[k] ?: ""
                            OutlinedTextField(
                                value = microVals[k].orEmpty(),
                                onValueChange = { microVals[k] = it },
                                label = { Text("${NutritionConstants.NUTRIENT_LABELS[k] ?: k} ($unit)", maxLines = 1) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        OutlinedTextField(value = bc, onValueChange = { bc = it.filter(Char::isDigit) }, label = { Text("باركود (اختياري)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        if (!err.isNullOrBlank()) Text(err!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Button(
            onClick = {
                val kv = kcal.replace(',', '.').toDoubleOrNull()
                if (name.isBlank() || (!isSupplement && kv == null)) {
                    err = if (isSupplement) "اكتب اسم المكمل الأول." else "اكتب الاسم والسعرات على الأقل."
                    return@Button
                }
                err = null
                val map = mutableMapOf<String, Double?>(
                    "kcal" to (kv ?: 0.0),
                    "p" to p.replace(',', '.').toDoubleOrNull(),
                    "c" to c.replace(',', '.').toDoubleOrNull(),
                    "f" to f.replace(',', '.').toDoubleOrNull(),
                    "fib" to fib.replace(',', '.').toDoubleOrNull(),
                    "na" to na.replace(',', '.').toDoubleOrNull()
                )
                SUPPLEMENT_MICRO_KEYS.forEach { k ->
                    microVals[k]?.replace(',', '.')?.toDoubleOrNull()?.let { map[k] = it }
                }
                val portions = if (isSupplement) {
                    listOf(
                        PortionOption("1 $unitLabel", 100.0),
                        PortionOption("2 $unitLabel", 200.0)
                    )
                } else emptyList()
                onSave(
                    CustomFood(
                        name = name.trim(),
                        source = "c",
                        barcode = bc.filter(Char::isDigit),
                        custom = true,
                        nutrients = NutritionConstants.vectorFromMap(map),
                        portions = portions
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().testTag("save_custom_food_button")
        ) { Text(if (isSupplement) "احفظ المكمل" else "احفظ الأكلة") }
    }
}
