/*
package com.example.budgethero.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CircularProgressUtilsTest {

    @Test
    fun `createCircularProgressBitmap creates a non-null bitmap`() {
        val size = 100
        val progress = 0.5f
        val color = Color.Blue
        val trackColor = Color.LightGray
        val strokeWidth = 10f

        val bitmap = CircularProgressUtils.createCircularProgressBitmap(
            size, progress, color, trackColor, strokeWidth
        )

        assertNotNull(bitmap)
        assert(bitmap.width == size)
        assert(bitmap.height == size)
    }

    @Test
    fun `createCircularProgressBitmap handles edge cases`() {
        val size = 100
        val color = Color.Green
        val trackColor = Color.Gray
        val strokeWidth = 5f

        // 0% progress
        val bitmap0 = CircularProgressUtils.createCircularProgressBitmap(
            size, 0f, color, trackColor, strokeWidth
        )
        assertNotNull(bitmap0)

        // 100% progress
        val bitmap100 = CircularProgressUtils.createCircularProgressBitmap(
            size, 1f, color, trackColor, strokeWidth
        )
        assertNotNull(bitmap100)
        
        // Over 100% progress
        val bitmap120 = CircularProgressUtils.createCircularProgressBitmap(
            size, 1.2f, color, trackColor, strokeWidth
        )
        assertNotNull(bitmap120)
    }
}
*/
