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
import android.widget.LinearLayout
import android.view.View
import android.provider.Settings
import android.content.ComponentName
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
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FACE_LANDMARKS_LEFT_EYE
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FACE_LANDMARKS_RIGHT_EYE

class MainActivity : AppCompatActivity(), LandmarkerListener {

    private lateinit var cameraPreviewView: PreviewView
    private lateinit var overlayView: OverlayView
    // Calibration UI removed
    private lateinit var btnPlus: Button
    private lateinit var btnMinus: Button
    private lateinit var btnCalibrate: Button
    private lateinit var toggleOverlay: ToggleButton
    private lateinit var toggleVoiceCommands: ToggleButton
    private lateinit var tvSensitivity: TextView
    
    // Calibration UI
    private lateinit var calibrationContainer: LinearLayout
    private lateinit var tvCalibrationInstruction: TextView
    private lateinit var btnCapture: Button
    private lateinit var btnCalibrationDone: Button
    private lateinit var tvCalibrationProgress: TextView

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
    private var mouthWasOpen = false
    
    // Calibration state
    private var isCalibrating = false
    private var calibrationDots = mutableListOf<PointF>()
    private var currentDotIndex = 0
    private var calibrationData = mutableListOf<Pair<PointF, PointF>>() // (gaze, target)
    
    // Head movement detection for swipes
    private var lastHeadPosition = PointF(0f, 0f)
    private var headMovementThreshold = 0.05f // Minimum movement to trigger swipe
    private var swipeCooldown = 0L
    private val swipeCooldownDuration = 500L // 500ms between swipes
    private var eyesClosed = false
    private var lastEyesClosedTime = 0L
    private val eyesClosedThreshold = 300L // Must keep eyes closed for 300ms before swipes work

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraPreviewView = findViewById(R.id.camera_preview)
        overlayView = findViewById(R.id.overlay_view)
        btnPlus = findViewById(R.id.btn_plus)
        btnMinus = findViewById(R.id.btn_minus)
        btnCalibrate = findViewById(R.id.btn_calibrate)
        toggleOverlay = findViewById(R.id.toggle_overlay)
        toggleVoiceCommands = findViewById(R.id.toggle_voice_commands)
        tvSensitivity = findViewById(R.id.tv_sensitivity)
        
        // Calibration UI
        calibrationContainer = findViewById(R.id.calibration_container)
        tvCalibrationInstruction = findViewById(R.id.tv_calibration_instruction)
        btnCapture = findViewById(R.id.btn_capture)
        btnCalibrationDone = findViewById(R.id.btn_calibration_done)
        tvCalibrationProgress = findViewById(R.id.tv_calibration_progress)

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
            // Initialize head position to center
            lastHeadPosition = PointF(0.5f, 0.5f)
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

        // Calibration button
        btnCalibrate.setOnClickListener {
            startCalibration()
        }

        // Calibration buttons
        btnCapture.setOnClickListener {
            captureCalibrationPoint()
        }

        btnCalibrationDone.setOnClickListener {
            finishCalibration()
        }

        // Overlay toggle
        toggleOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (isAccessibilityServiceEnabled()) {
                    startOverlayWithPermission()
                } else {
                    Toast.makeText(this, "Please enable accessibility service first", Toast.LENGTH_LONG).show()
                    requestAccessibilityPermission()
                    toggleOverlay.isChecked = false
                }
            } else {
                stopOverlayService()
            }
        }

        // Voice Commands toggle
        toggleVoiceCommands.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (checkMicrophonePermission()) {
                    startVoiceCommandService()
                    // Show help on first use
                    val prefs = getSharedPreferences("settings", MODE_PRIVATE)
                    if (!prefs.getBoolean("voice_help_shown", false)) {
                        VoiceCommandHelper.showVoiceCommandsHelp(this)
                        prefs.edit().putBoolean("voice_help_shown", true).apply()
                    }
                } else {
                    requestMicrophonePermission()
                    toggleVoiceCommands.isChecked = false
                }
            } else {
                stopVoiceCommandService()
            }
        }
        
        // Long press on voice toggle to show help
        toggleVoiceCommands.setOnLongClickListener {
            VoiceCommandHelper.showVoiceCommandsHelp(this)
            true
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

    private fun checkMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestMicrophonePermission() {
        ActivityCompat.requestPermissions(
            this, 
            arrayOf(Manifest.permission.RECORD_AUDIO), 
            102
        )
    }

    private fun startVoiceCommandService() {
        val intent = Intent(this, VoiceCommandService::class.java)
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "Voice Commands Enabled", Toast.LENGTH_SHORT).show()
    }

    private fun stopVoiceCommandService() {
        stopService(Intent(this, VoiceCommandService::class.java))
        Toast.makeText(this, "Voice Commands Disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // When app comes to foreground, pause overlay camera to avoid conflicts
        if (toggleOverlay.isChecked) {
            val intent = Intent(this, GazeOverlayService::class.java)
            intent.action = GazeOverlayService.ACTION_PAUSE_CAMERA
            startService(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        // When app goes to background, resume overlay camera for background tracking
        if (toggleOverlay.isChecked) {
            val intent = Intent(this, GazeOverlayService::class.java)
            intent.action = GazeOverlayService.ACTION_RESUME_CAMERA
            startService(intent)
        }
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

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = ComponentName(this, GazeAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(serviceName.flattenToString()) == true
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun startCalibration() {
        isCalibrating = true
        calibrationData.clear()
        currentDotIndex = 0
        
        // Create 4x4 grid of calibration dots
        calibrationDots.clear()
        val margin = 100f
        val screenWidth = overlayView.width.toFloat()
        val screenHeight = overlayView.height.toFloat()
        val stepX = (screenWidth - 2 * margin) / 3f
        val stepY = (screenHeight - 2 * margin) / 3f
        
        for (row in 0..3) {
            for (col in 0..3) {
                val x = margin + col * stepX
                val y = margin + row * stepY
                calibrationDots.add(PointF(x, y))
            }
        }
        
        overlayView.setCalibrationDots(calibrationDots)
        overlayView.setShowCalibrationDots(true)
        calibrationContainer.visibility = View.VISIBLE
        
        updateCalibrationUI()
    }

    private fun updateCalibrationUI() {
        if (currentDotIndex < calibrationDots.size) {
            overlayView.setActiveDotIndex(currentDotIndex)
            tvCalibrationInstruction.text = "Look at the red dot and click Capture"
            tvCalibrationProgress.text = "Progress: ${currentDotIndex}/${calibrationDots.size}"
        } else {
            tvCalibrationInstruction.text = "Calibration complete! Click Done to finish."
            tvCalibrationProgress.text = "Progress: ${calibrationDots.size}/${calibrationDots.size}"
        }
    }

    private fun captureCalibrationPoint() {
        if (currentDotIndex < calibrationDots.size) {
            // Get current gaze position
            val currentGaze = smoothedCursor
            val targetDot = calibrationDots[currentDotIndex]
            
            // Store calibration data
            calibrationData.add(Pair(currentGaze, targetDot))
            
            currentDotIndex++
            updateCalibrationUI()
        }
    }

    private fun finishCalibration() {
        isCalibrating = false
        overlayView.setShowCalibrationDots(false)
        calibrationContainer.visibility = View.GONE
        
        // Process calibration data
        if (calibrationData.size >= 4) {
            processCalibrationData()
            Toast.makeText(this, "Calibration completed!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Calibration incomplete. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processCalibrationData() {
        // Simple calibration: create a mapping from gaze to screen coordinates
        // This is a basic implementation - you could use more sophisticated methods
        val minGazeX = calibrationData.minOf { it.first.x }
        val maxGazeX = calibrationData.maxOf { it.first.x }
        val minGazeY = calibrationData.minOf { it.first.y }
        val maxGazeY = calibrationData.maxOf { it.first.y }
        
        val minScreenX = calibrationData.minOf { it.second.x }
        val maxScreenX = calibrationData.maxOf { it.second.x }
        val minScreenY = calibrationData.minOf { it.second.y }
        val maxScreenY = calibrationData.maxOf { it.second.y }
        
        // Store calibration parameters
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("minGazeX", minGazeX)
            putFloat("maxGazeX", maxGazeX)
            putFloat("minGazeY", minGazeY)
            putFloat("maxGazeY", maxGazeY)
            putFloat("minScreenX", minScreenX)
            putFloat("maxScreenX", maxScreenX)
            putFloat("minScreenY", minScreenY)
            putFloat("maxScreenY", maxScreenY)
            putBoolean("isCalibrated", true)
            apply()
        }
    }

    private fun isCalibrated(): Boolean {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        return prefs.getBoolean("isCalibrated", false)
    }

    private fun mapGazeToScreen(gaze: PointF, screenWidth: Int, screenHeight: Int): PointF {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val minGazeX = prefs.getFloat("minGazeX", 0f)
        val maxGazeX = prefs.getFloat("maxGazeX", 1f)
        val minGazeY = prefs.getFloat("minGazeY", 0f)
        val maxGazeY = prefs.getFloat("maxGazeY", 1f)
        val minScreenX = prefs.getFloat("minScreenX", 0f)
        val maxScreenX = prefs.getFloat("maxScreenX", screenWidth.toFloat())
        val minScreenY = prefs.getFloat("minScreenY", 0f)
        val maxScreenY = prefs.getFloat("maxScreenY", screenHeight.toFloat())
        
        // Map gaze coordinates to screen coordinates using calibration data
        val normalizedX = (gaze.x - minGazeX) / (maxGazeX - minGazeX)
        val normalizedY = (gaze.y - minGazeY) / (maxGazeY - minGazeY)
        
        val screenX = minScreenX + normalizedX * (maxScreenX - minScreenX)
        val screenY = minScreenY + normalizedY * (maxScreenY - minScreenY)
        
        return PointF(screenX.coerceIn(0f, screenWidth.toFloat()), 
                     screenY.coerceIn(0f, screenHeight.toFloat()))
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
        when (requestCode) {
            101 -> {
                if (allPermissionsGranted()) {
                    startCamera()
                } else {
                    Toast.makeText(this, "Camera permission not granted.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            102 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startVoiceCommandService()
                    toggleVoiceCommands.isChecked = true
                } else {
                    Toast.makeText(this, "Microphone permission required for voice commands", Toast.LENGTH_SHORT).show()
                }
            }
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

        val mappedPoint = if (isCalibrated()) {
            mapGazeToScreen(rawGaze, screenWidth, screenHeight)
        } else {
            calibration.map(rawGaze.x, rawGaze.y, screenWidth, screenHeight)
        }

        // --- Apply Smoothing ---
        smoothedCursor.x = SMOOTH_ALPHA * mappedPoint.x + (1 - SMOOTH_ALPHA) * smoothedCursor.x
        smoothedCursor.y = SMOOTH_ALPHA * mappedPoint.y + (1 - SMOOTH_ALPHA) * smoothedCursor.y

        // --- Mouth Detection ---
        val isMouthOpen = try {
            val landmarks = result.faceLandmarks().firstOrNull()
            if (landmarks != null) {
                val topLip = landmarks[13] // MediaPipe landmark approx top inner lip
                val bottomLip = landmarks[14] // approx bottom inner lip
                val dy = Math.abs(topLip.y() - bottomLip.y())
                dy > 0.02f // heuristic threshold
            } else false
        } catch (_: Exception) { false }

        // --- Head Movement Detection for Swipes ---
        // For now, we'll use a simple approach - detect head movement when mouth is open
        if (isMouthOpen) {
            detectHeadMovement(result, frameWidth, frameHeight)
        }

        // --- Update UI on the main thread ---
        runOnUiThread {
            overlayView.setInputImageInfo(frameWidth, frameHeight)
            overlayView.setFaceLandmarkerResult(result)
            overlayView.updateCursor(smoothedCursor)
            overlayView.setCursorColor(isMouthOpen)
        }

        // --- Trigger tap once when mouth transitions from closed->open ---
        if (isMouthOpen && !mouthWasOpen) {
            mouthWasOpen = true
            performTap(smoothedCursor)
        } else if (!isMouthOpen) {
            mouthWasOpen = false
        }
    }

    override fun onError(error: String, errorCode: Int) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun performTap(cursor: PointF) {
        // Use accessibility service for reliable clicking
        GazeAccessibilityService.performTap(cursor.x, cursor.y)
    }


    private fun detectHeadMovement(result: FaceLandmarkerResult, frameWidth: Int, frameHeight: Int) {
        try {
            val landmarks = result.faceLandmarks().firstOrNull() ?: return
            
            // Use nose tip as reference point for head position
            val noseTip = landmarks[1] // Nose tip landmark
            val currentHeadPosition = PointF(noseTip.x(), noseTip.y())
            
            // Calculate head movement
            val deltaX = currentHeadPosition.x - lastHeadPosition.x
            val deltaY = currentHeadPosition.y - lastHeadPosition.y
            
            // Check if movement is significant and cooldown has passed
            val currentTime = System.currentTimeMillis()
            if (kotlin.math.abs(deltaX) > headMovementThreshold || kotlin.math.abs(deltaY) > headMovementThreshold) {
                if (currentTime - swipeCooldown > swipeCooldownDuration) {
                    performSwipe(deltaX, deltaY)
                    swipeCooldown = currentTime
                }
            }
            
            lastHeadPosition = currentHeadPosition
        } catch (e: Exception) {
            // Ignore errors in head movement detection
        }
    }

    private fun performSwipe(deltaX: Float, deltaY: Float) {
        // Determine swipe direction based on head movement
        val swipeDirection = when {
            kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) -> {
                if (deltaX > 0) "right" else "left"
            }
            else -> {
                if (deltaY > 0) "down" else "up"
            }
        }
        
        // Perform the corresponding swipe gesture
        when (swipeDirection) {
            "left" -> performSwipeGesture("right") // Head left = swipe right
            "right" -> performSwipeGesture("left") // Head right = swipe left
            "up" -> performSwipeGesture("down") // Head up = swipe down
            "down" -> performSwipeGesture("up") // Head down = swipe up
        }
    }

    private fun performSwipeGesture(direction: String) {
        // Use accessibility service to perform swipe gestures
        GazeAccessibilityService.performSwipe(direction)
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
        faceLandmarkerHelper.clearFaceLandmarker()
        
        // Save voice command state
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        prefs.edit().putBoolean("voice_commands_enabled", toggleVoiceCommands.isChecked).apply()
    }
}
