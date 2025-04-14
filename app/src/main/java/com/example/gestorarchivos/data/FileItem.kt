package com.example.gestorarchivos.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val file: File,
    val isDirectory: Boolean = file.isDirectory,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val size: Long = if (isDirectory) 0 else file.length(),
    val lastModified: Long = file.lastModified(),
    val isHidden: Boolean = file.isHidden(),
    val extension: String = if (isDirectory) "" else file.extension.lowercase()
) {
    companion object {
        // Tipos de archivos admitidos
        const val TYPE_DIRECTORY = "directory"
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_JSON = "json"
        const val TYPE_XML = "xml"
        const val TYPE_OTHER = "other"

        // Extensiones por tipo
        val TEXT_EXTENSIONS = listOf("txt", "md", "log", "gradle", "properties", "kt", "java", "c", "cpp", "py", "js", "css", "html")
        val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        val JSON_EXTENSIONS = listOf("json")
        val XML_EXTENSIONS = listOf("xml")
    }

    // Método para obtener el tipo de archivo
    fun getFileType(): String {
        if (isDirectory) return TYPE_DIRECTORY

        return when (extension) {
            in TEXT_EXTENSIONS -> TYPE_TEXT
            in IMAGE_EXTENSIONS -> TYPE_IMAGE
            in JSON_EXTENSIONS -> TYPE_JSON
            in XML_EXTENSIONS -> TYPE_XML
            else -> TYPE_OTHER
        }
    }

    // Formato de fecha para mostrar en la UI
    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date(lastModified))
    }

    // Formato de tamaño para mostrar en la UI
    fun getFormattedSize(): String {
        if (isDirectory) return "-"

        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
}