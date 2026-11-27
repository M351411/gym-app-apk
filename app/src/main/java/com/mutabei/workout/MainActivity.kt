package com.mutabei.workout

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.mutabei.workout.data.model.UiState
import com.mutabei.workout.data.model.todayIso
import com.mutabei.workout.data.repository.FirestoreRepository
import com.mutabei.workout.data.repository.GeminiAiRepository
import com.mutabei.workout.data.repository.OfflineFoodRepository
import com.mutabei.workout.data.repository.OpenFoodFactsRepository
import com.mutabei.workout.ui.components.RestTimerFab
import com.mutabei.workout.ui.components.RestTimerOverlay
import com.mutabei.workout.ui.components.formatWorkoutElapsed
import com.mutabei.workout.ui.components.isolateLtr
import com.mutabei.workout.ui.components.subtleCardBorderColor
import com.mutabei.workout.ui.components.tactilePressScale
import com.mutabei.workout.ui.screens.AiCoachHubSheet
import com.mutabei.workout.ui.screens.AuthScreen
import com.mutabei.workout.ui.screens.BodyScreen
import com.mutabei.workout.ui.screens.CoachTodayMacroStatus
import com.mutabei.workout.ui.screens.NutritionScreen
import com.mutabei.workout.ui.screens.SettingsSheet
import com.mutabei.workout.ui.screens.WorkoutScreen
import com.mutabei.workout.ui.theme.LocalSemanticColors
import com.mutabei.workout.ui.theme.MutabeiTheme
import com.mutabei.workout.ui.viewmodel.AiDeficiencyUiState
import com.mutabei.workout.ui.viewmodel.AiMacroTetrisUiState
import com.mutabei.workout.ui.viewmodel.AppDataSnapshot
import com.mutabei.workout.ui.viewmodel.MainTab
import com.mutabei.workout.ui.viewmodel.MainViewModel
import com.mutabei.workout.ui.viewmodel.WorkoutSessionTimerState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Must execute first before any Firebase API (Auth, Firestore, AI Logic) is accessed.
        configureAppCheck(this, intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val authListener = FirebaseAuth.AuthStateListener { auth ->
                        currentUser = auth.currentUser
                    }
                    val lifecycleObserver = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            currentUser = Firebase.auth.currentUser
                        }
                    }
                    Firebase.auth.addAuthStateListener(authListener)
                    lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
                    currentUser = Firebase.auth.currentUser
                    onDispose {
                        Firebase.auth.removeAuthStateListener(authListener)
                        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                    }
                }

                val activeUser = currentUser
                if (activeUser == null) {
                    MutabeiTheme(themeMode = "dark") {
                        AuthScreen(
                            onAuthSuccess = { verifiedUser ->
                                currentUser = Firebase.auth.currentUser ?: verifiedUser
                            }
                        )
                    }
                } else {
                    key(activeUser.uid) {
                        MainAppShell(user = activeUser)
                    }
                }
            }
        }
    }
}

private data class NavDestination(
    val tab: MainTab,
    val labelRes: Int,
    val icon: ImageVector,
    val tag: String
)

private val NAV_DESTINATIONS = listOf(
    NavDestination(MainTab.WORKOUT, R.string.nav_workouts, Icons.Rounded.FitnessCenter, "nav_workout"),
    NavDestination(MainTab.NUTRITION, R.string.nav_nutrition, Icons.Rounded.Restaurant, "nav_nutrition"),
    NavDestination(MainTab.BODY, R.string.nav_body, Icons.Rounded.MonitorWeight, "nav_body")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppShell(user: FirebaseUser) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val viewModel: MainViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                MainViewModel(
                    appContext = app,
                    firestoreRepo = FirestoreRepository(app),
                    offlineFoodRepo = OfflineFoodRepository(app),
                    geminiRepo = GeminiAiRepository(),
                    offRepo = OpenFoodFactsRepository(),
                    currentUserId = user.uid
                )
            }
        }
    )

    val dataState by viewModel.dataState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedDayIndex by viewModel.selectedDayIndex.collectAsStateWithLifecycle()
    val selectedNutritionDate by viewModel.selectedNutritionDate.collectAsStateWithLifecycle()
    val workoutDrafts by viewModel.workoutDrafts.collectAsStateWithLifecycle()
    val restTimer by viewModel.restTimer.collectAsStateWithLifecycle()
    val workoutTimer by viewModel.workoutSessionTimer.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val foodSheetAi by viewModel.foodSheetAi.collectAsStateWithLifecycle()
    val bodyDraft by viewModel.bodyDraft.collectAsStateWithLifecycle()
    val microSuggestions by viewModel.microSuggestions.collectAsStateWithLifecycle()
    val deficiencyAi by viewModel.deficiencyAi.collectAsStateWithLifecycle()
    val coachReport by viewModel.coachReport.collectAsStateWithLifecycle()
    val preWorkoutPlan by viewModel.preWorkoutPlan.collectAsStateWithLifecycle()
    val coachChat by viewModel.coachChat.collectAsStateWithLifecycle()
    val macroTetrisAi by viewModel.macroTetrisAi.collectAsStateWithLifecycle()
    val battleResultsA by viewModel.battleResultsA.collectAsStateWithLifecycle()
    val battleResultsB by viewModel.battleResultsB.collectAsStateWithLifecycle()

    val isOnlineFlow = remember(context) { observeNetworkStatus(context) }
    val isOnline by isOnlineFlow.collectAsStateWithLifecycle(initialValue = true)

    val snackbarHostState = remember { SnackbarHostState() }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showCoachDialog by remember { mutableStateOf(false) }
    var workoutFabVisible by remember { mutableStateOf(true) }

    val snapshot = (dataState as? UiState.Success)?.data
    // Default to dark during initial hydration so no white frame ever flashes after the dark splash screen
    val themeMode = snapshot?.config?.themeMode ?: "dark"

    LaunchedEffect(selectedTab) {
        workoutFabVisible = true
    }

    LaunchedEffect(viewModel) {
        viewModel.toastMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.timerPreAlertEvents.collect {
            triggerTimerPreAlertFeedback(context)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.timerFinishedEvents.collect {
            triggerTimerCompletionFeedback(context)
        }
    }

    BackHandler(enabled = selectedTab != MainTab.WORKOUT) {
        viewModel.selectTab(MainTab.WORKOUT)
    }

    MutabeiTheme(themeMode = themeMode) {
        val semantic = LocalSemanticColors.current

        if (showSettingsSheet && snapshot != null) {
            SettingsSheet(
                user = user,
                config = snapshot.config,
                viewModel = viewModel,
                onDismiss = { showSettingsSheet = false }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 600.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            NAV_DESTINATIONS.forEach { dest ->
                                NavigationRailItem(
                                    selected = selectedTab == dest.tab,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.selectTab(dest.tab)
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = dest.icon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.tag)
                                )
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = {
                            if (!showCoachDialog) {
                                SnackbarHost(hostState = snackbarHostState)
                            }
                        },
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                title = {
                                    // Minimal sleek single-row header with compact week pill badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.app_header_title),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (snapshot != null) {
                                            val week = viewModel.computeWeekNumber(snapshot.config.startDate)
                                            val pillLabel = if (week <= 12) "أسبوع ${isolateLtr("$week/12")}" else "✓ 12/12"
                                            Surface(
                                                shape = RoundedCornerShape(999.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, subtleCardBorderColor())
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isOnline) semantic.ok else semantic.warn)
                                                    )
                                                    Text(
                                                        text = pillLabel,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontFeatureSettings = "tnum"
                                                        ),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                actions = {
                                    if (snapshot != null && workoutTimer.hasStarted) {
                                        val timerAccent = if (workoutTimer.isRunning) semantic.ok else semantic.warn
                                        Surface(
                                            shape = RoundedCornerShape(99.dp),
                                            color = timerAccent.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, timerAccent.copy(alpha = 0.6f)),
                                            modifier = Modifier
                                                .padding(end = 4.dp)
                                                .clip(RoundedCornerShape(99.dp))
                                                .clickable {
                                                    if (selectedTab != MainTab.WORKOUT) {
                                                        viewModel.selectTab(MainTab.WORKOUT)
                                                    } else {
                                                        viewModel.toggleWorkoutSessionTimer()
                                                    }
                                                }
                                                .testTag("topbar_workout_timer_pill")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(timerAccent)
                                                )
                                                Text(
                                                    text = formatWorkoutElapsed(workoutTimer.elapsedSeconds),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = timerAccent
                                                )
                                            }
                                        }
                                    }
                                    if (snapshot != null) {
                                        IconButton(
                                            onClick = { showCoachDialog = true },
                                            modifier = Modifier.testTag("ai_coach_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoAwesome,
                                                contentDescription = "المدرب الذكي الشامل",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { showSettingsSheet = true },
                                        modifier = Modifier.testTag("settings_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = stringResource(R.string.cd_settings)
                                        )
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            if (!useRail) {
                                FloatingPillBottomBar(
                                    selectedTab = selectedTab,
                                    onSelectTab = { tab ->
                                        if (tab != selectedTab) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.selectTab(tab)
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (!isOnline) {
                                    Surface(
                                        color = semantic.warn,
                                        contentColor = MaterialTheme.colorScheme.onTertiary,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.offline_banner),
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                when (val st = dataState) {
                                    UiState.Loading -> {
                                        StartupDashboardSkeleton()
                                    }

                                    is UiState.Error -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = st.message,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.error,
                                                    textAlign = TextAlign.Center
                                                )
                                                Button(onClick = { showSettingsSheet = true }) {
                                                    Text("فتح الإعدادات")
                                                }
                                            }
                                        }
                                    }

                                    is UiState.Success -> {
                                        ActiveTabContent(
                                            tab = selectedTab,
                                            snapshot = st.data,
                                            selectedDayIndex = selectedDayIndex,
                                            selectedNutritionDate = selectedNutritionDate,
                                            workoutDrafts = workoutDrafts,
                                            workoutTimer = workoutTimer,
                                            searchResults = searchResults,
                                            foodSheetAi = foodSheetAi,
                                            bodyDraft = bodyDraft,
                                            microSuggestions = microSuggestions,
                                            deficiencyAi = deficiencyAi,
                                            macroTetrisAi = macroTetrisAi,
                                            battleResultsA = battleResultsA,
                                            battleResultsB = battleResultsB,
                                            viewModel = viewModel,
                                            onWorkoutFabVisibilityChange = { workoutFabVisible = it }
                                        )
                                    }
                                }
                            }

                            // Floating Rest Timer Overlay / Scroll-aware FAB
                            if (snapshot != null) {
                                if (restTimer.visible) {
                                    RestTimerOverlay(
                                        state = restTimer,
                                        defaultPresetSeconds = snapshot.config.restSeconds,
                                        onAdjust = viewModel::adjustRestTimer,
                                        onSelectPreset = viewModel::selectRestPreset,
                                        onDismiss = viewModel::dismissRestTimer,
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                    )
                                } else {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = selectedTab == MainTab.WORKOUT && workoutFabVisible,
                                        enter = scaleIn(
                                            animationSpec = spring(
                                                dampingRatio = 0.75f,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) + fadeIn(tween(180)),
                                        exit = scaleOut(
                                            animationSpec = spring(
                                                dampingRatio = 0.85f,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) + fadeOut(tween(150)),
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(16.dp)
                                    ) {
                                        RestTimerFab(
                                            onClick = { viewModel.startRestTimer() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showCoachDialog && snapshot != null) {
                val todayNut = snapshot.nutritionDays[todayIso()]
                val targets = viewModel.computeTargets(snapshot.config, snapshot.bodyRecords)
                val todayMacros = targets?.let { t ->
                    val items = todayNut?.items.orEmpty()
                    CoachTodayMacroStatus(
                        targetKcal = t.kcal,
                        targetProteinG = t.proteinG,
                        targetCarbsG = t.carbsG,
                        targetFatG = t.fatG,
                        consumedKcal = items.sumOf { it.nutrientAmount("kcal") }.roundToInt(),
                        consumedProteinG = items.sumOf { it.nutrientAmount("p") }.roundToInt(),
                        consumedCarbsG = items.sumOf { it.nutrientAmount("c") }.roundToInt(),
                        consumedFatG = items.sumOf { it.nutrientAmount("f") }.roundToInt()
                    )
                }
                AiCoachHubSheet(
                    weekNumber = viewModel.computeWeekNumber(snapshot.config.startDate),
                    splitDays = snapshot.config.effectiveSplit(),
                    selectedDayIndex = selectedDayIndex,
                    todayMacros = todayMacros,
                    chatState = coachChat,
                    preWorkoutState = preWorkoutPlan,
                    weeklyState = coachReport,
                    savedReports = snapshot.coachReports,
                    snackbarHostState = snackbarHostState,
                    onSendChat = viewModel::sendCoachChatMessage,
                    onRetryChat = viewModel::retryLastCoachChatMessage,
                    onClearChat = viewModel::clearCoachChat,
                    onUndoAutoLoggedFood = viewModel::undoCoachAutoLoggedFood,
                    onMoveAutoLoggedFoodMeal = viewModel::moveCoachAutoLoggedFoodMeal,
                    onGeneratePreWorkout = viewModel::requestPreWorkoutGamePlan,
                    onGenerateWeeklyReport = viewModel::requestWeeklyCoachReport,
                    onDeleteReport = viewModel::deleteCoachReport,
                    onApplyTargets = viewModel::applyCoachSuggestedTargets,
                    onAddFood = { food -> viewModel.addCoachSuggestedFoodToToday(food) },
                    onOpenOfflineNutrition = { forYesterday ->
                        showCoachDialog = false
                        viewModel.openOfflineMealLogger(forYesterday)
                    },
                    onShowMessage = viewModel::showToast,
                    onDismiss = { showCoachDialog = false }
                )
            }
        }
    }
}

@Composable
private fun StartupDashboardSkeleton() {
    val pulseTransition = rememberInfiniteTransition(label = "startup_skeleton_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    val surfaceColor = MaterialTheme.colorScheme.surface
    val blockColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f * pulseAlpha)
    val borderColor = subtleCardBorderColor()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Split chips skeleton strip
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(84.dp, 92.dp, 72.dp, 78.dp).forEach { chipW ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = surfaceColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .width(chipW)
                            .height(34.dp)
                            .alpha(pulseAlpha)
                    ) {}
                }
            }

            // Sub-view switcher skeleton
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = surfaceColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .alpha(pulseAlpha)
            ) {}

            // Progress hero skeleton card
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = surfaceColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(blockColor)
                        )
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(blockColor)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(blockColor)
                    )
                }
            }

            // Two compact exercise card skeletons
            repeat(2) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = surfaceColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(blockColor)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .height(15.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(blockColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(11.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(blockColor)
                                )
                            }
                        }
                        repeat(2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(blockColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(blockColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(blockColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingPillBottomBar(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val entranceOffsetY by animateDpAsState(
        targetValue = if (entered) 0.dp else 36.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "nav_bar_entrance_offset"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "nav_bar_entrance_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .offset(y = entranceOffsetY)
            .alpha(entranceAlpha)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, subtleCardBorderColor()),
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NAV_DESTINATIONS.forEach { dest ->
                    val selected = selectedTab == dest.tab
                    val pillColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.17f) else Color.Transparent,
                        animationSpec = tween(220),
                        label = "nav_pill_color"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(220),
                        label = "nav_content_color"
                    )
                    val horizontalPad by animateDpAsState(
                        targetValue = if (selected) 14.dp else 8.dp,
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                        label = "nav_pill_pad"
                    )
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (selected) 20.dp else 0.dp,
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                        label = "nav_indicator_width"
                    )
                    val interaction = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .tactilePressScale(interaction, pressedScale = 0.95f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(pillColor)
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) { onSelectTab(dest.tab) }
                            .padding(horizontal = horizontalPad, vertical = 6.dp)
                            .testTag(dest.tag),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = stringResource(dest.labelRes),
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(dest.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .width(indicatorWidth)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveTabContent(
    tab: MainTab,
    snapshot: AppDataSnapshot,
    selectedDayIndex: Int,
    selectedNutritionDate: String,
    workoutDrafts: Map<String, List<com.mutabei.workout.data.model.SetEntry>>,
    workoutTimer: WorkoutSessionTimerState,
    searchResults: List<com.mutabei.workout.data.model.FoodItem>,
    foodSheetAi: com.mutabei.workout.ui.viewmodel.FoodSheetAiState,
    bodyDraft: com.mutabei.workout.ui.viewmodel.BodyDraftState,
    microSuggestions: Map<String, List<com.mutabei.workout.data.repository.NutrientFoodSuggestion>>,
    deficiencyAi: AiDeficiencyUiState,
    macroTetrisAi: AiMacroTetrisUiState,
    battleResultsA: List<com.mutabei.workout.data.model.FoodItem>,
    battleResultsB: List<com.mutabei.workout.data.model.FoodItem>,
    viewModel: MainViewModel,
    onWorkoutFabVisibilityChange: (Boolean) -> Unit
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedContent(
        targetState = tab,
        transitionSpec = {
            val forward = targetState.ordinal >= initialState.ordinal
            val dir = if (forward) (if (isRtl) -1 else 1) else (if (isRtl) 1 else -1)
            val enter = fadeIn(tween(240)) + slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
            ) { fullWidth -> dir * (fullWidth / 10) }
            val exit = fadeOut(tween(180)) + slideOutHorizontally(
                animationSpec = tween(180)
            ) { fullWidth -> -dir * (fullWidth / 10) }
            enter togetherWith exit
        },
        label = "main_tab_slide_fade"
    ) { activeTab ->
        when (activeTab) {
            MainTab.WORKOUT -> WorkoutScreen(
                snapshot = snapshot,
                selectedDayIndex = selectedDayIndex,
                drafts = workoutDrafts,
                workoutTimer = workoutTimer,
                viewModel = viewModel,
                onFabVisibilityChange = onWorkoutFabVisibilityChange
            )

            MainTab.NUTRITION -> NutritionScreen(
                snapshot = snapshot,
                selectedDate = selectedNutritionDate,
                searchResults = searchResults,
                aiState = foodSheetAi,
                microSuggestions = microSuggestions,
                deficiencyAi = deficiencyAi,
                macroTetrisAi = macroTetrisAi,
                battleResultsA = battleResultsA,
                battleResultsB = battleResultsB,
                viewModel = viewModel
            )

            MainTab.BODY -> BodyScreen(
                snapshot = snapshot,
                bodyDraft = bodyDraft,
                viewModel = viewModel
            )
        }
    }
}

private fun observeNetworkStatus(context: Context): Flow<Boolean> = callbackFlow {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (cm == null) {
        trySend(true)
        close()
        return@callbackFlow
    }
    fun currentOnline(): Boolean {
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    trySend(currentOnline())
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trySend(true)
        }

        override fun onLost(network: Network) {
            trySend(currentOnline())
        }
    }
    try {
        cm.registerDefaultNetworkCallback(callback)
    } catch (_: Exception) {
        trySend(true)
    }
    awaitClose {
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
            // ignore
        }
    }
}

/**
 * Gentle double-vibration + subtle short chime fired at 00:05 remaining in Rest Timer.
 */
private fun triggerTimerPreAlertFeedback(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 80, 90, 80), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 80, 90, 80), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(140L)
            }
        }
    } catch (_: Exception) {
        // Vibration not available on this device
    }

    try {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 140)
    } catch (_: Exception) {
        // Audio feedback unavailable
    }
}

private fun triggerTimerCompletionFeedback(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 220, 120, 220, 120, 320), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 220, 120, 220, 120, 320), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(400L)
            }
        }
    } catch (_: Exception) {
        // Vibration not available on this device
    }

    try {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
    } catch (_: Exception) {
        // Audio feedback unavailable
    }
}
