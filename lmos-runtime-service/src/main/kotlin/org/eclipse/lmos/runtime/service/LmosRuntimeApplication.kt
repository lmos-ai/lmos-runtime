/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.lmos.runtime.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class LmosRuntimeApplication

fun main(args: Array<String>) {
    runApplication<LmosRuntimeApplication>(*args)
}
