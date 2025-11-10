package com.example.face_mesh_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.util.Log
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * Hybrid head pose detector combining:
 * 1. TFLite model inference on face crops for accurate classification
 * 2. MediaPipe landmark-based estimation for real-time tracking
 */
class HeadPoseDetector(context: Context) {
    
    companion object {
        private const val TAG = "HeadPoseDetector"
        
        // Model constants
        private const val MODEL_PATH = "head_pose_model.tflite"
        private const val IMG_HEIGHT = 224
        private const val IMG_WIDTH = 224
        private const val NUM_CLASSES = 5
        
        // Landmark indices (MediaPipe Face Mesh)
        private const val LEFT_EYE_OUTER = 33
        private const val RIGHT_EYE_OUTER = 263
        private const val NOSE_TIP = 1
        
        // Thresholds
        private const val MODEL_CONFIDENCE_THRESHOLD = 0.45f
        private const val LANDMARK_YAW_THRESHOLD = 0.03f
        private const val LANDMARK_PITCH_THRESHOLD = 0.03f
        
        // Smoothing
        private const val HISTORY_SIZE = 5
    }
    
    // Class labels
    private val classLabels = arrayOf("center", "left", "right", "up", "down")
    
    // TFLite interpreter
    private var interpreter: Interpreter? = null
    
    // History for smoothing
    private val modelHistory = ArrayDeque<String>()
    private val landmarkHistory = ArrayDeque<String>()
    
    // Current detections
    private var lastModelDirection = "center"
    private var lastLandmarkDirection = "center"
    private var currentDirection = "center"
    
    // Model input/output buffers
    private var inputBuffer: ByteBuffer? = null
    private var outputBuffer: Array<FloatArray>? = null
    
    init {
        try {
            // Load TFLite model
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_PATH)
            val options = Interpreter.Options().apply {
                setNumThreads(4) // Use multiple threads for better performance
            }
            interpreter = Interpreter(modelBuffer, options)
            
            // Allocate buffers
            inputBuffer = ByteBuffer.allocateDirect(4 * IMG_HEIGHT * IMG_WIDTH * 3).apply {
                order(ByteOrder.nativeOrder())
            }
            outputBuffer = Array(1) { FloatArray(NUM_CLASSES) }
            
            Log.d(TAG, "TFLite model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading TFLite model: ${e.message}", e)
            interpreter = null
        }
    }

    /**
     * Public helper to check if TFLite model was successfully loaded.
     */
    fun isModelLoaded(): Boolean = interpreter != null
    
    /**
     * Hybrid detection: combines model inference with landmark tracking
     */
    fun detectHeadPose(
        faceLandmarks: FaceLandmarkerResult,
        faceCropBitmap: Bitmap? = null
    ): HeadPoseResult {
        // 1. Landmark-based detection (fast, always available)
        val landmarkDirection = detectFromLandmarks(faceLandmarks)
        
        // 2. Model-based detection (accurate, requires face crop)
        var modelDirection: String? = null
        var modelConfidence = 0f
        
        if (faceCropBitmap != null && interpreter != null) {
            val modelResult = detectFromModel(faceCropBitmap)
            modelDirection = modelResult.first
            modelConfidence = modelResult.second
        }
        
        // 3. Fusion: use model if confident, otherwise use landmarks
        val finalDirection = when {
            modelDirection != null && modelConfidence >= MODEL_CONFIDENCE_THRESHOLD -> {
                lastModelDirection = modelDirection
                modelDirection
            }
            else -> landmarkDirection
        }
        
        // 4. Smooth the result
        val smoothedDirection = smoothDirection(finalDirection)
        
        // Update current direction
        val directionChanged = smoothedDirection != currentDirection
        currentDirection = smoothedDirection
        
        return HeadPoseResult(
            direction = smoothedDirection,
            confidence = modelConfidence,
            directionChanged = directionChanged,
            source = if (modelDirection != null) "hybrid" else "landmark"
        )
    }
    
    /**
     * Detect head pose from MediaPipe landmarks
     */
    private fun detectFromLandmarks(result: FaceLandmarkerResult): String {
        try {
            val landmarks = result.faceLandmarks().firstOrNull() ?: return "center"
            
            if (landmarks.size <= RIGHT_EYE_OUTER) return "center"
            
            val leftEye = landmarks[LEFT_EYE_OUTER]
            val rightEye = landmarks[RIGHT_EYE_OUTER]
            val noseTip = landmarks[NOSE_TIP]
            
            // Calculate eye midpoint
            val eyeMidX = (leftEye.x() + rightEye.x()) / 2f
            val eyeMidY = (leftEye.y() + rightEye.y()) / 2f
            
            // Vector from eye midpoint to nose
            val dx = noseTip.x() - eyeMidX
            val dy = noseTip.y() - eyeMidY
            
            // Determine direction
            val direction = when {
                abs(dx) > abs(dy) -> {
                    when {
                        dx > LANDMARK_YAW_THRESHOLD -> "right"
                        dx < -LANDMARK_YAW_THRESHOLD -> "left"
                        else -> "center"
                    }
                }
                else -> {
                    when {
                        dy > LANDMARK_PITCH_THRESHOLD -> "down"
                        dy < -LANDMARK_PITCH_THRESHOLD -> "up"
                        else -> "center"
                    }
                }
            }
            
            lastLandmarkDirection = direction
            return direction
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in landmark detection: ${e.message}")
            return lastLandmarkDirection
        }
    }
    
    /**
     * Detect head pose from TFLite model
     */
    private fun detectFromModel(faceBitmap: Bitmap): Pair<String, Float> {
        try {
            val interpreter = this.interpreter ?: return Pair(lastModelDirection, 0f)
            val inputBuffer = this.inputBuffer ?: return Pair(lastModelDirection, 0f)
            val outputBuffer = this.outputBuffer ?: return Pair(lastModelDirection, 0f)
            
            // Preprocess image
            val resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, IMG_WIDTH, IMG_HEIGHT, true)
            preprocessBitmap(resizedBitmap, inputBuffer)
            
            // Run inference
            interpreter.run(inputBuffer, outputBuffer)
            
            // Get prediction
            val probabilities = outputBuffer[0]
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val maxConfidence = probabilities[maxIndex]
            val predictedClass = classLabels[maxIndex]
            
            Log.d(TAG, "Model prediction: $predictedClass (${maxConfidence * 100}%)")
            
            return Pair(predictedClass, maxConfidence)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in model inference: ${e.message}", e)
            return Pair(lastModelDirection, 0f)
        }
    }
    
    /**
     * Preprocess bitmap for model input
     */
    private fun preprocessBitmap(bitmap: Bitmap, buffer: ByteBuffer) {
        buffer.rewind()
        
        val intValues = IntArray(IMG_WIDTH * IMG_HEIGHT)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        // Convert to normalized RGB float values
        for (pixelValue in intValues) {
            // Extract RGB and normalize to [0, 1]
            val r = ((pixelValue shr 16) and 0xFF) / 255.0f
            val g = ((pixelValue shr 8) and 0xFF) / 255.0f
            val b = (pixelValue and 0xFF) / 255.0f
            
            buffer.putFloat(r)
            buffer.putFloat(g)
            buffer.putFloat(b)
        }
    }
    
    /**
     * Smooth direction using history
     */
    private fun smoothDirection(direction: String): String {
        landmarkHistory.addLast(direction)
        if (landmarkHistory.size > HISTORY_SIZE) {
            landmarkHistory.removeFirst()
        }
        
        // Return most frequent direction
        val counts = landmarkHistory.groupingBy { it }.eachCount()
        return counts.maxByOrNull { it.value }?.key ?: direction
    }
    
    /**
     * Create face crop bitmap from camera frame
     */
    fun createFaceCropBitmap(
        frameBitmap: Bitmap,
        faceLandmarks: FaceLandmarkerResult
    ): Bitmap? {
        try {
            val landmarks = faceLandmarks.faceLandmarks().firstOrNull() ?: return null
            
            // Calculate bounding box from landmarks
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE
            
            for (i in 0 until minOf(landmarks.size, 468)) {
                val landmark = landmarks[i]
                minX = minOf(minX, landmark.x())
                minY = minOf(minY, landmark.y())
                maxX = maxOf(maxX, landmark.x())
                maxY = maxOf(maxY, landmark.y())
            }
            
            // Convert to pixel coordinates
            val width = frameBitmap.width
            val height = frameBitmap.height
            
            val x = (minX * width).toInt()
            val y = (minY * height).toInt()
            val w = ((maxX - minX) * width).toInt()
            val h = ((maxY - minY) * height).toInt()
            
            // Expand and crop
            val expandedSize = (maxOf(w, h) * 1.3f).toInt()
            val centerX = x + w / 2
            val centerY = y + h / 2
            
            val cropX = maxOf(0, centerX - expandedSize / 2)
            val cropY = maxOf(0, centerY - expandedSize / 2)
            val cropW = minOf(expandedSize, width - cropX)
            val cropH = minOf(expandedSize, height - cropY)
            
            if (cropW <= 0 || cropH <= 0) return null
            
            return Bitmap.createBitmap(frameBitmap, cropX, cropY, cropW, cropH)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating face crop: ${e.message}")
            return null
        }
    }
    
    fun close() {
        interpreter?.close()
        interpreter = null
    }
    
    /**
     * Result data class
     */
    data class HeadPoseResult(
        val direction: String,
        val confidence: Float,
        val directionChanged: Boolean,
        val source: String // "model", "landmark", or "hybrid"
    )
}
