package com.example.face_mesh_app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.Executors

class GazeOverlayService : LifecycleService(), FaceLandmarkerHelper.LandmarkerListener {
    companion object {
        const val ACTION_STOP = "com.example.face_mesh_app.ACTION_STOP"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: OverlayView
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper

    override fun onCreate() {
        super.onCreate()
        startAsForeground()

        windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = OverlayView(this, null)
        overlayView.setShowLandmarks(false)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        windowManager.addView(overlayView, params)

        faceLandmarkerHelper = FaceLandmarkerHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            landmarkerListener = this
        )

        startCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { windowManager.removeView(overlayView) } catch (_: Exception) {}
        executor.shutdown()
        faceLandmarkerHelper.clearFaceLandmarker()
    }

    private fun startAsForeground() {
        val channelId = "gaze_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Gaze Overlay", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        val stopIntent = Intent(this, GazeOverlayService::class.java).apply { action = ACTION_STOP }
        val stopPending = android.app.PendingIntent.getService(
            this,
            0,
            stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notif: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Gaze cursor running")
            .setContentText("Tracking eyes in the background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .addAction(0, "Stop", stopPending)
            .build()
        startForeground(1, notif)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val analyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            analyzer.setAnalyzer(executor) { imageProxy ->
                val plane = imageProxy.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * imageProxy.width
                val baseBitmap = Bitmap.createBitmap(
                    imageProxy.width + rowPadding / pixelStride,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
                baseBitmap.copyPixelsFromBuffer(buffer)

                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val matrix = android.graphics.Matrix().apply {
                    postRotate(rotationDegrees.toFloat(), baseBitmap.width / 2f, baseBitmap.height / 2f)
                    postScale(-1f, 1f, baseBitmap.width / 2f, baseBitmap.height / 2f)
                }
                val transformed = Bitmap.createBitmap(
                    baseBitmap, 0, 0, baseBitmap.width, baseBitmap.height, matrix, true
                )

                faceLandmarkerHelper.detectLiveStream(transformed, System.currentTimeMillis())
                imageProxy.close()
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(/* lifecycleOwner= */this, cameraSelector, analyzer)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(applicationContext))
    }

    override fun onError(error: String, errorCode: Int) { }

    private val smoothAlpha = 0.2f
    private var smoothedX = 0f
    private var smoothedY = 0f
    private var eyeSensitivity: Float = 3.0f

    override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        val result = resultBundle.result
        val frameWidth = resultBundle.inputImageWidth
        val frameHeight = resultBundle.inputImageHeight

        // Load latest sensitivity from shared prefs
        run {
            val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
            eyeSensitivity = prefs.getFloat("sensitivity", eyeSensitivity)
        }

        var gaze = EyeGazeCalculator.getPureEyeGaze(result, frameWidth, frameHeight) ?: return
        // Apply same sensitivity shaping as in app: around center
        var ex = (gaze.x - 0.5f) * eyeSensitivity + 0.5f
        var ey = (gaze.y - 0.5f) * eyeSensitivity + 0.5f
        ex = ex.coerceIn(0f, 1f)
        ey = ey.coerceIn(0f, 1f)
        gaze = android.graphics.PointF(ex, ey)

        // Map directly to overlay size
        val viewW = overlayView.width
        val viewH = overlayView.height
        if (viewW == 0 || viewH == 0) return
        val mappedX = gaze.x * viewW
        val mappedY = gaze.y * viewH

        smoothedX = smoothAlpha * mappedX + (1 - smoothAlpha) * smoothedX
        smoothedY = smoothAlpha * mappedY + (1 - smoothAlpha) * smoothedY

        // Simple mouth-open detection via lip distance
        val isMouthOpen = try {
            val landmarks = result.faceLandmarks().firstOrNull()
            if (landmarks != null) {
                val topLip = landmarks[13] // MediaPipe landmark approx top inner lip
                val bottomLip = landmarks[14] // approx bottom inner lip
                val dy = Math.abs(topLip.y() - bottomLip.y())
                dy > 0.02f // heuristic threshold
            } else false
        } catch (_: Exception) { false }

        overlayView.post {
            overlayView.setInputImageInfo(frameWidth, frameHeight)
            overlayView.setFaceLandmarkerResult(result)
            overlayView.updateCursor(android.graphics.PointF(smoothedX, smoothedY))
            overlayView.setCursorColor(isMouthOpen)
        }

        // Trigger tap once when mouth transitions from closed->open
        if (isMouthOpen && !mouthWasOpen) {
            mouthWasOpen = true
            // Note: Accessibility service removed - tap functionality disabled
            // GazeAccessibilityService.instance?.performTap(smoothedX, smoothedY)
        } else if (!isMouthOpen) {
            mouthWasOpen = false
        }
    }

    private var mouthWasOpen = false
}


