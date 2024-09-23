/*
 * // SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.runtime.core.service.inbound

import ai.ancf.lmos.runtime.core.model.Agent
import ai.ancf.lmos.runtime.core.model.AssistantMessage
import ai.ancf.lmos.runtime.core.model.Conversation
import ai.ancf.lmos.runtime.core.service.outbound.AgentClientService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRegistryService
import ai.ancf.lmos.runtime.core.service.outbound.AgentRoutingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

public interface ConversationService {
    suspend fun processConversation(
        conversation: Conversation,
        conversationId: String,
        tenantId: String,
        turnId: String,
    ): AssistantMessage
}

@Service
class DefaultConversationService(
    private val agentRegistryService: AgentRegistryService,
    private val agentRoutingService: AgentRoutingService,
    private val agentClientService: AgentClientService,
) : ConversationService {
    private val log = LoggerFactory.getLogger(DefaultConversationService::class.java)

    override suspend fun processConversation(
        conversation: Conversation,
        conversationId: String,
        tenantId: String,
        turnId: String,
    ): AssistantMessage {
        log.info("Request Received, conversationId: $conversationId, turnId: $turnId")
        val agentList = agentRegistryService.getAgents(tenantId, conversation.systemContext.channelId)
        log.info("agentList: $agentList")
        val agent: Agent = agentRoutingService.resolveAgentForConversation(conversation, agentList)
        log.info("Resolved agent: $agent")
        val agentResponse = agentClientService.askAgent(conversation, conversationId, turnId, agent.name, agent.addresses.random())
        log.info("Agent Response: $agentResponse")
        return agentResponse
    }
}
