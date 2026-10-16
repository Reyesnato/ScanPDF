package com.reynato.scannperiqundf.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import com.reynato.scannperiqundf.data.ScanDocument
import com.reynato.scannperiqundf.data.ScanPage
import com.reynato.scannperiqundf.imaging.Filters
import java.io.File
import java.io.OutputStream
import kotlin.math.max

object PdfExporter {

    private const val MAX_SIDE = 2000

    fun export(doc: ScanDocument, fileOf: (ScanPage) -> File, out: OutputStream) {
        val pdf = PdfDocument()
        try {
            doc.pages.forEachIndexed { index, page ->
                val bitmap = loadBitmap(fileOf(page)) ?: return@forEachIndexed
                val info = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val pdfPage = pdf.startPage(info)

                val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                Filters.matrix(page.filter)?.let {
                    paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(it))
                }

                pdfPage.canvas.drawColor(Color.WHITE)
                pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, paint)
                pdf.finishPage(pdfPage)
                bitmap.recycle()
            }
            pdf.writeTo(out)
        } finally {
            pdf.close()
        }
    }

    private fun loadBitmap(file: File): Bitmap? {
        if (!file.exists()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null

        val degrees = runCatching {
            when (
                ExifInterface(file.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)

        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return rotated
    }
}