package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overlay.OverlayService
import com.example.overlay.ScreenCaptureManager
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.GameLingoViewModel

@Composable
fun OverlayScreen(
    viewModel: GameLingoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val overlaySettings by viewModel.overlaySettings.collectAsState()
    val isServiceRunning by OverlayService.isServiceRunning.collectAsState()
    val isProUnlocked by viewModel.isProUnlocked.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isSignedIn = currentUser != null && !currentUser!!.isGuest
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasCapturePermission by remember { mutableStateOf(ScreenCaptureManager.hasProjectionPermission()) }

    // Launcher for MediaProjection screen capture permission
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureManager.setMediaProjectionData(result.resultCode, result.data!!)
            hasCapturePermission = true
            OverlayService.start(context)
            viewModel.updateOverlaySettings { it.copy(isOverlayEnabled = true) }
        }
    }

    // Launcher for overlay permission settings
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        if (hasOverlayPermission) {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (projectionManager != null && !ScreenCaptureManager.hasProjectionPermission()) {
                mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
            } else {
                OverlayService.start(context)
                viewModel.updateOverlaySettings { it.copy(isOverlayEnabled = true) }
            }
        }
    }

    LaunchedEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasCapturePermission = ScreenCaptureManager.hasProjectionPermission()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.PictureInPicture,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Игровой Оверлей (HUD)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Плавающий переводчик поверх любой мобильной игры с экрана (OCR)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = !isProUnlocked,
            enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(200))
        ) {
            // Pro Feature Lock Card for non-premium users (Color #0F172A, 85% opacity)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("pro_feature_lock_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xD90F172A)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Pro Feature",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Функция GameLingo Pro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (!isSignedIn) "Sign in and upgrade to Pro to use screen translation"
                        else "Upgrade to Pro to use screen translation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { viewModel.selectTab(AppTab.PRO) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("unlock_pro_overlay_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (!isSignedIn) Icons.Outlined.AccountCircle else Icons.Outlined.WorkspacePremium,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!isSignedIn) "Sign in with Google" else "Upgrade to Pro",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Overlay Permission Alert Banner (if Pro and not granted)
        if (isProUnlocked && !hasOverlayPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Требуется разрешение на отображение поверх окон",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Чтобы плавающая кнопка перевода отображалась поверх игр, включите разрешение в настройках системы.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            overlayPermissionLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("grant_overlay_permission_button")
                    ) {
                        Text("Предоставить разрешение", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Screen Capture Permission Banner (if Pro and overlay is granted but capture is not)
        if (isProUnlocked && hasOverlayPermission && !hasCapturePermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Активация захвата экрана (OCR)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Разрешите захват экрана, чтобы кнопка в игре мгновенно распознавала текст с экрана и переводила его.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                            if (projectionManager != null) {
                                mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("grant_screen_capture_button")
                    ) {
                        Text("Разрешить захват экрана", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Activation Card
        val isActive = isServiceRunning || overlaySettings.isOverlayEnabled
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isActive)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isActive) "Оверлей активен" else "Оверлей выключен",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isActive) "Плавающая кнопка распознавания экрана активна" else "Включите для быстрого перевода экрана прямо в игре",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isActive,
                        enabled = isProUnlocked,
                        onCheckedChange = { isEnabled ->
                            if (!isProUnlocked) {
                                viewModel.selectTab(AppTab.PRO)
                                return@Switch
                            }
                            if (isEnabled) {
                                if (!Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    overlayPermissionLauncher.launch(intent)
                                } else if (!ScreenCaptureManager.hasProjectionPermission()) {
                                    val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                                    if (projectionManager != null) {
                                        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                                    } else {
                                        OverlayService.start(context)
                                        viewModel.updateOverlaySettings { it.copy(isOverlayEnabled = true) }
                                    }
                                } else {
                                    OverlayService.start(context)
                                    viewModel.updateOverlaySettings { it.copy(isOverlayEnabled = true) }
                                }
                            } else {
                                OverlayService.stop(context)
                                viewModel.updateOverlaySettings { it.copy(isOverlayEnabled = false) }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("overlay_toggle_switch")
                    )
                }

                if (isActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            OverlayService.stop(context)
                            viewModel.updateOverlaySettings { it.copy(isOverlayEnabled = false) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Остановить службу оверлея")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Overlay Settings Card
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
                Text(
                    text = "Параметры оверлея экрана",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Opacity Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Прозрачность HUD:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(overlaySettings.opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = overlaySettings.opacity,
                    enabled = isProUnlocked,
                    onValueChange = { newOpacity ->
                        viewModel.updateOverlaySettings { it.copy(opacity = newOpacity) }
                    },
                    valueRange = 0.4f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
