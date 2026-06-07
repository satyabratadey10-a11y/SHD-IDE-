package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TerminalLine
import com.example.data.model.TerminalLineType
import com.example.ui.theme.*

@Composable
fun TerminalPanel(
    terminalLines: List<TerminalLine>,
    inputText: String,
    isProcessRunning: Boolean,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onKill: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(Background)) {
        // Terminal header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(SurfaceVariant)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Terminal, null, modifier = Modifier.size(16.dp), tint = AccentCyan)
            Spacer(Modifier.width(4.dp))
            Text("TERMINAL", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp, color = OnSurfaceDim)

            if (isProcessRunning) {
                Spacer(Modifier.width(4.dp))
                val infiniteTransition = rememberInfiniteTransition()
                val alpha by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse))
                Box(modifier = Modifier.size(6.dp).background(SuccessGreen.copy(alpha = alpha), CircleShape))
            }

            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.CleaningServices, "Clear", modifier = Modifier.size(16.dp), tint = OnSurfaceDim)
            }
            if (isProcessRunning) {
                IconButton(onClick = onKill, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.StopCircle, "Kill", modifier = Modifier.size(16.dp), tint = ErrorRed)
                }
            }
        }

        HorizontalDivider(color = Background)

        val listState = rememberLazyListState()
        LaunchedEffect(terminalLines.size) {
            if (terminalLines.isNotEmpty()) listState.animateScrollToItem(terminalLines.size - 1)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(terminalLines, key = { "${it.timestamp}_${it.text.hashCode()}" }) { line ->
                val color = when (line.type) {
                    TerminalLineType.STDOUT -> OnSurface
                    TerminalLineType.STDERR -> StderrRed
                    TerminalLineType.SYSTEM -> AccentCyan
                    TerminalLineType.INPUT_ECHO -> AccentAmber
                }
                Text(
                    text = line.text,
                    style = terminalOutputStyle.copy(color = color),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceVariant)
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$ ", style = terminalInputStyle.copy(color = AccentCyan))
            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                textStyle = terminalInputStyle.copy(color = OnSurface),
                singleLine = true,
                modifier = Modifier.weight(1f),
                cursorBrush = SolidColor(AccentCyan),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                decorationBox = { it() }
            )
            IconButton(onClick = onSubmit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Send, "Run command", tint = AccentCyan, modifier = Modifier.size(18.dp))
            }
        }
    }
}
