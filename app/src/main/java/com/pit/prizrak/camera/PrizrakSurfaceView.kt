package com.pit.prizrak.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Renders the latest composited frame (raw passthrough, or the person-removed output)
 * directly to a Surface, bypassing Compose recomposition for the actual video - Compose
 * only draws the tap overlay and controls on top of this. [postFrame] is called from the
 * camera analysis thread; drawing to a Surface from a background thread is safe.
 */
class PrizrakSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    @Volatile
    private var surfaceReady = false

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
    }

    fun postFrame(bitmap: Bitmap) {
        if (!surfaceReady) return
        val canvas = try {
            holder.lockCanvas()
        } catch (e: Exception) {
            null
        } ?: return
        try {
            drawCenterCrop(canvas, bitmap)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawCenterCrop(canvas: Canvas, bitmap: Bitmap) {
        canvas.drawColor(Color.BLACK)
        val viewRatio = canvas.width.toFloat() / canvas.height.toFloat()
        val bmpRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        val dst = if (bmpRatio > viewRatio) {
            val scaledWidth = canvas.height * bmpRatio
            val left = (canvas.width - scaledWidth) / 2f
            RectF(left, 0f, left + scaledWidth, canvas.height.toFloat())
        } else {
            val scaledHeight = canvas.width / bmpRatio
            val top = (canvas.height - scaledHeight) / 2f
            RectF(0f, top, canvas.width.toFloat(), top + scaledHeight)
        }
        canvas.drawBitmap(bitmap, null, dst, null)
    }
}
