package com.example.gestorarchivos.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestorarchivos.data.FileItem
import com.example.gestorarchivos.data.FileRepository
import com.example.gestorarchivos.data.RecentFile
import kotlinx.coroutines.launch
import java.io.File

class FileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRepository(application)

    // Directorio actual
    private val _currentDirectory = MutableLiveData<File>()
    val currentDirectory: LiveData<File> = _currentDirectory

    // Lista de archivos en el directorio actual
    private val _files = MutableLiveData<List<FileItem>>()
    val files: LiveData<List<FileItem>> = _files

    // Ruta de navegación (breadcrumbs)
    private val _pathSegments = MutableLiveData<List<Pair<String, String>>>()
    val pathSegments: LiveData<List<Pair<String, String>>> = _pathSegments

    // Archivos recientes
    val recentFiles: LiveData<List<RecentFile>> = repository.recentFiles

    // Archivos favoritos
    val favoriteFiles: LiveData<List<RecentFile>> = repository.favoriteFiles

    // Estado de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Inicializar con el directorio raíz
    init {
        navigateToDirectory(repository.getInternalStorageDirectory())
    }

    // Navegar a un directorio
    fun navigateToDirectory(directory: File) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val files = repository.getFilesInDirectory(directory)
                _files.value = files
                _currentDirectory.value = directory
                _pathSegments.value = repository.getPathSegments(directory.absolutePath)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error al acceder al directorio: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Navegar hacia arriba (directorio padre)
    fun navigateUp() {
        _currentDirectory.value?.parentFile?.let { parent ->
            navigateToDirectory(parent)
        }
    }

    // Abrir un archivo
    fun openFile(fileItem: FileItem) {
        if (fileItem.isDirectory) {
            navigateToDirectory(fileItem.file)
        } else {
            addToRecent(fileItem)
        }
    }

    // Añadir a recientes
    fun addToRecent(fileItem: FileItem) {
        viewModelScope.launch {
            repository.addToRecent(fileItem)
        }
    }

    // Marcar/desmarcar como favorito
    fun toggleFavorite(path: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(path, isFavorite)
        }
    }

    // Limpiar historial
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Crear un nuevo archivo o directorio
    fun createFile(name: String, isDirectory: Boolean) {
        viewModelScope.launch {
            try {
                currentDirectory.value?.let { currentDir ->
                    repository.createFile(currentDir, name, isDirectory)
                    // Refrescar la lista de archivos
                    navigateToDirectory(currentDir)
                }
            } catch (e: Exception) {
                _error.value = "Error al crear archivo: ${e.message}"
            }
        }
    }

    // Renombrar un archivo
    fun renameFile(fileItem: FileItem, newName: String) {
        viewModelScope.launch {
            try {
                repository.renameFile(fileItem.file, newName)
                // Refrescar la lista de archivos
                currentDirectory.value?.let { currentDir ->
                    navigateToDirectory(currentDir)
                }
            } catch (e: Exception) {
                _error.value = "Error al renombrar archivo: ${e.message}"
            }
        }
    }

    // Eliminar un archivo
    fun deleteFile(fileItem: FileItem) {
        viewModelScope.launch {
            try {
                repository.deleteFile(fileItem.file)
                // Refrescar la lista de archivos
                currentDirectory.value?.let { currentDir ->
                    navigateToDirectory(currentDir)
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar archivo: ${e.message}"
            }
        }
    }

    // Copiar un archivo
    fun copyFile(fileItem: FileItem, targetDirectory: File) {
        viewModelScope.launch {
            try {
                repository.copyFile(fileItem.file, targetDirectory)
                // Si estamos viendo el directorio destino, refrescar
                if (currentDirectory.value?.absolutePath == targetDirectory.absolutePath) {
                    navigateToDirectory(targetDirectory)
                }
            } catch (e: Exception) {
                _error.value = "Error al copiar archivo: ${e.message}"
            }
        }
    }

    // Mover un archivo
    fun moveFile(fileItem: FileItem, targetDirectory: File) {
        viewModelScope.launch {
            try {
                repository.moveFile(fileItem.file, targetDirectory)
                // Refrescar la lista de archivos del directorio actual
                currentDirectory.value?.let { currentDir ->
                    navigateToDirectory(currentDir)
                }
            } catch (e: Exception) {
                _error.value = "Error al mover archivo: ${e.message}"
            }
        }
    }

    // Obtener directorio de almacenamiento externo
    fun getExternalStorageDirectory(): File? {
        return repository.getExternalStorageDirectory()
    }

    // Obtener directorio de almacenamiento interno
    fun getInternalStorageDirectory(): File {
        return repository.getInternalStorageDirectory()
    }

    // Leer contenido de un archivo de texto
    suspend fun readTextFile(file: File): String {
        return repository.readTextFile(file)
    }

    // Limpiar mensaje de error
    fun clearError() {
        _error.value = null
    }
}