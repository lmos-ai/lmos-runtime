/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */
dependencies {

    val springBootVersion: String by rootProject.extra

    api(project(":lmos-runtime-core"))
    implementation("org.springframework.boot:spring-boot-starter:$springBootVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
}
