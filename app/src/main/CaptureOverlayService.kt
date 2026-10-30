package com.example.dragontigerreader

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

class CaptureOverlayService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL = "card_reader_capture"
        private const val NOTIFICATION_ID = 71
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var windowManager: WindowManager? = null
    private var panel: LinearLayout? = null
    private var statusView: TextView? = null
    private var dragonView: TextView? = null
    private var tigerView: TextView? = null
    private var winnerView: TextView? = null
    private var historyView: TextView? = null
    private val processing = AtomicBoolean(false)
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val history = ArrayDeque<String>()
    private var lastSignature = ""
    private var lastReadAt = 0L
    private var screenWidth = 0
    private var screenHeight = 0
    private var density = 1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Dragon Tiger Reader")
            .setContentText("Screen capture is active while you use it")
            .setOngoing(true).build())

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (resultCode == Activity.RESULT_OK && data != null && projection == null) {
            startCapture(resultCode, data)
            showOverlay()
        }
        return START_NOT_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        val dm = resources.displayMetrics
        screenWidth = dm.widthPixels
        screenHeight = dm.heightPixels
        density = dm.densityDpi
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(resultCode, data)
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            if (processing.compareAndSet(false, true)) processImage(image) else image.close()
        }, android.os.Handler(mainLooper))
        virtualDisplay = projection?.createVirtualDisplay(
            "DragonTigerReader", screenWidth, screenHeight, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    private fun processImage(image: Image) {
        val plane = image.planes[0]
        val bitmap = android.graphics.Bitmap.createBitmap(
            image.width, image.height, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(plane.buffer)
        image.close()
        val input = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(input)
            .addOnSuccessListener { text ->
                val now = System.currentTimeMillis()
                if (now - lastReadAt < 650) return@addOnSuccessListener
                lastReadAt = now
                val cards = findCards(text, bitmap.width, bitmap.height)
                val d = cards.first
                val t = cards.second
                if (d != null) dragonView?.text = "🐉 DRAGON: ${d.label} (${d.value})"
                if (t != null) tigerView?.text = "🐅 TIGER: ${t.label} (${t.value})"
                if (d != null && t != null) {
                    val sig = "${d.label}|${t.label}"
                    val result = when {
                        d.value > t.value -> "DRAGON HIGHER"
                        t.value > d.value -> "TIGER HIGHER"
                        else -> "TIE"
                    }
                    winnerView?.text = "Result: $result"
                    statusView?.text = "● Cards detected — verify visually"
                    if (sig != lastSignature) {
                        lastSignature = sig
                        history.addFirst("${d.label} vs ${t.label}: $result")
                        while (history.size > 8) history.removeLast()
                        historyView?.text = "Recent reads\n" + history.joinToString("\n")
                    }
                } else {
                    statusView?.text = "● Scanning — place revealed cards in view"
                }
            }
            .addOnFailureListener { statusView?.text = "Recognition error — retrying" }
            .addOnCompleteListener {
                bitmap.recycle()
                processing.set(false)
            }
    }

    private data class Card(val label: String, val value: Int)

    // Heuristic OCR: looks for rank tokens near the upper card area and
    // assigns the left card to Dragon and right card to Tiger.
    // It is intentionally marked experimental; tune regions for the actual Phoenix layout.
    private fun findCards(text: com.google.mlkit.vision.text.Text, w: Int, h: Int): Pair<Card?, Card?> {
        val candidates = mutableListOf<Pair<Float, Card>>()
        for (block in text.textBlocks) for (line in block.lines) {
            val box = line.boundingBox ?: continue
            val cy = box.centerY().toFloat() / h
            val cx = box.centerX().toFloat() / w
            if (cy > 0.42f) continue
            val token = line.text.trim().uppercase()
                .replace("|", "I")
                .split(Regex("[^A-Z0-9]+")).firstOrNull() ?: continue
            val value = when (token) {
                "A" -> 14
                "K" -> 13
                "Q" -> 12
                "J" -> 11
                "10" -> 10
                "9" -> 9; "8" -> 8; "7" -> 7; "6" -> 6
                "5" -> 5; "4" -> 4; "3" -> 3; "2" -> 2
                else -> 0
            }
            if (value > 0) candidates.add(cx to Card(token, value))
        }
        val left = candidates.filter { it.first in 0.20f..0.49f }.maxByOrNull { it.second.value }?.second
        val right = candidates.filter { it.first in 0.50f..0.82f }.maxByOrNull { it.second.value }?.second
        return left to right
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            setBackgroundColor(0xEE111827.toInt())
        }
        fun label(text: String, size: Float = 14f) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(0xFFFFFFFF.toInt())
        }
        val header = label("🐉 DRAGON TIGER CARD READER     ×", 16f)
        statusView = label("● Waiting for screen capture…", 12f)
        dragonView = label("🐉 DRAGON: —", 16f)
        tigerView = label("🐅 TIGER: —", 16f)
        winnerView = label("Result: waiting", 17f)
        historyView = label("Recent reads\\n—", 12f)
        root.addView(header); root.addView(statusView); root.addView(dragonView)
        root.addView(tigerView); root.addView(winnerView); root.addView(historyView)
        header.setOnClickListener { stopSelf() }
        panel = root
        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 12; y = 120 }
        // Drag panel by touch.
        var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f
        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = event.rawX; touchY = event.rawY; true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX - (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    windowManager?.updateViewLayout(root, params); true
                }
                else -> false
            }
        }
        windowManager?.addView(root, params)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Card Reader Capture", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { imageReader?.close() } catch (_: Exception) {}
        try { projection?.stop() } catch (_: Exception) {}
        try { panel?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        recognizer.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
