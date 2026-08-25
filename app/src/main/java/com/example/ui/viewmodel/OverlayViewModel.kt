package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import com.example.overlay.OverlayService
import kotlinx.coroutines.flow.StateFlow

class OverlayViewModel(application: Application) : AndroidViewModel(application) {

    val isOverlayRunning: StateFlow<Boolean> = OverlayService.isServiceRunning

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun startOverlay(context: Context) {
        if (hasOverlayPermission(context)) {
            OverlayService.start(context)
        }
    }

    fun stopOverlay(context: Context) {
        OverlayService.stop(context)
    }

    fun toggleOverlay(context: Context): Boolean {
        if (!hasOverlayPermission(context)) {
            return false
        }
        if (isOverlayRunning.value) {
            stopOverlay(context)
        } else {
            startOverlay(context)
        }
        return true
    }
}
