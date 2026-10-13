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
        pagesDir(doc.id) // asegura que existan las carpetas
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
}