package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.data.model.EditorTab
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CodeEditor(
    modifier: Modifier = Modifier,
    activeTab: EditorTab?,
    textFieldValue: TextFieldValue?,
    onContentChange: (TextFieldValue) -> Unit,
) {
    if (activeTab == null) {
        Box(modifier = modifier.background(Background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Code, null, modifier = Modifier.size(64.dp), tint = SurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Open a file to start editing", color = OnSurfaceDim, style = MaterialTheme.typography.bodyMedium)
                Text("Use the Explorer or ≡ to navigate", color = OnSurfaceDim.copy(0.6f), style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }

    val scrollStateVertical = rememberScrollState()
    val scrollStateHorizontal = rememberScrollState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(textFieldValue?.selection) {
        bringIntoViewRequester.bringIntoView()
    }

    val lines = (textFieldValue?.text ?: "").split("\n")
    val lineCount = lines.size

    val interactionSource = remember { MutableInteractionSource() }

    Row(modifier = modifier.background(Background)) {
        // Line number gutter
        Column(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .background(GutterBg)
                .verticalScroll(scrollStateVertical, enabled = false)
                .padding(end = 8.dp, top = 12.dp)
        ) {
            repeat(lineCount) { i ->
                Text(
                    text = "${i + 1}",
                    style = lineNumberStyle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Active line highlight + text content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(scrollStateVertical)
                .horizontalScroll(scrollStateHorizontal)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { focusRequester.requestFocus() }
                )
        ) {
            val cursorLine = textFieldValue?.let {
                it.text.substring(0, it.selection.start.coerceAtMost(it.text.length)).count { c -> c == '\n' }
            } ?: 0

            // Active line background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (cursorLine * 22).dp)  // lineHeight from codeStyle
                    .height(22.dp)
                    .background(ActiveLineBg)
            )

            BasicTextField(
                value = textFieldValue ?: TextFieldValue(""),
                onValueChange = onContentChange,
                textStyle = codeStyle.copy(color = OnSurface),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp, start = 8.dp, end = 16.dp, bottom = 120.dp)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .focusRequester(focusRequester),
                cursorBrush = SolidColor(AccentCyan),
                visualTransformation = SyntaxVisualTransformation(activeTab.fileName),
                decorationBox = { innerTextField -> innerTextField() }
            )
        }
    }
}
