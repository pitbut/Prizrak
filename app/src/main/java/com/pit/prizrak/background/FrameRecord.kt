package com.pit.prizrak.background

import org.opencv.core.Mat

/**
 * One remembered past frame: the working-resolution BGR image, its grayscale version
 * (kept around for optical-flow / ORB feature matching against later frames), and the
 * binary mask of where the tracked person was standing at the moment this frame was
 * captured. All three share the same pixel grid.
 */
class FrameRecord(
    val bgrMat: Mat,
    val grayMat: Mat,
    val personMask: Mat,
    val timestampMs: Long
) {
    fun release() {
        bgrMat.release()
        grayMat.release()
        personMask.release()
    }
}
