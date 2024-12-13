/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.runtime.core.service

import org.eclipse.lmos.runtime.core.model.Agent
import org.eclipse.lmos.runtime.core.model.Conversation
import org.eclipse.lmos.runtime.core.service.outbound.AgentRoutingService

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
