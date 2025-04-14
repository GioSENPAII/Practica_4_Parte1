package com.example.gestorarchivos.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestorarchivos.R
import com.example.gestorarchivos.data.FileItem
import com.example.gestorarchivos.data.RecentFile
import com.example.gestorarchivos.databinding.ItemRecentFileBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentFileAdapter(
    private val onItemClick: (String) -> Unit,
    private val onFavoriteClick: (String, Boolean) -> Unit
) : ListAdapter<RecentFile, RecentFileAdapter.RecentFileViewHolder>(RECENT_FILE_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentFileViewHolder {
        val binding = ItemRecentFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentFileViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class RecentFileViewHolder(private val binding: ItemRecentFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val path = getItem(position).path
                    onItemClick(path)
                }
            }

            binding.btnFavorite.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    onFavoriteClick(item.path, !item.isFavorite)
                }
            }
        }

        fun bind(recentFile: RecentFile) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val lastAccessedDate = dateFormat.format(Date(recentFile.lastAccessed))

            binding.apply {
                tvFileName.text = recentFile.name
                tvFileDate.text = lastAccessedDate
                tvFilePath.text = recentFile.path

                // Cambiar el icono de favorito según el estado
                btnFavorite.setImageResource(
                    if (recentFile.isFavorite) R.drawable.ic_favorite_filled
                    else R.drawable.ic_favorite_border
                )

                // Establecer icono según el tipo de archivo
                val fileType = getFileType(recentFile)
                ivFileIcon.setImageResource(
                    when (fileType) {
                        FileItem.TYPE_DIRECTORY -> R.drawable.ic_folder
                        FileItem.TYPE_TEXT -> R.drawable.ic_text
                        FileItem.TYPE_IMAGE -> R.drawable.ic_image
                        FileItem.TYPE_JSON -> R.drawable.ic_json
                        FileItem.TYPE_XML -> R.drawable.ic_xml
                        else -> R.drawable.ic_file
                    }
                )
            }
        }

        private fun getFileType(recentFile: RecentFile): String {
            if (recentFile.isDirectory) return FileItem.TYPE_DIRECTORY

            return when (recentFile.extension) {
                in FileItem.TEXT_EXTENSIONS -> FileItem.TYPE_TEXT
                in FileItem.IMAGE_EXTENSIONS -> FileItem.TYPE_IMAGE
                in FileItem.JSON_EXTENSIONS -> FileItem.TYPE_JSON
                in FileItem.XML_EXTENSIONS -> FileItem.TYPE_XML
                else -> FileItem.TYPE_OTHER
            }
        }
    }

    companion object {
        private val RECENT_FILE_COMPARATOR = object : DiffUtil.ItemCallback<RecentFile>() {
            override fun areItemsTheSame(oldItem: RecentFile, newItem: RecentFile): Boolean {
                return oldItem.path == newItem.path
            }

            override fun areContentsTheSame(oldItem: RecentFile, newItem: RecentFile): Boolean {
                return oldItem == newItem
            }
        }
    }
}