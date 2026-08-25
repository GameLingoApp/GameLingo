package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GameLingoLogo
import com.example.ui.components.GoogleSignInButton
import com.example.ui.theme.BlueAccentDark
import com.example.ui.theme.SlateBgDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.GameLingoViewModel

@Composable
fun OnboardingScreen(
    viewModel: GameLingoViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSigningIn by viewModel.isSigningIn.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Logo & Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    BlueAccentDark.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    GameLingoLogo(
                        size = 64.dp,
                        primaryColor = TextPrimaryDark,
                        accentColor = BlueAccentDark
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "GameLingo",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Игровой переводчик с умным экранным HUD",
                    fontSize = 14.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Feature Highlights
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureHighlightRow(
                        icon = Icons.Outlined.Translate,
                        title = "Мгновенный перевод игр",
                        description = "Диалоги, квесты и сюжет на русском в реальном времени"
                    )

                    FeatureHighlightRow(
                        icon = Icons.Outlined.SportsEsports,
                        title = "Экранный OCR-перевод",
                        description = "Перевод диалогов и меню прямо поверх игры"
                    )

                    FeatureHighlightRow(
                        icon = Icons.Outlined.Shield,
                        title = "Вход без паролей и форм",
                        description = "Безопасная авторизация через аккаунт Google в 1 клик"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Section: Sign In Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Main Google Sign-In Button
                GoogleSignInButton(
                    onClick = {
                        viewModel.signInWithGoogle(
                            onSuccess = { onComplete() }
                        )
                    },
                    isLoading = isSigningIn,
                    text = "Continue with Google",
                    modifier = Modifier.testTag("onboarding_google_sign_in_btn")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Continue as guest
                TextButton(
                    onClick = {
                        viewModel.continueAsGuest(onSuccess = { onComplete() })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("onboarding_guest_btn")
                ) {
                    Text(
                        text = "Продолжить как гость",
                        color = TextSecondaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Privacy footnote
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = TextSecondaryDark.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Получаем только имя, email и аватар. Никаких паролей.",
                        fontSize = 11.sp,
                        color = TextSecondaryDark.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.75f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BlueAccentDark.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BlueAccentDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
