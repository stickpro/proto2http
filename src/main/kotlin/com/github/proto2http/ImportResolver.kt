package com.github.proto2http

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * Import resolver for proto files.
 * Supports:
 * - Relative paths from current file
 * - Project-wide search
 * - Recursive resolution of nested imports
 */
object ImportResolver {

    /**
     * Parses proto file with recursive resolution of all imports
     */
    fun parseWithImports(
        protoFile: VirtualFile,
        project: Project
    ): ProtoData {
        val resolved = mutableSetOf<String>()
        return parseRecursively(protoFile, project, resolved)
    }

    private fun parseRecursively(
        protoFile: VirtualFile,
        project: Project,
        resolved: MutableSet<String>
    ): ProtoData {
        val filePath = protoFile.path
        if (filePath in resolved) {
            return ProtoData("", emptyList(), emptyMap(), emptyMap())
        }
        resolved.add(filePath)

        val content = String(protoFile.contentsToByteArray(), Charsets.UTF_8)
        var protoData = ProtoParser.parse(content)

        // Recursively resolve imports
        for (importPath in protoData.imports) {
            val importedFile = resolveImport(importPath, protoFile, project)
            if (importedFile != null) {
                val importedData = parseRecursively(importedFile, project, resolved)
                protoData = protoData.mergeImported(importedData)
            }
        }

        return protoData
    }

    /**
     * Resolves import path to VirtualFile
     */
    private fun resolveImport(
        importPath: String,
        currentFile: VirtualFile,
        project: Project
    ): VirtualFile? {
        // 1. Try relative path from current file
        val parentDir = currentFile.parent
        val relativeFile = parentDir?.findFileByRelativePath(importPath)
        if (relativeFile != null && relativeFile.exists()) {
            return relativeFile
        }

        // 2. Search by filename in project
        val fileName = importPath.substringAfterLast("/")
        val scope = GlobalSearchScope.projectScope(project)
        val files = FilenameIndex.getVirtualFilesByName(fileName, scope)

        // Prioritize files whose path ends with importPath
        return files.find { it.path.endsWith(importPath) }
            ?: files.firstOrNull()
    }
}
