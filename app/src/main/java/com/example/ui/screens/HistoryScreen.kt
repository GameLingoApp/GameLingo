package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TranslationRecord
import com.example.ui.theme.MonoTextStyle
import com.example.ui.viewmodel.GameLingoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: GameLingoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val allTranslations by viewModel.allTranslations.collectAsState()
    val historySearchQuery by viewModel.historySearchQuery.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search field for translations
        OutlinedTextField(
            value = historySearchQuery,
            onValueChange = { viewModel.setHistorySearchQuery(it) },
            placeholder = {
                Text(
                    "Поиск по исходному тексту или переводу...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Поиск",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (allTranslations.isEmpty()) {
            // Empty state animation: Icon fade-in + scale 0.8 -> 1.0 (400ms), Text fade-in (300ms, delay 200ms)
            var showEmptyState by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                showEmptyState = true
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedVisibility(
                        visible = showEmptyState,
                        enter = fadeIn(tween(400, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.8f, animationSpec = tween(400, easing = FastOutSlowInEasing))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val textAlpha = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        delay(200)
                        textAlpha.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                    }

                    Text(
                        text = if (historySearchQuery.isNotBlank()) "Ничего не найдено" else "История переводов пуста",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alpha(textAlpha.value)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(allTranslations, key = { _, it -> it.id }) { index, record ->
                    StaggeredHistoryItem(delayMillis = (index * 40L).coerceAtMost(400L)) {
                        HistoryRecordCard(
                            record = record,
                            onToggleFavorite = { viewModel.toggleTranslationFavorite(record.id) },
                            onDelete = { viewModel.deleteTranslation(record.id) },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(record.translatedText))
                                Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StaggeredHistoryItem(
    delayMillis: Long,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(20f) }

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
fun HistoryRecordCard(
    record: TranslationRecord,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }
    val formattedTime = remember(record.timestamp) { dateFormat.format(Date(record.timestamp)) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(100),
        label = "history_card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            },
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
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = record.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (record.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Избранное",
                            tint = if (record.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Копировать",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = record.sourceText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = record.translatedText,
                style = MonoTextStyle.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
