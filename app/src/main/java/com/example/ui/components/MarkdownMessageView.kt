package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanTertiaryDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface MarkdownBlock {
    data class TextParagraph(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
}

private fun splitMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    val textBuffer = StringBuilder()
    val codeBuffer = StringBuilder()
    var inCodeBlock = false
    var codeLang = "code"

    for (line in lines) {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("```")) {
            if (!inCodeBlock) {
                if (textBuffer.isNotBlank()) {
                    blocks.add(MarkdownBlock.TextParagraph(textBuffer.toString().trimEnd()))
                    textBuffer.clear()
                }
                inCodeBlock = true
                codeLang = trimmed.removePrefix("```").trim().ifBlank { "code" }
            } else {
                blocks.add(MarkdownBlock.CodeBlock(codeLang, codeBuffer.toString().trimEnd()))
                codeBuffer.clear()
                inCodeBlock = false
                codeLang = "code"
            }
        } else {
            if (inCodeBlock) {
                codeBuffer.appendLine(line)
            } else {
                textBuffer.appendLine(line)
            }
        }
    }

    if (inCodeBlock && codeBuffer.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(codeLang, codeBuffer.toString().trimEnd()))
    } else if (textBuffer.isNotBlank()) {
        blocks.add(MarkdownBlock.TextParagraph(textBuffer.toString().trimEnd()))
    }

    return blocks
}

@Composable
fun MarkdownMessageView(
    content: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val blocks = remember(content) { splitMarkdownBlocks(content) }

    SelectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.CodeBlock -> {
                        CodeBlockCard(language = block.language, code = block.code)
                    }
                    is MarkdownBlock.TextParagraph -> {
                        FormattedTextParagraph(
                            rawText = block.text,
                            baseColor = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(language: String, code: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0B1120),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131C31))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = language.uppercase(),
                    color = CyanTertiaryDark,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (copied) {
                        Text(
                            text = "Code copied",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(code))
                            copied = true
                            MainScope().launch {
                                delay(1600)
                                copied = false
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = if (copied) "Code copied" else "Copy code only",
                            tint = if (copied) EmeraldSuccess else Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    color = Color(0xFFE2E8F0),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun FormattedTextParagraph(rawText: String, baseColor: Color) {
    val accentColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val codeFg = MaterialTheme.colorScheme.tertiary

    val lines = rawText.lines()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("### ") -> {
                    Text(
                        text = buildInlineAnnotated(trimmed.removePrefix("### "), baseColor, codeBg, codeFg),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = baseColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = buildInlineAnnotated(trimmed.removePrefix("## "), baseColor, codeBg, codeFg),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = baseColor,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = buildInlineAnnotated(trimmed.removePrefix("# "), baseColor, codeBg, codeFg),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = baseColor,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                trimmed.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(accentColor, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildInlineAnnotated(trimmed.removePrefix("> "), baseColor, codeBg, codeFg),
                            style = MaterialTheme.typography.bodyLarge,
                            color = baseColor
                        )
                    }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = "•",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = buildInlineAnnotated(trimmed.drop(2), baseColor, codeBg, codeFg),
                            style = MaterialTheme.typography.bodyLarge,
                            color = baseColor
                        )
                    }
                }
                else -> {
                    if (line.isBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                    } else {
                        Text(
                            text = buildInlineAnnotated(line, baseColor, codeBg, codeFg),
                            style = MaterialTheme.typography.bodyLarge,
                            color = baseColor
                        )
                    }
                }
            }
        }
    }
}

private fun buildInlineAnnotated(
    text: String,
    baseColor: Color,
    codeBg: Color,
    codeFg: Color
): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append("**")
                    i += 2
                }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.sp,
                            background = codeBg,
                            color = codeFg
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                } else {
                    append('`')
                    i++
                }
            }
            text[i] == '*' && !text.startsWith("**", i) -> {
                val end = text.indexOf('*', i + 1)
                if (end != -1 && end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append('*')
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
