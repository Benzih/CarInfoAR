package com.carinfo.ar.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import com.carinfo.ar.BuildConfig
import com.carinfo.ar.data.SupportedCountry
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

/**
 * Runs OCR on a static image (gallery pick or camera snapshot) and extracts license plates.
 *
 * Unlike [PlateAnalyzer] which processes live camera frames (one shot per frame for speed),
 * this processor is allowed to be slow — it runs ML Kit multiple times with different image
 * preprocessing strategies to maximize the chance of reading a difficult plate:
 *
 * 1. Original bitmap
 * 2. Contrast-boosted bitmap (helps with faded/dim plates)
 * 3. Grayscale bitmap (removes color noise)
 * 4. 90° rotated (phone was sideways)
 * 5. 180° rotated (phone upside down)
 * 6. 270° rotated
 *
 * Each OCR pass produces candidate plates, which are ranked by frequency + confidence.
 * The most-seen plate wins.
 */
object StaticImageOcr {

    private const val TAG = "StaticImageOcr"
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Max dimension for processing — prevents OOM on huge gallery photos (~20MP phones)
    private const val MAX_DIMENSION = 2048

    /**
     * Processes a bitmap and returns the best-guess plate number, or null if no plate found.
     *
     * @param bitmap Source image (will not be recycled — caller owns it)
     * @param country Active country rules (regex + OCR correction)
     */
    suspend fun extractPlate(bitmap: Bitmap, country: SupportedCountry): String? {
        if (BuildConfig.DEBUG) Log.d(TAG, "Processing ${bitmap.width}x${bitmap.height} for ${country.code}")

        val downscaled = downscaleIfNeeded(bitmap)

        // Generate preprocessing variants
        val variants = buildList {
            add("original" to downscaled)
            add("contrast" to boostContrast(downscaled))
            add("grayscale" to toGrayscale(downscaled))
            add("rot90" to rotate(downscaled, 90f))
            add("rot180" to rotate(downscaled, 180f))
            add("rot270" to rotate(downscaled, 270f))
        }

        // Vote: each detected plate gets +1 per variant it appears in
        val votes = mutableMapOf<String, Int>()

        for ((name, variantBitmap) in variants) {
            try {
                val visionText = runOcr(variantBitmap)
                if (visionText != null) {
                    val plates = extractPlatesFromText(visionText, country)
                    if (BuildConfig.DEBUG && plates.isNotEmpty()) {
                        Log.d(TAG, "[$name] found: $plates")
                    }
                    for (plate in plates) {
                        votes[plate] = (votes[plate] ?: 0) + 1
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "OCR failed on variant $name", e)
            } finally {
                // Recycle variant bitmap (but NOT the downscaled original, which is the first one)
                if (variantBitmap !== downscaled && variantBitmap !== bitmap) {
                    try { variantBitmap.recycle() } catch (_: Exception) {}
                }
            }
        }

        // Recycle downscaled if we actually downscaled (created a new bitmap)
        if (downscaled !== bitmap) {
            try { downscaled.recycle() } catch (_: Exception) {}
        }

        if (votes.isEmpty()) {
            if (BuildConfig.DEBUG) Log.d(TAG, "No plates detected across any variant")
            return null
        }

        // Pick the plate with the most votes
        val winner = votes.maxByOrNull { it.value }?.key
        if (BuildConfig.DEBUG) Log.d(TAG, "Votes: $votes -> winner: $winner")
        return winner
    }

    private suspend fun runOcr(bitmap: Bitmap): Text? = suspendCancellableCoroutine { cont ->
        val input = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(input)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    }

    private fun extractPlatesFromText(visionText: Text, country: SupportedCountry): List<String> {
        val found = mutableListOf<String>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                // Try each element (word)
                for (element in line.elements) {
                    tryExtract(element.text, country)?.let { found.add(it) }
                }
                // Try whole line
                tryExtract(line.text, country)?.let { found.add(it) }
                // Try all elements concatenated (for plates that got split into 2 words, e.g. "12-345 67")
                val concatenated = line.elements.joinToString("") { it.text }
                tryExtract(concatenated, country)?.let { found.add(it) }
            }
            // Also try the whole block concatenated (sometimes the plate is broken across lines)
            val blockText = block.lines.joinToString("") { it.text }
            tryExtract(blockText, country)?.let { found.add(it) }
        }
        return found.distinct()
    }

    private fun tryExtract(text: String, country: SupportedCountry): String? {
        val cleaned = cleanText(text)
        if (cleaned.length !in 4..12) return null
        val upper = cleaned.uppercase()

        // Direct match
        if (country.plateRegex.matches(upper)) return upper

        // Country-specific fixes
        return when (country) {
            SupportedCountry.ISRAEL -> {
                val digitsOnly = fixOcrDigitsOnly(cleaned)
                if (country.plateRegex.matches(digitsOnly)) digitsOnly else null
            }
            SupportedCountry.NETHERLANDS -> {
                fixOcrNl(upper).firstOrNull { country.plateRegex.matches(it) }
            }
            SupportedCountry.UK -> {
                val fixed = fixOcrUk(upper)
                if (country.plateRegex.matches(fixed)) fixed else null
            }
        }
    }

    // ===== Text cleaning (matches PlateAnalyzer) =====

    private fun cleanText(text: String): String = text
        .replace("-", "").replace(" ", "")
        .replace(".", "").replace(",", "")
        .replace("·", "").replace(":", "")
        .replace(";", "").replace("'", "")
        .replace("\"", "").replace("|", "")
        .trim()

    private fun fixOcrDigitsOnly(text: String): String = text
        .replace('O', '0').replace('o', '0')
        .replace('I', '1').replace('l', '1').replace('i', '1')
        .replace('S', '5').replace('s', '5')
        .replace('B', '8')
        .replace('Z', '2').replace('z', '2')
        .replace('G', '6').replace('g', '9')
        .replace('A', '4').replace('b', '6')
        .replace('D', '0').replace('T', '7').replace('q', '9')
        .filter { it.isDigit() }

    private fun fixOcrUk(text: String): String {
        val upper = text.uppercase()
        if (upper.length != 7) return upper
        val sb = StringBuilder()
        for (i in upper.indices) {
            val c = upper[i]
            if (i in 2..3) {
                sb.append(when (c) {
                    'S' -> '5'; 'O' -> '0'; 'I' -> '1'; 'l' -> '1'
                    'B' -> '8'; 'Z' -> '2'; 'G' -> '6'; 'T' -> '7'
                    'A' -> '4'; 'D' -> '0'; 'Q' -> '0'
                    else -> c
                })
            } else {
                sb.append(when (c) {
                    '0' -> 'O'; '1' -> 'I'; '5' -> 'S'; '8' -> 'B'
                    '2' -> 'Z'; '6' -> 'G'; '4' -> 'A'; '7' -> 'T'
                    else -> c
                })
            }
        }
        return sb.toString()
    }

    private fun fixOcrNl(text: String): List<String> {
        val upper = text.uppercase()
        if (upper.length != 6) return listOf(upper)
        val variants = mutableSetOf(upper)
        val swaps = mapOf(
            'O' to '0', '0' to 'O',
            'I' to '1', '1' to 'I',
            'S' to '5', '5' to 'S',
            'B' to '8', '8' to 'B',
            'Z' to '2', '2' to 'Z'
        )
        for (i in upper.indices) {
            swaps[upper[i]]?.let { replacement ->
                variants.add(upper.substring(0, i) + replacement + upper.substring(i + 1))
            }
        }
        return variants.toList()
    }

    // ===== Image preprocessing =====

    private fun downscaleIfNeeded(bitmap: Bitmap): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / longest
        val newW = (bitmap.width * scale).toInt()
        val newH = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun rotate(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun toGrayscale(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun boostContrast(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        // Contrast boost: scale ~1.6x around middle gray
        val contrast = 1.6f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val colorMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
