package com.example.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.engine.GameTranslationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayWindow(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val translationEngine = GameTranslationEngine()
    private val ocrProcessor = ScreenOcrProcessor()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var rootView: View? = null
    var isShowing = false
        private set

    private var lastTranslatedText = ""

    fun show() {
        if (isShowing) {
            hide()
            return
        }

        val layoutParams = WindowManager.LayoutParams(
            (context.resources.displayMetrics.widthPixels * 0.90).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val view = createOverlayView(layoutParams)
        rootView = view

        try {
            windowManager.addView(view, layoutParams)
            isShowing = true
            captureScreenAndTranslate()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Не удалось открыть окно оверлея: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun hide() {
        rootView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        rootView = null
        isShowing = false
    }

    private var originalTv: TextView? = null
    private var translatedTv: TextView? = null
    private var progressBar: ProgressBar? = null
    private var statusTv: TextView? = null
    private var copyBtn: Button? = null

    private fun createOverlayView(params: WindowManager.LayoutParams): View {
        val dp = context.resources.displayMetrics.density

        // Main Card Container with Dark Gaming Theme (#0F172A)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#0F172A"))
                cornerRadius = 16 * dp
                setStroke((1.5f * dp).toInt(), Color.parseColor("#334155"))
            }
            background = bg
            setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
            elevation = 20 * dp
        }

        // Draggable Header Bar
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            weightSum = 1f
        }

        val title = TextView(context).apply {
            text = "🎮 GameLingo HUD • Экран"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding((8 * dp).toInt(), 0, (8 * dp).toInt(), 0)
            setOnClickListener { hide() }
        }

        header.addView(title)
        header.addView(closeBtn)

        // Make window draggable via header
        var startX = 0f
        var startY = 0f
        var initialX = 0
        var initialY = 0

        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    initialX = params.x
                    initialY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - startX).toInt()
                    params.y = initialY + (event.rawY - startY).toInt()
                    rootView?.let { windowManager.updateViewLayout(it, params) }
                    true
                }
                else -> false
            }
        }

        container.addView(header)

        // Divider
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * dp).toInt()
            ).apply {
                topMargin = (10 * dp).toInt()
                bottomMargin = (12 * dp).toInt()
            }
            setBackgroundColor(Color.parseColor("#334155"))
        }
        container.addView(divider)

        // Content ScrollView
        val scroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Status Label & Progress Bar
        val statusLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }
        val pb = ProgressBar(context).apply {
            layoutParams = LinearLayout.LayoutParams((18 * dp).toInt(), (18 * dp).toInt()).apply {
                rightMargin = (8 * dp).toInt()
            }
        }
        val stv = TextView(context).apply {
            text = "Сканирование экрана..."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
        }
        statusLayout.addView(pb)
        statusLayout.addView(stv)
        progressBar = pb
        statusTv = stv

        contentLayout.addView(statusLayout)

        // Original Text Card
        val origLabel = TextView(context).apply {
            text = "РАСПОЗНАННЫЙ ТЕКСТ:"
            setTextColor(Color.parseColor("#64748B"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (6 * dp).toInt()
                bottomMargin = (4 * dp).toInt()
            }
        }
        contentLayout.addView(origLabel)

        val origBox = TextView(context).apply {
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 8 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#334155"))
            }
            background = bg
            setPadding((10 * dp).toInt(), (8 * dp).toInt(), (10 * dp).toInt(), (8 * dp).toInt())
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 13f
            text = "Анализ игрового экрана..."
        }
        originalTv = origBox
        contentLayout.addView(origBox)

        // Translation Result Card
        val transLabel = TextView(context).apply {
            text = "ИГРОВОЙ ПЕРЕВОД (GAMELINGO HUD):"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (12 * dp).toInt()
                bottomMargin = (4 * dp).toInt()
            }
        }
        contentLayout.addView(transLabel)

        val transBox = TextView(context).apply {
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#0F2847"))
                cornerRadius = 8 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#2563EB"))
            }
            background = bg
            setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 14f
            typeface = Typeface.MONOSPACE
            text = "—"
        }
        translatedTv = transBox
        contentLayout.addView(transBox)

        scroll.addView(contentLayout)
        container.addView(scroll)

        // Action Buttons Row
        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (14 * dp).toInt()
            }
        }

        val scanScreenBtn = Button(context).apply {
            text = "📸 Экран"
            setTextColor(Color.WHITE)
            textSize = 11f
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#0284C7"))
                cornerRadius = 8 * dp
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(0, (40 * dp).toInt(), 1f).apply {
                rightMargin = (4 * dp).toInt()
            }
            setOnClickListener { captureScreenAndTranslate() }
        }

        val clipBtn = Button(context).apply {
            text = "📋 Буфер"
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 11f
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 8 * dp
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(0, (40 * dp).toInt(), 1f).apply {
                rightMargin = (4 * dp).toInt()
                leftMargin = (2 * dp).toInt()
            }
            setOnClickListener { readClipboardAndTranslate() }
        }

        val cBtn = Button(context).apply {
            text = "Копировать"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#2563EB"))
                cornerRadius = 8 * dp
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(0, (40 * dp).toInt(), 1f).apply {
                leftMargin = (2 * dp).toInt()
            }
            setOnClickListener {
                if (lastTranslatedText.isNotBlank()) {
                    val clip = ClipData.newPlainText("GameLingo", lastTranslatedText)
                    clipboardManager.setPrimaryClip(clip)
                    Toast.makeText(context, "Перевод скопирован!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        copyBtn = cBtn

        btnRow.addView(scanScreenBtn)
        btnRow.addView(clipBtn)
        btnRow.addView(cBtn)
        container.addView(btnRow)

        return container
    }

    fun captureScreenAndTranslate() {
        if (!ScreenCaptureManager.hasProjectionPermission()) {
            // If screen capture permission is not yet granted, fallback to clipboard with a clear hint
            readClipboardAndTranslate(hint = "Разрешение на захват экрана не активно. Запустите захват в приложении или используйте буфер обмена.")
            return
        }

        progressBar?.visibility = View.VISIBLE
        statusTv?.visibility = View.VISIBLE
        statusTv?.text = "Захват экрана игры..."
        originalTv?.text = "Анализ экрана через OCR..."
        translatedTv?.text = "Обработка..."

        serviceScope.launch {
            try {
                val bitmap = ScreenCaptureManager.captureScreen(context)
                if (bitmap == null) {
                    readClipboardAndTranslate(hint = "Не удалось сделать снимок экрана. Проверьте активность игры.")
                    return@launch
                }

                statusTv?.text = "Распознавание текста с экрана..."
                val recognizedText = ocrProcessor.recognizeText(bitmap, "auto")

                if (recognizedText.isBlank()) {
                    originalTv?.text = "(Текст на экране не обнаружен)"
                    translatedTv?.text = "Нажмите «Экран» когда диалог или меню игры появятся на экране."
                    progressBar?.visibility = View.GONE
                    statusTv?.visibility = View.GONE
                    return@launch
                }

                originalTv?.text = recognizedText
                statusTv?.text = "Перевод и контекстная адаптация..."

                val result = withContext(Dispatchers.IO) {
                    translationEngine.translate(
                        text = recognizedText,
                        sourceLang = "auto",
                        targetLang = "ru"
                    )
                }

                lastTranslatedText = result.translatedText
                translatedTv?.text = result.translatedText
                progressBar?.visibility = View.GONE
                statusTv?.visibility = View.GONE
            } catch (e: Exception) {
                translatedTv?.text = "Ошибка: ${e.localizedMessage}"
                progressBar?.visibility = View.GONE
                statusTv?.visibility = View.GONE
            }
        }
    }

    private fun readClipboardAndTranslate(hint: String? = null) {
        val clip = clipboardManager.primaryClip
        val text = if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).text?.toString()?.trim() ?: ""
        } else {
            ""
        }

        if (text.isBlank()) {
            originalTv?.text = "Буфер обмена пуст"
            translatedTv?.text = hint ?: "Скопируйте игровой текст или нажмите «📸 Экран» для прямого OCR-перевода с экрана игры."
            progressBar?.visibility = View.GONE
            statusTv?.visibility = View.GONE
            lastTranslatedText = ""
            return
        }

        originalTv?.text = text
        translatedTv?.text = "Переводим текст..."
        progressBar?.visibility = View.VISIBLE
        statusTv?.visibility = View.VISIBLE
        statusTv?.text = "Обработка текста..."

        serviceScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    translationEngine.translate(
                        text = text,
                        sourceLang = "auto",
                        targetLang = "ru"
                    )
                }

                lastTranslatedText = result.translatedText
                translatedTv?.text = result.translatedText
                progressBar?.visibility = View.GONE
                statusTv?.visibility = View.GONE
            } catch (e: Exception) {
                translatedTv?.text = "Ошибка перевода: ${e.localizedMessage}"
                progressBar?.visibility = View.GONE
                statusTv?.visibility = View.GONE
            }
        }
    }
}
