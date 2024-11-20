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
        return agentList.first { agent -> agent.name == conversation.inputContext.explicitAgent }
    }
}
