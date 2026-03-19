package com.carinfo.ar.camera

import android.util.Log
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.min

/**
 * Lightweight frame-to-frame motion tracker using block matching on downsampled grayscale frames.
 *
 * Strategy:
 * - Downsample the Y (luminance) plane to a small resolution (e.g., 80x60)
 * - Compare consecutive frames by exhaustive search for the global 2D translation
 *   that minimizes the Sum of Absolute Differences (SAD)
 * - Return the estimated motion in original frame pixels
 *
 * This is a simple "whole-image shift" estimator, not full optical flow.
 * It works well for smooth camera panning (the main use case for AR overlay stabilization).
 */
class FrameMotionTracker {

    companion object {
        private const val TAG = "MotionTracker"
        private const val DS_WIDTH = 80     // Downsampled width
        private const val DS_HEIGHT = 60    // Downsampled height
        private const val SEARCH_RANGE = 10 // Search ±10 pixels in downsampled space
    }

    private var prevFrame: ByteArray? = null
    private var prevOrigW = 0
    private var prevOrigH = 0
    private var frameCount = 0

    /**
     * Process a new frame and return the estimated motion (dx, dy) in SCREEN pixels.
     *
     * @param yPlane The Y (luminance) plane bytes from ImageProxy
     * @param yRowStride Row stride of the Y plane
     * @param imgWidth Original image width
     * @param imgHeight Original image height
     * @param screenWidth View width on screen
     * @param screenHeight View height on screen
     * @return Pair(dx, dy) in screen pixels, or null if this is the first frame
     */
    fun processFrame(
        yPlane: ByteBuffer,
        yRowStride: Int,
        imgWidth: Int,
        imgHeight: Int,
        screenWidth: Int,
        screenHeight: Int
    ): Pair<Float, Float>? {
        // Downsample the Y plane
        val downsampled = downsample(yPlane, yRowStride, imgWidth, imgHeight)

        val prev = prevFrame
        val result: Pair<Float, Float>?

        if (prev != null && prevOrigW == imgWidth && prevOrigH == imgHeight) {
            // Find the best matching offset between prev and current frame
            val (dxDs, dyDs) = findBestOffset(prev, downsampled)

            // Scale from downsampled pixels to screen pixels
            // downsampled → original image → screen
            val scaleX = screenWidth.toFloat() / imgWidth.toFloat()
            val scaleY = screenHeight.toFloat() / imgHeight.toFloat()
            val imgScaleX = imgWidth.toFloat() / DS_WIDTH.toFloat()
            val imgScaleY = imgHeight.toFloat() / DS_HEIGHT.toFloat()

            val dx = dxDs * imgScaleX * scaleX
            val dy = dyDs * imgScaleY * scaleY

            result = Pair(dx, dy)

            frameCount++
            if (frameCount % 30 == 0) {
                Log.d(TAG, "Motion: ds=($dxDs,$dyDs) screen=(${dx.toInt()},${dy.toInt()}) " +
                        "img=${imgWidth}x${imgHeight} screen=${screenWidth}x${screenHeight}")
            }
        } else {
            result = null
            if (prev == null) {
                Log.d(TAG, "First frame captured: ${imgWidth}x${imgHeight} → ${DS_WIDTH}x${DS_HEIGHT}")
            }
        }

        prevFrame = downsampled
        prevOrigW = imgWidth
        prevOrigH = imgHeight

        return result
    }

    /**
     * Reset tracker state (e.g., after OCR detection provides ground truth).
     */
    fun resetAccumulation() {
        // Don't clear prevFrame — we still want frame-to-frame tracking
        // This just signals that the cumulative motion should be reset
    }

    /**
     * Downsample the Y plane to DS_WIDTH x DS_HEIGHT using nearest-neighbor sampling.
     */
    private fun downsample(
        yPlane: ByteBuffer,
        yRowStride: Int,
        width: Int,
        height: Int
    ): ByteArray {
        val result = ByteArray(DS_WIDTH * DS_HEIGHT)
        val stepX = width.toFloat() / DS_WIDTH
        val stepY = height.toFloat() / DS_HEIGHT

        yPlane.rewind()

        for (dy in 0 until DS_HEIGHT) {
            val srcY = (dy * stepY).toInt().coerceIn(0, height - 1)
            for (dx in 0 until DS_WIDTH) {
                val srcX = (dx * stepX).toInt().coerceIn(0, width - 1)
                val idx = srcY * yRowStride + srcX
                result[dy * DS_WIDTH + dx] = if (idx < yPlane.capacity()) {
                    yPlane.get(idx)
                } else {
                    0
                }
            }
        }

        return result
    }

    /**
     * Find the 2D offset (dx, dy) that best aligns prevFrame to curFrame.
     * Uses exhaustive search with Sum of Absolute Differences (SAD).
     */
    private fun findBestOffset(prev: ByteArray, cur: ByteArray): Pair<Int, Int> {
        var bestDx = 0
        var bestDy = 0
        var bestSad = Long.MAX_VALUE

        // The overlap region shrinks as offset increases
        // We search in a ±SEARCH_RANGE window
        for (offY in -SEARCH_RANGE..SEARCH_RANGE) {
            for (offX in -SEARCH_RANGE..SEARCH_RANGE) {
                val sad = computeSAD(prev, cur, offX, offY)
                if (sad < bestSad) {
                    bestSad = sad
                    bestDx = offX
                    bestDy = offY
                }
            }
        }

        return Pair(bestDx, bestDy)
    }

    /**
     * Compute Sum of Absolute Differences between prev and cur with offset (offX, offY).
     * Only computes over the overlapping region.
     */
    private fun computeSAD(prev: ByteArray, cur: ByteArray, offX: Int, offY: Int): Long {
        // Overlapping region
        val startX = maxOf(0, offX)
        val endX = minOf(DS_WIDTH, DS_WIDTH + offX)
        val startY = maxOf(0, offY)
        val endY = minOf(DS_HEIGHT, DS_HEIGHT + offY)

        if (startX >= endX || startY >= endY) return Long.MAX_VALUE

        var sad = 0L
        var count = 0

        for (y in startY until endY) {
            val prevY = y - offY
            for (x in startX until endX) {
                val prevX = x - offX
                val prevVal = prev[prevY * DS_WIDTH + prevX].toInt() and 0xFF
                val curVal = cur[y * DS_WIDTH + x].toInt() and 0xFF
                sad += abs(prevVal - curVal)
                count++
            }
        }

        // Normalize by overlap area (so larger offsets aren't penalized)
        return if (count > 0) sad * (DS_WIDTH * DS_HEIGHT).toLong() / count else Long.MAX_VALUE
    }
}
