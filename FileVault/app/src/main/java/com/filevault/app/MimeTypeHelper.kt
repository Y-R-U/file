package com.filevault.app

object MimeTypeHelper {

    fun getMimeType(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return when (ext) {
            // Images
            "jpg", "jpeg" -> "image/jpeg"
            "png"         -> "image/png"
            "gif"         -> "image/gif"
            "webp"        -> "image/webp"
            "bmp"         -> "image/bmp"
            "svg"         -> "image/svg+xml"
            "heic", "heif" -> "image/heic"
            // Video
            "mp4"         -> "video/mp4"
            "mkv"         -> "video/x-matroska"
            "avi"         -> "video/x-msvideo"
            "mov"         -> "video/quicktime"
            "webm"        -> "video/webm"
            "3gp"         -> "video/3gpp"
            // Audio
            "mp3"         -> "audio/mpeg"
            "wav"         -> "audio/wav"
            "ogg"         -> "audio/ogg"
            "flac"        -> "audio/flac"
            "aac"         -> "audio/aac"
            "m4a"         -> "audio/mp4"
            // Documents
            "pdf"         -> "application/pdf"
            "doc"         -> "application/msword"
            "docx"        -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls"         -> "application/vnd.ms-excel"
            "xlsx"        -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt"         -> "application/vnd.ms-powerpoint"
            "pptx"        -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt"         -> "text/plain"
            "csv"         -> "text/csv"
            "html", "htm" -> "text/html"
            "xml"         -> "application/xml"
            "json"        -> "application/json"
            "md"          -> "text/markdown"
            // Archives
            "zip"         -> "application/zip"
            "rar"         -> "application/x-rar-compressed"
            "7z"          -> "application/x-7z-compressed"
            "tar"         -> "application/x-tar"
            "gz"          -> "application/gzip"
            // Code
            "java", "kt", "py", "js", "ts", "swift", "cpp", "c", "h" -> "text/plain"
            // APK
            "apk"         -> "application/vnd.android.package-archive"
            else          -> "application/octet-stream"
        }
    }

    fun getCategory(extension: String): FileCategory {
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif" -> FileCategory.IMAGE
            "mp4", "mkv", "avi", "mov", "webm", "3gp" -> FileCategory.VIDEO
            "mp3", "wav", "ogg", "flac", "aac", "m4a" -> FileCategory.AUDIO
            "pdf" -> FileCategory.PDF
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "md" -> FileCategory.DOCUMENT
            "zip", "rar", "7z", "tar", "gz" -> FileCategory.ARCHIVE
            else -> FileCategory.GENERIC
        }
    }
}

enum class FileCategory {
    IMAGE, VIDEO, AUDIO, PDF, DOCUMENT, ARCHIVE, GENERIC
}
