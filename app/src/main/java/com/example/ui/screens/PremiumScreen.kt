package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.GameLingoViewModel
import com.example.ui.viewmodel.PremiumViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BrandBlue = Color(0xFF3B82F6)
private val DarkCardSurface = Color(0xFF1E293B)
private val GreenBadge = Color(0xFF16A34A)
private val TextSecondary = Color(0xFF94A3B8)
private val TextTertiary = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    viewModel: GameLingoViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val premiumViewModel = remember(viewModel) {
        PremiumViewModel(viewModel.paymentManager, viewModel.authManager)
    }

    val isPremium by premiumViewModel.isPremium.collectAsState()
    val isSignedIn by premiumViewModel.isSignedIn.collectAsState()
    val isLoading by premiumViewModel.isLoading.collectAsState()
    val paymentUrl by premiumViewModel.paymentUrl.collectAsState()
    val error by premiumViewModel.error.collectAsState()
    val showSuccess by premiumViewModel.showSuccess.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Pulsing animation for discount badge (1.0 <-> 1.05, 1500ms, infinite)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_badge")
    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "save_badge_scale"
    )

    // Button press scale states
    val monthlyInteractionSource = remember { MutableInteractionSource() }
    val isMonthlyPressed by monthlyInteractionSource.collectIsPressedAsState()
    val monthlyButtonScale by animateFloatAsState(
        targetValue = if (isMonthlyPressed) 0.97f else 1.0f,
        animationSpec = tween(100),
        label = "monthly_btn_scale"
    )

    val yearlyInteractionSource = remember { MutableInteractionSource() }
    val isYearlyPressed by yearlyInteractionSource.collectIsPressedAsState()
    val yearlyButtonScale by animateFloatAsState(
        targetValue = if (isYearlyPressed) 0.97f else 1.0f,
        animationSpec = tween(100),
        label = "yearly_btn_scale"
    )

    // Auto-launch Chrome Custom Tabs when payment URL is generated
    LaunchedEffect(paymentUrl) {
        if (paymentUrl != null) {
            premiumViewModel.openPayment(context)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            premiumViewModel.dismissError()
        }
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { premiumViewModel.dismissSuccess() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Verified,
                        contentDescription = "Success",
                        tint = BrandBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GameLingo Pro Active!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Спасибо за оформление подписки! Безлимитные переводы, оверлей и отсутствие рекламы успешно активированы.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        premiumViewModel.dismissSuccess()
                        viewModel.selectTab(AppTab.TRANSLATE)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Начать перевод", color = Color.White)
                }
            },
            containerColor = DarkCardSurface
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GameLingo Pro",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("premium_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Назад"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section with fade-in + scale (400ms)
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.9f, animationSpec = tween(400, easing = FastOutSlowInEasing))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unlock full power",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Unlimited translations, no ads, screen overlay",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 15.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = BrandBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Создание платежа в ЮKassa...",
                        fontSize = 13.sp,
                        color = BrandBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Карточка Free ---
            StaggeredPlanItem(delayMillis = 50L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardSurface)
                        .then(
                            if (!isPremium) Modifier.border(2.dp, BrandBlue, RoundedCornerShape(12.dp))
                            else Modifier.border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Free",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (!isPremium) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GreenBadge)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Current Plan",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        PlanFeatureItem(text = "10 translations per day", included = true)
                        PlanFeatureItem(text = "Game term detection", included = true)
                        PlanFeatureItem(text = "Online translation", included = true)
                        PlanFeatureItem(text = "Screen overlay", included = false)
                        PlanFeatureItem(text = "No ads", included = false)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Карточка Pro Monthly ---
            StaggeredPlanItem(delayMillis = 120L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardSurface)
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pro Monthly",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (isPremium) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BrandBlue)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Pro Active",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "150 ₽",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "/month",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        PlanFeatureItem(text = "Unlimited translations", included = true)
                        PlanFeatureItem(text = "Screen overlay", included = true)
                        PlanFeatureItem(text = "No ads", included = true)
                        PlanFeatureItem(text = "Priority support", included = true)

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (!isPremium) {
                                    premiumViewModel.subscribe("monthly")
                                } else {
                                    Toast.makeText(context, "Подписка уже активна!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = isSignedIn && !isLoading,
                            interactionSource = monthlyInteractionSource,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .graphicsLayer {
                                    scaleX = monthlyButtonScale
                                    scaleY = monthlyButtonScale
                                }
                                .testTag("subscribe_monthly_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandBlue,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF334155),
                                disabledContentColor = Color(0xFF64748B)
                            )
                        ) {
                            Text(
                                text = if (isPremium) "Subscribed (Monthly)" else "Subscribe — 150 ₽/month",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (!isSignedIn) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Sign in with Google to subscribe",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            TextButton(
                                onClick = { premiumViewModel.signInAndThen(context, "monthly") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signin_monthly_btn")
                            ) {
                                Text(
                                    text = "Sign in with Google",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandBlue
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Карточка Pro Yearly ---
            StaggeredPlanItem(delayMillis = 190L) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCardSurface)
                            .border(2.dp, BrandBlue, RoundedCornerShape(12.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pro Yearly",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "999 ₽",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "/year",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }

                            Text(
                                text = "83 ₽/month",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            PlanFeatureItem(text = "Everything in Monthly", included = true)
                            PlanFeatureItem(text = "Offline translation mode", included = true)
                            PlanFeatureItem(text = "Export history", included = true)

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (!isPremium) {
                                        premiumViewModel.subscribe("yearly")
                                    } else {
                                        Toast.makeText(context, "Подписка уже активна!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = isSignedIn && !isLoading,
                                interactionSource = yearlyInteractionSource,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .graphicsLayer {
                                        scaleX = yearlyButtonScale
                                        scaleY = yearlyButtonScale
                                    }
                                    .testTag("subscribe_yearly_btn"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandBlue,
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF334155),
                                    disabledContentColor = Color(0xFF64748B)
                                )
                            ) {
                                Text(
                                    text = if (isPremium) "Subscribed (Yearly)" else "Subscribe — 999 ₽/year",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (!isSignedIn) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Sign in with Google to subscribe",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                TextButton(
                                    onClick = { premiumViewModel.signInAndThen(context, "yearly") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signin_yearly_btn")
                                ) {
                                    Text(
                                        text = "Sign in with Google",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandBlue
                                    )
                                }
                            }
                        }
                    }

                    // Badge "Save 44%" anchored at top end with pulsing animation
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 16.dp)
                            .graphicsLayer {
                                scaleX = badgeScale
                                scaleY = badgeScale
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandBlue)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Save 44%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- Внизу ---
            Text(
                text = "Secure payment via YooMoney",
                fontSize = 12.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Supports cards, SBP, SberPay, Tinkoff Pay",
                fontSize = 12.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Cancel anytime",
                fontSize = 12.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StaggeredPlanItem(
    delayMillis: Long,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        delay(delayMillis)
        launch {
            alpha.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
        launch {
            offsetY.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(0, (offsetY.value * density).toInt()) }
            .alpha(alpha.value)
    ) {
        content()
    }
}

@Composable
private fun PlanFeatureItem(
    text: String,
    included: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (included) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Включено",
                tint = GreenBadge,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Не доступно",
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (included) Color.White else TextTertiary
        )
    }
}
