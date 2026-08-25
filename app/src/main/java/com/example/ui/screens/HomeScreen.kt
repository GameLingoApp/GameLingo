package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Language
import com.example.data.model.TranslationRecord
import com.example.ui.components.GameLingoLogo
import com.example.ui.components.GoogleLogoIcon
import com.example.ui.components.LanguageSelectorSheet
import com.example.ui.theme.MonoTextStyle
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.GameLingoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: GameLingoViewModel,
    onOpenSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val sourceLang by viewModel.sourceLanguage.collectAsState()
    val targetLang by viewModel.targetLanguage.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val translationState by viewModel.translationState.collectAsState()
    val recentTranslations by viewModel.recentTranslations.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isProUnlocked by viewModel.isProUnlocked.collectAsState()

    var showSourceSheet by remember { mutableStateOf(false) }
    var showTargetSheet by remember { mutableStateOf(false) }
    var showGuestSignInDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isGuest = currentUser == null || currentUser!!.isGuest
    val scrollState = rememberScrollState()

    // 1. Logo fade-in (400ms)
    val logoAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
    }

    // Dynamic scroll bar background opacity
    val scrollProgress = (scrollState.value / 200f).coerceIn(0f, 1f)
    val topBarBgColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f + (scrollProgress * 0.15f))

    // 2. Language swap rotation state (180 deg, 200ms, Linear)
    var swapRotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedSwapRotation by animateFloatAsState(
        targetValue = swapRotationAngle,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "swap_rotation"
    )

    // 3. Text field focus state and animated border color (200ms)
    var isInputFocused by remember { mutableStateOf(false) }
    val inputBorderColor by animateColorAsState(
        targetValue = if (isInputFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "input_border_color"
    )

    // 4. Translate button scale on press (100ms)
    val translateInteractionSource = remember { MutableInteractionSource() }
    val isTranslatePressed by translateInteractionSource.collectIsPressedAsState()
    val translateScale by animateFloatAsState(
        targetValue = if (isTranslatePressed) 0.96f else 1.0f,
        animationSpec = tween(100),
        label = "translate_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarBgColor)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(logoAlpha.value)
            ) {
                GameLingoLogo(size = 28.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "GameLingo",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (isGuest) {
                    Text(
                        text = "Guest",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showGuestSignInDialog = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("guest_badge")
                    )
                } else if (isProUnlocked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF3B82F6))
                            .clickable { viewModel.selectTab(AppTab.PRO) }
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                            .testTag("pro_badge")
                    ) {
                        Text(
                            text = "Pro",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = "Free",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.selectTab(AppTab.PRO) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("free_badge")
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // User Avatar Account chip
                Surface(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("user_account_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentUser?.photoUrl != null) {
                            AsyncImage(
                                model = currentUser?.photoUrl,
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (currentUser?.displayName?.take(1) ?: "G").uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (currentUser != null && !currentUser!!.isGuest) {
                            Spacer(modifier = Modifier.width(5.dp))
                            GoogleLogoIcon(size = 12.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Настройки",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Guest Banner
        if (isGuest) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGuestSignInDialog = true }
                    .testTag("guest_banner"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        GoogleLogoIcon(size = 18.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Войдите через Google",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Синхронизация истории и безлимитный Pro",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Language Selector Card (Card with 20dp padding)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Source Language Field
                    LanguagePickerField(
                        label = "С какого языка",
                        language = sourceLang,
                        onClick = { showSourceSheet = true },
                        modifier = Modifier.weight(1f)
                    )

                    // Swap Button with 180° rotation animation (200ms, Linear)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                swapRotationAngle += 180f
                                viewModel.swapLanguages()
                            }
                            .testTag("swap_languages_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = "Поменять языки",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(animatedSwapRotation)
                        )
                    }

                    // Target Language Field
                    LanguagePickerField(
                        label = "На какой язык",
                        language = targetLang,
                        onClick = { showTargetSheet = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Translation input section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Быстрый перевод из игры",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row {
                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    viewModel.updateInputText(clip)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentPaste,
                                contentDescription = "Вставить",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.clearInput() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Очистить",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.updateInputText(it) },
                    placeholder = {
                        AnimatedVisibility(
                            visible = inputText.isEmpty(),
                            enter = fadeIn(tween(150)),
                            exit = fadeOut(tween(150))
                        ) {
                            Text(
                                text = "Введите или вставьте текст из игры...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .onFocusChanged { isInputFocused = it.isFocused }
                        .testTag("translate_input_field"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = inputBorderColor,
                        unfocusedBorderColor = inputBorderColor,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Translate Button with press scale (0.96) and loading fade-in
                Button(
                    onClick = { viewModel.translateCurrent() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = translateScale
                            scaleY = translateScale
                        }
                        .testTag("translate_button"),
                    interactionSource = translateInteractionSource,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = !translationState.isLoading && inputText.isNotBlank()
                ) {
                    if (translationState.isLoading) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Локализация контекста...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Перевести",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Translation Result Card (Fade in + slide-up 30dp -> 0dp for 300ms)
        AnimatedVisibility(
            visible = translationState.result != null || translationState.errorMessage != null,
            enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) + slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 2 }
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                if (translationState.result != null) {
                    val res = translationState.result!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "🌐 GameLingo Engine",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (res.notes != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = res.notes,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(res.translatedText))
                                            Toast.makeText(context, "Перевод скопирован", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = "Копировать",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = res.translatedText,
                                style = MonoTextStyle.copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("translation_result_text")
                            )
                        }
                    }
                } else if (translationState.errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = translationState.errorMessage ?: "Ошибка перевода",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Translations Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Недавние переводы",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onNavigateToHistory() }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Все",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (recentTranslations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Здесь появятся ваши недавние переводы из игр",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Staggered animated recent translation cards
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                itemsIndexed(recentTranslations, key = { _, it -> it.id }) { index, record ->
                    StaggeredItem(delayMillis = index * 50L) {
                        RecentTranslationCard(
                            record = record,
                            onClick = {
                                viewModel.updateInputText(record.sourceText)
                                viewModel.translateCurrent()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Language Selection Bottom Sheets
    if (showSourceSheet) {
        LanguageSelectorSheet(
            sheetState = sheetState,
            title = "Выберите исходный язык",
            selectedLanguage = sourceLang,
            onLanguageSelected = { viewModel.setSourceLanguage(it) },
            onDismiss = { showSourceSheet = false }
        )
    }

    if (showTargetSheet) {
        LanguageSelectorSheet(
            sheetState = sheetState,
            title = "Выберите язык перевода",
            selectedLanguage = targetLang,
            onLanguageSelected = { viewModel.setTargetLanguage(it) },
            onDismiss = { showTargetSheet = false }
        )
    }

    if (showGuestSignInDialog) {
        AlertDialog(
            onDismissRequest = { showGuestSignInDialog = false },
            title = {
                Text(
                    text = "Sign in with Google",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Sign in to sync your data and unlock Pro features",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGuestSignInDialog = false
                        viewModel.signInWithGoogle()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("dialog_signin_google_btn")
                ) {
                    GoogleLogoIcon(size = 14.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in with Google", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuestSignInDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun StaggeredItem(
    delayMillis: Long,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(16f) }

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
fun LanguagePickerField(
    label: String,
    language: Language,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Smooth Crossfade when language changes (200ms)
            AnimatedContent(
                targetState = language,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "lang_change",
                modifier = Modifier.weight(1f, fill = false)
            ) { targetLanguage ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = targetLanguage.flagEmoji,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = targetLanguage.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun RecentTranslationCard(
    record: TranslationRecord,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(record.timestamp) { dateFormat.format(Date(record.timestamp)) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = tween(100),
        label = "recent_card_scale"
    )

    Card(
        modifier = Modifier
            .width(220.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${record.sourceLangCode.uppercase()} → ${record.targetLangCode.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = record.sourceText.take(40) + if (record.sourceText.length > 40) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = record.translatedText.take(40) + if (record.translatedText.length > 40) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}
