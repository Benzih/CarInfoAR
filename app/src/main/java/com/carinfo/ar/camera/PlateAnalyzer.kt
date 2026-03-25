package com.carinfo.ar.camera

import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import com.carinfo.ar.BuildConfig
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.carinfo.ar.data.SupportedCountry
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class DetectedPlate(
    val plateNumber: String,
    val boundingBox: Rect
)

class PlateAnalyzer(
    private val countryProvider: () -> SupportedCountry,
    private val onPlatesDetected: (plates: List<DetectedPlate>, imageWidth: Int, imageHeight: Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Israel-specific: fix letters that look like digits
    private fun fixOcrDigitsOnly(text: String): String {
        return text
            .replace('O', '0').replace('o', '0')
            .replace('I', '1').replace('l', '1').replace('i', '1')
            .replace('S', '5').replace('s', '5')
            .replace('B', '8')
            .replace('Z', '2').replace('z', '2')
            .replace('G', '6').replace('g', '9')
            .replace('A', '4').replace('b', '6')
            .replace('D', '0').replace('T', '7').replace('q', '9')
    }

    private fun cleanText(text: String): String {
        return text
            .replace("-", "").replace(" ", "")
            .replace(".", "").replace(",", "")
            .replace("·", "").replace(":", "")
            .replace(";", "").replace("'", "")
            .replace("\"", "").replace("|", "")
            .trim()
    }

    // UK: fix OCR mistakes in plate format XX99XXX
    // positions 0,1 should be letters, 2,3 should be digits, 4,5,6 should be letters
    private fun fixOcrUk(text: String): String {
        val upper = text.uppercase()
        if (upper.length != 7) return upper
        val sb = StringBuilder()
        for (i in upper.indices) {
            val c = upper[i]
            if (i in 2..3) {
                // Should be digit
                sb.append(when (c) {
                    'S' -> '5'; 'O' -> '0'; 'I' -> '1'; 'l' -> '1'
                    'B' -> '8'; 'Z' -> '2'; 'G' -> '6'; 'T' -> '7'
                    'A' -> '4'; 'D' -> '0'; 'Q' -> '0'
                    else -> c
                })
            } else {
                // Should be letter
                sb.append(when (c) {
                    '0' -> 'O'; '1' -> 'I'; '5' -> 'S'; '8' -> 'B'
                    '2' -> 'Z'; '6' -> 'G'; '4' -> 'A'; '7' -> 'T'
                    else -> c
                })
            }
        }
        return sb.toString()
    }

    // NL: try O->0 and 0->O variants since Dutch plates mix letters and digits
    private fun fixOcrNl(text: String): List<String> {
        val upper = text.uppercase()
        if (upper.length != 6) return listOf(upper)
        // Generate variants swapping O<->0, I<->1, S<->5, B<->8, Z<->2
        val variants = mutableSetOf(upper)
        val swaps = mapOf('O' to '0', '0' to 'O', 'I' to '1', '1' to 'I', 'S' to '5', '5' to 'S', 'B' to '8', '8' to 'B')
        for (i in upper.indices) {
            swaps[upper[i]]?.let { replacement ->
                variants.add(upper.substring(0, i) + replacement + upper.substring(i + 1))
            }
        }
        return variants.toList()
    }

    private fun tryExtractPlate(text: String, box: Rect?, country: SupportedCountry): DetectedPlate? {
        if (box == null) return null
        val cleaned = cleanText(text)

        // Try direct match
        val upper = cleaned.uppercase()
        if (country.plateRegex.matches(upper)) {
            return DetectedPlate(upper, box)
        }

        // Country-specific OCR fixes
        if (country == SupportedCountry.NETHERLANDS) {
            // NL: try all O/0 variants
            for (variant in fixOcrNl(cleaned)) {
                if (variant != upper && country.plateRegex.matches(variant)) {
                    if (BuildConfig.DEBUG) Log.d("PlateAnalyzer", "OCR fix: '$upper' -> '$variant'")
                    return DetectedPlate(variant, box)
                }
            }
        } else {
            val fixed = when (country) {
                SupportedCountry.ISRAEL -> fixOcrDigitsOnly(cleaned)
                SupportedCountry.UK -> fixOcrUk(cleaned)
                else -> upper
            }
            if (fixed != upper && country.plateRegex.matches(fixed)) {
                if (BuildConfig.DEBUG) Log.d("PlateAnalyzer", "OCR fix: '$upper' -> '$fixed'")
                return DetectedPlate(fixed, box)
            }
        }

        return null
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        // === OCR ===
        val country = countryProvider()
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val foundPlates = mutableListOf<DetectedPlate>()
                val seenNumbers = mutableSetOf<String>()

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        // Log all OCR text for debugging
                        val cleaned = cleanText(line.text)
                        if (cleaned.length in 4..10) {
                            if (BuildConfig.DEBUG) Log.d("PlateAnalyzer", "OCR[${country.code}]: '${line.text}' -> cleaned='$cleaned' regex=${country.plateRegex.matches(cleaned.uppercase())}")
                        }

                        for (element in line.elements) {
                            val plate = tryExtractPlate(element.text, element.boundingBox, country)
                            if (plate != null && plate.plateNumber !in seenNumbers) {
                                foundPlates.add(plate)
                                seenNumbers.add(plate.plateNumber)
                            }
                        }

                        // Try the whole line concatenated
                        if (seenNumbers.none { cleanText(line.text).contains(it) }) {
                            val plate = tryExtractPlate(line.text, line.boundingBox, country)
                            if (plate != null && plate.plateNumber !in seenNumbers) {
                                foundPlates.add(plate)
                                seenNumbers.add(plate.plateNumber)
                            }
                        }
                    }
                }

                if (foundPlates.isNotEmpty()) {
                    if (BuildConfig.DEBUG) Log.d("PlateAnalyzer", "Found ${foundPlates.size} plates: ${foundPlates.map { it.plateNumber }}")
                }

                onPlatesDetected(foundPlates, inputImage.width, inputImage.height)
            }
            .addOnFailureListener { e ->
                Log.e("PlateAnalyzer", "Recognition failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
