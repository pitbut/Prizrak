package com.pit.prizrak.background

import com.pit.prizrak.model.ProcessingMode
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.photo.Photo

// For SHAKE/FULL modes, estimating a motion transform against every buffered frame is
// too expensive to do every output frame - the newest few frames are also the ones most
// likely to still resemble the current view, so we only try those.
private const val MAX_HISTORY_FRAMES_TRIED = 6
private const val INPAINT_RADIUS = 5.0

/**
 * The core "video inpainting" step: given the current frame, the mask of the pixels the
 * tracked person currently occupies, and a short history of recent frames, fills the hole
 * with whatever was visible at that location in a recent frame (motion-compensated per
 * [ProcessingMode]). Anywhere no history frame had a clean view of the background (the
 * person hasn't moved yet, or the buffer is still warming up), falls back to OpenCV's
 * Telea inpainting so the result is at least a smooth guess instead of a visible hole.
 */
class InpaintEngine {

    private val motionCompensator = MotionCompensator()

    fun composite(
        currentBgr: Mat,
        currentGray: Mat,
        currentPersonMask: Mat,
        history: FrameHistory,
        mode: ProcessingMode
    ): Mat {
        val output = currentBgr.clone()
        val remainingHole = Mat()
        currentPersonMask.copyTo(remainingHole)
        val size = Size(currentBgr.cols().toDouble(), currentBgr.rows().toDouble())

        val candidates = if (mode == ProcessingMode.STATIC) {
            history.snapshotNewestFirst()
        } else {
            history.snapshotNewestFirst(MAX_HISTORY_FRAMES_TRIED)
        }

        for (record in candidates) {
            if (Core.countNonZero(remainingHole) == 0) break

            val warpedFrame: Mat
            val warpedPersonMask: Mat
            var ownsWarped = false

            if (mode == ProcessingMode.STATIC) {
                warpedFrame = record.bgrMat
                warpedPersonMask = record.personMask
            } else {
                val homography = motionCompensator.estimate(mode, record.grayMat, currentGray) ?: continue
                warpedFrame = Mat()
                warpedPersonMask = Mat()
                Imgproc.warpPerspective(record.bgrMat, warpedFrame, homography, size)
                Imgproc.warpPerspective(
                    record.personMask, warpedPersonMask, homography, size,
                    Imgproc.INTER_NEAREST
                )
                homography.release()
                ownsWarped = true
            }

            val notPerson = Mat()
            Core.bitwise_not(warpedPersonMask, notPerson)
            val fillable = Mat()
            Core.bitwise_and(remainingHole, notPerson, fillable)

            warpedFrame.copyTo(output, fillable)
            Core.subtract(remainingHole, fillable, remainingHole)

            notPerson.release()
            fillable.release()
            if (ownsWarped) {
                warpedFrame.release()
                warpedPersonMask.release()
            }
        }

        if (Core.countNonZero(remainingHole) > 0) {
            val inpainted = Mat()
            Photo.inpaint(output, remainingHole, inpainted, INPAINT_RADIUS, Photo.INPAINT_TELEA)
            inpainted.copyTo(output, remainingHole)
            inpainted.release()
        }
        remainingHole.release()
        return output
    }
}
