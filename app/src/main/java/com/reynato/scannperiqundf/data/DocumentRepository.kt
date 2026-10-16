package com.reynato.scannperiqundf.data

import ScanDocument
import ScanFilter
import ScanPage
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.net.Uri

class DocumentRepository(private val context: Context) {

    private val rootDir: File
        get() = File(context.filesDir, "documents").also { it.mkdirs() }

    private fun docDir(id: String) = File(rootDir, id)

    private fun metaFile(id: String) = File(docDir(id), "meta.json")

    fun pagesDir(id: String) = File(docDir(id), "pages").also { it.mkdirs() }

    suspend fun createDocument(): ScanDocument = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(now))
        val doc = ScanDocument(
            id = UUID.randomUUID().toString(),
            title = "Escaneo $fecha",
            createdAt = now,
            updatedAt = now,
            pages = emptyList()
        )
        save(doc)
        doc
    }

    suspend fun save(doc: ScanDocument) = withContext(Dispatchers.IO) {
        pagesDir(doc.id)
        val pagesJson = JSONArray()
        doc.pages.forEach { page ->
            pagesJson.put(
                JSONObject()
                    .put("id", page.id)
                    .put("fileName", page.fileName)
                    .put("filter", page.filter.name)
            )
        }
        val json = JSONObject()
            .put("id", doc.id)
            .put("title", doc.title)
            .put("createdAt", doc.createdAt)
            .put("updatedAt", doc.updatedAt)
            .put("pages", pagesJson)
        metaFile(doc.id).writeText(json.toString())
    }

    suspend fun getDocument(id: String): ScanDocument? = withContext(Dispatchers.IO) {
        val file = metaFile(id)
        if (!file.exists()) null else parse(file.readText())
    }

    suspend fun listDocuments(): List<ScanDocument> = withContext(Dispatchers.IO) {
        (rootDir.listFiles() ?: emptyArray())
            .filter { it.isDirectory }
            .mapNotNull { dir ->
                runCatching { parse(File(dir, "meta.json").readText()) }.getOrNull()
            }
            .sortedByDescending { it.updatedAt }
    }

    private fun parse(text: String): ScanDocument {
        val obj = JSONObject(text)
        val arr = obj.getJSONArray("pages")
        val pages = List(arr.length()) { i ->
            val p = arr.getJSONObject(i)
            ScanPage(
                id = p.getString("id"),
                fileName = p.getString("fileName"),
                filter = ScanFilter.valueOf(p.getString("filter"))
            )
        }
        return ScanDocument(
            id = obj.getString("id"),
            title = obj.getString("title"),
            createdAt = obj.getLong("createdAt"),
            updatedAt = obj.getLong("updatedAt"),
            pages = pages
        )
    }

    fun pageFile(docId: String, page: ScanPage) = File(pagesDir(docId), page.fileName)
    suspend fun addPageFromUri(docId: String, uri: Uri) = withContext(Dispatchers.IO) {
        val doc = getDocument(docId) ?: return@withContext
        val pageId = UUID.randomUUID().toString()
        val fileName = "$pageId.jpg"
        val target = File(pagesDir(docId), fileName)
        val input = context.contentResolver.openInputStream(uri) ?: return@withContext
        input.use { entrada ->
            target.outputStream().use { salida -> entrada.copyTo(salida) }
        }
        save(
            doc.copy(
                pages = doc.pages + ScanPage(id = pageId, fileName = fileName),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addPageFromFile(docId: String, source: File) = withContext(Dispatchers.IO) {
        val doc = getDocument(docId) ?: return@withContext
        val pageId = UUID.randomUUID().toString()
        val fileName = "$pageId.jpg"
        source.copyTo(File(pagesDir(docId), fileName), overwrite = true)
        source.delete()
        save(
            doc.copy(
                pages = doc.pages + ScanPage(id = pageId, fileName = fileName),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun reorderPages(docId: String, from: Int, to: Int) = withContext(Dispatchers.IO) {
        val doc = getDocument(docId) ?: return@withContext
        if (from !in doc.pages.indices || to !in doc.pages.indices) return@withContext
        val lista = doc.pages.toMutableList()
        lista.add(to, lista.removeAt(from))
        save(doc.copy(pages = lista, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setFilter(docId: String, pageId: String, filter: ScanFilter) =
        withContext(Dispatchers.IO) {
            val doc = getDocument(docId) ?: return@withContext
            val nuevas = doc.pages.map { if (it.id == pageId) it.copy(filter = filter) else it }
            save(doc.copy(pages = nuevas, updatedAt = System.currentTimeMillis()))
        }

    suspend fun deletePage(docId: String, pageId: String) = withContext(Dispatchers.IO) {
        val doc = getDocument(docId) ?: return@withContext
        val pagina = doc.pages.firstOrNull { it.id == pageId } ?: return@withContext
        File(pagesDir(docId), pagina.fileName).delete()
        save(
            doc.copy(
                pages = doc.pages.filter { it.id != pageId },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun rename(docId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val doc = getDocument(docId) ?: return@withContext
        save(doc.copy(title = newTitle.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteDocument(docId: String) = withContext(Dispatchers.IO) {
        docDir(docId).deleteRecursively()
    }
}