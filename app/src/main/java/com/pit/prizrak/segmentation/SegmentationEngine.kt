package com.pit.prizrak.segmentation

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapExtractor
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.nio.ByteBuffer

private const val MODEL_PATH = "models/selfie_multiclass_256x256.tflite"

/**
 * Wraps MediaPipe's selfie multiclass segmenter. The model outputs one category per
 * pixel: 0 = background, 1..5 = hair/body-skin/face-skin/clothes/other-person-stuff.
 * We don't try to separate multiple people here - that's done afterwards by intersecting
 * this "any person" mask with the tracked target's bounding box (see [SegmentationUtils]).
 */
class SegmentationEngine(context: Context) {

    private val segmenter: ImageSegmenter

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_PATH)
            .build()
        val options = ImageSegmenter.ImageSegmenterOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setOutputCategoryMask(true)
            .setOutputConfidenceMasks(false)
            .build()
        segmenter = ImageSegmenter.createFromOptions(context, options)
    }

    /** Returns a single-channel (CV_8UC1) Mat of category indices, at the model's native resolution. */
    fun segmentCategoryMask(bitmap: Bitmap): Mat {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = segmenter.segment(mpImage)
        val maskImage = result.categoryMask().get()
        val maskBitmap = BitmapExtractor.extract(maskImage)
        return alpha8BitmapToMat(maskBitmap)
    }

    private fun alpha8BitmapToMat(bitmap: Bitmap): Mat {
        val buffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(buffer)
        buffer.rewind()
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC1)
        mat.put(0, 0, bytes)
        return mat
    }

    fun close() {
        segmenter.close()
    }
}
