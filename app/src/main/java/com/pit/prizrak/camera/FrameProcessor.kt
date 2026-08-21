package com.pit.prizrak.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.pit.prizrak.background.FrameHistory
import com.pit.prizrak.background.FrameRecord
import com.pit.prizrak.background.InpaintEngine
import com.pit.prizrak.model.DetectedPose
import com.pit.prizrak.model.PersonTarget
import com.pit.prizrak.model.ProcessingMode
import com.pit.prizrak.segmentation.SegmentationEngine
import com.pit.prizrak.segmentation.SegmentationUtils
import com.pit.prizrak.tracking.PersonMatcher
import com.pit.prizrak.tracking.PoseTracker
import com.pit.prizrak.util.FrameGeometry
import com.pit.prizrak.util.ImageUtils
import com.pit.prizrak.util.MatUtils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * Runs on the CameraX analysis executor thread, one frame at a time (CameraX won't
 * deliver the next frame until we close the current [ImageProxy], which naturally
 * throttles us to whatever framerate this pipeline can sustain).
 *
 * Two modes, chosen by [effectRunning]:
 *  - off: cheap passthrough, just show the (upright) camera frame, still running pose
 *    detection so the UI can show live bounding boxes for tap-to-select.
 *  - on: match the tracked person, segment them out, and composite them away using
 *    [InpaintEngine] before displaying the result.
 */
class FrameProcessor(
    context: Context,
    private val outputSurface: PrizrakSurfaceView,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onFrame(poses: List<DetectedPose>)
        fun onTargetUpdated(target: PersonTarget, wasMatchedThisFrame: Boolean)
    }

    private val poseTracker = PoseTracker(context)
    private val segmenter = SegmentationEngine(context)
    private val history = FrameHistory(capacity = 12)
    private val inpaintEngine = InpaintEngine()

    @Volatile var target: PersonTarget? = null
    @Volatile var effectRunning: Boolean = false
    @Volatile var processingMode: ProcessingMode = ProcessingMode.STATIC

    fun updateTarget(newTarget: PersonTarget?) {
        target = newTarget
        history.clear()
    }

    fun process(imageProxy: ImageProxy) {
        try {
            val uprightBitmap = ImageUtils.imageProxyToUprightBitmap(imageProxy)
            val poses = poseTracker.detect(uprightBitmap)
            callbacks.onFrame(poses)

            val currentTarget = target
            if (!effectRunning || currentTarget == null) {
                outputSurface.postFrame(uprightBitmap)
                return
            }

            val now = System.currentTimeMillis()
            val matched = PersonMatcher.match(currentTarget, poses, now)
            val effectiveTarget = matched ?: currentTarget
            target = effectiveTarget
            callbacks.onTargetUpdated(effectiveTarget, matched != null)

            processFrameWithEffect(uprightBitmap, effectiveTarget, matched != null, now)
        } finally {
            imageProxy.close()
        }
    }

    private fun processFrameWithEffect(
        uprightBitmap: Bitmap,
        target: PersonTarget,
        wasMatched: Boolean,
        timestampMs: Long
    ) {
        val workingBitmap = Bitmap.createScaledBitmap(
            uprightBitmap, FrameGeometry.WORKING_WIDTH, FrameGeometry.WORKING_HEIGHT, true
        )
        val bgrMat = MatUtils.bitmapToBgrMat(workingBitmap)
        val grayMat = Mat()
        Imgproc.cvtColor(bgrMat, grayMat, Imgproc.COLOR_BGR2GRAY)

        var personMask = Mat.zeros(bgrMat.size(), CvType.CV_8UC1)
        if (wasMatched) {
            val categoryMask = segmenter.segmentCategoryMask(uprightBitmap)
            personMask.release()
            personMask = SegmentationUtils.buildTargetMask(categoryMask, target.bboxNorm(), bgrMat.size())
            categoryMask.release()
        }

        val hasHole = Core.countNonZero(personMask) > 0
        val composited = if (hasHole) {
            inpaintEngine.composite(bgrMat, grayMat, personMask, history, processingMode)
        } else {
            bgrMat
        }

        history.push(FrameRecord(bgrMat.clone(), grayMat.clone(), personMask.clone(), timestampMs))

        val outputBitmap = MatUtils.bgrMatToBitmap(composited)
        val displayBitmap = Bitmap.createScaledBitmap(outputBitmap, uprightBitmap.width, uprightBitmap.height, true)
        outputSurface.postFrame(displayBitmap)

        grayMat.release()
        personMask.release()
        if (hasHole) composited.release()
        bgrMat.release()
    }

    fun close() {
        poseTracker.close()
        segmenter.close()
        history.clear()
    }
}
