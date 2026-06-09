package com.example

import android.os.Environment
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
import java.io.File

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

    var showWorkspacePicker by remember { mutableStateOf(false) }

    if (showWorkspacePicker) {
        WorkspacePickerDialog(
            initialPath = Environment.getExternalStorageDirectory().absolutePath,
            onConfirm = { path ->
                showWorkspacePicker = false
                viewModel.openWorkspace(path)
            },
            onDismiss = { showWorkspacePicker = false }
        )
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
                        val root = uiState.workspaceRootPath ?: run {
                            val defaultPath = Environment.getExternalStorageDirectory().absolutePath + "/SHD-IDE"
                            val dir = File(defaultPath)
                            if (!dir.exists()) dir.mkdirs()
                            defaultPath
                        }
                        viewModel.showNewFileDialog(root)
                    } catch (e: Exception) {
                        viewModel.showSnackbarMessage("Failed to initialize new file path")
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
                onOpenWorkspace = { showWorkspacePicker = true }
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
