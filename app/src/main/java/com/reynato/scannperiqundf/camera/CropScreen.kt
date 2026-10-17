package com.reynato.scannperiqundf.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.reynato.scannperiqundf.data.DocumentRepository
import com.reynato.scannperiqundf.imaging.ImageEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

private data class CropRect(val l: Float, val t: Float, val r: Float, val b: Float)

@Composable
fun CropScreen(
    docId: String,
    photoName: String,
    onAccepted: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { DocumentRepository(context.applicationContext) }
    val file = remember(photoName) { File(context.cacheDir, photoName) }

    var rotation by remember { mutableIntStateOf(0) }
    var processing by remember { mutableStateOf(false) }
    var crop by remember(rotation) { mutableStateOf(CropRect(0.05f, 0.05f, 0.95f, 0.95f)) }

    val base by produceState<Bitmap?>(initialValue = null, file) {
        value = withContext(Dispatchers.IO) { ImageEdit.load(file, 1600) }
    }
    val display = remember(base, rotation) { base?.let { ImageEdit.rotate(it, rotation) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val bmp = display
            if (bmp == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                CropCanvas(bitmap = bmp, crop = crop, onCropChange = { crop = it })
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                enabled = !processing,
                onClick = {
                    file.delete()
                    onCancel()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Cancelar") }

            OutlinedButton(
                enabled = !processing && display != null,
                onClick = { rotation = (rotation + 90) % 360 },
                modifier = Modifier.weight(1f)
            ) { Text("Rotar") }

            Button(
                enabled = !processing && display != null,
                onClick = {
                    processing = true
                    val c = crop
                    val rot = rotation
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching {
                                val full = ImageEdit.load(file, 3000) ?: error("Sin imagen")
                                val result = ImageEdit.crop(
                                    ImageEdit.rotate(full, rot), c.l, c.t, c.r, c.b
                                )
                                val out = File(context.cacheDir, "${UUID.randomUUID()}.jpg")
                                ImageEdit.save(result, out)
                                file.delete()
                                repo.addPageFromFile(docId, out)
                            }.isSuccess
                        }
                        processing = false
                        if (ok) onAccepted() else onCancel()
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Aceptar") }
        }
    }
}

@Composable
private fun CropCanvas(
    bitmap: Bitmap,
    crop: CropRect,
    onCropChange: (CropRect) -> Unit
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val currentCrop by rememberUpdatedState(crop)
    var active by remember { mutableIntStateOf(-1) }
    val touchRadius = with(LocalDensity.current) { 40.dp.toPx() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cw = constraints.maxWidth.toFloat()
        val ch = constraints.maxHeight.toFloat()
        val scale = min(cw / bitmap.width, ch / bitmap.height)
        val dw = bitmap.width * scale
        val dh = bitmap.height * scale
        val ox = (cw - dw) / 2f
        val oy = (ch - dh) / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap, cw, ch) {
                    detectDragGestures(
                        onDragStart = { p ->
                            val c = currentCrop
                            val corners = listOf(
                                Offset(ox + c.l * dw, oy + c.t * dh),
                                Offset(ox + c.r * dw, oy + c.t * dh),
                                Offset(ox + c.l * dw, oy + c.b * dh),
                                Offset(ox + c.r * dw, oy + c.b * dh)
                            )
                            val nearest = corners.indices.minByOrNull {
                                (corners[it] - p).getDistance()
                            } ?: 0
                            active = if ((corners[nearest] - p).getDistance() <= touchRadius) {
                                nearest
                            } else if (p.x in corners[0].x..corners[3].x &&
                                p.y in corners[0].y..corners[3].y
                            ) {
                                4
                            } else {
                                -1
                            }
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            val c = currentCrop
                            val dx = drag.x / dw
                            val dy = drag.y / dh
                            val minSize = 0.1f
                            onCropChange(
                                when (active) {
                                    0 -> c.copy(
                                        l = (c.l + dx).coerceIn(0f, c.r - minSize),
                                        t = (c.t + dy).coerceIn(0f, c.b - minSize)
                                    )
                                    1 -> c.copy(
                                        r = (c.r + dx).coerceIn(c.l + minSize, 1f),
                                        t = (c.t + dy).coerceIn(0f, c.b - minSize)
                                    )
                                    2 -> c.copy(
                                        l = (c.l + dx).coerceIn(0f, c.r - minSize),
                                        b = (c.b + dy).coerceIn(c.t + minSize, 1f)
                                    )
                                    3 -> c.copy(
                                        r = (c.r + dx).coerceIn(c.l + minSize, 1f),
                                        b = (c.b + dy).coerceIn(c.t + minSize, 1f)
                                    )
                                    4 -> {
                                        val w = c.r - c.l
                                        val h = c.b - c.t
                                        val nl = (c.l + dx).coerceIn(0f, 1f - w)
                                        val nt = (c.t + dy).coerceIn(0f, 1f - h)
                                        CropRect(nl, nt, nl + w, nt + h)
                                    }
                                    else -> c
                                }
                            )
                        },
                        onDragEnd = { active = -1 },
                        onDragCancel = { active = -1 }
                    )
                }
        ) {
            drawImage(
                image = image,
                dstOffset = IntOffset(ox.roundToInt(), oy.roundToInt()),
                dstSize = IntSize(dw.roundToInt(), dh.roundToInt())
            )

            val l = ox + crop.l * dw
            val t = oy + crop.t * dh
            val r = ox + crop.r * dw
            val b = oy + crop.b * dh

            val sombra = Color.Black.copy(alpha = 0.55f)
            drawRect(sombra, Offset(ox, oy), Size(dw, t - oy))
            drawRect(sombra, Offset(ox, b), Size(dw, oy + dh - b))
            drawRect(sombra, Offset(ox, t), Size(l - ox, b - t))
            drawRect(sombra, Offset(r, t), Size(ox + dw - r, b - t))

            drawRect(Color.White, Offset(l, t), Size(r - l, b - t), style = Stroke(width = 3f))
            listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach {
                drawCircle(Color.White, radius = 14f, center = it)
            }
        }
    }
}