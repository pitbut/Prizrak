package com.pit.prizrak.segmentation

import android.graphics.RectF
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/** How much to grow the tracked target's bbox before intersecting with the segmentation mask,
 *  as a fraction of the box's own size - absorbs tracking jitter and limb overshoot. */
private const val BBOX_MARGIN = 0.25f

object SegmentationUtils {

    /**
     * Combines the "any person" category mask with the tracked target's bounding box to
     * approximate "this specific person's" mask: resize the raw category mask to the
     * working frame size, threshold it to a binary foreground mask, then keep only the
     * part that falls inside the (margin-padded) target box.
     */
    fun buildTargetMask(categoryMask: Mat, targetBboxNorm: RectF, workingSize: Size): Mat {
        val resized = Mat()
        Imgproc.resize(categoryMask, resized, workingSize, 0.0, 0.0, Imgproc.INTER_NEAREST)

        val foreground = Mat()
        Core.compare(resized, Scalar(0.0), foreground, Core.CMP_GT)
        resized.release()

        val roiMask = Mat.zeros(workingSize, CvType.CV_8UC1)
        val rect = dilatedRectPx(targetBboxNorm, workingSize, BBOX_MARGIN)
        Imgproc.rectangle(roiMask, rect.first, rect.second, Scalar(255.0), -1)

        val result = Mat()
        Core.bitwise_and(foreground, roiMask, result)
        foreground.release()
        roiMask.release()
        return result
    }

    fun dilatedRectPx(bboxNorm: RectF, size: Size, margin: Float): Pair<Point, Point> {
        val w = bboxNorm.width()
        val h = bboxNorm.height()
        val left = ((bboxNorm.left - w * margin) * size.width).coerceIn(0.0, size.width)
        val top = ((bboxNorm.top - h * margin) * size.height).coerceIn(0.0, size.height)
        val right = ((bboxNorm.right + w * margin) * size.width).coerceIn(0.0, size.width)
        val bottom = ((bboxNorm.bottom + h * margin) * size.height).coerceIn(0.0, size.height)
        return Point(left, top) to Point(right, bottom)
    }
}
