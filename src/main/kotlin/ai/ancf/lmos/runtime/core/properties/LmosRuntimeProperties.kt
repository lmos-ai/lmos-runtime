/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lmos.runtime")
class LmosRuntimeProperties(
    val agentRegistry: AgentRegistry,
    val openAi: OpenAI? = null,
    val router: Router = Router(RouterType.LLM),
) {
    data class Router(
        val type: RouterType,
    )

    enum class RouterType {
        EXPLICIT,
        LLM,
    }

    data class AgentRegistry(
        val baseUrl: String,
    )

    data class OpenAI(
        val url: String,
        val key: String,
        val model: String,
        val maxTokens: Int,
        val temperature: Double,
        val format: String,
    )
}
