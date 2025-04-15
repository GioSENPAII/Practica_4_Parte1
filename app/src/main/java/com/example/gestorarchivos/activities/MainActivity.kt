package com.example.gestorarchivos.activities

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.gestorarchivos.R
import com.example.gestorarchivos.databinding.ActivityMainBinding
import com.example.gestorarchivos.viewmodels.FileViewModel
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var preferences: SharedPreferences
    private lateinit var navController: NavController
    private val STORAGE_PERMISSION_CODE = 100
    private val PREFERENCE_NAME = "GestorArchivosPrefs"
    private val PREFERENCE_THEME = "theme_preference"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cargar tema antes de inflar la vista
        preferences = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE)
        applyTheme(preferences.getString(PREFERENCE_THEME, "ipn") ?: "ipn")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar NavController - CORREGIDO
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Configurar Drawer
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.menu_open_drawer,  // Deberás agregar esta string
            R.string.menu_close_drawer  // Deberás agregar esta string
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_explore, R.id.nav_recent),
            binding.drawerLayout
        )

        setSupportActionBar(binding.toolbar) // Agregar esta línea si no tienes un Toolbar configurado
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        // Manejar eventos del menú de navegación
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_explore, R.id.nav_recent -> {
                    navController.navigate(menuItem.itemId)
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_internal -> {
                    // Navegar al fragmento explorador y cambiar a almacenamiento interno
                    navController.navigate(R.id.nav_explore)
                    val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val fragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                    if (fragment is FileExplorerFragment) {
                        fragment.switchToInternalStorage()
                    }
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_external -> {
                    if (checkPermissions()) {
                        // Navegar al fragmento explorador y cambiar a almacenamiento externo
                        navController.navigate(R.id.nav_explore)
                        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                        val fragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                        if (fragment is FileExplorerFragment) {
                            fragment.switchToExternalStorage()
                        }
                    } else {
                        requestPermissions()
                    }
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_theme -> {
                    showThemeDialog()
                    true
                }
                else -> false
            }
        }

        // Verificar permisos
        if (!checkPermissions()) {
            requestPermissions()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, actualizar UI
                Snackbar.make(
                    binding.root,
                    "Permisos concedidos. Ahora puedes acceder a los archivos externos.",
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                // Permiso denegado, mostrar mensaje
                Snackbar.make(
                    binding.root,
                    "Permisos denegados. Algunas funciones no estarán disponibles.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf(
            getString(R.string.theme_ipn),
            getString(R.string.theme_escom)
        )

        val currentTheme = preferences.getString(PREFERENCE_THEME, "ipn") ?: "ipn"
        val initialSelection = if (currentTheme == "ipn") 0 else 1

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_theme))
            .setSingleChoiceItems(themes, initialSelection) { dialog, which ->
                val selectedTheme = if (which == 0) "ipn" else "escom"
                applyTheme(selectedTheme)
                preferences.edit().putString(PREFERENCE_THEME, selectedTheme).apply()
                dialog.dismiss()
                // Recrear la actividad para aplicar el tema correctamente
                recreate()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun applyTheme(theme: String) {
        when (theme) {
            "ipn" -> setTheme(R.style.Theme_GestorArchivos_IPN)
            "escom" -> setTheme(R.style.Theme_GestorArchivos_ESCOM)
            else -> setTheme(R.style.Theme_GestorArchivos_IPN)
        }
    }
}