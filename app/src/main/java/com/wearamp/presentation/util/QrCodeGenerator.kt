package com.wearamp.presentation.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Generates a QR code bitmap with an optional label rendered into the centre.
 * Uses high error correction so the QR remains scannable even with the label.
 *
 * @param content    The text to encode (e.g. a URL).
 * @param sizePx     The width/height of the resulting bitmap in pixels.
 * @param centreLabel Optional text (e.g. a PIN) drawn in the centre of the QR code.
 * @return A [Bitmap] containing the QR code with a transparent background
 *         and white foreground for use on dark Wear OS screens.
 */
fun generateQrCodeBitmap(
    content: String,
    sizePx: Int = 512,
    centreLabel: String? = null
): Bitmap {
    val hints = mapOf(
        EncodeHintType.MARGIN to 1,
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H
    )
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.WHITE else Color.TRANSPARENT)
        }
    }

    if (!centreLabel.isNullOrEmpty()) {
        val canvas = Canvas(bitmap)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textSize = sizePx * 0.09f
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.15f
        }

        val textWidth = textPaint.measureText(centreLabel)
        val textHeight = textPaint.fontMetrics.let { it.descent - it.ascent }
        val paddingH = sizePx * 0.04f
        val paddingV = sizePx * 0.025f

        val bgRect = RectF(
            (sizePx - textWidth) / 2f - paddingH,
            (sizePx - textHeight) / 2f - paddingV,
            (sizePx + textWidth) / 2f + paddingH,
            (sizePx + textHeight) / 2f + paddingV
        )

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        val cornerRadius = sizePx * 0.02f
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)

        val textY = sizePx / 2f - (textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f
        canvas.drawText(centreLabel, sizePx / 2f, textY, textPaint)
    }

    return bitmap
}
