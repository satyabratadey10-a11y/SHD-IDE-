package com.example.ui.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FileRepository
import com.example.data.TerminalRepository
import com.example.data.model.DialogState
import com.example.data.model.EditorTab
import com.example.data.model.FileNode
import com.example.data.model.TerminalLine
import com.example.data.model.TerminalLineType
import com.example.data.model.UndoStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AppViewModel(
    private val fileRepository: FileRepository,
    private val terminalRepository: TerminalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        // Automatically start the shell engine
        terminalRepository.startShell { output ->
            appendTerminalLine(TerminalLine(output, TerminalLineType.STDOUT))
        }
    }

    private fun appendTerminalLine(line: TerminalLine) {
        _uiState.update { state ->
            val newLines = (state.terminalLines + line).takeLast(2000)
            state.copy(terminalLines = newLines, isProcessRunning = terminalRepository.isSessionActive)
        }
    }

    // Workspace
    fun openWorkspace(rootPath: String) {
        _uiState.update { it.copy(workspaceRootPath = rootPath) }
        refreshFileTree()
    }

    // File Tree
    fun toggleDirectoryExpansion(nodePath: String) {
        _uiState.update { state ->
            state.copy(fileTree = updateNodeExpansion(state.fileTree, nodePath))
        }
    }

    private fun updateNodeExpansion(nodes: List<FileNode>, targetPath: String): List<FileNode> {
        return nodes.map { node ->
            if (node.absolutePath == targetPath) {
                node.copy(isExpanded = !node.isExpanded)
            } else if (node.isDirectory) {
                node.copy(children = updateNodeExpansion(node.children, targetPath))
            } else {
                node
            }
        }
    }

    fun selectNode(nodePath: String) {
        _uiState.update { it.copy(selectedNodePath = nodePath) }
    }

    fun refreshFileTree() {
        val rootPath = _uiState.value.workspaceRootPath ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val tree = fileRepository.loadFileTree(rootPath)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(fileTree = tree) }
            }
        }
    }

    // Dialogs
    fun showNewFileDialog(parentPath: String) {
        _uiState.update { it.copy(dialogState = DialogState.NewFile(parentPath)) }
    }
    
    fun showNewFolderDialog(parentPath: String) {
        _uiState.update { it.copy(dialogState = DialogState.NewFolder(parentPath)) }
    }
    
    fun showRenameDialog(nodePath: String, currentName: String) {
        _uiState.update { it.copy(dialogState = DialogState.Rename(nodePath, currentName)) }
    }
    
    fun showDeleteConfirmDialog(nodePath: String, name: String) {
        _uiState.update { it.copy(dialogState = DialogState.ConfirmDelete(nodePath, name)) }
    }
    
    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = DialogState.None) }
    }

    // File Operations
    fun createFile(parentPath: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            fileRepository.createFile(parentPath, name).onSuccess {
                refreshFileTree()
            }.onFailure { e ->
                withContext(Dispatchers.Main) { _uiState.update { it.copy(snackbarMessage = e.message) } }
            }
        }
        dismissDialog()
    }

    fun createFolder(parentPath: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            fileRepository.createFolder(parentPath, name).onSuccess {
                refreshFileTree()
            }.onFailure { e ->
                withContext(Dispatchers.Main) { _uiState.update { it.copy(snackbarMessage = e.message) } }
            }
        }
        dismissDialog()
    }

    fun renameNode(oldPath: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            fileRepository.renameNode(oldPath, newName).onSuccess {
                refreshFileTree()
            }.onFailure { e ->
                withContext(Dispatchers.Main) { _uiState.update { it.copy(snackbarMessage = e.message) } }
            }
        }
        dismissDialog()
    }

    fun deleteNode(absolutePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            fileRepository.deleteNode(absolutePath).onSuccess {
                refreshFileTree()
                withContext(Dispatchers.Main) {
                    val tabIdToClose = _uiState.value.openTabs.find { it.absolutePath == absolutePath }?.id
                    if (tabIdToClose != null) {
                        closeTab(tabIdToClose)
                    }
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) { _uiState.update { it.copy(snackbarMessage = e.message) } }
            }
        }
        dismissDialog()
    }

    // Editor Tabs
    fun openFileInEditor(absolutePath: String, fileName: String) {
        val existingTab = _uiState.value.openTabs.find { it.absolutePath == absolutePath }
        if (existingTab != null) {
            switchTab(existingTab.id)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = fileRepository.readFileContent(absolutePath)
                withContext(Dispatchers.Main) {
                    val tabId = UUID.randomUUID().toString()
                    val newTab = EditorTab(tabId, fileName, absolutePath)
                    _uiState.update { state ->
                        val newTabs = state.openTabs + newTab
                        val newContents = state.editorContents + (absolutePath to TextFieldValue(content))
                        val newUndos = state.undoStacks + (absolutePath to UndoStack())
                        state.copy(
                            openTabs = newTabs,
                            activeTabId = tabId,
                            editorContents = newContents,
                            undoStacks = newUndos
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(snackbarMessage = "Failed to open file: ${e.message}") }
                }
            }
        }
    }

    fun closeTab(tabId: String) {
        _uiState.update { state ->
            val tabToRemove = state.openTabs.find { it.id == tabId } ?: return@update state
            val newTabs = state.openTabs.filter { it.id != tabId }
            var newActiveTabId = state.activeTabId
            if (state.activeTabId == tabId) {
                newActiveTabId = newTabs.lastOrNull()?.id
            }
            
            val newContents = state.editorContents.filterKeys { it != tabToRemove.absolutePath }
            val newUndos = state.undoStacks.filterKeys { it != tabToRemove.absolutePath }
            
            state.copy(
                openTabs = newTabs,
                activeTabId = newActiveTabId,
                editorContents = newContents,
                undoStacks = newUndos
            )
        }
    }

    fun switchTab(tabId: String) {
        _uiState.update { it.copy(activeTabId = tabId) }
    }

    // Editor Content
    fun onEditorContentChange(absolutePath: String, newValue: TextFieldValue) {
        val state = _uiState.value
        val currentValue = state.editorContents[absolutePath]
        if (currentValue?.text == newValue.text && currentValue.selection == newValue.selection) return

        _uiState.update { s ->
            val newContents = s.editorContents.toMutableMap()
            newContents[absolutePath] = newValue
            
            val newUndos = s.undoStacks.toMutableMap()
            val undoStack = newUndos[absolutePath] ?: UndoStack()
            
            if (currentValue != null && currentValue.text != newValue.text) {
                val newHistory = ArrayDeque(undoStack.history)
                newHistory.addLast(currentValue)
                if (newHistory.size > undoStack.maxSize) newHistory.removeFirst()
                newUndos[absolutePath] = undoStack.copy(history = newHistory, future = ArrayDeque())
            }

            val newTabs = s.openTabs.map {
                if (it.absolutePath == absolutePath && currentValue != null && currentValue.text != newValue.text) {
                    it.copy(isModified = true)
                } else it
            }

            s.copy(
                editorContents = newContents,
                undoStacks = newUndos,
                openTabs = newTabs
            )
        }
    }

    fun saveCurrentFile() {
        val state = _uiState.value
        val activeTabId = state.activeTabId ?: return
        val activeTab = state.openTabs.find { it.id == activeTabId } ?: return
        val content = state.editorContents[activeTab.absolutePath]?.text ?: return

        viewModelScope.launch(Dispatchers.IO) {
            fileRepository.writeFileContent(activeTab.absolutePath, content)
            withContext(Dispatchers.Main) {
                _uiState.update { s ->
                    val newTabs = s.openTabs.map {
                        if (it.id == activeTabId) it.copy(isModified = false) else it
                    }
                    val newLine = TerminalLine("[Saved: ${activeTab.fileName}]", TerminalLineType.SYSTEM)
                    val newLines = (s.terminalLines + newLine).takeLast(2000)
                    s.copy(openTabs = newTabs, terminalLines = newLines)
                }
            }
        }
    }

    // Explict block actions requested by user
    fun undo(absolutePath: String) {
        _uiState.update { state ->
            val undoStack = state.undoStacks[absolutePath] ?: return@update state
            if (undoStack.history.isEmpty()) return@update state

            val currentValue = state.editorContents[absolutePath] ?: return@update state
            val previousValue = undoStack.history.removeLast()
            undoStack.future.addLast(currentValue)

            val newContents = state.editorContents.toMutableMap()
            newContents[absolutePath] = previousValue

            state.copy(
                editorContents = newContents,
                openTabs = state.openTabs.map { if (it.absolutePath == absolutePath) it.copy(isModified = true) else it }
            )
        }
    }

    fun redo(absolutePath: String) {
        _uiState.update { state ->
            val undoStack = state.undoStacks[absolutePath] ?: return@update state
            if (undoStack.future.isEmpty()) return@update state

            val currentValue = state.editorContents[absolutePath] ?: return@update state
            val nextValue = undoStack.future.removeLast()
            undoStack.history.addLast(currentValue)

            val newContents = state.editorContents.toMutableMap()
            newContents[absolutePath] = nextValue

            state.copy(
                editorContents = newContents,
                openTabs = state.openTabs.map { if (it.absolutePath == absolutePath) it.copy(isModified = true) else it }
            )
        }
    }

    fun toggleExplorer() {
        _uiState.update { it.copy(isExplorerVisible = !it.isExplorerVisible) }
    }
    
    fun toggleTerminal() {
        _uiState.update { it.copy(isTerminalExpanded = !it.isTerminalExpanded) }
    }

    // Terminal
    fun onTerminalInputChange(text: String) {
        _uiState.update { it.copy(terminalInputText = text) }
    }
    
    fun submitTerminalCommand() {
        val state = _uiState.value
        val command = state.terminalInputText
        if (command.isBlank()) return

        _uiState.update { 
            val newLines = (it.terminalLines + TerminalLine("$ $command", TerminalLineType.INPUT_ECHO)).takeLast(2000)
            it.copy(terminalInputText = "", terminalLines = newLines)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val workingDir = state.workspaceRootPath ?: "/"
            if (terminalRepository.isSessionActive) {
                terminalRepository.sendInput(command)
            } else {
                terminalRepository.executeCommand(command, workingDir) { line ->
                    appendTerminalLine(line)
                }
            }
        }
    }

    fun clearTerminal() {
        _uiState.update { it.copy(terminalLines = emptyList()) }
    }
    
    fun killProcess() {
        terminalRepository.killProcess()
        appendTerminalLine(TerminalLine("[Process killed]", TerminalLineType.SYSTEM))
    }

    // Run/Build
    fun runCurrentScript() {
        val state = _uiState.value
        val activeTabId = state.activeTabId ?: return
        val activeTab = state.openTabs.find { it.id == activeTabId } ?: return
        val absolutePath = activeTab.absolutePath

        val ext = absolutePath.substringAfterLast('.', "").lowercase()
        val command = when(ext) {
            "py" -> "python3 $absolutePath"
            "sh" -> "sh $absolutePath"
            "js" -> "node $absolutePath"
            "kt" -> "echo 'Kotlin execution requires compilation (kotlinc)'"
            else -> "sh $absolutePath"
        }

        val workingDir = File(absolutePath).parent ?: state.workspaceRootPath ?: "/"
        
        appendTerminalLine(TerminalLine("$ $command", TerminalLineType.INPUT_ECHO))
        _uiState.update { it.copy(isTerminalExpanded = true) }

        viewModelScope.launch(Dispatchers.IO) {
            terminalRepository.executeCommand(command, workingDir) { line ->
                appendTerminalLine(line)
            }
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isProcessRunning = false) }
            }
        }
    }

    fun showSnackbarMessage(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
