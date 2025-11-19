package com.example.portraitcamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuffXfermode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.tasks.await
import java.nio.FloatBuffer

object PortraitProcessor {
    private val options = SelfieSegmenterOptions.Builder().setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE).build()
    private val segmenter by lazy { SelfieSegmenter.getClient(options) }

    suspend fun processPortrait(context: Context, srcBitmap: Bitmap): Bitmap {
        return try {
            val image = InputImage.fromBitmap(srcBitmap, 0)
            val result = segmenter.process(image).await()
            val mask = result.segmentationMask
            val maskBitmap = maskToBitmap(mask)
            val blurred = fastBlur(srcBitmap)
            compositeWithMask(srcBitmap, blurred, maskBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            srcBitmap
        }
    }

    private fun maskToBitmap(mask: SegmentationMask): Bitmap {
        val width = mask.width
        val height = mask.height
        val floatBuffer: FloatBuffer = mask.buffer.asFloatBuffer()
        val floats = FloatArray(width * height)
        floatBuffer.get(floats)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val pixels = ByteArray(width * height)
        for (i in floats.indices) {
            val v = (floats[i] * 255).toInt().coerceIn(0, 255)
            pixels[i] = v.toByte()
        }
        bmp.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(pixels))
        return bmp.copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun fastBlur(src: Bitmap): Bitmap {
        val scale = 0.125f
        val smallW = (src.width * scale).toInt().coerceAtLeast(1)
        val smallH = (src.height * scale).toInt().coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(src, smallW, smallH, true)
        return Bitmap.createScaledBitmap(small, src.width, src.height, true)
    }

    private fun compositeWithMask(foreground: Bitmap, background: Bitmap, mask: Bitmap): Bitmap {
        val fg = foreground.copy(Bitmap.Config.ARGB_8888, true)
        val bg = background.copy(Bitmap.Config.ARGB_8888, true)
        val maskScaled = Bitmap.createScaledBitmap(mask, fg.width, fg.height, true)
        val out = Bitmap.createBitmap(fg.width, fg.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(bg, 0f, 0f, null)
        val temp = Bitmap.createBitmap(fg.width, fg.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(temp)
        c.drawBitmap(fg, 0f, 0f, null)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        maskPaint.xfermode = PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
        c.drawBitmap(maskScaled, 0f, 0f, maskPaint)
        maskPaint.xfermode = null
        canvas.drawBitmap(temp, 0f, 0f, null)
        return out
    }
}
