package com.carinfo.ar.camera

import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
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
    private val country: SupportedCountry,
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
            .replace("·", "").trim()
    }

    private fun tryExtractPlate(text: String, box: Rect?): DetectedPlate? {
        if (box == null) return null
        val cleaned = cleanText(text)

        // Try direct match
        if (country.plateRegex.matches(cleaned)) {
            return DetectedPlate(formatPlateForApi(cleaned), box)
        }

        // Country-specific OCR fixes
        val fixed = when (country) {
            SupportedCountry.ISRAEL -> fixOcrDigitsOnly(cleaned)
            SupportedCountry.NETHERLANDS -> cleaned.uppercase()
            SupportedCountry.UK -> cleaned.uppercase()
        }

        if (fixed != cleaned && country.plateRegex.matches(fixed)) {
            return DetectedPlate(formatPlateForApi(fixed), box)
        }

        return null
    }

    private fun formatPlateForApi(plate: String): String {
        return when (country) {
            SupportedCountry.NETHERLANDS -> plate.uppercase()
            SupportedCountry.UK -> plate.uppercase()
            else -> plate
        }
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val foundPlates = mutableListOf<DetectedPlate>()
                val seenNumbers = mutableSetOf<String>()

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        for (element in line.elements) {
                            val plate = tryExtractPlate(element.text, element.boundingBox)
                            if (plate != null && plate.plateNumber !in seenNumbers) {
                                foundPlates.add(plate)
                                seenNumbers.add(plate.plateNumber)
                            }
                        }

                        // Try the whole line concatenated
                        if (seenNumbers.none { cleanText(line.text).contains(it) }) {
                            val plate = tryExtractPlate(line.text, line.boundingBox)
                            if (plate != null && plate.plateNumber !in seenNumbers) {
                                foundPlates.add(plate)
                                seenNumbers.add(plate.plateNumber)
                            }
                        }
                    }
                }

                if (foundPlates.isNotEmpty()) {
                    Log.d("PlateAnalyzer", "Found ${foundPlates.size} plates: ${foundPlates.map { it.plateNumber }}")
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
