package com.filevault.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileRepository(context: Context) {

    val vaultDir: File = File(context.filesDir, "vault").also { it.mkdirs() }

    /** Import a file from a URI into the vault. Returns the new VaultFile or null on failure. */
    fun importFile(context: Context, uri: Uri): VaultFile? {
        return try {
            val contentResolver = context.contentResolver
            val displayName = getFileNameFromUri(context, uri) ?: "imported_file_${System.currentTimeMillis()}"

            // Resolve name conflicts
            val destFile = resolveConflict(File(vaultDir, displayName))

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }

            VaultFile(destFile)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /** Import multiple files at once. Returns list of successfully imported VaultFiles. */
    fun importFiles(context: Context, uris: List<Uri>): List<VaultFile> {
        return uris.mapNotNull { importFile(context, it) }
    }

    /** Delete a file from the vault. */
    fun deleteFile(vaultFile: VaultFile): Boolean {
        return vaultFile.file.delete()
    }

    /** Delete multiple files. */
    fun deleteFiles(files: List<VaultFile>): Int {
        return files.count { deleteFile(it) }
    }

    /** List all files in the vault, sorted by the given sort order. */
    fun listFiles(sortOrder: SortOrder = SortOrder.DATE_DESC): List<VaultFile> {
        val files = vaultDir.listFiles()
            ?.filter { it.isFile }
            ?.map { VaultFile(it) }
            ?: emptyList()

        return when (sortOrder) {
            SortOrder.NAME_ASC   -> files.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC  -> files.sortedByDescending { it.name.lowercase() }
            SortOrder.DATE_ASC   -> files.sortedBy { it.lastModified }
            SortOrder.DATE_DESC  -> files.sortedByDescending { it.lastModified }
            SortOrder.SIZE_ASC   -> files.sortedBy { it.size }
            SortOrder.SIZE_DESC  -> files.sortedByDescending { it.size }
            SortOrder.TYPE_ASC   -> files.sortedBy { it.extension }
        }
    }

    /** Get vault storage info */
    fun getStorageInfo(): StorageInfo {
        val files = vaultDir.listFiles()?.filter { it.isFile } ?: emptyList()
        val totalBytes = files.sumOf { it.length() }
        return StorageInfo(fileCount = files.size, totalBytes = totalBytes)
    }

    private fun resolveConflict(file: File): File {
        if (!file.exists()) return file
        val name = file.nameWithoutExtension
        val ext = if (file.extension.isNotEmpty()) ".${file.extension}" else ""
        var counter = 1
        var candidate: File
        do {
            candidate = File(file.parent, "$name ($counter)$ext")
            counter++
        } while (candidate.exists())
        return candidate
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        // Try content resolver query first
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        // Fallback to path
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}

data class StorageInfo(val fileCount: Int, val totalBytes: Long) {
    fun formattedSize(): String = formatBytes(totalBytes)
    fun summary(): String = "$fileCount file${if (fileCount != 1) "s" else ""} · ${formattedSize()}"
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}

enum class SortOrder {
    NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, SIZE_ASC, SIZE_DESC, TYPE_ASC
}
