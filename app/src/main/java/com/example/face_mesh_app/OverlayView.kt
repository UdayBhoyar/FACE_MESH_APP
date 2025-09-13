package com.example.face_mesh_app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FACE_LANDMARKS_LEFT_EYE
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FACE_LANDMARKS_RIGHT_EYE
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FACE_LANDMARKS_LEFT_IRIS
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FACE_LANDMARKS_RIGHT_IRIS

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var faceLandmarkerResult: FaceLandmarkerResult? = null
    private var runningMode: RunningMode = RunningMode.IMAGE

    // Dimensions of the input image that produced the landmarks
    private var inputImageWidth: Int = 0
    private var inputImageHeight: Int = 0

    // Control whether to draw landmarks (disable on system overlay)
    private var showLandmarks: Boolean = true

    private val pointPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }
    private val linePaint = Paint().apply {
        color = Color.CYAN
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    // Paints for our cursor and calibration target
    private val cursorPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    private val calibrationTargetPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }

    private var cursorPosition: PointF? = null
    private var calibrationTarget: PointF? = null // deprecated UI element, no longer used

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw debug landmarks (optional)
        if (showLandmarks) {
            faceLandmarkerResult?.let { result ->
                if (inputImageWidth > 0 && inputImageHeight > 0) {
                    val viewW = width.toFloat()
                    val viewH = height.toFloat()
                    val scale = maxOf(viewW / inputImageWidth.toFloat(), viewH / inputImageHeight.toFloat())
                    val drawnW = inputImageWidth * scale
                    val drawnH = inputImageHeight * scale
                    val offsetX = (viewW - drawnW) / 2f
                    val offsetY = (viewH - drawnH) / 2f

                    for (landmarks in result.faceLandmarks()) {
                        val allEyeLandmarks = FACE_LANDMARKS_LEFT_EYE + FACE_LANDMARKS_RIGHT_EYE +
                                FACE_LANDMARKS_LEFT_IRIS + FACE_LANDMARKS_RIGHT_IRIS
                        allEyeLandmarks.forEach { pair ->
                            val start = landmarks[pair.start()]
                            val px = start.x() * inputImageWidth * scale + offsetX
                            val py = start.y() * inputImageHeight * scale + offsetY
                            canvas.drawCircle(px, py, 3f, pointPaint)
                        }
                    }
                }
            }
        }

        // Calibration target removed from UI

        // Draw the main cursor
        cursorPosition?.let {
            canvas.drawCircle(it.x, it.y, 28f, cursorPaint)
        }
    }

    fun setFaceLandmarkerResult(result: FaceLandmarkerResult) {
        faceLandmarkerResult = result
        invalidate() // Redraw the view
    }

    fun setRunningMode(runningMode: RunningMode) {
        this.runningMode = runningMode
    }

    fun updateCursor(position: PointF) {
        cursorPosition = position
        invalidate()
    }

    fun setCursorColor(isClicking: Boolean) {
        cursorPaint.color = if (isClicking) Color.RED else Color.GREEN
        invalidate()
    }

    fun showCalibrationTarget(target: PointF?) { /* no-op */ }

    fun setInputImageInfo(width: Int, height: Int) {
        inputImageWidth = width
        inputImageHeight = height
    }

    fun setShowLandmarks(show: Boolean) {
        showLandmarks = show
        invalidate()
    }
}