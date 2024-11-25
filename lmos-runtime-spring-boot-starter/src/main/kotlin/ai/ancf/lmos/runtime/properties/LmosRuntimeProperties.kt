/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.properties

import ai.ancf.lmos.runtime.core.LmosRuntimeConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lmos.runtime")
class LmosRuntimeProperties(
    agentRegistry: AgentRegistry,
    openAi: OpenAI? = null,
    cache: Cache,
    val router: Router,
): LmosRuntimeConfig(agentRegistry, openAi, cache)

data class Router(
    val type: RouterType,
)

enum class RouterType {
    EXPLICIT,
    LLM,
}
