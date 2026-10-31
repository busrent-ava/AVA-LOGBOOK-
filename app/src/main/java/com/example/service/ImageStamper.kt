package com.example.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageStamper {

    data class StampedResult(
        val bitmap: Bitmap,
        val base64DataUrl: String
    )

    fun stampImage(
        original: Bitmap,
        driverId: String,
        busNumber: String,
        location: String,
        timestamp: Date = Date()
    ): StampedResult {
        // Scale to maximum 1280px on the longest dimension
        val maxDim = maxOf(original.width, original.height).toFloat()
        val scale = if (maxDim > 1280f) 1280f / maxDim else 1f
        val targetWidth = (original.width * scale).toInt()
        val targetHeight = (original.height * scale).toInt()

        val scaledBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
        } else {
            original.copy(Bitmap.Config.ARGB_8888, true)
        }

        val canvas = Canvas(scaledBitmap)
        val width = scaledBitmap.width
        val height = scaledBitmap.height

        // Font size proportional to width
        val fontSize = maxOf(20f, width / 34f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = fontSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(timestamp)

        val line1 = "Driver: $driverId | Bus: $busNumber"
        val line2 = location
        val line3 = formattedDate

        val bannerHeight = fontSize * 4.2f
        val bannerTop = height - bannerHeight

        // Dark translucent overlay banner
        val bgPaint = Paint().apply {
            color = Color.argb(175, 0, 0, 0)
        }
        canvas.drawRect(Rect(0, bannerTop.toInt(), width, height), bgPaint)

        // Accent stripe on top of banner
        val stripePaint = Paint().apply {
            color = Color.argb(230, 11, 107, 203) // FleetOps Blue
        }
        canvas.drawRect(Rect(0, bannerTop.toInt(), width, (bannerTop + 4).toInt()), stripePaint)

        // Draw text lines
        val paddingLeft = 16f
        canvas.drawText(line1, paddingLeft, bannerTop + fontSize * 1.2f, paint)
        canvas.drawText(line2, paddingLeft, bannerTop + fontSize * 2.4f, paint)
        canvas.drawText(line3, paddingLeft, bannerTop + fontSize * 3.6f, paint)

        // Compress to JPEG base64
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        val dataUrl = "data:image/jpeg;base64,$base64String"

        return StampedResult(
            bitmap = scaledBitmap,
            base64DataUrl = dataUrl
        )
    }
}
