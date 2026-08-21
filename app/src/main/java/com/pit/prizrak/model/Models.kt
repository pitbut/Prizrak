package com.pit.prizrak.model

import android.graphics.PointF
import android.graphics.RectF

/** A person detected in the current frame, in normalized (0..1) image coordinates. */
data class DetectedPose(
    val bboxNorm: RectF,
    val centerNorm: PointF,
    val meanColorRgb: FloatArray
)

/** The person the user selected, tracked frame to frame. */
data class PersonTarget(
    val centerNorm: PointF,
    val widthNorm: Float,
    val heightNorm: Float,
    val meanColorRgb: FloatArray,
    val lastSeenMs: Long,
    val framesSinceSeen: Int = 0
) {
    fun bboxNorm(): RectF = RectF(
        centerNorm.x - widthNorm / 2f,
        centerNorm.y - heightNorm / 2f,
        centerNorm.x + widthNorm / 2f,
        centerNorm.y + heightNorm / 2f
    )
}

enum class ProcessingMode {
    STATIC,
    SHAKE_COMPENSATION,
    FULL_HOMOGRAPHY
}

enum class EffectState {
    IDLE,
    TARGET_SELECTED,
    RUNNING
}

data class UiState(
    val hasCameraPermission: Boolean = false,
    val effectState: EffectState = EffectState.IDLE,
    val targetBoxNorm: RectF? = null,
    val targetLost: Boolean = false,
    val processingMode: ProcessingMode = ProcessingMode.STATIC,
    val statusText: String = ""
)
