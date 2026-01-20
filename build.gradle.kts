plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "com.github.proto2http"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        pluginVerifier()
        zipSigner()
    }
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginVerification {
        ides {
            ide("IC", "2024.1")  // IntelliJ IDEA Community
            ide("IU", "2024.1")  // IntelliJ IDEA Ultimate
            ide("PS", "2024.1")  // PhpStorm
            ide("WS", "2024.1")  // WebStorm
            ide("PY", "2024.1")  // PyCharm
            ide("GO", "2024.1")  // GoLand
            ide("CL", "2024.1")  // CLion
            ide("RD", "2024.1")  // Rider
        }
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("253.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
