package com.reynato.scannperiqundf.imaging
import com.reynato.scannperiqundf.data.ScanFilter

object Filters {

     fun matrix(filter: ScanFilter): FloatArray? = when (filter) {
        ScanFilter.ORIGINAL -> null
        ScanFilter.GRAYSCALE -> gris(1f, 0f)
        ScanFilter.SCAN -> gris(1.7f, -100f)
        ScanFilter.BLACK_WHITE -> gris(8f, -1080f)
    }

    fun label(filter: ScanFilter): String = when (filter) {
        ScanFilter.ORIGINAL -> "Original"
        ScanFilter.GRAYSCALE -> "Escala de grises"
        ScanFilter.SCAN -> "Escaneo"
        ScanFilter.BLACK_WHITE -> "Blanco y negro"
    }

    // Gris
    private fun gris(contraste: Float, brillo: Float): FloatArray {
        val r = 0.299f * contraste
        val g = 0.587f * contraste
        val b = 0.114f * contraste
        return floatArrayOf(
            r, g, b, 0f, brillo,
            r, g, b, 0f, brillo,
            r, g, b, 0f, brillo,
            0f, 0f, 0f, 1f, 0f
        )
    }
}