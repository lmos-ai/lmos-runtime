/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lmos.runtime")
class LmosRuntimeProperties(
    val agentRegistry: AgentRegistry,
    val openAI: OpenAI,
) {
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
