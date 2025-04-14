package com.example.gestorarchivos.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.gestorarchivos.R
import com.example.gestorarchivos.data.FileItem
import com.example.gestorarchivos.databinding.ActivityFileViewerBinding
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.simpleframework.xml.core.Persister
import org.simpleframework.xml.stream.Format
import java.io.File
import java.io.FileReader
import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

class FileViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileViewerBinding
    private lateinit var filePath: String
    private lateinit var file: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFileViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Obtener el archivo desde intent
        filePath = intent.getStringExtra("file_path") ?: ""
        if (filePath.isEmpty()) {
            Toast.makeText(this, "Error: ruta de archivo inválida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(this, "Error: el archivo no existe", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configurar título
        supportActionBar?.title = file.name

        // Mostrar cargando
        showLoading(true)

        // Cargar el contenido del archivo
        loadFileContent()

        // Configurar botón de abrir con
        binding.btnOpenWith.setOnClickListener {
            openFileWithExternalApp()
        }
    }

    private fun loadFileContent() {
        lifecycleScope.launch {
            try {
                val fileItem = FileItem(file)
                when (fileItem.getFileType()) {
                    FileItem.TYPE_TEXT -> loadTextFile()
                    FileItem.TYPE_IMAGE -> loadImageFile()
                    FileItem.TYPE_JSON -> loadJsonFile()
                    FileItem.TYPE_XML -> loadXmlFile()
                    else -> showUnsupportedFile()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FileViewerActivity,
                        "Error al cargar el archivo: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showUnsupportedFile()
                }
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun loadTextFile() {
        val content = withContext(Dispatchers.IO) {
            file.readText()
        }
        withContext(Dispatchers.Main) {
            binding.textContent.text = content
            binding.scrollText.visibility = View.VISIBLE
        }
    }

    private fun loadImageFile() {
        Glide.with(this)
            .load(file)
            .into(binding.imageView)
        binding.imageView.visibility = View.VISIBLE
    }

    private suspend fun loadJsonFile() {
        val content = withContext(Dispatchers.IO) {
            try {
                val fileReader = FileReader(file)
                val jsonElement = JsonParser.parseReader(fileReader)
                val gson = GsonBuilder().setPrettyPrinting().create()
                gson.toJson(jsonElement)
            } catch (e: Exception) {
                // Si hay un error al formatear, mostrar el texto plano
                file.readText()
            }
        }
        withContext(Dispatchers.Main) {
            binding.jsonContent.text = content
            binding.scrollJson.visibility = View.VISIBLE
        }
    }

    private suspend fun loadXmlFile() {
        val content = withContext(Dispatchers.IO) {
            try {
                val fileContent = file.readText()
                val factory = TransformerFactory.newInstance()
                val transformer = factory.newTransformer()
                transformer.setOutputProperty(OutputKeys.INDENT, "yes")
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

                val source = StreamSource(fileContent.reader())
                val writer = StringWriter()
                val result = StreamResult(writer)
                transformer.transform(source, result)
                writer.toString()
            } catch (e: Exception) {
                // Si hay un error al formatear, mostrar el texto plano
                file.readText()
            }
        }
        withContext(Dispatchers.Main) {
            binding.xmlContent.text = content
            binding.scrollXml.visibility = View.VISIBLE
        }
    }

    private fun showUnsupportedFile() {
        binding.unsupportedContainer.visibility = View.VISIBLE
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun openFileWithExternalApp() {
        try {
            val uri = Uri.fromFile(file)
            val intent = Intent(Intent.ACTION_VIEW)
            val mimeType = getMimeType(file.name)
            intent.setDataAndType(uri, mimeType)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "No se encontró una aplicación para abrir este archivo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getMimeType(url: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}