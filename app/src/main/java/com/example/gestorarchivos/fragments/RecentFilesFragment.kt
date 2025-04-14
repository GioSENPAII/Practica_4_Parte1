package com.example.gestorarchivos.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.gestorarchivos.R
import com.example.gestorarchivos.activities.FileViewerActivity
import com.example.gestorarchivos.databinding.FragmentRecentFilesBinding
import com.example.gestorarchivos.viewmodels.FileViewModel
import com.google.android.material.tabs.TabLayoutMediator

class RecentFilesFragment : Fragment() {

    private var _binding: FragmentRecentFilesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupViewPager()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.menu_recent)
    }

    private fun setupViewPager() {
        val tabTitles = arrayOf(
            getString(R.string.tab_recent),
            getString(R.string.tab_favorites)
        )

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> HistoryFragment()
                    1 -> FavoritesFragment()
                    else -> HistoryFragment()
                }
            }
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}