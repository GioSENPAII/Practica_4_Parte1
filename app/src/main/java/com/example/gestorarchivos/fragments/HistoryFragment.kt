package com.example.gestorarchivos.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestorarchivos.R
import com.example.gestorarchivos.activities.FileViewerActivity
import com.example.gestorarchivos.adapters.RecentFileAdapter
import com.example.gestorarchivos.databinding.FragmentHistoryBinding
import com.example.gestorarchivos.viewmodels.FileViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.File

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FileViewModel by viewModels()
    private lateinit var recentAdapter: RecentFileAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        recentAdapter = RecentFileAdapter(
            onItemClick = { path ->
                openFile(path)
            },
            onFavoriteClick = { path, isFavorite ->
                viewModel.toggleFavorite(path, isFavorite)
            }
        )

        binding.rvHistory.apply {
            adapter = recentAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupObservers() {
        viewModel.recentFiles.observe(viewLifecycleOwner) { files ->
            recentAdapter.submitList(files)
            binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupListeners() {
        binding.fabClear.setOnClickListener {
            showClearHistoryDialog()
        }
    }

    private fun openFile(path: String) {
        val file = File(path)
        if (file.exists()) {
            if (file.isDirectory) {
                // Navegar al directorio usando el FragmentExplorer
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, FileExplorerFragment())
                    .addToBackStack(null)
                    .commit()

                // Aquí se debe actualizar el directorio actual en el ViewModel
                viewModel.navigateToDirectory(file)
            } else {
                // Abrir archivo
                val intent = Intent(requireContext(), FileViewerActivity::class.java).apply {
                    putExtra("file_path", path)
                }
                startActivity(intent)
            }
        } else {
            Snackbar.make(
                binding.root,
                "El archivo ya no existe",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun showClearHistoryDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Limpiar historial")
            .setMessage("¿Estás seguro de que quieres limpiar el historial?")
            .setPositiveButton("Limpiar") { _, _ ->
                viewModel.clearHistory()
                Snackbar.make(
                    binding.root,
                    R.string.history_cleared,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}