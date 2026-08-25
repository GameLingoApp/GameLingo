package com.example.overlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object ScreenCaptureManager {

    private const val TAG = "ScreenCaptureManager"
    private var mediaProjection: MediaProjection? = null
    var permissionResultCode: Int = Activity.RESULT_CANCELED
    var permissionData: Intent? = null

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var latestBitmap: Bitmap? = null

    fun setMediaProjectionData(resultCode: Int, data: Intent) {
        this.permissionResultCode = resultCode
        this.permissionData = data
    }

    fun hasProjectionPermission(): Boolean {
        return permissionResultCode == Activity.RESULT_OK && permissionData != null
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            Log.d(TAG, "MediaProjection stopped by system")
            cleanup()
        }
    }

    @Synchronized
    private fun ensureSession(context: Context): Boolean {
        if (mediaProjection != null && virtualDisplay != null && imageReader != null) {
            return true
        }

        val data = permissionData ?: return false
        if (permissionResultCode != Activity.RESULT_OK) return false

        try {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                ?: return false

            if (mediaProjection == null) {
                val proj = projectionManager.getMediaProjection(permissionResultCode, data) ?: return false
                proj.registerCallback(projectionCallback, mainHandler)
                mediaProjection = proj
            }

            val proj = mediaProjection ?: return false

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            if (width <= 0 || height <= 0) return false

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            reader.setOnImageAvailableListener({ ir ->
                val image = try { ir.acquireLatestImage() } catch (e: Exception) { null } ?: return@setOnImageAvailableListener
                try {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val bmp = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    bmp.copyPixelsFromBuffer(buffer)

                    val cropped = if (rowPadding == 0) {
                        bmp
                    } else {
                        Bitmap.createBitmap(bmp, 0, 0, width, height)
                    }

                    latestBitmap = cropped
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing screen image: ${e.message}")
                } finally {
                    image.close()
                }
            }, mainHandler)

            imageReader = reader

            val vd = try {
                proj.createVirtualDisplay(
                    "GameLingoScreenCapture",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface,
                    null,
                    mainHandler
                )
            } catch (e: Exception) {
                Log.e(TAG, "VirtualDisplay creation failed: ${e.message}")
                null
            }

            if (vd == null) {
                return false
            }

            virtualDisplay = vd
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaProjection session: ${e.message}", e)
            cleanup()
            return false
        }
    }

    suspend fun captureScreen(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        val ready = ensureSession(context)
        if (!ready) return@withContext null

        // Wait a brief moment if we don't have a frame yet
        var attempts = 0
        while (latestBitmap == null && attempts < 15) {
            delay(50)
            attempts++
        }

        return@withContext latestBitmap
    }

    private fun cleanup() {
        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing virtual display: ${e.message}")
        }
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing image reader: ${e.message}")
        }
        imageReader = null

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media projection: ${e.message}")
        }
        mediaProjection = null
        latestBitmap = null
    }

    fun release() {
        cleanup()
        permissionData = null
        permissionResultCode = Activity.RESULT_CANCELED
    }
}
