package com.pit.prizrak.background

import com.pit.prizrak.model.ProcessingMode
import org.opencv.calib3d.Calib3d
import org.opencv.core.CvType
import org.opencv.core.DMatch
import org.opencv.core.KeyPoint
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import org.opencv.video.Video

private const val MIN_TRACK_POINTS = 12
private const val MIN_GOOD_MATCHES = 15

/**
 * Estimates the camera-motion transform between two grayscale frames, so a background
 * patch sampled from an older frame can be warped to line up with the current view.
 *
 * - [ProcessingMode.STATIC]: caller skips this entirely (identity transform assumed).
 * - [ProcessingMode.SHAKE_COMPENSATION]: sparse Lucas-Kanade optical flow on a handful of
 *   corner features -> a partial affine (translation + rotation + uniform scale). Cheap,
 *   good for hand shake, breaks down under large viewpoint change.
 * - [ProcessingMode.FULL_HOMOGRAPHY]: ORB features + brute-force matching + RANSAC
 *   homography. Handles a moving camera in general, but is expensive and needs enough
 *   texture in the scene to find good matches - the "ambitious, may need to fall back"
 *   stage from the spec.
 *
 * Returns a 3x3 CV_64F matrix mapping points in [fromGray] to their location in [toGray],
 * or null when a confident estimate couldn't be produced (caller should skip this frame).
 */
class MotionCompensator {

    fun estimate(mode: ProcessingMode, fromGray: Mat, toGray: Mat): Mat? = when (mode) {
        ProcessingMode.STATIC -> null
        ProcessingMode.SHAKE_COMPENSATION -> estimateShake(fromGray, toGray)
        ProcessingMode.FULL_HOMOGRAPHY -> estimateHomography(fromGray, toGray) ?: estimateShake(fromGray, toGray)
    }

    private fun estimateShake(fromGray: Mat, toGray: Mat): Mat? {
        val corners = MatOfPoint()
        Imgproc.goodFeaturesToTrack(fromGray, corners, 200, 0.01, 8.0)
        val fromPoints = corners.toArray()
        if (fromPoints.size < MIN_TRACK_POINTS) return null

        val fromPts2f = MatOfPoint2f(*fromPoints.map { Point(it.x, it.y) }.toTypedArray())
        val toPts2f = MatOfPoint2f()
        val status = MatOfByte()
        val err = MatOfFloat()
        Video.calcOpticalFlowPyrLK(fromGray, toGray, fromPts2f, toPts2f, status, err)

        val statusArr = status.toArray()
        val fromList = fromPts2f.toList()
        val toList = toPts2f.toList()
        val goodFrom = ArrayList<Point>()
        val goodTo = ArrayList<Point>()
        for (i in statusArr.indices) {
            if (statusArr[i].toInt() != 0) {
                goodFrom.add(fromList[i])
                goodTo.add(toList[i])
            }
        }
        if (goodFrom.size < MIN_TRACK_POINTS) return null

        val affine = Calib3d.estimateAffinePartial2D(
            MatOfPoint2f(*goodFrom.toTypedArray()),
            MatOfPoint2f(*goodTo.toTypedArray())
        )
        if (affine.empty()) return null

        val homography = Mat.eye(3, 3, CvType.CV_64F)
        affine.copyTo(homography.rowRange(0, 2))
        affine.release()
        return homography
    }

    private fun estimateHomography(fromGray: Mat, toGray: Mat): Mat? {
        val orb = ORB.create(500)
        val kp1 = MatOfKeyPoint()
        val kp2 = MatOfKeyPoint()
        val desc1 = Mat()
        val desc2 = Mat()
        orb.detectAndCompute(fromGray, Mat(), kp1, desc1)
        orb.detectAndCompute(toGray, Mat(), kp2, desc2)
        if (desc1.empty() || desc2.empty() || desc1.rows() < 2 || desc2.rows() < 2) return null

        val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
        val knnMatches = ArrayList<MatOfDMatch>()
        matcher.knnMatch(desc1, desc2, knnMatches, 2)

        val good = ArrayList<DMatch>()
        for (m in knnMatches) {
            val arr = m.toArray()
            if (arr.size >= 2 && arr[0].distance < 0.75f * arr[1].distance) good.add(arr[0])
        }
        if (good.size < MIN_GOOD_MATCHES) return null

        val kp1List: List<KeyPoint> = kp1.toList()
        val kp2List: List<KeyPoint> = kp2.toList()
        val srcPts = MatOfPoint2f(*good.map { kp1List[it.queryIdx].pt }.toTypedArray())
        val dstPts = MatOfPoint2f(*good.map { kp2List[it.trainIdx].pt }.toTypedArray())

        val homography = Calib3d.findHomography(srcPts, dstPts, Calib3d.RANSAC, 5.0)
        if (homography.empty()) return null
        return homography
    }
}
