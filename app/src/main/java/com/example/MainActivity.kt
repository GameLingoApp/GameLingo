package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OverlayScreen
import com.example.ui.screens.ProScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.GameLingoTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.GameLingoViewModel

class MainActivity : ComponentActivity() {
    private var viewModelRef: GameLingoViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GameLingoViewModel = viewModel()
            viewModelRef = viewModel
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()

            GameLingoTheme(darkTheme = isDarkTheme) {
                var isSplashActive by remember { mutableStateOf(true) }

                if (isSplashActive) {
                    SplashScreen(
                        onSplashFinished = { isSplashActive = false }
                    )
                } else if (!isOnboardingCompleted) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onComplete = { /* Handled reactively via StateFlow */ }
                    )
                } else {
                    GameLingoApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModelRef?.checkPaymentStatus()
    }
}

@Composable
fun GameLingoApp(
    viewModel: GameLingoViewModel
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val apiKey by viewModel.customApiKey.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            val isGuest = currentUser?.isGuest != false
            val actionLabel = if (isGuest) "Sign in" else "Upgrade"
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                if (isGuest) {
                    viewModel.signInWithGoogle()
                } else {
                    viewModel.selectTab(AppTab.PRO)
                }
            }
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("app_snackbar"),
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        actionColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
                val tertiaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

                // Tab 1: Перевод
                val isTab1 = selectedTab == AppTab.TRANSLATE
                val tab1Color by animateColorAsState(
                    targetValue = if (isTab1) primaryColor else tertiaryTextColor,
                    animationSpec = tween(200),
                    label = "tab1_color"
                )
                NavigationBarItem(
                    selected = isTab1,
                    onClick = { viewModel.selectTab(AppTab.TRANSLATE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = "Перевод",
                            tint = if (isTab1) onPrimaryColor else tab1Color
                        )
                    },
                    label = { Text("Перевод", fontSize = 12.sp, color = tab1Color) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = onPrimaryColor,
                        selectedTextColor = primaryColor,
                        indicatorColor = primaryColor,
                        unselectedIconColor = tertiaryTextColor,
                        unselectedTextColor = tertiaryTextColor
                    ),
                    modifier = Modifier.testTag("nav_tab_translate")
                )

                // Tab 2: Оверлей
                val isTab2 = selectedTab == AppTab.OVERLAY
                val tab2Color by animateColorAsState(
                    targetValue = if (isTab2) primaryColor else tertiaryTextColor,
                    animationSpec = tween(200),
                    label = "tab2_color"
                )
                NavigationBarItem(
                    selected = isTab2,
                    onClick = { viewModel.selectTab(AppTab.OVERLAY) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.PictureInPicture,
                            contentDescription = "Оверлей",
                            tint = if (isTab2) onPrimaryColor else tab2Color
                        )
                    },
                    label = { Text("Оверлей", fontSize = 12.sp, color = tab2Color) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = onPrimaryColor,
                        selectedTextColor = primaryColor,
                        indicatorColor = primaryColor,
                        unselectedIconColor = tertiaryTextColor,
                        unselectedTextColor = tertiaryTextColor
                    ),
                    modifier = Modifier.testTag("nav_tab_overlay")
                )

                // Tab 3: История
                val isTab3 = selectedTab == AppTab.HISTORY
                val tab3Color by animateColorAsState(
                    targetValue = if (isTab3) primaryColor else tertiaryTextColor,
                    animationSpec = tween(200),
                    label = "tab3_color"
                )
                NavigationBarItem(
                    selected = isTab3,
                    onClick = { viewModel.selectTab(AppTab.HISTORY) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "История",
                            tint = if (isTab3) onPrimaryColor else tab3Color
                        )
                    },
                    label = { Text("История", fontSize = 12.sp, color = tab3Color) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = onPrimaryColor,
                        selectedTextColor = primaryColor,
                        indicatorColor = primaryColor,
                        unselectedIconColor = tertiaryTextColor,
                        unselectedTextColor = tertiaryTextColor
                    ),
                    modifier = Modifier.testTag("nav_tab_history")
                )

                // Tab 4: Premium
                val isTab4 = selectedTab == AppTab.PRO
                val tab4Color by animateColorAsState(
                    targetValue = if (isTab4) primaryColor else tertiaryTextColor,
                    animationSpec = tween(200),
                    label = "tab4_color"
                )
                NavigationBarItem(
                    selected = isTab4,
                    onClick = { viewModel.selectTab(AppTab.PRO) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.WorkspacePremium,
                            contentDescription = "Premium",
                            tint = if (isTab4) onPrimaryColor else tab4Color
                        )
                    },
                    label = { Text("Premium", fontSize = 12.sp, color = tab4Color) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = onPrimaryColor,
                        selectedTextColor = primaryColor,
                        indicatorColor = primaryColor,
                        unselectedIconColor = tertiaryTextColor,
                        unselectedTextColor = tertiaryTextColor
                    ),
                    modifier = Modifier.testTag("nav_tab_pro")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val forward = targetState.ordinal >= initialState.ordinal
                    if (forward) {
                        (slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(tween(200)))
                    }
                },
                label = "screen_transition"
            ) { tab ->
                when (tab) {
                    AppTab.TRANSLATE -> HomeScreen(
                        viewModel = viewModel,
                        onOpenSettings = { showSettingsDialog = true },
                        onNavigateToHistory = { viewModel.selectTab(AppTab.HISTORY) }
                    )
                    AppTab.OVERLAY -> OverlayScreen(
                        viewModel = viewModel
                    )
                    AppTab.HISTORY -> HistoryScreen(
                        viewModel = viewModel
                    )
                    AppTab.PRO -> ProScreen(
                        viewModel = viewModel
                    )
                }
            }
        }

        if (showSettingsDialog) {
            SettingsDialog(
                currentUser = currentUser,
                isDarkTheme = isDarkTheme,
                onToggleTheme = { viewModel.toggleTheme() },
                onSignInWithGoogle = { viewModel.signInWithGoogle() },
                onSignOut = { viewModel.signOut() },
                onClearHistory = { viewModel.clearHistory() },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

