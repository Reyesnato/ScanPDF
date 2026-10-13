package com.reynato.scannperiqundf.navigation

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor/{docId}"
    const val CAMERA = "camera/{docId}"

    fun editor(docId: String) = "editor/$docId"
    fun camera(docId: String) = "camera/$docId"
}