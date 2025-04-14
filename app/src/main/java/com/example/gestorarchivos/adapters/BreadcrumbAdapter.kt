package com.example.gestorarchivos.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestorarchivos.databinding.ItemBreadcrumbBinding

class BreadcrumbAdapter(private val onBreadcrumbClick: (String) -> Unit) :
    ListAdapter<Pair<String, String>, BreadcrumbAdapter.BreadcrumbViewHolder>(BREADCRUMB_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreadcrumbViewHolder {
        val binding = ItemBreadcrumbBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BreadcrumbViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BreadcrumbViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem, position == itemCount - 1)
    }

    inner class BreadcrumbViewHolder(private val binding: ItemBreadcrumbBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val path = getItem(position).first
                    onBreadcrumbClick(path)
                }
            }
        }

        fun bind(breadcrumb: Pair<String, String>, isLast: Boolean) {
            binding.apply {
                tvBreadcrumb.text = breadcrumb.second
                divider.visibility = if (isLast) ViewGroup.GONE else ViewGroup.VISIBLE
            }
        }
    }

    companion object {
        private val BREADCRUMB_COMPARATOR = object : DiffUtil.ItemCallback<Pair<String, String>>() {
            override fun areItemsTheSame(
                oldItem: Pair<String, String>,
                newItem: Pair<String, String>
            ): Boolean {
                return oldItem.first == newItem.first
            }

            override fun areContentsTheSame(
                oldItem: Pair<String, String>,
                newItem: Pair<String, String>
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}