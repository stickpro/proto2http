package com.github.proto2http.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel

class PluginSettingsConfigurable : Configurable {

    private var panel: JPanel? = null
    private var hostField: JBTextField? = null
    private var outputDirCombo: ComboBox<PluginSettings.OutputDirectory>? = null
    private var customPathField: TextFieldWithBrowseButton? = null
    private var includeCommentsCheckbox: JBCheckBox? = null
    private var emptyBodiesCheckbox: JBCheckBox? = null

    private val settings get() = PluginSettings.getInstance()

    override fun getDisplayName(): String = "Proto2HTTP"

    override fun createComponent(): JComponent {
        panel = panel {
            group("Generation Settings") {
                row("Default host:") {
                    hostField = textField()
                        .columns(30)
                        .comment("Host for gRPC requests (e.g., localhost:50051)")
                        .component as JBTextField
                }

                row {
                    includeCommentsCheckbox = checkBox("Include comments from proto file")
                        .component
                }

                row {
                    emptyBodiesCheckbox = checkBox("Generate empty request bodies")
                        .comment("If checked, generates {} instead of example values")
                        .component
                }
            }

            group("Output Directory") {
                row("Save .http files to:") {
                    outputDirCombo = comboBox(
                        listOf(
                            PluginSettings.OutputDirectory.SAME_AS_PROTO,
                            PluginSettings.OutputDirectory.PROJECT_ROOT,
                            PluginSettings.OutputDirectory.CUSTOM
                        ),
                        object : SimpleListCellRenderer<PluginSettings.OutputDirectory>() {
                            override fun customize(
                                list: JList<out PluginSettings.OutputDirectory>,
                                value: PluginSettings.OutputDirectory?,
                                index: Int,
                                selected: Boolean,
                                hasFocus: Boolean
                            ) {
                                text = when (value) {
                                    PluginSettings.OutputDirectory.SAME_AS_PROTO -> "Same directory as .proto file"
                                    PluginSettings.OutputDirectory.PROJECT_ROOT -> "Project root"
                                    PluginSettings.OutputDirectory.CUSTOM -> "Custom directory"
                                    null -> ""
                                }
                            }
                        }
                    ).component
                }

                row("Custom path:") {
                    val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Output Directory")
                    customPathField = textFieldWithBrowseButton(fileChooserDescriptor = descriptor)
                        .columns(40)
                        .component
                }
            }
        }

        // Добавляем слушатель для включения/выключения поля custom path
        outputDirCombo?.addActionListener {
            updateCustomPathEnabled()
        }

        return panel!!
    }

    private fun updateCustomPathEnabled() {
        customPathField?.isEnabled =
            outputDirCombo?.selectedItem == PluginSettings.OutputDirectory.CUSTOM
    }

    override fun isModified(): Boolean {
        return hostField?.text != settings.defaultHost ||
                outputDirCombo?.selectedItem != settings.outputDirectory ||
                customPathField?.text != settings.customOutputPath ||
                includeCommentsCheckbox?.isSelected != settings.includeComments ||
                emptyBodiesCheckbox?.isSelected != settings.generateEmptyBodies
    }

    override fun apply() {
        settings.defaultHost = hostField?.text ?: "localhost:50051"
        settings.outputDirectory = outputDirCombo?.selectedItem as? PluginSettings.OutputDirectory
            ?: PluginSettings.OutputDirectory.SAME_AS_PROTO
        settings.customOutputPath = customPathField?.text ?: ""
        settings.includeComments = includeCommentsCheckbox?.isSelected ?: true
        settings.generateEmptyBodies = emptyBodiesCheckbox?.isSelected ?: false
    }

    override fun reset() {
        hostField?.text = settings.defaultHost
        outputDirCombo?.selectedItem = settings.outputDirectory
        customPathField?.text = settings.customOutputPath
        includeCommentsCheckbox?.isSelected = settings.includeComments
        emptyBodiesCheckbox?.isSelected = settings.generateEmptyBodies
        updateCustomPathEnabled()
    }

    override fun disposeUIResources() {
        panel = null
        hostField = null
        outputDirCombo = null
        customPathField = null
        includeCommentsCheckbox = null
        emptyBodiesCheckbox = null
    }
}
