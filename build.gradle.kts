/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
    id("org.jetbrains.dokka") version "1.9.20"

    id("org.springframework.boot") version "3.3.4" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    id("org.cadixdev.licenser") version "0.6.1"

    id("com.citi.helm") version "2.2.0" apply false
    id("com.citi.helm-publish") version "2.2.0" apply false
    id("net.researchgate.release") version "3.0.2" apply false
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
}

val springBootVersion by extra { "3.3.5" }

repositories {
    mavenCentral()
    mavenLocal()
}

subprojects {
    group = "ai.ancf.lmos"

    apply(plugin = "kotlin")
    apply(plugin = "kotlinx-serialization")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    repositories {
        mavenCentral()
        mavenLocal()
    }

    dependencies {
        testImplementation(kotlin("test"))
        testImplementation("io.mockk:mockk:1.13.13")
    }

    tasks.test {
        useJUnitPlatform()
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

license {
    include("**/*.java")
    include("**/*.kt")
    include("**/*.yaml")
    exclude("**/*.properties")
}

fun getProperty(propertyName: String) = System.getenv(propertyName) ?: project.findProperty(propertyName) as String

dependencies {
    kover(project("lmos-runtime-core"))
    kover(project("lmos-runtime-spring-boot-starter"))
    kover(project("lmos-runtime-service"))
}
