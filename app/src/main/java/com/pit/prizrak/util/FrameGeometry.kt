package com.pit.prizrak.util

import android.graphics.PointF
import android.graphics.RectF

/**
 * Shared assumptions about the analysis frame size, so the tap-to-select math in the UI
 * layer agrees with how [com.pit.prizrak.camera.PrizrakSurfaceView] actually draws frames
 * (center-crop fill). The app is locked to portrait, so the upright frame is the analysis
 * target resolution with width/height swapped.
 */
object FrameGeometry {
    const val ANALYSIS_WIDTH = 640
    const val ANALYSIS_HEIGHT = 480
    const val UPRIGHT_WIDTH = ANALYSIS_HEIGHT
    const val UPRIGHT_HEIGHT = ANALYSIS_WIDTH

    // Resolution the OpenCV background compositing actually runs at - the heavy step, so
    // kept well below the analysis/display resolution and upscaled back afterwards.
    const val WORKING_WIDTH = UPRIGHT_WIDTH / 2
    const val WORKING_HEIGHT = UPRIGHT_HEIGHT / 2

    /**
     * Maps a tap in view-local pixels to normalized [0,1] image coordinates, assuming the
     * image is drawn into the view with center-crop fill. Returns null if the tap landed
     * in the cropped-out margin.
     */
    fun viewTapToNormalizedImage(tapX: Float, tapY: Float, viewWidth: Float, viewHeight: Float): PointF? {
        if (viewWidth <= 0f || viewHeight <= 0f) return null
        val viewRatio = viewWidth / viewHeight
        val imgRatio = UPRIGHT_WIDTH.toFloat() / UPRIGHT_HEIGHT.toFloat()

        val drawnWidth: Float
        val drawnHeight: Float
        val offsetX: Float
        val offsetY: Float
        if (imgRatio > viewRatio) {
            drawnHeight = viewHeight
            drawnWidth = viewHeight * imgRatio
            offsetX = (viewWidth - drawnWidth) / 2f
            offsetY = 0f
        } else {
            drawnWidth = viewWidth
            drawnHeight = viewWidth / imgRatio
            offsetX = 0f
            offsetY = (viewHeight - drawnHeight) / 2f
        }

        val localX = tapX - offsetX
        val localY = tapY - offsetY
        if (localX < 0f || localY < 0f || localX > drawnWidth || localY > drawnHeight) return null
        return PointF(localX / drawnWidth, localY / drawnHeight)
    }

    /** Inverse of [viewTapToNormalizedImage]: maps a normalized image-space rect to view pixels. */
    fun normalizedImageRectToView(rect: RectF, viewWidth: Float, viewHeight: Float): RectF? {
        if (viewWidth <= 0f || viewHeight <= 0f) return null
        val viewRatio = viewWidth / viewHeight
        val imgRatio = UPRIGHT_WIDTH.toFloat() / UPRIGHT_HEIGHT.toFloat()

        val drawnWidth: Float
        val drawnHeight: Float
        val offsetX: Float
        val offsetY: Float
        if (imgRatio > viewRatio) {
            drawnHeight = viewHeight
            drawnWidth = viewHeight * imgRatio
            offsetX = (viewWidth - drawnWidth) / 2f
            offsetY = 0f
        } else {
            drawnWidth = viewWidth
            drawnHeight = viewWidth / imgRatio
            offsetX = 0f
            offsetY = (viewHeight - drawnHeight) / 2f
        }

        return RectF(
            offsetX + rect.left * drawnWidth,
            offsetY + rect.top * drawnHeight,
            offsetX + rect.right * drawnWidth,
            offsetY + rect.bottom * drawnHeight
        )
    }
}
