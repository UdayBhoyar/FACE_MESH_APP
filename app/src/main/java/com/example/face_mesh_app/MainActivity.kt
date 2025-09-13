package com.example.face_mesh_app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import android.content.Intent
import android.widget.ToggleButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.face_mesh_app.FaceLandmarkerHelper.LandmarkerListener
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

// Note: Explicitly importing the helper classes from the same package
// can help the IDE resolve references.
import com.google.mediapipe.tasks.vision.core.RunningMode

class MainActivity : AppCompatActivity(), LandmarkerListener {

    private lateinit var cameraPreviewView: PreviewView
    private lateinit var overlayView: OverlayView
    // Calibration UI removed
    private lateinit var btnPlus: Button
    private lateinit var btnMinus: Button
    private lateinit var toggleOverlay: ToggleButton
    private lateinit var tvSensitivity: TextView

    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private lateinit var backgroundExecutor: ExecutorService

    // --- CONFIG ---
    private val SMOOTH_ALPHA = 0.2f
    private var eyeSensitivity = 3.0f
    private val EYE_SENSITIVITY_MIN = 3.0f
    private val EYE_SENSITIVITY_MAX = 5.0f
    // Calibration removed: margin no longer used

    // --- STATE ---
    private var smoothedCursor = PointF()
    private val calibration = EyeCalibration()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraPreviewView = findViewById(R.id.camera_preview)
        overlayView = findViewById(R.id.overlay_view)
        btnPlus = findViewById(R.id.btn_plus)
        btnMinus = findViewById(R.id.btn_minus)
        toggleOverlay = findViewById(R.id.toggle_overlay)
        tvSensitivity = findViewById(R.id.tv_sensitivity)

        // Load saved sensitivity
        run {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            val saved = prefs.getFloat("sensitivity", eyeSensitivity)
            eyeSensitivity = min(EYE_SENSITIVITY_MAX, max(EYE_SENSITIVITY_MIN, saved))
        }

        // Initialize background executor for MediaPipe
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Create the FaceLandmarkerHelper
        faceLandmarkerHelper = FaceLandmarkerHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            landmarkerListener = this
        )

        // Request camera permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }

        setupUI()
    }

    private fun setupUI() {
        // Center the initial cursor when layout is ready
        window.decorView.post {
            val screenWidth = overlayView.width.toFloat()
            val screenHeight = overlayView.height.toFloat()
            smoothedCursor = PointF(screenWidth / 2, screenHeight / 2)
        }

        // Buttons only: adjust sensitivity
        btnPlus.setOnClickListener {
            eyeSensitivity = min(EYE_SENSITIVITY_MAX, eyeSensitivity + 0.1f)
            updateSensitivityLabel()
            saveSensitivity()
        }

        btnMinus.setOnClickListener {
            eyeSensitivity = max(EYE_SENSITIVITY_MIN, eyeSensitivity - 0.1f)
            updateSensitivityLabel()
            saveSensitivity()
        }

        updateSensitivityLabel()

        // Overlay toggle
        toggleOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) startOverlayWithPermission() else stopOverlayService()
        }
    }

    private fun startOverlayWithPermission() {
        if (android.provider.Settings.canDrawOverlays(this)) {
            ContextCompat.startForegroundService(this, Intent(this, GazeOverlayService::class.java))
        } else {
            Toast.makeText(this, "Enable 'Draw over other apps' to show overlay.", Toast.LENGTH_LONG).show()
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivityForResult(intent, 1001)
            toggleOverlay.isChecked = false
        }
    }

    private fun stopOverlayService() {
        stopService(Intent(this, GazeOverlayService::class.java))
    }

    override fun onResume() {
        super.onResume()
        // Stop overlay to avoid duplicate camera usage when app is foreground
        if (toggleOverlay.isChecked) stopOverlayService()
    }

    override fun onPause() {
        super.onPause()
        // If enabled, start overlay when app goes to background
        if (toggleOverlay.isChecked) startOverlayWithPermission()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && android.provider.Settings.canDrawOverlays(this)) {
            startOverlayWithPermission()
            toggleOverlay.isChecked = true
        }
    }

    // Calibration removed

    private fun updateSensitivityLabel() {
        tvSensitivity.text = "Sensitivity: ${"%.1f".format(eyeSensitivity)}"
    }

    private fun saveSensitivity() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        prefs.edit().putFloat("sensitivity", eyeSensitivity).apply()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalyzer.setAnalyzer(backgroundExecutor) { imageProxy ->
                // Convert ImageProxy plane to a Bitmap
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

                // First rotate to the sensor's upright orientation, then mirror for front camera
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
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("MainActivity", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED


    // This is the callback from FaceLandmarkerHelper where we get the results
    override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        val result = resultBundle.result
        val frameWidth = resultBundle.inputImageWidth
        val frameHeight = resultBundle.inputImageHeight

        // Get normalized gaze point [0,1]
        var rawGaze = EyeGazeCalculator.getPureEyeGaze(result, frameWidth, frameHeight) ?: return

        // --- Apply Sensitivity ---
        var ex = (rawGaze.x - 0.5f) * eyeSensitivity + 0.5f
        var ey = (rawGaze.y - 0.5f) * eyeSensitivity + 0.5f
        ex = max(0.0f, min(1.0f, ex))
        ey = max(0.0f, min(1.0f, ey))
        rawGaze = PointF(ex, ey)

        // --- Map to Screen ---
        val screenWidth = overlayView.width
        val screenHeight = overlayView.height
        if (screenWidth == 0 || screenHeight == 0) return

        val mappedPoint = calibration.map(rawGaze.x, rawGaze.y, screenWidth, screenHeight)

        // --- Apply Smoothing ---
        smoothedCursor.x = SMOOTH_ALPHA * mappedPoint.x + (1 - SMOOTH_ALPHA) * smoothedCursor.x
        smoothedCursor.y = SMOOTH_ALPHA * mappedPoint.y + (1 - SMOOTH_ALPHA) * smoothedCursor.y

        // --- Update UI on the main thread ---
        runOnUiThread {
            overlayView.setInputImageInfo(frameWidth, frameHeight)
            overlayView.setFaceLandmarkerResult(result)
            overlayView.updateCursor(smoothedCursor)
        }
    }

    override fun onError(error: String, errorCode: Int) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
        faceLandmarkerHelper.clearFaceLandmarker()
    }
}
