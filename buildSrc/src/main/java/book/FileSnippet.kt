package book

import com.natpryce.onFailure

data class FileSnippet(
    val codeFile: CodeFile,
    val fragment: String?,
    val linkTag: String?,
    val kotlinVersion: String
) {
    fun rendered() =
        listOf(
            "```$sourceType",
            codeLines().joinToString("\n"),
            "```",
            linkTag
        ).filterNotNull().joinToString("\n")
    
    private fun codeLines() =
        codeFile.lines
            .onFailure { throw it.reason }
            .snipped(fragment, kotlinVersion)
            .also {
                if (it.isEmpty()) {
                    error("tag $fragment not found in $codeFile")
                }
            }
    
    private val sourceType = when (codeFile.extension) {
        "kt", "ktx", "kts" -> "kotlin"
        "java" -> "java"
        "gradle", "groovy" -> "groovy"
        else -> "text"
    }
}