package com.github.proto2http

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

/**
 * Action for bulk generation of .http files from all .proto files in a directory
 */
class GenerateHttpBulkAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        // Show only for directories containing .proto files
        val isDirectoryWithProtos = file != null && file.isDirectory && hasProtoFiles(file)
        e.presentation.isEnabledAndVisible = isDirectoryWithProtos
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val directory = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (!directory.isDirectory) return

        val protoFiles = collectProtoFiles(directory)
        if (protoFiles.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No .proto files found in the selected directory.",
                "Proto2HTTP"
            )
            return
        }

        generateAllWithProgress(project, protoFiles)
    }

    private fun hasProtoFiles(directory: VirtualFile): Boolean {
        return directory.children.any { child ->
            child.extension == "proto" || (child.isDirectory && hasProtoFiles(child))
        }
    }

    private fun collectProtoFiles(directory: VirtualFile, recursive: Boolean = true): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()

        for (child in directory.children) {
            if (child.extension == "proto") {
                result.add(child)
            } else if (recursive && child.isDirectory) {
                result.addAll(collectProtoFiles(child, recursive))
            }
        }

        return result
    }

    private fun generateAllWithProgress(project: Project, protoFiles: List<VirtualFile>) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "Generating HTTP files...",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                var successCount = 0
                var skippedCount = 0
                var errorCount = 0

                protoFiles.forEachIndexed { index, protoFile ->
                    if (indicator.isCanceled) return

                    indicator.text = "Processing: ${protoFile.name}"
                    indicator.fraction = index.toDouble() / protoFiles.size

                    try {
                        var generated = false
                        ApplicationManager.getApplication().invokeAndWait {
                            val result = GenerateHttpAction.generateHttpFile(
                                protoFile,
                                project,
                                skipEmpty = true
                            )
                            generated = result != null
                        }
                        if (generated) {
                            successCount++
                        } else {
                            skippedCount++
                        }
                    } catch (e: Exception) {
                        errorCount++
                    }
                }

                indicator.fraction = 1.0

                // Show result
                ApplicationManager.getApplication().invokeLater {
                    val message = buildString {
                        append("Generated $successCount HTTP file(s)")
                        if (skippedCount > 0) {
                            append("\nSkipped: $skippedCount file(s) (no services)")
                        }
                        if (errorCount > 0) {
                            append("\nFailed: $errorCount file(s)")
                        }
                    }
                    Messages.showInfoMessage(project, message, "Proto2HTTP - Bulk Generation")
                }
            }
        })
    }
}
