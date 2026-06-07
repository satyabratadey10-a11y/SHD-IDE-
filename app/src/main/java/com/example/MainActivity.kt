package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.FileRepository
import com.example.data.TerminalRepository
import com.example.engine.ShellEngine
import com.example.ui.theme.ShdIdeTheme
import com.example.ui.viewmodel.AppViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var shellEngine: ShellEngine
    
    // For OPEN_DOCUMENT_TREE
    private val openDocumentTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = convertUriToFilePath(it)
            viewModelInstance?.openWorkspace(path)
        }
    }

    private var viewModelInstance: AppViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Settings requested from Prompt:
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        enableEdgeToEdge()

        // 1. Check and request ALL_FILES_ACCESS on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } else {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 100
            )
        }

        // Initialize dependencies
        shellEngine = ShellEngine(applicationContext)
        val fileRepository = FileRepository()
        val terminalRepository = TerminalRepository(shellEngine)

        setContent {
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(fileRepository, terminalRepository) as T
                }
            }
            val viewModel: AppViewModel = viewModel(factory = factory)
            viewModelInstance = viewModel

            ShdIdeTheme {
                ShdIdeApp(
                    viewModel = viewModel,
                    onOpenWorkspacePicker = {
                        openDocumentTreeLauncher.launch(null)
                    }
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        shellEngine.stop()
    }

    // Convert Safari/Tree URI to actual file path
    private fun convertUriToFilePath(uri: Uri): String {
        val path = uri.path ?: return "/sdcard"
        // Typically looks like /tree/primary:Documents
        if (path.contains("primary:")) {
            val split = path.split("primary:")
            if (split.size > 1) {
                return Environment.getExternalStorageDirectory().absolutePath + "/" + split[1]
            }
        }
        return path
    }
}
