package com.reynato.scannperiqundf.ui.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reynato.scannperiqundf.data.DocumentRepository
import com.reynato.scannperiqundf.data.ScanDocument
import com.reynato.scannperiqundf.data.ScanPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DocumentRepository(app)

    private val _document = MutableStateFlow<ScanDocument?>(null)
    val document: StateFlow<ScanDocument?> = _document

    fun load(docId: String) {
        viewModelScope.launch {
            _document.value = repo.getDocument(docId)
        }
    }

    fun addImages(docId: String, uris: List<Uri>) {
        viewModelScope.launch {
            uris.forEach { repo.addPageFromUri(docId, it) }
            _document.value = repo.getDocument(docId)
        }
    }

    fun pageFile(docId: String, page: ScanPage): File = repo.pageFile(docId, page)
}