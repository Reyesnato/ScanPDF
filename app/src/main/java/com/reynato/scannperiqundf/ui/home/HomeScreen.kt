package com.reynato.scannperiqundf.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.reynato.scannperiqundf.imaging.Filters
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reynato.scannperiqundf.data.ScanDocument
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

    LaunchedEffect(Unit) { viewModel.load() }
    var menuDocId by remember { mutableStateOf<String?>(null) }
    var renameDoc by remember { mutableStateOf<ScanDocument?>(null) }
    var deleteDoc by remember { mutableStateOf<ScanDocument?>(null) }

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
                        headlineContent = { Text(doc.title) },
                        supportingContent = {
                            Text("${doc.pages.size} páginas · ${formato.format(Date(doc.updatedAt))}")
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                doc.pages.firstOrNull()?.let { first ->
                                    AsyncImage(
                                        model = viewModel.pageFile(doc.id, first),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        colorFilter = Filters.matrix(first.filter)?.let {
                                            ColorFilter.colorMatrix(ColorMatrix(it))
                                        },
                                        modifier = Modifier
                                            .size(width = 48.dp, height = 64.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                }
                                Box {
                                    IconButton(onClick = { menuDocId = doc.id }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                                    }
                                    DropdownMenu(
                                        expanded = menuDocId == doc.id,
                                        onDismissRequest = { menuDocId = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Renombrar") },
                                            onClick = {
                                                menuDocId = null
                                                renameDoc = doc
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Eliminar") },
                                            onClick = {
                                                menuDocId = null
                                                deleteDoc = doc
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.clickable { onOpenDocument(doc.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    renameDoc?.let { doc ->
        var texto by remember(doc.id) { mutableStateOf(doc.title) }
        AlertDialog(
            onDismissRequest = { renameDoc = null },
            title = { Text("Renombrar documento") },
            text = {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = texto.isNotBlank(),
                    onClick = {
                        viewModel.rename(doc.id, texto)
                        renameDoc = null
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { renameDoc = null }) { Text("Cancelar") }
            }
        )
    }

    deleteDoc?.let { doc ->
        AlertDialog(
            onDismissRequest = { deleteDoc = null },
            title = { Text("Eliminar documento") },
            text = { Text("Se borrará \"${doc.title}\" con todas sus páginas. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(doc.id)
                        deleteDoc = null
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deleteDoc = null }) { Text("Cancelar") }
            }
        )
    }
}