/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import com.vanniktech.maven.publish.SonatypeHost
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.lang.System.getenv
import java.net.URI

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
    id("org.jetbrains.dokka") version "1.9.20"

    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.cadixdev.licenser") version "0.6.1"

    id("com.citi.helm") version "2.2.0"
    id("com.citi.helm-publish") version "2.2.0"
    id("net.researchgate.release") version "3.0.2"
    id("com.vanniktech.maven.publish") version "0.29.0"
}

group = "ai.ancf.lmos"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

license {
    include("**/*.kt")
    include("**/*.yaml")
    exclude("**/*.properties")
}

fun getProperty(propertyName: String) = System.getenv(propertyName) ?: project.findProperty(propertyName) as String

tasks.named<BootBuildImage>("bootBuildImage") {
    val registryUrl = getProperty("REGISTRY_URL")
    val registryUsername = getProperty("REGISTRY_USERNAME")
    val registryPassword = getProperty("REGISTRY_PASSWORD")
    val registryNamespace = getProperty("REGISTRY_NAMESPACE")

    imageName.set("$registryUrl/$registryNamespace/${project.name}:${project.version}")
    publish = true
    docker {
        publishRegistry {
            url.set(registryUrl)
            username.set(registryUsername)
            password.set(registryPassword)
        }
    }
}

helm {
    charts {
        create("main") {
            chartName.set("${project.name}-chart")
            chartVersion.set("${project.version}")
            sourceDir.set(file("src/main/helm"))
        }
    }
}

tasks.register("replaceChartVersion") {
    doLast {
        val chartFile = file("src/main/helm/Chart.yaml")
        val content = chartFile.readText()
        val updatedContent = content.replace("\${chartVersion}", "${project.version}")
        chartFile.writeText(updatedContent)
    }
}

tasks.register("helmPush") {
    description = "Push Helm chart to OCI registry"
    group = "helm"
    dependsOn(tasks.named("helmPackageMainChart"))

    doLast {
        val registryUrl = getProperty("REGISTRY_URL")
        val registryUsername = getProperty("REGISTRY_USERNAME")
        val registryPassword = getProperty("REGISTRY_PASSWORD")
        val registryNamespace = getProperty("REGISTRY_NAMESPACE")

        helm.execHelm("registry", "login") {
            option("-u", registryUsername)
            option("-p", registryPassword)
            args(registryUrl)
        }

        helm.execHelm("push") {
            args(tasks.named("helmPackageMainChart").get().outputs.files.singleFile.toString())
            args("oci://$registryUrl/$registryNamespace")
        }

        helm.execHelm("registry", "logout") {
            args(registryUrl)
        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    pom {
        name = "LMOS Runtime"
        description = "LMOS Runtime is a component of the LMOS (Language Model Operating System) ecosystem, designed to facilitate dynamic agent routing and conversation handling in a multi-tenant, multi-channel environment."
        url = "https://github.com/lmos-ai/lmos-runtime"
        licenses {
            license {
                name = "Apache-2.0"
                distribution = "repo"
                url = "https://github.com/lmos-ai/lmos-runtime/blob/main/LICENSES/Apache-2.0.txt"
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
            url = "https://github.com/lmos-ai/lmos-runtime.git"
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
            url = URI("https://maven.pkg.github.com/lmos-ai/lmos-runtime")
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

dependencies {

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("io.github.lmos:lmos-router-llm:0.2.0")
    implementation("io.github.lmos-ai.arc:arc-agent-client:0.30.0")
    implementation("io.github.lmos-ai.arc:arc-api:0.30.0")

    val ktorVersion = "2.3.12"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")

    // https://github.com/Kotlin/dokka/issues/3472
    configurations.matching { it.name.startsWith("dokka") }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group.startsWith("com.fasterxml.jackson")) {
                useVersion("2.15.3")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
