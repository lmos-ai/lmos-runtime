/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    `java-test-fixtures`
}

dependencies {

    val ktorVersion = "2.3.12"
    val arcVersion = "0.116.0"
    val junitVersion = "5.9.3"

    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("org.slf4j:slf4j-api:1.7.25")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.3")
    api("ai.ancf.lmos:lmos-router-llm:0.1.0")
    api("ai.ancf.lmos:arc-agent-client:$arcVersion")
    api("ai.ancf.lmos:arc-api:$arcVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")

    testFixturesImplementation("com.marcinziolo:kotlin-wiremock:2.1.1")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
}
