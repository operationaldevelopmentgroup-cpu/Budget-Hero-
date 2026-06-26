package com.example.budgethero.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object CircularProgressUtils {

    /**
     * Creates a circular progress bitmap with a track and a progress indicator.
     * 
     * @param sizePx The size of the bitmap in pixels (square).
     * @param progress The progress value from 0.0 to 1.0.
     * @param color The color of the progress indicator.
     * @param trackColor The color of the background track.
     * @param strokeWidthPx The width of the stroke in pixels.
     * @return A square bitmap containing the circular progress.
     */
    fun createCircularProgressBitmap(
        sizePx: Int,
        progress: Float,
        color: Color,
        trackColor: Color,
        strokeWidthPx: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
        }

        val margin = strokeWidthPx / 2f
        val rect = RectF(margin, margin, sizePx - margin, sizePx - margin)

        // Draw track
        paint.color = trackColor.toArgb()
        canvas.drawOval(rect, paint)

        // Draw progress
        if (progress > 0f) {
            paint.color = color.toArgb()
            val sweepAngle = (progress.coerceIn(0f, 1f) * 360f)
            canvas.drawArc(rect, -90f, sweepAngle, false, paint)
        }

        return bitmap
    }
}
