package com.example.gestorarchivos.data

import android.content.Context
import android.os.Environment
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

class FileRepository(private val context: Context) {

    private val recentFileDao = FileDatabase.getDatabase(context).recentFileDao()

    // Obtener archivos recientes
    val recentFiles: LiveData<List<RecentFile>> = recentFileDao.getRecentFiles()

    // Obtener archivos favoritos
    val favoriteFiles: LiveData<List<RecentFile>> = recentFileDao.getFavoriteFiles()

    // Obtener archivos en un directorio
    suspend fun getFilesInDirectory(directory: File): List<FileItem> = withContext(Dispatchers.IO) {
        val files = directory.listFiles() ?: emptyArray()
        val result = files.map { FileItem(it) }
        return@withContext result.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    // Agregar a recientes
    suspend fun addToRecent(fileItem: FileItem) = withContext(Dispatchers.IO) {
        val recentFile = RecentFile(
            path = fileItem.path,
            name = fileItem.name,
            isDirectory = fileItem.isDirectory,
            extension = fileItem.extension
        )
        recentFileDao.insert(recentFile)
    }

    // Mover un archivo
    suspend fun moveFile(sourceFile: File, targetDir: File): FileItem = withContext(Dispatchers.IO) {
        val targetFile = File(targetDir, sourceFile.name)

        // Si el destino ya existe, añadir un sufijo al nombre
        var finalTargetFile = targetFile
        var counter = 1

        while (finalTargetFile.exists()) {
            val fileName = sourceFile.nameWithoutExtension
            val extension = if (sourceFile.extension.isNotEmpty()) ".${sourceFile.extension}" else ""
            finalTargetFile = File(targetDir, "${fileName}_${counter}${extension}")
            counter++
        }

        // Mover el archivo (primero copiar, luego eliminar el original)
        val result = sourceFile.renameTo(finalTargetFile)

        if (!result) {
            // Si el rename directo falla, intentar copiar y eliminar
            copyFile(sourceFile, targetDir)
            deleteFile(sourceFile)
        }

        return@withContext FileItem(finalTargetFile)
    }

    // Obtener la ruta para breadcrumbs
    fun getPathSegments(path: String): List<Pair<String, String>> {
        val segments = mutableListOf<Pair<String, String>>()
        val file = File(path)
        var current = file

        // Añadir el archivo/directorio actual
        segments.add(Pair(current.absolutePath, current.name))

        // Añadir todos los directorios padre
        while (current.parent != null) {
            current = current.parentFile!!
            segments.add(0, Pair(current.absolutePath, current.name))
        }

        return segments
    }arcar/desmarcar como favorito
    suspend fun toggleFavorite(path: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        recentFileDao.updateFavorite(path, isFavorite)
    }

    // Limpiar historial
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        recentFileDao.clearHistory()
    }

    // Obtener directorio de almacenamiento interno
    fun getInternalStorageDirectory(): File {
        return context.filesDir
    }

    // Obtener directorio de almacenamiento externo (si está disponible)
    fun getExternalStorageDirectory(): File? {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            Environment.getExternalStorageDirectory()
        } else {
            null
        }
    }

    // Leer contenido de un archivo de texto
    suspend fun readTextFile(file: File): String = withContext(Dispatchers.IO) {
        return@withContext file.readText()
    }

    // Crear un archivo o directorio
    suspend fun createFile(parentDir: File, name: String, isDirectory: Boolean): FileItem = withContext(Dispatchers.IO) {
        val newFile = File(parentDir, name)

        if (isDirectory) {
            newFile.mkdir()
        } else {
            newFile.createNewFile()
        }

        return@withContext FileItem(newFile)
    }

    // Renombrar un archivo o directorio
    suspend fun renameFile(file: File, newName: String): FileItem = withContext(Dispatchers.IO) {
        val newFile = File(file.parentFile, newName)
        file.renameTo(newFile)
        return@withContext FileItem(newFile)
    }

    // Eliminar un archivo o directorio
    suspend fun deleteFile(file: File): Boolean = withContext(Dispatchers.IO) {
        // Si es un directorio, eliminar todos los archivos dentro
        if (file.isDirectory) {
            file.listFiles()?.forEach { it.deleteRecursively() }
        }
        return@withContext file.delete()
    }

    // Copiar un archivo
    suspend fun copyFile(sourceFile: File, targetDir: File): FileItem = withContext(Dispatchers.IO) {
        val targetFile = File(targetDir, sourceFile.name)

        if (sourceFile.isDirectory) {
            // Crear directorio destino
            targetFile.mkdir()

            // Copiar contenidos recursivamente
            sourceFile.listFiles()?.forEach { childFile ->
                copyFile(childFile, targetFile)
            }
        } else {
            // Copiar archivo usando FileChannel (más eficiente para archivos grandes)
            var sourceChannel: FileChannel? = null
            var destChannel: FileChannel? = null

            try {
                sourceChannel = FileInputStream(sourceFile).channel
                destChannel = FileOutputStream(targetFile).channel
                destChannel.transferFrom(sourceChannel, 0, sourceChannel.size())
            } finally {
                sourceChannel?.close()
                destChannel?.close()
            }
        }

        return@withContext FileItem(targetFile)
    }

// M