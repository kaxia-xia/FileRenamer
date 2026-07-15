package com.filerenamer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filerenamer.ui.screens.MainScreen
import com.filerenamer.ui.screens.MainViewModel
import com.filerenamer.ui.theme.FileRenamerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val viewModel: MainViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            // SAF 文件夹选择器
            val folderPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri: Uri? ->
                if (uri != null) {
                    viewModel.onFolderSelected(uri)
                }
            }

            FileRenamerTheme(
                wallpaperColor = uiState.wallpaperColor
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        uiState = uiState,
                        onSelectFolder = { folderPickerLauncher.launch(null) },
                        onEnterDirectory = { viewModel.enterDirectory(it) },
                        onGoBack = { viewModel.goBack() },
                        onToggleFile = { viewModel.toggleFileSelection(it) },
                        onToggleSelectAll = { viewModel.toggleSelectAll() },
                        onShowRenameDialog = { viewModel.showRenameDialog() },
                        onHideRenameDialog = { viewModel.hideRenameDialog() },
                        onRenameTextChange = { viewModel.setRenameText(it) },
                        onRenameCharCountChange = { viewModel.setRenameCharCount(it) },
                        onRenamePositionChange = { viewModel.setRenamePosition(it) },
                        onRenameTypeChange = { viewModel.setRenameType(it) },
                        onExecuteRename = { viewModel.executeRename() },
                        onClearError = { viewModel.clearError() },
                        onClearResult = { viewModel.clearRenameResult() },
                    )
                }
            }
        }
    }
}
