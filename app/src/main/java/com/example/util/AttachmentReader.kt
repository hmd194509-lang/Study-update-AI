package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.data.model.AttachmentPayload
import com.example.data.model.NotebookSource
import com.example.data.model.NotebookSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object AttachmentReader {
    private const val MAX_IMAGE_DIM = 1600
    const val MAX_PDF_BYTES = 1024L * 1024L * 1024L // 1 GB maximum PDF upload
    const val INLINE_PDF_THRESHOLD_BYTES = 15L * 1024L * 1024L // 15 MB inline cap
    private const val MAX_TEXT_CHARS = 80_000
    private const val STREAM_BUFFER_BYTES = 64 * 1024

    private val TEXT_EXTENSIONS = listOf(
        ".txt", ".md", ".csv", ".json", ".py", ".js", ".ts", ".tsx", ".jsx",
        ".kt", ".java", ".c", ".cpp", ".h", ".html", ".css", ".xml", ".sql", ".yaml", ".yml"
    )

    suspend fun readImage(context: Context, uri: Uri): Result<AttachmentPayload.ImageAttachment> =
        withContext(Dispatchers.IO) {
            try {
                val name = resolveFileName(context, uri) ?: "image_${System.currentTimeMillis()}.jpg"
                val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(IllegalStateException("Cannot open selected image."))

                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
                var sampleSize = 1
                while (boundsOpts.outWidth / sampleSize > MAX_IMAGE_DIM ||
                    boundsOpts.outHeight / sampleSize > MAX_IMAGE_DIM
                ) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                    ?: return@withContext Result.failure(IllegalStateException("Unable to decode image."))

                val jpegStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, jpegStream)
                val jpegBytes = jpegStream.toByteArray()

                Result.success(
                    AttachmentPayload.ImageAttachment(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        bitmap = bitmap,
                        jpegBytes = jpegBytes
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun readDocument(context: Context, uri: Uri): Result<AttachmentPayload> =
        withContext(Dispatchers.IO) {
            try {
                val name = resolveFileName(context, uri) ?: "document"
                val mime = context.contentResolver.getType(uri).orEmpty()
                val lower = name.lowercase()

                if (mime == "application/pdf" || lower.endsWith(".pdf")) {
                    val dir = File(context.cacheDir, "chat_pdfs").apply { mkdirs() }
                    val fileId = UUID.randomUUID().toString()
                    val outFile = File(dir, "$fileId.pdf")
                    val copiedBytes = streamUriToFileWithLimit(context, uri, outFile, MAX_PDF_BYTES)
                        .getOrElse { err -> return@withContext Result.failure(err) }

                    val inlineBytes = if (copiedBytes <= INLINE_PDF_THRESHOLD_BYTES) {
                        outFile.readBytes()
                    } else {
                        ByteArray(0)
                    }
                    return@withContext Result.success(
                        AttachmentPayload.PdfAttachment(
                            id = fileId,
                            name = name,
                            bytes = inlineBytes,
                            localFilePath = outFile.absolutePath,
                            sizeBytes = copiedBytes
                        )
                    )
                }

                if (mime.startsWith("image/")) {
                    return@withContext readImage(context, uri)
                }

                if (mime.startsWith("text/") || TEXT_EXTENSIONS.any { lower.endsWith(it) }) {
                    val text = context.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader(Charsets.UTF_8).readText()
                    } ?: return@withContext Result.failure(IllegalStateException("Cannot read document."))
                    return@withContext Result.success(
                        AttachmentPayload.TextDocumentAttachment(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            textContent = text.take(MAX_TEXT_CHARS)
                        )
                    )
                }

                Result.failure(
                    IllegalArgumentException("Unsupported file type ($name). Please select an image, PDF, or text/code file.")
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun savePdfForNotebook(context: Context, uri: Uri): Result<NotebookSource> =
        withContext(Dispatchers.IO) {
            try {
                val name = resolveFileName(context, uri) ?: "document_${System.currentTimeMillis()}.pdf"
                val mime = context.contentResolver.getType(uri).orEmpty()
                if (mime != "application/pdf" && !name.lowercase().endsWith(".pdf")) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Please pick a PDF file (.pdf).")
                    )
                }
                val dir = File(context.filesDir, "notebook_sources").apply { mkdirs() }
                val sourceId = "src_${UUID.randomUUID()}"
                val outFile = File(dir, "$sourceId.pdf")
                val totalBytes = streamUriToFileWithLimit(context, uri, outFile, MAX_PDF_BYTES)
                    .getOrElse { err -> return@withContext Result.failure(err) }

                Result.success(
                    NotebookSource(
                        id = sourceId,
                        title = name,
                        type = NotebookSourceType.PDF,
                        sizeBytes = totalBytes,
                        localFilePath = outFile.absolutePath
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun loadNotebookAttachments(sources: List<NotebookSource>): List<AttachmentPayload> =
        withContext(Dispatchers.IO) {
            sources.mapNotNull { src ->
                when (src.type) {
                    NotebookSourceType.PDF -> {
                        val file = File(src.localFilePath)
                        if (file.exists() && file.length() > 0L && file.length() <= MAX_PDF_BYTES) {
                            val len = file.length()
                            AttachmentPayload.PdfAttachment(
                                id = src.id,
                                name = src.title,
                                bytes = if (len <= INLINE_PDF_THRESHOLD_BYTES) file.readBytes() else ByteArray(0),
                                localFilePath = file.absolutePath,
                                sizeBytes = len
                            )
                        } else null
                    }
                    NotebookSourceType.NOTE -> {
                        if (src.textContent.isNotBlank()) {
                            AttachmentPayload.TextDocumentAttachment(
                                id = src.id,
                                name = src.title,
                                textContent = src.textContent.take(MAX_TEXT_CHARS)
                            )
                        } else null
                    }
                }
            }
        }

    fun renderSamplePdfPages(pdfPath: String, maxPages: Int = 8): List<Bitmap> {
        val file = File(pdfPath)
        if (!file.exists()) return emptyList()
        val bitmaps = mutableListOf<Bitmap>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val total = renderer.pageCount
            if (total <= 0) return emptyList()
            val indices = if (total <= maxPages) {
                (0 until total).toList()
            } else {
                (0 until maxPages).map { i ->
                    ((i.toFloat() / (maxPages - 1)) * (total - 1)).toInt().coerceIn(0, total - 1)
                }.distinct()
            }
            for (idx in indices) {
                renderer.openPage(idx).use { page ->
                    val targetW = 1000
                    val targetH = ((page.height.toFloat() / page.width.coerceAtLeast(1)) * targetW)
                        .toInt().coerceIn(400, 1400)
                    val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bmp)
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { renderer?.close() }
            runCatching { pfd?.close() }
        }
        return bitmaps
    }

    fun deleteStoredNotebookSourceFile(source: NotebookSource) {
        if (source.localFilePath.isNotBlank()) {
            runCatching { File(source.localFilePath).delete() }
        }
    }

    private fun streamUriToFileWithLimit(
        context: Context,
        uri: Uri,
        destFile: File,
        maxBytes: Long
    ): Result<Long> {
        return try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalStateException("Cannot open selected PDF."))
            var total = 0L
            input.use { stream ->
                FileOutputStream(destFile).use { out ->
                    val buffer = ByteArray(STREAM_BUFFER_BYTES)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read == -1) break
                        total += read
                        if (total > maxBytes) {
                            runCatching { destFile.delete() }
                            return Result.failure(
                                IllegalArgumentException("PDF exceeds maximum 1 GB size limit.")
                            )
                        }
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                }
            }
            if (total <= 0L) {
                runCatching { destFile.delete() }
                return Result.failure(IllegalArgumentException("Selected PDF file is empty."))
            }
            Result.success(total)
        } catch (e: Exception) {
            runCatching { destFile.delete() }
            Result.failure(e)
        }
    }

    private fun resolveFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }
}
