package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceDim
import java.io.File

@Composable
fun WorkspacePickerDialog(initialPath: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var currentPath by remember { mutableStateOf(initialPath) }
    var directories by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(currentPath) {
        directories = try {
            val file = File(currentPath)
            if (file.isDirectory) {
                file.listFiles()?.filter { it.isDirectory && !it.isHidden }?.sortedBy { it.name } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Workspace", color = OnSurface)
                Text(currentPath, style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                if (currentPath != "/") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val parent = File(currentPath).parent
                                if (parent != null) currentPath = parent
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Up", tint = AccentCyan, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("..", color = OnSurface)
                    }
                    HorizontalDivider(color = OnSurfaceDim.copy(alpha = 0.3f))
                }
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(directories, key = { it.absolutePath }) { dir ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentPath = dir.absolutePath }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Folder, contentDescription = "Folder", tint = AccentCyan, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(dir.name, color = OnSurface)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentPath) }) {
                Text("Select This Folder", color = AccentCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurface) }
        }
    )
}

@Composable
fun NewFileDialog(parentPath: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New File") },
        icon = { Icon(Icons.Rounded.NoteAdd, contentDescription = null, tint = AccentCyan) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("File name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    focusedLabelColor = AccentCyan,
                    focusedTextColor = OnSurface
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Create", color = if (text.isNotBlank()) AccentCyan else MaterialTheme.colorScheme.onSurface.copy(alpha=0.38f))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurface) }
        }
    )
}

@Composable
fun NewFolderDialog(parentPath: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        icon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, tint = AccentCyan) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Folder name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    focusedLabelColor = AccentCyan,
                    focusedTextColor = OnSurface
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Create", color = if (text.isNotBlank()) AccentCyan else MaterialTheme.colorScheme.onSurface.copy(alpha=0.38f))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurface) }
        }
    )
}

@Composable
fun RenameDialog(currentPath: String, currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = currentName, selection = TextRange(0, currentName.length)))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                label = { Text("New name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    focusedLabelColor = AccentCyan,
                    focusedTextColor = OnSurface
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(textFieldValue.text) }, enabled = textFieldValue.text.isNotBlank()) {
                Text("Rename", color = if (textFieldValue.text.isNotBlank()) AccentCyan else MaterialTheme.colorScheme.onSurface.copy(alpha=0.38f))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurface) }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Item") },
        text = { Text("Delete \"$name\"? This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = ErrorRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurface) }
        }
    )
}
