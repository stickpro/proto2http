package com.github.proto2http

data class ProtoData(
    val packageName: String,
    val services: List<ServiceData>,
    val messages: Map<String, MessageData>,
    val enums: Map<String, EnumData>,
    val imports: List<String> = emptyList()
) {
    /**
     * Merges data from an imported proto file
     */
    fun mergeImported(imported: ProtoData): ProtoData {
        return copy(
            messages = imported.messages + this.messages,
            enums = imported.enums + this.enums
        )
    }
}

data class ServiceData(
    val name: String,
    val methods: List<MethodData>,
    val comment: String? = null
)

data class MethodData(
    val name: String,
    val inputType: String,
    val outputType: String,
    val comment: String? = null
)

data class MessageData(
    val name: String,
    val fields: List<FieldData>
)

data class FieldData(
    val name: String,
    val type: String,
    val isRepeated: Boolean = false,
    val isOptional: Boolean = false
)

data class EnumData(
    val name: String,
    val values: List<String>
)

object ProtoParser {

    fun parse(content: String): ProtoData {
        val packageName = parsePackage(content)
        val imports = parseImports(content)
        val enums = parseEnums(content)
        val messages = parseMessages(content)
        val services = parseServices(content)

        return ProtoData(packageName, services, messages, enums, imports)
    }

    private fun parseImports(content: String): List<String> {
        val importRegex = Regex("""import\s*(?:public\s+|weak\s+)?"([^"]+)"\s*;""")
        return importRegex.findAll(content)
            .map { it.groupValues[1] }
            .toList()
    }

    private fun parsePackage(content: String): String {
        val regex = Regex("""package\s+([\w.]+)\s*;""")
        return regex.find(content)?.groupValues?.get(1) ?: ""
    }

    private fun parseEnums(content: String): Map<String, EnumData> {
        val enums = mutableMapOf<String, EnumData>()
        val enumRegex = Regex("""enum\s+(\w+)\s*\{([^}]+)\}""", RegexOption.DOT_MATCHES_ALL)

        enumRegex.findAll(content).forEach { match ->
            val name = match.groupValues[1]
            val body = match.groupValues[2]
            val values = Regex("""(\w+)\s*=\s*\d+""").findAll(body)
                .map { it.groupValues[1] }
                .toList()
            enums[name] = EnumData(name, values)
        }

        return enums
    }

    private fun parseMessages(content: String): Map<String, MessageData> {
        val messages = mutableMapOf<String, MessageData>()
        val messageRegex = Regex("""message\s+(\w+)\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""", RegexOption.DOT_MATCHES_ALL)

        messageRegex.findAll(content).forEach { match ->
            val name = match.groupValues[1]
            val body = match.groupValues[2]
            val fields = parseFields(body)
            messages[name] = MessageData(name, fields)
        }

        return messages
    }

    private fun parseFields(body: String): List<FieldData> {
        val fields = mutableListOf<FieldData>()
        val fieldRegex = Regex("""(repeated\s+|optional\s+)?(\w+(?:\.\w+)*)\s+(\w+)\s*=\s*\d+""")

        fieldRegex.findAll(body).forEach { match ->
            val modifier = match.groupValues[1].trim()
            val type = match.groupValues[2]
            val name = match.groupValues[3]

            fields.add(FieldData(
                name = name,
                type = type,
                isRepeated = modifier == "repeated",
                isOptional = modifier == "optional"
            ))
        }

        return fields
    }

    private fun parseServices(content: String): List<ServiceData> {
        val services = mutableListOf<ServiceData>()
        val serviceRegex = Regex("""(//[^\n]*\n\s*)?service\s+(\w+)\s*\{([^}]+)\}""", RegexOption.DOT_MATCHES_ALL)

        serviceRegex.findAll(content).forEach { match ->
            val comment = match.groupValues[1].takeIf { it.isNotBlank() }
                ?.lines()?.firstOrNull()?.replace("//", "")?.trim()
            val name = match.groupValues[2]
            val body = match.groupValues[3]
            val methods = parseMethods(body)
            services.add(ServiceData(name, methods, comment))
        }

        return services
    }

    private fun parseMethods(body: String): List<MethodData> {
        val methods = mutableListOf<MethodData>()
        val methodRegex = Regex("""(//[^\n]*\n\s*)?rpc\s+(\w+)\s*\(\s*(\w+)\s*\)\s*returns\s*\(\s*(\w+)\s*\)""")

        methodRegex.findAll(body).forEach { match ->
            val comment = match.groupValues[1].takeIf { it.isNotBlank() }
                ?.replace("//", "")?.trim()
            val name = match.groupValues[2]
            val inputType = match.groupValues[3]
            val outputType = match.groupValues[4]
            methods.add(MethodData(name, inputType, outputType, comment))
        }

        return methods
    }
}
