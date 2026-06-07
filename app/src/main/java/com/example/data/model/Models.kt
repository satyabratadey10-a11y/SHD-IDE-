package com.example.data.model

import androidx.compose.ui.text.input.TextFieldValue

data class FileNode(
    val name: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val depth: Int,
    val children: List<FileNode> = emptyList(),
    val isExpanded: Boolean = false
)

data class EditorTab(
    val id: String,           // UUID string
    val fileName: String,
    val absolutePath: String,
    val isModified: Boolean = false
)

data class TerminalLine(
    val text: String,
    val type: TerminalLineType,
    val timestamp: Long = java.lang.System.currentTimeMillis()
)

enum class TerminalLineType { STDOUT, STDERR, SYSTEM, INPUT_ECHO }

data class UndoStack(
    val history: ArrayDeque<TextFieldValue> = ArrayDeque(),
    val future: ArrayDeque<TextFieldValue> = ArrayDeque(),
    val maxSize: Int = 100
)

sealed class FileOperation {
    data class CreateFile(val parentPath: String, val name: String) : FileOperation()
    data class CreateFolder(val parentPath: String, val name: String) : FileOperation()
    data class RenameNode(val oldPath: String, val newName: String) : FileOperation()
    data class DeleteNode(val absolutePath: String) : FileOperation()
}

sealed class DialogState {
    object None : DialogState()
    data class NewFile(val parentPath: String) : DialogState()
    data class NewFolder(val parentPath: String) : DialogState()
    data class Rename(val currentPath: String, val currentName: String) : DialogState()
    data class ConfirmDelete(val absolutePath: String, val name: String) : DialogState()
}
