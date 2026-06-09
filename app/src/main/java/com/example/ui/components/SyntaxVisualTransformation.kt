package com.example.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.OnSurfaceDim
import com.example.ui.theme.SuccessGreen

private val HTML_COMMENT = Regex("""<!--[\s\S]*?-->""")
private val LINE_COMMENT = Regex("""//.*""")
private val BLOCK_COMMENT = Regex("""/\*[\s\S]*?\*/""")
private val STRINGS = Regex("""("[^"\\]*(?:\\.[^"\\]*)*")|('[^'\\]*(?:\\.[^'\\]*)*')|(`[^`\\]*(?:\\.[^`\\]*)*`)""")
private val HTML_TAGS = Regex("""</?[a-zA-Z0-9\-]+|/?>""")
private val HTML_ATTRS = Regex("""\b[a-zA-Z\-:]+(?=\s*=)""")
private val KEYWORDS = Regex("""\b(val|var|fun|class|interface|object|return|if|else|for|while|do|when|import|package|true|false|null|typeof|function|const|let|def|print|from|as|public|private|protected|suspend|override|await|async|yield)\b""")
private val NUMBERS = Regex("""\b\d+(\.\d+)?\b""")
private val PUNCTUATION = Regex("""[{}[\]()]""")

class SyntaxVisualTransformation(private val fileName: String) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return TransformedText(
            highlightSyntax(text.text, extension),
            OffsetMapping.Identity
        )
    }

    private fun highlightSyntax(text: String, extension: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            val isStyled = BooleanArray(text.length)

            fun applyStyle(range: IntRange, style: SpanStyle) {
                if (range.isEmpty()) return
                // Safe overlapping: Ensure none of the characters in the range are already styled
                for (i in range) {
                    if (i in isStyled.indices && isStyled[i]) return
                }
                addStyle(style, range.first, range.last + 1)
                for (i in range) {
                    if (i in isStyled.indices) {
                        isStyled[i] = true
                    }
                }
            }

            val isHtmlXml = extension == "html" || extension == "xml"

            // 1. Comments (SuccessGreen)
            if (isHtmlXml) {
                HTML_COMMENT.findAll(text).forEach { applyStyle(it.range, SpanStyle(color = SuccessGreen)) }
            } else {
                LINE_COMMENT.findAll(text).forEach { applyStyle(it.range, SpanStyle(color = SuccessGreen)) }
                BLOCK_COMMENT.findAll(text).forEach { applyStyle(it.range, SpanStyle(color = SuccessGreen)) }
            }

            // 2. Strings (AccentAmber)
            STRINGS.findAll(text).forEach { applyStyle(it.range, SpanStyle(color = AccentAmber)) }

            // 3. Tags, Attributes, Keywords, Numbers
            if (isHtmlXml) {
                HTML_TAGS.findAll(text).forEach { applyStyle(it.range, SpanStyle(color = AccentCyan)) }
                HTML_ATTRS.findAll(text).forEach { applyStyle(it.range, SpanStyle(color = SuccessGreen)) }
            } else {
                KEYWORDS.findAll(text).forEach { applyStyle(it.range, SpanStyle(color = AccentCyan)) }
                NUMBERS.findAll(text).forEach { applyStyle(it.range, SpanStyle(color = AccentAmber)) }
            }

            // 4. Punctuation (OnSurfaceDim)
            PUNCTUATION.findAll(text).forEach { applyStyle(it.range, SpanStyle(color = OnSurfaceDim)) }
        }
    }
}
