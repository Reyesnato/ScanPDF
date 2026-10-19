package com.reynato.scannperiqundf.ui.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reynato.scannperiqundf.data.DocumentRepository
import com.reynato.scannperiqundf.data.ScanDocument
import com.reynato.scannperiqundf.data.ScanFilter
import com.reynato.scannperiqundf.data.ScanPage
import com.reynato.scannperiqundf.pdf.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    fun movePage(from: Int, to: Int) {
        val doc = _document.value ?: return
        if (from !in doc.pages.indices || to !in doc.pages.indices) return
        val lista = doc.pages.toMutableList()
        lista.add(to, lista.removeAt(from))
        _document.value = doc.copy(pages = lista)
    }

    fun saveOrder() {
        val doc = _document.value ?: return
        viewModelScope.launch {
            repo.save(doc.copy(updatedAt = System.currentTimeMillis()))
        }
    }
    fun setFilter(docId: String, pageId: String, filter: ScanFilter) {
        viewModelScope.launch {
            repo.setFilter(docId, pageId, filter)
            _document.value = repo.getDocument(docId)
        }
    }

    fun setFilterForAll(docId: String, filter: ScanFilter) {
        viewModelScope.launch {
            repo.setFilterForAll(docId, filter)
            _document.value = repo.getDocument(docId)
        }
    }

    fun exportPdf(docId: String, uri: Uri, onResult: (Boolean) -> Unit) {
        val doc = _document.value ?: return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val salida = getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?: error("No se pudo abrir el archivo")
                    salida.use { out ->
                        PdfExporter.export(doc, { repo.pageFile(docId, it) }, out)
                    }
                }.isSuccess
            }
            onResult(ok)
        }
    }

    fun deletePage(docId: String, pageId: String) {
        viewModelScope.launch {
            repo.deletePage(docId, pageId)
            _document.value = repo.getDocument(docId)
        }
    }
    fun rename(docId: String, title: String) {
        viewModelScope.launch {
            repo.rename(docId, title)
            _document.value = repo.getDocument(docId)
        }
    }
}