package com.reynato.scannperiqundf.data
enum class ScanFilter { SCAN, ORIGINAL, GRAYSCALE, BLACK_WHITE}

data class ScanPage(
    val id: String,
    val fileName: String,
    val filter: ScanFilter = ScanFilter.SCAN
)

data class ScanDocument(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pages: List<ScanPage>
)