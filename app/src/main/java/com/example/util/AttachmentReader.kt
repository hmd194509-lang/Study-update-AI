package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.AttachmentPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

object AttachmentReader {
    private const val MAX_IMAGE_DIM = 1600
    private const val MAX_PDF_BYTES = 15 * 1024 * 1024
    private const val MAX_TEXT_CHARS = 80_000

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
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@withContext Result.failure(IllegalStateException("Cannot open PDF file."))
                    if (bytes.size > MAX_PDF_BYTES) {
                        return@withContext Result.failure(
                            IllegalArgumentException("PDF exceeds 15 MB limit.")
                        )
                    }
                    return@withContext Result.success(
                        AttachmentPayload.PdfAttachment(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            bytes = bytes
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
