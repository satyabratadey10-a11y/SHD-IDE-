package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileNode
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileExplorer(
    modifier: Modifier = Modifier,
    fileTree: List<FileNode>,
    selectedPath: String?,
    workspaceRootPath: String?,
    onFileOpen: (FileNode) -> Unit,
    onToggleDirectory: (String) -> Unit,
    onSelectNode: (String) -> Unit,
    onShowNewFileDialog: (String) -> Unit,
    onShowNewFolderDialog: (String) -> Unit,
    onShowRenameDialog: (String, String) -> Unit,
    onShowDeleteDialog: (String, String) -> Unit,
) {
    Column(modifier = modifier.background(Surface)) {
        // Explorer header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("EXPLORER", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim, letterSpacing = 1.5.sp)
            Spacer(Modifier.weight(1f))
            Text(
                text = workspaceRootPath?.substringAfterLast("/") ?: "No Workspace",
                style = MaterialTheme.typography.labelSmall,
                color = if (workspaceRootPath != null) AccentCyan else OnSurfaceDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(color = SurfaceVariant)

        // File tree
        LazyColumn(modifier = Modifier.weight(1f)) {
            val renderNodes: (List<FileNode>) -> Unit = { nodes ->
                // Since LazyListScope is inside, we will use a hack to traverse recursively
                // But LazyColumn requires explicit calls. We'll flatten the visible nodes first instead.
            }
            
            // To make it easy with Compose, let's flatten the list of visible nodes
            fun flattenVisibleNodes(nodes: List<FileNode>): List<FileNode> {
                val flatList = mutableListOf<FileNode>()
                for (node in nodes) {
                    flatList.add(node)
                    if (node.isDirectory && node.isExpanded) {
                        flatList.addAll(flattenVisibleNodes(node.children))
                    }
                }
                return flatList
            }
            
            val visibleNodes = flattenVisibleNodes(fileTree)
            
            items(visibleNodes.size, key = { visibleNodes[it].absolutePath }) { index ->
                val node = visibleNodes[index]
                FileTreeRow(
                    node = node,
                    isSelected = node.absolutePath == selectedPath,
                    onClick = {
                        onSelectNode(node.absolutePath)
                        if (node.isDirectory) onToggleDirectory(node.absolutePath)
                        else onFileOpen(node)
                    },
                    onLongClick = { onSelectNode(node.absolutePath) }
                )
            }
        }

        HorizontalDivider(color = SurfaceVariant)

        // File Operations Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(SurfaceVariant)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val targetPath = selectedPath ?: workspaceRootPath ?: ""
            val hasSelection = selectedPath != null
            val selectedName = fileTree.flatten().find { it.absolutePath == selectedPath }?.name ?: ""

            IconButton(onClick = { onShowNewFileDialog(targetPath) }, enabled = targetPath.isNotEmpty()) {
                Icon(Icons.Rounded.NoteAdd, "New File", tint = if (targetPath.isNotEmpty()) AccentCyan else OnSurfaceDim, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { onShowNewFolderDialog(targetPath) }, enabled = targetPath.isNotEmpty()) {
                Icon(Icons.Rounded.CreateNewFolder, "New Folder", tint = if (targetPath.isNotEmpty()) AccentCyan else OnSurfaceDim, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { onShowRenameDialog(selectedPath!!, selectedName) }, enabled = hasSelection) {
                Icon(Icons.Rounded.DriveFileRenameOutline, "Rename", tint = if (hasSelection) OnSurface else OnSurfaceDim, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { onShowDeleteDialog(selectedPath!!, selectedName) }, enabled = hasSelection) {
                Icon(Icons.Rounded.DeleteOutline, "Delete", tint = if (hasSelection) ErrorRed else OnSurfaceDim, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// Helper to flatten entirely for name lookup
fun List<FileNode>.flatten(): List<FileNode> {
    val flatList = mutableListOf<FileNode>()
    for (node in this) {
        flatList.add(node)
        if (node.isDirectory) flatList.addAll(node.children.flatten())
    }
    return flatList
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTreeRow(
    node: FileNode,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bgColor = if (isSelected) AccentCyan.copy(alpha = 0.12f) else Color.Transparent
    val indentDp = (node.depth * 12).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(bgColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 8.dp + indentDp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.isDirectory) {
            val rotation by animateFloatAsState(if (node.isExpanded) 90f else 0f)
            Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(16.dp).rotate(rotation), tint = OnSurfaceDim)
        } else {
            Spacer(Modifier.size(16.dp))
        }
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = if (node.isDirectory) Icons.Rounded.Folder else fileIconFor(node.name),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (node.isDirectory) Color(0xFFF0A500) else fileColorFor(node.name)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = node.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) AccentCyan else OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun fileIconFor(filename: String): ImageVector {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "kt", "java" -> Icons.Rounded.Code
        "py" -> Icons.Rounded.Terminal
        "json", "xml" -> Icons.Rounded.DataObject
        "md" -> Icons.Rounded.Description
        else -> Icons.Rounded.InsertDriveFile
    }
}

fun fileColorFor(filename: String): Color {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "kt" -> Color(0xFF7F52FF)
        "java" -> Color(0xFFFF6D00)
        "py" -> Color(0xFF3572A5)
        "js", "ts" -> Color(0xFFF7DF1E)
        "json" -> Color(0xFFCBCB41)
        "xml" -> Color(0xFFE37933)
        "md" -> Color(0xFF519ABA)
        "sh" -> Color(0xFF4EAA25)
        else -> OnSurfaceDim
    }
}
