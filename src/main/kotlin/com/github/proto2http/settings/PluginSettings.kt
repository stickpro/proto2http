package com.github.proto2http.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(
    name = "Proto2HttpSettings",
    storages = [Storage("proto2http.xml")]
)
class PluginSettings : PersistentStateComponent<PluginSettings.State> {

    data class State(
        var defaultHost: String = "localhost:50051",
        var outputDirectory: OutputDirectory = OutputDirectory.SAME_AS_PROTO,
        var customOutputPath: String = "",
        var includeComments: Boolean = true,
        var generateEmptyBodies: Boolean = false
    )

    enum class OutputDirectory {
        SAME_AS_PROTO,
        PROJECT_ROOT,
        CUSTOM
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var defaultHost: String
        get() = state.defaultHost
        set(value) { state.defaultHost = value }

    var outputDirectory: OutputDirectory
        get() = state.outputDirectory
        set(value) { state.outputDirectory = value }

    var customOutputPath: String
        get() = state.customOutputPath
        set(value) { state.customOutputPath = value }

    var includeComments: Boolean
        get() = state.includeComments
        set(value) { state.includeComments = value }

    var generateEmptyBodies: Boolean
        get() = state.generateEmptyBodies
        set(value) { state.generateEmptyBodies = value }

    companion object {
        fun getInstance(): PluginSettings =
            ApplicationManager.getApplication().getService(PluginSettings::class.java)
    }
}
