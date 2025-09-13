package com.example.face_mesh_app

import android.graphics.Matrix
import android.graphics.PointF

class EyeCalibration {

    private val screenPts = mutableListOf<PointF>()
    private val eyePts = mutableListOf<PointF>()
    private val transformMatrix = Matrix()

    var isCalibrated = false
        private set

    fun reset() {
        screenPts.clear()
        eyePts.clear()
        transformMatrix.reset()
        isCalibrated = false
    }

    fun addPair(screenXY: PointF, eyeXY: PointF) {
        if (screenPts.size < 4) {
            screenPts.add(screenXY)
            eyePts.add(eyeXY)
        }
    }

    fun solve(): Boolean {
        if (screenPts.size == 4) {
            val src = floatArrayOf(
                eyePts[0].x, eyePts[0].y,
                eyePts[1].x, eyePts[1].y,
                eyePts[2].x, eyePts[2].y,
                eyePts[3].x, eyePts[3].y
            )
            val dst = floatArrayOf(
                screenPts[0].x, screenPts[0].y,
                screenPts[1].x, screenPts[1].y,
                screenPts[2].x, screenPts[2].y,
                screenPts[3].x, screenPts[3].y
            )

            // This is the Android equivalent of cv2.getPerspectiveTransform
            isCalibrated = transformMatrix.setPolyToPoly(src, 0, dst, 0, 4)
            return isCalibrated
        }
        return false
    }

    fun map(eyeX: Float, eyeY: Float, screenWidth: Int, screenHeight: Int): PointF {
        if (!isCalibrated) {
            // Direct scaling if not calibrated
            return PointF(eyeX * screenWidth, eyeY * screenHeight)
        }

        val eyePoint = floatArrayOf(eyeX, eyeY)
        val screenPoint = floatArrayOf(0f, 0f)

        // This is the equivalent of cv2.perspectiveTransform
        transformMatrix.mapPoints(screenPoint, eyePoint)

        return PointF(screenPoint[0], screenPoint[1])
    }
}