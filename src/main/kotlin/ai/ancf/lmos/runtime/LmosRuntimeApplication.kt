/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime

import ai.ancf.lmos.runtime.core.properties.LmosRuntimeProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(LmosRuntimeProperties::class)
open class LmosRuntimeApplication

fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger(LmosRuntimeApplication::class.java)
    try {
        runApplication<LmosRuntimeApplication>(*args)
    } catch (e: Exception) {
        log.info("Startup Failed: $e")
    }
}
