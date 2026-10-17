package com.reynato.scannperiqundf.navigation

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor/{docId}"
    const val CAMERA = "camera/{docId}"
    const val CROP = "crop/{docId}/{photoName}"


    fun editor(docId: String) = "editor/$docId"
    fun camera(docId: String) = "camera/$docId"
    fun crop(docId: String, photoName: String) = "crop/$docId/$photoName"

}