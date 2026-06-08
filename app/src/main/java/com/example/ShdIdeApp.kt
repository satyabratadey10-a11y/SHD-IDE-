package com.example

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DialogState
import com.example.ui.components.*
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

private fun convertUriToFilePath(uri: Uri): String {
    val path = uri.path ?: return "/sdcard"
    if (path.contains("primary:")) {
        val split = path.split("primary:")
        if (split.size > 1) {
            return Environment.getExternalStorageDirectory().absolutePath + "/" + split[1]
        }
    }
    return path
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShdIdeApp(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val openWorkspaceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        try {
            uri?.let {
                val path = convertUriToFilePath(it)
                viewModel.openWorkspace(path)
            }
        } catch (e: Exception) {
            viewModel.showSnackbarMessage("Failed to open workspace")
        }
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        try {
            uri?.let {
                val path = convertUriToFilePath(it)
                val fileName = path.substringAfterLast("/")
                viewModel.openFileInEditor(path, fileName)
            }
        } catch (e: Exception) {
            viewModel.showSnackbarMessage("Failed to open file")
        }
    }

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    LaunchedEffect(uiState.isTerminalExpanded) {
        if (uiState.isTerminalExpanded) {
            sheetState.expand()
        } else {
            sheetState.partialExpand()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 36.dp, // terminal header only
        sheetContent = {
            TerminalPanel(
                terminalLines = uiState.terminalLines,
                inputText = uiState.terminalInputText,
                isProcessRunning = uiState.isProcessRunning,
                onInputChange = viewModel::onTerminalInputChange,
                onSubmit = viewModel::submitTerminalCommand,
                onClear = viewModel::clearTerminal,
                onKill = viewModel::killProcess,
                onToggle = viewModel::toggleTerminal
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            GlobalActionRail(
                isExplorerVisible = uiState.isExplorerVisible,
                openTabs = uiState.openTabs,
                activeTabId = uiState.activeTabId,
                isProcessRunning = uiState.isProcessRunning,
                onToggleExplorer = viewModel::toggleExplorer,
                onTabSelect = viewModel::switchTab,
                onTabClose = viewModel::closeTab,
                onNewTab = { 
                    try { 
                        openFileLauncher.launch(arrayOf("*/*")) 
                    } catch (e: Exception) {
                        viewModel.showSnackbarMessage("File picker not available")
                    }
                },
                onRun = viewModel::runCurrentScript,
                onSave = viewModel::saveCurrentFile,
                onUndo = { 
                    uiState.activeTabId?.let { id -> 
                        val tab = uiState.openTabs.find { it.id == id }
                        tab?.let { viewModel.undo(it.absolutePath) }
                    }
                },
                onRedo = { 
                    uiState.activeTabId?.let { id -> 
                        val tab = uiState.openTabs.find { it.id == id }
                        tab?.let { viewModel.redo(it.absolutePath) }
                    }
                },
                onOpenWorkspace = { 
                    try {
                        openWorkspaceLauncher.launch(null)
                    } catch (e: Exception) {
                        viewModel.showSnackbarMessage("Folder picker not available")
                    }
                }
            )

            Row(modifier = Modifier.weight(1f)) {
                AnimatedVisibility(
                    visible = uiState.isExplorerVisible,
                    enter = slideInHorizontally() + fadeIn(),
                    exit = slideOutHorizontally() + fadeOut()
                ) {
                    FileExplorer(
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight(),
                        fileTree = uiState.fileTree,
                        selectedPath = uiState.selectedNodePath,
                        workspaceRootPath = uiState.workspaceRootPath,
                        onFileOpen = { viewModel.openFileInEditor(it.absolutePath, it.name) },
                        onToggleDirectory = viewModel::toggleDirectoryExpansion,
                        onSelectNode = viewModel::selectNode,
                        onShowNewFileDialog = viewModel::showNewFileDialog,
                        onShowNewFolderDialog = viewModel::showNewFolderDialog,
                        onShowRenameDialog = viewModel::showRenameDialog,
                        onShowDeleteDialog = viewModel::showDeleteConfirmDialog
                    )
                }

                val activeTab = uiState.openTabs.find { it.id == uiState.activeTabId }
                CodeEditor(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    activeTab = activeTab,
                    textFieldValue = activeTab?.let { uiState.editorContents[it.absolutePath] },
                    onContentChange = { newValue ->
                        activeTab?.let { viewModel.onEditorContentChange(it.absolutePath, newValue) }
                    }
                )
            }
        }
    }

    when (val dialog = uiState.dialogState) {
        is DialogState.NewFile -> {
            NewFileDialog(
                parentPath = dialog.parentPath,
                onConfirm = { name -> viewModel.createFile(dialog.parentPath, name) },
                onDismiss = viewModel::dismissDialog
            )
        }
        is DialogState.NewFolder -> {
            NewFolderDialog(
                parentPath = dialog.parentPath,
                onConfirm = { name -> viewModel.createFolder(dialog.parentPath, name) },
                onDismiss = viewModel::dismissDialog
            )
        }
        is DialogState.Rename -> {
            RenameDialog(
                currentPath = dialog.currentPath,
                currentName = dialog.currentName,
                onConfirm = { newName -> viewModel.renameNode(dialog.currentPath, newName) },
                onDismiss = viewModel::dismissDialog
            )
        }
        is DialogState.ConfirmDelete -> {
            ConfirmDeleteDialog(
                name = dialog.name,
                onConfirm = { viewModel.deleteNode(dialog.absolutePath) },
                onDismiss = viewModel::dismissDialog
            )
        }
        DialogState.None -> {}
    }
}
