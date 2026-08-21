package com.pit.prizrak.tracking

import android.graphics.PointF
import com.pit.prizrak.model.DetectedPose
import com.pit.prizrak.model.PersonTarget
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Re-identifies the tapped person frame to frame among all people MediaPipe currently
 * detects, using position continuity + bbox size + a rough clothing-color match.
 * This is a heuristic, not real re-identification - it is honest about that limitation:
 * if two people swap places quickly or dress identically, the tracker can latch onto
 * the wrong one.
 */
object PersonMatcher {

    // How many frames a target may go undetected before we declare it "lost" in the UI.
    const val MAX_FRAMES_LOST = 15

    private const val POSITION_WEIGHT = 3.0f
    private const val SIZE_WEIGHT = 1.5f
    private const val COLOR_WEIGHT = 1.0f
    private const val MATCH_THRESHOLD = 1.4f

    // Smoothing factors (higher = follow the new detection more closely).
    private const val POSITION_SMOOTHING = 0.55f
    private const val SIZE_SMOOTHING = 0.35f
    private const val COLOR_SMOOTHING = 0.15f

    fun createTarget(pose: DetectedPose, timestampMs: Long): PersonTarget = PersonTarget(
        centerNorm = PointF(pose.centerNorm.x, pose.centerNorm.y),
        widthNorm = pose.bboxNorm.width(),
        heightNorm = pose.bboxNorm.height(),
        meanColorRgb = pose.meanColorRgb.copyOf(),
        lastSeenMs = timestampMs
    )

    fun findPoseAt(poses: List<DetectedPose>, xNorm: Float, yNorm: Float): DetectedPose? {
        val containing = poses.filter { it.bboxNorm.contains(xNorm, yNorm) }
        if (containing.isNotEmpty()) {
            return containing.minByOrNull { distance(it.centerNorm, xNorm, yNorm) }
        }
        // Fall back to the nearest pose center within a generous radius, in case the tap
        // landed just outside the bbox (loose limbs, motion blur, etc.).
        return poses.minByOrNull { distance(it.centerNorm, xNorm, yNorm) }
            ?.takeIf { distance(it.centerNorm, xNorm, yNorm) < 0.15f }
    }

    /** Returns the updated (smoothed) target if a confident match was found this frame, else null. */
    fun match(target: PersonTarget, poses: List<DetectedPose>, timestampMs: Long): PersonTarget? {
        if (poses.isEmpty()) return null

        var best: DetectedPose? = null
        var bestScore = Float.MAX_VALUE
        for (pose in poses) {
            val posDist = distance(pose.centerNorm, target.centerNorm.x, target.centerNorm.y)
            val sizeDiff = abs(pose.bboxNorm.width() - target.widthNorm) + abs(pose.bboxNorm.height() - target.heightNorm)
            val colorDist = colorDistance(pose.meanColorRgb, target.meanColorRgb) / 255f
            val score = posDist * POSITION_WEIGHT + sizeDiff * SIZE_WEIGHT + colorDist * COLOR_WEIGHT
            if (score < bestScore) {
                bestScore = score
                best = pose
            }
        }
        val matched = best ?: return null
        if (bestScore > MATCH_THRESHOLD) return null

        return PersonTarget(
            centerNorm = lerp(target.centerNorm, matched.centerNorm, POSITION_SMOOTHING),
            widthNorm = target.widthNorm + (matched.bboxNorm.width() - target.widthNorm) * SIZE_SMOOTHING,
            heightNorm = target.heightNorm + (matched.bboxNorm.height() - target.heightNorm) * SIZE_SMOOTHING,
            meanColorRgb = lerpColor(target.meanColorRgb, matched.meanColorRgb, COLOR_SMOOTHING),
            lastSeenMs = timestampMs,
            framesSinceSeen = 0
        )
    }

    private fun distance(p: PointF, x: Float, y: Float): Float {
        val dx = p.x - x
        val dy = p.y - y
        return sqrt(dx * dx + dy * dy)
    }

    private fun colorDistance(a: FloatArray, b: FloatArray): Float {
        val dr = a[0] - b[0]
        val dg = a[1] - b[1]
        val db = a[2] - b[2]
        return sqrt(dr * dr + dg * dg + db * db)
    }

    private fun lerp(a: PointF, b: PointF, t: Float): PointF =
        PointF(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

    private fun lerpColor(a: FloatArray, b: FloatArray, t: Float): FloatArray =
        floatArrayOf(
            a[0] + (b[0] - a[0]) * t,
            a[1] + (b[1] - a[1]) * t,
            a[2] + (b[2] - a[2]) * t
        )
}
