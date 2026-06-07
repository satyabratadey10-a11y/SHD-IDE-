package com.example.ui.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.model.DialogState
import com.example.data.model.EditorTab
import com.example.data.model.FileNode
import com.example.data.model.TerminalLine
import com.example.data.model.UndoStack

data class AppUiState(
    val workspaceRootPath: String? = null,
    val fileTree: List<FileNode> = emptyList(),
    val selectedNodePath: String? = null,
    val openTabs: List<EditorTab> = emptyList(),
    val activeTabId: String? = null,
    val editorContents: Map<String, TextFieldValue> = emptyMap(),
    val undoStacks: Map<String, UndoStack> = emptyMap(),
    val isExplorerVisible: Boolean = true,
    val terminalLines: List<TerminalLine> = emptyList(),
    val terminalInputText: String = "",
    val isTerminalExpanded: Boolean = false,
    val isProcessRunning: Boolean = false,
    val dialogState: DialogState = DialogState.None,
    val snackbarMessage: String? = null
)
