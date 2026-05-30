package com.filevault.app

import java.io.File
import java.util.Date

data class VaultFile(
    val file: File,
    val name: String = file.name,
    val size: Long = file.length(),
    val lastModified: Date = Date(file.lastModified()),
    val mimeType: String = MimeTypeHelper.getMimeType(file.name),
    val extension: String = file.extension.lowercase(),
    var isSelected: Boolean = false
)
