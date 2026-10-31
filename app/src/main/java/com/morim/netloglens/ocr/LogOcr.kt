package com.morim.netloglens.ocr

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.morim.netloglens.core.OcrLine
import com.morim.netloglens.core.RowAssembler

/** スマホ内（オフライン）で文字認識する。AIサービスには何も送らない。 */
object LogOcr {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    fun recognize(image: ImageProxy, onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        val rotation = image.imageInfo.rotationDegrees
        val bitmap = try {
            image.toBitmap()
        } catch (e: Exception) {
            image.close()
            onError(e)
            return
        }
        image.close()
        recognizer.process(InputImage.fromBitmap(bitmap, rotation))
            .addOnSuccessListener { text ->
                val lines = text.textBlocks.flatMap { it.lines }.mapNotNull { l ->
                    val b = l.boundingBox ?: return@mapNotNull null
                    OcrLine(l.text, b.left, b.top, b.right, b.bottom)
                }
                onResult(if (lines.isEmpty()) text.text else RowAssembler.assemble(lines))
            }
            .addOnFailureListener { onError(it) }
    }
}
