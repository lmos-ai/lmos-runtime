/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import com.vanniktech.maven.publish.SonatypeHost
import java.lang.System.getenv
import java.net.URI

plugins {
    id("org.springframework.boot") version "3.4.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.cadixdev.licenser") version "0.6.1"

    id("com.citi.helm") version "2.2.0"
    id("com.citi.helm-publish") version "2.2.0"
    id("net.researchgate.release") version "3.0.2"
    id("com.vanniktech.maven.publish") version "0.30.0"
    kotlin("jvm")
    kotlin("kapt") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21" apply false
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
    id("org.jetbrains.dokka") version "1.9.20"
}

val springBootVersion by extra { "3.4.0" }

fun getProperty(propertyName: String) = getenv(propertyName) ?: project.findProperty(propertyName) as String

mavenPublishing {
    publishToMavenCentral(SonatypeHost.DEFAULT)
    signAllPublications()

    pom {
        name = "LMOS Runtime"
        description =
            """The LMOS Runtime facilitates dynamic agent routing and conversation handling in a multi-tenant, multi-channel environment.
            """.trimMargin()
        url = "https://github.com/eclipse-lmos/lmos-runtime"
        licenses {
            license {
                name = "Apache-2.0"
                distribution = "repo"
                url = "https://github.com/eclipse-lmos/lmos-runtime/blob/main/LICENSES/Apache-2.0.txt"
            }
        }
        developers {
            developer {
                id = "telekom"
                name = "Telekom Open Source"
                email = "opensource@telekom.de"
            }
        }
        scm {
            url = "https://github.com/eclipse-lmos/lmos-runtime.git"
        }
    }

    release {
        buildTasks = listOf("releaseBuild")
        ignoredSnapshotDependencies =
            listOf()
        newVersionCommitMessage = "New Snapshot-Version:"
        preTagCommitMessage = "Release:"
    }

    tasks.register("releaseBuild") {
        dependsOn(subprojects.mapNotNull { it.tasks.findByName("build") })
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/eclipse-lmos/lmos-runtime")
            credentials {
                username = findProperty("GITHUB_USER")?.toString() ?: getenv("GITHUB_USER")
                password = findProperty("GITHUB_TOKEN")?.toString() ?: getenv("GITHUB_TOKEN")
            }
        }
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

group = "org.eclipse.lmos"

subprojects {
    group = "org.eclipse.lmos"

    apply(plugin = "kotlin")
    apply(plugin = "kotlinx-serialization")
    apply(plugin = "org.jetbrains.kotlinx.kover")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "com.vanniktech.maven.publish")

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    dependencies {
        testImplementation(kotlin("test"))
        testImplementation("io.mockk:mockk:1.13.13")
    }

    tasks.test {
        useJUnitPlatform()
    }
}

dependencies {
    kover(project("lmos-runtime-core"))
    kover(project("lmos-runtime-spring-boot-starter"))
    kover(project("lmos-runtime-service"))
}
