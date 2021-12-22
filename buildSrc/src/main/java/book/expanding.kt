package book

import com.natpryce.recover
import java.io.File
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.MULTILINE

private val noLogging: (String) -> Unit = {}
private val log: (String) -> Unit =
//     noLogging
    ::println

data class SourceRoots(
    val workedExample: File,
    val digressionCode: File
)

private fun SourceRoots.sourceRootFor(version: String?): File = when (version) {
    null -> digressionCode
    else -> workedExample
}

fun processFiles(
    inputRoot: File,
    outputRoot: File,
    sourceRoots: SourceRoots,
    abortOnFailure: Boolean,
    kotlinVersion: String
) {
    val diffTracks: List<DiffTracking> =
        inputRoot.walkTopDown()
            .filter { it.name.endsWith(".md") }
            .toList()
            .sortedBy { it.name }
            .also {
                if (it.isEmpty())
                    throw IllegalStateException("no Markdown files found!")
                else
                    println("expanding ${it.size} Markdown files")
            }
            .map { file ->
                processFile(
                    file,
                    outputRoot.resolve(file.relativeTo(inputRoot)),
                    sourceRoots,
                    abortOnFailure,
                    kotlinVersion
                )
            }
    
    val file = File("buildSrc/diff-tracking.json")
    file.parentFile.mkdirs()
    diffTracks.writeTo(file)
}

fun processFile(
    src: File,
    dest: File,
    roots: SourceRoots,
    abortOnFailure: Boolean,
    kotlinVersion: String
): DiffTracking {
    log("Processing $src")
    val text = src.readText()
    
    val diffTracking = DiffTracking(src)
    val newText = expandCodeBlocks(
        text,
        lookupWithRoot(roots, diffTracking, abortOnFailure, kotlinVersion, src)
    )
    dest.also {
        it.parentFile.mkdirs()
    }.writeText(newText)
    return diffTracking
}

private fun lookupWithRoot(
    roots: SourceRoots,
    diffTracking: DiffTracking,
    abortOnFailure: Boolean,
    kotlinVersion: String,
    srcFileForDebug: File
) = { key: String ->
    val (codeFile, fragment) = key.parse(roots)
    if (codeFile.exists) {
//        val linkTag = when (codeFile) {
//            is GitFile -> diffTracking.record(codeFile)
//            else -> null
//        }
        FileSnippet(codeFile, fragment, null, kotlinVersion)
            .rendered()
    } else {
        val message =
            "${srcFileForDebug.canonicalPath}:\n" +
                "inserted file $codeFile not found\n" +
                "(${codeFile.lines.recover { it.message }})"
        if (abortOnFailure) {
            error(message)
        } else {
            log(message)
            message
        }
    }
}

private fun String.parse(roots: SourceRoots): Pair<CodeFile, String?> {
    val (file, fragment) = this.trim().split("#").let { parts ->
        parts[0] to parts.getOrNull(1)
    }
    val (path, version) = file.split(":").let { parts ->
        parts.last() to parts.first().takeIf { parts.size == 2 }
    }
    return codeFileFor(roots, path, version) to fragment
}

private fun codeFileFor(roots: SourceRoots, path: String, version: String?): CodeFile =
    when (version) {
        null -> DiskFile(
            sourceRoot = roots.sourceRootFor(version),
            relativePath = path
        )
        else -> GitFile(
            sourceRoot = roots.sourceRootFor(version),
            relativePath = path, version = version
        )
    }

private fun expandCodeBlocks(text: String, lookup: (String) -> String): String =
    expandedCodeBlockFinder.replace(text) { matchResult ->
        val intro = matchResult.groups["intro"]!!.value
        val key = matchResult.groups["key"]!!.value
        val outro = matchResult.groups["outro"]!!.value
    
        val replacement = lookup(key)
        
        listOf(intro, replacement, outro)
            .joinToString("\n")
    }

//language=RegExp
private val expandedCodeBlockFinder =
    """^(?<intro><!--\s+begin-insert:\s+(?<key>.*?)\s+-->\s*$)(.*?)^(?<outro><!--\s+end-insert\s+-->\s*)$""".trimMargin()
        .toRegex(setOf(DOT_MATCHES_ALL, MULTILINE))
