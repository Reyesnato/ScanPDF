package com.reynato.scannperiqundf.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reynato.scannperiqundf.data.DocumentRepository
import com.reynato.scannperiqundf.data.ScanDocument
import kotlinx.coroutines.flow.MutableStateFlow
import com.reynato.scannperiqundf.data.ScanPage
import java.io.File
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DocumentRepository(app)

    private val _documents = MutableStateFlow<List<ScanDocument>>(emptyList())
    val documents: StateFlow<List<ScanDocument>> = _documents

    fun load() {
        viewModelScope.launch {
            repo.listDocuments()
                .filter { it.pages.isEmpty() }
                .forEach { repo.deleteDocument(it.id) }
            _documents.value = repo.listDocuments()
        }
    }

    fun createDocument(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val doc = repo.createDocument()
            onCreated(doc.id)
        }
    }

    fun rename(id: String, title: String) {
        viewModelScope.launch {
            repo.rename(id, title)
            load()
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repo.deleteDocument(id)
            load()
        }
    }

    fun pageFile(docId: String, page: ScanPage): File = repo.pageFile(docId, page)
}