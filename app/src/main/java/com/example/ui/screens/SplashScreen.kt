package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GameLingoLogo
import com.example.ui.theme.BlueAccentDark
import com.example.ui.theme.SlateBgDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // 1. Logo: fade-in 0 -> 1 over 600ms
    val logoAlpha = remember { Animatable(0f) }

    // 2. Title: fade-in + slide-up (20dp -> 0dp) over 500ms, delay 300ms
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(20f) }

    // 3. Subtitle: fade-in + slide-up (20dp -> 0dp) over 500ms, delay 500ms
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffset = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        // Launch logo animation immediately (600ms)
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }

        // Launch title animation with 300ms delay
        launch {
            delay(300)
            launch {
                titleAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                )
            }
            launch {
                titleOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                )
            }
        }

        // Launch subtitle animation with 500ms delay
        launch {
            delay(500)
            launch {
                subtitleAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                )
            }
            launch {
                subtitleOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                )
            }
        }

        delay(1600) // Hold briefly so all animations complete elegantly
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBgDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Centered GameLingo logo (Fade in 600ms)
            Box(modifier = Modifier.alpha(logoAlpha.value)) {
                GameLingoLogo(
                    size = 76.dp,
                    primaryColor = TextPrimaryDark,
                    accentColor = BlueAccentDark
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App Name (Fade in + slide-up 20dp -> 0dp)
            Text(
                text = "GameLingo",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                letterSpacing = (-0.5).sp,
                modifier = Modifier
                    .offset { IntOffset(0, (titleOffset.value * density).toInt()) }
                    .alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle (Fade in + slide-up 20dp -> 0dp)
            Text(
                text = "Game Text Translator",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondaryDark,
                letterSpacing = 0.2.sp,
                modifier = Modifier
                    .offset { IntOffset(0, (subtitleOffset.value * density).toInt()) }
                    .alpha(subtitleAlpha.value)
            )
        }
    }
}

