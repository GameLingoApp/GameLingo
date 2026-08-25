package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.data.remote.PaymentManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class OverlayService : Service() {

    companion object {
        const val ACTION_START_OVERLAY = "com.example.overlay.ACTION_START"
        const val ACTION_STOP_OVERLAY = "com.example.overlay.ACTION_STOP"
        private const val CHANNEL_ID = "gamelingo_overlay_channel"
        private const val NOTIFICATION_ID = 7771

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_START_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP_OVERLAY
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var floatingBubbleView: View? = null
    private var overlayWindow: OverlayWindow? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val paymentManager = PaymentManager(context = this)
        if (!paymentManager.getCachedPremiumStatus()) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayWindow = OverlayWindow(this)

        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    buildForegroundNotification(),
                    fgsType
                )
            } else {
                startForeground(NOTIFICATION_ID, buildForegroundNotification())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                startForeground(NOTIFICATION_ID, buildForegroundNotification())
            } catch (ignored: Exception) {
            }
        }
        _isServiceRunning.value = true

        setupFloatingBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_OVERLAY) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun setupFloatingBubble() {
        val dp = resources.displayMetrics.density
        val sizePx = (54 * dp).toInt()

        val wmLayoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - sizePx - 16 * dp).toInt()
            y = (resources.displayMetrics.heightPixels * 0.35f).toInt()
        }

        val bubble = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#2563EB"))
                setStroke((2 * dp).toInt(), Color.parseColor("#38BDF8"))
            }
            background = bg
            elevation = 16 * dp
        }

        val iconText = TextView(this).apply {
            text = "GL"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        bubble.addView(iconText)

        // Dragging & Click Handling
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        val touchSlop = 10 * dp

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = wmLayoutParams.x
                    initialY = wmLayoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    wmLayoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    wmLayoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    floatingBubbleView?.let { windowManager?.updateViewLayout(it, wmLayoutParams) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = abs(event.rawX - initialTouchX)
                    val diffY = abs(event.rawY - initialTouchY)
                    if (diffX < touchSlop && diffY < touchSlop) {
                        // Click event: toggle translation overlay
                        overlayWindow?.show()
                    }
                    true
                }
                else -> false
            }
        }

        floatingBubbleView = bubble

        try {
            windowManager?.addView(bubble, wmLayoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GameLingo Floating Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating HUD translation bubble for mobile games"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP_OVERLAY
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GameLingo is active")
            .setContentText("Tap floating bubble in game to translate clipboard")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        floatingBubbleView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        floatingBubbleView = null
        overlayWindow?.hide()
        overlayWindow = null
        ScreenCaptureManager.release()
    }
}
