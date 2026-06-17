package com.examshuffler.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.os.Build
import androidx.core.app.NotificationCompat
import com.examshuffler.ExamShufflerApp
import com.examshuffler.database.AppDatabase
import com.examshuffler.database.QuestionEntity
import com.examshuffler.ocr.OcrProcessor
import com.examshuffler.parser.QuestionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FloatingIconService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, ExamShufflerApp.CHANNEL_FLOATING)
            .setContentTitle("题目乱序")
            .setContentText("悬浮图标已启动，点击截屏识别题目")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        if (mediaProjection == null && intent != null) {
            val resultCode = intent.getIntExtra("RESULT_CODE", -1)
            val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("RESULT_DATA", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("RESULT_DATA")
            }
            if (resultCode != -1 && data != null) {
                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = manager.getMediaProjection(resultCode, data)
            }
        }

        showFloatingIcon()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeFloatingIcon()
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
        super.onDestroy()
    }

    private fun showFloatingIcon() {
        if (floatingView != null) return

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_icon, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.sqrt((dx * dx + dy * dy).toDouble()) < 15.0) {
                        // It's a tap, not a drag
                        captureAndProcess()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(floatingView, params)
    }

    private fun removeFloatingIcon() {
        floatingView?.let { windowManager.removeView(it) }
        floatingView = null
    }

    private fun captureAndProcess() {
        val projection = mediaProjection ?: return

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        val reader = imageReader!!

        virtualDisplay = projection.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, null
        )

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val image = reader.acquireLatestImage()
            if (image != null) {
                val bitmap = imageToBitmap(image)
                image.close()
                if (bitmap != null) {
                    serviceScope.launch {
                        processBitmap(bitmap)
                    }
                }
            }
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
        }, 500)
    }

    private fun imageToBitmap(image: android.media.Image): Bitmap? {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * screenWidth

        val bitmap = Bitmap.createBitmap(
            screenWidth + rowPadding / pixelStride,
            screenHeight,
            Bitmap.Config.ARGB_8888
        )
        buffer.position(0)
        bitmap.copyPixelsFromBuffer(buffer)
        return Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
    }

    private suspend fun processBitmap(bitmap: Bitmap) {
        // Save screenshot
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(cacheDir, "captures")
        file.mkdirs()
        val imageFile = File(file, "capture_$dateStr.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }

        // OCR
        val ocrResult = OcrProcessor.process(this, bitmap)
        if (ocrResult.isNullOrBlank()) {
            showNotification("未识别到文字", "截图中没有找到文字内容")
            return
        }

        // Parse questions
        val parser = QuestionParser()
        val questions = parser.parse(ocrResult)

        if (questions.isEmpty()) {
            showNotification("未识别到题目", "截图中没有识别到试题格式，已保存原始文本")
            saveRawText(ocrResult, imageFile.absolutePath)
            return
        }

        // Save to database
        val db = AppDatabase.getInstance(this)
        val dao = db.questionDao()
        for (q in questions) {
            dao.insert(
                QuestionEntity(
                    content = q.content,
                    options = q.options,
                    answer = q.answer,
                    type = q.type,
                    sourcePage = q.sourcePage,
                    imagePath = q.imagePath,
                    rawOcrText = q.rawOcrText
                )
            )
        }

        showNotification(
            "识别到 ${questions.size} 道题",
            "已保存到题库，可在应用内练习"
        )
    }

    private fun saveRawText(text: String, imagePath: String) {
        val db = AppDatabase.getInstance(this)
        val dao = db.questionDao()
        serviceScope.launch {
            dao.insert(
                QuestionEntity(
                    content = text.take(200),
                    type = "未解析",
                    imagePath = imagePath,
                    rawOcrText = text
                )
            )
        }
    }

    private fun showNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(this, ExamShufflerApp.CHANNEL_CAPTURE)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
