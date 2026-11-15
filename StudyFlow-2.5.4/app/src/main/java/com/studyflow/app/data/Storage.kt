package com.studyflow.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

/** A single background thread, so writes always land on disk in the order they were requested. */
val StorageDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor { r ->
    Thread(r, "studyflow-storage").apply { isDaemon = true }
}.asCoroutineDispatcher()

data class StoredAttachment(val name: String, val storedName: String)

object Storage {
    private const val FILE_NAME = "studyflow_data.json"
    private val lock = Any()

    private fun dataFile(ctx: Context) = File(ctx.filesDir, FILE_NAME)

    fun load(ctx: Context): AppData = synchronized(lock) {
        val file = dataFile(ctx)
        if (!file.exists()) return@synchronized initialData()
        val text = try {
            file.readText()
        } catch (e: Exception) {
            return@synchronized initialData()
        }
        val parsed = AppJson.parse(text)
        if (parsed == null) {
            // Never silently throw away a file we could not read: keep a copy next to it.
            try {
                file.copyTo(File(ctx.filesDir, "$FILE_NAME.corrupt"), overwrite = true)
            } catch (e: Exception) {
            }
            initialData()
        } else {
            parsed
        }
    }

    fun save(ctx: Context, data: AppData) {
        synchronized(lock) {
            val file = dataFile(ctx)
            val tmp = File(ctx.filesDir, "$FILE_NAME.tmp")
            tmp.writeText(AppJson.toJsonString(data))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
    }

    // ---- attachments --------------------------------------------------------------------

    fun attachmentsDir(ctx: Context): File = File(ctx.filesDir, "attachments").apply { mkdirs() }

    fun attachmentFile(ctx: Context, storedName: String): File = File(attachmentsDir(ctx), storedName)

    fun displayName(ctx: Context, uri: Uri): String {
        var name = "attachment"
        try {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx)?.let { name = it }
                }
            }
        } catch (e: Exception) {
        }
        return name
    }

    /** Copies the picked file into app storage so it stays available later. */
    fun copyAttachment(ctx: Context, uri: Uri): StoredAttachment? {
        val name = displayName(ctx, uri)
        val safe = name.replace(Regex("[^A-Za-z0-9._ -]"), "_")
        val stored = UUID.randomUUID().toString().take(8) + "_" + safe
        val dest = attachmentFile(ctx, stored)
        return try {
            val input = ctx.contentResolver.openInputStream(uri) ?: return null
            input.use { src -> dest.outputStream().use { out -> src.copyTo(out) } }
            StoredAttachment(name, stored)
        } catch (e: Exception) {
            dest.delete()
            null
        }
    }

    fun deleteAttachment(ctx: Context, storedName: String?) {
        if (storedName.isNullOrEmpty()) return
        try {
            attachmentFile(ctx, storedName).delete()
        } catch (e: Exception) {
        }
    }

    fun wipeAttachments(ctx: Context) {
        try {
            attachmentsDir(ctx).listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
        }
    }
}
