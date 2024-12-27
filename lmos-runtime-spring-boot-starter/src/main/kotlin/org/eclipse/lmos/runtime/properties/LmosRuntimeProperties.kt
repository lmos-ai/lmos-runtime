/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.runtime.properties

import org.eclipse.lmos.runtime.core.LmosRuntimeConfig
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lmos.runtime")
class LmosRuntimeProperties(
    agentRegistry: AgentRegistry,
    openAi: OpenAI? = null,
    cache: Cache,
    val router: Router,
) : LmosRuntimeConfig(agentRegistry, openAi, cache) {
    private val log = LoggerFactory.getLogger(LmosRuntimeProperties::class.java)

    init {
        log.info(
            "LmosRuntimeProperties initialized with: " +
                "agentRegistry = $agentRegistry, cache = $cache, router = $router",
        )
    }
}

data class Router(
    val type: Type,
)

enum class Type {
    EXPLICIT,
    LLM,
}
