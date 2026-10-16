package com.reynato.scannperiqundf.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reynato.scannperiqundf.ui.HomeViewModel
import java.nio.file.WatchEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDocument: (String) -> Unit,
    onNewScan: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val  documents by viewModel.documents.collectAsState()

    //Se carga la lista siempre que aparece la pantalla
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis escaneos") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {viewModel.createDocument (onNewScan)},
                text = {Text("Nuevo escaneo")},
                icon = { Icon(Icons.Default.Add, contentDescription = null) }
            )
        }
    ) { padding ->
        if (documents.isEmpty()){
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ){
                Text("Aún no tienes escaneos")
            }
        } else {
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(documents, key = { it.id}) {doc ->
                    ListItem(
                        headlineContent = { Text(doc.title)},
                        supportingContent = {
                            Text("${doc.pages.size} páginas · ${formato.format(Date(doc.updatedAt))}")
                        },
                        modifier = Modifier.clickable{onOpenDocument(doc.id)}
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}