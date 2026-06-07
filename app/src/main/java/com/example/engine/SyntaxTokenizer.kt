package com.example.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

object SyntaxTokenizer {

    enum class TokenType {
        KEYWORD, STRING, COMMENT, NUMBER, OPERATOR, IDENTIFIER, DEFAULT
    }

    data class Token(val range: IntRange, val type: TokenType)

    private val KEYWORDS = listOf(
        "abstract", "actual", "as", "class", "const", "crossinline", "data", "enum",
        "expect", "external", "final", "fun", "if", "else", "in", "inline", "inner",
        "interface", "internal", "is", "lateinit", "noinline", "open", "operator",
        "out", "override", "private", "protected", "public", "reified", "sealed",
        "suspend", "tailrec", "val", "var", "vararg", "when", "return", "true", "false",
        "null", "break", "continue", "object", "typealias", "import", "package", "for",
        "while", "try", "catch", "finally", "throw", "this", "super", 
        // Javascript
        "function", "let", "const", "var", "typeof", "instanceof", "new", "await", "async", "yield"
    )

    private val KEYWORD_REGEX = "\\b(${KEYWORDS.joinToString("|")})\\b".toRegex()
    private val STRING_REGEX = "(\"[^\"]*\")|('[^']*')".toRegex()
    private val COMMENT_REGEX = "(//.*)|(/\\*[\\s\\S]*?\\*/)".toRegex()
    private val NUMBER_REGEX = "\\b(\\d+(\\.\\d+)?)\\b".toRegex()
    private val OPERATOR_REGEX = "([=+\\-*/%&|<>!^~]+)|(\\b(and|or|not)\\b)".toRegex()

    fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        
        // Find comments first
        COMMENT_REGEX.findAll(text).forEach { match ->
            tokens.add(Token(match.range, TokenType.COMMENT))
        }
        
        // Find strings
        STRING_REGEX.findAll(text).forEach { match ->
            tokens.add(Token(match.range, TokenType.STRING))
        }

        // Keep track of mask
        val mask = BooleanArray(text.length)
        tokens.forEach { token ->
            for (i in token.range) {
                mask[i] = true
            }
        }

        KEYWORD_REGEX.findAll(text).forEach { match ->
            if (!mask[match.range.first]) {
                tokens.add(Token(match.range, TokenType.KEYWORD))
                for (i in match.range) mask[i] = true
            }
        }

        NUMBER_REGEX.findAll(text).forEach { match ->
            if (!mask[match.range.first]) {
                tokens.add(Token(match.range, TokenType.NUMBER))
                for (i in match.range) mask[i] = true
            }
        }

        OPERATOR_REGEX.findAll(text).forEach { match ->
            if (!mask[match.range.first]) {
                tokens.add(Token(match.range, TokenType.OPERATOR))
            }
        }

        return tokens
    }

    fun applySyntaxHighlighting(
        text: String,
        keywordColor: Color = Color(0xFFC678DD),
        stringColor: Color = Color(0xFF98C379),
        commentColor: Color = Color(0xFF5C6370),
        numberColor: Color = Color(0xFFD19A66),
        operatorColor: Color = Color(0xFF56B6C2),
        defaultColor: Color = Color(0xFFE6EDF3)
    ): AnnotatedString {
        val tokens = tokenize(text)
        
        return buildAnnotatedString {
            append(text)
            addStyle(SpanStyle(color = defaultColor), 0, text.length)
            
            tokens.forEach { token ->
                val color = when (token.type) {
                    TokenType.KEYWORD -> keywordColor
                    TokenType.STRING -> stringColor
                    TokenType.COMMENT -> commentColor
                    TokenType.NUMBER -> numberColor
                    TokenType.OPERATOR -> operatorColor
                    TokenType.IDENTIFIER -> defaultColor
                    TokenType.DEFAULT -> defaultColor
                }
                addStyle(SpanStyle(color = color), token.range.first, token.range.last + 1)
            }
        }
    }
}
