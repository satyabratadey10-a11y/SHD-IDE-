package com.example.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.SuccessGreen

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
            
            // Strings: "..." or '...'
            val stringRegex = Regex("(\"[^\"]*\")|('[^']*')")
            stringRegex.findAll(text).forEach { matchResult ->
                addStyle(SpanStyle(color = AccentAmber), matchResult.range.first, matchResult.range.last + 1)
            }

            // Comments
            val singleLineCommentRegex = Regex("//.*")
            singleLineCommentRegex.findAll(text).forEach { matchResult ->
                addStyle(SpanStyle(color = SuccessGreen), matchResult.range.first, matchResult.range.last + 1)
            }
            
            val multiLineCommentRegex = Regex("/\\*[\\s\\S]*?\\*/")
            multiLineCommentRegex.findAll(text).forEach { matchResult ->
                addStyle(SpanStyle(color = SuccessGreen), matchResult.range.first, matchResult.range.last + 1)
            }

            when (extension) {
                "kt", "java", "js", "ts", "py" -> {
                    val keywords = listOf(
                        "val", "var", "fun", "class", "interface", "object", "return",
                        "if", "else", "for", "while", "do", "when", "import", "package",
                        "true", "false", "null", "typeof", "function", "const", "let",
                        "def", "print", "from", "as", "public", "private", "protected"
                    ).joinToString("|")
                    
                    val keywordRegex = Regex("\\b($keywords)\\b")
                    keywordRegex.findAll(text).forEach { matchResult ->
                        addStyle(SpanStyle(color = AccentCyan), matchResult.range.first, matchResult.range.last + 1)
                    }
                }
                "html", "xml" -> {
                    // HTML Tags
                    val tagRegex = Regex("</?[a-zA-Z0-9]+>?|/?>")
                    tagRegex.findAll(text).forEach { matchResult ->
                        addStyle(SpanStyle(color = AccentCyan), matchResult.range.first, matchResult.range.last + 1)
                    }
                    
                    // HTML Attributes
                    val attrRegex = Regex("\\b[a-zA-Z\\-:]+(?=\\s*=)")
                    attrRegex.findAll(text).forEach { matchResult ->
                        addStyle(SpanStyle(color = SuccessGreen), matchResult.range.first, matchResult.range.last + 1)
                    }
                }
            }
        }
    }
}
