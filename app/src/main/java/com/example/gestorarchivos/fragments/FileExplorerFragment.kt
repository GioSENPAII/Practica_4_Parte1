package com.example.gestorarchivos.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestorarchivos.R
import com.example.gestorarchivos.activities.FileViewerActivity
import com.example.gestorarchivos.adapters.BreadcrumbAdapter
import com.example.gestorarchivos.adapters.FileAdapter
import com.example.gestorarchivos.data.FileItem
import com.example.gestorarchivos.databinding.FragmentFileExplorerBinding
import com.example.gestorarchivos.viewmodels.FileViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File

class FileExplorerFragment : Fragment() {

    private var _binding: FragmentFileExplorerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FileViewModel by viewModels()
    private lateinit var fileAdapter: FileAdapter
    private lateinit var breadcrumbAdapter: BreadcrumbAdapter
    private var isListView = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileExplorerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerViews()
        setupObservers()
        setupListeners()
    }

    private fun setupToolbar() {
        // Configurar toolbar y menú
        binding.toolbar.title = getString(R.string.menu_explore)
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.file_explorer_menu, menu)

                // Configurar búsqueda
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem?.actionView as? SearchView

                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        if (!query.isNullOrBlank()) {
                            viewModel.searchFiles(query)
                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        // No buscamos con cada cambio para no sobrecargar
                        return false
                    }
                })

                searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                        return true
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        // Al cerrar la búsqueda, volvemos a mostrar los archivos normales
                        viewModel.refreshCurrentDirectory()
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_search -> {
                        // Ya está manejado por el SearchView
                        true
                    }
                    R.id.sort_name -> {
                        viewModel.sortFiles(FileViewModel.SortType.NAME)
                        true
                    }
                    R.id.sort_date -> {
                        viewModel.sortFiles(FileViewModel.SortType.DATE)
                        true
                    }
                    R.id.sort_size -> {
                        viewModel.sortFiles(FileViewModel.SortType.SIZE)
                        true
                    }
                    R.id.sort_type -> {
                        viewModel.sortFiles(FileViewModel.SortType.TYPE)
                        true
                    }
                    R.id.view_list -> {
                        if (!isListView) {
                            setListView()
                        }
                        true
                    }
                    R.id.view_grid -> {
                        if (isListView) {
                            setGridView()
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerViews() {
        // Configurar el RecyclerView de archivos
        fileAdapter = FileAdapter(
            onItemClick = { fileItem ->
                if (fileItem.isDirectory) {
                    viewModel.navigateToDirectory(fileItem.file)
                } else {
                    openFile(fileItem)
                }
            },
            onItemLongClick = { fileItem ->
                showContextMenu(binding.root, fileItem)
                true
            }
        )

        binding.rvFiles.apply {
            adapter = fileAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Configurar el RecyclerView de breadcrumbs
        breadcrumbAdapter = BreadcrumbAdapter { path ->
            viewModel.navigateToDirectory(File(path))
        }

        binding.rvBreadcrumbs.adapter = breadcrumbAdapter
    }

    private fun setupObservers() {
        // Observar cambios en el directorio actual
        viewModel.currentDirectory.observe(viewLifecycleOwner) { directory ->
            binding.toolbar.subtitle = directory.absolutePath
        }

        // Observar cambios en la lista de archivos
        viewModel.files.observe(viewLifecycleOwner) { files ->
            fileAdapter.submitList(files)
            binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        }

        // Observar cambios en los breadcrumbs
        viewModel.pathSegments.observe(viewLifecycleOwner) { segments ->
            breadcrumbAdapter.submitList(segments)
        }

        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observar errores
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupListeners() {
        // Configurar FAB para añadir
        binding.fabAdd.setOnClickListener {
            showCreateDialog()
        }
    }

    private fun openFile(fileItem: FileItem) {
        // Registrar el archivo en recientes
        viewModel.addToRecent(fileItem)

        // Iniciar la actividad del visor de archivos
        val intent = Intent(requireContext(), FileViewerActivity::class.java).apply {
            putExtra("file_path", fileItem.path)
        }
        startActivity(intent)
    }

    private fun showContextMenu(view: View, fileItem: FileItem) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.file_context_menu, popupMenu.menu)

        // Configurar elementos de menú según el tipo de archivo
        val menu = popupMenu.menu
        if (fileItem.isDirectory) {
            menu.findItem(R.id.action_open)?.title = getString(R.string.action_open)
        } else {
            menu.findItem(R.id.action_open)?.title = getString(R.string.action_open)
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_open -> {
                    if (fileItem.isDirectory) {
                        viewModel.navigateToDirectory(fileItem.file)
                    } else {
                        openFile(fileItem)
                    }
                    true
                }
                R.id.action_share -> {
                    if (!fileItem.isDirectory) {
                        shareFile(fileItem)
                    }
                    true
                }
                R.id.action_rename -> {
                    showRenameDialog(fileItem)
                    true
                }
                R.id.action_copy -> {
                    showDirectoryPicker(fileItem, isMove = false)
                    true
                }
                R.id.action_move -> {
                    showDirectoryPicker(fileItem, isMove = true)
                    true
                }
                R.id.action_delete -> {
                    showDeleteDialog(fileItem)
                    true
                }
                R.id.action_info -> {
                    showFileInfo(fileItem)
                    true
                }
                R.id.action_favorite -> {
                    viewModel.toggleFavorite(fileItem.path, true)
                    Snackbar.make(binding.root, "Añadido a favoritos", Snackbar.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun shareFile(fileItem: FileItem) {
        try {
            val uri = Uri.fromFile(fileItem.file)
            val intent = Intent(Intent.ACTION_SEND)
            val mimeType = getMimeType(fileItem.name)
            intent.setType(mimeType)
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(intent, "Compartir archivo"))
        } catch (e: Exception) {
            Snackbar.make(
                binding.root,
                "No se puede compartir este archivo: ${e.message}",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun showDirectoryPicker(fileItem: FileItem, isMove: Boolean) {
        // Obtenemos directorios del directorio actual
        val currentDir = viewModel.currentDirectory.value
        val files = currentDir?.listFiles() ?: emptyArray()
        val directories = files.filter { it.isDirectory }
            .map { it.name }
            .toTypedArray()

        if (directories.isEmpty()) {
            Snackbar.make(
                binding.root,
                "No hay directorios disponibles en esta ubicación",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Selecciona un directorio")
            .setItems(directories) { _, which ->
                val targetDir = File(currentDir, directories[which])
                if (isMove) {
                    viewModel.moveFile(fileItem, targetDir)
                    Snackbar.make(binding.root, "Archivo movido con éxito", Snackbar.LENGTH_SHORT).show()
                } else {
                    viewModel.copyFile(fileItem, targetDir)
                    Snackbar.make(binding.root, "Archivo copiado con éxito", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }

    private fun showRenameDialog(fileItem: FileItem) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_input, null)
        val editText = dialogView.findViewById<EditText>(R.id.et_input)
        editText.setText(fileItem.name)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_rename_title)
            .setView(dialogView)
            .setPositiveButton("Aceptar") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != fileItem.name) {
                    viewModel.renameFile(fileItem, newName)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDirectoryPicker(fileItem: FileItem, isMove: Boolean) {
        // Implementar selector de directorio
        // Esto requeriría otra pantalla o diálogo que muestre solo directorios
        // Por ahora, como una implementación simple, podríamos mostrar un Snackbar
        Snackbar.make(
            binding.root,
            "Funcionalidad de ${if (isMove) "mover" else "copiar"} en desarrollo",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun showDeleteDialog(fileItem: FileItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, fileItem.name))
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteFile(fileItem)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showFileInfo(fileItem: FileItem) {
        val message = """
            Nombre: ${fileItem.name}
            Tipo: ${fileItem.getFileType()}
            Tamaño: ${fileItem.getFormattedSize()}
            Fecha de modificación: ${fileItem.getFormattedDate()}
            Ruta: ${fileItem.path}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_file_info)
            .setMessage(message)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun showCreateDialog() {
        val items = arrayOf(
            getString(R.string.dialog_folder),
            getString(R.string.dialog_file)
        )

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_create_title)
            .setItems(items) { _, which ->
                val isDirectory = which == 0
                showNameInputDialog(isDirectory)
            }
            .show()
    }

    private fun showNameInputDialog(isDirectory: Boolean) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_input, null)
        val editText = dialogView.findViewById<EditText>(R.id.et_input)

        AlertDialog.Builder(requireContext())
            .setTitle(if (isDirectory) R.string.dialog_new_folder else R.string.dialog_new_file)
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createFile(name, isDirectory)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setListView() {
        isListView = true
        binding.rvFiles.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setGridView() {
        isListView = false
        binding.rvFiles.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun switchToInternalStorage() {
        val internalDir = viewModel.getInternalStorageDirectory()
        viewModel.navigateToDirectory(internalDir)
        Snackbar.make(binding.root, "Cambiado a almacenamiento interno", Snackbar.LENGTH_SHORT).show()
    }

    fun switchToExternalStorage() {
        val externalDir = viewModel.getExternalStorageDirectory()
        if (externalDir != null) {
            viewModel.navigateToDirectory(externalDir)
            Snackbar.make(binding.root, "Cambiado a almacenamiento externo", Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(binding.root, "Almacenamiento externo no disponible", Snackbar.LENGTH_SHORT).show()
        }
    }
}