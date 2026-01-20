package com.github.proto2http

import com.github.proto2http.settings.PluginSettings

data class GenerationOptions(
    val host: String = "localhost:50051",
    val includeComments: Boolean = true,
    val generateEmptyBodies: Boolean = false
) {
    companion object {
        fun fromSettings(): GenerationOptions {
            val settings = PluginSettings.getInstance()
            return GenerationOptions(
                host = settings.defaultHost,
                includeComments = settings.includeComments,
                generateEmptyBodies = settings.generateEmptyBodies
            )
        }
    }
}

object HttpGenerator {

    /**
     * Google Well-Known Types with their JSON representation
     * https://protobuf.dev/reference/protobuf/google.protobuf/
     */
    private val wellKnownTypes = mapOf(
        // Timestamp: RFC 3339 format
        "google.protobuf.Timestamp" to "\"2024-01-01T00:00:00Z\"",
        "Timestamp" to "\"2024-01-01T00:00:00Z\"",

        // Duration: seconds with 's' suffix
        "google.protobuf.Duration" to "\"3600s\"",
        "Duration" to "\"3600s\"",

        // Empty: empty object
        "google.protobuf.Empty" to "{}",
        "Empty" to "{}",

        // FieldMask: comma-separated field paths
        "google.protobuf.FieldMask" to "\"field1,field2\"",
        "FieldMask" to "\"field1,field2\"",

        // Struct: arbitrary JSON object
        "google.protobuf.Struct" to "{\n    \"key\": \"value\"\n  }",
        "Struct" to "{\n    \"key\": \"value\"\n  }",

        // Value: any JSON value
        "google.protobuf.Value" to "\"value\"",
        "Value" to "\"value\"",

        // ListValue: JSON array
        "google.protobuf.ListValue" to "[\"item1\", \"item2\"]",
        "ListValue" to "[\"item1\", \"item2\"]",

        // NullValue
        "google.protobuf.NullValue" to "null",
        "NullValue" to "null",

        // Any: requires @type
        "google.protobuf.Any" to "{\n    \"@type\": \"type.googleapis.com/google.protobuf.StringValue\",\n    \"value\": \"string\"\n  }",
        "Any" to "{\n    \"@type\": \"type.googleapis.com/google.protobuf.StringValue\",\n    \"value\": \"string\"\n  }",

        // Wrapper types - unwrapped to primitives
        "google.protobuf.DoubleValue" to "0.0",
        "DoubleValue" to "0.0",
        "google.protobuf.FloatValue" to "0.0",
        "FloatValue" to "0.0",
        "google.protobuf.Int64Value" to "0",
        "Int64Value" to "0",
        "google.protobuf.UInt64Value" to "0",
        "UInt64Value" to "0",
        "google.protobuf.Int32Value" to "0",
        "Int32Value" to "0",
        "google.protobuf.UInt32Value" to "0",
        "UInt32Value" to "0",
        "google.protobuf.BoolValue" to "false",
        "BoolValue" to "false",
        "google.protobuf.StringValue" to "\"string\"",
        "StringValue" to "\"string\"",
        "google.protobuf.BytesValue" to "\"base64encoded\"",
        "BytesValue" to "\"base64encoded\""
    )

    fun generate(
        proto: ProtoData,
        sourceFileName: String,
        options: GenerationOptions = GenerationOptions.fromSettings()
    ): String {
        val sb = StringBuilder()

        sb.appendLine("### Generated from: $sourceFileName")
        sb.appendLine("### Package: ${proto.packageName}")
        sb.appendLine()
        sb.appendLine("@host = ${options.host}")
        sb.appendLine()

        for (service in proto.services) {
            val fullServiceName = if (proto.packageName.isNotEmpty()) {
                "${proto.packageName}.${service.name}"
            } else {
                service.name
            }

            sb.appendLine("###############################################################################")
            sb.appendLine("### ${service.name}")
            if (options.includeComments && service.comment != null) {
                sb.appendLine("### ${service.comment}")
            }
            sb.appendLine("###############################################################################")
            sb.appendLine()

            for (method in service.methods) {
                if (options.includeComments) {
                    val comment = method.comment ?: method.name
                    sb.appendLine("### $comment")
                }
                sb.appendLine("GRPC {{host}}/$fullServiceName/${method.name}")
                sb.appendLine()

                val requestBody = if (options.generateEmptyBodies) {
                    "{}"
                } else {
                    generateRequestBody(method.inputType, proto)
                }
                sb.appendLine(requestBody)
                sb.appendLine()
            }
        }

        return sb.toString().trimEnd() + "\n"
    }

    private fun generateRequestBody(messageType: String, proto: ProtoData): String {
        val message = proto.messages[messageType]
            ?: return "{}"

        val json = generateJsonForMessage(message, proto, mutableSetOf())
        return json
    }

    private fun generateJsonForMessage(
        message: MessageData,
        proto: ProtoData,
        visited: MutableSet<String>
    ): String {
        if (message.name in visited) {
            return "{}"
        }
        visited.add(message.name)

        val fields = message.fields.map { field ->
            val value = generateFieldValue(field, proto, visited)
            "  \"${field.name}\": $value"
        }

        visited.remove(message.name)

        return if (fields.isEmpty()) {
            "{}"
        } else {
            "{\n${fields.joinToString(",\n")}\n}"
        }
    }

    private fun generateFieldValue(
        field: FieldData,
        proto: ProtoData,
        visited: MutableSet<String>
    ): String {
        val baseValue = when (field.type) {
            "string" -> "\"string\""
            "int32", "int64", "uint32", "uint64", "sint32", "sint64" -> "0"
            "float", "double" -> "0.0"
            "bool" -> "false"
            "bytes" -> "\"base64encoded\""
            else -> {
                // 1. Check Well-Known Types
                val wktValue = wellKnownTypes[field.type]
                if (wktValue != null) {
                    wktValue
                }
                // 2. Check if it's an enum (by short or fully qualified name)
                else {
                    val enumData = findEnum(field.type, proto)
                    if (enumData != null) {
                        val firstNonZero = enumData.values.find { !it.endsWith("UNSPECIFIED") }
                            ?: enumData.values.firstOrNull()
                            ?: "0"
                        "\"$firstNonZero\""
                    } else {
                        // 3. It's a nested message (by short or fully qualified name)
                        val nestedMessage = findMessage(field.type, proto)
                        if (nestedMessage != null) {
                            generateJsonForMessage(nestedMessage, proto, visited)
                                .lines()
                                .mapIndexed { index, line -> if (index == 0) line else "  $line" }
                                .joinToString("\n")
                        } else {
                            "{}"
                        }
                    }
                }
            }
        }

        return if (field.isRepeated) {
            "[$baseValue]"
        } else {
            baseValue
        }
    }

    /**
     * Finds message by short or fully qualified name (with package)
     */
    private fun findMessage(typeName: String, proto: ProtoData): MessageData? {
        // Direct lookup by name
        proto.messages[typeName]?.let { return it }

        // Lookup by short name (last part after dot)
        val shortName = typeName.substringAfterLast(".")
        return proto.messages[shortName]
    }

    /**
     * Finds enum by short or fully qualified name (with package)
     */
    private fun findEnum(typeName: String, proto: ProtoData): EnumData? {
        // Direct lookup by name
        proto.enums[typeName]?.let { return it }

        // Lookup by short name (last part after dot)
        val shortName = typeName.substringAfterLast(".")
        return proto.enums[shortName]
    }
}
