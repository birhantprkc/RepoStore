package com.samyak.repostore.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Generates QR code bitmaps at runtime so they always reflect the current data
 * (e.g. the donate UPI URI) instead of relying on a pre-rendered image asset.
 */
object QrCodeGenerator {

    /**
     * Encode [content] into a square QR [Bitmap] of [sizePx] pixels.
     *
     * @param foreground color of the modules (defaults to black)
     * @param background color of the quiet zone / empty modules (defaults to white)
     * @return the generated bitmap, or null if encoding failed.
     */
    fun generate(
        content: String,
        sizePx: Int = 640,
        foreground: Int = Color.BLACK,
        background: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank() || sizePx <= 0) return null
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = matrix.width
            val height = matrix.height
            val bitmap = createBitmap(width, height)
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (matrix.get(x, y)) foreground else background
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
