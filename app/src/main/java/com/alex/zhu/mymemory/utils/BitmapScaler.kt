package com.alex.zhu.mymemory.utils

import android.graphics.Bitmap

// Turn this into an object because this is a utility class, it doesn't make sense to have multiple
// instances of BitmapScaler
object BitmapScaler {

    // Scale and maintain aspect ration given a desired width
    // BitmapScaler.scaleToFitWidth(bitmap, 100);
    fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
        val factor = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b, width, (b.height * factor).toInt(), true)
    }

    // Scale and maintain aspect ratio given a desired height
    fun scaleToFitHeight(b: Bitmap, height: Int): Bitmap {
        val factor = height / b.height.toFloat()
        return Bitmap.createScaledBitmap(b, (b.width * factor).toInt(), height, true)
    }
}
