package com.pit.prizrak.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.pit.prizrak.model.DetectedPose
import kotlin.math.max
import kotlin.math.min

private const val MODEL_PATH = "models/pose_landmarker_lite.task"
private const val MAX_POSES = 4
private const val VISIBILITY_THRESHOLD = 0.4f

// BlazePose landmark indices used to sample a rough torso color patch.
private const val LEFT_SHOULDER = 11
private const val RIGHT_SHOULDER = 12
private const val LEFT_HIP = 23
private const val RIGHT_HIP = 24

/**
 * Wraps MediaPipe PoseLandmarker (synchronous IMAGE mode - CameraX's KEEP_ONLY_LATEST
 * back-pressure strategy naturally throttles frame delivery to how fast we can process).
 * Detects up to [MAX_POSES] people per frame; matching a specific person to the one the
 * user tapped is done afterwards by [PersonMatcher].
 */
class PoseTracker(context: Context) {

    private val landmarker: PoseLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_PATH)
            .build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumPoses(MAX_POSES)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()
        landmarker = PoseLandmarker.createFromOptions(context, options)
    }

    fun detect(bitmap: Bitmap): List<DetectedPose> {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = landmarker.detect(mpImage)
        val poses = ArrayList<DetectedPose>(result.landmarks().size)

        for (landmarks in result.landmarks()) {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var visibleCount = 0

            for (lm in landmarks) {
                val visible = lm.visibility().orElse(1f) >= VISIBILITY_THRESHOLD
                if (!visible) continue
                visibleCount++
                minX = min(minX, lm.x())
                minY = min(minY, lm.y())
                maxX = max(maxX, lm.x())
                maxY = max(maxY, lm.y())
            }
            if (visibleCount < 4) continue

            val bbox = RectF(minX, minY, maxX, maxY)
            val center = PointF(bbox.centerX(), bbox.centerY())
            val color = sampleTorsoColor(bitmap, landmarks)
            poses.add(DetectedPose(bbox, center, color))
        }
        return poses
    }

    private fun sampleTorsoColor(bitmap: Bitmap, landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): FloatArray {
        fun point(idx: Int): PointF? {
            val lm = landmarks.getOrNull(idx) ?: return null
            if (lm.visibility().orElse(0f) < VISIBILITY_THRESHOLD) return null
            return PointF(lm.x() * bitmap.width, lm.y() * bitmap.height)
        }

        val pts = listOfNotNull(point(LEFT_SHOULDER), point(RIGHT_SHOULDER), point(LEFT_HIP), point(RIGHT_HIP))
        if (pts.isEmpty()) return floatArrayOf(128f, 128f, 128f)

        var minX = pts.minOf { it.x }
        var maxX = pts.maxOf { it.x }
        var minY = pts.minOf { it.y }
        var maxY = pts.maxOf { it.y }
        // Shrink slightly toward the center so we sample clothing, not background/edges.
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        minX = cx + (minX - cx) * 0.5f
        maxX = cx + (maxX - cx) * 0.5f
        minY = cy + (minY - cy) * 0.5f
        maxY = cy + (maxY - cy) * 0.5f

        val left = minX.toInt().coerceIn(0, bitmap.width - 1)
        val top = minY.toInt().coerceIn(0, bitmap.height - 1)
        val right = maxX.toInt().coerceIn(left + 1, bitmap.width)
        val bottom = maxY.toInt().coerceIn(top + 1, bitmap.height)

        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0
        val stepX = max(1, (right - left) / 12)
        val stepY = max(1, (bottom - top) / 12)
        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                val pixel = bitmap.getPixel(x, y)
                sumR += (pixel shr 16) and 0xFF
                sumG += (pixel shr 8) and 0xFF
                sumB += pixel and 0xFF
                count++
                x += stepX
            }
            y += stepY
        }
        if (count == 0) return floatArrayOf(128f, 128f, 128f)
        return floatArrayOf(sumR.toFloat() / count, sumG.toFloat() / count, sumB.toFloat() / count)
    }

    fun close() {
        landmarker.close()
    }
}
