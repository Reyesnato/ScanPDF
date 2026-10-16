package com.reynato.scannperiqundf.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reynato.scannperiqundf.data.DocumentRepository
import com.reynato.scannperiqundf.data.ScanDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DocumentRepository(app)

    private val _documents = MutableStateFlow<List<ScanDocument>>(emptyList())
    val documents: StateFlow<List<ScanDocument>> = _documents

    fun load() {
        viewModelScope.launch {
            _documents.value = repo.listDocuments()
        }
    }

    fun createDocument(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val doc = repo.createDocument()
            onCreated(doc.id)
        }
    }
}