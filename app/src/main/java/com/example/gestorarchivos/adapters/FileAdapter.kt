package com.example.gestorarchivos.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestorarchivos.R
import com.example.gestorarchivos.data.FileItem
import com.example.gestorarchivos.databinding.ItemFileBinding

class FileAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Boolean
) : ListAdapter<FileItem, FileAdapter.FileViewHolder>(FILE_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class FileViewHolder(private val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    onItemClick(item)
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    return@setOnLongClickListener onItemLongClick(item)
                }
                return@setOnLongClickListener false
            }
        }

        fun bind(fileItem: FileItem) {
            binding.apply {
                tvFileName.text = fileItem.name
                tvFileDate.text = fileItem.getFormattedDate()
                tvFileSize.text = fileItem.getFormattedSize()

                // Establecer icono según el tipo de archivo
                ivFileIcon.setImageResource(
                    when (fileItem.getFileType()) {
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
    }

    companion object {
        private val FILE_COMPARATOR = object : DiffUtil.ItemCallback<FileItem>() {
            override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
                return oldItem.path == newItem.path
            }

            override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}