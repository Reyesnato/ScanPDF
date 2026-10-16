package com.reynato.scannperiqundf.ui.editor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import com.reynato.scannperiqundf.data.ScanFilter
import com.reynato.scannperiqundf.data.ScanPage
import com.reynato.scannperiqundf.imaging.Filters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    docId: String,
    onTakePhotos: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val document by viewModel.document.collectAsState()
    val gridState = rememberLazyGridState()
    val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
        viewModel.movePage(from.index, to.index)
    }
    var selectedPage by remember { mutableStateOf<ScanPage?>(null) }
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            viewModel.exportPdf(docId, uri) { ok ->
                Toast.makeText(
                    context,
                    if (ok) "PDF guardado" else "No se pudo guardar el PDF",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    LaunchedEffect(docId) { viewModel.load(docId) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addImages(docId, uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(document?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(
                        enabled = document?.pages?.isNotEmpty() == true,
                        onClick = {
                            val nombre = (document?.title ?: "Escaneo")
                                .replace("/", "-")
                                .replace(":", "-")
                            exportLauncher.launch("$nombre.pdf")
                        }
                    ) {
                        Text("Exportar PDF")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onTakePhotos, modifier = Modifier.weight(1f)) {
                    Text("Tomar fotos")
                }
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Importar")
                }
            }
        }
    ) { padding ->
        val doc = document
        if (doc == null || doc.pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Este documento aún no tiene páginas")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(doc.pages, key = { it.id }) { page ->
                    val index = doc.pages.indexOf(page) + 1
                    ReorderableItem(reorderState, key = page.id) { isDragging ->
                        Card(
                            onClick = { selectedPage = page },
                            modifier = Modifier.longPressDraggableHandle(
                                onDragStopped = { viewModel.saveOrder() }
                            )
                        ) {
                            Column {
                                AsyncImage(
                                    model = viewModel.pageFile(docId, page),
                                    contentDescription = "Página $index",
                                    contentScale = ContentScale.Crop,
                                    colorFilter = Filters.matrix(page.filter)?.let {
                                        ColorFilter.colorMatrix(ColorMatrix(it))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 4f)
                                )
                                Text(
                                    if (isDragging) "Moviendo…" else "Página $index",
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPage?.let { page ->
        AlertDialog(
            onDismissRequest = { selectedPage = null },
            title = { Text("Filtro de la página") },
            text = {
                Column {
                    ScanFilter.entries.forEach { filtro ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setFilter(docId, page.id, filtro)
                                    selectedPage = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = page.filter == filtro, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(Filters.label(filtro))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPage = null }) { Text("Cerrar") }
            }
        )
    }
}