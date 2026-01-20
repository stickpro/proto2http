package com.github.proto2http

import com.github.proto2http.settings.PluginSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class GenerateHttpAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && file.extension == "proto"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val protoFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        generateHttpFile(protoFile, project, openInEditor = true)
    }

    companion object {
        /**
         * Generates .http file from .proto file
         * @param skipEmpty if true, skips file creation when no methods found
         * @return created VirtualFile or null if nothing to generate
         */
        fun generateHttpFile(
            protoFile: VirtualFile,
            project: Project,
            openInEditor: Boolean = false,
            skipEmpty: Boolean = false
        ): VirtualFile? {
            val protoData = ImportResolver.parseWithImports(protoFile, project)

            // Check for methods presence
            val hasMethods = protoData.services.any { it.methods.isNotEmpty() }
            if (skipEmpty && !hasMethods) {
                return null
            }

            val httpContent = HttpGenerator.generate(protoData, protoFile.name)

            val httpFileName = protoFile.nameWithoutExtension + ".http"
            val outputDir = getOutputDirectory(protoFile, project)

            var resultFile: VirtualFile? = null

            WriteCommandAction.runWriteCommandAction(project) {
                val existingFile = outputDir.findChild(httpFileName)
                val httpFile: VirtualFile = if (existingFile != null) {
                    existingFile
                } else {
                    outputDir.createChildData(this, httpFileName)
                }
                httpFile.setBinaryContent(httpContent.toByteArray(Charsets.UTF_8))
                resultFile = httpFile

                if (openInEditor) {
                    FileEditorManager.getInstance(project).openFile(httpFile, true)
                }
            }

            return resultFile
        }

        private fun getOutputDirectory(protoFile: VirtualFile, project: Project): VirtualFile {
            val settings = PluginSettings.getInstance()

            return when (settings.outputDirectory) {
                PluginSettings.OutputDirectory.SAME_AS_PROTO -> {
                    protoFile.parent
                }
                PluginSettings.OutputDirectory.PROJECT_ROOT -> {
                    project.basePath?.let { basePath ->
                        LocalFileSystem.getInstance().findFileByPath(basePath)
                    } ?: protoFile.parent
                }
                PluginSettings.OutputDirectory.CUSTOM -> {
                    val customPath = settings.customOutputPath
                    if (customPath.isNotBlank()) {
                        val file = File(customPath)
                        if (!file.exists()) {
                            file.mkdirs()
                        }
                        LocalFileSystem.getInstance().refreshAndFindFileByPath(customPath)
                            ?: protoFile.parent
                    } else {
                        protoFile.parent
                    }
                }
            }
        }
    }
}
