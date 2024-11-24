/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.service

import ai.ancf.lmos.runtime.core.model.Agent
import ai.ancf.lmos.runtime.core.model.Conversation
import ai.ancf.lmos.runtime.core.service.outbound.AgentRoutingService

class ExplicitAgentRoutingService : AgentRoutingService {
    override suspend fun resolveAgentForConversation(
        conversation: Conversation,
        agentList: List<Agent>,
    ): Agent {
        val explicitAgent =
            conversation.inputContext.explicitAgent
                ?: throw IllegalArgumentException("Explicit agent name is required")
        return agentList.firstOrNull { agent -> agent.name == explicitAgent }
            ?: throw NoSuchElementException("No agent found with the name ${conversation.inputContext.explicitAgent}")
    }
}
