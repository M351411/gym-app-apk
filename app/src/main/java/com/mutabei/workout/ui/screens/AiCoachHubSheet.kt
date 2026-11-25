package com.mutabei.workout.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutabei.workout.data.model.CoachChatMessage
import com.mutabei.workout.data.model.CoachReportRecord
import com.mutabei.workout.data.model.CoachSuggestedFood
import com.mutabei.workout.data.model.CoachSuggestedTargets
import com.mutabei.workout.data.model.SplitDay
import com.mutabei.workout.ui.components.CoachActionProposalsSection
import com.mutabei.workout.ui.components.CoachAnalysisAndPlanTab
import com.mutabei.workout.ui.components.CoachArchiveTab
import com.mutabei.workout.ui.components.CoachAutoLoggedSection
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.util.rememberImageCaptureController
import com.mutabei.workout.ui.viewmodel.AiCoachChatUiState
import com.mutabei.workout.ui.viewmodel.AiCoachUiState
import com.mutabei.workout.ui.viewmodel.AiPreWorkoutUiState

data class CoachTodayMacroStatus(
    val targetKcal: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatG: Int,
    val consumedKcal: Int,
    val consumedProteinG: Int,
    val consumedCarbsG: Int,
    val consumedFatG: Int
)

private val QUICK_CHAT_PROMPTS = listOf(
    "بديل تمرين بسبب ألم في الكتف" to "عندي وجع بسيط في كتفي النهارده، إيه أفضل بديلين آمنين لتمارين الدفع في جدولي الحالي؟",
    "تخطيط خروجة / مطعم" to "عندي خروجة ومطعم بالليل، قولي أوزع باقي سعراتي وماكروز النهارده إزاي عشان مبوظش الدايت؟",
    "تحليل ثبات الوزن" to "حلّل وزني وسعراتي آخر أسبوعين وقولي لو فيه ثبات أعدّل السعرات والماكروز لكام بالظبط؟",
    "عشاء يقفل البروتين" to "اقترحلي عشاء مصري سريع يقفل باقي البروتين والسعرات بتاعة النهارده عشان أضيفه على طول.",
    "مراجعة مكملاتي" to "راجع الميكروز والمكملات اللي سجلتها النهارده وقولي محتاج أزود أو أعدّل إيه؟"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCoachHubSheet(
    weekNumber: Int,
    splitDays: List<SplitDay>,
    selectedDayIndex: Int,
    todayMacros: CoachTodayMacroStatus?,
    chatState: AiCoachChatUiState,
    preWorkoutState: AiPreWorkoutUiState,
    weeklyState: AiCoachUiState,
    savedReports: List<CoachReportRecord>,
    snackbarHostState: SnackbarHostState,
    onSendChat: (String, Bitmap?) -> Unit,
    onRetryChat: () -> Unit,
    onClearChat: () -> Unit,
    onUndoAutoLoggedFood: (String, String) -> Unit,
    onMoveAutoLoggedFoodMeal: (String, String, Int) -> Unit,
    onGeneratePreWorkout: (Int) -> Unit,
    onGenerateWeeklyReport: () -> Unit,
    onDeleteReport: (String) -> Unit,
    onApplyTargets: (CoachSuggestedTargets) -> Unit,
    onAddFood: (CoachSuggestedFood) -> Unit,
    onOpenOfflineNutrition: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onDismiss)

    // Render directly inside the Activity window (instead of a separate Dialog window)
    // so Android's adjustResize + WindowInsets.ime work reliably on all OEMs (ColorOS/Xiaomi/Samsung/Pixel).
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                )
            },
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "المدرب الذكي",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "الأسبوع $weekNumber · متصل ببياناتك الحقيقية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            if (activeTab == 0 && chatState.messages.isNotEmpty()) {
                                IconButton(
                                    onClick = onClearChat,
                                    modifier = Modifier.testTag("coach_clear_chat_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteSweep,
                                        contentDescription = "مسح المحادثة",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("coach_hub_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "إغلاق المدرب الذكي"
                                )
                            }
                        }
                    )

                    PrimaryTabRow(
                        selectedTabIndex = activeTab,
                        containerColor = MaterialTheme.colorScheme.background
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("الشات الذكي", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("coach_tab_chat")
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("التحليل والخطة", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("coach_tab_plan")
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = {
                                val countSuffix = if (savedReports.isNotEmpty()) " (${savedReports.size})" else ""
                                Text(
                                    "الأرشيف$countSuffix",
                                    fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("coach_tab_archive")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    0 -> CoachChatTab(
                        todayMacros = todayMacros,
                        chatState = chatState,
                        onSend = onSendChat,
                        onRetry = onRetryChat,
                        onUndoAutoLoggedFood = onUndoAutoLoggedFood,
                        onMoveAutoLoggedFoodMeal = onMoveAutoLoggedFoodMeal,
                        onApplyTargets = onApplyTargets,
                        onAddFood = onAddFood,
                        onOpenOfflineNutrition = onOpenOfflineNutrition,
                        onPhotoError = onShowMessage
                    )

                    1 -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        CoachAnalysisAndPlanTab(
                            splitDays = splitDays,
                            initialDayIndex = selectedDayIndex,
                            preWorkoutState = preWorkoutState,
                            weeklyState = weeklyState,
                            onGeneratePreWorkout = onGeneratePreWorkout,
                            onGenerateWeeklyReport = onGenerateWeeklyReport,
                            onApplyTargets = onApplyTargets,
                            onAddFood = onAddFood
                        )
                    }

                    2 -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        CoachArchiveTab(
                            reports = savedReports,
                            onDeleteReport = onDeleteReport,
                            onApplyTargets = onApplyTargets,
                            onAddFood = onAddFood,
                            onJumpToGenerate = { activeTab = 1 }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveRemainingMacrosStrip(
    status: CoachTodayMacroStatus,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val remKcal = status.targetKcal - status.consumedKcal
    val remP = status.targetProteinG - status.consumedProteinG
    val remC = status.targetCarbsG - status.consumedCarbsG
    val remF = status.targetFatG - status.consumedFatG
    val overKcal = remKcal < 0
    val kcalAccent = if (overKcal) semantic.low else semantic.ok

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("coach_live_remaining_strip")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (overKcal) "زيادة اليوم:" else "المتبقي اليوم:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (overKcal) "+${-remKcal} سعر" else "$remKcal سعر",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = kcalAccent
                    )
                    Text(
                        text = "· ب ${remP}جم",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (remP > 0) MaterialTheme.colorScheme.primary else semantic.ok
                    )
                    Text(
                        text = "· ك ${remC}جم",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "· د ${remF}جم",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun CoachChatTab(
    todayMacros: CoachTodayMacroStatus?,
    chatState: AiCoachChatUiState,
    onSend: (String, Bitmap?) -> Unit,
    onRetry: () -> Unit,
    onUndoAutoLoggedFood: (String, String) -> Unit,
    onMoveAutoLoggedFoodMeal: (String, String, Int) -> Unit,
    onApplyTargets: (CoachSuggestedTargets) -> Unit,
    onAddFood: (CoachSuggestedFood) -> Unit,
    onOpenOfflineNutrition: (Boolean) -> Unit,
    onPhotoError: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    val imageCapture = rememberImageCaptureController(
        onBitmapCaptured = { bmp -> pendingBitmap = bmp },
        onError = onPhotoError
    )

    LaunchedEffect(chatState.messages.size, chatState.busyMessage != null, isImeVisible) {
        val total = chatState.messages.size +
            (if (chatState.busyMessage != null) 1 else 0) +
            (if (!chatState.errorMessage.isNullOrBlank()) 1 else 0)
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (todayMacros != null) {
            LiveRemainingMacrosStrip(status = todayMacros)
        }

        if (chatState.messages.isEmpty() && chatState.busyMessage == null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "أهلاً يا بطل! اسألني أو سجّل أكلك بالكلام 💪",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "لو لخبطت أو مش عارف سعرات وجبة (كشري، شاورما، أكل بيت، عزومة) — اوصفها لي أو صوّرها وهحسبها وأضيفها ليوم النهارده أو امبارح تلقائياً وأقولك متبقيلك كام بالظبط!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "ابدأ بسرعة:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            inputText = "لخبطت وأكلت النهارده: "
                        },
                        label = {
                            Text(
                                "لخبطت / سجّل وجبة مش عارف سعراتها",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("coach_quick_log_meal_chip")
                    )
                    AssistChip(
                        onClick = {
                            inputText = "نسيت أسجل وجبات امبارح، أكلت: "
                        },
                        label = {
                            Text(
                                "سجّل وجبات امبارح اللي نسيتها",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("coach_quick_log_yesterday_chip")
                    )
                    QUICK_CHAT_PROMPTS.forEachIndexed { idx, (label, prompt) ->
                        AssistChip(
                            onClick = { onSend(prompt, null) },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("coach_quick_prompt_$idx")
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatState.messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        message = msg,
                        onUndoAutoLoggedFood = { itemId -> onUndoAutoLoggedFood(msg.id, itemId) },
                        onMoveAutoLoggedFoodMeal = { itemId, newSlot -> onMoveAutoLoggedFoodMeal(msg.id, itemId, newSlot) },
                        onApplyTargets = onApplyTargets,
                        onAddFood = onAddFood
                    )
                }

                if (chatState.busyMessage != null) {
                    item(key = "chat_typing_indicator") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(
                                text = chatState.busyMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!chatState.errorMessage.isNullOrBlank()) {
                    item(key = "chat_error_card") {
                        var showTechDetails by remember(chatState.errorMessage) { mutableStateOf(false) }
                        val diagIdx = listOf("\n[Stage=", "\n[init=")
                            .map { chatState.errorMessage.indexOf(it) }
                            .filter { it >= 0 }
                            .minOrNull() ?: -1
                        val friendlyText = if (diagIdx >= 0) {
                            chatState.errorMessage.substring(0, diagIdx).trim()
                        } else {
                            chatState.errorMessage.trim()
                        }
                        val techDetails = if (diagIdx >= 0) {
                            chatState.errorMessage.substring(diagIdx + 1).trim()
                        } else {
                            ""
                        }
                        val lastUserText = chatState.messages.lastOrNull { it.fromUser }?.text.orEmpty()
                        val mentionsYesterday = listOf("امبارح", "أمس", "امس", "البارح", "yesterday")
                            .any { lastUserText.contains(it, ignoreCase = true) }

                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = friendlyText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { onOpenOfflineNutrition(mentionsYesterday) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("coach_offline_meal_fallback_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Restaurant,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (mentionsYesterday) {
                                                "سجّل وجبات أمس الآن (أوفلاين بدون نت)"
                                            } else {
                                                "سجّل وجباتك الآن (أوفلاين بدون نت)"
                                            }
                                        )
                                    }

                                    if (chatState.canRetry && chatState.busyMessage == null) {
                                        OutlinedButton(
                                            onClick = onRetry,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("coach_retry_last_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("إعادة المحاولة مع المدرب الذكي")
                                        }
                                    }
                                }

                                if (techDetails.isNotBlank()) {
                                    TextButton(
                                        onClick = { showTechDetails = !showTechDetails },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = if (showTechDetails) "إخفاء التفاصيل التقنية" else "عرض التفاصيل التقنية",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    if (showTechDetails) {
                                        Text(
                                            text = techDetails,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom multimodal input dock — single union(navigationBars, ime) inset padding
        // keeps the input box flush directly above the keyboard with zero double-padding or clipping.
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                AnimatedVisibility(visible = chatState.messages.isNotEmpty() && !isImeVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = { inputText = "أكلت النهارده: " },
                            label = { Text("سجّل وجبة / لخبطة", style = MaterialTheme.typography.labelSmall) }
                        )
                        AssistChip(
                            onClick = { inputText = "نسيت أسجل وجبات امبارح، أكلت: " },
                            label = { Text("سجّل وجبات امبارح", style = MaterialTheme.typography.labelSmall) }
                        )
                        QUICK_CHAT_PROMPTS.take(3).forEach { (label, prompt) ->
                            AssistChip(
                                onClick = {
                                    if (chatState.busyMessage == null) onSend(prompt, null)
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                pendingBitmap?.let { bmp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "صورة مرفقة",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(22.dp)
                            ) {
                                IconButton(
                                    onClick = { pendingBitmap = null },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "إزالة الصورة",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "صورة جاهزة للإرسال للمدرب",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Full-width clear text input field + compact action bar underneath
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "اكتب وجبتك أو سؤالك للمدرب...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        minLines = 1,
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("coach_chat_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = imageCapture.launchCamera,
                                enabled = chatState.busyMessage == null,
                                modifier = Modifier.testTag("coach_chat_camera_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoCamera,
                                    contentDescription = "تصوير منيو أو تلاجة أو مكمل",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = imageCapture.launchGallery,
                                enabled = chatState.busyMessage == null,
                                modifier = Modifier.testTag("coach_chat_gallery_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoLibrary,
                                    contentDescription = "اختيار صورة من المعرض",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        FilledIconButton(
                            onClick = {
                                val textToSend = inputText
                                val bmpToSend = pendingBitmap
                                inputText = ""
                                pendingBitmap = null
                                onSend(textToSend, bmpToSend)
                            },
                            enabled = chatState.busyMessage == null && (inputText.isNotBlank() || pendingBitmap != null),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("coach_chat_send_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "إرسال"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: CoachChatMessage,
    onUndoAutoLoggedFood: (String) -> Unit,
    onMoveAutoLoggedFoodMeal: (String, Int) -> Unit,
    onApplyTargets: (CoachSuggestedTargets) -> Unit,
    onAddFood: (CoachSuggestedFood) -> Unit
) {
    val alignment = if (message.fromUser) Alignment.End else Alignment.Start
    val bubbleColor = if (message.fromUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val bubbleShape = if (message.fromUser) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            border = if (message.fromUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                message.imageBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "صورة مرسلة للمدرب",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.fromUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        if (!message.fromUser && message.autoLoggedFoods.isNotEmpty()) {
            CoachAutoLoggedSection(
                entries = message.autoLoggedFoods,
                onUndoItem = onUndoAutoLoggedFood,
                onMoveMealSlot = onMoveAutoLoggedFoodMeal,
                modifier = Modifier.widthIn(max = 350.dp)
            )
        }

        if (!message.fromUser && (message.suggestedTargets != null || message.suggestedFoods.isNotEmpty())) {
            CoachActionProposalsSection(
                suggestedTargets = message.suggestedTargets,
                suggestedFoods = message.suggestedFoods,
                onApplyTargets = onApplyTargets,
                onAddFood = onAddFood,
                modifier = Modifier.widthIn(max = 350.dp)
            )
        }
    }
}
