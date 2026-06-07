package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.EditorTab
import com.example.ui.theme.*

@Composable
fun GlobalActionRail(
    isExplorerVisible: Boolean,
    openTabs: List<EditorTab>,
    activeTabId: String?,
    isProcessRunning: Boolean,
    onToggleExplorer: () -> Unit,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onNewTab: () -> Unit,
    onRun: () -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenWorkspace: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(SurfaceVariant)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Explorer toggle
        IconButton(onClick = onToggleExplorer, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Menu, contentDescription = "Toggle Explorer", tint = OnSurface)
        }

        // Tab row
        LazyRow(modifier = Modifier.weight(1f)) {
            items(openTabs, key = { it.id }) { tab ->
                TabChip(
                    tab = tab,
                    isActive = tab.id == activeTabId,
                    onSelect = { onTabSelect(tab.id) },
                    onClose = { onTabClose(tab.id) }
                )
            }
            item {
                IconButton(onClick = onNewTab) {
                    Icon(Icons.Rounded.Add, contentDescription = "Open file", tint = OnSurface)
                }
            }
        }

        // Run button
        val runButtonColor by animateColorAsState(
            if (isProcessRunning) ErrorRed else AccentAmber
        )
        IconButton(
            onClick = onRun,
            modifier = Modifier.background(runButtonColor.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                if (isProcessRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                tint = runButtonColor,
                contentDescription = if (isProcessRunning) "Kill" else "Run"
            )
        }

        // Overflow menu
        var menuExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More options", tint = OnSurface)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Save  Ctrl+S") },
                    onClick = { onSave(); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Rounded.Save, null) }
                )
                DropdownMenuItem(
                    text = { Text("Undo") },
                    onClick = { onUndo(); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Rounded.Undo, null) }
                )
                DropdownMenuItem(
                    text = { Text("Redo") },
                    onClick = { onRedo(); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Rounded.Redo, null) }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Open Workspace…") },
                    onClick = { onOpenWorkspace(); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Rounded.FolderOpen, null) }
                )
            }
        }
    }
}

@Composable
fun TabChip(
    tab: EditorTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
        color = if (isActive) TabActiveBg else TabInactiveBg,
        modifier = Modifier.height(44.dp).clickable { onSelect() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 10.dp, end = 4.dp)
                    .align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = fileIconFor(tab.fileName),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = fileColorFor(tab.fileName)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = tab.fileName + if (tab.isModified) " ●" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) AccentCyan else OnSurfaceDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 80.dp)
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(12.dp),
                        tint = OnSurfaceDim
                    )
                }
            }
            if (isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(AccentCyan)
                )
            }
        }
    }
}
