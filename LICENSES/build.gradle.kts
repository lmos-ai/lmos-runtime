// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.System.getenv
import java.net.URI

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "1.9.23" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
    id("org.jetbrains.dokka") version "1.9.20"
    id("org.cyclonedx.bom") version "1.8.2" apply false
    id("net.researchgate.release") version "3.0.2"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

val springBootVersion by extra { "3.3.5" }

group = "ai.ancf.lmos"

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "kotlinx-serialization")
    apply(plugin = "org.cyclonedx.bom")
    apply(plugin = "org.jetbrains.kotlinx.kover")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.vanniktech.maven.publish")

    version = rootProject.version

    repositories {
        mavenLocal()
        mavenCentral()
        maven { setUrl("https://repo.spring.io/milestone") }
        maven { setUrl("https://repo.spring.io/snapshot") }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            freeCompilerArgs += "-Xcontext-receivers"
            jvmTarget = "17"
        }
    }

    kotlin {
        jvmToolchain(17)
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        withSourcesJar()
        // withJavadocJar()
    }

    dependencies {
        testImplementation(kotlin("test"))
        testImplementation("io.mockk:mockk:1.13.13")
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.withType<Test> {
        val runFlowTests = project.findProperty("runFlowTests")?.toString()?.toBoolean() ?: false

        if (!runFlowTests) {
            exclude("**/*Flow*")
        }
    }

    val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
        dependsOn(tasks.dokkaJavadoc)
        from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
        archiveClassifier.set("javadoc")
    }

    mavenPublishing {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        signAllPublications()

        pom {
            name = "LMOS Router"
            description = "Efficient Agent Routing with SOTA Language and Embedding Models."
            url = "https://github.com/lmos-ai/lmos-router"
            licenses {
                license {
                    name = "Apache-2.0"
                    distribution = "repo"
                    url = "https://github.com/lmos-ai/lmos-router/blob/main/LICENSES/Apache-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "xmxnt"
                    name = "Amant Kumar"
                    email = "opensource@telekom.de"
                }
                developer {
                    id = "jas34"
                    name = "Jasbir Singh"
                    email = "opensource@telekom.de"
                }
                developer {
                    id = "merrenfx"
                    name = "Max Erren"
                    email = "opensource@telekom.de"
                }
            }
            scm {
                url = "https://github.com/lmos-ai/lmos-router.git"
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = URI("https://maven.pkg.github.com/lmos-ai/lmos-router")
                credentials {
                    username = findProperty("GITHUB_USER")?.toString() ?: getenv("GITHUB_USER")
                    password = findProperty("GITHUB_TOKEN")?.toString() ?: getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

repositories {
    mavenLocal()
    mavenCentral()
}

fun Project.java(configure: Action<JavaPluginExtension>): Unit = (this as ExtensionAware).extensions.configure("java", configure)

fun String.execWithCode(workingDir: File? = null): Pair<CommandResult, Sequence<String>> {
    ProcessBuilder().apply {
        workingDir?.let { directory(it) }
        command(split(" "))
        redirectErrorStream(true)
        val process = start()
        val result = process.readStream()
        val code = process.waitFor()
        return CommandResult(code) to result
    }
}

class CommandResult(val code: Int) {
    val isFailed = code != 0
    val isSuccess = !isFailed

    fun ifFailed(block: () -> Unit) {
        if (isFailed) block()
    }
}

fun Project.isBOM() = name.endsWith("-bom")

private fun Process.readStream() =
    sequence<String> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        try {
            var line: String?
            while (true) {
                line = reader.readLine()
                if (line == null) {
                    break
                }
                yield(line)
            }
        } finally {
            reader.close()
        }
    }

release {
    buildTasks = listOf("releaseBuild")
    ignoredSnapshotDependencies =
        listOf(
            "org.springframework.ai:spring-ai-bom",
            "org.springframework.ai:spring-ai-core",
            "org.springframework.ai:spring-ai-openai-spring-boot-starter",
            "org.springframework.ai:spring-ai-qdrant-store-spring-boot-starter",
        )
    newVersionCommitMessage = "New Snapshot-Version:"
    preTagCommitMessage = "Release:"
}

tasks.register("releaseBuild") {
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("build") })
}
