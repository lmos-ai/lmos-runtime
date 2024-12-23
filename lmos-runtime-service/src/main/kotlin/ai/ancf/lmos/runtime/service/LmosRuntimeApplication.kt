/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package ai.ancf.lmos.runtime.service

import ai.ancf.lmos.runtime.service.properties.LmosRuntimeCorsProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(value = [LmosRuntimeCorsProperties::class])
open class LmosRuntimeApplication

fun main(args: Array<String>) {
    runApplication<LmosRuntimeApplication>(*args)
}
