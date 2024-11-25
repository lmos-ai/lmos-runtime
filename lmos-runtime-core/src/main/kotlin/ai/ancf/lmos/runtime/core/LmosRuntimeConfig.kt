/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core

open class LmosRuntimeConfig(
    val agentRegistry: AgentRegistry,
    val openAi: OpenAI? = null,
    val cache: Cache,
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

    data class Cache(
        val ttl: Long
    )

}
