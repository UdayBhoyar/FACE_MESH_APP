package com.example.face_mesh_app

import android.graphics.PointF
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
// This is the import statement that was missing.
// It tells Kotlin where to find the 'NormalizedLandmark' class.
//import com.google.mediapipe.tasks.vision.core.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.max
import kotlin.math.min

object EyeGazeCalculator {

    // These are the indices from MediaPipe's official documentation
    private val LEFT_EYE_INDICES = FaceLandmarker.FACE_LANDMARKS_LEFT_EYE.map { it.start() }
    private val RIGHT_EYE_INDICES = FaceLandmarker.FACE_LANDMARKS_RIGHT_EYE.map { it.start() }
    private const val LEFT_PUPIL_INDEX = 473 // Corrected index for Left Iris in new model
    private const val RIGHT_PUPIL_INDEX = 468 // Corrected index for Right Iris

    fun getPureEyeGaze(
        result: FaceLandmarkerResult,
        frameWidth: Int,
        frameHeight: Int
    ): PointF? {
        if (result.faceLandmarks().isEmpty()) return null

        val landmarks = result.faceLandmarks()[0]

        // Calculate gaze for each eye
        val leftGaze = calculateGazeForEye(landmarks, LEFT_EYE_INDICES, LEFT_PUPIL_INDEX, frameWidth, frameHeight)
        val rightGaze = calculateGazeForEye(landmarks, RIGHT_EYE_INDICES, RIGHT_PUPIL_INDEX, frameWidth, frameHeight)

        // If one eye fails, we can choose to return null or use the other eye
        if (leftGaze == null || rightGaze == null) return null

        // Average the gaze from both eyes
        val avgX = (leftGaze.x + rightGaze.x) * 0.5f
        val avgY = (leftGaze.y + rightGaze.y) * 0.5f

        return PointF(avgX, avgY)
    }

    private fun calculateGazeForEye(
        landmarks: List<NormalizedLandmark>, // The type is now correctly resolved
        eyeIndices: List<Int>,
        pupilIndex: Int,
        frameWidth: Int,
        frameHeight: Int
    ): PointF? {
        // Get pixel coordinates of eye landmarks
        val eyePoints = eyeIndices.map { PointF(landmarks[it].x() * frameWidth, landmarks[it].y() * frameHeight) }

        // Get bounding box of the eye
        val minX = eyePoints.minOf { it.x }
        val minY = eyePoints.minOf { it.y }
        val maxX = eyePoints.maxOf { it.x }
        val maxY = eyePoints.maxOf { it.y }
        val eyeWidth = maxX - minX
        val eyeHeight = maxY - minY

        if (eyeWidth <= 0 || eyeHeight <= 0) return null

        // Get pixel coordinates of the pupil
        val pupilPoint = landmarks[pupilIndex]
        val pupilX = pupilPoint.x() * frameWidth
        val pupilY = pupilPoint.y() * frameHeight

        // Normalize pupil position within the eye's bounding box
        val relativeX = (pupilX - minX) / eyeWidth
        val relativeY = (pupilY - minY) / eyeHeight

        // Clamp to [0, 1]
        return PointF(
            max(0.0f, min(1.0f, relativeX)),
            max(0.0f, min(1.0f, relativeY))
        )
    }
}
